package io.github.kituin.chatimage.tool;

import io.github.kituin.ChatImageCode.ChatImageCode;
import io.github.kituin.ChatImageCode.exception.InvalidChatImageCodeException;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * @author kitUIN
 */
public class ChatImageStyle {
// IF >= fabric-1.20.3
    public static final com.mojang.serialization.MapCodec<ChatImageCode> MAP_CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(obj -> obj.group(
        com.mojang.serialization.Codec.STRING.fieldOf("url").forGetter(ChatImageCode::getUrl),
        com.mojang.serialization.Codec.BOOL.optionalFieldOf("nsfw", false).forGetter(ChatImageCode::isNsfw)
    ).apply(obj, (url, nsfw) -> new ChatImageCode.Builder().setNsfw(nsfw).setUrlForce(url).build()));
    public static final com.mojang.serialization.Codec<ChatImageCode> CODEC = MAP_CODEC.codec();

// END IF
// IF >= fabric-1.20.5
//     public static final HoverEvent.Action<ChatImageCode> SHOW_IMAGE = new HoverEvent.Action<>(
//         "show_chatimage",
//         true,
//         CODEC,
//         ChatImageStyle::legacySerializer);
//     private static com.mojang.serialization.DataResult<ChatImageCode> legacySerializer(Text text, @org.jetbrains.annotations.Nullable net.minecraft.registry.RegistryOps<?> ops) {
//         try {
//             return com.mojang.serialization.DataResult.success(new ChatImageCode.Builder().fromCode(text.toString()).build());
//         } catch (InvalidChatImageCodeException e) {
//             return com.mojang.serialization.DataResult.error(() -> "Failed to parse ChatImageCode: " + e.getMessage());
//         }
//     }
// ELSE IF >= fabric-1.20.3
//    public static final HoverEvent.Action<ChatImageCode> SHOW_IMAGE = new HoverEvent.Action<>(
//            "show_chatimage",
//            true,
//            CODEC,
//            ChatImageStyle::legacySerializer);
//    private static com.mojang.serialization.DataResult<ChatImageCode> legacySerializer(Text text) {
//        try {
//            return com.mojang.serialization.DataResult.success(new ChatImageCode.Builder().fromCode(text.toString()).build());
//        } catch (InvalidChatImageCodeException e) {
//            return com.mojang.serialization.DataResult.error(() -> "Failed to parse ChatImageCode: " + e.getMessage());
//        }
//    }
// ELSE
//    public static final HoverEvent.Action<ChatImageCode> SHOW_IMAGE =
//        new HoverEvent.Action<>("show_chatimage", true,
//                ChatImageCode::fromJson,
//                ChatImageCode::toJson,
//                ChatImageStyle::fromJson);
// END IF


    /**
     * 文本 悬浮图片样式
     *
     * @param code {@link ChatImageCode}
     * @return 悬浮图片样式
     */
    public static Style getStyleFromCode(ChatImageCode code) {
        return getStyleFromCode(code, ChatFormatting.GREEN);
    }

    /**
     * 文本 悬浮图片样式
     *
     * @param code  {@link ChatImageCode}
     * @param color 颜色
     * @return 悬浮图片样式
     */
    public static Style getStyleFromCode(ChatImageCode code, ChatFormatting color) {
        Style style = Style.EMPTY.withHoverEvent(
                new HoverEvent.ShowText(Component.literal(code.toString()))
        );
        return style.withColor(color);
    }

    /**
     * 获取悬浮图片样式的Text消息
     *
     * @param code {@link ChatImageCode}
     * @return {@link MutableComponent}
     */
    public static MutableComponent messageFromCode(ChatImageCode code) {
        MutableComponent t = code.messageFromCode(
// IF fabric-1.16.5 || fabric-1.18.2
//                LiteralText::new,
//                TranslatableText::new,
//                (obj, s)-> (BaseText) obj.append(s));
// ELSE
                Component::literal,
                Component::translatable,
                MutableComponent::append);
// END IF
        Style style = ChatImageStyle.getStyleFromCode(code);
        return t.withStyle(style);
    }

// IF < fabric-1.20.3
//    public static ChatImageCode fromJson(Text text) {
//        try {
//            return io.github.kituin.ChatImageCode.ChatImageCodeInstance.createBuilder().fromCode(text.toString()).build();
//        } catch (InvalidChatImageCodeException e) {
//            return io.github.kituin.ChatImageCode.ChatImageCodeInstance.createBuilder().build();
//        }
//    }
// END IF



}
