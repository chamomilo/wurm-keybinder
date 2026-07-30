package org.keybinder.wurm.ui;

/** Pixel-based layout helpers. Production callers supply Wurm font measurements. */
public final class LocalizedLayout {
    public interface TextMeasurer {
        int width(String text);
    }

    private LocalizedLayout() {}

    public static int controlWidth(
            String text, int minimum, int horizontalPadding, TextMeasurer measurer) {
        return Math.max(minimum, measurer.width(text) + horizontalPadding);
    }

    public static int maximumOptionWidth(
            String[] options, int horizontalPadding, TextMeasurer measurer) {
        int width = 0;
        for (String option : options) width = Math.max(width, measurer.width(option));
        return width + horizontalPadding;
    }

    /** Width of a fixed horizontal row, independent of its mutable GUI panel size. */
    public static int horizontalRowWidth(int gap, int... componentWidths) {
        int width = 0;
        for (int componentWidth : componentWidths) width += Math.max(0, componentWidth);
        if (componentWidths.length > 1) width += gap * (componentWidths.length - 1);
        return width;
    }
}
