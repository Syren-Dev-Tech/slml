package com.github.syren_dev_tech.slml.builder;

import com.google.gson.JsonObject;

/**
 * Configuration for element rendering (colors, borders).
 */
public class RenderStyle {
    private Integer fillColor;
    private Integer borderColor;
    private int borderWeight;

    public RenderStyle() {
        this.fillColor = null;
        this.borderColor = null;
        this.borderWeight = 0;
    }

    public static RenderStyle fromJson(JsonObject json) {
        RenderStyle style = new RenderStyle();

        if (json.has("fillColor")) {
            String colorStr = json.get("fillColor").getAsString();
            style.fillColor = TextStyle.parseHexColor(colorStr);
        }

        if (json.has("borderColor")) {
            String colorStr = json.get("borderColor").getAsString();
            style.borderColor = TextStyle.parseHexColor(colorStr);
        }

        if (json.has("borderWeight")) {
            style.borderWeight = json.get("borderWeight").getAsInt();
        }

        return style;
    }

    public Integer getFillColor() {
        return fillColor;
    }

    public void setFillColor(Integer fillColor) {
        this.fillColor = fillColor;
    }

    public Integer getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(Integer borderColor) {
        this.borderColor = borderColor;
    }

    public int getBorderWeight() {
        return borderWeight;
    }

    public void setBorderWeight(int borderWeight) {
        this.borderWeight = borderWeight;
    }

    public boolean hasFillColor() {
        return fillColor != null;
    }

    public boolean hasBorderColor() {
        return borderColor != null;
    }
}
