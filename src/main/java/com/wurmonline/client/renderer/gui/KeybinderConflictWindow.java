package com.wurmonline.client.renderer.gui;

import org.keybinder.wurm.KeybinderMod;
import org.keybinder.wurm.model.KeybindConflict;
import org.keybinder.wurm.model.ConflictResolution;
import org.keybinder.wurm.ui.KeybindEditorController;

public final class KeybinderConflictWindow extends WWindow implements ButtonListener {
    private final KeybindEditorController controller;
    private final WButton keepNew;
    private final WButton keepOld;
    private final WButton cancel;

    public KeybinderConflictWindow(KeybindEditorController controller, KeybindConflict conflict) {
        super("keybinder.conflict", false);
        this.controller = controller;
        setTitle("Keybind conflict: " + conflict.getKey());

        WurmArrayPanel<FlexComponent> root =
                new WurmArrayPanel<>("keybinder.conflict.root", WurmArrayPanel.DIR_VERTICAL, true);
        root.componentWidthOffset = 2;
        root.addComponent(new WurmLabel(
                "Key " + conflict.getKey() + " is already used by: " + conflict.getOwner()));
        root.addComponent(new WurmLabel("Created by user: "
                + displayOrigin(conflict.getCreatedByUser(), "Unknown user")));
        root.addComponent(new WurmLabel("Created on server: "
                + displayOrigin(conflict.getCreatedOnServer(), "Unknown server")));

        WurmArrayPanel<FlexComponent> buttons =
                new WurmArrayPanel<>("keybinder.conflict.buttons", WurmArrayPanel.DIR_HORIZONTAL);
        buttons.componentWidthOffset = 8;
        keepNew = new WButton("Keep new", this);
        keepOld = new WButton("Keep old", this);
        cancel = new WButton("Cancel", this);
        keepNew.setHoverString("Save and enable the edited keybind; disable the existing keybind.");
        keepOld.setHoverString("Keep the existing keybind active; save the edited keybind disabled.");
        cancel.setHoverString("Cancel saving and return to the keybind constructor.");
        buttons.addComponent(keepNew);
        buttons.addComponent(keepOld);
        buttons.addComponent(cancel);
        root.addComponent(buttons);
        setComponent(root);
        setInitialSize(Math.max(350, root.width + 24), 145, false);
    }

    private static String displayOrigin(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    @Override public void buttonPressed(WButton button) {}

    @Override
    public void buttonClicked(WButton button) {
        if (button == keepNew) controller.resolveKeybindConflict(ConflictResolution.KEEP_NEW);
        else if (button == keepOld) controller.resolveKeybindConflict(ConflictResolution.KEEP_OLD);
        else if (button == cancel) controller.resolveKeybindConflict(ConflictResolution.CANCEL);
    }

    @Override
    protected void closePressed() {
        controller.resolveKeybindConflict(ConflictResolution.CANCEL);
    }

    public void closeWindow() {
        KeybinderMod.deferUi(() -> hud.hideComponent(this));
    }
}
