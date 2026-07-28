// ONLY >= fabric-1.21.5
package io.github.kituin.chatimage.integration;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.logging.LogUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.Dumpable;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

@Environment(EnvType.CLIENT)
public class NativeImageBackedTexture extends AbstractTexture implements Dumpable {
    private static final Logger LOGGER = LogUtils.getLogger();

    @Nullable
    private NativeImage image;

    public NativeImageBackedTexture(NativeImage image) {
        this.image = image;
        if (RenderSystem.isOnRenderThread()) {
            this.initializeTexture();
            return;
        }

        CompletableFuture<Void> initialized = new CompletableFuture<>();
        Minecraft.getInstance().execute(() -> {
            try {
                this.initializeTexture();
                initialized.complete(null);
            } catch (Throwable throwable) {
                initialized.completeExceptionally(throwable);
            }
        });

        try {
            initialized.join();
        } catch (CompletionException exception) {
            throw new IllegalStateException("Failed to initialize texture", exception.getCause());
        }
    }

    private void initializeTexture() {
        NativeImage currentImage = this.image;
        if (currentImage == null) {
            throw new IllegalStateException("Cannot initialize a disposed texture");
        }

            this.texture = RenderSystem.getDevice().createTexture(
                    (String) null,
// IF >= fabric-1.21.6
                    1,
// END IF
                    TextureFormat.RGBA8,
                    this.image.getWidth(),
                    this.image.getHeight(),
// IF >= fabric-1.21.6
                    1,
// END IF
                    1
            );
            this.upload();
// IF >= fabric-1.21.6
            this.textureView = RenderSystem.getDevice().createTextureView(this.texture);
// END IF
    }

    public void upload() {
        if (this.image != null && this.texture != null) {
            RenderSystem.getDevice().createCommandEncoder().writeToTexture(this.texture, this.image);
        } else {
            LOGGER.warn("Trying to upload an uninitialized or disposed texture");
        }

    }

    @Nullable
    public NativeImage getImage() {
        return this.image;
    }

    public void setImage(NativeImage image) {
        if (this.image != null) {
            this.image.close();
        }

        this.image = image;
    }

    public void close() {
        if (this.image != null) {
            this.image.close();
            this.image = null;
        }

        super.close();
    }

    public void dumpContents(Identifier id, Path path) throws IOException {
        if (this.image != null) {
            String string = id.toDebugFileName() + ".png";
            Path path2 = path.resolve(string);
            this.image.writeToFile(path2);
        }

    }
}
