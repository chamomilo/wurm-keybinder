package org.keybinder.wurm.transfer;

import org.keybinder.wurm.KeybindRegistry;
import org.keybinder.wurm.event.EventLogger;
import org.keybinder.wurm.i18n.Messages;
import org.keybinder.wurm.integration.TransferFileChooser;
import org.keybinder.wurm.model.KeybindRecord;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/** Coordinates chooser callbacks and transfer persistence outside the mod entry class. */
public final class KeybindTransferWorkflow {
    public interface Environment {
        void defer(Runnable task);
        void refreshCreationContext();
        String currentUser();
        String currentServer();
        void refreshWindow();
    }

    private final KeybindRegistry registry;
    private final KeybindTransferStore transfer;
    private final TransferFileChooser chooser;
    private final EventLogger log;
    private final Environment environment;
    private final String version;

    public KeybindTransferWorkflow(KeybindRegistry registry, KeybindTransferStore transfer,
                                   TransferFileChooser chooser, EventLogger log,
                                   Environment environment, String version) {
        this.registry = registry;
        this.transfer = transfer;
        this.chooser = chooser;
        this.log = log;
        this.environment = environment;
        this.version = version;
    }

    public void requestImport() {
        chooser.chooseImport(new Consumer<Path>() {
            @Override public void accept(final Path path) {
                environment.defer(new Runnable() {
                    @Override public void run() { importFrom(path); }
                });
            }
        }, chooserFailure());
    }

    public void requestExport() {
        chooser.chooseExport(new Consumer<Path>() {
            @Override public void accept(final Path path) {
                environment.defer(new Runnable() {
                    @Override public void run() { exportTo(path); }
                });
            }
        }, chooserFailure());
    }

    void importFrom(Path path) {
        try {
            environment.refreshCreationContext();
            List<PortableKeybindDefinition> definitions = transfer.read(path);
            TransferImportResult result = registry.importPortable(definitions);
            log.info(Messages.text("event.transfer_import", result.getImported(),
                    result.getSkippedDuplicates(), result.getRejected(), path.toAbsolutePath()));
            environment.refreshWindow();
        } catch (Exception failure) {
            log.warning(Messages.text("event.transfer_import",
                    0, 0, 1, path.toAbsolutePath()));
            log.error(Messages.text("error.transfer_import", path), failure);
        }
    }

    void exportTo(Path path) {
        try {
            List<KeybindRecord> records = registry.snapshot();
            transfer.write(path, records, environment.currentUser(),
                    environment.currentServer(), version);
            log.info(Messages.text("event.transfer_export",
                    records.size(), path.toAbsolutePath()));
        } catch (Exception failure) {
            log.error(Messages.text("error.transfer_export", path), failure);
        }
    }

    private Consumer<Throwable> chooserFailure() {
        return new Consumer<Throwable>() {
            @Override public void accept(final Throwable failure) {
                environment.defer(new Runnable() {
                    @Override public void run() {
                        log.error(Messages.text("error.transfer_chooser"), failure);
                    }
                });
            }
        };
    }
}
