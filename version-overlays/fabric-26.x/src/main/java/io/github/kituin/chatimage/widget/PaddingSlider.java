package io.github.kituin.chatimage.widget;

import io.github.kituin.ChatImageCode.ChatImageConfig;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;

import static io.github.kituin.chatimage.client.ChatImageClient.CONFIG;
import static io.github.kituin.chatimage.tool.SimpleUtil.composeGenericOptionText;
import static io.github.kituin.chatimage.tool.SimpleUtil.createLiteralComponent;


public class PaddingSlider extends SettingSliderWidget {
    protected final Component title;
    protected final PaddingType paddingType;


// IF >= fabric-1.19.3
    public PaddingSlider(Component title, int value, float min, float max, PaddingType paddingType) {
        super(100, 100, 150, 20, value, min, max);
        this.title = title;
        this.paddingType = paddingType;
        this.updateMessage();
        this.tooltip();
}

// ELSE
//    public PaddingSlider(int x, int y, int width, int height, Text title, int value, float max, PaddingType paddingType, TooltipSupplier tooltipSupplier) {
//        super(x, y, width, height, value, 0F, max, tooltipSupplier);
//        this.title = title;
//        this.paddingType = paddingType;
//        this.updateMessage();
//    }
// END IF
    @Override
    protected void updateMessage() {
        this.setMessage(composeGenericOptionText(title, createLiteralComponent(String.valueOf(this.position))));
        switch (paddingType) {
            case TOP:
                CONFIG.paddingTop = this.position;
                break;
            case BOTTOM:
                CONFIG.paddingBottom = this.position;
                break;
            case LEFT:
                CONFIG.paddingLeft = this.position;
                break;
            case RIGHT:
                CONFIG.paddingRight = this.position;
                break;
            default:
                return;
        }
        ChatImageConfig.saveConfig(CONFIG);
    }
// IF >= fabric-1.19.3
    private void tooltip() {
        Component text;
        switch (paddingType) {
            case TOP:
                text = Component.translatable("top.padding.chatimage.tooltip");
                break;
            case BOTTOM:
                text = Component.translatable("bottom.padding.chatimage.tooltip");
                break;
            case LEFT:
                text = Component.translatable("left.padding.chatimage.tooltip");
                break;
            case RIGHT:
                text = Component.translatable("right.padding.chatimage.tooltip");
                break;
            default:
                return;
        }
        this.tip = Tooltip.create(text);
        this.setTooltip(this.tip);
    }
// END IF
    public enum PaddingType {
        LEFT, RIGHT, TOP, BOTTOM

    }
}