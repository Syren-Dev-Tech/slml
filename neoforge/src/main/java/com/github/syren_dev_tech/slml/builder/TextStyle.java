package com.github.syren_dev_tech.slml.builder;

import com.google.gson.JsonObject;

/**
 * Configuration for text styling and appearance.
 */
public class TextStyle {
    public static final int DEFAULT_COLOR = 0xFFFFFF;
    public static final int DEFAULT_SIZE = 12;
    public static final String DEFAULT_FONT = "minecraft:default";

    private int color;
    private int size;
    private String font;
    private boolean bold;
    private boolean italic;
    private boolean underline;
    private boolean strikethrough;
    private boolean obfuscated;

    public TextStyle() {
        this.color = DEFAULT_COLOR;
        this.size = DEFAULT_SIZE;
        this.font = DEFAULT_FONT;
        this.bold = false;
        this.italic = false;
        this.underline = false;
        this.strikethrough = false;
        this.obfuscated = false;
    }

    public static TextStyle fromJson(JsonObject json) {
        TextStyle style = new TextStyle();

        if (json.has("color")) {
            String colorStr = json.get("color").getAsString();
            style.color = parseHexColor(colorStr);
        }

        if (json.has("size")) {
            style.size = json.get("size").getAsInt();
        }

        if (json.has("font")) {
            style.font = json.get("font").getAsString();
        }

        if (json.has("bold")) {
            style.bold = json.get("bold").getAsBoolean();
        }

        if (json.has("italic")) {
            style.italic = json.get("italic").getAsBoolean();
        }

        if (json.has("underline")) {
            style.underline = json.get("underline").getAsBoolean();
        }

        if (json.has("strikethrough")) {
            style.strikethrough = json.get("strikethrough").getAsBoolean();
        }

        if (json.has("obfuscated")) {
            style.obfuscated = json.get("obfuscated").getAsBoolean();
        }

        return style;
    }

    public static int parseHexColor(String colorStr) {
        if (colorStr.startsWith("#")) {
            colorStr = colorStr.substring(1);
        }

        if (colorStr.length() == 6) {
            return Integer.parseInt(colorStr, 16) | 0xFF000000;
        } else if (colorStr.length() == 8) {
            return Integer.parseInt(colorStr, 16);
        }

        return DEFAULT_COLOR;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getFont() {
        return font;
    }

    public void setFont(String font) {
        this.font = font;
    }

    public boolean isBold() {
        return bold;
    }

    public void setBold(boolean bold) {
        this.bold = bold;
    }

    public boolean isItalic() {
        return italic;
    }

    public void setItalic(boolean italic) {
        this.italic = italic;
    }

    public boolean isUnderline() {
        return underline;
    }

    public void setUnderline(boolean underline) {
        this.underline = underline;
    }

    public boolean isStrikethrough() {
        return strikethrough;
    }

    public void setStrikethrough(boolean strikethrough) {
        this.strikethrough = strikethrough;
    }

    public boolean isObfuscated() {
        return obfuscated;
    }

    public void setObfuscated(boolean obfuscated) {
        this.obfuscated = obfuscated;
    }

    public String buildFormattingCode() {
        StringBuilder code = new StringBuilder("§");

        if (bold)
            code.append("l");
        if (italic)
            code.append("o");
        if (underline)
            code.append("n");
        if (strikethrough)
            code.append("m");
        if (obfuscated)
            code.append("k");

        return code.length() > 1 ? code.toString() : "";
    }
}
