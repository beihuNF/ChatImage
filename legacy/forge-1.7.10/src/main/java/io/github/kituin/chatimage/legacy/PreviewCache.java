package io.github.kituin.chatimage.legacy;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.ResourceLocation;

/** Map and textures are owned by the client thread; workers only publish decoded images. */
final class PreviewCache {
    private final Map<String, Entry> entries = new LinkedHashMap<>(16, 0.75f, true);
    private final ExecutorService workers = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "ChatImage-download");
        thread.setDaemon(true);
        return thread;
    });

    Entry get(String url, LegacyConfig config) {
        Entry entry = entries.get(url);
        if (entry != null) return entry;
        while (entries.size() >= config.cacheEntries) {
            Iterator<Entry> iterator = entries.values().iterator();
            iterator.next().close();
            iterator.remove();
        }
        boolean[] usedSlots = new boolean[16];
        for (Entry existing : entries.values()) usedSlots[existing.slot] = true;
        int slot = 0;
        while (usedSlots[slot]) slot++;
        entry = new Entry(slot);
        entries.put(url, entry);
        final Entry pending = entry;
        pending.future = workers.submit(() -> {
            try {
                byte[] bytes = ImageDownload.read(url, config.MaxFileSize * 1024, config.timeout * 1000);
                if (pending.closed) return;
                ImageData data = ImageData.decode(bytes);
                if (!pending.closed) pending.data = data;
            } catch (Exception e) {
                pending.error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
                ChatImageLegacy.LOGGER.debug("Unable to load chat image {}", url, e);
            }
        });
        return entry;
    }

    void clear() {
        for (Entry entry : entries.values()) entry.close();
        entries.clear();
    }

    void remove(String url) {
        Entry entry = entries.remove(url);
        if (entry != null) entry.close();
    }

    static final class Entry {
        private final int slot;
        volatile ImageData data;
        volatile String error;
        volatile boolean closed;
        private Future<?> future;
        private ResourceLocation[] textures;
        private long started;

        Entry(int slot) { this.slot = slot; }

        ResourceLocation texture(long now) {
            ImageData image = data;
            if (image == null || closed) return null;
            if (textures == null) {
                textures = new ResourceLocation[image.frames.length];
                started = now;
            }
            long duration = 0;
            for (int delay : image.delays) duration += delay;
            long elapsed = (now - started) % duration;
            int index = 0;
            while (index < image.delays.length - 1 && elapsed >= image.delays[index]) {
                elapsed -= image.delays[index++];
            }
            if (textures[index] == null) {
                // Called only from DrawScreenEvent.Post, never from the decoder thread.
                // Reuse a bounded set of names: 1.7.10 deleteTexture does not remove its map entry.
                textures[index] = new ResourceLocation("chatimage", "preview/" + slot + "/" + index);
                Minecraft.getMinecraft().getTextureManager().loadTexture(
                        textures[index], new DynamicTexture(image.frames[index]));
                image.frames[index] = null;
            }
            return textures[index];
        }

        void close() {
            if (closed) return;
            closed = true;
            if (future != null) future.cancel(true);
            data = null;
            if (textures != null) {
                for (ResourceLocation texture : textures) {
                    if (texture != null) {
                        Minecraft.getMinecraft().getTextureManager().deleteTexture(texture);
                        // Drop DynamicTexture's retained pixel array as well as the OpenGL allocation.
                        Minecraft.getMinecraft().getTextureManager().loadTexture(texture, TextureUtil.missingTexture);
                    }
                }
            }
        }
    }
}
