package io.github.kituin.chatimage.mixin;

import io.github.kituin.chatimage.client.ChatImageClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Environment(EnvType.CLIENT)
@Mixin(MouseHandler.class)
public class FileDragMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(at = @At("RETURN"), method = "onDrop")
    private void onFilesDropped(long window, List<String> paths, CallbackInfo ci) {
        if (this.minecraft.gui.screen() instanceof ChatScreen &&
                this.minecraft.level != null && ChatImageClient.CONFIG.dragImage) {
            StringBuilder sb = new StringBuilder();
            for (String p : paths) {
                try {
                    Path o = Paths.get(p);
                    if (ChatImageClient.CONFIG.dragUseCicode) {
                        sb.append("[[CICode,url=file:///").append(o).append("]]");
                    } else {
                        sb.append("file:///").append(o);
                    }
                } catch (Exception ignored) {
                }
            }
            this.minecraft.gui.setScreen(new ChatScreen(sb.toString(), true));
        }
    }
}
