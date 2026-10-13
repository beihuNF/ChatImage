package io.github.kituin.chatimage.legacy;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.event.ClickEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import org.junit.Test;
import static org.junit.Assert.*;

public class ChatMessagesTest {
    @Test
    public void preservesInheritedFormattingAndUnrelatedLinksWithoutMutatingOriginal() {
        IChatComponent original = new ChatComponentText("prefix ").setChatStyle(
                new ChatStyle().setColor(EnumChatFormatting.RED).setItalic(true));
        original.appendSibling(new ChatComponentText("https://example.org/a.png"));
        original.appendSibling(new ChatComponentText(" tail").setChatStyle(new ChatStyle().setChatClickEvent(
                new ClickEvent(ClickEvent.Action.OPEN_URL, "https://example.org/"))));
        List<ImageLink> images = new ArrayList<>();
        IChatComponent converted = ChatMessages.replace(original, new LegacyConfig(), link -> {
            images.add(link);
            return new ChatComponentText("[image]").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GREEN));
        });
        assertEquals("prefix [image] tail", converted.getUnformattedText());
        assertEquals("prefix https://example.org/a.png tail", original.getUnformattedText());
        assertEquals(EnumChatFormatting.RED, converted.getChatStyle().getColor());
        assertTrue(converted.getChatStyle().getItalic());
        IChatComponent tail = (IChatComponent) converted.getSiblings().get(converted.getSiblings().size() - 1);
        assertEquals(ClickEvent.Action.OPEN_URL, tail.getChatStyle().getChatClickEvent().getAction());
        assertEquals(1, images.size());
    }

    @Test
    public void preservesTranslationKeyAndNonTextArguments() {
        ChatComponentTranslation original = new ChatComponentTranslation("test.translation", "player",
                "https://example.org/a.gif", 42);
        ChatComponentTranslation converted = (ChatComponentTranslation) ChatMessages.replace(
                original, new LegacyConfig(), link -> new ChatComponentText("[gif]"));
        assertEquals("test.translation", converted.getKey());
        assertEquals("player", converted.getFormatArgs()[0]);
        assertEquals("[gif]", ((IChatComponent) converted.getFormatArgs()[1]).getUnformattedText());
        assertEquals(42, converted.getFormatArgs()[2]);
        assertEquals("https://example.org/a.gif", original.getFormatArgs()[1]);
    }
}
