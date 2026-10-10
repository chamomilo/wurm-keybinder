package com.wurmonline.client.renderer.gui;

import java.nio.file.Path;
import java.util.function.Consumer;
import org.keybinder.wurm.KeybinderMod;
import org.keybinder.wurm.integration.TransferFileBrowser;

/** Public package-access adapter; the picker is created only after an explicit import/export click. */
public final class KeybinderFileChooserBridge {
    private KeybinderFileChooserBridge() {}
    public static void open(Path directory, boolean export, Consumer<Path> selected, Consumer<Throwable> failed) {
        KeybinderMod.deferUi(() -> {
            try {
                new KeybinderFileWindow(new TransferFileBrowser(directory), export, selected).show(WurmComponent.hud);
            } catch (Throwable failure) { if (failed != null) failed.accept(failure); }
        });
    }
}
