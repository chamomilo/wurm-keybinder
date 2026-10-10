package org.keybinder.wurm.integration;

import com.wurmonline.client.renderer.gui.KeybinderFileChooserBridge;
import java.nio.file.Path;
import java.util.function.Consumer;

/** Opens the shared in-game theme on the HUD thread, without an AWT dialog. */
public final class TransferFileChooser {
    private final Path directory;
    public TransferFileChooser(Path directory) { this.directory = directory; }
    public void chooseImport(Consumer<Path> selected) { chooseImport(selected, null); }
    public void chooseExport(Consumer<Path> selected) { chooseExport(selected, null); }
    public void chooseImport(Consumer<Path> selected, Consumer<Throwable> failed) {
        KeybinderFileChooserBridge.open(directory, false, selected, failed);
    }
    public void chooseExport(Consumer<Path> selected, Consumer<Throwable> failed) {
        KeybinderFileChooserBridge.open(directory, true, selected, failed);
    }
}
