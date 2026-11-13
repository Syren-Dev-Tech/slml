package com.github.syren_dev_tech.slml.builder;

/**
 * Context information for UI rendering, containing screen dimensions and other metadata.
 */
public class UIContext {
    private int screenWidth;
    private int screenHeight;
    private double guiScale;
    private java.util.Map<String, String> placeholders;

    public UIContext(int screenWidth, int screenHeight, double guiScale) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.guiScale = guiScale;
        this.placeholders = new java.util.HashMap<>();
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public void setScreenWidth(int screenWidth) {
        this.screenWidth = screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }

    public void setScreenHeight(int screenHeight) {
        this.screenHeight = screenHeight;
    }

    public double getGuiScale() {
        return guiScale;
    }

    public void setGuiScale(double guiScale) {
        this.guiScale = guiScale;
    }

    public void setPlaceholder(String key, String value) {
        placeholders.put(key, value);
    }

    public String getPlaceholder(String key, String defaultValue) {
        return placeholders.getOrDefault(key, defaultValue);
    }

    public java.util.Map<String, String> getPlaceholders() {
        return new java.util.HashMap<>(placeholders);
    }

    public void setPlaceholders(java.util.Map<String, String> placeholders) {
        this.placeholders = new java.util.HashMap<>(placeholders);
    }

    public void clearPlaceholders() {
        placeholders.clear();
    }
}
