package io.github.kituin.chatimage.widget;

import io.github.kituin.ChatImageCode.ChatImageConfig;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import static io.github.kituin.chatimage.client.ChatImageClient.CONFIG;
import static io.github.kituin.chatimage.tool.SimpleUtil.*;

public class LimitSlider extends SettingSliderWidget {
    protected final Component title;
    protected final LimitType limitType;
// IF >= fabric-1.19.3
    public LimitSlider(Component title, int value, float min, float max, LimitType limitType) {
        super(100, 100, 150, 20, value, min, max);
        this.title = title;
        this.limitType = limitType;
        this.updateMessage();
        this.tooltip();
    }
// ELSE
//    public LimitSlider(int x, int y, int width, int height, Text title, int value, float max, LimitType limitType, TooltipSupplier tooltipSupplier) {
//        super(x, y, width, height, value, 1F, max, tooltipSupplier);
//        this.title = title;
//        this.limitType = limitType;
//        this.updateMessage();
//    }
// END IF
    @Override
    protected void updateMessage() {
        switch (limitType) {
            case WIDTH:
                this.setMessage(composeGenericOptionText(title, CONFIG.limitWidth == 0 ? createTranslatableComponent("default.chatimage.gui") : createLiteralComponent(String.valueOf(this.position))));
                CONFIG.limitWidth = this.position;
                break;
            case HEIGHT:
                this.setMessage(composeGenericOptionText(title, CONFIG.limitHeight == 0 ? createTranslatableComponent("default.chatimage.gui") : createLiteralComponent(String.valueOf(this.position))));
                CONFIG.limitHeight = this.position;
                break;
            default:
                return;
        }
        ChatImageConfig.saveConfig(CONFIG);
    }
// IF >= fabric-1.19.3
    private void tooltip() {
        Component text;
        switch (limitType) {
            case WIDTH:
                text = Component.translatable("width.limit.chatimage.tooltip");
                break;
            case HEIGHT:
                text = Component.translatable("height.limit.chatimage.tooltip");
                break;
            default:
                return;
        }
        this.tip = Tooltip.create(text);
        this.setTooltip(this.tip);
    }
// END IF
    public enum LimitType {
        WIDTH, HEIGHT

    }
}