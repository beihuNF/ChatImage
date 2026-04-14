package io.github.kituin.chatimage.mixin;

import com.mojang.logging.LogUtils;
import io.github.kituin.ChatImageCode.ChatImageFrame;
import io.github.kituin.ChatImageCode.ClientStorage;
import io.github.kituin.ChatImageCode.FileImageHandler;
import io.github.kituin.chatimage.integration.ChatImageClientAdapter;
import io.github.kituin.chatimage.integration.NativeImageBackedTexture;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Mixin(value = FileImageHandler.class, remap = false)
public class FileImageHandlerMixin {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Overwrite
    public static void loadFile(String url) {
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(url));
            
            Minecraft.getInstance().execute(() -> {
                try {
                    NativeImage nativeImage = NativeImage.read(new ByteArrayInputStream(bytes));
                    NativeImageBackedTexture texture = new NativeImageBackedTexture(nativeImage);
                    ChatImageClientAdapter adapter = new ChatImageClientAdapter();
                    Identifier id = adapter.registerDynamicTexture("chatimage/local", texture);
                    
                    ChatImageFrame.TextureReader<Identifier> reader = new ChatImageFrame.TextureReader<>(
                            id, nativeImage.getWidth(), nativeImage.getHeight());
                    
                    ChatImageFrame<Identifier> realFrame = new ChatImageFrame<>(new ByteArrayInputStream(bytes));
                    
                    ClientStorage.AddImage(url, realFrame);
                } catch (Exception e) {
                    LOGGER.error("[ChatImage] loadFile Error: ", e);
                    ClientStorage.AddImageError(url, ChatImageFrame.FrameError.FILE_LOAD_ERROR);
                }
            });
        } catch (IOException e) {
            LOGGER.error("[ChatImage] loadFile Error: ", e);
            ClientStorage.AddImageError(url, ChatImageFrame.FrameError.FILE_LOAD_ERROR);
        }
    }
}
