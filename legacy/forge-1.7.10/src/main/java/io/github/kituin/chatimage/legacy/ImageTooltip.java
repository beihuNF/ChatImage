package io.github.kituin.chatimage.legacy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

final class ImageTooltip {
    private ImageTooltip() {}

    static void draw(GuiScreen screen, int mouseX, int mouseY, PreviewCache.Entry entry, LegacyConfig config) {
        if (entry.error != null) {
            text(screen, mouseX, mouseY, new ChatComponentTranslation("error.chatimage.legacy", entry.error).getUnformattedText());
            return;
        }
        ResourceLocation texture = entry.texture(System.nanoTime() / 1000000L);
        ImageData data = entry.data;
        if (texture == null || data == null) {
            text(screen, mouseX, mouseY, new ChatComponentTranslation("loading.chatimage.legacy").getUnformattedText());
            return;
        }
        int maxWidth = Math.max(1, Math.min(config.limitWidth, screen.width - 16));
        int maxHeight = Math.max(1, Math.min(config.limitHeight, screen.height - 16));
        double scale = Math.min(1, Math.min((double) maxWidth / data.width, (double) maxHeight / data.height));
        int width = Math.max(1, (int) (data.width * scale));
        int height = Math.max(1, (int) (data.height * scale));
        int x = Math.max(5, Math.min(mouseX + 12, screen.width - width - 5));
        int y = Math.max(5, Math.min(mouseY - 12, screen.height - height - 5));
        push();
        try {
            Gui.drawRect(x - 4, y - 4, x + width + 4, y + height + 4, 0xF0100010);
            Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glColor4f(1, 1, 1, 1);
            Tessellator tessellator = Tessellator.instance;
            tessellator.startDrawingQuads();
            tessellator.addVertexWithUV(x, y + height, 0, 0, 1);
            tessellator.addVertexWithUV(x + width, y + height, 0, 1, 1);
            tessellator.addVertexWithUV(x + width, y, 0, 1, 0);
            tessellator.addVertexWithUV(x, y, 0, 0, 0);
            tessellator.draw();
        } finally {
            pop();
        }
    }

    static void text(GuiScreen screen, int mouseX, int mouseY, String message) {
        Minecraft mc = Minecraft.getMinecraft();
        int available = Math.max(20, screen.width - 20);
        java.util.List<String> lines = mc.fontRenderer.listFormattedStringToWidth(message, available);
        int width = 0;
        for (String line : lines) width = Math.max(width, mc.fontRenderer.getStringWidth(line));
        int height = Math.min(lines.size() * 10, Math.max(10, screen.height - 20));
        int x = Math.max(5, Math.min(mouseX + 12, screen.width - width - 5));
        int y = Math.max(5, Math.min(mouseY - 12, screen.height - height - 5));
        push();
        try {
            Gui.drawRect(x - 4, y - 4, x + width + 4, y + height + 4, 0xF0100010);
            for (int i = 0; i < lines.size() && i * 10 < height; i++) {
                mc.fontRenderer.drawStringWithShadow(lines.get(i), x, y + i * 10, 0xFFFFFF);
            }
        } finally {
            pop();
        }
    }

    private static void push() {
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glPushMatrix();
        GL11.glTranslatef(0, 0, 400);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
    }

    private static void pop() {
        GL11.glPopMatrix();
        GL11.glPopAttrib();
    }
}
