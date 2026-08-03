package com.wurmonline.client.renderer.gui;

import org.keybinder.wurm.KeybinderMod;
import org.keybinder.wurm.ui.LegacyMigrationController;
import org.keybinder.wurm.i18n.Messages;

public final class KeybinderLegacyWindow extends WWindow implements ButtonListener {
    private final LegacyMigrationController controller;
    private final WButton review;
    private final WButton disable;
    private final WButton later;

    public KeybinderLegacyWindow(LegacyMigrationController controller) {
        super("keybinder.legacy", false);
        this.controller = controller;
        setTitle(Messages.text("migration.title"));
        WurmArrayPanel<FlexComponent> root = new WurmArrayPanel<>("keybinder.legacy.root", WurmArrayPanel.DIR_VERTICAL);
        root.addComponent(new WurmLabel(Messages.text("migration.installed")));
        root.addComponent(new WurmLabel(Messages.text("migration.replaced")));
        root.addComponent(new WurmLabel(Messages.text("migration.instructions")));
        review = new WButton(Messages.text("migration.review"), this);
        disable = new WButton(Messages.text("migration.disable"), this);
        later = new WButton(Messages.text("common.later"), this);
        root.addComponent(review);
        root.addComponent(disable);
        root.addComponent(later);
        setComponent(root);
        setInitialSize(Math.max(480, root.width + 24), 220, false);
    }

    @Override public void buttonPressed(WButton button) {}
    @Override public void buttonClicked(WButton button) {
        if (button == review) controller.requestImport();
        else if (button == disable) {
            controller.disableLegacyAction();
            KeybinderMod.deferUi(() -> hud.hideComponent(this));
        } else if (button == later) KeybinderMod.deferUi(() -> hud.hideComponent(this));
    }
    @Override protected void closePressed() { KeybinderMod.deferUi(() -> hud.hideComponent(this)); }
}
