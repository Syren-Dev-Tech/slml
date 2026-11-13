package com.github.syren_dev_tech.slml.builder;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Utility class for common UI Builder tasks and patterns.
 */
public class UIBuilderUtils {
    private UIBuilderUtils() {
        /* This utility class should not be instantiated */
    }


    /**
     * Create a placeholder map for trade information.
     */
    public static Map<String, String> createTradePlaceholders(String itemName, int itemCount, int emeraldCost, boolean isAvailable) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("item_name", itemName);
        placeholders.put("item_count", String.valueOf(itemCount));
        placeholders.put("price", String.valueOf(emeraldCost));
        placeholders.put("available", isAvailable ? "In Stock" : "Out of Stock");
        return placeholders;
    }

    /**
     * Create a placeholder map for player reputation info.
     */
    public static Map<String, String> createReputationPlaceholders(String playerName, float reputation, String reputationLevel) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player_name", playerName);
        placeholders.put("reputation", String.format("%.2f", reputation));
        placeholders.put("reputation_level", reputationLevel);
        return placeholders;
    }

    /**
     * Create a placeholder map for villager information.
     */
    public static Map<String, String> createVillagerPlaceholders(String villagerName, String profession, int tradeCount) {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("villager_name", villagerName);
        placeholders.put("profession", profession);
        placeholders.put("trade_count", String.valueOf(tradeCount));
        return placeholders;
    }

    /**
     * Find an element recursively in the UI tree by type.
     */
    public static <T extends UIElement> T findElementByType(ContainerElement container, Class<T> type) {
        for (UIElement child : container.getChildren()) {
            if (type.isInstance(child)) {
                return type.cast(child);
            }

            if (child instanceof ContainerElement subContainer) {
                T found = findElementByType(subContainer, type);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * Find all elements of a specific type in the UI tree.
     */
    public static <T extends UIElement> void findAllElementsByType(ContainerElement container, Class<T> type, java.util.List<T> results) {
        for (UIElement child : container.getChildren()) {
            if (type.isInstance(child)) {
                results.add(type.cast(child));
            }

            if (child instanceof ContainerElement subContainer) {
                findAllElementsByType(subContainer, type, results);
            }
        }
    }

    /**
     * Recursively apply a function to all elements in the UI tree.
     */
    public static void forEachElement(ContainerElement container, Consumer<UIElement> action) {
        for (UIElement child : container.getChildren()) {
            action.accept(child);

            if (child instanceof ContainerElement subContainer) {
                forEachElement(subContainer, action);
            }
        }
    }

    /**
     * Disable all buttons in a container tree.
     */
    public static void disableAllButtons(ContainerElement container) {
        java.util.List<ButtonElement> buttons = new java.util.ArrayList<>();
        findAllElementsByType(container, ButtonElement.class, buttons);

        for (ButtonElement button : buttons) {
            button.setActive(false);
        }
    }

    /**
     * Enable all buttons in a container tree.
     */
    public static void enableAllButtons(ContainerElement container) {
        java.util.List<ButtonElement> buttons = new java.util.ArrayList<>();
        findAllElementsByType(container, ButtonElement.class, buttons);

        for (ButtonElement button : buttons) {
            button.setActive(true);
        }
    }

    /**
     * Set all input fields to a specific value.
     */
    public static void setAllInputValues(ContainerElement container, String value) {
        java.util.List<InputElement> inputs = new java.util.ArrayList<>();
        findAllElementsByType(container, InputElement.class, inputs);

        for (InputElement input : inputs) {
            input.setValue(value);
        }
    }

    /**
     * Get all input values from a container.
     */
    public static Map<String, String> getAllInputValues(ContainerElement container) {
        Map<String, String> values = new HashMap<>();
        java.util.List<InputElement> inputs = new java.util.ArrayList<>();
        findAllElementsByType(container, InputElement.class, inputs);

        for (int i = 0; i < inputs.size(); i++) {
            values.put("input_" + i, inputs.get(i).getValue());
        }

        return values;
    }

    /**
     * Calculate the total height needed for container content.
     */
    public static int calculateContentHeight(ContainerElement container) {
        int maxHeight = 0;

        for (UIElement child : container.getChildren()) {
            int bottomEdge = child.getY() + child.getHeight();
            maxHeight = Math.max(maxHeight, bottomEdge);
        }

        return maxHeight;
    }

    /**
     * Center an element horizontally within a container.
     */
    public static void centerHorizontal(UIElement element, int containerWidth) {
        int centeredX = (containerWidth - element.getWidth()) / 2;
        element.setPosition(centeredX, element.getY());
    }

    /**
     * Center an element vertically within a container.
     */
    public static void centerVertical(UIElement element, int containerHeight) {
        int centeredY = (containerHeight - element.getHeight()) / 2;
        element.setPosition(element.getX(), centeredY);
    }

    /**
     * Center an element both horizontally and vertically.
     */
    public static void center(UIElement element, int containerWidth, int containerHeight) {
        centerHorizontal(element, containerWidth);
        centerVertical(element, containerHeight);
    }

    /**
     * Distribute child elements evenly in a container.
     */
    public static void distributeVertically(ContainerElement container, int spacing) {
        int currentY = 0;

        for (UIElement child : container.getChildren()) {
            child.setPosition(child.getX(), currentY);
            currentY += child.getHeight() + spacing;
        }
    }

    /**
     * Distribute child elements horizontally.
     */
    public static void distributeHorizontally(ContainerElement container, int spacing) {
        int currentX = 0;

        for (UIElement child : container.getChildren()) {
            child.setPosition(currentX, child.getY());
            currentX += child.getWidth() + spacing;
        }
    }

    /**
     * Create a colored text style.
     */
    public static TextStyle createColoredStyle(int color) {
        TextStyle style = new TextStyle();
        style.setColor(color);
        return style;
    }

    /**
     * Create a colored render style with border.
     */
    public static RenderStyle createStyledBorder(int fillColor, int borderColor, int borderWeight) {
        RenderStyle style = new RenderStyle();
        style.setFillColor(fillColor);
        style.setBorderColor(borderColor);
        style.setBorderWeight(borderWeight);
        return style;
    }

    /**
     * Validate that all required placeholders are set.
     */
    public static boolean validatePlaceholders(UIBuilder builder, String[] requiredPlaceholders) {
        for (String placeholder : requiredPlaceholders) {
            String value = builder.getContext().getPlaceholder(placeholder, null);
            if (value == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Create a simple dialog confirmation builder.
     */
    public static class DialogBuilder {
        private final String title;
        private String message;
        private String confirmLabel;
        private String cancelLabel;

        public DialogBuilder(String title) {
            this.title = title;
            this.message = "";
            this.confirmLabel = "Confirm";
            this.cancelLabel = "Cancel";
        }

        public DialogBuilder message(String message) {
            this.message = message;
            return this;
        }

        public DialogBuilder confirmLabel(String label) {
            this.confirmLabel = label;
            return this;
        }

        public DialogBuilder cancelLabel(String label) {
            this.cancelLabel = label;
            return this;
        }

        public Map<String, String> buildPlaceholders() {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("title", title);
            placeholders.put("message", message);
            placeholders.put("confirm_label", confirmLabel);
            placeholders.put("cancel_label", cancelLabel);
            return placeholders;
        }
    }
}
