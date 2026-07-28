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
import java.util.List;

@Environment(EnvType.CLIENT)
@Mixin(MouseHandler.class)
public class FileDragMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

// IF fabric-1.16.5
//    @Inject(at = @At("RETURN"), method = "method_29616")
// ELSE
    @Inject(at = @At("RETURN"), method = "onDrop")
// END IF
    private void onFilesDropped(long window, List<Path> paths,
// IF >= fabric-1.21
                                int invalidFilesCount,
// END IF
                                CallbackInfo ci) {
        if (this.minecraft.gui.screen() instanceof ChatScreen &&
                this.minecraft.level != null && ChatImageClient.CONFIG.dragImage) {
            StringBuilder sb = new StringBuilder();
            for (Path o : paths) {
                if (ChatImageClient.CONFIG.dragUseCicode) {
                    sb.append("[[CICode,url=file:///").append(o).append("]]");
                } else {
                    sb.append("file:///").append(o);
                }
            }
// IF >= fabric-1.21.9
            this.minecraft.gui.setScreen(new ChatScreen(sb.toString(), true));
// ELSE IF fabric-1.16.5
//            this.client.openScreen(new ChatScreen(sb.toString()));
// ELSE
//             this.client.setScreen(new ChatScreen(sb.toString()));
// END IF
        }
    }
}
