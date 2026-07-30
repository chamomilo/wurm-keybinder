package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.backend.Queue;

/** Stable, font-independent row controls shared by both Keybinder windows. */
final class KeybinderGlyphButton extends WButton {
    enum Kind { PLUS_BOX, MINUS_BOX, CLOSE_BOX, RECORD_DOT, REFRESH }

    private Kind kind;

    KeybinderGlyphButton(Kind kind, ButtonListener listener, String tooltip) {
        super("", listener);
        this.kind = kind;
        setSize(20, 20);
        setHoverString(tooltip);
    }

    void setKind(Kind kind) {
        this.kind = kind;
    }

    @Override
    protected void renderComponent(Queue queue, float ignoredAlpha) {
        float brightness = isEnabled() ? (hovered ? 1.0f : 0.82f) : 0.36f;
        if (kind == Kind.PLUS_BOX || kind == Kind.MINUS_BOX || kind == Kind.CLOSE_BOX) {
            drawBoxIcon(queue, brightness, kind);
        } else if (kind == Kind.REFRESH) {
            drawRefresh(queue, brightness);
        } else {
            fillRect(queue, 0.95f, 0.18f, 0.12f, 1.0f,
                    x + width / 2 - 2, y + height / 2 - 2, 5, 5);
        }
    }

    private void drawRefresh(Queue queue, float brightness) {
        int left = x + (width - 14) / 2;
        int top = y + (height - 14) / 2;
        float green = brightness * 0.90f;
        float blue = brightness * 0.72f;
        // Two opposing curved arrows, kept pixel-sharp at Wurm's 20px button size.
        fillRect(queue, brightness, green, blue, 1.0f, left + 4, top + 2, 6, 2);
        fillRect(queue, brightness, green, blue, 1.0f, left + 2, top + 4, 2, 4);
        fillRect(queue, brightness, green, blue, 1.0f, left + 9, top, 2, 5);
        fillRect(queue, brightness, green, blue, 1.0f, left + 11, top + 2, 2, 2);
        fillRect(queue, brightness, green, blue, 1.0f, left + 4, top + 10, 6, 2);
        fillRect(queue, brightness, green, blue, 1.0f, left + 10, top + 6, 2, 4);
        fillRect(queue, brightness, green, blue, 1.0f, left + 3, top + 9, 2, 5);
        fillRect(queue, brightness, green, blue, 1.0f, left + 1, top + 10, 2, 2);
    }

    private void drawBoxIcon(Queue queue, float brightness, Kind boxKind) {
        int size = 11;
        int left = x + (width - size) / 2;
        int top = y + (height - size) / 2;
        boolean close = boxKind == Kind.CLOSE_BOX;
        float red = close && hovered ? 1.00f : brightness;
        float green = close && hovered ? 0.55f : brightness * 0.90f;
        float blue = close && hovered ? 0.42f : brightness * 0.72f;
        fillRect(queue, red, green, blue, 1.0f, left, top, size, 1);
        fillRect(queue, red, green, blue, 1.0f, left, top + size - 1, size, 1);
        fillRect(queue, red, green, blue, 1.0f, left, top, 1, size);
        fillRect(queue, red, green, blue, 1.0f, left + size - 1, top, 1, size);
        if (close) {
            for (int offset = 3; offset <= 7; offset++) {
                fillRect(queue, red, green, blue, 1.0f, left + offset, top + offset, 1, 1);
                fillRect(queue, red, green, blue, 1.0f,
                        left + size - 1 - offset, top + offset, 1, 1);
            }
        } else {
            fillRect(queue, red, green, blue, 1.0f, left + 3, top + 5, 5, 1);
            if (boxKind == Kind.PLUS_BOX)
                fillRect(queue, red, green, blue, 1.0f, left + 5, top + 3, 1, 5);
        }
    }
}
