package org.keybinder.wurm.bind;

import com.wurmonline.client.console.WurmConsole;
import org.keybinder.wurm.catalog.InputKeyCatalog;
import org.keybinder.wurm.event.EventLogger;
import org.keybinder.wurm.i18n.DisableReason;
import org.keybinder.wurm.i18n.Messages;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.storage.KeybindStore;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/** Reconciles externally edited definitions with account-local live bindings. */
public final class ExternalBindingSynchronizer {
    public static final class Result {
        private final List<KeybindRecord> records;
        private final long observedModified;

        private Result(List<KeybindRecord> records, long observedModified) {
            this.records = records;
            this.observedModified = observedModified;
        }

        public boolean isChanged() { return records != null; }
        public List<KeybindRecord> getRecords() { return records; }
        public long getObservedModified() { return observedModified; }
    }

    private final KeybindStore store;
    private final ManagedBindAccess binds;
    private final EventLogger log;

    public ExternalBindingSynchronizer(KeybindStore store, ManagedBindAccess binds,
                                       EventLogger log) {
        this.store = store;
        this.binds = binds;
        this.log = log;
    }

    public Result synchronize(List<KeybindRecord> localRecords, long previousModified,
                              WurmConsole console,
                              Function<KeybindRecord, String> commandFor) {
        long modified = store.lastModifiedMillis();
        if (modified == 0L || modified == previousModified)
            return new Result(null, previousModified);
        try {
            List<KeybindRecord> incoming = store.load();
            Map<String, KeybindRecord> localById = new HashMap<String, KeybindRecord>();
            for (KeybindRecord local : localRecords) localById.put(local.getId(), local);
            Set<String> incomingIds = new HashSet<String>();
            for (KeybindRecord external : incoming) {
                incomingIds.add(external.getId());
                KeybindRecord local = localById.get(external.getId());
                if (local == null) {
                    external.setEnabled(false);
                    external.setDisabledReason(DisableReason.value("not_enabled_account"));
                    continue;
                }
                reconcileChangedDefinition(local, external, console, commandFor);
            }
            for (KeybindRecord local : localRecords) {
                if (incomingIds.contains(local.getId()) || !local.isEnabled()) continue;
                removeLive(console, local.getKey(), commandFor.apply(local));
            }
            log.info(Messages.text("registry.synchronized"));
            return new Result(incoming, modified);
        } catch (Exception failure) {
            log.error(Messages.text("registry.sync_failed"), failure);
            return new Result(null, modified);
        }
    }

    private void reconcileChangedDefinition(KeybindRecord local, KeybindRecord external,
                                            WurmConsole console,
                                            Function<KeybindRecord, String> commandFor)
            throws ReflectiveOperationException {
        String oldKey = local.getKey();
        String oldCommand = commandFor.apply(local);
        boolean wasEnabled = local.isEnabled();
        external.setEnabled(wasEnabled);
        external.setDisabledReason(local.getDisabledReason());
        String newCommand = commandFor.apply(external);
        if (!wasEnabled || (oldKey.equalsIgnoreCase(external.getKey())
                && oldCommand.equalsIgnoreCase(newCommand))) return;
        if (removeLive(console, oldKey, oldCommand))
            installLive(console, external.getKey(), newCommand);
        else {
            external.setEnabled(false);
            external.setDisabledReason(DisableReason.value("sync_changed"));
        }
    }

    private void installLive(WurmConsole console, String key, String command) {
        if (!InputKeyCatalog.isVirtual(key)) binds.install(console, key, command);
    }

    private boolean removeLive(WurmConsole console, String key, String command)
            throws ReflectiveOperationException {
        return InputKeyCatalog.isVirtual(key)
                || binds.removeIfOwned(console, key, command);
    }
}
