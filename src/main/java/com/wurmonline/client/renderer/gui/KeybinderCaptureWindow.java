package com.wurmonline.client.renderer.gui;

import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.ui.KeybindEditorController;
import org.keybinder.wurm.KeybinderMod;
import org.keybinder.wurm.i18n.Messages;

public final class KeybinderCaptureWindow extends WWindow {
    private final KeybindEditorController controller;
    private final KeybinderEditorWindow editor;

    public KeybinderCaptureWindow(KeybindEditorController controller, KeybinderEditorWindow editor) {
        super("keybinder.capture", false);
        this.controller = controller;
        this.editor = editor;
        setTitle(Messages.text("capture.title"));
        WurmLabel label = new WurmLabel(Messages.text("capture.waiting"));
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
