package io.github.kituin.chatimage.legacy;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import org.junit.Test;
import static org.junit.Assert.*;

public class ImageDataTest {
    @Test
    public void decodesTransparentPngAndRejectsInvalidImages() throws Exception {
        BufferedImage image = new BufferedImage(2, 1, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0x80FF0000);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        ImageData decoded = ImageData.decode(output.toByteArray());
        assertEquals(2, decoded.width);
        assertEquals(0x80FF0000, decoded.frames[0].getRGB(0, 0));
        try {
            ImageData.decode(new byte[] {1, 2, 3});
            fail("Expected invalid image");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("Unsupported"));
        }
    }

    @Test
    public void rejectsLargeDimensionsBeforePixelDecode() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(4097, 1, BufferedImage.TYPE_INT_RGB), "png", output);
        try {
            ImageData.decode(output.toByteArray());
            fail("Expected dimension limit");
        } catch (IOException expected) {
            assertTrue(expected.getMessage().contains("pixel limit"));
        }
    }

    @Test
    public void composesGifSubframesWithDisposalAndFrameTiming() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("gif").next();
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(stream);
            writer.prepareWriteSequence(null);
            writeFrame(writer, solid(3, Color.RED), 0, "none", 5);
            writeFrame(writer, solid(1, Color.GREEN), 1, "restoreToPrevious", 3);
            writeFrame(writer, solid(1, Color.BLUE), 2, "restoreToBackgroundColor", 7);
            writeFrame(writer, solid(1, Color.GREEN), 0, "none", 0);
            writer.endWriteSequence();
        } finally {
            writer.dispose();
        }
        ImageData decoded = ImageData.decode(output.toByteArray());
        assertEquals(4, decoded.frames.length);
        assertArrayEquals(new int[] {50, 30, 70, 20}, decoded.delays);
        assertEquals(Color.RED.getRGB(), decoded.frames[1].getRGB(0, 0));
        assertEquals(Color.GREEN.getRGB(), decoded.frames[1].getRGB(1, 0));
        assertEquals(Color.RED.getRGB(), decoded.frames[2].getRGB(1, 0));
        assertEquals(Color.BLUE.getRGB(), decoded.frames[2].getRGB(2, 0));
        assertEquals(Color.RED.getRGB(), decoded.frames[3].getRGB(2, 0));
    }

    private static BufferedImage solid(int width, Color color) {
        java.awt.image.IndexColorModel palette = new java.awt.image.IndexColorModel(8, 3,
                new byte[] {(byte) 255, 0, 0}, new byte[] {0, (byte) 255, 0}, new byte[] {0, 0, (byte) 255});
        BufferedImage image = new BufferedImage(width, 1, BufferedImage.TYPE_BYTE_INDEXED, palette);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(color);
        graphics.fillRect(0, 0, width, 1);
        graphics.dispose();
        return image;
    }

    private static void writeFrame(ImageWriter writer, BufferedImage image, int left, String disposal, int delay) throws Exception {
        IIOMetadata metadata = writer.getDefaultImageMetadata(
                javax.imageio.ImageTypeSpecifier.createFromRenderedImage(image), writer.getDefaultWriteParam());
        String format = "javax_imageio_gif_image_1.0";
        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(format);
        IIOMetadataNode descriptor = (IIOMetadataNode) root.getElementsByTagName("ImageDescriptor").item(0);
        descriptor.setAttribute("imageLeftPosition", Integer.toString(left));
        IIOMetadataNode control = (IIOMetadataNode) root.getElementsByTagName("GraphicControlExtension").item(0);
        control.setAttribute("disposalMethod", disposal);
        control.setAttribute("delayTime", Integer.toString(delay));
        metadata.setFromTree(format, root);
        writer.writeToSequence(new IIOImage(image, null, metadata), writer.getDefaultWriteParam());
    }
}
