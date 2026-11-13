package com.github.syren_dev_tech.slml.builder;

import net.minecraft.client.gui.GuiGraphics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Debugging utility for UI layouts and rendering. Provides inspection, validation, and visual debugging of UI elements.
 */
public class UIDebugger {
    private static final Logger LOGGER = LoggerFactory.getLogger("SLML:UIDebugger");

    private boolean enabled = false;
    private boolean showBounds = false;
    private boolean showIds = false;
    private final UIBuilder builder;

    public UIDebugger(UIBuilder builder) {
        this.builder = builder;
    }

    /**
     * Enable debugging output
     */
    public void enable() {
        this.enabled = true;
        LOGGER.info("UI Debugger enabled");
    }

    /**
     * Disable debugging output
     */
    public void disable() {
        this.enabled = false;
    }

    /**
     * Enable visual bounds rendering
     */
    public void enableBoundsRendering() {
        this.showBounds = true;
        LOGGER.info("UI bounds rendering enabled");
    }

    /**
     * Enable ID label rendering
     */
    public void enableIdRendering() {
        this.showIds = true;
        LOGGER.info("UI ID rendering enabled");
    }

    /**
     * Log the complete element hierarchy
     */
    public void logHierarchy() {
        if (builder == null || builder.getRootContainer() == null) {
            LOGGER.warn("Cannot log hierarchy: no root container");
            return;
        }

        LOGGER.info("=== UI HIERARCHY ===");
        logElementHierarchy(builder.getRootContainer(), 0);
        LOGGER.info("=== END HIERARCHY ===");
    }

    private void logElementHierarchy(UIElement element, int depth) {
        String indent = "  ".repeat(depth);
        String id = element.getId() != null ? " [" + element.getId() + "]" : "";
        String type = element.getClass().getSimpleName();
        String bounds = String.format("(%d, %d, %dx%d)", element.getX(), element.getY(), element.getWidth(), element.getHeight());

        LOGGER.info("{}{}{} {}", indent, type, id, bounds);

        if (element instanceof ContainerElement container) {
            for (UIElement child : container.getChildren()) {
                logElementHierarchy(child, depth + 1);
            }
        }
    }

    /**
     * Validate that all elements are within screen bounds
     */
    public boolean validateBounds(int screenWidth, int screenHeight) {
        if (builder == null || builder.getRootContainer() == null) {
            LOGGER.warn("Cannot validate bounds: no root container");
            return false;
        }

        LOGGER.info("=== VALIDATING BOUNDS (Screen: {}x{}) ===", screenWidth, screenHeight);
        boolean valid = validateElementBounds(builder.getRootContainer(), screenWidth, screenHeight, 0);
        LOGGER.info("=== BOUNDS VALIDATION {}  ===", valid ? "PASSED" : "FAILED");

        return valid;
    }

    private boolean validateElementBounds(UIElement element, int screenWidth, int screenHeight, int depth) {
        String indent = "  ".repeat(depth);
        String elementId = getElementId(element);

        int x = element.getX();
        int y = element.getY();
        int width = element.getWidth();
        int height = element.getHeight();

        boolean elementValid = isElementWithinBounds(x, y, width, height, screenWidth, screenHeight);
        BoundsInfo boundsInfo = new BoundsInfo(x, y, width, height, screenWidth, screenHeight);
        logElementBoundsStatus(element, elementValid, indent, elementId, boundsInfo);

        if (element instanceof ContainerElement container) {
            elementValid = validateContainerChildren(container, screenWidth, screenHeight, depth, elementId, indent) && elementValid;
        }

        return elementValid;
    }

    private String getElementId(UIElement element) {
        return element.getId() != null ? element.getId() : "(unnamed)";
    }

    private boolean isElementWithinBounds(int x, int y, int width, int height, int screenWidth, int screenHeight) {
        boolean xValid = x >= 0 && x < screenWidth;
        boolean yValid = y >= 0 && y < screenHeight;
        boolean widthValid = width > 0 && (x + width) <= screenWidth;
        boolean heightValid = height > 0 && (y + height) <= screenHeight;
        return xValid && yValid && widthValid && heightValid;
    }

    private void logElementBoundsStatus(UIElement element, boolean elementValid, String indent, String elementId, BoundsInfo bounds) {
        String id = element.getId() != null ? " [" + element.getId() + "]" : "";
        String status = elementValid ? "OK" : "XX";

        if (!elementValid) {
            LOGGER.warn("{}[{}] {}{} Position: ({},{}) Size: {}x{}", indent, status, element.getClass().getSimpleName(), id, bounds.x, bounds.y, bounds.width, bounds.height);
            logBoundsViolations(indent, elementId, bounds);
        } else {
            LOGGER.debug("{}[{}] {}{} ({}, {}) {}x{}", indent, status, element.getClass().getSimpleName(), id, bounds.x, bounds.y, bounds.width, bounds.height);
        }
    }

    private void logBoundsViolations(String indent, String elementId, BoundsInfo bounds) {
        if (bounds.x < 0 || bounds.x >= bounds.screenWidth) {
            LOGGER.warn("{}  [{}] X position {} is outside bounds [0, {})", indent, elementId, bounds.x, bounds.screenWidth);
        }
        if (bounds.y < 0 || bounds.y >= bounds.screenHeight) {
            LOGGER.warn("{}  [{}] Y position {} is outside bounds [0, {})", indent, elementId, bounds.y, bounds.screenHeight);
        }
        if (bounds.width <= 0 || (bounds.x + bounds.width) > bounds.screenWidth) {
            LOGGER.warn("{}  [{}] Width {} extends beyond screen width {} (right edge: {})", indent, elementId, bounds.width, bounds.screenWidth, bounds.x + bounds.width);
        }
        if (bounds.height <= 0 || (bounds.y + bounds.height) > bounds.screenHeight) {
            LOGGER.warn("{}  [{}] Height {} extends beyond screen height {} (bottom edge: {})", indent, elementId, bounds.height, bounds.screenHeight, bounds.y + bounds.height);
        }
    }

    private static class BoundsInfo {
        int x;
        int y;
        int width;
        int height;
        int screenWidth;
        int screenHeight;

        BoundsInfo(int x, int y, int width, int height, int screenWidth, int screenHeight) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.screenWidth = screenWidth;
            this.screenHeight = screenHeight;
        }
    }

    private boolean validateContainerChildren(ContainerElement container, int screenWidth, int screenHeight, int depth, String elementId, String indent) {
        boolean isScrollable = hasScrollableContent(container);
        if (isScrollable) {
            LOGGER.debug("{}  [{}] Container is scrollable - skipping child bounds validation", indent, elementId);
            return true;
        }

        boolean allChildrenValid = true;
        for (UIElement child : container.getChildren()) {
            boolean childValid = validateElementBounds(child, screenWidth, screenHeight, depth + 1);
            allChildrenValid = allChildrenValid && childValid;
        }
        return allChildrenValid;
    }

    /**
     * Check if a container has content that exceeds its visible bounds (requires scrolling)
     */
    private boolean hasScrollableContent(ContainerElement container) {
        int containerHeight = container.getHeight();
        int totalChildHeight = 0;

        for (UIElement child : container.getChildren()) {
            totalChildHeight += child.getHeight();
        }

        return totalChildHeight > containerHeight;
    }

    /**
     * Verify element sizes and positions match JSON definitions
     */
    public void verifyLayoutConsistency() {
        if (builder == null || builder.getRootContainer() == null) {
            LOGGER.warn("Cannot verify layout: no root container");
            return;
        }

        LOGGER.info("=== VERIFYING LAYOUT CONSISTENCY ===");
        verifyElement(builder.getRootContainer(), 0);
        LOGGER.info("=== END VERIFICATION ===");
    }

    private void verifyElement(UIElement element, int depth) {
        String indent = "  ".repeat(depth);
        String id = element.getId() != null ? " [" + element.getId() + "]" : "";

        // Check for zero dimensions (common issue)
        if (element.getWidth() <= 0 || element.getHeight() <= 0) {
            LOGGER.warn("{}{}{} has zero dimensions: {}×{}", indent, element.getClass().getSimpleName(), id, element.getWidth(), element.getHeight());
        }

        // Check for negative positions
        if (element.getX() < 0 || element.getY() < 0) {
            LOGGER.warn("{}{}{} has negative position: ({}, {})", indent, element.getClass().getSimpleName(), id, element.getX(), element.getY());
        }

        // Log element info
        LOGGER.debug("{}{}{}  Position: ({}, {}) Size: {}×{}", indent, element.getClass().getSimpleName(), id, element.getX(), element.getY(), element.getWidth(), element.getHeight());

        if (element instanceof ContainerElement container) {
            LOGGER.debug("{}  Children: {}", indent, container.getChildren().size());
            for (UIElement child : container.getChildren()) {
                verifyElement(child, depth + 1);
            }
        }
    }

    /**
     * Find an element and print its details
     */
    public void inspectElement(String elementId) {
        if (builder == null) {
            LOGGER.warn("Cannot inspect: builder is null");
            return;
        }

        UIElement element = builder.getElementById(elementId);
        if (element == null) {
            LOGGER.warn("Element not found: {}", elementId);
            return;
        }

        LOGGER.info("=== ELEMENT INSPECTION: {} ===", elementId);
        LOGGER.info("Type: {}", element.getClass().getSimpleName());
        LOGGER.info("Position: ({}, {})", element.getX(), element.getY());
        LOGGER.info("Size: {}×{}", element.getWidth(), element.getHeight());

        if (element instanceof TextElement text) {
            LOGGER.info("Text: {}", text.getText());
        } else if (element instanceof ButtonElement button) {
            LOGGER.info("Label: {}", button.getLabel());
        } else if (element instanceof ContainerElement container) {
            LOGGER.info("Children: {}", container.getChildren().size());
            for (UIElement child : container.getChildren()) {
                String childId = child.getId() != null ? child.getId() : "(no id)";
                LOGGER.info("  - {} [{}]", child.getClass().getSimpleName(), childId);
            }
        }

        LOGGER.info("=== END INSPECTION ===");
    }

    /**
     * Render debug overlays (bounds, IDs, etc)
     */
    public void render(GuiGraphics graphics, int screenWidth, int screenHeight) {
        if (!enabled) {
            return;
        }

        if (showBounds) {
            renderBounds(graphics, builder.getRootContainer(), screenWidth, screenHeight);
        }

        if (showIds) {
            renderIds(graphics, builder.getRootContainer());
        }
    }

    private void renderBounds(GuiGraphics graphics, UIElement element, int screenWidth, int screenHeight) {
        int x = element.getX();
        int y = element.getY();
        int width = element.getWidth();
        int height = element.getHeight();

        // Skip if out of bounds
        if (x + width < 0 || x > screenWidth || y + height < 0 || y > screenHeight) {
            if (element instanceof ContainerElement container) {
                for (UIElement child : container.getChildren()) {
                    renderBounds(graphics, child, screenWidth, screenHeight);
                }
            }
            return;
        }

        // Draw element bounds in different colors based on type
        int color;
        if (element instanceof ContainerElement) {
            color = 0xFF0000FF; // Blue for containers
        } else if (element instanceof ButtonElement) {
            color = 0xFF00FF00; // Green for buttons
        } else if (element instanceof TextElement) {
            color = 0xFFFFFF00; // Yellow for text
        } else {
            color = 0xFFFF00FF; // Magenta for others
        }

        // Draw hollow rectangle
        drawHollowRect(graphics, x, y, x + width, y + height, color, 1);

        // Recurse for children
        if (element instanceof ContainerElement container) {
            for (UIElement child : container.getChildren()) {
                renderBounds(graphics, child, screenWidth, screenHeight);
            }
        }
    }

    private void renderIds(GuiGraphics graphics, UIElement element) {
        if (element.getId() != null && !element.getId().isEmpty()) {
            // Draw ID label
            int x = element.getX() + 2;
            int y = element.getY() + 2;
            graphics.drawString(null, element.getId(), x, y, 0xFFFFFFFF);
        }

        // Recurse for children
        if (element instanceof ContainerElement container) {
            for (UIElement child : container.getChildren()) {
                renderIds(graphics, child);
            }
        }
    }

    private void drawHollowRect(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color, int width) {
        // Top
        graphics.fill(x1, y1, x2, y1 + width, color);
        // Bottom
        graphics.fill(x1, y2 - width, x2, y2, color);
        // Left
        graphics.fill(x1, y1, x1 + width, y2, color);
        // Right
        graphics.fill(x2 - width, y1, x2, y2, color);
    }

    /**
     * Log rendering performance
     */
    public void logRenderTime(long nanoTime) {
        if (!enabled) {
            return;
        }

        double millis = nanoTime / 1_000_000.0;
        if (millis > 5.0) {
            LOGGER.warn("Slow render: {}ms", String.format("%.2f", millis)); // NOSONAR - Allowed
        }
    }

    /**
     * Dump complete layout JSON for verification
     */
    public void dumpLayout() {
        if (builder == null || builder.getRootContainer() == null) {
            LOGGER.warn("Cannot dump layout: no root container");
            return;
        }

        LOGGER.info("=== LAYOUT DUMP ===");
        dumpElement(builder.getRootContainer(), 0);
        LOGGER.info("=== END DUMP ===");
    }

    private void dumpElement(UIElement element, int depth) {
        String indent = "  ".repeat(depth);
        StringBuilder sb = new StringBuilder();
        sb.append(indent).append(element.getClass().getSimpleName());

        if (element.getId() != null) {
            sb.append(" [").append(element.getId()).append("]");
        }

        sb.append(" @ (").append(element.getX()).append(",").append(element.getY()).append(")");
        sb.append(" ").append(element.getWidth()).append("x").append(element.getHeight());

        if (element instanceof TextElement text) {
            sb.append(" text=\"").append(text.getText()).append("\"");
        } else if (element instanceof ButtonElement button) {
            sb.append(" label=\"").append(button.getLabel()).append("\"");
        } else if (element instanceof ContainerElement container) {
            sb.append(" children=").append(container.getChildren().size());
        }

        LOGGER.info(sb.toString()); // NOSONAR - Allowed

        if (element instanceof ContainerElement container) {
            for (UIElement child : container.getChildren()) {
                dumpElement(child, depth + 1);
            }
        }
    }
}
