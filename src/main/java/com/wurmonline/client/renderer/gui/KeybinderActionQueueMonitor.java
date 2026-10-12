package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.PickData;
import com.wurmonline.client.renderer.gui.text.TextFont;
import com.wurmonline.client.renderer.gui.text.ChamomiloUiV1Fonts;
import org.chamomilo.wurm.ui.v1.*;
import org.keybinder.wurm.i18n.Messages;
import org.keybinder.wurm.queue.ActionQueueEntry;
import org.keybinder.wurm.ui.ActionQueueMonitorController;
import org.keybinder.wurm.ui.QueueMonitorSide;

import java.util.Collections;
import java.util.List;

/**
 * Edge-anchored action-queue strip. The collapsed state intentionally paints only
 * lamps and its direction arrow on a dark surface; it has no title, close button,
 * labels, or footer.
 * Width changes in {@link #gameTick()} are the deliberate slide animation, not
 * minimum-size correction. All custom paint uses stable full opacity so HUD
 * hover/fade alpha changes cannot make the component flicker.
 */
public final class KeybinderActionQueueMonitor extends StaticComponent {
    private static final int MAX_SLOTS = 10;
    private static final int COLLAPSED_WIDTH = 28;
    private static final int MIN_EXPANDED_WIDTH = 220;
    private static final int MAX_EXPANDED_WIDTH = 520;
    private static final int PADDING = 4;
    private static final int LAMP_SIZE = 20;
    private static final int LAMP_LEFT = 4;
    private static final int TEXT_LEFT = 30;
    private static final int TEXT_RIGHT_PADDING = 10;
    private static final int TEXT_LAMP_GAP = 4;
    private static final int ANIMATION_MILLIS = 180;
    private static final int CLICK_SLOP = 3;

    private final ActionQueueMonitorController controller;
    private UiScale scale;
    private TextFont actionFont, detailFont, titleFont;
    private int rowHeight, toggleHeight;
    private final ChamomiloUiV1Canvas canvas = new ChamomiloUiV1Canvas(this);
    private List<ActionQueueEntry> entries = Collections.emptyList();
    private int slots = MAX_SLOTS;
    private QueueMonitorSide side = QueueMonitorSide.RIGHT;
    private boolean expanded;
    private int animationFrom;
    private int targetWidth;
    private long animationStarted;
    private int pressX;
    private int pressY;
    private int pressedLamp = -1;
    private boolean pressedToggle;
    private boolean pressed;

    public KeybinderActionQueueMonitor(ActionQueueMonitorController controller) {
        super("Keybinder action queue monitor");
        this.controller = controller;
        refreshScale();
        animationFrom = targetWidth = px(COLLAPSED_WIDTH);
        setSize(targetWidth, panelHeight(slots));
    }

    private int px(int nominal) { return scale.pixels(nominal); }

    private void refreshScale() {
        UiScale next = KeybinderUi.viewportScale();
        if (scale != null && Float.compare(scale.factor, next.factor) == 0) return;
        scale = next;
        actionFont = ChamomiloUiV1Fonts.caption(KeybinderUi.fontPixels(scale, 16), false, UiDensity.LOW);
        detailFont = ChamomiloUiV1Fonts.caption(KeybinderUi.fontPixels(scale, 14), false, UiDensity.LOW);
        titleFont = ChamomiloUiV1Fonts.caption(KeybinderUi.fontPixels(scale, 16), true, UiDensity.HIGH);
        rowHeight = Math.max(px(LAMP_SIZE), actionFont.getHeight() + detailFont.getHeight() + px(4));
        toggleHeight = Math.max(px(24), titleFont.getHeight() + px(4));
    }

    @Override
    public void gameTick() {
        refreshScale();
        int requestedSlots = controller == null ? MAX_SLOTS
                : controller.getQueueMonitorSlots();
        slots = Math.max(1, Math.min(MAX_SLOTS, requestedSlots));
        entries = controller == null ? Collections.<ActionQueueEntry>emptyList()
                : controller.getMonitoredActions();
        if (entries == null) entries = Collections.emptyList();
        QueueMonitorSide requestedSide = controller == null ? QueueMonitorSide.RIGHT
                : controller.getQueueMonitorSide();
        side = requestedSide == null ? QueueMonitorSide.RIGHT : requestedSide;

        int desired = expanded ? expandedWidth() : px(COLLAPSED_WIDTH);
        if (desired != targetWidth) beginAnimation(desired);
        int animatedWidth = animatedWidth(System.currentTimeMillis());
        int desiredHeight = panelHeight(slots);
        if (width != animatedWidth || height != desiredHeight)
            setSize(animatedWidth, desiredHeight);
        if (hud != null) {
            int anchoredX = KeybinderActionQueueMonitorHitBox.anchoredX(
                    side == QueueMonitorSide.LEFT, hud.getWidth(), width);
            int anchoredY = Math.max(0, (hud.getHeight() - height) / 2);
            if (x != anchoredX || y != anchoredY) setPosition(anchoredX, anchoredY);
        }
    }

    @Override
    protected void renderComponent(Queue queue, float ignoredAlpha) {
        UiPainter.background(canvas.begin(queue), UiBackground.LEATHER, 1f, x, y, width, height);
        UiPainter.frame(canvas, px(3), 1f, x, y, width, height);
        paintToggleArrow(queue);
        if (width == targetWidth && expanded && width > px(COLLAPSED_WIDTH))
            paintTitle(queue);

        for (int index = 0; index < slots; index++) {
            ActionQueueEntry entry = index < entries.size() ? entries.get(index) : null;
            int rowTop = y + px(PADDING) + toggleHeight + index * rowHeight;
            paintLamp(queue, index, lampColumnLeft(),
                    rowTop + Math.max(0, (rowHeight - px(LAMP_SIZE)) / 2), entry);
            if (width == targetWidth && expanded && width > px(COLLAPSED_WIDTH))
                paintEntry(queue, rowTop, entry);
        }
    }

    private void paintLamp(Queue queue, int index, int left, int top,
                           ActionQueueEntry entry) {
        UiPainter.button(canvas, entry == null ? .45f : 1f, 0, 0, scale, 1f,
                left, top, px(LAMP_SIZE), px(LAMP_SIZE));
        canvas.fill(entry == null ? UiColor.MUTED : UiColor.TEXT, 1f,
                left + px(7), top + px(7), px(6), px(6));
    }

    private void paintToggleArrow(Queue queue) {
        int arrowLeft = lampColumnLeft() + (px(LAMP_SIZE) - px(16)) / 2;
        int centerY = y + px(PADDING) + toggleHeight / 2;
        boolean pointsRight = KeybinderActionQueueMonitorHitBox.arrowPointsRight(
                side == QueueMonitorSide.LEFT, expanded);
        (pointsRight ? UiIcon.CHEVRON_RIGHT : UiIcon.CHEVRON_LEFT)
                .paint(canvas, UiColor.TEXT, 1f, arrowLeft, centerY - px(8), px(16));
    }

    private void paintTitle(Queue queue) {
        int textLeft;
        int textRight;
        if (side == QueueMonitorSide.LEFT) {
            textLeft = x + px(TEXT_RIGHT_PADDING);
            textRight = lampColumnLeft() - px(TEXT_LAMP_GAP);
        } else {
            textLeft = x + px(TEXT_LEFT);
            textRight = x + width - px(TEXT_RIGHT_PADDING);
        }
        int available = Math.max(1, textRight - textLeft);
        String title = KeybinderUi.fit(titleFont, Messages.text("queue.monitor.title"), available);
        int paintedLeft = KeybinderActionQueueMonitorHitBox.alignedTextLeft(
                side == QueueMonitorSide.LEFT, textLeft, textRight,
                titleFont.getWidth(title));
        titleFont.moveTo(paintedLeft, y + px(PADDING) + (toggleHeight - titleFont.getHeight()) / 2 + titleFont.getAscent());
        titleFont.paint(queue, title, UiColor.TEXT.red, UiColor.TEXT.green, UiColor.TEXT.blue, 1.0f);
    }

    private void paintEntry(Queue queue, int rowTop, ActionQueueEntry entry) {
        int textLeft;
        int textRight;
        int available;
        if (side == QueueMonitorSide.LEFT) {
            textLeft = x + px(TEXT_RIGHT_PADDING);
            textRight = lampColumnLeft() - px(TEXT_LAMP_GAP);
            available = Math.max(1, textRight - textLeft);
        } else {
            textLeft = x + px(TEXT_LEFT);
            available = Math.max(1, width - px(TEXT_LEFT) - px(TEXT_RIGHT_PADDING));
            textRight = textLeft + available;
        }
        String action = actionText(entry);
        String detail = detailText(entry);
        String fittedAction = KeybinderUi.fit(actionFont, action, available);
        String fittedDetail = KeybinderUi.fit(detailFont, detail, available);
        boolean mirrored = side == QueueMonitorSide.LEFT;

        actionFont.moveTo(KeybinderActionQueueMonitorHitBox.alignedTextLeft(
                mirrored, textLeft, textRight, actionFont.getWidth(fittedAction)),
                rowTop + px(2) + actionFont.getAscent());
        UiColor color = entry == null ? UiColor.MUTED : UiColor.TEXT;
        actionFont.paint(queue, fittedAction, color.red, color.green, color.blue, 1.0f);
        detailFont.moveTo(KeybinderActionQueueMonitorHitBox.alignedTextLeft(
                mirrored, textLeft, textRight, detailFont.getWidth(fittedDetail)),
                rowTop + px(2) + actionFont.getHeight() + detailFont.getAscent());
        detailFont.paint(queue, fittedDetail, UiColor.MUTED.red, UiColor.MUTED.green, UiColor.MUTED.blue, 1.0f);
    }

    @Override public void pick(PickData data, int mouseX, int mouseY) {
        if (!contains(mouseX, mouseY)) return;
        int relativeY = mouseY - y - px(PADDING) - toggleHeight;
        if (relativeY < 0) return;
        int index = relativeY / rowHeight;
        if (index >= slots || index >= entries.size()) return;
        ActionQueueEntry entry = entries.get(index);
        if (entry != null) { data.addText(actionText(entry)); data.addText(detailText(entry)); }
    }

    @Override
    protected void leftPressed(int mouseX, int mouseY, int clickCount) {
        pressed = true;
        pressX = mouseX;
        pressY = mouseY;
        pressedToggle = toggleAt(mouseX, mouseY);
        pressedLamp = lampAt(mouseX, mouseY);
    }

    @Override
    protected void leftReleased(int mouseX, int mouseY) {
        if (!pressed) return;
        boolean click = Math.abs(mouseX - pressX) <= px(CLICK_SLOP)
                && Math.abs(mouseY - pressY) <= px(CLICK_SLOP);
        int releasedLamp = lampAt(mouseX, mouseY);
        int lamp = pressedLamp;
        boolean toggle = pressedToggle;
        pressed = false;
        pressedLamp = -1;
        pressedToggle = false;
        if (!click) return;
        if (toggle) {
            if (toggleAt(mouseX, mouseY)) toggleExpanded();
            return;
        }
        if (lamp >= 0 && lamp == releasedLamp) {
            if (lamp < entries.size() && controller != null)
                controller.cancelMonitoredAction(entries.get(lamp).getSequence());
            return;
        }
        toggleExpanded();
    }

    @Override
    protected void rightPressed(int mouseX, int mouseY, int clickCount) {
        // The monitor deliberately has no right-click menu.
    }

    @Override
    int getMouseCursor(int mouseX, int mouseY) {
        return MOUSE_CURSOR_HAND;
    }

    private int lampAt(int mouseX, int mouseY) {
        int lampLeft = lampColumnLeft();
        int lampRight = lampLeft + px(LAMP_SIZE);
        if (mouseX < lampLeft || mouseX >= lampRight) return -1;
        int relativeY = mouseY - y - px(PADDING) - toggleHeight;
        if (relativeY < 0) return -1;
        int index = relativeY / rowHeight;
        if (index < 0 || index >= slots) return -1;
        int lampTop = index * rowHeight + (rowHeight - px(LAMP_SIZE)) / 2;
        return relativeY >= lampTop && relativeY < lampTop + px(LAMP_SIZE)
                ? index : -1;
    }

    private boolean toggleAt(int mouseX, int mouseY) {
        return KeybinderActionQueueMonitorHitBox.contains(mouseX, mouseY,
                lampColumnLeft(), y + px(PADDING), px(LAMP_SIZE), toggleHeight);
    }

    private int lampColumnLeft() {
        return KeybinderActionQueueMonitorHitBox.lampColumnLeft(
                side == QueueMonitorSide.LEFT, x, width, px(LAMP_LEFT), px(LAMP_SIZE));
    }

    private void toggleExpanded() {
        expanded = !expanded;
        beginAnimation(expanded ? expandedWidth() : px(COLLAPSED_WIDTH));
    }

    private void beginAnimation(int desiredWidth) {
        animationFrom = width;
        targetWidth = desiredWidth;
        animationStarted = System.currentTimeMillis();
    }

    private int animatedWidth(long now) {
        if (width == targetWidth || animationStarted <= 0L) return targetWidth;
        float progress = Math.min(1.0f,
                Math.max(0.0f, (now - animationStarted) / (float) ANIMATION_MILLIS));
        float eased = progress * progress * (3.0f - 2.0f * progress);
        return Math.round(animationFrom + (targetWidth - animationFrom) * eased);
    }

    private int expandedWidth() {
        int longest = titleFont.getWidth(Messages.text("queue.monitor.title"));
        for (ActionQueueEntry entry : entries) {
            longest = Math.max(longest, actionFont.getWidth(actionText(entry)));
            longest = Math.max(longest, detailFont.getWidth(detailText(entry)));
        }
        int contentWidth = Math.max(px(MIN_EXPANDED_WIDTH),
                Math.min(px(MAX_EXPANDED_WIDTH), px(TEXT_LEFT) + longest + px(TEXT_RIGHT_PADDING)));
        int screenWidth = hud == null ? px(MAX_EXPANDED_WIDTH)
                : Math.max(px(COLLAPSED_WIDTH), hud.getWidth() - px(8));
        return Math.min(contentWidth, screenWidth);
    }

    private static String actionText(ActionQueueEntry entry) {
        if (entry == null) return "";
        return entry.getAction().isEmpty()
                ? Messages.text("editor.action") : entry.getAction();
    }

    private static String detailText(ActionQueueEntry entry) {
        if (entry == null) return "";
        String source = entry.getSource().isEmpty()
                ? Messages.text("list.unknown") : entry.getSource();
        String target = entry.getTarget().isEmpty()
                ? Messages.text("list.unknown") : entry.getTarget();
        return Messages.text("queue.monitor.details",
                Messages.text("editor.tool"), source,
                Messages.text("editor.target"), target);
    }

    private int panelHeight(int slots) {
        return px(PADDING) * 2 + toggleHeight + Math.max(1, slots) * rowHeight;
    }
}
