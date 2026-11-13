package com.github.syren_dev_tech.slml.builder;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * A container element that can hold child elements and support scrolling.
 */
public class ContainerElement extends BaseUIElement {
    private static final Logger LOGGER = LoggerFactory.getLogger("VillagerWorkshopsUI");
    private final List<UIElement> children;
    private RenderStyle renderStyle;
    private int scrollY;
    private int maxScrollY;
    private int contentHeight;
    private static final int SCROLL_SPEED = 10;

    // Optional flex-like flow layout configuration (from JSON "flexLayout")
    private FlexLayoutConfig flexLayout;
    private final Map<UIElement, ChildFlowFlags> childFlowFlags;

    private static class FlexLayoutConfig {
        String direction = "column"; // row | column
        String justify = "flex-start"; // flex-start | center | even | between | flex-end
        String alignItems = "flex-start"; // flex-start | center | flex-end (cross-axis)
        int gap = 0;
        int paddingTop = 0;
        int paddingRight = 0;
        int paddingBottom = 0;
        int paddingLeft = 0;
    }

    private static class ChildFlowFlags {
        final boolean autoX;
        final boolean autoY;

        ChildFlowFlags(boolean autoX, boolean autoY) {
            this.autoX = autoX;
            this.autoY = autoY;
        }
    }

    public ContainerElement(int x, int y, int width, int height) {
        super(x, y, width, height);
        this.children = new ArrayList<>();
        this.renderStyle = new RenderStyle();
        this.scrollY = 0;
        this.maxScrollY = 0;
        this.contentHeight = 0;
        this.flexLayout = null;
        this.childFlowFlags = new IdentityHashMap<>();
    }

    public ContainerElement(Dimension x, Dimension y, Dimension width, Dimension height) {
        super(x, y, width, height);
        this.children = new ArrayList<>();
        this.renderStyle = new RenderStyle();
        this.scrollY = 0;
        this.maxScrollY = 0;
        this.contentHeight = 0;
        this.flexLayout = null;
        this.childFlowFlags = new IdentityHashMap<>();
    }

    public static ContainerElement fromJson(JsonObject json, Font font) {
        // Parse dimensions using DimensionParser to support percentages
        // Default to 0 for width/height - 0 means "auto-size based on content"
        // Explicit percentages (e.g., "100%") will override these defaults
        Dimension[] dims = DimensionParser.parseAllDimensions(json, 0, 0, 0, 0);
        Dimension x = dims[0];
        Dimension y = dims[1];
        Dimension width = dims[2];
        Dimension height = dims[3];

        LOGGER.debug("Creating container: x={} y={} w={} h={}", x, y, width, height);

        ContainerElement container = new ContainerElement(x, y, width, height);

        // Parse ID if present
        if (json.has("id")) {
            String id = json.get("id").getAsString();
            container.setId(id);
            LOGGER.debug("Container ID set: {}", id);
        }

        if (json.has("flexGrow")) {
            int flexGrow = json.get("flexGrow").getAsInt();
            container.setFlexGrow(flexGrow);
            LOGGER.debug("Container flexGrow set: {}", flexGrow);
        }

        if (json.has("renderStyle")) {
            container.renderStyle = RenderStyle.fromJson(json.getAsJsonObject("renderStyle"));
            LOGGER.debug("Container renderStyle loaded");
        }

        if (json.has("flexLayout")) {
            container.flexLayout = parseFlexLayout(json.getAsJsonObject("flexLayout"));
            LOGGER.debug("Container flexLayout loaded: direction={}, gap={}", container.flexLayout.direction, container.flexLayout.gap);
        }

        // Load children if present
        if (json.has("children")) {
            JsonArray childrenArray = json.getAsJsonArray("children");
            LOGGER.debug("Loading {} child elements", childrenArray.size());
            for (int i = 0; i < childrenArray.size(); i++) {
                JsonObject childJson = childrenArray.get(i).getAsJsonObject();
                UIElement child = UIElementFactory.createElement(childJson, font);
                if (child != null) {
                    // DO NOT set parent dimensions here - container hasn't been sized yet!
                    // Parent dimensions will be set when this container's setParentDimensions()
                    // is called later, which cascades to all children.
                    // This ensures children use the container's actual computed size, not 0.

                    // Flex flow behavior: if x/y omitted, auto-place in flow direction
                    boolean autoX = container.flexLayout != null && !childJson.has("x");
                    boolean autoY = container.flexLayout != null && !childJson.has("y");
                    container.addChild(child, autoX, autoY);
                } else {
                    LOGGER.warn("Failed to create child element at index {}", i);
                }
            }
            LOGGER.debug("Successfully loaded {} child elements", container.children.size());
        }

        // DO NOT call applyFlowLayout() here - container dimensions are not computed yet!
        // It will be called in setParentDimensions() after dimensions are properly computed.
        // Calling it now with width=0, height=0 causes incorrect child positioning.
        return container;
    }

    private static FlexLayoutConfig parseFlexLayout(JsonObject flexJson) {
        FlexLayoutConfig config = new FlexLayoutConfig();

        if (flexJson.has("direction")) {
            config.direction = flexJson.get("direction").getAsString();
        }
        if (flexJson.has("justify")) {
            config.justify = flexJson.get("justify").getAsString();
        }
        if (flexJson.has("alignItems")) {
            config.alignItems = flexJson.get("alignItems").getAsString();
        }
        if (flexJson.has("gap")) {
            config.gap = flexJson.get("gap").getAsInt();
        }

        if (flexJson.has("padding")) {
            JsonObject padding = flexJson.getAsJsonObject("padding");
            if (padding.has("top"))
                config.paddingTop = padding.get("top").getAsInt();
            if (padding.has("right"))
                config.paddingRight = padding.get("right").getAsInt();
            if (padding.has("bottom"))
                config.paddingBottom = padding.get("bottom").getAsInt();
            if (padding.has("left"))
                config.paddingLeft = padding.get("left").getAsInt();
        }

        return config;
    }

    /**
     * Auto-size container based on its children's dimensions. Used when container has width=0 or height=0 (auto-sizing mode). For column direction: width = max child width, height = sum of child heights + gaps For row direction: width = sum of child widths + gaps, height = max child height
     */
    private void autoSizeContainer() {
        String containerId = getId() != null ? getId() : "(root)";
        LOGGER.info("autoSizeContainer [{}]: width={} height={} flexLayout={} children={}", containerId, width, height, flexLayout != null, children.size());

        if (flexLayout == null || children.isEmpty()) {
            LOGGER.info("autoSizeContainer [{}]: skipping (flexLayout={} children={})", containerId, flexLayout != null, children.size());
            return;
        }

        logChildDimensions();

        boolean isRow = "row".equalsIgnoreCase(flexLayout.direction);
        int computedWidth = width == 0 ? computeWidth(isRow) : width;
        int computedHeight = height == 0 ? computeHeight(isRow) : height;

        updateDimensionsIfChanged(containerId, computedWidth, computedHeight);
    }

    private void logChildDimensions() {
        for (int c = 0; c < children.size(); c++) {
            UIElement child = children.get(c);
            LOGGER.info("  child[{}] {}x{}", c, child.getWidth(), child.getHeight());
        }
    }

    private int computeWidth(boolean isRow) {
        LOGGER.info("autoSizeContainer [{}]: calculating width from children", getId() != null ? getId() : "(root)");
        int computedWidth = flexLayout.paddingLeft + flexLayout.paddingRight;
        if (isRow) {
            computedWidth += computeWidthForRow();
        } else {
            computedWidth = computeWidthForColumn(computedWidth);
        }
        return computedWidth;
    }

    private int computeWidthForRow() {
        int width = 0;
        for (int i = 0; i < children.size(); i++) {
            width += children.get(i).getWidth();
            if (i < children.size() - 1) {
                width += flexLayout.gap;
            }
        }
        return width;
    }

    private int computeWidthForColumn(int baseWidth) {
        int maxWidth = baseWidth;
        for (UIElement child : children) {
            maxWidth = Math.max(maxWidth, flexLayout.paddingLeft + child.getWidth() + flexLayout.paddingRight);
        }
        return maxWidth;
    }

    private int computeHeight(boolean isRow) {
        LOGGER.info("autoSizeContainer [{}]: calculating height from children (direction={})", getId() != null ? getId() : "(root)", flexLayout.direction);
        int computedHeight = flexLayout.paddingTop + flexLayout.paddingBottom;
        if (isRow) {
            computedHeight = computeHeightForRow(computedHeight);
        } else {
            computedHeight += computeHeightForColumn();
        }
        return computedHeight;
    }

    private int computeHeightForRow(int baseHeight) {
        int maxHeight = baseHeight;
        for (UIElement child : children) {
            maxHeight = Math.max(maxHeight, flexLayout.paddingTop + child.getHeight() + flexLayout.paddingBottom);
        }
        return maxHeight;
    }

    private int computeHeightForColumn() {
        int height = 0;
        for (int i = 0; i < children.size(); i++) {
            height += children.get(i).getHeight();
            if (i < children.size() - 1) {
                height += flexLayout.gap;
            }
        }
        return height;
    }

    private void updateDimensionsIfChanged(String containerId, int computedWidth, int computedHeight) {
        if (computedWidth != width || computedHeight != height) {
            LOGGER.info("autoSizeContainer [{}]: updating {}x{} -> {}x{}", containerId, width, height, computedWidth, computedHeight);
            width = computedWidth;
            height = computedHeight;
        } else {
            LOGGER.info("autoSizeContainer [{}]: no changes needed", containerId);
        }
    }

    /**
     * Apply flex-grow sizing to children. Children with flex-grow > 0 expand proportionally to fill remaining space.
     */
    private void applyFlexGrowToChildren() {
        if (flexLayout == null || children.isEmpty()) {
            return;
        }

        boolean isRow = "row".equalsIgnoreCase(flexLayout.direction);
        FlexGrowData flexData = collectFlexGrowChildren();

        if (flexData.flexGrowChildren.isEmpty()) {
            return; // No flex-grow children
        }

        if (isRow) {
            applyFlexGrowRow(flexData);
        } else {
            applyFlexGrowColumn(flexData);
        }
    }

    private FlexGrowData collectFlexGrowChildren() {
        List<BaseUIElement> flexGrowChildren = new ArrayList<>();
        int totalFlexGrow = 0;

        for (UIElement child : children) {
            if (child instanceof BaseUIElement baseElement && baseElement.getFlexGrow() > 0) {
                flexGrowChildren.add(baseElement);
                totalFlexGrow += baseElement.getFlexGrow();
            }
        }

        return new FlexGrowData(flexGrowChildren, totalFlexGrow);
    }

    private void applyFlexGrowRow(FlexGrowData flexData) {
        int occupiedSpace = flexLayout.paddingLeft + flexLayout.paddingRight;
        int gap = flexLayout.gap * Math.max(0, children.size() - 1);

        for (UIElement child : children) {
            if (!isFlexGrowChild(child)) {
                occupiedSpace += child.getWidth();
            }
        }

        int availableWidth = Math.max(0, width - occupiedSpace - gap);
        distributeFlexGrowWidth(flexData, availableWidth);
    }

    private void applyFlexGrowColumn(FlexGrowData flexData) {
        int occupiedSpace = flexLayout.paddingLeft + flexLayout.paddingRight;
        int gap = flexLayout.gap * Math.max(0, children.size() - 1);

        for (UIElement child : children) {
            if (!isFlexGrowChild(child)) {
                occupiedSpace += child.getHeight();
            }
        }

        int availableHeight = Math.max(0, height - occupiedSpace - gap);
        distributeFlexGrowHeight(flexData, availableHeight);
    }

    private boolean isFlexGrowChild(UIElement child) {
        return child instanceof BaseUIElement baseElement && baseElement.getFlexGrow() > 0;
    }

    private void distributeFlexGrowWidth(FlexGrowData flexData, int availableWidth) {
        for (BaseUIElement flexChild : flexData.flexGrowChildren) {
            int childFlexGrow = flexChild.getFlexGrow();
            int extraWidth = (int) ((long) availableWidth * childFlexGrow / flexData.totalFlexGrow);
            flexChild.setSizeDimensions(Dimension.parse(flexChild.getWidthDimension().resolve(width) + extraWidth), flexChild.getHeightDimension());
            LOGGER.debug("Applied flex-grow={} to child (row): extra width={}", childFlexGrow, extraWidth);
        }
    }

    private void distributeFlexGrowHeight(FlexGrowData flexData, int availableHeight) {
        for (BaseUIElement flexChild : flexData.flexGrowChildren) {
            int childFlexGrow = flexChild.getFlexGrow();
            int extraHeight = (int) ((long) availableHeight * childFlexGrow / flexData.totalFlexGrow);
            flexChild.setSizeDimensions(flexChild.getWidthDimension(), Dimension.parse(flexChild.getHeightDimension().resolve(height) + extraHeight));
            LOGGER.debug("Applied flex-grow={} to child (column): extra height={}", childFlexGrow, extraHeight);
        }
    }

    private static class FlexGrowData {
        final List<BaseUIElement> flexGrowChildren;
        final int totalFlexGrow;

        FlexGrowData(List<BaseUIElement> flexGrowChildren, int totalFlexGrow) {
            this.flexGrowChildren = flexGrowChildren;
            this.totalFlexGrow = totalFlexGrow;
        }
    }

    /**
     * Apply flex layout to children based on direction and justify alignment. Supports "flex-start", "center", "even", and "between" alignment modes.
     */
    private void applyFlowLayout() {
        if (flexLayout == null) {
            return;
        }

        // Auto-size container based on children if dimensions are 0
        autoSizeContainer();

        int innerX = flexLayout.paddingLeft;
        int innerY = flexLayout.paddingTop;
        int innerWidth = Math.max(0, width - flexLayout.paddingLeft - flexLayout.paddingRight);
        int innerHeight = Math.max(0, height - flexLayout.paddingTop - flexLayout.paddingBottom);

        boolean isRow = "row".equalsIgnoreCase(flexLayout.direction);
        String justify = flexLayout.justify.toLowerCase();

        List<UIElement> participatingChildren = collectParticipatingChildren(isRow);
        if (participatingChildren.isEmpty()) {
            return;
        }

        int totalChildSize = calculateTotalChildSize(participatingChildren, isRow);
        int numChildren = participatingChildren.size();
        int availableSpace = isRow ? innerWidth - totalChildSize : innerHeight - totalChildSize;
        int startPos = isRow ? innerX : innerY;

        FlowLayoutState state = new FlowLayoutState(startPos, availableSpace, numChildren, flexLayout.gap);
        calculateJustifyGap(state, justify);

        positionChildren(participatingChildren, isRow, innerX, innerY, innerWidth, innerHeight, state);
    }

    private List<UIElement> collectParticipatingChildren(boolean isRow) {
        List<UIElement> result = new ArrayList<>();
        for (UIElement child : children) {
            ChildFlowFlags flags = childFlowFlags.get(child);
            if (flags != null && ((isRow && flags.autoX) || (!isRow && flags.autoY))) {
                result.add(child);
            }
        }
        return result;
    }

    private int calculateTotalChildSize(List<UIElement> children, boolean isRow) {
        int total = 0;
        for (UIElement child : children) {
            total += isRow ? child.getWidth() : child.getHeight();
        }
        return total;
    }

    private void calculateJustifyGap(FlowLayoutState state, String justify) {
        if ("center".equalsIgnoreCase(justify)) {
            state.cursor = state.startPos + state.availableSpace / 2;
        } else if ("even".equalsIgnoreCase(justify)) {
            state.mainAxisGap = state.numChildren > 0 ? state.availableSpace / (state.numChildren + 1) : 0;
            state.cursor = state.startPos + state.mainAxisGap;
        } else if ("between".equalsIgnoreCase(justify)) {
            if (state.numChildren == 1) {
                state.cursor = state.startPos + state.availableSpace / 2;
            } else {
                state.mainAxisGap = state.availableSpace / (state.numChildren - 1);
                state.cursor = state.startPos;
            }
        } else {
            state.cursor = state.startPos;
        }
    }

    private void positionChildren(List<UIElement> participatingChildren, boolean isRow, int innerX, int innerY, int innerWidth, int innerHeight, FlowLayoutState state) {
        for (int i = 0; i < participatingChildren.size(); i++) {
            UIElement child = participatingChildren.get(i);
            if (isRow) {
                positionChildRow(child, i, participatingChildren.size(), innerY, innerHeight, state);
            } else {
                positionChildColumn(child, i, participatingChildren.size(), innerX, innerWidth, state);
            }
        }
    }

    private void positionChildRow(UIElement child, int index, int totalChildren, int innerY, int innerHeight, FlowLayoutState state) {
        int childX = state.cursor;
        int childY = child.getY();

        ChildFlowFlags flags = childFlowFlags.get(child);
        if (flags.autoY) {
            childY = alignCrossAxisY(innerY, innerHeight, child.getHeight());
        }

        child.setPosition(childX, childY);
        state.cursor += child.getWidth();
        if (index < totalChildren - 1) {
            state.cursor += state.mainAxisGap;
        }
    }

    private void positionChildColumn(UIElement child, int index, int totalChildren, int innerX, int innerWidth, FlowLayoutState state) {
        int childX = child.getX();
        int childY = state.cursor;

        ChildFlowFlags flags = childFlowFlags.get(child);
        if (flags.autoX) {
            childX = alignCrossAxisX(innerX, innerWidth, child.getWidth());
        }

        child.setPosition(childX, childY);
        state.cursor += child.getHeight();
        if (index < totalChildren - 1) {
            state.cursor += state.mainAxisGap;
        }
    }

    private static class FlowLayoutState {
        int cursor;
        int mainAxisGap;
        final int startPos;
        final int availableSpace;
        final int numChildren;

        FlowLayoutState(int startPos, int availableSpace, int numChildren, int gap) {
            this.startPos = startPos;
            this.availableSpace = availableSpace;
            this.numChildren = numChildren;
            this.cursor = startPos;
            this.mainAxisGap = gap;
        }
    }

    private int alignCrossAxisY(int innerY, int innerHeight, int childHeight) {
        return switch (flexLayout.alignItems) {
            case "center" -> innerY + (innerHeight - childHeight) / 2;
            case "flex-end" -> innerY + Math.max(0, innerHeight - childHeight);
            default -> innerY; // flex-start
        };
    }

    private int alignCrossAxisX(int innerX, int innerWidth, int childWidth) {
        return switch (flexLayout.alignItems) {
            case "center" -> innerX + (innerWidth - childWidth) / 2;
            case "flex-end" -> innerX + Math.max(0, innerWidth - childWidth);
            default -> innerX; // flex-start
        };
    }

    /**
     * Set parent dimensions and propagate to children.
     */
    @Override
    public void setParentDimensions(int parentWidth, int parentHeight) {
        String containerId = getId() != null ? getId() : "(root)";
        LOGGER.info("Container.setParentDimensions [{}]: parent={}x{}", containerId, parentWidth, parentHeight);

        super.setParentDimensions(parentWidth, parentHeight);
        LOGGER.info("Container [{}] computed dims: {}x{}", containerId, this.width, this.height);

        // Update all children's parent dimensions based on this container's computed size
        LOGGER.info("Cascading dimensions to {} children", children.size());
        for (UIElement child : children) {
            if (child instanceof BaseUIElement baseElement) {
                String childId = baseElement.getId() != null ? baseElement.getId() : "(unnamed)";
                LOGGER.info("  Cascading to child [{}]: parent dims={}x{}", childId, this.width, this.height);
                baseElement.setParentDimensions(this.width, this.height);
            }
        }

        // Apply flex-grow to children that have flex-grow set
        applyFlexGrowToChildren();

        LOGGER.info("Container [{}] applying flow layout with {}x{} space", containerId, this.width, this.height);
        applyFlowLayout();
        updateContentHeight();
    }

    public void addChild(UIElement child) {
        addChild(child, false, false);
    }

    public void addChild(UIElement child, boolean autoX, boolean autoY) {
        children.add(child);
        childFlowFlags.put(child, new ChildFlowFlags(autoX, autoY));
        LOGGER.debug("Added child to container: {} (autoX={}, autoY={})", child.getClass().getSimpleName(), autoX, autoY);
        // DO NOT set parent dimensions or apply layout here!
        // Container dimensions are not computed yet (still 0x0 from initialization).
        // Children will get proper parent dimensions when this container's setParentDimensions()
        // is called later, which will cascade to children and then call applyFlowLayout().
    }

    public void removeChild(UIElement child) {
        boolean removed = children.remove(child);
        childFlowFlags.remove(child);
        LOGGER.debug("Removed child from container: {} (success: {})", child.getClass().getSimpleName(), removed);
        applyFlowLayout();
        updateContentHeight();
    }

    public List<UIElement> getChildren() {
        return new ArrayList<>(children);
    }

    public RenderStyle getRenderStyle() {
        return renderStyle;
    }

    public void setRenderStyle(RenderStyle renderStyle) {
        this.renderStyle = renderStyle;
    }

    private void updateContentHeight() {
        contentHeight = 0;
        for (UIElement child : children) {
            contentHeight = Math.max(contentHeight, child.getY() + child.getHeight());
        }
        maxScrollY = Math.max(0, contentHeight - height);
    }

    /**
     * Override setSize to update child parent dimensions.
     */
    @Override
    public void setSize(int width, int height) {
        if (this.width != width || this.height != height) {
            LOGGER.debug("Container size changed from {}x{} to {}x{}", this.width, this.height, width, height);
        }
        super.setSize(width, height);
        // Update all children's parent dimensions
        for (UIElement child : children) {
            if (child instanceof BaseUIElement baseElement) {
                baseElement.setParentDimensions(this.width, this.height);
            }
        }
        applyFlowLayout();
        updateContentHeight();
    }

    /**
     * Override setSizeDimensions to update child parent dimensions.
     */
    @Override
    public void setSizeDimensions(Dimension width, Dimension height) {
        super.setSizeDimensions(width, height);
        // Update all children's parent dimensions
        for (UIElement child : children) {
            if (child instanceof BaseUIElement baseElement) {
                baseElement.setParentDimensions(this.width, this.height);
            }
        }
        applyFlowLayout();
        updateContentHeight();
    }

    @Override
    public boolean isMouseInside(int mouseX, int mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        String containerId = getContainerId();

        if (!isMouseInside((int) mouseX, (int) mouseY)) {
            LOGGER.trace("Container {} - mouse ({},{}) outside container bounds", containerId, (int) mouseX, (int) mouseY);
            return false;
        }

        LOGGER.trace("Container {} - mouse ({},{}) inside, checking {} children", containerId, (int) mouseX, (int) mouseY, children.size());

        return checkChildrenForClick(mouseX, mouseY, button, containerId);
    }

    private String getContainerId() {
        return getId() != null ? getId() : "(unnamed)";
    }

    private boolean checkChildrenForClick(double mouseX, double mouseY, int button, String containerId) {
        // Check children in reverse order (top to bottom rendering)
        for (int i = children.size() - 1; i >= 0; i--) {
            UIElement child = children.get(i);
            if (handleChildClick(child, mouseX, mouseY, button, containerId)) {
                return true;
            }
        }

        LOGGER.trace("Container {} - no children matched click", containerId);
        return false;
    }

    private boolean handleChildClick(UIElement child, double mouseX, double mouseY, int button, String containerId) {
        int adjustedChildY = child.getY() - scrollY;

        // Check if child is visible in container viewport
        if (!isChildVisibleInViewport(adjustedChildY, child.getHeight())) {
            return false;
        }

        // Calculate child's screen position
        int childScreenX = x + child.getX();
        int childScreenY = y + adjustedChildY;

        // Check if mouse is within child's bounds
        if (!isMouseInChild(mouseX, mouseY, childScreenX, childScreenY, child)) {
            return false;
        }

        String childId = getChildId(child);
        LOGGER.trace("Container {} - child {} ({},{}) matches mouse, delegating click", containerId, childId, childScreenX, childScreenY);

        // Pass container-relative coordinates to child
        double childRelativeX = mouseX - childScreenX + child.getX();
        double childRelativeY = mouseY - childScreenY + child.getY();
        return child.mouseClicked(childRelativeX, childRelativeY, button);
    }

    private boolean isChildVisibleInViewport(int adjustedChildY, int childHeight) {
        return adjustedChildY + childHeight > 0 && adjustedChildY < height;
    }

    private boolean isMouseInChild(double mouseX, double mouseY, int childScreenX, int childScreenY, UIElement child) {
        return mouseX >= childScreenX && mouseX < childScreenX + child.getWidth() && mouseY >= childScreenY && mouseY < childScreenY + child.getHeight();
    }

    private String getChildId(UIElement child) {
        String childId = child instanceof BaseUIElement baseElement ? baseElement.getId() : "?";
        return childId != null ? childId : "(unnamed)";
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isMouseInside((int) mouseX, (int) mouseY)) {
            return false;
        }

        this.scrollY -= (int) (scrollY * SCROLL_SPEED);
        this.scrollY = Math.clamp(this.scrollY, 0, maxScrollY);

        // Check if any child handles the scroll
        for (UIElement child : children) {
            int adjustedChildY = child.getY() - this.scrollY;
            if (adjustedChildY + child.getHeight() > 0 && adjustedChildY < height) {
                // Calculate child's screen position
                int childScreenX = x + child.getX();
                int childScreenY = y + adjustedChildY;

                // Pass container-relative coordinates to child
                double childRelativeX = mouseX - childScreenX + child.getX();
                double childRelativeY = mouseY - childScreenY + child.getY();
                if (child.mouseScrolled(childRelativeX, childRelativeY, scrollX, scrollY)) {
                    return true;
                }
            }
        }

        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Propagate to all children
        for (UIElement child : children) {
            if (child.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return false;
    }

    private void renderBackground(GuiGraphics graphics) {
        if (renderStyle.hasFillColor()) {
            int fillColor = renderStyle.getFillColor();
            graphics.fill(x, y, x + width, y + height, fillColor);
        }
    }

    private void renderBorder(GuiGraphics graphics) {
        if (renderStyle.hasBorderColor()) {
            int borderWeight = renderStyle.getBorderWeight();
            int borderColor = renderStyle.getBorderColor();

            for (int i = 0; i < borderWeight; i++) {
                graphics.fill(x + i, y + i, x + width - i, y + i + 1, borderColor);
                graphics.fill(x + i, y + height - i - 1, x + width - i, y + height - i, borderColor);
                graphics.fill(x + i, y + i, x + i + 1, y + height - i, borderColor);
                graphics.fill(x + width - i - 1, y + i, x + width - i, y + height - i, borderColor);
            }
        }
    }

    private void renderChild(UIElement child, GuiGraphics graphics, int mouseX, int mouseY, float partialTick, String containerId) {
        int adjustedChildY = child.getY() - scrollY;

        if (isChildVisible(child, adjustedChildY)) {
            renderVisibleChild(child, graphics, mouseX, mouseY, partialTick, containerId, adjustedChildY);
        } else {
            logChildOutOfBounds(child, containerId, adjustedChildY);
        }
    }

    private boolean isChildVisible(UIElement child, int adjustedChildY) {
        return adjustedChildY + child.getHeight() > 0 && adjustedChildY < height;
    }

    private void renderVisibleChild(UIElement child, GuiGraphics graphics, int mouseX, int mouseY, float partialTick, String containerId, int adjustedChildY) {
        String childId = child instanceof BaseUIElement baseElement ? baseElement.getId() : "?";
        String childType = child.getClass().getSimpleName();
        LOGGER.trace("Container {} - rendering child {} ({}) at adjustedY={}, height={}", containerId, childId, childType, adjustedChildY, child.getHeight());

        int originalX = child.getX();
        int originalY = child.getY();

        int childScreenX = x + originalX;
        int childScreenY = y + adjustedChildY;

        child.setPosition(childScreenX, childScreenY);
        child.render(graphics, mouseX, mouseY, partialTick);
        child.setPosition(originalX, originalY);
    }

    private void logChildOutOfBounds(UIElement child, String containerId, int adjustedChildY) {
        String childId = child instanceof BaseUIElement baseElement ? baseElement.getId() : "?";
        if (adjustedChildY + child.getHeight() <= 0) {
            LOGGER.trace("Container {} - child {} is above bounds (adjustedY={}, height={}, skipping)", containerId, childId, adjustedChildY, child.getHeight());
        } else {
            LOGGER.trace("Container {} - child {} is below bounds (adjustedY={}, containerH={}, skipping)", containerId, childId, adjustedChildY, height);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        try {
            String containerId = getId() != null ? getId() : "(unnamed)";

            renderBackground(graphics);
            renderBorder(graphics);

            for (UIElement child : children) {
                renderChild(child, graphics, mouseX, mouseY, partialTick, containerId);
            }
        } catch (Exception e) {
            LOGGER.error("Error rendering container", e);
        }
    }
}
