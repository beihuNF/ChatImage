package io.github.kituin.chatimage.legacy;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/** Decodes on a worker thread; contains no Minecraft or OpenGL calls. */
final class ImageData {
    private static final long MAX_PIXELS = 8L * 1024 * 1024;
    private static final int MAX_FRAMES = 128;
    final BufferedImage[] frames;
    final int[] delays;
    final int width;
    final int height;

    private ImageData(BufferedImage[] frames, int[] delays) {
        this.frames = frames;
        this.delays = delays;
        width = frames[0].getWidth();
        height = frames[0].getHeight();
    }

    static ImageData decode(byte[] bytes) throws IOException {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("Unsupported image format");
            ImageReader reader = readers.next();
            try {
                reader.setInput(input);
                if (!"gif".equalsIgnoreCase(reader.getFormatName())) {
                    checkSize(reader.getWidth(0), reader.getHeight(0), 1);
                    return new ImageData(new BufferedImage[] {reader.read(0)}, new int[] {100});
                }
                return decodeGif(reader);
            } finally {
                reader.dispose();
            }
        } catch (RuntimeException e) {
            throw new IOException("Invalid image", e);
        }
    }

    private static ImageData decodeGif(ImageReader reader) throws IOException {
        int count = reader.getNumImages(true);
        if (count < 1 || count > MAX_FRAMES) throw new IOException("GIF exceeds frame limit (128)");
        Node stream = reader.getStreamMetadata().getAsTree("javax_imageio_gif_stream_1.0");
        Node logical = child(stream, "LogicalScreenDescriptor");
        Node table = child(stream, "GlobalColorTable");
        int backgroundIndex = number(table, "backgroundColorIndex", 0);
        int background = backgroundColor(table, backgroundIndex);
        int width = number(logical, "logicalScreenWidth", reader.getWidth(0));
        int height = number(logical, "logicalScreenHeight", reader.getHeight(0));
        checkSize(width, height, count);
        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        BufferedImage[] frames = new BufferedImage[count];
        int[] delays = new int[count];
        BufferedImage previousCanvas = null;
        String disposal = "none";
        int clearColor = 0;
        int left = 0, top = 0, frameWidth = 0, frameHeight = 0;
        for (int i = 0; i < count; i++) {
            if ("restoreToBackgroundColor".equals(disposal)) {
                Graphics2D clear = canvas.createGraphics();
                clear.setComposite(AlphaComposite.Src);
                clear.setColor(new Color(clearColor, true));
                clear.fillRect(left, top, frameWidth, frameHeight);
                clear.dispose();
            } else if ("restoreToPrevious".equals(disposal) && previousCanvas != null) {
                canvas = previousCanvas;
            }
            Node metadata = reader.getImageMetadata(i).getAsTree("javax_imageio_gif_image_1.0");
            Node descriptor = child(metadata, "ImageDescriptor");
            left = number(descriptor, "imageLeftPosition", 0);
            top = number(descriptor, "imageTopPosition", 0);
            frameWidth = number(descriptor, "imageWidth", reader.getWidth(i));
            frameHeight = number(descriptor, "imageHeight", reader.getHeight(i));
            checkSize(frameWidth, frameHeight, 1);
            if (left < 0 || top < 0 || (long) left + frameWidth > width || (long) top + frameHeight > height) {
                throw new IOException("GIF frame exceeds canvas");
            }
            Node control = child(metadata, "GraphicControlExtension");
            boolean transparentBackground = "TRUE".equalsIgnoreCase(attribute(control, "transparentColorFlag", "FALSE"))
                    && number(control, "transparentColorIndex", -1) == backgroundIndex;
            clearColor = transparentBackground ? 0 : background;
            if (i == 0 && clearColor != 0) {
                Graphics2D initial = canvas.createGraphics();
                initial.setColor(new Color(clearColor, true));
                initial.fillRect(0, 0, width, height);
                initial.dispose();
            }
            disposal = attribute(control, "disposalMethod", "none");
            previousCanvas = "restoreToPrevious".equals(disposal) ? copy(canvas) : null;
            Graphics2D graphics = canvas.createGraphics();
            graphics.drawImage(reader.read(i), left, top, null);
            graphics.dispose();
            frames[i] = copy(canvas);
            delays[i] = Math.max(20, number(control, "delayTime", 10) * 10);
        }
        return new ImageData(frames, delays);
    }

    private static void checkSize(int width, int height, int frames) throws IOException {
        if (width < 1 || height < 1 || width > 4096 || height > 4096 || (long) width * height * frames > MAX_PIXELS) {
            throw new IOException("Image exceeds decoded pixel limit");
        }
    }

    private static BufferedImage copy(BufferedImage original) {
        BufferedImage copy = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = copy.createGraphics();
        graphics.drawImage(original, 0, 0, null);
        graphics.dispose();
        return copy;
    }

    private static Node child(Node root, String name) {
        for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (name.equals(node.getNodeName())) return node;
        }
        return null;
    }

    private static String attribute(Node node, String name, String fallback) {
        NamedNodeMap attributes = node == null ? null : node.getAttributes();
        Node value = attributes == null ? null : attributes.getNamedItem(name);
        return value == null ? fallback : value.getNodeValue();
    }

    private static int number(Node node, String name, int fallback) {
        return Integer.parseInt(attribute(node, name, Integer.toString(fallback)));
    }

    private static int backgroundColor(Node table, int index) {
        if (table != null) {
            for (Node color = table.getFirstChild(); color != null; color = color.getNextSibling()) {
                if (number(color, "index", -1) == index) {
                    return 0xFF000000 | number(color, "red", 0) << 16
                            | number(color, "green", 0) << 8 | number(color, "blue", 0);
                }
            }
        }
        return 0;
    }
}
