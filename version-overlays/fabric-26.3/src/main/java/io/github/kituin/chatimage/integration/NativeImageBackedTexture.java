package io.github.kituin.chatimage.integration;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import com.mojang.renderpearl.api.GpuFormat;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.textures.FilterMode;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;

public class NativeImageBackedTexture extends AbstractTexture implements AutoCloseable {
    private static final Logger LOGGER = LogUtils.getLogger();
    @Nullable
    private NativeImage image;

    public NativeImageBackedTexture(NativeImage image) {
        this.image = image;
        this.initializeTexture();
    }

    private void initializeTexture() {
        NativeImage currentImage = this.image;
        if (currentImage == null) {
            throw new IllegalStateException("Cannot initialize a disposed texture");
        }

        GpuDevice device = RenderSystem.getDevice();
        this.texture = device.createTexture(
                (String) null,
                5,
                GpuFormat.RGBA8_UNORM,
                this.image.getWidth(),
                this.image.getHeight(),
                1,
                1
        );
        this.sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.NEAREST);
        this.textureView = device.createTextureView(this.texture);
        this.upload();
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
