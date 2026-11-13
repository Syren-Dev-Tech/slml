package com.github.syren_dev_tech.slml.builder;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;

/**
 * A checkbox element for boolean input.
 */
public class CheckboxElement extends InputElement {
    private boolean checked;
    private String label;
    private static final int CHECKBOX_SIZE = 16;

    public CheckboxElement(int x, int y, String label, Font font) {
        super(x, y, CHECKBOX_SIZE + 4 + font.width(label), CHECKBOX_SIZE, font);
        this.checked = false;
        this.label = label;
    }

    public CheckboxElement(Dimension x, Dimension y, String label, Font font) {
        // For percentage-based widths, we use a default size since we don't know font.width yet
        super(x, y, Dimension.parse(CHECKBOX_SIZE + 4 + font.width(label)), Dimension.parse(CHECKBOX_SIZE), font);
        this.checked = false;
        this.label = label;
    }

    public static CheckboxElement fromJson(JsonObject json, Font font) {
        String label = json.has("label") ? json.get("label").getAsString() : "";

        // Parse dimensions using DimensionParser to support percentages
        Dimension[] dims = DimensionParser.parseAllDimensions(json, 0, 0, CHECKBOX_SIZE + 4 + font.width(label), CHECKBOX_SIZE);
        Dimension x = dims[0];
        Dimension y = dims[1];
        Dimension width = dims[2];
        Dimension height = dims[3];

        // Create with dimensions instead of the default label-based sizing
        CheckboxElement checkbox = new CheckboxElement(x, y, label, font);

        // Override the dimensions with parsed values if they were explicitly provided
        if (json.has("width") || json.has("height")) {
            checkbox.setSizeDimensions(width, height);
        }

        if (json.has("checked")) {
            checkbox.checked = json.get("checked").getAsBoolean();
        }

        if (json.has("textStyle")) {
            checkbox.textStyle = TextStyle.fromJson(json.getAsJsonObject("textStyle"));
        }

        if (json.has("renderStyle")) {
            checkbox.renderStyle = RenderStyle.fromJson(json.getAsJsonObject("renderStyle"));
        }

        return checkbox;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public void setChecked(boolean checked) {
        this.checked = checked;
    }

    public boolean isChecked() {
        return checked;
    }

    @Override
    public String getValue() {
        return String.valueOf(checked);
    }

    @Override
    public void setValue(String value) {
        this.checked = Boolean.parseBoolean(value);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isMouseInside((int) mouseX, (int) mouseY)) {
            checked = !checked;
            return true;
        }
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Draw checkbox background
        int boxColor = checked ? 0xFF4CAF50 : 0xFF3C3C3C;
        if (renderStyle.hasFillColor()) {
            boxColor = renderStyle.getFillColor();
        }
        graphics.fill(x, y, x + CHECKBOX_SIZE, y + CHECKBOX_SIZE, boxColor);

        // Draw border
        if (renderStyle.hasBorderColor()) {
            int borderWeight = renderStyle.getBorderWeight();
            int borderColor = renderStyle.getBorderColor();

            for (int i = 0; i < borderWeight; i++) {
                graphics.fill(x + i, y + i, x + CHECKBOX_SIZE - i, y + i + 1, borderColor);
                graphics.fill(x + i, y + CHECKBOX_SIZE - i - 1, x + CHECKBOX_SIZE - i, y + CHECKBOX_SIZE - i, borderColor);
                graphics.fill(x + i, y + i, x + i + 1, y + CHECKBOX_SIZE - i, borderColor);
                graphics.fill(x + CHECKBOX_SIZE - i - 1, y + i, x + CHECKBOX_SIZE - i, y + CHECKBOX_SIZE - i, borderColor);
            }
        }

        // Draw checkmark if checked
        if (checked) {
            graphics.fill(x + 3, y + 7, x + 6, y + 10, 0xFFFFFFFF);
            graphics.fill(x + 6, y + 5, x + 13, y + 8, 0xFFFFFFFF);
        }

        // Draw label
        graphics.drawString(font, label, x + CHECKBOX_SIZE + 4, y + (CHECKBOX_SIZE - 8) / 2, textStyle.getColor(), false);
    }
}
