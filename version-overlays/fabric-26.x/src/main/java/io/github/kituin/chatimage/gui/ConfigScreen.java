package io.github.kituin.chatimage.gui;

import io.github.kituin.ChatImageCode.ChatImageConfig;
import io.github.kituin.chatimage.widget.GifSlider;
import io.github.kituin.chatimage.widget.TimeOutSlider;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

// END IF
import static io.github.kituin.chatimage.client.ChatImageClient.CONFIG;
import static io.github.kituin.chatimage.tool.SimpleUtil.*;

@Environment(EnvType.CLIENT)
// IF <= fabric-1.19.2
//public class ConfigScreen extends ConfigRawScreen {
//    public ConfigScreen(Screen screen) {
//        super(createTranslatableComponent("config.chatimage.category"), screen);
//    }
// ELSE
public class ConfigScreen extends Screen {
    private final Screen parent;

    public ConfigScreen(Screen screen) {
        super(createTranslatableComponent("config.chatimage.category"));
        parent = screen;
    }
// END IF

    public ConfigScreen() {
        this(null);
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
// IF >= fabric-1.19.3
    protected void init() {
        super.init();
        GridLayout gridWidget = new GridLayout();
        gridWidget.defaultCellSetting().paddingHorizontal(5).paddingBottom(4).alignHorizontallyCenter();
        GridLayout.RowHelper adder = gridWidget.createRowHelper(2);
        adder.addChild(Button.builder(getNsfw(CONFIG.nsfw), (button) -> {
            CONFIG.nsfw = !CONFIG.nsfw;
            button.setMessage(getNsfw(CONFIG.nsfw));
            ChatImageConfig.saveConfig(CONFIG);
        }).tooltip(Tooltip.create(Component.translatable("nsfw.chatimage.tooltip"))).build());
        adder.addChild(new GifSlider());
        adder.addChild(new TimeOutSlider());
        adder.addChild(Button.builder(Component.translatable("padding.chatimage.gui"), (button) -> {
            if (this.minecraft != null) {
                setScreen(this.minecraft, new LimitPaddingScreen(this));
            }
        }).tooltip(Tooltip.create(Component.translatable("padding.chatimage.tooltip"))).build());
        adder.addChild(Button.builder(getCq(CONFIG.cqCode), (button) -> {
            CONFIG.cqCode = !CONFIG.cqCode;
            button.setMessage(getCq(CONFIG.cqCode));
            ChatImageConfig.saveConfig(CONFIG);
        }).tooltip(Tooltip.create(Component.translatable("cq.chatimage.tooltip"))).build());
        adder.addChild(Button.builder(getUri(CONFIG.checkImageUri), (button) -> {
            CONFIG.checkImageUri = !CONFIG.checkImageUri;
            button.setMessage(getUri(CONFIG.checkImageUri));
            ChatImageConfig.saveConfig(CONFIG);
        }).build());
        adder.addChild(Button.builder(getDrag(CONFIG.dragUseCicode), (button) -> {
            CONFIG.dragUseCicode = !CONFIG.dragUseCicode;
            button.setMessage(getDrag(CONFIG.dragUseCicode));
            ChatImageConfig.saveConfig(CONFIG);
        }).tooltip(Tooltip.create(Component.translatable("drag.chatimage.tooltip"))).build());
        adder.addChild(Button.builder(getDragImage(CONFIG.dragImage), (button) -> {
            CONFIG.dragImage = !CONFIG.dragImage;
            button.setMessage(getDragImage(CONFIG.dragImage));
            ChatImageConfig.saveConfig(CONFIG);
        }).tooltip(Tooltip.create(Component.translatable("image.drag.chatimage.tooltip"))).build());
        adder.addChild(Button.builder(getExperimentalTextComponentCompatibility(CONFIG.experimentalTextComponentCompatibility), (button) -> {
            CONFIG.experimentalTextComponentCompatibility = !CONFIG.experimentalTextComponentCompatibility;
            button.setMessage(getExperimentalTextComponentCompatibility(CONFIG.experimentalTextComponentCompatibility));
            ChatImageConfig.saveConfig(CONFIG);
        }).tooltip(Tooltip.create(Component.translatable("experimental.component.chatimage.tooltip"))).build());
        adder.addChild(Button.builder(Component.translatable("gui.back"), (button) -> {
            if (this.minecraft != null) {
                setScreen(this.minecraft, this.parent);
            }
        }).build(), 2);
// IF > fabric-1.19.3
        gridWidget.arrangeElements();
        FrameLayout.alignInRectangle(gridWidget, 0, this.height / 3 - 12, this.width, this.height, 0.5F, 0.0F);
        gridWidget.visitWidgets(this::addRenderableWidget);
// ELSE IF fabric-1.19.3
//        gridWidget.recalculateDimensions();
//        SimplePositioningWidget.setPos(gridWidget, 0, this.height / 3 - 12, this.width, this.height, 0.5F, 0.0F);
//        this.addDrawableChild(gridWidget);
// END IF
// ELSE
//    protected void init() {
//        super.init();
//        addDrawableWeight(new ButtonWidget(this.width / 2 - 154, this.height / 4 + 24 + -16, 150, 20, getNsfw(CONFIG.nsfw), (button) -> {
//            CONFIG.nsfw = !CONFIG.nsfw;
//            button.setMessage(getNsfw(CONFIG.nsfw));
//            ChatImageConfig.saveConfig(CONFIG);
//        }, getButtonTooltip(createTranslatableComponent("nsfw.chatimage.tooltip"))));
//        addDrawableWeight(new GifSlider(this.width / 2 + 4, this.height / 4 + 24 + -16, 150, 20, getSliderTooltip(createTranslatableComponent("gif.chatimage.tooltip"))));
//        addDrawableWeight(new TimeOutSlider(this.width / 2 - 154, this.height / 4 + 48 + -16, 150, 20, getSliderTooltip(createTranslatableComponent("timeout.chatimage.tooltip"))));
//        addDrawableWeight(new ButtonWidget(this.width / 2 + 4, this.height / 4 + 48 + -16, 150, 20, createTranslatableComponent("padding.chatimage.gui"), (button) -> {
//            if (this.client != null) {
//                setScreen(this.client, new LimitPaddingScreen(this));
//            }
//        }, getButtonTooltip(createTranslatableComponent("padding.chatimage.tooltip"))));
//        addDrawableWeight(new ButtonWidget(this.width / 2 - 154, this.height / 4 + 72 - 16, 150, 20, getCq(CONFIG.cqCode), (button) -> {
//            CONFIG.cqCode = !CONFIG.cqCode;
//            button.setMessage(getCq(CONFIG.cqCode));
//            ChatImageConfig.saveConfig(CONFIG);
//        }, getButtonTooltip(createTranslatableComponent("cq.chatimage.tooltip"))));
//        addDrawableWeight(new ButtonWidget(this.width / 2 + 4, this.height / 4 + 72 - 16, 150, 20, getUri(CONFIG.checkImageUri), (button) -> {
//            CONFIG.checkImageUri = !CONFIG.checkImageUri;
//            button.setMessage(getUri(CONFIG.checkImageUri));
//            ChatImageConfig.saveConfig(CONFIG);
//        }));
//        addDrawableWeight(new ButtonWidget(this.width / 2 - 154, this.height / 4 + 96 - 16, 150, 20, getDrag(CONFIG.dragUseCicode), (button) -> {
//            CONFIG.dragUseCicode = !CONFIG.dragUseCicode;
//            button.setMessage(getDrag(CONFIG.dragUseCicode));
//            ChatImageConfig.saveConfig(CONFIG);
//        }, getButtonTooltip(createTranslatableComponent("drag.chatimage.tooltip"))));
//        addDrawableWeight(new ButtonWidget(this.width / 2 + 4, this.height / 4 + 96 - 16, 150, 20, getDragImage(CONFIG.dragImage), (button) -> {
//            CONFIG.dragImage = !CONFIG.dragImage;
//            button.setMessage(getDragImage(CONFIG.dragImage));
//            ChatImageConfig.saveConfig(CONFIG);
//        }, getButtonTooltip(createTranslatableComponent("image.drag.chatimage.tooltip"))));
//        addDrawableWeight(new ButtonWidget(this.width / 2 - 154, this.height / 4 + 120 - 16, 150, 20, getExperimentalTextComponentCompatibility(CONFIG.experimentalTextComponentCompatibility), (button) -> {
//            CONFIG.experimentalTextComponentCompatibility = !CONFIG.experimentalTextComponentCompatibility;
//            button.setMessage(getExperimentalTextComponentCompatibility(CONFIG.experimentalTextComponentCompatibility));
//            ChatImageConfig.saveConfig(CONFIG);
//        }, getButtonTooltip(createTranslatableComponent("experimental.component.chatimage.tooltip"))));
//        addDrawableWeight(new ButtonWidget(this.width / 2 - 77, this.height / 4 + 144 + -16, 150, 20, createTranslatableComponent("gui.back"), (button) -> {
//            if (this.client != null) {
//                setScreen(this.client, this.parent);
//            }
//        }));
// END IF
    }

    private MutableComponent getCq(boolean enable) {
        return getEnable("cq.chatimage.gui", enable);
    }

    private MutableComponent getNsfw(boolean enable) {
        return getEnable("nsfw.chatimage.gui", !enable);
    }

    private MutableComponent getDrag(boolean enable) {
        return getEnable("drag.chatimage.gui", enable);
    }
    private MutableComponent getDragImage(boolean enable) {
        return getEnable("image.drag.chatimage.gui", enable);
    }
    private MutableComponent getExperimentalTextComponentCompatibility(boolean enable) {
        return getEnable("experimental.component.chatimage.gui", enable);
    }
    private MutableComponent getUri(boolean enable) {
        return getEnable("uri.chatimage.gui", enable);
    }

    public static MutableComponent getEnable(String key, boolean enable) {
        return composeGenericOptionText(createTranslatableComponent(key), createTranslatableComponent((enable ? "open" : "close") + ".chatimage.common"));
    }

// IF fabric-1.16.5
//    public <T extends ClickableWidget> T addDrawableWeight(T element)
//    {
//        return this.addButton(element);
// ELSE
    public <T extends GuiEventListener & Renderable & NarratableEntry> T addDrawableWeight(T element)
    {
        return this.addRenderableWidget(element);
// END IF
    }
}
