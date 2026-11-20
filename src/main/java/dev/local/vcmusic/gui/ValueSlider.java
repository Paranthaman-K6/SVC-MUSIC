package dev.local.vcmusic.gui;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.DoubleConsumer;

public class ValueSlider extends AbstractSliderButton {

    private final String label;
    private final DoubleConsumer onChange;

    public ValueSlider(int x, int y, int width, int height, String label, double value, DoubleConsumer onChange) {
        super(x, y, width, height, Component.empty(), value);
        this.label = label;
        this.onChange = onChange;
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        setMessage(Component.literal(label + ": " + Math.round(value * 100D) + "%"));
    }

    @Override
    protected void applyValue() {
        onChange.accept(value);
    }
}
