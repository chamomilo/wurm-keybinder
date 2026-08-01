package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.gui.text.TextFont;
import org.keybinder.wurm.i18n.Messages;

/** Shared, fixed-alpha drag destination marker for Keybinder lists. */
final class KeybinderDragIndicator {
    private static final float YELLOW_RED = 0.92f;
    private static final float YELLOW_GREEN = 0.77f;
    private static final float YELLOW_BLUE = 0.42f;
    private static final float BLOCKED_RED = 0.92f;
    private static final float BLOCKED_GREEN = 0.18f;
    private static final float BLOCKED_BLUE = 0.12f;
    private static final int LINE_HEIGHT = 2;
    private static final int LABEL_GAP = 6;
    static final int INSERT_GAP_HEIGHT = 18;

    private KeybinderDragIndicator() { }

    static final class InsertionGap extends FlexComponent {
        private final int horizontalInset;
        private boolean indicatorVisible = true;

        InsertionGap(String name, int horizontalInset) {
            super(name);
            this.horizontalInset = Math.max(0, horizontalInset);
            setSize(1, INSERT_GAP_HEIGHT);
        }

        @Override
        protected void renderComponent(Queue queue, float ignoredAlpha) {
            if (!indicatorVisible) return;
            int drawWidth = Math.max(1, width - horizontalInset);
            paintInsert(this, queue, x + horizontalInset,
                    y + height / 2, drawWidth);
        }

        void setIndicatorVisible(boolean visible) {
            indicatorVisible = visible;
        }
    }

    static void place(WurmArrayPanel<FlexComponent> panel,
                      InsertionGap gap, int componentIndex) {
        int current = panel.components.indexOf(gap);
        if (current == componentIndex) return;
        if (current >= 0) panel.removeComponent(gap);
        panel.addComponent(gap);
        panel.components.remove(gap);
        int bounded = Math.max(0, Math.min(componentIndex, panel.components.size()));
        panel.components.add(bounded, gap);
        panel.componentResized();
    }

    static void remove(WurmArrayPanel<FlexComponent> panel, InsertionGap gap) {
        if (panel.components.contains(gap)) {
            panel.removeComponent(gap);
            panel.componentResized();
        }
    }

    static int keybindGapComponentIndex(int insertion) {
        return 1 + Math.max(0, insertion);
    }

    static int actionGapComponentIndex(int insertion) {
        return 3 + Math.max(0, insertion);
    }

    static int variantGapComponentIndex(int insertion) {
        return 2 + Math.max(0, insertion) * 2;
    }

    static void paintInsert(WurmComponent owner, Queue queue,
                            int left, int centerY, int width) {
        paintLineLabel(owner, queue, left, centerY, width,
                Messages.text("drag.insert_here"), false);
    }

    static void paintMerge(WurmComponent owner, Queue queue,
                           int left, int top, int width, int height, boolean blocked) {
        float red = blocked ? BLOCKED_RED : YELLOW_RED;
        float green = blocked ? BLOCKED_GREEN : YELLOW_GREEN;
        float blue = blocked ? BLOCKED_BLUE : YELLOW_BLUE;
        int border = 2;
        owner.fillRect(queue, red, green, blue, 1.0f,
                left, top, width, border);
        owner.fillRect(queue, red, green, blue, 1.0f,
                left, top + Math.max(0, height - border), width, border);
        owner.fillRect(queue, red, green, blue, 1.0f,
                left, top, border, height);
        owner.fillRect(queue, red, green, blue, 1.0f,
                left + Math.max(0, width - border), top, border, height);
        paintLabel(owner, queue, left, top + height / 2, width,
                Messages.text("drag.merge_with_keybind"), red, green, blue);
    }

    private static void paintLineLabel(WurmComponent owner, Queue queue,
                                       int left, int centerY, int width,
                                       String label, boolean blocked) {
        TextFont font = TextFont.getFixedSizeText();
        int labelWidth = font.getWidth(label);
        int labelLeft = left + Math.max(0, (width - labelWidth) / 2);
        int labelRight = labelLeft + labelWidth;
        int lineTop = centerY - LINE_HEIGHT / 2;
        float red = blocked ? BLOCKED_RED : YELLOW_RED;
        float green = blocked ? BLOCKED_GREEN : YELLOW_GREEN;
        float blue = blocked ? BLOCKED_BLUE : YELLOW_BLUE;

        int leftLineWidth = Math.max(0, labelLeft - LABEL_GAP - left);
        if (leftLineWidth > 0)
            owner.fillRect(queue, red, green, blue, 1.0f,
                    left, lineTop, leftLineWidth, LINE_HEIGHT);
        int rightLineStart = Math.min(left + width, labelRight + LABEL_GAP);
        int rightLineWidth = Math.max(0, left + width - rightLineStart);
        if (rightLineWidth > 0)
            owner.fillRect(queue, red, green, blue, 1.0f,
                    rightLineStart, lineTop, rightLineWidth, LINE_HEIGHT);

        paintLabel(owner, queue, left, centerY, width, label, red, green, blue);
    }

    private static void paintLabel(WurmComponent owner, Queue queue,
                                   int left, int centerY, int width, String label,
                                   float red, float green, float blue) {
        TextFont font = TextFont.getFixedSizeText();
        int labelWidth = font.getWidth(label);
        int labelLeft = left + Math.max(0, (width - labelWidth) / 2);
        int textTop = centerY - font.getHeight() / 2;
        owner.fillRect(queue, 0.10f, 0.08f, 0.05f, 1.0f,
                Math.max(left, labelLeft - 3), textTop - 1,
                Math.min(width, labelWidth + 6), font.getHeight() + 2);
        font.moveTo(labelLeft, textTop + font.getAscent());
        font.paint(queue, label, red, green, blue, 1.0f);
    }
}
