package org.keybinder.wurm.integration;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Window;
import java.io.File;
import java.nio.file.Path;
import java.util.function.Consumer;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.keybinder.wurm.i18n.Messages;
import org.keybinder.wurm.transfer.KeybindTransferStore;
import org.lwjgl.opengl.Display;

/** Non-blocking bridge from the Wurm render thread to the AWT file chooser. */
public final class TransferFileChooser {
    private final File directory;

    public TransferFileChooser(Path directory) {
        this.directory = directory.toFile();
    }

    public void chooseImport(final Consumer<Path> selected) {
        chooseImport(selected, null);
    }

    public void chooseExport(final Consumer<Path> selected) {
        chooseExport(selected, null);
    }

    public void chooseImport(final Consumer<Path> selected,
                             final Consumer<Throwable> failed) {
        choose(false, selected, failed);
    }

    public void chooseExport(final Consumer<Path> selected,
                             final Consumer<Throwable> failed) {
        choose(true, selected, failed);
    }

    private void choose(final boolean export, final Consumer<Path> selected,
                        final Consumer<Throwable> failed) {
        EventQueue.invokeLater(new Runnable() {
            @Override public void run() {
                try {
                    if (!directory.exists() && !directory.mkdirs() && !directory.isDirectory())
                        throw new IllegalStateException("Unable to create " + directory);
                    final JFileChooser chooser = new JFileChooser(directory);
                    chooser.setAcceptAllFileFilterUsed(false);
                    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    chooser.setDialogTitle(Messages.text(export
                            ? "transfer.export.title" : "transfer.import.title"));
                    chooser.setFileFilter(new FileNameExtensionFilter(
                            Messages.text("transfer.filter"), "keybinder"));
                    if (export)
                        chooser.setSelectedFile(new File(directory,
                                "keybinder-export" + KeybindTransferStore.EXTENSION));
                    chooser.addHierarchyListener(event -> {
                        Window dialog = SwingUtilities.getWindowAncestor(chooser);
                        if (dialog == null) return;
                        try {
                            dialog.setAlwaysOnTop(true);
                            dialog.toFront();
                        } catch (RuntimeException ignored) {
                            // The chooser still works if the platform refuses
                            // the always-on-top hint.
                        }
                    });
                    Component owner;
                    try { owner = Display.getParent(); }
                    catch (RuntimeException unavailableParent) { owner = null; }
                    int answer = export
                            ? chooser.showSaveDialog(owner) : chooser.showOpenDialog(owner);
                    if (answer != JFileChooser.APPROVE_OPTION) return;
                    File chosen = chooser.getSelectedFile();
                    if (export && !chosen.getName().toLowerCase(java.util.Locale.ENGLISH)
                            .endsWith(KeybindTransferStore.EXTENSION))
                        chosen = new File(chosen.getParentFile(),
                                chosen.getName() + KeybindTransferStore.EXTENSION);
                    if (export && chosen.exists()) {
                        int overwrite = JOptionPane.showConfirmDialog(owner,
                                Messages.text("transfer.overwrite", chosen.getAbsolutePath()),
                                Messages.text("transfer.overwrite.title"),
                                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                        if (overwrite != JOptionPane.YES_OPTION) return;
                    }
                    selected.accept(chosen.toPath());
                } catch (Throwable failure) {
                    if (failed != null) failed.accept(failure);
                }
            }
        });
    }
}
