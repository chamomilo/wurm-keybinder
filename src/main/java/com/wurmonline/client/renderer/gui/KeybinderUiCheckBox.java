package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.PickData;
import org.chamomilo.wurm.ui.v1.*;

/** Checkbox is not an SDK wrapper yet; reuse its painter, glyphs and cached fonts. */
final class KeybinderUiCheckBox extends FlexComponent {
    boolean checked, enabled = true;
    private String label, tip;
    private boolean armed, hovered;
    private final ChamomiloUiV1Canvas canvas = new ChamomiloUiV1Canvas(this);
    private final UiButtonMotion motion = new UiButtonMotion();
    KeybinderUiCheckBox(String label) {
        super("keybinder.checkbox"); this.label = label;
        KeybinderUi.fonts(this);
        setSize(26 + text.getWidth(label), 32); sizeFlags = FIXED_HEIGHT;
    }
    public void setHoverString(String value) { tip = value; }
    @Override protected void leftPressed(int mx, int my, int count) {
        armed = enabled; if (armed) motion.pointerPressed(System.nanoTime());
    }
    @Override protected void mouseDragged(int mx, int my) { if (!contains(mx, my)) armed = false; }
    @Override protected void leftReleased(int mx, int my) {
        if (armed && enabled && contains(mx, my)) checked = !checked;
        armed = false;
    }
    @Override protected void mouseMoved(int mx, int my) { hovered = contains(mx, my); }
    @Override protected void mouseExited() { hovered = false; }
    @Override int getMouseCursor(int mx, int my) { return enabled ? MOUSE_CURSOR_HAND : MOUSE_CURSOR_NORMAL; }
    @Override public void pick(PickData data, int mx, int my) {
        if (tip != null && contains(mx, my)) data.addText(tip);
    }
    @Override protected void renderComponent(Queue queue, float ignoredAlpha) {
        canvas.begin(queue); motion.update(enabled, hovered, false, System.nanoTime());
        int top = y + (height - 22) / 2;
        UiPainter.button(canvas, motion.brightness(), motion.depth(), motion.hover(), UiScale.BASE, 1f, x, top, 22, 22);
        if (checked) UiIcon.CHECK.paint(canvas, enabled ? UiColor.TEXT : UiColor.MUTED, 1f, x + 3, top + 3, 16);
        text.moveTo(x + 28, y + (height - text.getHeight()) / 2 + text.getAscent());
        UiColor color = enabled ? UiColor.TEXT : UiColor.MUTED;
        text.paint(queue, label, color.red, color.green, color.blue, 1f);
    }
}
