package com.github.syren_dev_tech.slml.builder.xml;

import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.Gson;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.xml.sax.InputSource;

/**
 * Parses XML layout definitions and converts them to JSON format. Supports custom tags: <part> (container), <text>, <button>, <item>, <layout>, <datasource>, <repeated>
 *
 * Features: • Flex layout system for responsive UI • Placeholder substitution from Java and data sources • Datasource declaration for data pack JSON loading (NO JAVA CODE CHANGES NEEDED!) • Modular component inclusion via <layout> elements • Dynamic content generation via <repeated> elements
 */
public class XMLLayoutParser {
    private static final Logger LOGGER = LoggerFactory.getLogger("SLML:XMLLayoutParser");
    private static final Gson GSON = new Gson();
    private static final ClassLoader RESOURCE_LOADER = XMLLayoutParser.class.getClassLoader();

    // Static caches to prevent redundant file I/O during repeated layout rendering
    private static final Map<String, JsonObject> LAYOUT_CACHE = new HashMap<>();
    private static final Map<String, JsonArray> JSON_FILE_CACHE = new HashMap<>();

    private final Map<String, Object> placeholders; // Runtime values from Java
    private final Map<String, String> datasourcePaths; // id -> file path mappings
    private final Map<String, JsonArray> loadedDatasources; // id -> loaded data cache

    public XMLLayoutParser() {
        this.placeholders = new HashMap<>();
        this.datasourcePaths = new HashMap<>();
        this.loadedDatasources = new HashMap<>();
    }

    /**
     * Set a placeholder value that will be replaced in {{key}} expressions. Use this for runtime values like villager_name, profession, etc.
     *
     * Do NOT use this for data file paths - use <datasource> elements in XML instead!
     */
    public void setPlaceholder(String key, Object value) {
        this.placeholders.put(key, value);
    }

    /**
     * Set multiple placeholders at once. Use this for runtime values from Java code.
     *
     * Do NOT include data file paths here - they belong in XML <datasource> elements!
     */
    public void setPlaceholders(Map<String, Object> placeholders) {
        this.placeholders.putAll(placeholders);
    }

    /**
     * Get a loaded datasource by id. Datasources are declared in XML via <datasource> elements.
     *
     * @param datasourceId The id of the datasource
     * @return The loaded JSON array, or null if not found/not loaded
     */
    public JsonArray getDatasource(String datasourceId) {
        return this.loadedDatasources.get(datasourceId);
    }

    /**
     * Parse XML layout from a string.
     *
     * @param xmlString The XML string to parse
     * @return JsonObject representing the layout, or null if parsing fails
     */
    public JsonObject parseXmlString(String xmlString) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            InputSource source = new InputSource(new StringReader(xmlString));
            Document doc = builder.parse(source);

            Element rootElement = doc.getDocumentElement();
            JsonObject rootJson = parseElement(rootElement);

            LOGGER.debug("Successfully parsed XML layout");
            return rootJson;
        } catch (Exception e) {
            LOGGER.error("Failed to parse XML layout", e);
            return null;
        }
    }

    /**
     * Parse XML layout from an InputStream.
     *
     * @param inputStream The input stream to parse
     * @return JsonObject representing the layout, or null if parsing fails
     */
    public JsonObject parseXmlStream(InputStream inputStream) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(inputStream);

            Element rootElement = doc.getDocumentElement();
            JsonObject rootJson = parseElement(rootElement);

            LOGGER.debug("Successfully parsed XML layout from stream");
            return rootJson;
        } catch (Exception e) {
            LOGGER.error("Failed to parse XML layout from stream", e);
            return null;
        }
    }

    /**
     * Parse a single XML element into JsonObject format. Handles special tags like <layout>, <datasource>, and <repeated>.
     */
    private JsonObject parseElement(Element element) throws SAXException {
        String tagName = element.getTagName().toLowerCase();

        // Handle <layout> tag specially - it includes another layout file
        // Supports formats: "namespace:path" (e.g., "my_mod:ui/layout") or legacy "filename" (e.g., "trade_item")
        if ("layout".equals(tagName)) {
            String layoutRef = element.getTextContent().trim();
            if (!layoutRef.isEmpty()) {
                return parseLayoutFile(layoutRef);
            } else {
                LOGGER.warn("<layout> tag has no layout reference");
                return null;
            }
        }

        // Handle <datasource> tag - declare data sources
        if ("datasource".equals(tagName)) {
            parseDatasourceElement(element);
            return null; // Datasources don't create UI elements
        }

        JsonObject json = new JsonObject();
        String type = mapTagToType(tagName);
        json.addProperty("type", type);

        // Parse attributes
        parseAttributes(element, json);

        // Store flex-grow separately from flex attributes (it goes on the element, not parent flexLayout)
        int flexGrow = 0;
        if (json.has("flex-grow")) {
            flexGrow = json.get("flex-grow").getAsInt();
            json.remove("flex-grow");
        }
        if (flexGrow > 0) {
            json.addProperty("flexGrow", flexGrow);
            LOGGER.debug("Element flex-grow set to: {}", flexGrow);
        }

        // Convert flex and align attributes to flexLayout object for containers
        if (json.has("flex") || json.has("align")) {
            String direction = json.has("flex") ? json.get("flex").getAsString().trim() : "column";
            String justify = json.has("align") ? json.get("align").getAsString().trim() : "flex-start";

            // Remove original attributes
            json.remove("flex");
            json.remove("align");

            JsonObject flexLayout = new JsonObject();
            flexLayout.addProperty("direction", direction);
            flexLayout.addProperty("justify", justify);
            flexLayout.addProperty("alignItems", "flex-start");
            flexLayout.addProperty("gap", 0);
            json.add("flexLayout", flexLayout);
            LOGGER.debug("Container flex/align attributes converted: direction={}, justify={}", direction, justify);
        }

        // Parse text content and children
        if ("text".equals(type) || "item".equals(type) || "button".equals(type)) {
            // For text, item, and button elements, extract text content
            String textContent = element.getTextContent().trim();
            if ("text".equals(type)) {
                if (!textContent.isEmpty()) {
                    json.addProperty("text", replacePlaceholders(textContent));
                }
            } else if ("item".equals(type)) {
                // Item content should be a Minecraft item ID (e.g., minecraft:diamond)
                // Always set the property, even if empty (for error handling)
                // Apply placeholder replacement to support {{placeholders}}
                String itemId = replacePlaceholders(textContent);
                json.addProperty("item", itemId);
                LOGGER.debug("Item element set to: {}", itemId);
            } else if ("button".equals(type)) {
                if (!textContent.isEmpty()) {
                    json.addProperty("label", replacePlaceholders(textContent));
                }
            }
        } else {
            // For container types, parse child elements
            JsonArray children = new JsonArray();
            NodeList childNodes = element.getChildNodes();

            for (int i = 0; i < childNodes.getLength(); i++) {
                org.w3c.dom.Node node = childNodes.item(i);
                if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    Element childElement = (Element) node;
                    String childTagName = childElement.getTagName().toLowerCase();

                    if ("datasource".equals(childTagName)) {
                        // Parse datasource declarations (don't add as child)
                        parseDatasourceElement(childElement);
                    } else if ("repeated".equals(childTagName)) {
                        // Handle <repeated> elements
                        handleRepeatedElement(childElement, children);
                    } else if ("layout".equals(childTagName)) {
                        // Handle <layout> elements with namespace:path format
                        String layoutRef = childElement.getTextContent().trim();
                        if (!layoutRef.isEmpty()) {
                            JsonObject layoutJson = parseLayoutFile(layoutRef);
                            if (layoutJson != null) {
                                children.add(layoutJson);
                            }
                        }
                    } else {
                        // Regular element
                        JsonObject childJson = parseElement(childElement);
                        if (childJson != null) {
                            children.add(childJson);
                        }
                    }
                }
            }

            if (children.size() > 0) {
                json.add("children", children);
            }
        }

        return json;
    }

    /**
     * Parse a datasource element declaration. Datasources define where data comes from - NO JAVA CODE CHANGES NEEDED!
     *
     * Supports two path formats:
     * 1. "namespace:path" (e.g., "my_mod:lists/trades.json") - resolves to data/my_mod/lists/trades.json
     * 2. "path" (e.g., "slml/lists/trades.json") - LEGACY, resolves to data/slml/lists/trades.json
     *
     * Format: <datasource id="trades" path="my_mod:lists/trades.json" format="json-array" cache="true"/>
     */
    private void parseDatasourceElement(Element datasourceElement) {
        String id = datasourceElement.getAttribute("id");
        String pathRef = datasourceElement.getAttribute("path");
        String formatAttr = datasourceElement.getAttribute("format");
        String cacheAttr = datasourceElement.getAttribute("cache");

        if (id == null || id.isEmpty()) {
            LOGGER.warn("<datasource> missing required 'id' attribute");
            return;
        }

        if (pathRef == null || pathRef.isEmpty()) {
            LOGGER.warn("<datasource> missing required 'path' attribute");
            return;
        }

        String format = (formatAttr != null && !formatAttr.isEmpty()) ? formatAttr : "json-array";
        boolean cache = cacheAttr == null || cacheAttr.isEmpty() || !"false".equalsIgnoreCase(cacheAttr);

        LOGGER.info("Declared datasource '{}' at path '{}' (format: {}, cache: {})", id, pathRef, format, cache);

        // Store path for later loading
        this.datasourcePaths.put(id, pathRef);

        // Load data if not already cached
        if (!this.loadedDatasources.containsKey(id)) {
            JsonArray data = loadJsonFile(pathRef);
            if (data != null) {
                this.loadedDatasources.put(id, data);
                LOGGER.debug("Loaded datasource '{}' with {} items", id, data.size());
            } else {
                LOGGER.error("Failed to load datasource '{}' from path '{}'", id, pathRef);
            }
        }
    }

    /**
     * Handle <repeated> elements which can dynamically generate children.
     *
     * NEW V2.0: Uses datasource attribute instead of dict placeholder Example: <repeated datasource="trades_data"> <layout>trade_item</layout> </repeated>
     *
     * LEGACY V1.0: Still supports dict attribute for backward compatibility Example: <repeated dict="{{available_trades}}"> <layout>trade_item</layout> </repeated>
     */
    private void handleRepeatedElement(Element repeatedElement, JsonArray childrenArray) throws SAXException {
        // Try new datasource attribute first
        String datasourceId = repeatedElement.getAttribute("datasource");

        if (datasourceId != null && !datasourceId.isEmpty()) {
            // NEW V2.0 WAY - Use datasource reference
            handleRepeatedWithDatasource(repeatedElement, childrenArray, datasourceId);
            return;
        }

        // Fall back to legacy dict attribute
        String dictAttr = repeatedElement.getAttribute("dict");
        if (dictAttr != null && !dictAttr.isEmpty()) {
            LOGGER.debug("Using legacy 'dict' attribute - consider migrating to <datasource>");
            handleRepeatedWithDict(repeatedElement, childrenArray, dictAttr);
            return;
        }

        LOGGER.warn("<repeated> element must have either 'datasource' or 'dict' attribute");
    }

    /**
     * Handle <repeated> with datasource attribute (v2.0 way - RECOMMENDED). References a <datasource> element by id.
     */
    private void handleRepeatedWithDatasource(Element repeatedElement, JsonArray childrenArray, String datasourceId) throws SAXException {
        JsonArray datasourceData = this.loadedDatasources.get(datasourceId);

        if (datasourceData == null) {
            LOGGER.error("Datasource '{}' not found or not loaded", datasourceId);
            return;
        }

        if (datasourceData.size() == 0) {
            LOGGER.debug("Datasource '{}' is empty, no items to repeat", datasourceId);
            return;
        }

        LOGGER.debug("Processing repeated element with datasource '{}' containing {} items", datasourceId, datasourceData.size());

        // Get child elements of <repeated> (should contain <layout> tags)
        NodeList childNodes = repeatedElement.getChildNodes();

        for (JsonElement item : datasourceData) {
            // For each item in the array, process all child elements
            for (int i = 0; i < childNodes.getLength(); i++) {
                org.w3c.dom.Node node = childNodes.item(i);
                if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    Element childElement = (Element) node;
                    String childTagName = childElement.getTagName().toLowerCase();

                    // Create a new parser with datasource item placeholders
                    XMLLayoutParser itemParser = new XMLLayoutParser();
                    itemParser.placeholders.putAll(this.placeholders);
                    itemParser.datasourcePaths.putAll(this.datasourcePaths);
                    itemParser.loadedDatasources.putAll(this.loadedDatasources);

                    // Add item properties as placeholders
                    if (item.isJsonObject()) {
                        JsonObject itemObj = item.getAsJsonObject();
                        for (String key : itemObj.keySet()) {
                            JsonElement value = itemObj.get(key);
                            if (value == null) {
                                LOGGER.warn("Null value for placeholder key: {}", key);
                                itemParser.placeholders.put(key, "");
                                continue;
                            }
                            // Extract actual value from JsonElement
                            if (value.isJsonPrimitive()) {
                                String strValue = value.getAsString();
                                itemParser.placeholders.put(key, strValue);
                                LOGGER.debug("Placeholder: {} = {}", key, strValue);
                            } else if (value.isJsonObject()) {
                                String strValue = value.getAsJsonObject().toString();
                                itemParser.placeholders.put(key, strValue);
                                LOGGER.debug("Placeholder (object): {} = {}", key, strValue);
                            } else if (value.isJsonArray()) {
                                String strValue = value.getAsJsonArray().toString();
                                itemParser.placeholders.put(key, strValue);
                                LOGGER.debug("Placeholder (array): {} = {}", key, strValue);
                            } else {
                                LOGGER.warn("Unknown JsonElement type for key {}: {}", key, value.getClass());
                            }
                        }
                    }

                    // Parse child element with item placeholders
                    if ("layout".equals(childTagName)) {
                        String layoutFileName = childElement.getTextContent().trim();
                        if (!layoutFileName.isEmpty()) {
                            JsonObject layoutJson = itemParser.parseLayoutFile(layoutFileName);
                            if (layoutJson != null) {
                                childrenArray.add(layoutJson);
                            }
                        }
                    } else {
                        JsonObject childJson = itemParser.parseElement(childElement);
                        if (childJson != null) {
                            childrenArray.add(childJson);
                        }
                    }
                }
            }
        }
    }

    /**
     * Handle <repeated> with dict attribute (v1.0 legacy way - DEPRECATED). Kept for backward compatibility but should migrate to datasource.
     */
    private void handleRepeatedWithDict(Element repeatedElement, JsonArray childrenArray, String dictAttr) throws SAXException {
        // First, replace placeholders in the dict attribute
        String processedDict = replacePlaceholders(dictAttr);
        LOGGER.debug("Processing repeated with dict: {}", processedDict);

        // Try to parse as JSON first
        JsonArray dictArray = null;
        try {
            dictArray = GSON.fromJson(processedDict, JsonArray.class);
        } catch (Exception e) {
            // Not valid JSON, try as file path
            LOGGER.debug("Dict is not JSON, attempting to load from file: {}", processedDict);
            dictArray = loadJsonFile(processedDict);
        }

        if (dictArray != null && dictArray.size() > 0) {
            LOGGER.debug("Loaded {} items from dict", dictArray.size());

            // Get child elements of <repeated> (should contain <layout> tags)
            NodeList childNodes = repeatedElement.getChildNodes();

            for (JsonElement item : dictArray) {
                // For each item in the array, process all child elements
                for (int i = 0; i < childNodes.getLength(); i++) {
                    org.w3c.dom.Node node = childNodes.item(i);
                    if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                        Element childElement = (Element) node;
                        String childTagName = childElement.getTagName().toLowerCase();

                        // Create a new parser with dict item placeholders
                        XMLLayoutParser itemParser = new XMLLayoutParser();
                        itemParser.placeholders.putAll(this.placeholders);

                        // Add item properties as placeholders
                        if (item.isJsonObject()) {
                            JsonObject itemObj = item.getAsJsonObject();
                            for (String key : itemObj.keySet()) {
                                JsonElement value = itemObj.get(key);
                                if (value == null) {
                                    LOGGER.warn("Null value for placeholder key: {}", key);
                                    itemParser.placeholders.put(key, "");
                                    continue;
                                }
                                // Extract actual value from JsonElement
                                if (value.isJsonPrimitive()) {
                                    String strValue = value.getAsString();
                                    itemParser.placeholders.put(key, strValue);
                                    LOGGER.debug("Placeholder: {} = {}", key, strValue);
                                } else if (value.isJsonObject()) {
                                    String strValue = value.getAsJsonObject().toString();
                                    itemParser.placeholders.put(key, strValue);
                                    LOGGER.debug("Placeholder (object): {} = {}", key, strValue);
                                } else if (value.isJsonArray()) {
                                    String strValue = value.getAsJsonArray().toString();
                                    itemParser.placeholders.put(key, strValue);
                                    LOGGER.debug("Placeholder (array): {} = {}", key, strValue);
                                } else {
                                    LOGGER.warn("Unknown JsonElement type for key {}: {}", key, value.getClass());
                                }
                            }
                        }

                        // Parse child element with item placeholders
                        if ("layout".equals(childTagName)) {
                            String layoutRef = childElement.getTextContent().trim();
                            if (!layoutRef.isEmpty()) {
                                JsonObject layoutJson = itemParser.parseLayoutFile(layoutRef);
                                if (layoutJson != null) {
                                    childrenArray.add(layoutJson);
                                }
                            }
                        } else {
                            JsonObject childJson = itemParser.parseElement(childElement);
                            if (childJson != null) {
                                childrenArray.add(childJson);
                            }
                        }
                    }
                }
            }
        } else {
            LOGGER.error("Failed to parse dict attribute or dict is empty: {}", processedDict);
        }
    }

    /**
     * Load a JSON file from resources and parse it into a JsonArray. Supports namespace-based file references.
     *
     * Supports two path formats:
     * 1. "namespace:path" (e.g., "my_mod:lists/trades.json") - resolves to data/my_mod/lists/trades.json
     * 2. "path" (e.g., "slml/lists/trades.json") - LEGACY, resolves to data/slml/lists/trades.json
     *
     * @param fileRef File reference in format "namespace:path" or legacy path format
     * @return JsonArray loaded from the file, or null if not found/not an array
     */
    private JsonArray loadJsonFile(String fileRef) {
        if (fileRef == null || fileRef.trim().isEmpty()) {
            return null;
        }

        String ref = fileRef.trim();
        String normalizedPath;

        // Check if using new namespace:path format
        if (ref.contains(":")) {
            String[] parts = ref.split(":", 2);
            String namespace = parts[0].trim();
            String path = parts[1].trim();

            if (namespace.isEmpty() || path.isEmpty()) {
                LOGGER.error("Invalid datasource path format: {}. Expected 'namespace:path'", ref);
                return null;
            }

            // Construct resource path from namespace:path
            normalizedPath = "data/" + namespace + "/" + path;
            LOGGER.debug("Resolved datasource reference '{}' to resource path: {}", ref, normalizedPath);
        } else {
            // LEGACY: Simple path, ensure it starts with "data/"
            if (!ref.startsWith("data/")) {
                normalizedPath = "data/" + ref;
            } else {
                normalizedPath = ref;
            }
            LOGGER.debug("Using legacy datasource path '{}', resolved to: {}", ref, normalizedPath);
        }

        // Check global cache first
        if (JSON_FILE_CACHE.containsKey(normalizedPath)) {
            LOGGER.debug("Using cached JSON from: {}", normalizedPath);
            return JSON_FILE_CACHE.get(normalizedPath);
        }

        try {
            try (InputStream inputStream = RESOURCE_LOADER.getResourceAsStream(normalizedPath)) {
                if (inputStream != null) {
                    try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                        JsonElement element = GSON.fromJson(reader, JsonElement.class);
                        if (element != null && element.isJsonArray()) {
                            JsonArray result = element.getAsJsonArray();
                            JSON_FILE_CACHE.put(normalizedPath, result);
                            LOGGER.debug("Loaded and cached JSON array from: {}", normalizedPath);
                            return result;
                        } else if (element != null && element.isJsonObject()) {
                            LOGGER.warn("JSON file is object, not array: {}", normalizedPath);
                            return null;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load JSON file: {}", normalizedPath, e);
        }

        LOGGER.warn("JSON file not found or could not be parsed: {}", normalizedPath);
        return null;
    }

    /**
     * Load an XML layout file from resources and parse it. Maintains placeholder context from the parent parser. Uses static cache to avoid redundant file I/O.
     *
     * Supports two formats:
     * 1. "namespace:path" (e.g., "my_mod:ui/layout") - resolves to data/my_mod/ui/layout.xml
     * 2. "filename" (e.g., "trade_item") - LEGACY, resolves to data/slml/ui/trade_item.xml
     *
     * @param layoutRef The layout reference in format "namespace:path" or just a filename
     * @return JsonObject representing the layout, or null if not found
     */
    private JsonObject parseLayoutFile(String layoutRef) {
        if (layoutRef == null || layoutRef.trim().isEmpty()) {
            return null;
        }

        String ref = layoutRef.trim();
        String resourcePath;

        // Check if using new namespace:path format
        if (ref.contains(":")) {
            String[] parts = ref.split(":", 2);
            String namespace = parts[0].trim();
            String path = parts[1].trim();

            if (namespace.isEmpty() || path.isEmpty()) {
                LOGGER.error("Invalid layout reference format: {}. Expected 'namespace:path'", ref);
                return null;
            }

            // Construct resource path from namespace:path
            if (!path.endsWith(".xml")) {
                path += ".xml";
            }
            resourcePath = "data/" + namespace + "/" + path;
            LOGGER.debug("Resolved layout reference '{}' to resource path: {}", ref, resourcePath);
        } else {
            // LEGACY: Simple filename, resolve to default slml/ui path
            String fileName = ref;
            if (!fileName.endsWith(".xml")) {
                fileName += ".xml";
            }
            resourcePath = "data/slml/ui/" + fileName;
            LOGGER.debug("Using legacy layout filename '{}', resolved to: {}", ref, resourcePath);
        }

        // Check cache first - return cached layout with placeholder applied
        if (LAYOUT_CACHE.containsKey(resourcePath)) {
            LOGGER.debug("Using cached layout from: {}", resourcePath);
            JsonObject cachedLayout = LAYOUT_CACHE.get(resourcePath);
            // Apply this parser's placeholders to the cached layout
            return applyPlaceholdersToJson(cachedLayout);
        }

        try {
            try (InputStream inputStream = RESOURCE_LOADER.getResourceAsStream(resourcePath)) {
                if (inputStream != null) {
                    // Create a child parser that inherits placeholders and datasources from parent
                    XMLLayoutParser childParser = new XMLLayoutParser();
                    childParser.placeholders.putAll(this.placeholders);
                    childParser.datasourcePaths.putAll(this.datasourcePaths);
                    childParser.loadedDatasources.putAll(this.loadedDatasources);

                    JsonObject layoutJson = childParser.parseXmlStream(inputStream);
                    if (layoutJson != null) {
                        // Cache the parsed layout for future use
                        LAYOUT_CACHE.put(resourcePath, layoutJson);
                        LOGGER.debug("Loaded and cached layout from file: {}", resourcePath);
                        return layoutJson;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load layout file: {}", resourcePath, e);
        }

        LOGGER.warn("Layout file not found: {}", resourcePath);
        return null;
    }

    /**
     * Map XML tag names to UI element types.
     */
    private String mapTagToType(String tagName) {
        return switch (tagName) {
            case "part" -> "container";
            case "text" -> "text";
            case "button" -> "button";
            case "item" -> "item";
            default -> "container";
        };
    }

    /**
     * Parse XML attributes into JsonObject properties.
     */
    private void parseAttributes(Element element, JsonObject json) {
        org.w3c.dom.NamedNodeMap attributes = element.getAttributes();

        for (int i = 0; i < attributes.getLength(); i++) {
            org.w3c.dom.Attr attr = (org.w3c.dom.Attr) attributes.item(i);
            String name = attr.getName().toLowerCase();
            String value = attr.getValue();

            // Replace placeholders in attribute values
            value = replacePlaceholders(value);

            // Handle special attributes
            switch (name) {
                case "x", "y", "width", "height" -> {
                    // Try to parse as number, otherwise keep as string
                    if (isNumericOrPercentage(value)) {
                        if (value.endsWith("%")) {
                            json.addProperty(name, value);
                        } else {
                            try {
                                json.addProperty(name, Integer.parseInt(value));
                            } catch (NumberFormatException e) {
                                json.addProperty(name, value);
                            }
                        }
                    } else {
                        json.addProperty(name, value);
                    }
                }
                case "style" -> {
                    // Parse semicolon-separated style string into JSON object
                    JsonObject styleObj = parseStyleString(value);
                    if (styleObj != null && styleObj.size() > 0) {
                        json.add("style", styleObj);
                    }
                }
                case "id", "flex", "align", "flex-grow", "name", "vanilla" -> json.addProperty(name, value);
                default -> json.addProperty(name, value);
            }
        }
    }

    /**
     * Check if a string represents a number or percentage.
     */
    private boolean isNumericOrPercentage(String value) {
        if (value.endsWith("%")) {
            return true;
        }
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException e) {
            try {
                Double.parseDouble(value);
                return true;
            } catch (NumberFormatException e2) {
                return false;
            }
        }
    }

    /**
     * Replace all {{key}} placeholders in a string with their values.
     *
     * Placeholder Resolution Order: 1. Check runtime placeholders (from Java code) 2. Check datasource item properties (if in repeated context) 3. If not found, mark as MISSING_PLACEHOLDER
     */
    private String replacePlaceholders(String text) {
        if (text == null || !text.contains("{{")) {
            return text;
        }

        String result = text;
        int startIdx = 0;
        boolean replacementMade = false;

        while ((startIdx = result.indexOf("{{", startIdx)) != -1) {
            int endIdx = result.indexOf("}}", startIdx);
            if (endIdx == -1)
                break;

            String key = result.substring(startIdx + 2, endIdx);
            String replacement;

            if (placeholders.containsKey(key)) {
                Object placeholderValue = placeholders.get(key);
                replacement = String.valueOf(placeholderValue);
                LOGGER.debug("Replacing placeholder {{{}}} with value: {}", key, replacement);
                replacementMade = true;
            } else {
                replacement = "MISSING_PLACEHOLDER:" + key;
                LOGGER.warn("Placeholder key not found: {}. Available keys: {}", key, placeholders.keySet());
            }

            result = result.substring(0, startIdx) + replacement + result.substring(endIdx + 2);
            startIdx += replacement.length();
        }

        if (replacementMade) {
            LOGGER.debug("Placeholder replacement result: {} -> {}", text, result);
        }

        return result;
    }

    /**
     * Apply this parser's placeholders to a cached layout by replacing placeholders in the JSON structure. Creates a deep copy to avoid modifying the cached version.
     */
    private JsonObject applyPlaceholdersToJson(JsonObject original) {
        // Deep copy the JSON object
        String json = original.toString();
        JsonObject copy = GSON.fromJson(json, JsonObject.class);

        // Replace all placeholders in the copy
        replaceJsonPlaceholders(copy);

        return copy;
    }

    /**
     * Recursively replace placeholders in JSON structure.
     */
    private void replaceJsonPlaceholders(JsonElement element) {
        if (element.isJsonObject()) {
            JsonObject obj = element.getAsJsonObject();
            for (String key : new java.util.HashSet<>(obj.keySet())) {
                JsonElement val = obj.get(key);
                if (val.isJsonPrimitive() && val.getAsJsonPrimitive().isString()) {
                    String str = val.getAsString();
                    String replaced = replacePlaceholders(str);
                    if (!str.equals(replaced)) {
                        obj.addProperty(key, replaced);
                    }
                } else if (val.isJsonObject() || val.isJsonArray()) {
                    replaceJsonPlaceholders(val);
                }
            }
        } else if (element.isJsonArray()) {
            JsonArray arr = element.getAsJsonArray();
            for (int i = 0; i < arr.size(); i++) {
                replaceJsonPlaceholders(arr.get(i));
            }
        }
    }

    /**
     * Parse a semicolon-separated style string into a JsonObject. Format: "backgroundColor:#FFFFFF;color:#000000;fontSize:12;padding:4"
     *
     * Supported properties: • Colors: backgroundColor, borderColor, color (#RRGGBB) • Sizing: fontSize, padding, margin, borderWidth (integers) • Text: textAlign, fontWeight, fontStyle (strings)
     *
     * @param styleString The style string to parse
     * @return JsonObject with parsed styles, or null if empty
     */
    private JsonObject parseStyleString(String styleString) {
        if (styleString == null || styleString.trim().isEmpty()) {
            return null;
        }

        JsonObject styleObj = new JsonObject();
        String[] properties = styleString.split(";");

        for (String property : properties) {
            String trimmed = property.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            int colonIdx = trimmed.indexOf(":");
            if (colonIdx <= 0) {
                LOGGER.warn("Invalid style property (missing colon): {}", trimmed);
                continue;
            }

            String key = trimmed.substring(0, colonIdx).trim();
            String value = trimmed.substring(colonIdx + 1).trim();

            if (key.isEmpty() || value.isEmpty()) {
                LOGGER.warn("Invalid style property (empty key or value): {}", trimmed);
                continue;
            }

            // Try to parse as number, otherwise keep as string
            try {
                if (value.contains(".")) {
                    styleObj.addProperty(key, Double.parseDouble(value));
                } else {
                    styleObj.addProperty(key, Integer.parseInt(value));
                }
            } catch (NumberFormatException e) {
                // Not a number, treat as string
                styleObj.addProperty(key, value);
            }
        }

        return styleObj.size() > 0 ? styleObj : null;
    }
}
