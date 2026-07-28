package io.github.kituin.chatimage.mixin;

import com.google.common.collect.Lists;
import io.github.kituin.chatimage.tool.ChatImageStyle;
import io.github.kituin.ChatImageCode.ChatImageBoolean;
import io.github.kituin.ChatImageCode.ChatImageCode;
import io.github.kituin.ChatImageCode.ChatImageCodeTool;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import net.minecraft.network.chat.HoverEvent;

import static io.github.kituin.ChatImageCode.ChatImageCode.pattern;
import static io.github.kituin.ChatImageCode.ChatImageCodeInstance.LOGGER;
import static io.github.kituin.ChatImageCode.ChatImageCodeInstance.createBuilder;

import static io.github.kituin.chatimage.tool.SimpleUtil.*;

/**
 * 注入修改文本显示,自动将CICode转换为可鼠标悬浮格式文字
 *
 * @author kitUIN
 */
@Mixin(net.minecraft.client.gui.components.ChatComponent.class)
public class ChatComponentMixin {
    @Shadow
    @Final
    private net.minecraft.client.Minecraft minecraft;

    @ModifyVariable(at = @At("HEAD"),
            method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V",
            argsOnly = true)
    public net.minecraft.network.chat.Component addMessage(net.minecraft.network.chat.Component message) {
        if (io.github.kituin.chatimage.client.ChatImageClient.CONFIG.experimentalTextComponentCompatibility) {
            try {
                StringBuilder sb = new StringBuilder();
                net.minecraft.network.chat.Component temp = chatImage$flattenTree(message, sb, false);
                ChatImageBoolean allString = new ChatImageBoolean(true);
                ChatImageCodeTool.sliceMsg(sb.toString(), true, allString, (e) -> LOGGER.error("slice msg error: {}", e));
                if (!allString.isValue()) message = temp;
            } catch (Exception e) {
                LOGGER.warn("experimentalTextComponentCompatibility 转换失败:{}", e.getMessage());
            }
        }
        return chatimage$replaceMessage(message);
    }

    // IF >= fabric-1.19
    @Unique
    private net.minecraft.network.chat.ComponentContents chatImage$getContents(net.minecraft.network.chat.Component text) {
        return text.getContents();
    }
// ELSE IF >= forge-1.19 || > neoforge-1.20.1
//    @Unique
//    private net.minecraft.text.TextContents chatImage$getContents(net.minecraft.text.Text text) {
//        return text.getContents();
//    }
// ELSE
//    @Unique
//    private net.minecraft.text.Text chatImage$getContents(net.minecraft.text.Text text) {
//        return text;
//    }
// END IF

    @Unique
    private String chatImage$getText(
// IF >= fabric-1.19
            net.minecraft.network.chat.ComponentContents text
// ELSE IF >= forge-1.19 || > neoforge-1.20.1
//            net.minecraft.text.TextContents text
// ELSE
//            net.minecraft.text.Text text
// END IF
    ) {
        if (text instanceof net.minecraft.network.chat.contents.PlainTextContents)
// IF >= fabric-1.19
            return ((net.minecraft.network.chat.contents.PlainTextContents) text).text();
// ELSE IF >= forge-1.19 || > neoforge-1.20.1
//            return ((net.minecraft.text.PlainTextContent) text).text();
// ELSE IF < fabric-1.19
//            return text.asString();
// ELSE
//            return ((net.minecraft.text.PlainTextContent) text).getContents();
// END IF
        return "";
    }

    @SuppressWarnings("t")
    @Unique
    private net.minecraft.network.chat.Component chatimage$replaceCode(net.minecraft.network.chat.Component text) {
        String checkedText;
        String key = "";
        net.minecraft.network.chat.MutableComponent player = null;
        boolean isSelf = false;
        net.minecraft.network.chat.MutableComponent originText = text.copy();
        originText.getSiblings().clear();
        net.minecraft.network.chat.Style style = text.getStyle();
        if (chatImage$getContents(text) instanceof net.minecraft.network.chat.contents.PlainTextContents) {
            checkedText = chatImage$getText((net.minecraft.network.chat.contents.PlainTextContents) chatImage$getContents(text));
        } else if (chatImage$getContents(text) instanceof net.minecraft.network.chat.contents.TranslatableContents) {
            net.minecraft.network.chat.contents.TranslatableContents ttc = (net.minecraft.network.chat.contents.TranslatableContents) chatImage$getContents(text);
            key = ttc.getKey();
            Object[] args = ttc.getArgs();
            if (ChatImageCodeTool.checkKey(key)) {
                player = (net.minecraft.network.chat.MutableComponent) args[0];
// IF >= fabric-1.16.5
                isSelf = chatImage$getContents(player).toString().equals(chatImage$getContents(minecraft.player.getName()).toString());
// ELSE IF >= forge-1.16.5 || > neoforge-1.20.1
//                isSelf = chatImage$getContents(player).toString().equals(chatImage$getContents(minecraft.player.getName()).toString());
// END IF
                if (args[1] instanceof String) {
                    checkedText = (String) args[1];
                } else {
                    net.minecraft.network.chat.MutableComponent contents = (net.minecraft.network.chat.MutableComponent) args[1];
                    if (chatImage$getContents(contents) instanceof net.minecraft.network.chat.contents.PlainTextContents) {
                        checkedText = chatImage$getText((net.minecraft.network.chat.contents.PlainTextContents) chatImage$getContents(contents));
                    } else {
                        checkedText = chatImage$getContents(contents).toString();
                    }
                }
            } else {
                List<net.minecraft.network.chat.Component> argTexts = Lists.newArrayList();
                for (Object arg : args) {
                    argTexts.add(this.chatimage$replaceMessage((net.minecraft.network.chat.Component) arg));
                }
// IF >= fabric-1.19.4 || >= neoforge-1.20.2
                String fallback = ttc.getFallback();
                if (fallback != null) {
                    return net.minecraft.network.chat.Component.translatableWithFallback(key, fallback, argTexts.toArray()).setStyle(style);
                }
// END IF
                return createTranslatableComponent(key, argTexts.toArray()).setStyle(style);
            }
        } else {
            checkedText = chatImage$getContents(text).toString();
        }

        // 尝试解析CQ码
        if (io.github.kituin.chatimage.client.ChatImageClient.CONFIG.cqCode)
            checkedText = ChatImageCodeTool.checkCQCode(checkedText);

        // 是否全是文本
        ChatImageBoolean allString = new ChatImageBoolean(true);

        // 尝试解析CICode
        List<Object> texts = ChatImageCodeTool.sliceMsg(checkedText, isSelf, allString, (e) -> LOGGER.error("slice msg error: {}", e));
        // 尝试解析URL
        if (io.github.kituin.chatimage.client.ChatImageClient.CONFIG.checkImageUri)
            ChatImageCodeTool.checkImageUri(texts, isSelf, allString);

        // 无识别则返回原样
        if (allString.isValue()) {
// IF >= fabric-1.21.5 || >= neoforge-1.21.5
            if (style.getHoverEvent() instanceof HoverEvent.ShowText(net.minecraft.network.chat.Component encoded)) {
                try {
                    createBuilder().fromCode(encoded.getString()).build().retry();
                } catch (Exception ignored) {
                }
            }
// ELSE
//              if (style.getHoverEvent() != null && style.getHoverEvent().getValue(ChatImageStyle.SHOW_IMAGE) !=null) style.getHoverEvent().getValue(ChatImageStyle.SHOW_IMAGE).retry();
// END IF
            try {
// IF >= fabric-1.21.5 || >= neoforge-1.21.5
                if (style.getHoverEvent() != null &&
                        style.getHoverEvent() instanceof HoverEvent.ShowText(net.minecraft.network.chat.Component showText) &&
                        chatImage$getContents(showText) instanceof net.minecraft.network.chat.contents.PlainTextContents) {
// ELSE
//              net.minecraft.text.Text showText = style.getHoverEvent() == null ? null : style.getHoverEvent().getValue(net.minecraft.text.HoverEvent.Action.SHOW_TEXT);
//              if (showText != null &&
//                      chatImage$getContents(showText) instanceof net.minecraft.text.PlainTextContent) {
// END IF

                    String originalCode = chatImage$getText((net.minecraft.network.chat.contents.PlainTextContents) chatImage$getContents(showText));
                    Matcher matcher = pattern.matcher(originalCode);
                    if (matcher.find()) {
                        originText.setStyle(
                                style.withHoverEvent(
// IF >= fabric-1.21.5 || >= neoforge-1.21.5
                                        new HoverEvent.ShowText(net.minecraft.network.chat.Component.literal(
// ELSE
//              new net.minecraft.text.HoverEvent(
//                                     ChatImageStyle.SHOW_IMAGE,
//
// END IF
                                                createBuilder()
                                                        .fromCode(originalCode)
                                                        .setIsSelf(isSelf)
                                                        .build().toString()))));
                    }
                }
            } catch (Exception e) {
                LOGGER.error("返回原样失败:{}", e);
            }
            return originText;
        }
        net.minecraft.network.chat.MutableComponent res = createLiteralComponent("");
        ChatImageCodeTool.buildMsg(texts,
                (obj) -> res.append(createLiteralComponent(obj).setStyle(style)),
                (obj) -> res.append(ChatImageStyle.messageFromCode(obj))
        );
        return player == null ? res : createTranslatableComponent(key, player, res).setStyle(style);
    }

    @SuppressWarnings("t")
    @Unique
    private net.minecraft.network.chat.Component chatImage$flattenTree(Object originNode, StringBuilder mergedText, boolean openUrlStyle) {
        if (originNode instanceof String) {
            return createLiteralComponent((String) originNode);
        } else if (originNode instanceof Integer) {
            return createLiteralComponent(originNode.toString());
        }
        net.minecraft.network.chat.Component node = (net.minecraft.network.chat.Component) originNode;
        net.minecraft.network.chat.Style tempStyle = node.getStyle();
        if (chatImage$getContents(node) instanceof net.minecraft.network.chat.contents.TranslatableContents) {
            net.minecraft.network.chat.contents.TranslatableContents ttc = (net.minecraft.network.chat.contents.TranslatableContents) chatImage$getContents(node);
            Object[] args = ttc.getArgs();
            List<Object> argsNew = Lists.newArrayList();
            for (Object arg : args) {
                argsNew.add(chatImage$flattenTree(arg, mergedText, false));
            }
// IF >= fabric-1.19.4 || >= neoforge-1.20.2
            String fallback = ttc.getFallback();
            if (fallback != null) {
                return net.minecraft.network.chat.Component.translatableWithFallback(ttc.getKey(), fallback, argsNew.toArray()).setStyle(tempStyle);
            }
// END IF
            return createTranslatableComponent(ttc.getKey(), argsNew.toArray()).setStyle(tempStyle);
        } else {
            String t = chatImage$getText(chatImage$getContents(node));
            mergedText.append(t);
            // 没有子就返回本身
            if (node.getSiblings().isEmpty()) return node;
            // 有子则构建
            net.minecraft.network.chat.MutableComponent res = null;
            List<net.minecraft.network.chat.Component> children = Lists.newArrayList();
            StringBuilder childSb = new StringBuilder(t);
            for (int i = 0; i < node.getSiblings().size(); i++) {
                net.minecraft.network.chat.Component child_ = node.getSiblings().get(i);
                net.minecraft.network.chat.Component child = chatImage$flattenTree(child_, mergedText, (child_.getStyle().getClickEvent() != null &&
// IF >= neoforge-1.21.5
//                        child_.getStyle().getClickEvent().action() == net.minecraft.text.ClickEvent.Action.OPEN_URL));
// ELSE
                        child_.getStyle().getClickEvent().action() == net.minecraft.network.chat.ClickEvent.Action.OPEN_URL));
// END IF
                if (child == null) continue;
                net.minecraft.network.chat.Style childStyle = child.getStyle();
                if (tempStyle == null) tempStyle = childStyle;
                boolean isLiteral = chatImage$getContents(child) instanceof net.minecraft.network.chat.contents.PlainTextContents && !Objects.equals(chatImage$getText(chatImage$getContents(child)), "");
                boolean check = isLiteral &&
                        (chatImage$isSame(childStyle, tempStyle) || openUrlStyle || (childStyle.getClickEvent() != null &&
// IF >= neoforge-1.21.5
//                childStyle.getClickEvent().action() == net.minecraft.text.ClickEvent.Action.OPEN_URL));
// ELSE
                                childStyle.getClickEvent().action() == net.minecraft.network.chat.ClickEvent.Action.OPEN_URL));
// END IF
                if (check) {
                    childSb.append(chatImage$getText(chatImage$getContents(child)));
                    // 检查成功并且没有子且不是最后一个直接跳过
                    if (child.getSiblings().isEmpty() && i != node.getSiblings().size() - 1) continue;
                }
                net.minecraft.network.chat.MutableComponent tempText = createLiteralComponent(childSb.toString()).setStyle(tempStyle);
                // 如果父级没创建就先创建父级
                if (res == null) res = tempText;
                    // 有父级则加在子里
                else children.add(tempText);
                childSb = new StringBuilder();
                tempStyle = null;
                // 没识别到直接添加
                if (!check) children.add(child);
            }
            for (net.minecraft.network.chat.Component child : children) {
                res.append(child);
            }
            return res;
        }
    }

    @Unique
    private boolean chatImage$isSame(net.minecraft.network.chat.Style childStyle, net.minecraft.network.chat.Style tempStyle) {
        if (childStyle == null || tempStyle == null) return false;
        return childStyle.isBold() == tempStyle.isBold() &&
                Objects.equals(childStyle.getColor(), tempStyle.getColor()) &&
                childStyle.isItalic() == tempStyle.isItalic() &&
                childStyle.isObfuscated() == tempStyle.isObfuscated() &&
                childStyle.isStrikethrough() == tempStyle.isStrikethrough() &&
                childStyle.isUnderlined() == tempStyle.isUnderlined() &&
                Objects.equals(childStyle.getClickEvent(), tempStyle.getClickEvent()) &&
                Objects.equals(childStyle.getHoverEvent(), tempStyle.getHoverEvent()) &&
                Objects.equals(childStyle.getInsertion(), tempStyle.getInsertion()) &&
                Objects.equals(childStyle.getFont(), tempStyle.getFont());
    }

    @Unique
    private net.minecraft.network.chat.Component chatimage$replaceMessage(net.minecraft.network.chat.Component message) {
        try {
            net.minecraft.network.chat.MutableComponent res = (net.minecraft.network.chat.MutableComponent) chatimage$replaceCode(message);
            for (net.minecraft.network.chat.Component t : message.getSiblings()) {
                res.append(chatimage$replaceMessage(t));
            }
            return res;
        } catch (Exception e) {
            LOGGER.warn("识别失败:{}", e.getMessage());
            return message;
        }
    }
}
