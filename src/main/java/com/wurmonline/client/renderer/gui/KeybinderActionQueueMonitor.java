package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.gui.text.TextFont;
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
    private static final int COLLAPSED_WIDTH = 24;
    private static final int MIN_EXPANDED_WIDTH = 220;
    private static final int MAX_EXPANDED_WIDTH = 520;
    private static final int ROW_HEIGHT = 22;
    private static final int TOGGLE_HEIGHT = 18;
    private static final int PADDING = 2;
    private static final int LAMP_SIZE = 20;
    private static final int LAMP_LEFT = 2;
    private static final int TEXT_LEFT = 26;
    private static final int TEXT_RIGHT_PADDING = 10;
    private static final int TEXT_LAMP_GAP = 4;
    private static final int ANIMATION_MILLIS = 180;
    private static final int CLICK_SLOP = 3;

    private final ActionQueueMonitorController controller;
    private final TextFont actionFont = TextFont.getFixedSizeText();
    private final TextFont detailFont = TextFont.getFixedSizeText();
    private final TextureButton[] lamps = new TextureButton[MAX_SLOTS];
    private List<ActionQueueEntry> entries = Collections.emptyList();
    private int slots = MAX_SLOTS;
    private QueueMonitorSide side = QueueMonitorSide.RIGHT;
    private boolean expanded;
    private int animationFrom = COLLAPSED_WIDTH;
    private int targetWidth = COLLAPSED_WIDTH;
    private long animationStarted;
    private int pressX;
    private int pressY;
    private int pressedLamp = -1;
    private boolean pressedToggle;
    private boolean pressed;

    public KeybinderActionQueueMonitor(ActionQueueMonitorController controller) {
        super("Keybinder action queue monitor");
        this.controller = controller;
        for (int index = 0; index < lamps.length; index++) {
            lamps[index] = new TextureButton("img.gui.crafting.que",
                    LAMP_SIZE, LAMP_SIZE, 0, 0, "", 0, 0,
                    0, 0, LAMP_SIZE, LAMP_SIZE * 2, false);
            lamps[index].loadTexture();
        }
        setSize(COLLAPSED_WIDTH, panelHeight(slots));
    }

    @Override
    public void gameTick() {
        int requestedSlots = controller == null ? MAX_SLOTS
                : controller.getQueueMonitorSlots();
        slots = Math.max(1, Math.min(MAX_SLOTS, requestedSlots));
        entries = controller == null ? Collections.<ActionQueueEntry>emptyList()
                : controller.getMonitoredActions();
        if (entries == null) entries = Collections.emptyList();
        QueueMonitorSide requestedSide = controller == null ? QueueMonitorSide.RIGHT
                : controller.getQueueMonitorSide();
        side = requestedSide == null ? QueueMonitorSide.RIGHT : requestedSide;

        int desired = expanded ? expandedWidth() : COLLAPSED_WIDTH;
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
        fillRect(queue, 0.015f, 0.012f, 0.009f, 1.0f, x, y, width, height);
        int borderX = side == QueueMonitorSide.LEFT ? x + width - 1 : x;
        fillRect(queue, 0.20f, 0.17f, 0.12f, 1.0f, borderX, y, 1, height);
        int backgroundX = side == QueueMonitorSide.LEFT ? x : x + 1;
        fillRect(queue, 0.10f, 0.08f, 0.05f, 1.0f,
                backgroundX, y + 1, Math.max(1, width - 1), Math.max(1, height - 2));
        paintToggleArrow(queue);
        if (width == targetWidth && expanded && width > COLLAPSED_WIDTH)
            paintTitle(queue);

        for (int index = 0; index < slots; index++) {
            ActionQueueEntry entry = index < entries.size() ? entries.get(index) : null;
            int rowTop = y + PADDING + TOGGLE_HEIGHT + index * ROW_HEIGHT;
            paintLamp(queue, index, lampColumnLeft(),
                    rowTop + Math.max(0, (ROW_HEIGHT - LAMP_SIZE) / 2), entry);
            if (width == targetWidth && expanded && width > COLLAPSED_WIDTH)
                paintEntry(queue, rowTop, entry);
        }
    }

    private void paintLamp(Queue queue, int index, int left, int top,
                           ActionQueueEntry entry) {
        TextureButton lamp = lamps[index];
        lamp.setRealPosition(left, top);
        lamp.setColour(1.0f, 1.0f, 1.0f);
        lamp.setColourAlpha(1.0f);
        lamp.setIsToggled(entry != null);
        lamp.render(queue, false);
    }

    private void paintToggleArrow(Queue queue) {
        int arrowLeft = lampColumnLeft() + (LAMP_SIZE - 5) / 2;
        int centerY = y + PADDING + TOGGLE_HEIGHT / 2;
        boolean pointsRight = KeybinderActionQueueMonitorHitBox.arrowPointsRight(
                side == QueueMonitorSide.LEFT, expanded);
        for (int column = 0; column < 5; column++) {
            int arrowX = pointsRight ? arrowLeft + 4 - column : arrowLeft + column;
            int halfHeight = column;
            fillRect(queue, 0.88f, 0.76f, 0.50f, 1.0f,
                    arrowX, centerY - halfHeight, 1, halfHeight * 2 + 1);
        }
    }

    private void paintTitle(Queue queue) {
        int textLeft;
        int textRight;
        if (side == QueueMonitorSide.LEFT) {
            textLeft = x + TEXT_RIGHT_PADDING;
            textRight = lampColumnLeft() - TEXT_LAMP_GAP;
        } else {
            textLeft = x + TEXT_LEFT;
            textRight = x + width - TEXT_RIGHT_PADDING;
        }
        int available = Math.max(1, textRight - textLeft);
        String title = fit(actionFont, Messages.text("queue.monitor.title"), available);
        int paintedLeft = KeybinderActionQueueMonitorHitBox.alignedTextLeft(
                side == QueueMonitorSide.LEFT, textLeft, textRight,
                actionFont.getWidth(title));
        actionFont.moveTo(paintedLeft, y + PADDING + 2 + actionFont.getAscent());
        actionFont.paint(queue, title, 0.88f, 0.76f, 0.50f, 1.0f);
    }

    private void paintEntry(Queue queue, int rowTop, ActionQueueEntry entry) {
        int textLeft;
        int textRight;
        int available;
        if (side == QueueMonitorSide.LEFT) {
            textLeft = x + TEXT_RIGHT_PADDING;
            textRight = lampColumnLeft() - TEXT_LAMP_GAP;
            available = Math.max(1, textRight - textLeft);
        } else {
            textLeft = x + TEXT_LEFT;
            available = Math.max(1, width - TEXT_LEFT - 8);
            textRight = textLeft + available;
        }
        String action = actionText(entry);
        String detail = detailText(entry);
        String fittedAction = fit(actionFont, action, available);
        String fittedDetail = fit(detailFont, detail, available);
        boolean mirrored = side == QueueMonitorSide.LEFT;

        actionFont.moveTo(KeybinderActionQueueMonitorHitBox.alignedTextLeft(
                mirrored, textLeft, textRight, actionFont.getWidth(fittedAction)),
                rowTop + 1 + actionFont.getAscent());
        actionFont.paint(queue, fittedAction,
                entry == null ? 0.34f : 0.95f,
                entry == null ? 0.31f : 0.88f,
                entry == null ? 0.25f : 0.68f, 1.0f);
        detailFont.moveTo(KeybinderActionQueueMonitorHitBox.alignedTextLeft(
                mirrored, textLeft, textRight, detailFont.getWidth(fittedDetail)),
                rowTop + 11 + detailFont.getAscent());
        detailFont.paint(queue, fittedDetail,
                0.68f, 0.66f, 0.60f, 1.0f);
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
        boolean click = Math.abs(mouseX - pressX) <= CLICK_SLOP
                && Math.abs(mouseY - pressY) <= CLICK_SLOP;
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
        int lampRight = lampLeft + LAMP_SIZE;
        if (mouseX < lampLeft || mouseX >= lampRight) return -1;
        int relativeY = mouseY - y - PADDING - TOGGLE_HEIGHT;
        if (relativeY < 0) return -1;
        int index = relativeY / ROW_HEIGHT;
        if (index < 0 || index >= slots) return -1;
        int lampTop = index * ROW_HEIGHT + (ROW_HEIGHT - LAMP_SIZE) / 2;
        return relativeY >= lampTop && relativeY < lampTop + LAMP_SIZE
                ? index : -1;
    }

    private boolean toggleAt(int mouseX, int mouseY) {
        return KeybinderActionQueueMonitorHitBox.contains(mouseX, mouseY,
                lampColumnLeft(), y + PADDING, LAMP_SIZE, TOGGLE_HEIGHT);
    }

    private int lampColumnLeft() {
        return KeybinderActionQueueMonitorHitBox.lampColumnLeft(
                side == QueueMonitorSide.LEFT, x, width, LAMP_LEFT, LAMP_SIZE);
    }

    private void toggleExpanded() {
        expanded = !expanded;
        beginAnimation(expanded ? expandedWidth() : COLLAPSED_WIDTH);
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
        int longest = 0;
        for (ActionQueueEntry entry : entries) {
            longest = Math.max(longest, actionFont.getWidth(actionText(entry)));
            longest = Math.max(longest, detailFont.getWidth(detailText(entry)));
        }
        int contentWidth = Math.max(MIN_EXPANDED_WIDTH,
                Math.min(MAX_EXPANDED_WIDTH, TEXT_LEFT + longest + TEXT_RIGHT_PADDING));
        int screenWidth = hud == null ? MAX_EXPANDED_WIDTH
                : Math.max(COLLAPSED_WIDTH, hud.getWidth() - 8);
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
                ? Messages.text("source.empty_hand") : entry.getSource();
        String target = entry.getTarget().isEmpty()
                ? Messages.text("queue.monitor.no_target") : entry.getTarget();
        return Messages.text("queue.monitor.details",
                Messages.text("editor.tool"), source,
                Messages.text("editor.target"), target);
    }

    private static int panelHeight(int slots) {
        return PADDING * 2 + TOGGLE_HEIGHT + Math.max(1, slots) * ROW_HEIGHT;
    }

    private static String fit(TextFont font, String value, int availableWidth) {
        if (value == null || value.isEmpty() || font.getWidth(value) <= availableWidth)
            return value == null ? "" : value;
        String ellipsis = "...";
        int end = value.length();
        while (end > 0 && font.getWidth(value.substring(0, end) + ellipsis) > availableWidth)
            end--;
        return end == 0 ? ellipsis : value.substring(0, end) + ellipsis;
    }
}
