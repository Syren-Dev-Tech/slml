package com.github.syren_dev_tech.slml.helpers;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for getting item information and translations.
 */
public class ItemNameHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger("SLML:ItemNameHelper");

    /**
     * Get the translated display name of an item from its resource location string. For example: "minecraft:wheat" -> "Wheat"
     *
     * @param itemId The item resource location as a string (e.g., "minecraft:wheat")
     * @return The translated item name, or a humanized version of the ID if translation fails
     */
    public static String getTranslatedItemName(String itemId) {
        try {
            // Parse the resource location
            ResourceLocation itemLocation = ResourceLocation.parse(itemId);

            // Get the item from the registry
            Item item = BuiltInRegistries.ITEM.get(itemLocation);

            if (item != null && item != Items.AIR) {
                // Create an itemstack and get its display name
                ItemStack stack = new ItemStack(item);
                String displayName = stack.getHoverName().getString();

                // If display name is available, use it
                if (displayName != null && !displayName.isEmpty()) {
                    return displayName;
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to get translated name for item: {}", itemId, e);
        }

        // Fallback: humanize the item ID
        return humanizeItemId(itemId);
    }

    /**
     * Convert an item ID like "minecraft:wheat" to "Wheat" Handles underscores and capitalizes properly.
     *
     * @param itemId The item ID to humanize
     * @return A humanized version of the ID
     */
    private static String humanizeItemId(String itemId) {
        // Remove the namespace prefix (minecraft:)
        String name = itemId;
        if (name.contains(":")) {
            name = name.substring(name.indexOf(":") + 1);
        }

        // Replace underscores with spaces
        name = name.replace("_", " ");

        // Capitalize first letter of each word
        String[] words = name.split(" ");
        StringBuilder result = new StringBuilder();

        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1).toLowerCase());
                result.append(" ");
            }
        }

        return result.toString().trim();
    }

    /**
     * Get the texture path for an item. For example: "minecraft:wheat" -> "minecraft:textures/item/wheat.png"
     *
     * @param itemId The item resource location as a string
     * @return The texture path
     */
    public static String getItemTexturePath(String itemId) {
        try {
            // Parse the resource location
            ResourceLocation itemLocation = ResourceLocation.parse(itemId);

            // Get the item from the registry
            Item item = BuiltInRegistries.ITEM.get(itemLocation);

            if (item != null && item != Items.AIR) {
                // Build the texture path from the item ID
                String namespace = itemLocation.getNamespace();
                String path = itemLocation.getPath();
                return namespace + ":textures/item/" + path + ".png";
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to get texture path for item: {}", itemId, e);
        }

        // Fallback texture
        return "minecraft:textures/item/diamond.png";
    }

    /**
     * Get the emerald texture path.
     *
     * @return The texture path for emeralds
     */
    public static String getEmeraldTexturePath() {
        return "minecraft:textures/item/emerald.png";
    }
}
