package io.github.kituin.chatimage.gui;

import io.github.kituin.chatimage.widget.LimitSlider;
import io.github.kituin.chatimage.widget.PaddingSlider;
import io.github.kituin.chatimage.client.ChatImageClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;

import static io.github.kituin.chatimage.tool.SimpleUtil.createTranslatableComponent;
import static io.github.kituin.chatimage.tool.SimpleUtil.setScreen;
import static io.github.kituin.chatimage.widget.PaddingSlider.PaddingType.*;

@Environment(EnvType.CLIENT)
// IF < fabric-1.19.3
//public class LimitPaddingScreen extends ConfigRawScreen {
//    public LimitPaddingScreen(Screen screen) {
//        super(createTranslatableComponent("padding.chatimage.gui"), screen);
//    }
// ELSE
public class LimitPaddingScreen extends Screen {
    private final Screen parent;
    public LimitPaddingScreen(Screen screen) {
        super(createTranslatableComponent("padding.chatimage.gui"));
        this.parent = screen;
    }
// END IF

    protected void init() {
        super.init();
// IF < fabric-1.19.3
//        addDrawableWeight(new PaddingSlider(
//                this.width / 2 - 154, this.height / 4 + 24 + -16, 150, 20,
//                createTranslatableComponent("left.padding.chatimage.gui"), ChatImageClient.CONFIG.paddingLeft,
//                (float) this.width / 2, LEFT, getSliderTooltip(getPaddingTooltipText(LEFT))));
//        addDrawableWeight(new PaddingSlider(
//                this.width / 2 + 4, this.height / 4 + 24 + -16, 150, 20,
//                createTranslatableComponent("right.padding.chatimage.gui"), ChatImageClient.CONFIG.paddingRight,
//                (float) this.width / 2, RIGHT, getSliderTooltip(getPaddingTooltipText(RIGHT))));
//        addDrawableWeight(new PaddingSlider(
//                this.width / 2 - 154, this.height / 4 + 48 + -16, 150, 20,
//                createTranslatableComponent("top.padding.chatimage.gui"), ChatImageClient.CONFIG.paddingTop,
//                (float) this.height / 2, TOP, getSliderTooltip(getPaddingTooltipText(TOP))));
//        addDrawableWeight(new PaddingSlider(
//                this.width / 2 + 4, this.height / 4 + 48 + -16, 150, 20,
//                createTranslatableComponent("bottom.padding.chatimage.gui"), ChatImageClient.CONFIG.paddingBottom,
//                (float) this.height / 2, BOTTOM, getSliderTooltip(getPaddingTooltipText(BOTTOM))));
//        addDrawableWeight(new LimitSlider(
//                this.width / 2 - 154, this.height / 4 + 72 + -16, 150, 20,
//                createTranslatableComponent("width.limit.chatimage.gui"), ChatImageClient.CONFIG.limitWidth,
//                this.width, LimitSlider.LimitType.WIDTH, getSliderTooltip(getLimitTooltipText(LimitSlider.LimitType.WIDTH))));
//        addDrawableWeight(new LimitSlider(
//                this.width / 2 + 4, this.height / 4 + 72 + -16, 150, 20,
//                createTranslatableComponent("height.limit.chatimage.gui"), ChatImageClient.CONFIG.limitHeight,
//                this.height, LimitSlider.LimitType.HEIGHT, getSliderTooltip(getLimitTooltipText(LimitSlider.LimitType.HEIGHT))));
//        addDrawableWeight(new ButtonWidget(
//                this.width / 2 - 77, this.height / 4 + 96 + -16, 150, 20,
//                createTranslatableComponent("gui.back"), (button) -> {
//            if (this.client != null) {
//                setScreen(this.client, this.parent);
//            }
//        }));
// ELSE
        GridLayout gridWidget = new GridLayout();
        gridWidget.defaultCellSetting().paddingHorizontal(5).paddingBottom(4).alignHorizontallyCenter();
        GridLayout.RowHelper adder = gridWidget.createRowHelper(2);
        adder.addChild(new PaddingSlider(createTranslatableComponent("left.padding.chatimage.gui"),
                ChatImageClient.CONFIG.paddingLeft, 0F, (float) this.width / 2, PaddingSlider.PaddingType.LEFT));
        adder.addChild(new PaddingSlider(createTranslatableComponent("right.padding.chatimage.gui"),
                ChatImageClient.CONFIG.paddingRight, 0F, (float) this.width / 2, PaddingSlider.PaddingType.RIGHT));
        adder.addChild(new PaddingSlider(createTranslatableComponent("top.padding.chatimage.gui"),
                ChatImageClient.CONFIG.paddingTop, 0F, (float) this.height / 2, PaddingSlider.PaddingType.TOP));
        adder.addChild(new PaddingSlider(createTranslatableComponent("bottom.padding.chatimage.gui"),
                ChatImageClient.CONFIG.paddingBottom, 0F, (float) this.height / 2, PaddingSlider.PaddingType.BOTTOM));
        adder.addChild(new LimitSlider(createTranslatableComponent("width.limit.chatimage.gui"),
                ChatImageClient.CONFIG.limitWidth, 1F, this.width, LimitSlider.LimitType.WIDTH));
        adder.addChild(new LimitSlider(createTranslatableComponent("height.limit.chatimage.gui"),
                ChatImageClient.CONFIG.limitHeight, 1F, this.height, LimitSlider.LimitType.HEIGHT));
        adder.addChild(Button.builder(createTranslatableComponent("gui.back"), (button) -> {
            if (this.minecraft != null) {
                this.minecraft.gui.setScreen(this.parent);
            }
        }).build(), 2);
// IF fabric-1.19.3
//        gridWidget.recalculateDimensions();
//        SimplePositioningWidget.setPos(gridWidget, 0, this.height / 3 - 12, this.width, this.height, 0.5F, 0.0F);
//        addDrawableWeight(gridWidget);
// ELSE IF > fabric-1.19.3
        gridWidget.arrangeElements();
        FrameLayout.alignInRectangle(gridWidget, 0, this.height / 3 - 12, this.width, this.height, 0.5F, 0.0F);
        gridWidget.visitWidgets(this::addRenderableWidget);
// END IF

// END IF


    }
//IF <= fabric-1.19.4
//    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
//        super.render(matrices, mouseX, mouseY, delta);
//        drawCenteredTextWithShadow(matrices, this.textRenderer, title, this.width / 2, this.height / 4 - 16, 16764108);
//    }
// ELSE
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(this.font, title, this.width / 2, this.height / 4 - 16, 16764108);
    }
// END IF

// IF < fabric-1.19.3
//    private Text getLimitTooltipText(LimitSlider.LimitType limitType) {
//        if (limitType == LimitSlider.LimitType.WIDTH) {
//            return createTranslatableComponent("width.limit.chatimage.tooltip");
//        } else {
//            return createTranslatableComponent("height.limit.chatimage.tooltip");
//        }
//    }
//    private Text getPaddingTooltipText(PaddingSlider.PaddingType paddingType) {
//        Text text;
//        switch (paddingType) {
//            case TOP:
//                text = createTranslatableComponent("top.padding.chatimage.tooltip");
//                break;
//            case BOTTOM:
//                text = createTranslatableComponent("bottom.padding.chatimage.tooltip");
//                break;
//            case LEFT:
//                text = createTranslatableComponent("left.padding.chatimage.tooltip");
//                break;
//            case RIGHT:
//                text = createTranslatableComponent("right.padding.chatimage.tooltip");
//                break;
//            default:
//                return null;
//        }
//        return text;
//    }
// END IF
// IF fabric-1.16.5
//    public <T extends ClickableWidget> T addDrawableWeight(T element)
//    {
//        return this.addButton(element);
// ELSE
    public <T extends GuiEventListener & Renderable & NarratableEntry> T addDrawableWeight(T element)
    {
        return addDrawableWeight(element);
// END IF
    }
}
