package com.github.syren_dev_tech.slml.builder;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.Font;

/**
 * A numeric input element that only accepts numbers.
 */
public class NumericInputElement extends TextInputElement {
    private int minValue;
    private int maxValue;
    private boolean allowNegative;

    public NumericInputElement(int x, int y, int width, int height, Font font) {
        super(x, y, width, height, font);
        this.minValue = Integer.MIN_VALUE;
        this.maxValue = Integer.MAX_VALUE;
        this.allowNegative = true;
        this.maxLength = 10;
    }

    public NumericInputElement(Dimension x, Dimension y, Dimension width, Dimension height, Font font) {
        super(x, y, width, height, font);
        this.minValue = Integer.MIN_VALUE;
        this.maxValue = Integer.MAX_VALUE;
        this.allowNegative = true;
        this.maxLength = 10;
    }

    public static NumericInputElement fromJson(JsonObject json, Font font) {
        // Parse dimensions using DimensionParser to support percentages
        Dimension[] dims = DimensionParser.parseAllDimensions(json, 0, 0, 100, 20);
        Dimension x = dims[0];
        Dimension y = dims[1];
        Dimension width = dims[2];
        Dimension height = dims[3];

        NumericInputElement input = new NumericInputElement(x, y, width, height, font);

        if (json.has("minValue")) {
            input.minValue = json.get("minValue").getAsInt();
        }

        if (json.has("maxValue")) {
            input.maxValue = json.get("maxValue").getAsInt();
        }

        if (json.has("allowNegative")) {
            input.allowNegative = json.get("allowNegative").getAsBoolean();
        }

        if (json.has("placeholder")) {
            input.placeholder = json.get("placeholder").getAsString();
        }

        if (json.has("textStyle")) {
            input.textStyle = TextStyle.fromJson(json.getAsJsonObject("textStyle"));
        }

        if (json.has("renderStyle")) {
            input.renderStyle = RenderStyle.fromJson(json.getAsJsonObject("renderStyle"));
        }

        return input;
    }

    public void setRange(int min, int max) {
        this.minValue = min;
        this.maxValue = max;
    }

    public void setAllowNegative(boolean allow) {
        this.allowNegative = allow;
    }

    public int getNumericValue() {
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public void addCharacter(char ch) {
        if (!focused || value.length() >= maxLength) {
            return;
        }

        // Allow digits
        if (ch >= '0' && ch <= '9') {
            value.insert(cursorPosition, ch);
            cursorPosition++;
            validateRange();
        }
        // Allow minus sign only at the beginning if allowed
        else if (ch == '-' && allowNegative && cursorPosition == 0 && !value.toString().contains("-")) {
            value.insert(0, ch);
            cursorPosition++;
            validateRange();
        }
    }

    private void validateRange() {
        try {
            int num = Integer.parseInt(value.toString());
            if (num < minValue || num > maxValue) {
                value.deleteCharAt(cursorPosition - 1);
                cursorPosition--;
            }
        } catch (NumberFormatException e) {
            // Invalid number format, allow user to continue typing
        }
    }
}
