// ONLY >= fabric-1.20.5
package io.github.kituin.chatimage.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;


public record FileChannelPacket(String message) implements CustomPacketPayload {
    /**
     * 客户端发送文件分块到服务器通道(Map)
     */
    public static final CustomPacketPayload.Type<FileChannelPacket> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("chatimage", "get_file_channel"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FileChannelPacket> CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, FileChannelPacket::message, FileChannelPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}