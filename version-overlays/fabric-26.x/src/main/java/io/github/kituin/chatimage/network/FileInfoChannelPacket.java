// ONLY >= fabric-1.20.5
package io.github.kituin.chatimage.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;


public record FileInfoChannelPacket(String message) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<FileInfoChannelPacket> ID =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("chatimage", "file_info"));
    public static final StreamCodec<RegistryFriendlyByteBuf, FileInfoChannelPacket> CODEC =
            StreamCodec.composite(ByteBufCodecs.STRING_UTF8, FileInfoChannelPacket::message, FileInfoChannelPacket::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return ID;
    }
}