package com.wurmonline.client.renderer.gui;

import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import org.keybinder.wurm.integration.TransferFileBrowser;
import org.keybinder.wurm.i18n.Messages;

/** All file-picker controls and overwrite confirmation use Chamomilo UI. */
final class KeybinderFileWindow extends KeybinderUiWindow implements ButtonListener, KeybinderUiInputListener {
    private final TransferFileBrowser browser;
    private final boolean export;
    private final Consumer<Path> selected;
    private final KeybinderUiInputField path;
    private final KeybinderUiLabel directory, status;
    private final WurmArrayPanel<FlexComponent> listing;
    private final WButton accept, cancel, parent, refresh;
    private final Map<WButton, Path> paths = new LinkedHashMap<>();
    private boolean resolved;

    KeybinderFileWindow(TransferFileBrowser browser, boolean export, Consumer<Path> selected) {
        super("keybinder.files", true);
        this.browser = browser; this.export = export; this.selected = selected;
        setLocalizedTitle(Messages.text(export ? "transfer.export.title" : "transfer.import.title"));
        WurmBorderPanel root = new WurmBorderPanel("keybinder.files.root");
        WurmArrayPanel<FlexComponent> top = new KeybinderUiArrayPanel<>("keybinder.files.top", WurmArrayPanel.DIR_VERTICAL, true);
        directory = new KeybinderUiLabel(""); top.addComponent(directory);
        path = new KeybinderUiInputField("keybinder.files.path", this);
        path.setText(export ? "keybinder-export.keybinder" : ""); top.addComponent(path);
        WurmArrayPanel<FlexComponent> navigation = new KeybinderUiArrayPanel<>("keybinder.files.navigation", WurmArrayPanel.DIR_HORIZONTAL);
        navigation.componentWidthOffset = 8;
        parent = new KeybinderUiButton(Messages.text("transfer.parent"), this);
        refresh = new KeybinderUiButton(Messages.text("transfer.refresh"), this);
        navigation.addComponent(parent); navigation.addComponent(refresh); top.addComponent(navigation);
        root.setComponent(top, WurmBorderPanel.NORTH);
        listing = new KeybinderUiArrayPanel<>("keybinder.files.entries", WurmArrayPanel.DIR_VERTICAL);
        root.setComponent(new KeybinderUiScrollPanel("keybinder.files.scroll", listing), WurmBorderPanel.CENTER);
        WurmArrayPanel<FlexComponent> bottom = new KeybinderUiArrayPanel<>("keybinder.files.bottom", WurmArrayPanel.DIR_VERTICAL, true);
        status = new KeybinderUiLabel(""); bottom.addComponent(status);
        WurmArrayPanel<FlexComponent> footer = new KeybinderUiArrayPanel<>("keybinder.files.footer", WurmArrayPanel.DIR_HORIZONTAL);
        footer.componentWidthOffset = 8;
        accept = new KeybinderUiButton(Messages.text(export ? "common.save" : "common.select"), this);
        cancel = new KeybinderUiButton(Messages.text("common.cancel"), this);
        footer.addComponent(accept); footer.addComponent(cancel); bottom.addComponent(footer);
        root.setComponent(bottom, WurmBorderPanel.SOUTH);
        reload(); setComponent(root); setInitialSize(720, 470, false);
    }
    private void reload() {
        listing.removeAllComponents(); paths.clear();
        directory.setLabel(browser.directory().toString());
        try {
            for (Path entry : browser.entries()) {
                WButton row = new KeybinderUiButton((Files.isDirectory(entry) ? "[+] " : "") + entry.getFileName(), this);
                row.setHoverString(entry.toString()); paths.put(row, entry); listing.addComponent(row);
            }
            status.setLabel(Messages.text("transfer.filter"));
        } catch (Exception failure) { status.setLabel(failure.getMessage()); }
        parent.setEnabled(browser.directory().getParent() != null);
        listing.componentResized(); KeybinderUi.theme(this);
    }
    private void enter(Path directory) {
        try { browser.enter(directory); reload(); }
        catch (Exception failure) { status.setLabel(failure.getMessage()); }
    }
    private void accept() {
        if (resolved) return;
        try {
            Path chosen = browser.resolve(path.getText(), export);
            if (Files.isDirectory(chosen)) { enter(chosen); return; }
            if (export && Files.exists(chosen))
                new KeybinderUiConfirmWindow(Messages.text("transfer.overwrite.title"),
                        Messages.text("transfer.overwrite", chosen.toString()), () -> complete(chosen)).show(hud);
            else complete(chosen);
        } catch (Exception failure) { path.setInvalid(true); status.setLabel(failure.getMessage()); }
    }
    private void complete(Path chosen) { if (resolved) return; resolved = true; dismiss(); selected.accept(chosen); }
    @Override public void buttonPressed(WButton button) {}
    @Override public void buttonClicked(WButton button) {
        if (button == cancel) closePressed();
        else if (button == accept) accept();
        else if (button == parent && browser.directory().getParent() != null) enter(browser.directory().getParent());
        else if (button == refresh) reload();
        else if (paths.containsKey(button)) {
            Path entry = paths.get(button);
            if (Files.isDirectory(entry)) enter(entry);
            else path.setText(entry.toString());
        }
    }
    @Override public void handleInput(String input) { accept(); }
    @Override public void handleInputChanged(KeybinderUiInputField field, String input) { if (path != null) path.setInvalid(false); }
    @Override public void handleEscape(KeybinderUiInputField field) { closePressed(); }
    private void dismiss() { hide(); if (hud != null) hud.removeComponent(this); }
    @Override protected void closePressed() { resolved = true; dismiss(); }
}
