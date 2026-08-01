package com.wurmonline.client.renderer.gui;

import org.keybinder.wurm.KeybinderMod;
import org.keybinder.wurm.i18n.Messages;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.ui.KeybinderUiController;

/** Explicit confirmation for the destructive half of a drag-to-merge operation. */
public final class KeybinderMergeWindow extends WWindow implements ButtonListener {
    private final KeybinderUiController controller;
    private final String sourceId;
    private final String destinationId;
    private final WButton merge;
    private final WButton cancel;

    public KeybinderMergeWindow(KeybinderUiController controller, KeybindRecord source,
                                KeybindRecord destination) {
        super("keybinder.merge", false);
        this.controller = controller;
        sourceId = source.getId();
        destinationId = destination.getId();
        setTitle(Messages.text("merge.title"));
        WurmArrayPanel<FlexComponent> root = new WurmArrayPanel<FlexComponent>(
                "keybinder.merge.root", WurmArrayPanel.DIR_VERTICAL, true);
        root.componentWidthOffset = 3;
        root.addComponent(new WurmLabel(Messages.text("merge.source", source.getName())));
        root.addComponent(new WurmLabel(Messages.text("merge.destination", destination.getName())));
        root.addComponent(new WurmLabel(Messages.text("merge.calculation",
                destination.getVariants().size(), source.getVariants().size(),
                destination.getVariants().size() + source.getVariants().size())));
        root.addComponent(new WurmLabel(Messages.text("merge.effects",
                destination.getKey(), Messages.text(destination.isHudMulti()
                        ? "multi.mode.hud" : "multi.mode.ordinary"))));
        WurmArrayPanel<FlexComponent> buttons = new WurmArrayPanel<FlexComponent>(
                "keybinder.merge.buttons", WurmArrayPanel.DIR_HORIZONTAL);
        buttons.componentWidthOffset = 8;
        merge = new WButton(Messages.text("merge.confirm"), this);
        cancel = new WButton(Messages.text("common.cancel"), this);
        merge.setHoverString(Messages.text("merge.confirm.tip"));
        cancel.setHoverString(Messages.text("merge.cancel.tip"));
        buttons.addComponent(merge);
        buttons.addComponent(cancel);
        root.addComponent(buttons);
        setComponent(root);
        setInitialSize(Math.max(450, root.width + 28), 170, false);
    }

    @Override public void buttonPressed(WButton button) {}

    @Override public void buttonClicked(WButton button) {
        if (button == merge) controller.confirmMerge(sourceId, destinationId);
        else if (button == cancel) controller.cancelMerge();
    }

    @Override protected void closePressed() { controller.cancelMerge(); }

    public void closeWindow() {
        KeybinderMod.deferUi(new Runnable() {
            @Override public void run() { hud.hideComponent(KeybinderMergeWindow.this); }
        });
    }
}
