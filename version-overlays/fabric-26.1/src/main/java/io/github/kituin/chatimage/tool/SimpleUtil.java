package io.github.kituin.chatimage.tool;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class SimpleUtil {

    public static void setScreen(Minecraft client, Screen screen) {
// IF fabric-1.16.5
//        client.openScreen(screen);
// ELSE
        client.setScreen(screen);
// END IF
    }
    public static MutableComponent createTranslatableComponent(String text){
// IF fabric-1.16.5 || fabric-1.18.2
//        return new TranslatableText(text);
// ELSE
        return Component.translatable(text);
// END IF
    }
    public static MutableComponent createTranslatableComponent(String key, Object... args){
// IF fabric-1.16.5 || fabric-1.18.2
//        return new TranslatableText(key, args);
// ELSE
        return Component.translatable(key, args);
// END IF
    }


    public static MutableComponent createLiteralComponent(String text){
// IF fabric-1.16.5 || fabric-1.18.2
//        return new LiteralText(text);
// ELSE
        return Component.literal(text);
// END IF
    }
    public static MutableComponent composeGenericOptionText(Component text, Component value) {
// IF fabric-1.16.5
//        return new TranslatableText("options.generic_value", text, value);
// ELSE IF fabric-1.18.2
//        return net.minecraft.client.gui.screen.ScreenTexts.composeGenericOptionText(text,value);
// ELSE
        return net.minecraft.network.chat.CommonComponents.optionNameValue(text,value);
// END IF
    }

}
