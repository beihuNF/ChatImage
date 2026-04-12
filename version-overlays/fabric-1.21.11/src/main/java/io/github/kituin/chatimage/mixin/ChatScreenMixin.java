package io.github.kituin.chatimage.mixin;

import io.github.kituin.ChatImageCode.ChatImageCode;
import io.github.kituin.ChatImageCode.ClientStorage;
import io.github.kituin.chatimage.gui.ConfirmNsfwScreen;
import io.github.kituin.chatimage.tool.ChatImageStyle.ShowImage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static io.github.kituin.chatimage.client.ChatImageClient.CONFIG;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
    @Inject(method = "handleClickEvent", at = @At("RETURN"), cancellable = true)
    private void chatimage$confirmNsfw(Style style, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (style == null || style.getHoverEvent() == null) {
            return;
        }

        HoverEvent hoverEvent = style.getHoverEvent();
        if (!(hoverEvent instanceof ShowImage(ChatImageCode code))) {
            return;
        }

        if (code.isNsfw() && !ClientStorage.ContainNsfw(code.getUrl()) && !CONFIG.nsfw) {
            String url = code.getUrl();
            MinecraftClient client = MinecraftClient.getInstance();
            client.setScreen(new ConfirmNsfwScreen(open -> {
                if (open) {
                    ClientStorage.AddNsfw(url, 1);
                }
                client.setScreen((Screen) (Object) this);
            }, url));
            cir.setReturnValue(true);
        }
    }
}
