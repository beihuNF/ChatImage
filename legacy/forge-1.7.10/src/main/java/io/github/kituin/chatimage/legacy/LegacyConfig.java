package io.github.kituin.chatimage.legacy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class LegacyConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public int limitWidth = 320;
    public int limitHeight = 240;
    public int timeout = 10;
    public int MaxFileSize = 4096; // KiB, enforced for both Content-Length and streamed bytes.
    public int cacheEntries = 8;
    public boolean nsfw = false;
    public boolean cqCode = true;
    public boolean checkImageUri = true;

    public static LegacyConfig load(File file) throws IOException {
        LegacyConfig config = new LegacyConfig();
        if (file.isFile()) {
            try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
                LegacyConfig loaded = GSON.fromJson(reader, LegacyConfig.class);
                if (loaded != null) config = loaded;
            } catch (RuntimeException e) {
                throw new IOException("Invalid ChatImage config: " + file, e);
            }
        }
        config.limitWidth = clamp(config.limitWidth, 16, 1024);
        config.limitHeight = clamp(config.limitHeight, 16, 1024);
        config.timeout = clamp(config.timeout, 1, 60);
        config.MaxFileSize = clamp(config.MaxFileSize, 64, 16384);
        config.cacheEntries = clamp(config.cacheEntries, 1, 16);
        if (!file.exists()) {
            Files.createDirectories(file.toPath().getParent());
            try (Writer writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
        }
        return config;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
