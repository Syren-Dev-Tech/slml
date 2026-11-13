package com.github.syren_dev_tech.slml.builder;

/**
 * Represents a dimension value that can be either absolute (pixels) or relative (percentage). Supports values like: - 100 (absolute pixels) - "50%" (percentage of parent) - "100" (string format, absolute pixels)
 */
public class Dimension {
    private final int absoluteValue;
    private final float percentageValue;
    private final boolean isPercentage;

    /**
     * Constructor for absolute dimension (pixels).
     */
    private Dimension(int absoluteValue) {
        this.absoluteValue = absoluteValue;
        this.percentageValue = 0;
        this.isPercentage = false;
    }

    /**
     * Constructor for percentage dimension.
     */
    private Dimension(float percentageValue) {
        this.absoluteValue = 0;
        this.percentageValue = percentageValue;
        this.isPercentage = true;
    }

    /**
     * Parse a dimension from various input formats. Accepts: - Integer: 100 - String percentage: "50%", "50.5%" - String integer: "100"
     *
     * @param value The value to parse (int, float, or String)
     * @return A new Dimension object
     * @throws IllegalArgumentException if the format is invalid
     */
    public static Dimension parse(Object value) {
        if (value == null) {
            return new Dimension(0);
        }

        if (value instanceof Number num) {
            return new Dimension(num.intValue());
        }

        if (value instanceof String str) {
            str = str.trim();

            if (str.endsWith("%")) {
                String percentStr = str.substring(0, str.length() - 1).trim();
                try {
                    float percentage = Float.parseFloat(percentStr);
                    return new Dimension(percentage);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid percentage format: " + str, e);
                }
            } else {
                try {
                    int absolute = Integer.parseInt(str);
                    return new Dimension(absolute);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("Invalid dimension format: " + str, e);
                }
            }
        }

        throw new IllegalArgumentException("Unsupported dimension type: " + value.getClass().getName());
    }

    /**
     * Get the computed pixel value given a parent dimension. If this dimension is absolute, returns the absolute value. If this dimension is percentage, returns (percentage / 100) * parentValue.
     *
     * @param parentValue The parent dimension in pixels (used for percentage calculation)
     * @return The computed pixel value
     */
    public int resolve(int parentValue) {
        if (isPercentage) {
            return Math.round((percentageValue / 100f) * parentValue);
        } else {
            return absoluteValue;
        }
    }

    /**
     * Check if this dimension is a percentage value.
     */
    public boolean isPercentage() {
        return isPercentage;
    }

    /**
     * Get the percentage value (only valid if isPercentage() is true).
     */
    public float getPercentageValue() {
        return percentageValue;
    }

    /**
     * Get the absolute value (only valid if isPercentage() is false).
     */
    public int getAbsoluteValue() {
        return absoluteValue;
    }

    @Override
    public String toString() {
        if (isPercentage) {
            return percentageValue + "%";
        } else {
            return absoluteValue + "px";
        }
    }
}
