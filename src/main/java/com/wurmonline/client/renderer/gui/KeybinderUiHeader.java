package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.backend.Queue;
import org.chamomilo.wurm.ui.v1.*;

/** One outer contour for the entire column-heading band, without internal divider rails. */
final class KeybinderUiHeader extends KeybinderUiArrayPanel<FlexComponent> {
    private final ChamomiloUiV1Canvas canvas = new ChamomiloUiV1Canvas(this);
    KeybinderUiHeader(String id) { super(id, DIR_HORIZONTAL); }
    @Override protected void renderComponent(Queue queue, float ignoredAlpha) {
        UiPainter.background(canvas.begin(queue), UiBackground.LEATHER, 1f, x, y, width, height);
        super.renderComponent(queue, 1f);
        UiPainter.frame(canvas, 3, 1f, x, y, width, height);
    }
    @Override void componentResized() {
        super.componentResized();
        for (FlexComponent child : components) {
            child.text = KeybinderUi.heading(); child.textBold = child.text;
            child.setPosition(child.x, y + Math.max(0, (height - child.height) / 2));
        }
    }
}
