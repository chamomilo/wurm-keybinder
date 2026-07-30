package com.wurmonline.client.renderer.gui;

import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.ui.KeybindEditorController;
import org.keybinder.wurm.KeybinderMod;

public final class KeybinderCaptureWindow extends WWindow {
    private final KeybindEditorController controller;
    private final KeybinderEditorWindow editor;

    public KeybinderCaptureWindow(KeybindEditorController controller, KeybinderEditorWindow editor) {
        super("keybinder.capture", false);
        this.controller = controller;
        this.editor = editor;
        setTitle("Capture action");
        WurmLabel label = new WurmLabel(
                "Waiting for action. Perform required action via menu. Keybinder will remember it.");
        setComponent(label);
        setInitialSize(Math.max(510, label.width + 24), 90, false);
    }

    @Override
    public void gameTick() {
        super.gameTick();
        ActionStep captured = controller.pollCapturedAction();
        if (captured != null) {
            editor.addCapturedAction(captured);
            closePressed();
        }
    }

    @Override
    protected void closePressed() {
        controller.cancelCapture();
        KeybinderMod.deferUi(() -> hud.hideComponent(this));
    }
}
