package io.github.kituin.chatimage.legacy;

import java.util.List;
import java.util.function.Function;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;

/** Preserve translation arguments, sibling order, inherited formatting and unrelated events. */
final class ChatMessages {
    private ChatMessages() {}

    static IChatComponent replace(IChatComponent original, LegacyConfig config,
            Function<ImageLink, IChatComponent> imageComponent) {
        IChatComponent result;
        if (original instanceof ChatComponentTranslation) {
            ChatComponentTranslation translation = (ChatComponentTranslation) original;
            Object[] args = translation.getFormatArgs().clone();
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof IChatComponent) {
                    args[i] = replace((IChatComponent) args[i], config, imageComponent);
                } else if (args[i] instanceof String) {
                    List<Object> parts = ImageLink.parse((String) args[i], config.checkImageUri, config.cqCode);
                    if (hasImage(parts)) args[i] = build(parts, imageComponent);
                }
            }
            result = new ChatComponentTranslation(translation.getKey(), args);
        } else if (original instanceof ChatComponentText) {
            String text = ((ChatComponentText) original).getChatComponentText_TextValue();
            result = build(ImageLink.parse(text, config.checkImageUri, config.cqCode), imageComponent);
        } else {
            result = original.createCopy();
            result.getSiblings().clear();
        }
        result.setChatStyle(original.getChatStyle().createShallowCopy());
        for (Object sibling : original.getSiblings()) {
            result.appendSibling(replace((IChatComponent) sibling, config, imageComponent));
        }
        return result;
    }

    private static boolean hasImage(List<Object> parts) {
        for (Object part : parts) if (part instanceof ImageLink) return true;
        return false;
    }

    private static IChatComponent build(List<Object> parts, Function<ImageLink, IChatComponent> imageComponent) {
        IChatComponent result = new ChatComponentText("");
        for (Object part : parts) {
            result.appendSibling(part instanceof ImageLink
                    ? imageComponent.apply((ImageLink) part) : new ChatComponentText((String) part));
        }
        return result;
    }
}
