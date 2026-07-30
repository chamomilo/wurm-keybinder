package com.wurmonline.client.renderer.gui;

import org.keybinder.wurm.KeybinderMod;
import org.keybinder.wurm.model.KeybindConflict;
import org.keybinder.wurm.model.ConflictResolution;
import org.keybinder.wurm.ui.KeybindEditorController;
import org.keybinder.wurm.i18n.Messages;

public final class KeybinderConflictWindow extends WWindow implements ButtonListener {
    private final KeybindEditorController controller;
    private final WButton keepNew;
    private final WButton keepOld;
    private final WButton cancel;

    public KeybinderConflictWindow(KeybindEditorController controller, KeybindConflict conflict) {
        super("keybinder.conflict", false);
        this.controller = controller;
        setTitle(Messages.text("conflict.title", conflict.getKey()));

        WurmArrayPanel<FlexComponent> root =
                new WurmArrayPanel<>("keybinder.conflict.root", WurmArrayPanel.DIR_VERTICAL, true);
        root.componentWidthOffset = 2;
        root.addComponent(new WurmLabel(
                Messages.text("conflict.used", conflict.getKey(),
                        conflict.isVanillaOwner()
                                ? Messages.text("conflict.vanilla_owner") : conflict.getOwner())));
        root.addComponent(new WurmLabel(Messages.text("conflict.created_user",
                displayOrigin(conflict.getCreatedByUser(), Messages.text("list.unknown_user")))));
        root.addComponent(new WurmLabel(Messages.text("conflict.created_server",
                displayOrigin(conflict.getCreatedOnServer(), Messages.text("list.unknown_server")))));

        WurmArrayPanel<FlexComponent> buttons =
                new WurmArrayPanel<>("keybinder.conflict.buttons", WurmArrayPanel.DIR_HORIZONTAL);
        buttons.componentWidthOffset = 8;
        keepNew = new WButton(Messages.text("conflict.keep_new"), this);
        keepOld = new WButton(Messages.text("conflict.keep_old"), this);
        cancel = new WButton(Messages.text("common.cancel"), this);
        keepNew.setHoverString(Messages.text("conflict.keep_new.tip"));
        keepOld.setHoverString(Messages.text("conflict.keep_old.tip"));
        cancel.setHoverString(Messages.text("conflict.cancel.tip"));
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
