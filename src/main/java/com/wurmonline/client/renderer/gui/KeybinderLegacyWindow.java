package com.wurmonline.client.renderer.gui;

import org.keybinder.wurm.KeybinderMod;
import org.keybinder.wurm.ui.KeybinderUiController;

public final class KeybinderLegacyWindow extends WWindow implements ButtonListener {
    private final KeybinderUiController controller;
    private final WButton review;
    private final WButton disable;
    private final WButton importReviewed;
    private final WButton later;

    public KeybinderLegacyWindow(KeybinderUiController controller) {
        super("keybinder.legacy", false);
        this.controller = controller;
        setTitle("Custom Actions migration");
        WurmArrayPanel<FlexComponent> root = new WurmArrayPanel<>("keybinder.legacy.root", WurmArrayPanel.DIR_VERTICAL);
        root.addComponent(new WurmLabel("The old Custom Actions mod is installed."));
        root.addComponent(new WurmLabel("Keybinder already contains its functionality."));
        root.addComponent(new WurmLabel("Review/import custom binds, then disable the old mod and restart."));
        review = new WButton("Review keybinds for import", this);
        importReviewed = new WButton("Import reviewed keybinds", this);
        disable = new WButton("Disable old Custom Actions on next restart", this);
        later = new WButton("Later", this);
        root.addComponent(review);
        root.addComponent(importReviewed);
        root.addComponent(disable);
        root.addComponent(later);
        setComponent(root);
        setInitialSize(480, 220, false);
    }

    @Override public void buttonPressed(WButton button) {}
    @Override public void buttonClicked(WButton button) {
        if (button == review) controller.requestImport();
        else if (button == importReviewed) controller.confirmImport();
        else if (button == disable) {
            controller.disableLegacyAction();
            KeybinderMod.deferUi(() -> hud.hideComponent(this));
        } else if (button == later) KeybinderMod.deferUi(() -> hud.hideComponent(this));
    }
    @Override protected void closePressed() { KeybinderMod.deferUi(() -> hud.hideComponent(this)); }
}
