package com.github.syren_dev_tech.slml.builder;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import org.lwjgl.glfw.GLFW;

/**
 * A text input element for user text entry.
 */
public class TextInputElement extends InputElement {
    protected StringBuilder value;
    protected String placeholder;
    protected int maxLength;
    protected int cursorPosition;

    public TextInputElement(int x, int y, int width, int height, Font font) {
        super(x, y, width, height, font);
        this.value = new StringBuilder();
        this.placeholder = "";
        this.maxLength = 256;
        this.cursorPosition = 0;
    }

    public TextInputElement(Dimension x, Dimension y, Dimension width, Dimension height, Font font) {
        super(x, y, width, height, font);
        this.value = new StringBuilder();
        this.placeholder = "";
        this.maxLength = 256;
        this.cursorPosition = 0;
    }

    public static TextInputElement fromJson(JsonObject json, Font font) {
        // Parse dimensions using DimensionParser to support percentages
        Dimension[] dims = DimensionParser.parseAllDimensions(json, 0, 0, 100, 20);
        Dimension x = dims[0];
        Dimension y = dims[1];
        Dimension width = dims[2];
        Dimension height = dims[3];

        TextInputElement input = new TextInputElement(x, y, width, height, font);

        if (json.has("placeholder")) {
            input.placeholder = json.get("placeholder").getAsString();
        }

        if (json.has("maxLength")) {
            input.maxLength = json.get("maxLength").getAsInt();
        }

        if (json.has("textStyle")) {
            input.textStyle = TextStyle.fromJson(json.getAsJsonObject("textStyle"));
        }

        if (json.has("renderStyle")) {
            input.renderStyle = RenderStyle.fromJson(json.getAsJsonObject("renderStyle"));
        }

        return input;
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public void setMaxLength(int maxLength) {
        this.maxLength = maxLength;
    }

    @Override
    public String getValue() {
        return value.toString();
    }

    @Override
    public void setValue(String value) {
        this.value = new StringBuilder(value != null ? value : "");
        this.cursorPosition = Math.min(this.value.length(), this.maxLength);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused) {
            return false;
        }

        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (cursorPosition > 0) {
                value.deleteCharAt(cursorPosition - 1);
                cursorPosition--;
            }
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_DELETE) {
            if (cursorPosition < value.length()) {
                value.deleteCharAt(cursorPosition);
            }
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_LEFT) {
            cursorPosition = Math.max(0, cursorPosition - 1);
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_RIGHT) {
            cursorPosition = Math.min(value.length(), cursorPosition + 1);
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_HOME) {
            cursorPosition = 0;
            return true;
        } else if (keyCode == GLFW.GLFW_KEY_END) {
            cursorPosition = value.length();
            return true;
        }

        return false;
    }

    public void addCharacter(char ch) {
        if (focused && value.length() < maxLength && isPrintableChar(ch)) {
            value.insert(cursorPosition, ch);
            cursorPosition++;
        }
    }

    private boolean isPrintableChar(char ch) {
        return ch >= 32 && ch < 127;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Draw background
        int bgColor = focused ? 0xFF4C4C4C : 0xFF3C3C3C;
        if (renderStyle.hasFillColor()) {
            bgColor = renderStyle.getFillColor();
        }
        graphics.fill(x, y, x + width, y + height, bgColor);

        // Draw border
        if (renderStyle.hasBorderColor()) {
            int borderWeight = renderStyle.getBorderWeight();
            int borderColor = renderStyle.getBorderColor();

            for (int i = 0; i < borderWeight; i++) {
                graphics.fill(x + i, y + i, x + width - i, y + i + 1, borderColor);
                graphics.fill(x + i, y + height - i - 1, x + width - i, y + height - i, borderColor);
                graphics.fill(x + i, y + i, x + i + 1, y + height - i, borderColor);
                graphics.fill(x + width - i - 1, y + i, x + width - i, y + height - i, borderColor);
            }
        }

        // Draw text
        String displayText = value.isEmpty() ? placeholder : value.toString();
        int textColor = value.isEmpty() ? 0xFF888888 : textStyle.getColor();
        graphics.drawString(font, displayText, x + 4, y + (height - 8) / 2, textColor, false);

        // Draw cursor if focused
        if (focused) {
            int cursorX = x + 4 + font.width(value.substring(0, Math.min(cursorPosition, value.length())));
            graphics.fill(cursorX, y + 2, cursorX + 1, y + height - 2, 0xFFFFFFFF);
        }
    }
}
