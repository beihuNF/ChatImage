package io.github.kituin.chatimage.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import io.github.kituin.ChatImageCode.ChatImageCode;
import io.github.kituin.ChatImageCode.ChatImageCodeInstance;
import io.github.kituin.ChatImageCode.ChatImageConfig;
// IF fabric-1.16.5 || fabric-1.18.2
//import net.fabricmc.fabric.api.client.command.v1.FabricClientCommandSource;
// ELSE
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
// END IF

import static io.github.kituin.ChatImageCode.ChatImageCodeInstance.LOGGER;
import static io.github.kituin.chatimage.client.ChatImageClient.CONFIG;
import static io.github.kituin.chatimage.tool.SimpleUtil.createTranslatableComponent;

public class ChatImageCommand {
    public static int sendChatImage(CommandContext<FabricClientCommandSource> context) {
        String url = StringArgumentType.getString(context, "url");
        ChatImageCode.Builder builder = ChatImageCodeInstance.createBuilder().setUrlForce(url);
        try {
            String name = StringArgumentType.getString(context, "name");
            builder.setName(name);
        } catch (java.lang.IllegalArgumentException e) {
            LOGGER.info("arg: `name` is omitted, use the default string");
        }
// IF fabric-1.16.5 || fabric-1.18.2
//        context.getSource().getPlayer().sendChatMessage(builder.build().toString());
// ELSE IF fabric-1.19.1 || fabric-1.19.2
//        context.getSource().getPlayer().sendChatMessage(builder.build().toString(), null);
// ELSE
        context.getSource().getPlayer().connection.sendChat(builder.build().toString());
// END IF
        return Command.SINGLE_SUCCESS;
    }

    public static int help(CommandContext<FabricClientCommandSource> context) {

        context.getSource().sendFeedback(
                getHelpText("/chatimage help", "", "help.chatimage.command")
                        .append(getHelpText("/chatimage send ", "<name> <url>", "send.chatimage.command"))
                        .append(getHelpText("/chatimage url ", "<url>", "url.chatimage.command"))
                        .append(getHelpText("/chatimage reload ", "", "reload.chatimage.command"))
        );
        return Command.SINGLE_SUCCESS;
    }

    public static int reloadConfig(CommandContext<FabricClientCommandSource> context) {
        CONFIG = ChatImageConfig.loadConfig();
        context.getSource().sendFeedback(createTranslatableComponent("success.reload.chatimage.command").setStyle(Style.EMPTY.withColor(ChatFormatting.GREEN)));
        return Command.SINGLE_SUCCESS;
    }

    private static MutableComponent getHelpText(String help, String arg, String usage) {
        String all = help + arg;
        StringBuilder sb = new StringBuilder(all);
        if (all.length() <= 35) {
            for (int i = 0; i < 35 - all.length(); i++) {
                sb.append(" ");
            }
        }
        MutableComponent text = (MutableComponent) Component.nullToEmpty(sb.toString());
// IF fabric-1.16.5 || fabric-1.18.2
//        MutableText info = new TranslatableText(usage);
// ELSE
        MutableComponent info = Component.translatable(usage);
// END IF
        return text.setStyle(Style.EMPTY.withColor(ChatFormatting.GOLD).withClickEvent(
// IF >= fabric-1.21.5
                new ClickEvent.SuggestCommand(help)
// ELSE
//                 new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, help)
// END IF
        )).append(info).append(Component.nullToEmpty("\n"));
    }

}
