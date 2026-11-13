package com.github.syren_dev_tech.slml.builder;

import com.google.gson.JsonObject;

/**
 * Utility class for parsing dimensions from JSON objects. Handles both absolute pixel values and percentage values.
 */
public class DimensionParser {
    private DimensionParser() {
        /* This utility class should not be instantiated */
    }

    /**
     * Parse a dimension value from a JSON object. Looks for the given key and parses it as a dimension. Returns a default dimension if the key doesn't exist.
     *
     * @param json The JSON object to parse from
     * @param key The key to look for
     * @param defaultValue The default value if key is not present
     * @return A Dimension object
     */
    public static Dimension parseDimension(JsonObject json, String key, int defaultValue) {
        if (!json.has(key)) {
            return Dimension.parse(defaultValue);
        }

        try {
            Object value = null;

            // Handle both numeric and string representations
            if (json.get(key).isJsonPrimitive()) {
                var primitive = json.get(key).getAsJsonPrimitive();
                if (primitive.isNumber()) {
                    value = primitive.getAsNumber().intValue();
                } else if (primitive.isString()) {
                    value = primitive.getAsString();
                }
            }

            if (value != null) {
                return Dimension.parse(value);
            }

            return Dimension.parse(defaultValue);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse dimension for key '" + key + "'", e);
        }
    }

    /**
     * Parse all dimensional properties (x, y, width, height) from a JSON object.
     *
     * @param json The JSON object to parse from
     * @param defaultX Default X position
     * @param defaultY Default Y position
     * @param defaultWidth Default width
     * @param defaultHeight Default height
     * @return An array of [x, y, width, height] Dimensions
     */
    public static Dimension[] parseAllDimensions(JsonObject json, int defaultX, int defaultY, int defaultWidth, int defaultHeight) {
        return new Dimension[] {parseDimension(json, "x", defaultX), parseDimension(json, "y", defaultY), parseDimension(json, "width", defaultWidth), parseDimension(json, "height", defaultHeight)};
    }
}
