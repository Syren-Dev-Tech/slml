package com.github.syren_dev_tech.slml.builder;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A button element that can be clicked and handles callbacks.
 */
public class ButtonElement extends BaseUIElement {
    private static final Logger LOGGER = LoggerFactory.getLogger("VillagerWorkshopsUI");
    private String label;
    private TextStyle textStyle;
    private RenderStyle renderStyle;
    private final Font font;
    private Runnable onClickCallback;
    private boolean hovered;
    private boolean active;

    public ButtonElement(int x, int y, int width, int height, String label, Font font) {
        super(x, y, width, height);
        this.label = label;
        this.font = font;
        this.textStyle = new TextStyle();
        this.renderStyle = new RenderStyle();
        this.onClickCallback = () -> {
        };
        this.hovered = false;
        this.active = true;
    }

    public ButtonElement(Dimension x, Dimension y, Dimension width, Dimension height, String label, Font font) {
        super(x, y, width, height);
        this.label = label;
        this.font = font;
        this.textStyle = new TextStyle();
        this.renderStyle = new RenderStyle();
        this.onClickCallback = () -> {
        };
        this.hovered = false;
        this.active = true;
    }

    public static ButtonElement fromJson(JsonObject json, Font font) {
        String label = json.has("label") ? json.get("label").getAsString() : "";

        // Parse dimensions using DimensionParser to support percentages
        Dimension[] dims = DimensionParser.parseAllDimensions(json, 0, 0, 100, 20);
        Dimension x = dims[0];
        Dimension y = dims[1];
        Dimension width = dims[2];
        Dimension height = dims[3];

        ButtonElement button = new ButtonElement(x, y, width, height, label, font);

        if (json.has("textStyle")) {
            button.textStyle = TextStyle.fromJson(json.getAsJsonObject("textStyle"));
        }

        if (json.has("renderStyle")) {
            button.renderStyle = RenderStyle.fromJson(json.getAsJsonObject("renderStyle"));
        }

        if (json.has("active")) {
            button.active = json.get("active").getAsBoolean();
        }

        return button;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public void setOnClickCallback(Runnable callback) {
        this.onClickCallback = callback != null ? callback : () -> {
        };
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    public TextStyle getTextStyle() {
        return textStyle;
    }

    public void setTextStyle(TextStyle textStyle) {
        this.textStyle = textStyle;
    }

    public RenderStyle getRenderStyle() {
        return renderStyle;
    }

    public void setRenderStyle(RenderStyle renderStyle) {
        this.renderStyle = renderStyle;
    }

    @Override
    public boolean isMouseInside(int mouseX, int mouseY) {
        return active && super.isMouseInside(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        String buttonId = getId() != null ? getId() : "(unnamed)";
        boolean isInside = isMouseInside((int) mouseX, (int) mouseY);

        if (isInside) {
            LOGGER.info("ButtonElement {} clicked at ({}, {})", buttonId, (int) mouseX, (int) mouseY);
            LOGGER.info("Button bounds: x={} y={} w={} h={}", x, y, width, height);
            onClickCallback.run();
            LOGGER.info("Button callback executed for {}", buttonId);
            return true;
        }
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        hovered = isMouseInside(mouseX, mouseY);

        // Draw background
        int bgColor;
        if (active) {
            bgColor = hovered ? 0xFF8B7355 : 0xFF6B5345;
        } else {
            bgColor = 0xFF4C4C4C;
        }
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
        String formattedText = textStyle.buildFormattingCode() + label;
        int textColor = active ? textStyle.getColor() : 0xFF888888;
        graphics.drawCenteredString(font, Component.literal(formattedText), x + width / 2, y + (height - 8) / 2, textColor);
    }
}
