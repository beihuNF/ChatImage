// ONLY >= fabric-1.20
package io.github.kituin.chatimage.mixin;

import io.github.kituin.ChatImageCode.ChatImageCode;
import io.github.kituin.ChatImageCode.ChatImageFrame;
import io.github.kituin.ChatImageCode.ClientStorage;
import io.github.kituin.ChatImageCode.exception.InvalidChatImageCodeException;
import net.minecraft.client.Minecraft;
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
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

// IF >= neoforge-1.21.2
import java.util.function.Function;

// END IF
// IF >=fabric-1.21.5
// ELSE
// import static io.github.kituin.chatimage.tool.ChatImageStyle.SHOW_IMAGE;
// END IF
import static io.github.kituin.chatimage.tool.SimpleUtil.createLiteralComponent;
import static io.github.kituin.chatimage.tool.SimpleUtil.createTranslatableComponent;
import static io.github.kituin.chatimage.client.ChatImageClient.CONFIG;
/**
 * 注入修改悬浮显示图片
 *
 * @author kitUIN
 */
@Mixin(GuiGraphicsExtractor.class)
public abstract class DrawContextMixin {
    @Shadow
    @Nullable
    private Minecraft minecraft;

    @Shadow
    @Final
    private org.joml.Matrix3x2fStack pose;

    @Shadow
    public abstract  int guiWidth();

    @Shadow
    public abstract int guiHeight();

    @Shadow
    public abstract void setTooltipForNextFrame(Font textRenderer, List<? extends FormattedCharSequence> text, int x, int y);

    @Shadow
    public abstract void blit(
// IF >= fabric-1.21.6
            com.mojang.blaze3d.pipeline.RenderPipeline pipeline,
// ELSE IF >= fabric-1.21.2
//          Function<Identifier, RenderLayer> renderLayers,
// END IF
            Identifier texture, int x, int y,  float u, float v, int width, int height, int textureWidth, int textureHeight);
// END IF
// IF < fabric-1.21.2
//     @Shadow
//     public abstract void draw(Runnable drawCallback);
// END IF
    @SuppressWarnings("t")
    @Inject(at = @At("HEAD"), method = "componentHoverEffect", cancellable = true)
    public void drawHoverEvent(Font textRenderer, Style style, int x, int y, CallbackInfo ci) {
        if (style != null && style.getHoverEvent() != null) {
            HoverEvent hoverEvent = style.getHoverEvent();
// IF >=fabric-1.21.5
            if (!(hoverEvent instanceof HoverEvent.ShowText(Component encoded))) return;
            ChatImageCode code;
            try {
                code = new ChatImageCode.Builder().fromCode(encoded.getString()).build();
            } catch (InvalidChatImageCodeException ignored) {
                return;
            }
            ci.cancel();
// ELSE
//             ChatImageCode code = hoverEvent.getValue(SHOW_IMAGE);
// END IF
            if (code != null) {
                if (CONFIG.nsfw || !code.isNsfw() || ClientStorage.ContainNsfw(code.getUrl())) {
                    ChatImageFrame frame = code.getFrame();
                    if (frame.loadImage(CONFIG.limitWidth, CONFIG.limitHeight)) {
                        int viewWidth = frame.getWidth();
                        int viewHeight = frame.getHeight();
                        int allWidth = viewWidth + CONFIG.paddingLeft + CONFIG.paddingRight;
                        int allHeight = viewHeight + CONFIG.paddingTop + CONFIG.paddingBottom;
                        DefaultTooltipPositioner positioner = (DefaultTooltipPositioner) DefaultTooltipPositioner.INSTANCE;
                        Vector2ic vector2ic = positioner.positionTooltip(this.guiWidth(),this.guiHeight(),x, y,allWidth,allHeight );
                        int l = vector2ic.x();
                        int m = vector2ic.y();
                        // 背景
// IF >= fabric-1.21.6
                        this.pose.pushMatrix();

// ELSE
//                         this.matrices.push();
// END IF
// IF >= fabric-1.21.2
                        TooltipRenderUtil.extractTooltipBackground((GuiGraphicsExtractor) (Object)this, l, m, allWidth, allHeight,
    // IF <= fabric-1.21.5
//                          400,
    // END IF
                                null);
// ELSE
//                         this.draw(() -> {
//                             TooltipBackgroundRenderer.render((DrawContext) (Object)this, l, m, allWidth,allHeight, 400);
//                         });
// END IF




                        // 图片
// IF >= fabric-1.21.6
                        this.pose.translate(0.0F, 0.0F);
                        this.blit(
                                net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,

// ELSE
//                         this.matrices.translate(0.0F, 0.0F, 400.0F);
//                         this.drawTexture(
// END IF
// IF >= fabric-1.21.2 && <= fabric-1.21.5
//                          RenderLayer::getGuiTextured,
// END IF
                                (Identifier) frame.getId(), l + CONFIG.paddingLeft, m + CONFIG.paddingTop, 0, 0, viewWidth, viewHeight, viewWidth, viewHeight
                        );
// IF >= fabric-1.21.6
                        this.pose.popMatrix();
// ELSE
//                         this.matrices.pop();
// END IF
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
