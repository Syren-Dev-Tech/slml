package com.github.syren_dev_tech.slml.builder;

import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

/**
 * A texture element for displaying items, blocks, or other textures.
 */
public class TextureElement extends BaseUIElement {
    private ResourceLocation textureLocation;
    private float scale;
    private int u; // Texture U coordinate
    private int v; // Texture V coordinate
    private int texWidth; // Texture width
    private int texHeight; // Texture height
    private boolean isItem; // If true, renders from item atlas
    private int offsetY; // Vertical render offset (for centering, etc)

    public TextureElement(int x, int y, int width, int height, ResourceLocation textureLocation) {
        super(x, y, width, height);
        this.textureLocation = textureLocation;
        this.scale = 1.0f;
        this.u = 0;
        this.v = 0;
        this.texWidth = 16;
        this.texHeight = 16;
        this.isItem = false;
        this.offsetY = 0;
    }

    public TextureElement(Dimension x, Dimension y, Dimension width, Dimension height, ResourceLocation textureLocation) {
        super(x, y, width, height);
        this.textureLocation = textureLocation;
        this.scale = 1.0f;
        this.u = 0;
        this.v = 0;
        this.texWidth = 16;
        this.texHeight = 16;
        this.isItem = false;
        this.offsetY = 0;
    }

    public static TextureElement fromJson(JsonObject json) {
        ResourceLocation texture = ResourceLocation.parse(json.get("texture").getAsString());

        // Parse dimensions using DimensionParser to support percentages
        Dimension[] dims = DimensionParser.parseAllDimensions(json, 0, 0, 16, 16);
        Dimension x = dims[0];
        Dimension y = dims[1];
        Dimension width = dims[2];
        Dimension height = dims[3];

        TextureElement element = new TextureElement(x, y, width, height, texture);

        if (json.has("scale")) {
            element.scale = json.get("scale").getAsFloat();
        }

        if (json.has("u")) {
            element.u = json.get("u").getAsInt();
        }

        if (json.has("v")) {
            element.v = json.get("v").getAsInt();
        }

        if (json.has("texWidth")) {
            element.texWidth = json.get("texWidth").getAsInt();
        }

        if (json.has("texHeight")) {
            element.texHeight = json.get("texHeight").getAsInt();
        }

        if (json.has("isItem")) {
            element.isItem = json.get("isItem").getAsBoolean();
        }

        if (json.has("offsetY")) {
            element.offsetY = json.get("offsetY").getAsInt();
        }

        return element;
    }

    public void setScale(float scale) {
        this.scale = Math.max(0.1f, scale);
    }

    public float getScale() {
        return scale;
    }

    public void setTextureCoordinates(int u, int v, int texWidth, int texHeight) {
        this.u = u;
        this.v = v;
        this.texWidth = texWidth;
        this.texHeight = texHeight;
    }

    public ResourceLocation getTextureLocation() {
        return textureLocation;
    }

    public void setTextureLocation(ResourceLocation location) {
        this.textureLocation = location;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (isItem) {
            renderItemTexture(graphics);
        } else {
            renderTextureQuad(graphics);
        }
    }

    private void renderTextureQuad(GuiGraphics graphics) {
        graphics.blit(textureLocation, x, y + offsetY, (float) u, (float) v, width, height, texWidth, texHeight);
    }

    private void renderItemTexture(GuiGraphics graphics) {
        // For item rendering, we need to use the item atlas
        try {
            graphics.blit(InventoryMenu.BLOCK_ATLAS, x, y + offsetY, 0, (float) u, (float) v, width, height, texWidth, texHeight);
        } catch (Exception e) {
            // Fallback if item rendering fails
            graphics.fill(x, y + offsetY, x + width, y + height + offsetY, 0xFFFF00FF);
        }
    }
}
