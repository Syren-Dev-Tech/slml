package com.github.syren_dev_tech.slml.builder;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating UI elements from JSON definitions.
 */
public class UIElementFactory {
    private UIElementFactory() {
        /* This utility class should not be instantiated */
    }

    private static final Logger LOGGER = LoggerFactory.getLogger("SLML:UIElementFactory");

    /**
     * Parse the ID from a JSON object and apply it to an element.
     */
    public static void applyIdToElement(JsonObject json, UIElement element) {
        if (json.has("id") && element instanceof BaseUIElement baseElement) {
            String id = json.get("id").getAsString();
            baseElement.setId(id);
        }
    }

    public static UIElement createElement(JsonObject json, Font font) {
        if (!json.has("type")) {
            LOGGER.warn("UI element missing 'type' property");
            return null;
        }

        String type = json.get("type").getAsString();

        try {
            UIElement element = switch (type) {
                case "text" -> TextElement.fromJson(json, font);
                case "button" -> ButtonElement.fromJson(json, font);
                case "texture" -> TextureElement.fromJson(json);
                case "item" -> ItemElement.fromJson(json, font);
                case "container" -> ContainerElement.fromJson(json, font);
                case "input_text" -> TextInputElement.fromJson(json, font);
                case "input_numeric" -> NumericInputElement.fromJson(json, font);
                case "input_checkbox" -> CheckboxElement.fromJson(json, font);
                default -> {
                    LOGGER.warn("Unknown UI element type: {}", type);
                    yield null;
                }
            };

            // Apply ID to element if present in JSON
            if (element != null && json.has("id")) {
                String id = json.get("id").getAsString();
                applyIdToElement(json, element);
                if (element instanceof BaseUIElement base && base.getId() != null && type.equals("button")) {
                    LOGGER.info("Button registered with id='{}'", id);
                }
            }

            return element;
        } catch (Exception e) {
            LOGGER.error("Error creating UI element of type '{}'", type, e);
            return null;
        }
    }
}
