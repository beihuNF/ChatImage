package io.github.kituin.chatimage.legacy;

import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.CommandException;
import net.minecraft.command.WrongUsageException;

final class LegacyCommand extends CommandBase {
    private final ClientProxy client;

    LegacyCommand(ClientProxy client) { this.client = client; }

    @Override
    public String getCommandName() { return "chatimage"; }

    @Override
    public String getCommandUsage(ICommandSender sender) { return "usage.chatimage.legacy"; }

    @Override
    public int getRequiredPermissionLevel() { return 0; }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) { return true; }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0 || "help".equals(args[0])) {
            ClientProxy.message("usage.chatimage.legacy");
            return;
        }
        if ("reload".equals(args[0]) && args.length == 1) {
            try {
                client.reload();
            } catch (java.io.IOException e) {
                throw new CommandException("error.chatimage.legacy", e.getMessage());
            }
            return;
        }
        if ("view".equals(args[0]) && args.length == 2) {
            try {
                client.view(Long.parseLong(args[1]));
            } catch (NumberFormatException e) {
                throw new WrongUsageException("usage.chatimage.legacy");
            }
            return;
        }
        if ("url".equals(args[0]) && args.length == 2) {
            send(args[1], args[1]);
            return;
        }
        if ("send".equals(args[0]) && args.length == 3) {
            if (args[1].contains(",") || args[1].contains("]")) throw new WrongUsageException("usage.chatimage.legacy");
            if (args[2].contains(",") || args[2].contains("]")) throw new CommandException("cicode.chatimage.legacy");
            send("[[CICode,name=" + args[1] + ",url=" + args[2] + "]]", args[2]);
            return;
        }
        if ("preview".equals(args[0]) && args.length == 2 && ImageLink.isNetworkUrl(args[1])) {
            // Print locally: previews longer URLs without hitting the vanilla 100-character packet limit.
            client.preview(args[1]);
            return;
        }
        throw new WrongUsageException("usage.chatimage.legacy");
    }

    private static void send(String message, String url) {
        if (!ImageLink.isNetworkUrl(url)) throw new CommandException("network.chatimage.legacy");
        if (message.length() > 100) throw new CommandException("length.chatimage.legacy");
        if (Minecraft.getMinecraft().thePlayer == null) throw new CommandException("world.chatimage.legacy");
        Minecraft.getMinecraft().thePlayer.sendChatMessage(message);
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args) {
        return args.length == 1 ? getListOfStringsMatchingLastWord(args, "url", "send", "preview", "reload", "help") : null;
    }

}
