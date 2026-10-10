package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.backend.Queue;

/** Stable, font-independent row controls shared by both Keybinder windows. */
final class KeybinderGlyphButton extends KeybinderUiButton {
    enum Kind { PLUS_BOX, MINUS_BOX, CLOSE_BOX, EXTRACT_UP, RECORD_DOT, REFRESH }

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
        paintSurface(queue);
        float brightness = isEnabled() ? (hovered ? 1.0f : 0.82f) : 0.36f;
        if (kind == Kind.PLUS_BOX || kind == Kind.MINUS_BOX || kind == Kind.CLOSE_BOX) {
            drawBoxIcon(queue, brightness, kind);
        } else if (kind == Kind.EXTRACT_UP) {
            drawExtractUp(queue, brightness);
        } else if (kind == Kind.REFRESH) {
            drawRefresh(queue, brightness);
        } else {
            fillRect(queue, 0.95f, 0.18f, 0.12f, 1.0f,
                    x + width / 2 - 2, y + height / 2 - 2, 5, 5);
        }
    }

    private void drawExtractUp(Queue queue, float brightness) {
        int left = x + (width - 14) / 2;
        int top = y + (height - 14) / 2;
        float green = brightness * 0.90f;
        float blue = brightness * 0.72f;
        // A compact curved arrow leaving the current row and pointing upward.
        fillRect(queue, brightness, green, blue, 1.0f, left + 8, top + 2, 2, 10);
        fillRect(queue, brightness, green, blue, 1.0f, left + 6, top + 4, 2, 2);
        fillRect(queue, brightness, green, blue, 1.0f, left + 10, top + 4, 2, 2);
        fillRect(queue, brightness, green, blue, 1.0f, left + 4, top + 8, 4, 2);
        fillRect(queue, brightness, green, blue, 1.0f, left + 2, top + 10, 2, 3);
        fillRect(queue, brightness, green, blue, 1.0f, left + 3, top + 12, 5, 2);
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
        int size = 16;
        int left = x + (width - size) / 2;
        int top = y + (height - size) / 2;
        org.chamomilo.wurm.ui.v1.UiIcon icon = boxKind == Kind.CLOSE_BOX
                ? org.chamomilo.wurm.ui.v1.UiIcon.CLOSE : boxKind == Kind.PLUS_BOX
                ? org.chamomilo.wurm.ui.v1.UiIcon.PLUS : org.chamomilo.wurm.ui.v1.UiIcon.MINUS;
        icon.paint(canvas, org.chamomilo.wurm.ui.v1.UiPainter.buttonCaptionColor(isEnabled(), motion.hover()),
                1f, left, top, size);
    }
}
