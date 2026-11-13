package com.github.syren_dev_tech.slml.builder.xml;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Extended layout loader that supports both JSON and XML formats. Automatically detects format and parses accordingly.
 */
public class XMLAwareLayoutLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger("SLML:XMLAwareLayoutLoader");
    private final XMLLayoutParser xmlParser;

    public XMLAwareLayoutLoader() {
        this.xmlParser = new XMLLayoutParser();
    }

    /**
     * Load a layout from resources, supporting both .json and .xml formats.
     *
     * @param resourceLocation The resource location (e.g., "slml:ui/trade_element_layout")
     * @return The parsed JsonObject, or null if not found
     */
    public JsonObject loadLayout(ResourceLocation resourceLocation) {
        // Try XML first, then fall back to JSON
        JsonObject layout = loadLayoutFromResource(resourceLocation, "xml");
        if (layout == null) {
            layout = loadLayoutFromResource(resourceLocation, "json");
        }
        return layout;
    }

    /**
     * Load a layout with specified format preference.
     *
     * @param resourceLocation The resource location
     * @param format The format ("xml" or "json")
     * @return The parsed JsonObject, or null if not found
     */
    public JsonObject loadLayoutFromResource(ResourceLocation resourceLocation, String format) {
        try {
            ClassLoader loader = XMLAwareLayoutLoader.class.getClassLoader();
            String resourcePath = String.format("data/%s/%s.%s", resourceLocation.getNamespace(), resourceLocation.getPath(), format);

            try (InputStream inputStream = loader.getResourceAsStream(resourcePath)) {
                if (inputStream != null) {
                    JsonObject root = parseLayout(inputStream, format);
                    LOGGER.debug("Loaded {} layout from resources: {}", format.toUpperCase(), resourceLocation);
                    return root;
                }
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to load {} layout from resources: {}", format, resourceLocation, e);
        }

        return null;
    }

    /**
     * Parse a layout from an input stream.
     *
     * @param inputStream The input stream to parse
     * @param format The format ("xml" or "json")
     * @return The parsed JsonObject, or null if parsing fails
     */
    private JsonObject parseLayout(InputStream inputStream, String format) {
        try {
            if ("xml".equalsIgnoreCase(format)) {
                return xmlParser.parseXmlStream(inputStream);
            } else {
                // Assume JSON
                try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                    com.google.gson.Gson gson = new com.google.gson.Gson();
                    return gson.fromJson(reader, JsonObject.class);
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to parse {} layout", format, e);
            return null;
        }
    }

    /**
     * Parse a layout from a string, auto-detecting format.
     *
     * @param layoutString The layout string to parse
     * @return The parsed JsonObject, or null if parsing fails
     */
    public JsonObject parseLayoutFromString(String layoutString) {
        String trimmed = layoutString.trim();

        if (trimmed.startsWith("<")) {
            // XML format
            return xmlParser.parseXmlString(layoutString);
        } else if (trimmed.startsWith("{")) {
            // JSON format
            com.google.gson.Gson gson = new com.google.gson.Gson();
            try {
                return gson.fromJson(layoutString, JsonObject.class);
            } catch (Exception e) {
                LOGGER.error("Failed to parse JSON layout", e);
                return null;
            }
        } else {
            LOGGER.warn("Unable to determine layout format from string");
            return null;
        }
    }

    /**
     * Set a placeholder value that will be used when parsing XML layouts.
     *
     * @param key The placeholder key
     * @param value The value to substitute for {{key}}
     */
    public void setPlaceholder(String key, Object value) {
        xmlParser.setPlaceholder(key, value);
    }

    /**
     * Set multiple placeholders at once.
     *
     * @param placeholders Map of placeholder keys to values
     */
    public void setPlaceholders(Map<String, Object> placeholders) {
        xmlParser.setPlaceholders(placeholders);
    }

    /**
     * Get the underlying XMLLayoutParser for advanced usage.
     */
    public XMLLayoutParser getXmlParser() {
        return xmlParser;
    }
}
