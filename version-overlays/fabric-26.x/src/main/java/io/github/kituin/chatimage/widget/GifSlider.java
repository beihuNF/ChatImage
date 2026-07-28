package io.github.kituin.chatimage.widget;

import io.github.kituin.ChatImageCode.ChatImageConfig;
import io.github.kituin.chatimage.client.ChatImageClient;
import net.minecraft.client.gui.components.Tooltip;

// END IF
import static io.github.kituin.chatimage.client.ChatImageClient.CONFIG;
import static io.github.kituin.chatimage.tool.SimpleUtil.*;
/**
 * @author kitUIN
 */
public class GifSlider extends SettingSliderWidget {
// IF >= fabric-1.19.3
    public GifSlider() {
        super(100, 100, 150, 20, CONFIG.gifSpeed, 1, 20);
        this.updateMessage();
        this.tip = Tooltip.create(createTranslatableComponent("gif.chatimage.tooltip"));
        this.setTooltip(this.tip);
    }
// ELSE
//    public GifSlider(int x, int y, int width, int height, TooltipSupplier tooltipSupplier) {
//        super(x, y, width, height, ChatImageClient.CONFIG.gifSpeed, 1, 20, tooltipSupplier);
//        this.updateMessage();
//    }
// END IF
    @Override
    protected void updateMessage() {
        this.setMessage(composeGenericOptionText(createTranslatableComponent("gif.chatimage.gui"), createLiteralComponent(String.valueOf(this.position))));
        ChatImageClient.CONFIG.gifSpeed = this.position;
        ChatImageConfig.saveConfig(ChatImageClient.CONFIG);
    }

}