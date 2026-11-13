package com.github.syren_dev_tech.slml.builder;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A text element that can display text with customizable styling.
 */
public class TextElement extends BaseUIElement {
    private static final Logger LOGGER = LoggerFactory.getLogger("VillagerWorkshopsUI");

    private String text;
    private TextStyle style;
    private final Font font;
    private boolean autoSizeFromText = false;

    public TextElement(int x, int y, String text, Font font) {
        super(x, y, 0, 0);
        this.text = text != null ? text : "";
        this.font = font;
        this.style = new TextStyle();
        this.autoSizeFromText = true;
        updateSize();
    }

    public TextElement(Dimension x, Dimension y, String text, Font font) {
        super(x, y, Dimension.parse(0), Dimension.parse(0));
        this.text = text != null ? text : "";
        this.font = font;
        this.style = new TextStyle();
        this.autoSizeFromText = true;
        updateSize();
    }

    /**
     * Constructor with explicit pixel dimensions. Used for dynamic element creation where size is pre-calculated.
     */
    public TextElement(int x, int y, int width, int height, String text, Font font) {
        super(x, y, width, height);
        this.text = text != null ? text : "";
        this.font = font;
        this.style = new TextStyle();
        this.autoSizeFromText = false; // Don't auto-size when explicit dimensions given
    }


    public static TextElement fromJson(JsonObject json, Font font) {
        String text = json.has("text") ? json.get("text").getAsString() : "";

        // Parse position dimensions
        Dimension x = DimensionParser.parseDimension(json, "x", 0);
        Dimension y = DimensionParser.parseDimension(json, "y", 0);

        // Parse size dimensions - use explicit defaults if not specified
        Dimension width = DimensionParser.parseDimension(json, "width", 0);
        Dimension height = DimensionParser.parseDimension(json, "height", 0);

        LOGGER.debug("TextElement.fromJson: text='{}'", text);
        LOGGER.debug("  Position: x={}, y={}", x, y);
        LOGGER.debug("  Size: width={}, height={}", width, height);
        LOGGER.debug("  Has width in JSON: {}, Has height in JSON: {}", json.has("width"), json.has("height"));

        TextElement element = new TextElement(x, y, text, font);

        // Determine what needs auto-sizing
        boolean hasExplicitWidth = json.has("width");
        boolean hasExplicitHeight = json.has("height");

        if (hasExplicitWidth && hasExplicitHeight) {
            // Both width and height explicitly provided - use both, no auto-sizing
            element.setSizeDimensions(width, height);
            element.autoSizeFromText = false;
            LOGGER.debug("TextElement: both width and height provided, not auto-sizing");
        } else if (hasExplicitWidth && !hasExplicitHeight) {
            // Only width provided - use width, auto-size height
            element.setSizeDimensions(width, Dimension.parse(0));
            element.autoSizeFromText = true; // Height will be auto-calculated
            element.updateSize(); // Calculate height based on content
            LOGGER.debug("TextElement: explicit width provided, auto-sizing height");
        } else if (!hasExplicitWidth && hasExplicitHeight) {
            // Only height provided - use height, auto-size width
            element.setSizeDimensions(Dimension.parse(0), height);
            element.autoSizeFromText = true; // Width will be auto-calculated
            element.updateSize(); // Calculate width based on content
            LOGGER.debug("TextElement: explicit height provided, auto-sizing width");
        } else {
            // Neither width nor height provided - auto-size both
            element.autoSizeFromText = true;
            element.updateSize();
            LOGGER.debug("TextElement: no explicit size, auto-sizing both dimensions");
        }

        if (json.has("style")) {
            element.style = TextStyle.fromJson(json.getAsJsonObject("style"));
        }

        if (json.has("id")) {
            element.setId(json.get("id").getAsString());
        }

        if (json.has("flexGrow")) {
            int flexGrow = json.get("flexGrow").getAsInt();
            element.setFlexGrow(flexGrow);
        }

        LOGGER.debug("TextElement created: id={} text='{}' final size={}x{}", element.getId() != null ? element.getId() : "(no id)", text, element.width, element.height);

        return element;
    }

    public void setText(String text) {
        String oldText = this.text;
        this.text = text != null ? text : "";
        // Always try to update size - updateSize() is smart about what to recalculate
        updateSize();
        LOGGER.info("TextElement.setText for {}: '{}' -> '{}' (size: {}x{}, autoSize: {})", getId() != null ? getId() : "(unnamed)", oldText, this.text, this.width, this.height, autoSizeFromText);
    }

    /**
     * Set text by replacing {{key}} placeholders with provided values.
     *
     * @param template The template string containing {{key}} placeholders
     * @param placeholders A map of placeholder keys to their replacement values
     */
    public void setTextWithPlaceholders(String template, java.util.Map<String, String> placeholders) {
        if (template == null || placeholders == null) {
            setText(template != null ? template : "");
            return;
        }

        String result = template;
        for (java.util.Map.Entry<String, String> entry : placeholders.entrySet()) {
            if (entry.getValue() != null) {
                result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }
        }
        setText(result);
    }

    public String getText() {
        return text;
    }

    public TextStyle getStyle() {
        return style;
    }

    public void setStyle(TextStyle style) {
        this.style = style;
    }

    private void updateSize() {
        // Auto-size any dimension that is 0
        if (this.width == 0) {
            // Width not set - calculate from text
            int calculatedWidth = font.width(text);
            this.width = Math.max(calculatedWidth, 1); // Ensure at least 1 pixel width
        }
        if (this.height == 0) {
            // Height not set - use standard Minecraft font height
            this.height = 9; // Standard Minecraft font height (9 pixels)
        }
    }

    /**
     * Override setParentDimensions to recalculate text height after percentage widths are resolved.
     */
    @Override
    public void setParentDimensions(int parentWidth, int parentHeight) {
        super.setParentDimensions(parentWidth, parentHeight);
        // After dimensions are computed (percentages resolved), update size to handle 0-height
        updateSize();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!isValidForRendering()) {
            return;
        }
        try {
            performRender(graphics);
        } catch (Exception e) {
            LOGGER.error("Error rendering text element", e);
        }
    }

    private boolean isValidForRendering() {
        if (text == null || text.isEmpty()) {
            LOGGER.debug("TextElement.render: {} - text is {}, skipping", getId() != null ? getId() : "(unnamed)", text == null ? "null" : "empty");
            return false;
        }

        if (this.width <= 0 || this.height <= 0) {
            LOGGER.warn("TextElement.render: {} has 0 size ({}x{}), text='{}', skipping", getId() != null ? getId() : "(unnamed)", width, height, text);
            return false;
        }

        if (font == null) {
            String elementId = getId() != null ? getId() : "(unnamed)";
            LOGGER.error("TextElement.render: {} - font is null, cannot render", elementId);
            return false;
        }

        return true;
    }

    private void performRender(GuiGraphics graphics) {
        String formattedText = style.buildFormattingCode() + text;
        int renderColor = style.getColor();
        String elementId = getId() != null ? getId() : "(unnamed)";

        logRenderDiagnostics(elementId, formattedText, renderColor);

        // Draw the text using Component (modern Minecraft requires this)
        graphics.drawString(font, Component.literal(formattedText), x, y, renderColor);
        LOGGER.debug("TextElement.render: {} - text rendered successfully", elementId);
    }

    private void logRenderDiagnostics(String elementId, String formattedText, int renderColor) {
        if (!LOGGER.isDebugEnabled()) {
            return;
        }
        boolean isValidColor = renderColor != 0 && renderColor != 0xFF000000;
        boolean isValidPosition = x >= -1000 && x <= 10000 && y >= -1000 && y <= 10000;

        LOGGER.debug("TextElement.render: {} at ({},{}) size {}x{} color=0x{} text='{}'", elementId, x, y, width, height, Integer.toHexString(renderColor), formattedText);
        LOGGER.debug("  Diagnostic: validColor={}, validPosition={}, fontNull={}, formattedText='{}'", isValidColor, isValidPosition, font == null, formattedText);
    }
}
