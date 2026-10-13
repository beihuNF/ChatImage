package io.github.kituin.chatimage.legacy;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import java.io.File;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = "chatimage", name = "ChatImage", version = Tags.VERSION,
        acceptedMinecraftVersions = "[1.7.10]", acceptableRemoteVersions = "*")
public final class ChatImageLegacy {
    public static final Logger LOGGER = LogManager.getLogger("ChatImage");
    @SidedProxy(clientSide = "io.github.kituin.chatimage.legacy.ClientProxy",
            serverSide = "io.github.kituin.chatimage.legacy.ChatImageLegacy$CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(new File(event.getModConfigurationDirectory(), "chatimage-1.7.10.json"));
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init();
    }

    public static class CommonProxy {
        public void preInit(File file) {}
        public void init() {}
    }
}
