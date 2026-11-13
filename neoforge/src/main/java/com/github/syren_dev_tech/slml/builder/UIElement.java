package com.github.syren_dev_tech.slml.builder;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Base interface for all UI elements that can be rendered and interacted with.
 */
public interface UIElement {

    /**
     * Render this element on screen.
     *
     * @param graphics The GuiGraphics used for rendering
     * @param mouseX The current mouse X position
     * @param mouseY The current mouse Y position
     * @param partialTick Partial tick value for interpolation
     */
    void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);

    /**
     * Get the X position of this element.
     */
    int getX();

    /**
     * Get the Y position of this element.
     */
    int getY();

    /**
     * Get the width of this element.
     */
    int getWidth();

    /**
     * Get the height of this element.
     */
    int getHeight();

    /**
     * Get the unique identifier for this element (if set).
     */
    String getId();

    /**
     * Set the identifier for this element.
     */
    void setId(String id);

    /**
     * Set the position of this element.
     */
    void setPosition(int x, int y);

    /**
     * Set the size of this element.
     */
    void setSize(int width, int height);

    /**
     * Check if a point is inside this element.
     */
    boolean isMouseInside(int mouseX, int mouseY);

    /**
     * Handle mouse click events.
     *
     * @return true if the event was handled
     */
    boolean mouseClicked(double mouseX, double mouseY, int button);

    /**
     * Handle mouse scroll events.
     *
     * @return true if the event was handled
     */
    boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY);

    /**
     * Handle key press events.
     *
     * @return true if the event was handled
     */
    boolean keyPressed(int keyCode, int scanCode, int modifiers);
}
