package com.leclowndu93150.hungertweaks.client.config;

import com.leclowndu93150.hungertweaks.config.StadiumMicConfig;
import com.leclowndu93150.hungertweaks.network.StadiumMicConfigSyncPayload;
import com.leclowndu93150.hungertweaks.voicechat.StadiumMicClient;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.function.Consumer;


@Environment(EnvType.CLIENT)
public class StadiumMicConfigScreen extends Screen {

    private final Screen parent;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    private ConfigList list;
    private StadiumMicConfig config;

    public StadiumMicConfigScreen(Screen parent) {
        super(Component.literal("Stadium Mic Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.config = StadiumMicConfig.get();

        this.layout.addTitleHeader(this.title, this.font);

        this.list = this.layout.addToContents(new ConfigList(this));

        this.list.add(new CategoryEntry(this, "General"));
        this.list.addSlider("Distance", 1f, 20000f, config.distance, v -> config.distance = v, 0);
        this.list.addSlider("Volume", 0f, 5f, config.volume, v -> config.volume = v, 2);

        this.list.add(new CategoryEntry(this, "Direct Signal"));
        this.list.addSlider("Direct Gain", 0f, 1f, config.directGain, v -> config.directGain = v, 2);
        this.list.addSlider("Direct HF Gain", 0f, 1f, config.directHfGain, v -> config.directHfGain = v, 2);

        this.list.add(new CategoryEntry(this, "Reverb"));
        this.list.addSlider("Reverb Gain", 0f, 1f, config.reverbGain, v -> config.reverbGain = v, 2);
        this.list.addSlider("Reverb Decay", 0.1f, 10f, config.reverbDecay, v -> config.reverbDecay = v, 1);
        this.list.addSlider("Reverb Density", 0f, 1f, config.reverbDensity, v -> config.reverbDensity = v, 2);
        this.list.addSlider("Reverb Diffusion", 0f, 1f, config.reverbDiffusion, v -> config.reverbDiffusion = v, 2);
        this.list.addSlider("Late Reverb Gain", 0f, 1f, config.reverbLateGain, v -> config.reverbLateGain = v, 2);
        this.list.addSlider("Reflections Gain", 0f, 1f, config.reverbReflectionsGain, v -> config.reverbReflectionsGain = v, 2);

        this.list.add(new CategoryEntry(this, "Source Position"));
        this.list.addSlider("Source X", -10000f, 10000f, config.sourceX, v -> config.sourceX = v, 1);
        this.list.addSlider("Source Y", -10000f, 10000f, config.sourceY, v -> config.sourceY = v, 1);
        this.list.addSlider("Source Z", -10000f, 10000f, config.sourceZ, v -> config.sourceZ = v, 1);

        this.layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).width(200).build());

        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
        if (this.list != null) {
            this.list.updateSize(this.width, this.layout);
        }
    }

    @Override
    public void onClose() {
        StadiumMicConfig.save();
        StadiumMicClient.resetEfx();
        if (this.minecraft.getConnection() != null) {
            ClientPlayNetworking.send(StadiumMicConfigSyncPayload.fromConfig(config));
        }
        this.minecraft.setScreen(this.parent);
    }

    @Environment(EnvType.CLIENT)
    static class ConfigList extends ContainerObjectSelectionList<ConfigEntry> {

        private static final int ITEM_HEIGHT = 25;
        private final StadiumMicConfigScreen screen;

        public ConfigList(StadiumMicConfigScreen screen) {
            super(screen.minecraft, screen.width, screen.layout.getContentHeight(), screen.layout.getHeaderHeight(), ITEM_HEIGHT);
            this.centerListVertically = false;
            this.screen = screen;
        }

        public void add(ConfigEntry entry) {
            this.addEntry(entry);
        }

        public void addSlider(String label, float min, float max, float current, Consumer<Float> setter, int decimals) {
            this.addEntry(new SliderEntry(screen, label, min, max, current, setter, decimals));
        }

        @Override
        public int getRowWidth() {
            return 310;
        }
    }

    @Environment(EnvType.CLIENT)
    static abstract class ConfigEntry extends ContainerObjectSelectionList.Entry<ConfigEntry> {
        protected final StadiumMicConfigScreen screen;

        ConfigEntry(StadiumMicConfigScreen screen) {
            this.screen = screen;
        }
    }

    @Environment(EnvType.CLIENT)
    static class CategoryEntry extends ConfigEntry {

        private final Component label;

        CategoryEntry(StadiumMicConfigScreen screen, String text) {
            super(screen);
            this.label = Component.literal(text);
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            int centerX = screen.width / 2;
            int textWidth = screen.font.width(label);
            graphics.drawString(screen.font, label, centerX - textWidth / 2, top + (height - 9) / 2, 0xFFFFFF);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of();
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of();
        }
    }

    @Environment(EnvType.CLIENT)
    static class SliderEntry extends ConfigEntry {

        private final ConfigSlider slider;

        SliderEntry(StadiumMicConfigScreen screen, String label, float min, float max, float current, Consumer<Float> setter, int decimals) {
            super(screen);
            float clamped = Mth.clamp(current, min, max);
            double normalized = (max - min) > 0 ? (clamped - min) / (max - min) : 0;
            this.slider = new ConfigSlider(0, 0, 310, 20, label, min, max, normalized, setter, decimals);
        }

        @Override
        public void render(GuiGraphics graphics, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovered, float partialTick) {
            this.slider.setPosition(screen.width / 2 - 155, top);
            this.slider.render(graphics, mouseX, mouseY, partialTick);
        }

        @Override
        public List<? extends GuiEventListener> children() {
            return List.of(slider);
        }

        @Override
        public List<? extends NarratableEntry> narratables() {
            return List.of(slider);
        }
    }

    @Environment(EnvType.CLIENT)
    static class ConfigSlider extends AbstractSliderButton {

        private final String label;
        private final float min;
        private final float max;
        private final Consumer<Float> setter;
        private final int decimals;

        ConfigSlider(int x, int y, int width, int height, String label, float min, float max, double normalizedValue, Consumer<Float> setter, int decimals) {
            super(x, y, width, height, Component.empty(), normalizedValue);
            this.label = label;
            this.min = min;
            this.max = max;
            this.setter = setter;
            this.decimals = decimals;
            this.updateMessage();
        }

        private float getActualValue() {
            return this.min + (float) this.value * (this.max - this.min);
        }

        @Override
        protected void updateMessage() {
            String format = "%." + decimals + "f";
            this.setMessage(Component.literal(label + ": " + String.format(format, getActualValue())));
        }

        @Override
        protected void applyValue() {
            setter.accept(getActualValue());
        }
    }
}
