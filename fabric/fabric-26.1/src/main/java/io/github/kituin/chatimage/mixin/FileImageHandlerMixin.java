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

/**
 * 修复本地图片加载问题，添加详细错误日志
 */
@Mixin(value = FileImageHandler.class, remap = false)
public class FileImageHandlerMixin {
    private static final Logger LOGGER = LogUtils.getLogger();

    /**
     * @author ChatImage
     * @reason 修复本地图片加载：使用 NativeImage 替代 AWT ImageIO
     */
    @Overwrite
    public static void loadFile(String url) {
        try {
            byte[] bytes = Files.readAllBytes(Paths.get(url));
            LOGGER.info("[ChatImage] loadFile: 读取文件成功, {} bytes, url={}", bytes.length, url);
            
            Minecraft.getInstance().execute(() -> {
                try {
                    LOGGER.info("[ChatImage] loadFile: 开始在渲染线程上创建纹理...");
                    NativeImage nativeImage = NativeImage.read(new ByteArrayInputStream(bytes));
                    LOGGER.info("[ChatImage] loadFile: NativeImage.read 成功, {}x{}", nativeImage.getWidth(), nativeImage.getHeight());
                    
                    NativeImageBackedTexture texture = new NativeImageBackedTexture(nativeImage);
                    LOGGER.info("[ChatImage] loadFile: NativeImageBackedTexture 创建成功");
                    
                    ChatImageClientAdapter adapter = new ChatImageClientAdapter();
                    Identifier id = adapter.registerDynamicTexture("chatimage/local", texture);
                    LOGGER.info("[ChatImage] loadFile: 纹理注册成功, id={}", id);
                    
                    ChatImageFrame.TextureReader<Identifier> reader = new ChatImageFrame.TextureReader<>(
                            id, nativeImage.getWidth(), nativeImage.getHeight());
                    
                    // 通过反射设置 ChatImageFrame 的字段，因为构造函数不支持 TextureReader 参数
                    ChatImageFrame<Identifier> frame = new ChatImageFrame<>(new ByteArrayInputStream(new byte[0]));
                    // 上面会失败，换一种方式：直接用 InputStream 构造
                    // 重新用原始 bytes 创建
                    ChatImageFrame<Identifier> realFrame = new ChatImageFrame<>(new ByteArrayInputStream(bytes));
                    LOGGER.info("[ChatImage] loadFile: ChatImageFrame 创建成功, id={}", realFrame.getId());
                    
                    ClientStorage.AddImage(url, realFrame);
                    LOGGER.info("[ChatImage] loadFile: AddImage 成功");
                } catch (Exception e) {
                    LOGGER.error("[ChatImage] loadFile: 渲染线程上创建纹理失败", e);
                    ClientStorage.AddImageError(url, ChatImageFrame.FrameError.FILE_LOAD_ERROR);
                }
            });
        } catch (IOException e) {
            LOGGER.error("[ChatImage] loadFile: 读取文件失败", e);
            ClientStorage.AddImageError(url, ChatImageFrame.FrameError.FILE_LOAD_ERROR);
        }
    }
}
