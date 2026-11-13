package com.github.syren_dev_tech.slml.builder;

import net.minecraft.client.gui.Font;

/**
 * Base class for input elements.
 */
public abstract class InputElement extends BaseUIElement {
    protected TextStyle textStyle;
    protected RenderStyle renderStyle;
    protected Font font;
    protected boolean focused;

    protected InputElement(int x, int y, int width, int height, Font font) {
        super(x, y, width, height);
        this.font = font;
        this.textStyle = new TextStyle();
        this.renderStyle = new RenderStyle();
        this.focused = false;
    }

    protected InputElement(Dimension x, Dimension y, Dimension width, Dimension height, Font font) {
        super(x, y, width, height);
        this.font = font;
        this.textStyle = new TextStyle();
        this.renderStyle = new RenderStyle();
        this.focused = false;
    }

    public TextStyle getTextStyle() {
        return textStyle;
    }

    public void setTextStyle(TextStyle textStyle) {
        this.textStyle = textStyle;
    }

    public RenderStyle getRenderStyle() {
        return renderStyle;
    }

    public void setRenderStyle(RenderStyle renderStyle) {
        this.renderStyle = renderStyle;
    }

    public boolean isFocused() {
        return focused;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        focused = isMouseInside((int) mouseX, (int) mouseY);
        return focused;
    }

    public abstract String getValue();

    public abstract void setValue(String value);
}
