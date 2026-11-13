package com.github.syren_dev_tech.slml.builder;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Abstract base class for UI elements with common positioning and sizing. Supports both absolute (pixel) and percentage-based dimensions.
 */
public abstract class BaseUIElement implements UIElement {
    // Stored dimensions (can be absolute or percentage)
    protected Dimension xDimension;
    protected Dimension yDimension;
    protected Dimension widthDimension;
    protected Dimension heightDimension;

    // Computed pixel values
    protected int x;
    protected int y;
    protected int width;
    protected int height;

    // Parent dimensions used for percentage calculations
    protected int parentWidth;
    protected int parentHeight;

    // Flex grow factor for expanding in flex containers
    protected int flexGrow;

    // Unique identifier for this element
    protected String id;

    /**
     * Constructor with absolute pixel values.
     */
    protected BaseUIElement(int x, int y, int width, int height) {
        this.xDimension = Dimension.parse(x);
        this.yDimension = Dimension.parse(y);
        this.widthDimension = Dimension.parse(width);
        this.heightDimension = Dimension.parse(height);
        this.parentWidth = 0;
        this.parentHeight = 0;
        this.flexGrow = 0;
        this.id = null;
        updateComputedDimensions();
    }

    /**
     * Constructor with Dimension objects (allowing percentages).
     */
    protected BaseUIElement(Dimension x, Dimension y, Dimension width, Dimension height) {
        this.xDimension = x != null ? x : Dimension.parse(0);
        this.yDimension = y != null ? y : Dimension.parse(0);
        this.widthDimension = width != null ? width : Dimension.parse(0);
        this.heightDimension = height != null ? height : Dimension.parse(0);
        this.parentWidth = 0;
        this.parentHeight = 0;
        this.flexGrow = 0;
        this.id = null;
        updateComputedDimensions();
    }

    /**
     * Update computed dimensions based on parent dimensions. Call this whenever parent dimensions change.
     */
    public void updateComputedDimensions() {
        this.x = xDimension.resolve(parentWidth);
        this.y = yDimension.resolve(parentHeight);
        this.width = widthDimension.resolve(parentWidth);
        this.height = heightDimension.resolve(parentHeight);
    }

    /**
     * Set the parent dimensions used for percentage calculations.
     */
    public void setParentDimensions(int parentWidth, int parentHeight) {
        this.parentWidth = parentWidth;
        this.parentHeight = parentHeight;
        updateComputedDimensions();
    }

    /**
     * Get the stored dimension objects (not computed values).
     */
    public Dimension getXDimension() {
        return xDimension;
    }

    public Dimension getYDimension() {
        return yDimension;
    }

    public Dimension getWidthDimension() {
        return widthDimension;
    }

    public Dimension getHeightDimension() {
        return heightDimension;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get the flex grow factor for this element. Used to determine how much space this element takes up in flex containers.
     */
    public int getFlexGrow() {
        return flexGrow;
    }

    /**
     * Set the flex grow factor for this element.
     */
    public void setFlexGrow(int flexGrow) {
        this.flexGrow = Math.max(0, flexGrow);
    }

    @Override
    public void setPosition(int x, int y) {
        this.xDimension = Dimension.parse(x);
        this.yDimension = Dimension.parse(y);
        updateComputedDimensions();
    }

    /**
     * Set position using Dimension objects (allowing percentages).
     */
    public void setPositionDimensions(Dimension x, Dimension y) {
        this.xDimension = x != null ? x : Dimension.parse(0);
        this.yDimension = y != null ? y : Dimension.parse(0);
        updateComputedDimensions();
    }

    @Override
    public void setSize(int width, int height) {
        this.widthDimension = Dimension.parse(width);
        this.heightDimension = Dimension.parse(height);
        updateComputedDimensions();
    }

    /**
     * Set size using Dimension objects (allowing percentages).
     */
    public void setSizeDimensions(Dimension width, Dimension height) {
        this.widthDimension = width != null ? width : Dimension.parse(0);
        this.heightDimension = height != null ? height : Dimension.parse(0);
        updateComputedDimensions();
    }

    @Override
    public boolean isMouseInside(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    @Override
    public abstract void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);
}
