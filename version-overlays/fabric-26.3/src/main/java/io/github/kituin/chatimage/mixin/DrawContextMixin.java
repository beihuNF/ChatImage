package io.github.kituin.chatimage.mixin;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import io.github.kituin.ChatImageCode.ChatImageCode;
import io.github.kituin.ChatImageCode.ChatImageFrame;
import io.github.kituin.ChatImageCode.ClientStorage;
import io.github.kituin.ChatImageCode.exception.InvalidChatImageCodeException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2fStack;
import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import static io.github.kituin.chatimage.client.ChatImageClient.CONFIG;
import static io.github.kituin.chatimage.tool.SimpleUtil.createLiteralComponent;
import static io.github.kituin.chatimage.tool.SimpleUtil.createTranslatableComponent;

/**
 * 注入修改悬浮显示图片
 *
 * @author kitUIN
 */
@Environment(EnvType.CLIENT)
@Mixin(GuiGraphicsExtractor.class)
public abstract class DrawContextMixin {

    @Shadow
    @Final
    public Matrix3x2fStack pose;

    @Shadow
    public abstract int guiWidth();

    @Shadow
    public abstract int guiHeight();

    @Shadow
    public abstract void setTooltipForNextFrame(Font textRenderer, List<? extends FormattedCharSequence> text, int x, int y);

    @Shadow
    public abstract void blit(
            RenderPipeline pipeline,
            Identifier texture, int x, int y, float u, float v, int width, int height, int textureWidth, int textureHeight);

    @SuppressWarnings("t")
    @Inject(at = @At("HEAD"), method = "componentHoverEffect", cancellable = true)
    public void drawHoverEvent(Font textRenderer, Style style, int x, int y, CallbackInfo ci) {
        if (style != null && style.getHoverEvent() != null) {
            HoverEvent hoverEvent = style.getHoverEvent();
            if (!(hoverEvent instanceof HoverEvent.ShowText(Component encoded))) return;
            ChatImageCode code;
            try {
                code = new ChatImageCode.Builder().fromCode(encoded.getString()).build();
            } catch (InvalidChatImageCodeException ignored) {
                return;
            }
            ci.cancel();
            if (code != null) {
                if (CONFIG.nsfw || !code.isNsfw() || ClientStorage.ContainNsfw(code.getUrl())) {
                    ChatImageFrame frame = code.getFrame();
                    if (frame.loadImage(CONFIG.limitWidth, CONFIG.limitHeight)) {
                        int viewWidth = frame.getWidth();
                        int viewHeight = frame.getHeight();
                        int allWidth = viewWidth + CONFIG.paddingLeft + CONFIG.paddingRight;
                        int allHeight = viewHeight + CONFIG.paddingTop + CONFIG.paddingBottom;
                        DefaultTooltipPositioner positioner = (DefaultTooltipPositioner) DefaultTooltipPositioner.INSTANCE;
                        Vector2ic vector2ic = positioner.positionTooltip(this.guiWidth(), this.guiHeight(), x, y, allWidth, allHeight);
                        int l = vector2ic.x();
                        int m = vector2ic.y();
                        // 背景
                        this.pose.pushMatrix();
                        TooltipRenderUtil.extractTooltipBackground((GuiGraphicsExtractor) (Object) this, l, m, allWidth, allHeight, null);

                        // 图片
                        this.pose.translate(0.0F, 0.0F);
                        this.blit(
                                net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,
                                (Identifier) frame.getId(), l + CONFIG.paddingLeft, m + CONFIG.paddingTop, 0, 0, viewWidth, viewHeight, viewWidth, viewHeight
                        );
                        this.pose.popMatrix();
                        frame.gifLoop(CONFIG.gifSpeed);
                    } else {
                        MutableComponent text = (MutableComponent) frame.getErrorMessage(
                                (str) -> createLiteralComponent((String) str),
                                (str) -> createTranslatableComponent((String) str),
                                (obj, s) -> ((MutableComponent) obj).append((Component) s), code);
                        this.setTooltipForNextFrame(textRenderer, textRenderer.split(text, Math.max(this.guiWidth() / 2, 200)), x, y);
                    }
                } else {
                    this.setTooltipForNextFrame(textRenderer, textRenderer.split(createTranslatableComponent("nsfw.chatimage.message"), Math.max(this.guiWidth() / 2, 200)), x, y);
                }
            }
        }
    }
}
