package com.wurmonline.client.renderer.gui;

import org.keybinder.wurm.KeybinderMod;
import org.keybinder.wurm.ui.KeybindEditorController;
import org.keybinder.wurm.i18n.Messages;

public final class KeybinderSelectionWindow extends WWindow {
    private final KeybindEditorController controller;

    public KeybinderSelectionWindow(KeybindEditorController controller, String message) {
        super("keybinder.slot.selection", false);
        this.controller = controller;
        setTitle(Messages.text("selection.title"));
        WurmArrayPanel<FlexComponent> lines =
                new WurmArrayPanel<>("keybinder.selection.message", WurmArrayPanel.DIR_VERTICAL, true);
        int widest = 0;
        int count = 0;
        for (String line : wrap(message, 72)) {
            WurmLabel label = new WurmLabel(line);
            widest = Math.max(widest, label.width);
            lines.addComponent(label);
            count++;
        }
        setComponent(lines);
        setInitialSize(Math.max(300, widest + 42), Math.max(90, 54 + count * 18), false);
    }

    @Override
    protected void closePressed() {
        controller.cancelTargetSelection();
        KeybinderMod.deferUi(() -> hud.hideComponent(this));
    }

    private static java.util.List<String> wrap(String message, int limit) {
        java.util.List<String> result = new java.util.ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : message.split("\\s+")) {
            if (line.length() > 0 && line.length() + word.length() + 1 > limit) {
                result.add(line.toString());
                line.setLength(0);
            }
            if (line.length() > 0) line.append(' ');
            line.append(word);
        }
        if (line.length() > 0) result.add(line.toString());
        if (result.isEmpty()) result.add("");
        return result;
    }
}
