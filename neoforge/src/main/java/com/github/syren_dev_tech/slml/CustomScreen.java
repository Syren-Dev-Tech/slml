package com.github.syren_dev_tech.slml;

import com.github.syren_dev_tech.slml.builder.*;
import com.github.syren_dev_tech.slml.builder.xml.XMLAwareLayoutLoader;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class CustomScreen extends Screen {
    private static final Logger LOGGER = LoggerFactory.getLogger("SLML:CustomScreen");

    private final Player player;

    private final ResourceLocation layoutLocation;
    private UIBuilder uiBuilder;
    private Map<String, String> placeholders;
    private final Boolean pauseScreen;

    private Function<Minecraft, Boolean> doOnClosure;
    private Function<UIBuilder, Boolean> doOnInit;

    public CustomScreen(Player player, String namespace, String layout, Boolean isPauseScreen) {
        super(Component.literal("Villager Trades"));

        this.player = player;
        this.placeholders = new HashMap<>();

        this.layoutLocation = ResourceLocation.fromNamespaceAndPath(namespace, "ui/" + layout);
        this.placeholders = new HashMap<>();

        this.pauseScreen = isPauseScreen;
    }

    public CustomScreen(Player player, String namespace, String layout) {
        this(player, namespace, layout, false);
    }

    public void setOnCloseCallback(Function<Minecraft, Boolean> callback) {
        this.doOnClosure = callback;
    }

    public void setOnInitCallback(Function<UIBuilder, Boolean> callback) {
        this.doOnInit = callback;
    }

    public void definePlaceholder(String key, String value) {
        this.placeholders.put(key, value);
    }

    public Player getPlayer() {
        return this.player;
    }

    @Override
    public boolean isPauseScreen() {
        return this.pauseScreen;
    }

    @Override
    protected void init() {
        super.init();

        try {
            LOGGER.info("=== INITIALIZING VILLAGER TRADE SCREEN ===");
            LOGGER.info("Screen dimensions: {}x{}", this.width, this.height);

            // Set up placeholders first
            setupPlaceholders();

            // Create UI builder
            this.uiBuilder = new UIBuilder(this.font);

            // Enable debugging if needed
            if (LOGGER.isDebugEnabled()) {
                uiBuilder.getDebugger().enable();
                uiBuilder.getDebugger().enableBoundsRendering();
            }

            // Load layout from XML file with placeholders
            LOGGER.info("Loading layout from: {}", layoutLocation);
            XMLAwareLayoutLoader xmlLoader = new XMLAwareLayoutLoader();

            // Set placeholders BEFORE loading so they're substituted during parsing
            LOGGER.info("Setting {} placeholders for XML parser", placeholders.size());
            Map<String, Object> placeholdersAsObjects = new HashMap<>(placeholders);
            xmlLoader.setPlaceholders(placeholdersAsObjects);

            JsonObject layoutJson = xmlLoader.loadLayout(layoutLocation);

            if (layoutJson == null) {
                LOGGER.error("Failed to load layout XML from resources!");
                LOGGER.error("Expected location: data/{}/{}.xml", layoutLocation.getNamespace(), layoutLocation.getPath());
                return;
            }

            LOGGER.info("Layout XML loaded successfully");

            // Load the layout into UIBuilder
            boolean loaded = uiBuilder.loadFromJson(layoutJson, this.width, this.height, this.minecraft.getWindow().getGuiScale());

            if (!loaded) {
                LOGGER.error("Failed to load layout into UIBuilder");
                return;
            }

            // Apply placeholders to update text with actual values
            LOGGER.info("Applying placeholders to layout");
            applyPlaceholders();

            // Verify the loaded layout
            if (LOGGER.isDebugEnabled()) {
                LOGGER.info("Running layout verification...");
                uiBuilder.getDebugger().verifyLayoutConsistency();
                uiBuilder.getDebugger().logHierarchy();
            }

            if (this.doOnInit != null) {
                this.doOnInit.apply(uiBuilder);
            }

            // Setup event handlers (button clicks, etc)
            setupEventHandlers();

            LOGGER.info("=== SCREEN INITIALIZATION COMPLETE ===");
        } catch (Exception e) {
            LOGGER.error("Fatal error during screen initialization", e);
            // Don't close the screen - let it display with partial content or error message
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Fill background with semi-transparent dark color to darken the world
        // This makes the UI stand out and readable
        guiGraphics.fill(0, 0, this.width, this.height, 0xAA000000); // 2/3 transparent black

        // Render UI if builder is available
        if (this.uiBuilder != null) {
            this.uiBuilder.render(guiGraphics, mouseX, mouseY, partialTick);
        } else {
            LOGGER.warn("UIBuilder is null - cannot render UI");
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalScroll, double verticalScroll) {
        if (this.uiBuilder != null && this.uiBuilder.mouseScrolled(mouseX, mouseY, horizontalScroll, verticalScroll)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalScroll, verticalScroll);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.uiBuilder != null && this.uiBuilder.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Apply placeholder values from the placeholders map to the UI layout. This updates text elements with actual game data.
     */
    private void applyPlaceholders() {
        try {
            ContainerElement root = uiBuilder.getRootContainer();
            if (root == null) {
                LOGGER.warn("Root container is null, cannot apply placeholders");
                return;
            }

            LOGGER.info("=== APPLYING PLACEHOLDERS ===");
            LOGGER.info("Placeholder map has {} entries", placeholders.size());
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                LOGGER.info("  '{}' = '{}'", entry.getKey(), entry.getValue());
            }

            // Count text elements before applying
            int[] textCount = {0};
            countTextElements(root, textCount);
            LOGGER.info("Found {} text elements in UI tree", textCount[0]);

            applyPlaceholdersRecursive(root);
            LOGGER.info("Placeholders applied successfully");
        } catch (Exception e) {
            LOGGER.error("Error applying placeholders", e);
        }
    }

    private void countTextElements(UIElement element, int[] count) {
        if (element instanceof TextElement) {
            count[0]++;
        } else if (element instanceof ContainerElement container) {
            for (UIElement child : container.getChildren()) {
                countTextElements(child, count);
            }
        }
    }

    private void applyPlaceholdersRecursive(UIElement element) {
        if (element instanceof TextElement text) {
            String elementId = text.getId() != null ? text.getId() : "(unnamed)";
            String originalText = text.getText();
            String newText = originalText;

            LOGGER.debug("TextElement {}: original='{}', size={}x{}", elementId, originalText, text.getWidth(), text.getHeight());

            // Replace each placeholder (format: {{key}})
            for (Map.Entry<String, String> placeholder : placeholders.entrySet()) {
                String search = "{{" + placeholder.getKey() + "}}";
                newText = newText.replace(search, placeholder.getValue() != null ? placeholder.getValue() : "");
            }

            if (!newText.equals(originalText)) {
                LOGGER.info("Updated {}: '{}' -> '{}' (now {}x{})", elementId, originalText, newText, text.getWidth(), text.getHeight());
                text.setText(newText);
            }
        } else if (element instanceof ContainerElement container) {
            for (UIElement child : container.getChildren()) {
                applyPlaceholdersRecursive(child);
            }
        }
    }

    /**
     * Set up placeholder values from game data. These values will be substituted into the XML layout.
     */
    private void setupPlaceholders() {
        LOGGER.info("=== SETTING UP PLACEHOLDERS ===");

        // No need to set data file paths here anymore - they're declared in XML.
        LOGGER.info("=== PLACEHOLDERS SETUP COMPLETE ===");
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            LOGGER.info("  {}: '{}'", entry.getKey(), entry.getValue());
        }
    }

    /**
     * Set up event handlers (button clicks, etc)
     */
    private void setupEventHandlers() {
        // Close button handler
        UIElement closeButton = uiBuilder.getElementById("close_button");
        if (closeButton instanceof ButtonElement buttonElement) {
            buttonElement.setOnClickCallback(this::onClose);
        }
    }

    /**
     * Called when the trade screen is closed (either by button or ESC key) Sends a packet to the server to unfreeze the villager's AI
     */
    @Override
    public void onClose() {
        LOGGER.info("Closing screen");

        // Send packet to server to unfreeze the villager
        if (this.minecraft != null && this.minecraft.getConnection() != null && this.doOnClosure != null) {
            this.doOnClosure.apply(this.minecraft);
        }

        // Call parent onClose to actually close the screen
        super.onClose();
    }
}

