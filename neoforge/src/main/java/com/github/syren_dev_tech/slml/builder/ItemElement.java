package com.github.syren_dev_tech.slml.builder;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An element for rendering Minecraft items with their textures. Takes an item name (e.g., "minecraft:wheat") and renders the item icon.
 * Optionally displays the item count in the bottom-right corner when showCount is enabled.
 */
public class ItemElement extends BaseUIElement {
    private static final Logger LOGGER = LoggerFactory.getLogger("VillagerWorkshopsUI");

    private ResourceLocation itemName;
    private ItemStack itemStack;
    private boolean showCount;
    private int count;
    private final Font font;

    public ItemElement(int x, int y, int width, int height, ResourceLocation itemName, Font font) {
        super(x, y, width, height);
        this.itemName = itemName;
        this.font = font;
        this.showCount = false;
        this.count = 1;
        this.itemStack = ItemStack.EMPTY;
        loadItemStack();
    }

    public ItemElement(Dimension x, Dimension y, Dimension width, Dimension height, ResourceLocation itemName, Font font) {
        super(x, y, width, height);
        this.itemName = itemName;
        this.font = font;
        this.showCount = false;
        this.count = 1;
        this.itemStack = ItemStack.EMPTY;
        loadItemStack();
    }

    public static ItemElement fromJson(JsonObject json, Font font) {
        if (!json.has("item") || json.get("item") == null) {
            String id = json.has("id") ? json.get("id").getAsString() : "(no id)";
            LOGGER.error("ItemElement {} missing required item property", id);
            throw new IllegalArgumentException("ItemElement " + id + " missing required item property");
        }

        String itemIdStr = json.get("item").getAsString();
        if (itemIdStr == null || itemIdStr.isEmpty()) {
            String id = json.has("id") ? json.get("id").getAsString() : "(no id)";
            LOGGER.error("ItemElement {} has empty item ID", id);
            throw new IllegalArgumentException("ItemElement " + id + " has empty item ID");
        }

        LOGGER.info("ItemElement.fromJson(): Creating element with item ID: {}", itemIdStr);
        ResourceLocation itemName = ResourceLocation.parse(itemIdStr);

        Dimension[] dims = DimensionParser.parseAllDimensions(json, 0, 0, 16, 16);
        Dimension x = dims[0];
        Dimension y = dims[1];
        Dimension width = dims[2];
        Dimension height = dims[3];

        LOGGER.info("ItemElement.fromJson(): Dimensions parsed - x={}, y={}, width={}, height={}", x, y, width, height);

        ItemElement element = new ItemElement(x, y, width, height, itemName, font);

        if (json.has("showCount")) {
            element.showCount = json.get("showCount").getAsBoolean();
            LOGGER.debug("ItemElement.fromJson(): showCount set to {}", element.showCount);
        }

        if (json.has("count")) {
            element.count = json.get("count").getAsInt();
            LOGGER.debug("ItemElement.fromJson(): count set to {}", element.count);
        }

        LOGGER.info("ItemElement.fromJson(): Successfully created ItemElement with item: {}", itemName);
        return element;
    }

    /**
     * Load the ItemStack from the registry using the item name.
     */
    private void loadItemStack() {
        try {
            LOGGER.info("ItemElement.loadItemStack(): Attempting to load item from registry: {}", itemName);
            Item item = BuiltInRegistries.ITEM.get(itemName);
            LOGGER.info("ItemElement.loadItemStack(): Registry lookup returned: {}", item != null ? item.toString() : "null");

            if (item != null && !item.equals(net.minecraft.world.item.Items.AIR)) {
                this.itemStack = new ItemStack(item, count);
                LOGGER.info("ItemElement.loadItemStack(): Successfully loaded item: {} -> {} (stack empty: {})", itemName, item.getDescription().getString(), itemStack.isEmpty());
            } else {
                LOGGER.warn("ItemElement.loadItemStack(): Item not found in registry or is AIR: {}", itemName);
                this.itemStack = ItemStack.EMPTY;
            }
        } catch (Exception e) {
            LOGGER.error("ItemElement.loadItemStack(): Failed to load item: {}", itemName, e);
            this.itemStack = ItemStack.EMPTY;
        }
    }

    public void setItem(ResourceLocation itemName) {
        this.itemName = itemName;
        loadItemStack();
    }

    public void setCount(int count) {
        this.count = Math.max(1, count);
        if (!itemStack.isEmpty()) {
            itemStack.setCount(this.count);
        }
    }

    public ResourceLocation getItemName() {
        return itemName;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public void setShowCount(boolean showCount) {
        this.showCount = showCount;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!itemStack.isEmpty()) {
            try {
                LOGGER.debug("ItemElement.render(): Rendering {} at screen pos ({},{}), element size {}x{}", itemStack.getHoverName().getString(), x, y, width, height);

                // Push a pose (transform matrix) to preserve render state
                graphics.pose().pushPose();

                // Render the item at the specified position
                graphics.renderItem(itemStack, x, y);

                // Render count if showCount is enabled and count > 1
                if (showCount && count > 1 && font != null) {
                    String countText = String.valueOf(count);
                    int countX = x + width - font.width(countText) - 1; // Right align with 1px margin
                    int countY = y + height - 8; // Bottom align (8 is standard font height)

                    LOGGER.debug("ItemElement.render(): Rendering count '{}' at ({},{})", countText, countX, countY);
                    graphics.drawString(font, countText, countX, countY, 0xFFFFFF, true); // White text with shadow
                }

                graphics.pose().popPose();
                LOGGER.debug("ItemElement.render(): Successfully rendered {}", itemName);
            } catch (Exception e) {
                LOGGER.error("ItemElement.render(): Error rendering item {}: {}", itemName, e.getMessage());
                // Draw error box
                graphics.fill(x, y, x + width, y + height, 0xFFFF0000);
            }
        } else {
            LOGGER.debug("ItemElement.render(): ItemStack is empty, drawing magenta placeholder at ({},{}), size {}x{}", x, y, width, height);
            // Draw placeholder for missing item
            graphics.fill(x, y, x + width, y + height, 0xFFAA00AA);
        }
    }
}
