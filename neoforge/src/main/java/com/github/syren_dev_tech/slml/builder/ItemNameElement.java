package com.github.syren_dev_tech.slml.builder;

import com.github.syren_dev_tech.slml.helpers.ItemNameHelper;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An item-name element that displays the translated name of an item.
 * Takes an item ID (e.g., "minecraft:dirt") and renders its localized name.
 */
public class ItemNameElement extends BaseUIElement {
    private static final Logger LOGGER = LoggerFactory.getLogger("SLML:ItemNameElement");

    private String itemId;
    private String displayText;
    private TextStyle style;
    private final Font font;
    private boolean autoSizeFromText = false;

    public ItemNameElement(int x, int y, String itemId, Font font) {
        super(x, y, 0, 0);
        this.itemId = itemId != null ? itemId : "";
        this.font = font;
        this.style = new TextStyle();
        this.autoSizeFromText = true;
        updateDisplayText();
    }

    public ItemNameElement(Dimension x, Dimension y, String itemId, Font font) {
        super(x, y, Dimension.parse(0), Dimension.parse(0));
        this.itemId = itemId != null ? itemId : "";
        this.font = font;
        this.style = new TextStyle();
        this.autoSizeFromText = true;
        updateDisplayText();
        LOGGER.info("ItemNameElement constructor: itemId='{}', displayText='{}', font={}, size={}x{}", 
            this.itemId, this.displayText, font != null ? "set" : "null", this.width, this.height);
    }

    /**
     * Constructor with explicit pixel dimensions. Used for dynamic element creation where size is pre-calculated.
     */
    public ItemNameElement(int x, int y, int width, int height, String itemId, Font font) {
        super(x, y, width, height);
        this.itemId = itemId != null ? itemId : "";
        this.font = font;
        this.style = new TextStyle();
        this.autoSizeFromText = false;
        updateDisplayText();
    }

    public static ItemNameElement fromJson(JsonObject json, Font font) {
        LOGGER.info("ItemNameElement.fromJson called with properties: {}", json.keySet());
        
        if (!json.has("item") || json.get("item") == null) {
            String id = json.has("id") ? json.get("id").getAsString() : "(no id)";
            LOGGER.error("ItemNameElement {} missing required item property", id);
            LOGGER.error("Available properties: {}", json.keySet());
            throw new IllegalArgumentException("ItemNameElement " + id + " missing required item property");
        }

        String itemId = json.get("item").getAsString();
        if (itemId == null || itemId.isEmpty()) {
            String id = json.has("id") ? json.get("id").getAsString() : "(no id)";
            LOGGER.error("ItemNameElement {} has empty item ID", id);
            throw new IllegalArgumentException("ItemNameElement " + id + " has empty item ID");
        }

        LOGGER.info("ItemNameElement.fromJson: item='{}'", itemId);

        // Parse position dimensions
        Dimension x = DimensionParser.parseDimension(json, "x", 0);
        Dimension y = DimensionParser.parseDimension(json, "y", 0);

        // Parse size dimensions - use explicit defaults if not specified
        Dimension width = DimensionParser.parseDimension(json, "width", 0);
        Dimension height = DimensionParser.parseDimension(json, "height", 0);

        LOGGER.debug("ItemNameElement.fromJson: item='{}', Position: x={}, y={}, Size: width={}, height={}", itemId, x, y, width, height);

        ItemNameElement element = new ItemNameElement(x, y, itemId, font);

        // Determine what needs auto-sizing
        boolean hasExplicitWidth = json.has("width");
        boolean hasExplicitHeight = json.has("height");

        if (hasExplicitWidth && hasExplicitHeight) {
            // Both width and height explicitly provided - use both, no auto-sizing
            element.setSizeDimensions(width, height);
            element.autoSizeFromText = false;
            LOGGER.debug("ItemNameElement: both width and height provided, not auto-sizing");
        } else if (hasExplicitWidth && !hasExplicitHeight) {
            // Only width provided - use width, auto-size height
            element.setSizeDimensions(width, Dimension.parse(0));
            element.autoSizeFromText = true;
            element.updateSize();
            LOGGER.debug("ItemNameElement: explicit width provided, auto-sizing height");
        } else if (!hasExplicitWidth && hasExplicitHeight) {
            // Only height provided - use height, auto-size width
            element.setSizeDimensions(Dimension.parse(0), height);
            element.autoSizeFromText = true;
            element.updateSize();
            LOGGER.debug("ItemNameElement: explicit height provided, auto-sizing width");
        } else {
            // Neither width nor height provided - auto-size both
            element.autoSizeFromText = true;
            element.updateSize();
            LOGGER.debug("ItemNameElement: no explicit size, auto-sizing both dimensions");
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

        LOGGER.debug("ItemNameElement created: id={} item='{}' displayText='{}' final size={}x{}", 
            element.getId() != null ? element.getId() : "(no id)", 
            itemId, 
            element.displayText, 
            element.width, 
            element.height);

        return element;
    }

    /**
     * Update the display text by translating the item ID.
     */
    private void updateDisplayText() {
        this.displayText = ItemNameHelper.getTranslatedItemName(itemId);
        LOGGER.info("ItemNameElement.updateDisplayText: item='{}' -> displayText='{}'", itemId, displayText);
        if (this.displayText == null || this.displayText.isEmpty()) {
            LOGGER.warn("ItemNameElement.updateDisplayText: displayText is null or empty for item='{}'", itemId);
        }
        updateSize();
    }

    public void setItemId(String itemId) {
        String oldId = this.itemId;
        this.itemId = itemId != null ? itemId : "";
        updateDisplayText();
        LOGGER.info("ItemNameElement.setItemId for {}: '{}' -> '{}' (displayText: '{}', size: {}x{})", 
            getId() != null ? getId() : "(unnamed)", 
            oldId, 
            this.itemId, 
            displayText,
            this.width, 
            this.height);
    }

    public String getItemId() {
        return itemId;
    }

    public String getDisplayText() {
        return displayText;
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
            int calculatedWidth = font.width(displayText);
            this.width = Math.max(calculatedWidth, 1);
        }
        if (this.height == 0) {
            // Height not set - use standard Minecraft font height
            this.height = 9; // Standard Minecraft font height
        }
    }

    /**
     * Override setParentDimensions to recalculate text height after percentage widths are resolved.
     */
    @Override
    public void setParentDimensions(int parentWidth, int parentHeight) {
        super.setParentDimensions(parentWidth, parentHeight);
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
            LOGGER.error("Error rendering item-name element", e);
        }
    }

    private boolean isValidForRendering() {
        if (displayText == null || displayText.isEmpty()) {
            LOGGER.debug("ItemNameElement.render: {} - text is {}, skipping", 
                getId() != null ? getId() : "(unnamed)", 
                displayText == null ? "null" : "empty");
            return false;
        }

        if (this.width <= 0 || this.height <= 0) {
            LOGGER.warn("ItemNameElement.render: {} has 0 size ({}x{}), item='{}', skipping", 
                getId() != null ? getId() : "(unnamed)", 
                width, 
                height, 
                itemId);
            return false;
        }

        if (font == null) {
            LOGGER.error("ItemNameElement.render: {} - font is null, cannot render", 
                getId() != null ? getId() : "(unnamed)");
            return false;
        }

        return true;
    }

    private void performRender(GuiGraphics graphics) {
        String formattedText = style.buildFormattingCode() + displayText;
        int renderColor = style.getColor();
        String elementId = getId() != null ? getId() : "(unnamed)";

        LOGGER.debug("ItemNameElement.render: {} at ({},{}) size {}x{} color=0x{} item='{}' text='{}'", 
            elementId, 
            x, 
            y, 
            width, 
            height, 
            Integer.toHexString(renderColor), 
            itemId,
            formattedText);

        graphics.drawString(font, Component.literal(formattedText), x, y, renderColor);
        LOGGER.debug("ItemNameElement.render: {} - item name rendered successfully", elementId);
    }
}
