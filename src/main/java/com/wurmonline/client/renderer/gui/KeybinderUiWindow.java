package com.wurmonline.client.renderer.gui;

import org.chamomilo.wurm.ui.v1.UiBackground;
import com.wurmonline.client.renderer.backend.Queue;

/** Every Keybinder dialog shares the kit frame, chrome and branded typography. */
class KeybinderUiWindow extends ChamomiloUiV1Window {
    private boolean contentReady;
    private WButton maximize;

    KeybinderUiWindow(String id, boolean resizable) {
        super(id, "Keybinder", "en", UiBackground.WALNUT, 5);
        this.resizable = resizable;
        KeybinderUi.identify(this, id);
        contentReady = true;
        setHeaderHeight(40);
        setContentPadding(4);
        KeybinderUi.theme(this);
        maximize = KeybinderUi.maximizeControl(this);
    }

    @Override void setComponent(FlexComponent component) {
        if (!contentReady) { super.setComponent(component); return; }
        setContent(component);
        KeybinderUi.theme(this);
    }

    @Override void componentResized() {
        super.componentResized();
        if (contentReady) KeybinderUi.theme(this);
    }

    @Override public void toggleMaximized() {
        // These windows are registered by Keybinder's existing HUD/persistence integration.
        if (!resizable || hud == null) return;
        show(hud);
        super.toggleMaximized();
    }

    @Override public void setInitialSize(int width, int height, boolean relative) {
        // SDK geometry is expressed in pixels; the game's native font option must not rescale it.
        super.setInitialSize(width + (contentReady ? 24 : 0), height + (contentReady ? 32 : 0),
                false, false, .5f, .5f);
    }

    @Override protected void renderComponent(Queue queue, float ignoredAlpha) {
        if (maximize != null) maximize.setEnabled(resizable);
        super.renderComponent(queue, 1f);
    }
}
