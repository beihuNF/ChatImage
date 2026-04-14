package io.github.kituin.ChatImageCode;

import chatimage.com.madgag.gif.fmsware.GifDecoder;
import com.mojang.blaze3d.platform.NativeImage;
import io.github.kituin.ChatImageCode.enums.ChatImageType;
import io.github.kituin.chatimage.integration.NativeImageBackedTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class FileImageHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("ChatImage");
    private static final AtomicInteger TEXTURE_COUNTER = new AtomicInteger(0);

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(b & 0xFF);
            if (hex.length() < 2) sb.append(0);
            sb.append(hex);
        }
        return sb.toString();
    }

    public static ChatImageType getPicType(byte[] bytes) {
        byte[] header = new byte[4];
        System.arraycopy(bytes, 0, header, 0, Math.min(bytes.length, header.length));
        String hex = bytesToHex(header).toUpperCase();
        if (hex.startsWith("47494638")) return ChatImageType.GIF;
        if (hex.startsWith("00000100")) return ChatImageType.ICO;
        if (hex.startsWith("52494646")) return ChatImageType.WEBP;
        return ChatImageType.PNG;
    }

    public static void loadFile(String url) {
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(url));
            loadFile(bytes, url);
        } catch (IOException e) {
            ClientStorage.AddImageError(url, ChatImageFrame.FrameError.FILE_LOAD_ERROR);
        }
    }

    public static void loadFile(InputStream inputStream, String url) {
        try {
            byte[] bytes = readAllBytes(inputStream);
            loadFile(bytes, url);
        } catch (IOException e) {
            ClientStorage.AddImageError(url, ChatImageFrame.FrameError.FILE_LOAD_ERROR);
        }
    }

    public static void loadFile(byte[] bytes, String url) {
        ChatImageType type = getPicType(bytes);
        ChatImageCodeInstance.LOGGER.info("[FileImageHandler][{}]Image Type: {}", url, type.name());
        if (type == ChatImageType.GIF) {
            loadGif(bytes, url);
        } else {
            loadStaticImage(bytes, url);
        }
    }

    public static void loadGif(InputStream inputStream, String url) {
        try {
            byte[] bytes = readAllBytes(inputStream);
            loadGif(bytes, url);
        } catch (Exception e) {
            ClientStorage.AddImageError(url, ChatImageFrame.FrameError.FILE_LOAD_ERROR);
        }
    }

    public static void loadGif(byte[] bytes, String url) {
        CompletableFuture.supplyAsync(() -> {
            try {
                GifDecoder decoder = new GifDecoder();
                int status = decoder.read(new ByteArrayInputStream(bytes));
                if (status != 0) {
                    ClientStorage.AddImageError(url, ChatImageFrame.FrameError.FILE_LOAD_ERROR);
                    return null;
                }
                int frameCount = decoder.getFrameCount();
                if (frameCount == 0) {
                    ClientStorage.AddImageError(url, ChatImageFrame.FrameError.FILE_LOAD_ERROR);
                    return null;
                }

                BufferedImage[] frames = new BufferedImage[frameCount];
                for (int i = 0; i < frameCount; i++) {
                    frames[i] = decoder.getFrame(i);
                }

                Minecraft.getInstance().execute(() -> {
                    try {
                        ChatImageFrame<Identifier> firstFrame = null;
                        for (int i = 0; i < frames.length; i++) {
                            NativeImage nativeImage = convertToNativeImage(frames[i]);
                            Identifier id = createUniqueIdentifier();
                            Minecraft.getInstance().getTextureManager()
                                    .register(id, new NativeImageBackedTexture(nativeImage));
                            ChatImageFrame<Identifier> frame = createFrame(
                                    id, nativeImage.getWidth(), nativeImage.getHeight());
                            if (i == 0) {
                                firstFrame = frame;
                            } else {
                                firstFrame.append(frame);
                            }
                        }
                        if (firstFrame != null) {
                            ClientStorage.AddImage(url, firstFrame);
                        } else {
                            ClientStorage.AddImageError(url, ChatImageFrame.FrameError.FILE_LOAD_ERROR);
                        }
                    } catch (Exception e) {
                        LOGGER.error("[FileImageHandler] GIF texture failed: {}", url, e);
                        ClientStorage.AddImageError(url, ChatImageFrame.FrameError.FILE_LOAD_ERROR);
                    }
                });
            } catch (Exception e) {
                LOGGER.error("[FileImageHandler] GIF decode error: {}", url, e);
                ClientStorage.AddImageError(url, ChatImageFrame.FrameError.FILE_LOAD_ERROR);
            }
            return null;
        });
    }

    private static void loadStaticImage(byte[] bytes, String url) {
        Minecraft.getInstance().execute(() -> {
            try {
                NativeImage nativeImage;
                if (isPng(bytes)) {
                    nativeImage = NativeImage.read(new ByteArrayInputStream(bytes));
                } else {
                    BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(bytes));
                    if (bufferedImage == null) {
                        throw new IOException("ImageIO failed to decode image");
                    }
                    nativeImage = convertToNativeImage(bufferedImage);
                }
                Identifier id = createUniqueIdentifier();
                Minecraft.getInstance().getTextureManager()
                        .register(id, new NativeImageBackedTexture(nativeImage));
                ChatImageFrame<Identifier> frame = createFrame(
                        id, nativeImage.getWidth(), nativeImage.getHeight());
                ClientStorage.AddImage(url, frame);
            } catch (Exception e) {
                LOGGER.error("[FileImageHandler] Static image failed: {}", url, e);
                ClientStorage.AddImageError(url, ChatImageFrame.FrameError.FILE_LOAD_ERROR);
            }
        });
    }

    private static boolean isPng(byte[] bytes) {
        return bytes.length >= 4
                && bytes[0] == (byte) 0x89
                && bytes[1] == (byte) 0x50
                && bytes[2] == (byte) 0x4E
                && bytes[3] == (byte) 0x47;
    }

    private static Identifier createUniqueIdentifier() {
        int id = TEXTURE_COUNTER.incrementAndGet();
        return Identifier.withDefaultNamespace(
                String.format(Locale.ROOT, "dynamic/chatimage_local_%d", id));
    }

    private static NativeImage convertToNativeImage(BufferedImage bufferedImage) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();
        NativeImage nativeImage = new NativeImage(width, height, false);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = bufferedImage.getRGB(x, y);
                int a = (argb >> 24) & 0xFF;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                nativeImage.setPixelABGR(x, y, (a << 24) | (b << 16) | (g << 8) | r);
            }
        }
        return nativeImage;
    }

    private static ChatImageFrame<Identifier> createFrame(Identifier id, int width, int height) throws Exception {
        ChatImageFrame<Identifier> frame = new ChatImageFrame<>(ChatImageFrame.FrameError.FILE_LOAD_ERROR);
        setField(frame, "id", id);
        setField(frame, "width", width);
        setField(frame, "height", height);
        setField(frame, "originalWidth", width);
        setField(frame, "originalHeight", height);
        setField(frame, "error", null);
        return frame;
    }

    private static void setField(Object obj, String fieldName, Object value) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(obj, value);
    }

    private static byte[] readAllBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, bytesRead);
        }
        buffer.flush();
        return buffer.toByteArray();
    }
}
