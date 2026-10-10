package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.backend.Queue;
import org.chamomilo.wurm.ui.v1.UiAxis;

/** Native clipping with one shared-kit scrollbar and one authoritative offset. */
final class KeybinderUiScrollPanel extends WurmScrollPanel {
    private final ChamomiloUiV1ScrollBar vertical;
    private boolean synchronizing;
    private boolean barVisible;

    KeybinderUiScrollPanel(String id, FlexComponent content) {
        super(id, content, false, true);
        vertical = new ChamomiloUiV1ScrollBar(id + ".vertical", UiAxis.VERTICAL, 180,
                value -> {
                    if (synchronizing) return;
                    yo = value;
                    isAtBottom = value >= Math.max(0, content.height - ((FlexComponent) offs).height);
                    ((ContainerComponent) offs).layout();
                    synchronize();
                });
        // Replace the native bar; never keep two visible controls for one offset.
        setComponent(null, EAST);
        layout();
    }

    ChamomiloUiV1ScrollBar verticalBar() { return vertical; }
    boolean isBarVisible() { return barVisible; }

    @Override void performLayout() {
        if (vertical != null) {
            boolean needed = content.height > Math.max(0, height);
            if (needed != barVisible) {
                barVisible = needed;
                setComponent(needed ? vertical : null, EAST);
            }
        }
        super.performLayout();
        if (vertical != null) synchronize();
    }

    private void synchronize() {
        if (synchronizing) return;
        synchronizing = true;
        try {
            int previous = yo;
            vertical.setRange(Math.max(0, content.height), Math.max(0, ((FlexComponent) offs).height));
            vertical.setValue(yo);
            yo = vertical.value();
            isAtBottom = yo >= vertical.maximum();
            if (previous != yo) ((ContainerComponent) offs).layout();
        } finally { synchronizing = false; }
    }

    @Override protected void mouseWheeled(int mx, int my, int clicks) {
        synchronize();
        vertical.mouseWheeled(mx, my, clicks);
    }

    @Override void scrollDownTo(int position) { synchronize(); vertical.setValue(position); }
    @Override void scrollDownToBottom() { synchronize(); vertical.setValue(vertical.maximum()); }

    @Override public void gameTick() {
        super.gameTick();
        if (barVisible != (content.height > Math.max(0, height))) layout();
        synchronize();
    }

    @Override protected void renderComponent(Queue queue, float ignoredAlpha) {
        synchronize();
        super.renderComponent(queue, 1f);
    }
}
