package com.github.syren_dev_tech.slml.builder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Main UI builder that loads and manages UI layouts from JSON definitions. Supports template placeholders and responsive rendering.
 */
public class UIBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger("VillagerWorkshopsUI");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ContainerElement rootContainer;
    private UIContext context;
    private final Font font;
    private final Map<String, UIElement> elementRegistry;
    private final UIDebugger debugger;

    public UIBuilder(Font font) {
        this.font = font;
        this.elementRegistry = new HashMap<>();
        this.debugger = new UIDebugger(this);
    }

    /**
     * Load a UI definition from a data pack JSON file. Supports both packaged resources and data pack overrides.
     *
     * @param resourceLocation The resource location of the JSON file
     * @param screenWidth The screen width
     * @param screenHeight The screen height
     * @param guiScale The GUI scale
     * @return true if loaded successfully
     */
    public boolean loadFromDatapack(ResourceLocation resourceLocation, int screenWidth, int screenHeight, double guiScale) {
        try {
            // Try to load from classpath resources first (packaged defaults)
            ClassLoader loader = UIBuilder.class.getClassLoader();
            String resourcePath = String.format("data/%s/%s.json", resourceLocation.getNamespace(), resourceLocation.getPath());

            if (loadFromClasspathResources(loader, resourcePath, resourceLocation, screenWidth, screenHeight, guiScale)) {
                return true;
            }

            // If not found in resources, this is expected - data packs should provide the file
            LOGGER.warn("UI definition not found in resources: {}. Data pack must provide this file at: data/{}/{}.json", resourceLocation, resourceLocation.getNamespace(), resourceLocation.getPath());
            return false;
        } catch (Exception e) {
            LOGGER.error("Failed to load UI definition from {}", resourceLocation, e);
            return false;
        }
    }

    private boolean loadFromClasspathResources(ClassLoader loader, String resourcePath, ResourceLocation resourceLocation, int screenWidth, int screenHeight, double guiScale) {
        try (Reader reader = new InputStreamReader(loader.getResourceAsStream(resourcePath), StandardCharsets.UTF_8)) {
            if (reader != null) {
                JsonObject root = GSON.fromJson(reader, JsonObject.class);
                LOGGER.info("Loaded UI definition from resources: {}", resourceLocation);
                return loadFromJson(root, screenWidth, screenHeight, guiScale);
            }
        } catch (Exception e) {
            LOGGER.debug("Failed to load UI from classpath resources: {}", resourceLocation, e);
        }
        return false;
    }

    /**
     * Load a UI definition from a data pack JSON file via ResourceManager. This allows data packs to override or provide UI layouts.
     *
     * @param resourceManager The Minecraft ResourceManager
     * @param resourceLocation The resource location of the JSON file
     * @param screenWidth The screen width
     * @param screenHeight The screen height
     * @param guiScale The GUI scale
     * @return true if loaded successfully
     */
    public boolean loadFromResourceManager(Object resourceManager, ResourceLocation resourceLocation, int screenWidth, int screenHeight, double guiScale) {
        try {
            // This method accepts Object type to avoid hard dependency on ResourceManager
            // It will be called from VillagerTradeScreen with Minecraft's ResourceManager
            // For now, we delegate to loadFromDatapack which uses classpath resources
            LOGGER.info("Loading UI definition via ResourceManager: {}", resourceLocation);
            return loadFromDatapack(resourceLocation, screenWidth, screenHeight, guiScale);
        } catch (Exception e) {
            LOGGER.error("Failed to load UI from ResourceManager: {}", resourceLocation, e);
            return false;
        }
    }

    /**
     * Load UI from a JSON string.
     *
     * @param json The JSON string
     * @param screenWidth The screen width
     * @param screenHeight The screen height
     * @param guiScale The GUI scale
     * @return true if loaded successfully
     */
    public boolean loadFromJson(String json, int screenWidth, int screenHeight, double guiScale) {
        LOGGER.debug("Loading UI from JSON string ({}x{})", screenWidth, screenHeight);
        try {
            JsonObject root = GSON.fromJson(json, JsonObject.class);
            if (root == null) {
                LOGGER.error("Failed to parse UI JSON: root is null");
                return false;
            }
            LOGGER.debug("JSON parsed successfully");
            return loadFromJson(root, screenWidth, screenHeight, guiScale);
        } catch (Exception e) {
            LOGGER.error("Failed to parse UI JSON", e);
            return false;
        }
    }

    /**
     * Load UI from a parsed JSON object.
     */
    public boolean loadFromJson(JsonObject root, int screenWidth, int screenHeight, double guiScale) {
        try {
            LOGGER.info("=== LOADING UI LAYOUT ===");
            LOGGER.info("Screen size: {}x{}", screenWidth, screenHeight);
            LOGGER.info("GUI scale: {}", guiScale);

            elementRegistry.clear();
            this.context = new UIContext(screenWidth, screenHeight, guiScale);
            LOGGER.debug("Created UI context");

            // Load root container
            LOGGER.debug("Creating root container from JSON");
            this.rootContainer = ContainerElement.fromJson(root, font);

            if (rootContainer == null) {
                LOGGER.error("Failed to create root container from JSON");
                return false;
            }

            // CRITICAL: Set parent dimensions on root BEFORE resolving percentages
            // This allows "100%" width/height to resolve correctly
            LOGGER.debug("Setting root container parent dimensions to screen size: {}x{}", screenWidth, screenHeight);
            rootContainer.setParentDimensions(screenWidth, screenHeight);

            LOGGER.debug("Root container created: {}x{}", rootContainer.getWidth(), rootContainer.getHeight());

            // Set root container to screen size (unless explicitly specified in JSON)
            // This ensures the UI fills the screen and is responsive
            int rootWidth = rootContainer.getWidth();
            int rootHeight = rootContainer.getHeight();

            LOGGER.info("Root container: initial size {}x{}, screen size {}x{}", rootWidth, rootHeight, screenWidth, screenHeight);

            // If root container size is 0 or doesn't match screen, use screen size
            if (rootWidth <= 0 || rootWidth != screenWidth || rootHeight <= 0 || rootHeight != screenHeight) {
                LOGGER.info("Root container size mismatch, resizing to screen size");
                rootContainer.setSize(screenWidth, screenHeight);
            } else {
                LOGGER.info("Root container size matches screen perfectly");
            }

            // Register all elements
            LOGGER.debug("Registering elements in hierarchy");
            registerElements(rootContainer);
            LOGGER.info("Registered {} elements", elementRegistry.size());

            // Validate bounds
            if (!debugger.validateBounds(screenWidth, screenHeight)) {
                LOGGER.warn("Some elements are outside screen bounds");
            }

            LOGGER.info("UI definition loaded successfully");
            LOGGER.info("=== END LAYOUT LOADING ===");
            return true;
        } catch (Exception e) {
            LOGGER.error("Failed to load UI from JSON object", e);
            return false;
        }
    }

    /**
     * Register all elements in the tree for lookup.
     */
    private void registerElements(UIElement element) {
        if (element != null && element.getId() != null) {
            elementRegistry.put(element.getId(), element);
            LOGGER.info("Registered element: id='{}', type={}", element.getId(), element.getClass().getSimpleName());
        }
        if (element instanceof ContainerElement container) {
            for (UIElement child : container.getChildren()) {
                registerElements(child);
            }
        }
    }

    /**
     * Set a placeholder value that will be used in template strings.
     */
    public void setPlaceholder(String key, String value) {
        if (context != null) {
            context.setPlaceholder(key, value);
        }
    }

    /**
     * Set multiple placeholders at once.
     */
    public void setPlaceholders(Map<String, String> placeholders) {
        if (context != null) {
            context.setPlaceholders(placeholders);
        }
    }

    /**
     * Apply placeholders to text elements with template syntax.
     */
    public void applyPlaceholders() {
        if (rootContainer != null && context != null) {
            applyPlaceholdersToElement(rootContainer);
        }
    }

    private void applyPlaceholdersToElement(UIElement element) {
        if (element instanceof TextElement text) {
            text.setTextWithPlaceholders(text.getText(), context.getPlaceholders());
        } else if (element instanceof ContainerElement container) {
            for (UIElement child : container.getChildren()) {
                applyPlaceholdersToElement(child);
            }
        }
    }

    /**
     * Find an element by ID in the UI tree.
     */
    public UIElement getElementById(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        return elementRegistry.get(id);
    }

    /**
     * Render the entire UI.
     */
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (rootContainer != null) {
            rootContainer.render(graphics, mouseX, mouseY, partialTick);
        }
    }

    /**
     * Handle mouse click events.
     */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (rootContainer != null) {
            LOGGER.trace("UIBuilder.mouseClicked: delegating to root container at ({},{})", (int) mouseX, (int) mouseY);
            boolean result = rootContainer.mouseClicked(mouseX, mouseY, button);
            if (result) {
                LOGGER.trace("UIBuilder: root container consumed click");
            }
            return result;
        }
        return false;
    }

    /**
     * Handle mouse scroll events.
     */
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (rootContainer != null) {
            return rootContainer.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        return false;
    }

    /**
     * Handle key press events.
     */
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (rootContainer != null) {
            return rootContainer.keyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }

    /**
     * Handle character input for text fields.
     */
    public void charTyped(char ch) {
        if (rootContainer != null) {
            charTypedInElement(rootContainer, ch);
        }
    }

    private void charTypedInElement(UIElement element, char ch) {
        if (element instanceof TextInputElement input) {
            input.addCharacter(ch);
        } else if (element instanceof ContainerElement container) {
            for (UIElement child : container.getChildren()) {
                charTypedInElement(child, ch);
            }
        }
    }

    /**
     * Update screen dimensions on window resize.
     */
    public void updateScreenDimensions(int screenWidth, int screenHeight, double guiScale) {
        if (context != null) {
            context.setScreenWidth(screenWidth);
            context.setScreenHeight(screenHeight);
            context.setGuiScale(guiScale);
        }

        if (rootContainer != null) {
            rootContainer.setSize(screenWidth, screenHeight);
        }
    }

    /**
     * Get the root container.
     */
    public ContainerElement getRootContainer() {
        return rootContainer;
    }

    /**
     * Get the UI context.
     */
    public UIContext getContext() {
        return context;
    }

    /**
     * Get the debugger for this builder
     */
    public UIDebugger getDebugger() {
        return debugger;
    }
}
