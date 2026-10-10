package com.wurmonline.client.renderer.gui;

import org.keybinder.wurm.i18n.Messages;

/** Branded confirmation, including delete and restoration; never opens a native-font ConfirmWindow. */
final class KeybinderUiConfirmWindow extends KeybinderUiWindow implements ButtonListener {
    private final Runnable confirmed;
    private final WButton accept, cancel;
    private boolean resolved;
    KeybinderUiConfirmWindow(String question, String message, Runnable confirmed) {
        super("keybinder.confirm", false);
        this.confirmed = confirmed;
        setTitle(question);
        WurmArrayPanel<FlexComponent> content = new KeybinderUiArrayPanel<>("keybinder.confirm.content", WurmArrayPanel.DIR_VERTICAL, true);
        int widest = 400;
        for (String line : message.split("\n")) {
            KeybinderUiLabel label = new KeybinderUiLabel(line);
            widest = Math.max(widest, label.width);
            content.addComponent(label);
        }
        WurmArrayPanel<FlexComponent> footer = new KeybinderUiArrayPanel<>("keybinder.confirm.footer", WurmArrayPanel.DIR_HORIZONTAL);
        footer.componentWidthOffset = 8;
        accept = new KeybinderUiButton(Messages.text("common.confirm"), this);
        cancel = new KeybinderUiButton(Messages.text("common.cancel"), this);
        footer.addComponent(accept); footer.addComponent(cancel); content.addComponent(footer);
        setComponent(content);
        setInitialSize(widest + 30, content.calcHeight() + 65, false);
    }
    @Override public void buttonPressed(WButton button) {}
    @Override public void buttonClicked(WButton button) {
        if (resolved) return;
        resolved = true; dismiss();
        if (button == accept) confirmed.run();
    }
    private void dismiss() { hide(); if (hud != null) hud.removeComponent(this); }
    @Override protected void closePressed() { resolved = true; dismiss(); }
}
