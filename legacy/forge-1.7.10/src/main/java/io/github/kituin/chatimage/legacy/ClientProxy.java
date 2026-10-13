package io.github.kituin.chatimage.legacy;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.event.ClickEvent;
import net.minecraft.event.HoverEvent;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Mouse;

public final class ClientProxy extends ChatImageLegacy.CommonProxy {
    static final String VIEW_COMMAND = "/chatimage view ";
    private final Map<Long, ImageLink> links = new LinkedHashMap<>();
    private final Set<Long> approved = new HashSet<>();
    private final PreviewCache cache = new PreviewCache();
    private File configFile;
    private LegacyConfig config = new LegacyConfig();
    private long nextId;
    private volatile boolean clearPending;

    @Override
    public void preInit(File file) {
        configFile = file;
        try {
            config = LegacyConfig.load(file);
        } catch (IOException e) {
            ChatImageLegacy.LOGGER.error("Unable to read ChatImage config; using defaults", e);
        }
    }

    @Override
    public void init() {
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance().bus().register(this);
        ClientCommandHandler.instance.registerCommand(new LegacyCommand(this));
        ChatImageLegacy.LOGGER.info("ChatImage Forge 1.7.10 client initialized");
    }

    @SubscribeEvent
    public void onChat(ClientChatReceivedEvent event) {
        try {
            event.message = ChatMessages.replace(event.message, config, this::component);
        } catch (RuntimeException e) {
            ChatImageLegacy.LOGGER.warn("Unable to convert image message", e);
        }
    }

    private IChatComponent component(ImageLink link) {
        long id = ++nextId;
        links.put(id, link);
        if (links.size() > 512) {
            Iterator<Long> iterator = links.keySet().iterator();
            approved.remove(iterator.next());
            iterator.remove();
        }
        IChatComponent component = new ChatComponentText(link.prefix);
        component.appendSibling(link.name == null
                ? new ChatComponentTranslation("codename.chatimage.default") : new ChatComponentText(link.name));
        component.appendText(link.suffix);
        // Ordinary 1.7.10 click events survive vanilla chat wrapping; no custom HoverEvent enum or mixins.
        component.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.GREEN)
                .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, VIEW_COMMAND + id))
                .setChatHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ChatComponentText(""))));
        return component;
    }

    @SubscribeEvent
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Post event) {
        if (!(event.gui instanceof GuiChat)) return;
        Minecraft mc = Minecraft.getMinecraft();
        IChatComponent component = mc.ingameGUI.getChatGUI().func_146236_a(Mouse.getX(), Mouse.getY());
        if (component == null) return;
        ClickEvent click = component.getChatStyle().getChatClickEvent();
        if (click == null || click.getAction() != ClickEvent.Action.RUN_COMMAND
                || !click.getValue().startsWith(VIEW_COMMAND)) return;
        try {
            long id = Long.parseLong(click.getValue().substring(VIEW_COMMAND.length()));
            ImageLink link = links.get(id);
            if (link == null) return;
            if (link.nsfw && !config.nsfw && !approved.contains(id)) {
                ImageTooltip.text(event.gui, event.mouseX, event.mouseY,
                        new ChatComponentTranslation("nsfw.chatimage.legacy").getUnformattedText());
                return;
            }
            PreviewCache.Entry entry = cache.get(link.url, config);
            ImageTooltip.draw(event.gui, event.mouseX, event.mouseY, entry, config);
        } catch (NumberFormatException ignored) {
            // Unrelated or invalid server-provided command.
        }
    }

    void view(long id) {
        ImageLink link = links.get(id);
        if (link == null) {
            message("expired.chatimage.legacy");
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        GuiScreen previous = mc.currentScreen;
        if (link.nsfw && !config.nsfw && !approved.contains(id)) {
            mc.displayGuiScreen(new GuiYesNo((accepted, button) -> {
                if (accepted) approved.add(id);
                mc.displayGuiScreen(previous);
            }, new ChatComponentTranslation("confirm.chatimage.legacy").getUnformattedText(),
                    link.name == null ? "Image" : link.name, 0));
        } else {
            PreviewCache.Entry entry = cache.get(link.url, config);
            // Retry failed downloads after an explicit click.
            if (entry.error != null) {
                cache.remove(link.url);
                cache.get(link.url, config);
            }
        }
    }

    void reload() throws IOException {
        LegacyConfig loaded = LegacyConfig.load(configFile);
        cache.clear();
        config = loaded;
        message("reload.chatimage.legacy");
    }

    void preview(String url) {
        Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(component(ImageLink.network(url)));
    }

    static void message(String key, Object... args) {
        Minecraft.getMinecraft().ingameGUI.getChatGUI().printChatMessage(new ChatComponentTranslation(key, args));
    }

    @SubscribeEvent
    public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        // Network events can fire off-thread; deletion of GL textures waits for the client tick.
        clearPending = true;
    }

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && clearPending) {
            clearPending = false;
            cache.clear();
            links.clear();
            approved.clear();
        }
    }
}
