// ONLY >= fabric-1.20.5
package io.github.kituin.chatimage.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;


public record DownloadFileChannelPacket(String message) implements CustomPacketPayload {
    /**
     * 发送文件分块到客户端通道(Map)
     */
    public static final Type<DownloadFileChannelPacket> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("chatimage", "download_file_channel"));

    public static final StreamCodec<RegistryFriendlyByteBuf, DownloadFileChannelPacket> CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, DownloadFileChannelPacket::message, DownloadFileChannelPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}