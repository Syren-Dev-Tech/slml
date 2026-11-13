package com.github.syren_dev_tech.slml.helpers;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for scanning player inventory for items. Used to check if player has enough items for trades across multiple slots.
 */
public class InventoryHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger("SLML:InventoryHelper");

    /**
     * Count how many of a specific item the player has in their inventory
     *
     * @param player The player to check
     * @param itemId The item ResourceLocation (e.g., "minecraft:wheat")
     * @return Total count of items found
     */
    public static int countItemInInventory(Player player, String itemId) {
        int count = 0;
        ResourceLocation target = ResourceLocation.parse(itemId);

        // Check main inventory
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty() && stack.getItem() == BuiltInRegistries.ITEM.get(target)) {
                count += stack.getCount();
            }
        }

        return count;
    }

    /**
     * Remove items from player inventory, scanning all slots
     *
     * @param player The player to remove
     * @param itemId The item to remove
     * @param amount How many to remove
     * @return True if successful, false if not enough items
     */
    public static boolean removeItemFromInventory(Player player, String itemId, int amount) {
        if (countItemInInventory(player, itemId) < amount) {
            return false; // Not enough items
        }

        int remaining = amount;
        ResourceLocation target = ResourceLocation.parse(itemId);

        // Remove from main inventory
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack stack = player.getInventory().getItem(i);

            if (!stack.isEmpty() && stack.getItem() == BuiltInRegistries.ITEM.get(target)) {
                int toRemove = Math.min(remaining, stack.getCount());
                stack.shrink(toRemove);
                remaining -= toRemove;

                if (stack.isEmpty()) {
                    player.getInventory().setItem(i, ItemStack.EMPTY);
                }
            }
        }

        return remaining == 0;
    }

    /**
     * Add items to player inventory, attempting to merge with existing stacks first
     *
     * @param player The player to add
     * @param itemStack The items to add (can be partial stack)
     * @return True if all items were added, false if inventory full
     */
    public static boolean addItemToInventory(Player player, ItemStack itemStack) {
        ItemStack remaining = itemStack.copy();

        // Try to add to existing stacks first
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack slot = player.getInventory().getItem(i);

            if (!slot.isEmpty() && ItemStack.isSameItemSameComponents(slot, remaining)) {
                int space = Math.min(slot.getMaxStackSize() - slot.getCount(), remaining.getCount());
                if (space > 0) {
                    slot.grow(space);
                    remaining.shrink(space);

                    if (remaining.isEmpty()) {
                        return true;
                    }
                }
            }
        }

        // Add to empty slots
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack slot = player.getInventory().getItem(i);

            if (slot.isEmpty()) {
                int toAdd = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                ItemStack added = remaining.copy();
                added.setCount(toAdd);
                player.getInventory().setItem(i, added);
                remaining.shrink(toAdd);

                if (remaining.isEmpty()) {
                    return true;
                }
            }
        }

        // If we get here, inventory is full
        return false;
    }

    /**
     * Get a detailed breakdown of where items are in inventory Useful for debugging or displaying to user
     */
    public static void logInventoryContents(Player player) {
        LOGGER.info("=== Inventory for {} ===", player.getName().getString());

        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.isEmpty()) {
                LOGGER.info("Slot {}: {} x{}", i, stack.getItem(), stack.getCount());
            }
        }
    }
}
