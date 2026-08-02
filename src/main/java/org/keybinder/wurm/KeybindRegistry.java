package org.keybinder.wurm;

import com.wurmonline.client.console.WurmConsole;
import org.keybinder.wurm.bind.BindSnapshot;
import org.keybinder.wurm.bind.DefaultBindCatalog;
import org.keybinder.wurm.bind.ManagedBindAccess;
import org.keybinder.wurm.bind.ManagedBindTransaction;
import org.keybinder.wurm.bind.VanillaImportPolicy;
import org.keybinder.wurm.event.EventLogger;
import org.keybinder.wurm.i18n.DisableReason;
import org.keybinder.wurm.i18n.Messages;
import org.keybinder.wurm.migration.CustomActionsImporter;
import org.keybinder.wurm.migration.ImprovedImproveImporter;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.model.KeybindConflict;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.ConsoleCommandStep;
import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.model.KeybindLimits;
import org.keybinder.wurm.model.KeybindNamePrefixes;
import org.keybinder.wurm.model.KeybindDefinitionService;
import org.keybinder.wurm.model.KeybindVariant;
import org.keybinder.wurm.queue.ActionQueueCostCalculator;
import org.keybinder.wurm.queue.QueueCost;
import org.keybinder.wurm.storage.KeybindStore;
import org.keybinder.wurm.storage.AccountKeybindStateStore;
import org.keybinder.wurm.ui.RowInsertionCalculator;
import org.keybinder.wurm.catalog.InputKeyCatalog;
import org.keybinder.wurm.transfer.PortableKeybindDefinition;
import org.keybinder.wurm.transfer.SemanticFingerprint;
import org.keybinder.wurm.transfer.TransferImportResult;
import org.keybinder.wurm.transfer.ValuePackTarget;
import org.keybinder.wurm.validation.KeybindValidator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class KeybindRegistry implements ValuePackTarget {
    private final List<KeybindRecord> records = new ArrayList<>();
    private final KeybindStore store;
    private final AccountKeybindStateStore accountStates;
    private final ManagedBindAccess binds;
    private final CustomActionsImporter customActionsImporter;
    private final ImprovedImproveImporter improvedImproveImporter;
    private final ActionQueueCostCalculator costs;
    private final EventLogger log;
    private final VanillaImportPolicy importPolicy = new VanillaImportPolicy();
    private final KeybindDefinitionService definitions = new KeybindDefinitionService();
    private String currentUser = "";
    private String currentServer = "";
    private String activeAccount = "";
    private long observedStoreModified;
    private boolean loadedSuccessfully;

    public KeybindRegistry(KeybindStore store, ManagedBindAccess binds,
                           CustomActionsImporter customActionsImporter,
                           ActionQueueCostCalculator costs, EventLogger log) {
        this(store, null, binds, customActionsImporter, costs, log);
    }

    public KeybindRegistry(KeybindStore store, AccountKeybindStateStore accountStates,
                           ManagedBindAccess binds,
                           CustomActionsImporter customActionsImporter,
                           ActionQueueCostCalculator costs, EventLogger log) {
        this.store = store;
        this.accountStates = accountStates;
        this.binds = binds;
        this.customActionsImporter = customActionsImporter;
        this.improvedImproveImporter = new ImprovedImproveImporter();
        this.costs = costs;
        this.log = log;
    }

    public synchronized void load() {
        loadedSuccessfully = false;
        try {
            List<KeybindRecord> loaded = store.load();
            records.clear();
            records.addAll(loaded);
            normalizeNames();
            observedStoreModified = store.lastModifiedMillis();
            loadedSuccessfully = true;
            if (store.wasRecoveredFromBackup())
                log.warning(Messages.text("registry.recovered", records.size()));
            log.info(Messages.text("registry.loaded", records.size()));
        } catch (Exception e) {
            log.error(Messages.text("registry.load_failed"), e);
        }
    }

    public synchronized List<KeybindRecord> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(records));
    }

    @Override public synchronized boolean isLoadedSuccessfully() {
        return loadedSuccessfully;
    }

    /**
     * Updates presentation metadata for managed steps after the server exposes
     * a custom action name. Action identity continues to be the numeric ID.
     */
    public synchronized void rememberActionName(short actionId, String name) {
        String clean = name == null ? "" : name.trim();
        if (clean.isEmpty()) return;
        boolean changed = false;
        for (KeybindRecord record : records) {
            for (org.keybinder.wurm.model.KeybindVariant variant : record.getVariants()) {
                for (KeybindStep step : variant.getSteps()) {
                    if (!(step instanceof ActionStep)) continue;
                    ActionStep action = (ActionStep) step;
                    if (action.getActionId() == actionId
                            && !clean.equals(action.getLastKnownName())) {
                        action.setLastKnownName(clean);
                        changed = true;
                    }
                }
            }
        }
        if (!changed) return;
        try {
            saveRecords();
        } catch (IOException e) {
            log.error("Unable to persist action name " + actionId, e);
        }
    }

    public synchronized boolean syncExternal(WurmConsole console) {
        long modified = store.lastModifiedMillis();
        if (modified == 0L || modified == observedStoreModified) return false;
        try {
            List<KeybindRecord> incoming = store.load();
            java.util.Map<String, KeybindRecord> localById = new java.util.HashMap<>();
            for (KeybindRecord local : records) localById.put(local.getId(), local);
            java.util.Set<String> incomingIds = new java.util.HashSet<>();
            for (KeybindRecord external : incoming) {
                incomingIds.add(external.getId());
                KeybindRecord local = localById.get(external.getId());
                if (local == null) {
                    external.setEnabled(false);
                    external.setDisabledReason(DisableReason.value("not_enabled_account"));
                    continue;
                }
                String oldKey = local.getKey();
                String oldCommand = commandFor(local);
                boolean wasEnabled = local.isEnabled();
                external.setEnabled(wasEnabled);
                external.setDisabledReason(local.getDisabledReason());
                if (wasEnabled && (!oldKey.equalsIgnoreCase(external.getKey())
                        || !oldCommand.equalsIgnoreCase(commandFor(external)))) {
                    if (removeLive(console, oldKey, oldCommand))
                        installLive(console, external.getKey(), commandFor(external));
                    else {
                        external.setEnabled(false);
                        external.setDisabledReason(DisableReason.value("sync_changed"));
                    }
                }
            }
            for (KeybindRecord local : records) {
                if (incomingIds.contains(local.getId()) || !local.isEnabled()) continue;
                removeLive(console, local.getKey(), commandFor(local));
            }
            records.clear();
            records.addAll(incoming);
            normalizeNames();
            observedStoreModified = modified;
            persistAccountState();
            log.info(Messages.text("registry.synchronized"));
            return true;
        } catch (Exception e) {
            observedStoreModified = modified;
            log.error(Messages.text("registry.sync_failed"), e);
            return false;
        }
    }

    public synchronized void setCreationContext(String user, String server) {
        currentUser = user == null ? "" : user.trim();
        currentServer = server == null ? "" : server.trim();
    }

    public synchronized String getCurrentUser() { return currentUser; }

    public synchronized String getCurrentServer() { return currentServer; }

    @Override public synchronized boolean wasValuePackProvided() {
        return store.wasValuePackProvided();
    }

    /**
     * Applies the account-local desired activation list after Wurm has loaded
     * that player's keybindings, then repairs missing owned dispatcher binds.
     */
    public synchronized void restoreAccountBindings(
            String account, WurmConsole console, int limit) {
        if (account == null || account.trim().isEmpty() || console == null) return;
        activeAccount = account.trim();
        if (accountStates != null) {
            try {
                AccountKeybindStateStore.State state = accountStates.load(activeAccount);
                if (state.isPresent()) {
                    for (KeybindRecord record : records) {
                        boolean enabled = state.getEnabledIds().contains(record.getId());
                        record.setEnabled(enabled);
                        record.setDisabledReason(enabled ? ""
                                : DisableReason.value("disabled_by_user"));
                    }
                } else {
                    persistAccountState();
                }
            } catch (Exception e) {
                log.error(Messages.text("registry.account_load_failed", activeAccount), e);
            }
        }

        int restored = 0;
        int removed = 0;
        int conflicts = 0;
        for (KeybindRecord record : records) {
            String command = commandFor(record);
            try {
                if (record.isEnabled()) {
                    try {
                        validate(record);
                        applyLimit(record, limit);
                    } catch (RuntimeException invalid) {
                        record.setEnabled(false);
                        record.setDisabledReason(validationDisabledReason(record));
                    }
                }

                BindSnapshot live = record.getKey().trim().isEmpty()
                        ? null : findLive(console, record.getKey());
                if (record.isEnabled()) {
                    if (live == null) {
                        installLive(console, record.getKey(), command);
                        restored++;
                    } else if (!live.getCommand().equalsIgnoreCase(command)) {
                        record.setEnabled(false);
                        record.setDisabledReason(DisableReason.value("key_used",
                                record.getKey(), live.getCommand()));
                        conflicts++;
                        log.warning(Messages.text("registry.restore_conflict",
                                record.getName(), record.getKey(), live.getCommand()));
                    }
                } else if (live != null
                        && live.getCommand().equalsIgnoreCase(command)
                        && removeLive(console, record.getKey(), command)) {
                    removed++;
                }
            } catch (Exception e) {
                record.setEnabled(false);
                record.setDisabledReason(DisableReason.value("restore_failed",
                        e.getClass().getSimpleName()));
                conflicts++;
                log.error(Messages.text("registry.restore_failed",
                        record.getName(), activeAccount), e);
            }
        }
        persistAccountState();
        if (restored > 0 || removed > 0 || conflicts > 0)
            log.info(Messages.text("registry.restore_summary",
                    activeAccount, restored, removed, conflicts));
    }

    public synchronized KeybindRecord createDraft() throws IOException {
        return createDraftAfter(null);
    }

    public synchronized KeybindRecord createDraftAfter(String afterId) throws IOException {
        KeybindRecord draft = new KeybindRecord(null, Messages.text("editor.new"), "",
                Collections.<org.keybinder.wurm.model.KeybindStep>emptyList());
        stamp(draft);
        draft.setEnabled(false);
        draft.setDisabledReason(DisableReason.value("not_configured"));
        int index = afterId == null ? -1 : indexOf(afterId);
        records.add(index < 0 ? records.size() : index + 1, draft);
        saveRecords();
        log.info(Messages.text("registry.draft_created"));
        return draft;
    }

    public synchronized boolean moveVisible(String id, List<String> visibleIds, int insertionIndex)
            throws IOException {
        int from = indexOf(id);
        if (from < 0) return false;
        KeybindRecord moving = records.remove(from);
        List<String> remainingVisible = new ArrayList<>(visibleIds);
        int visibleSource = remainingVisible.indexOf(id);
        if (visibleSource >= 0) remainingVisible.remove(visibleSource);
        int destination = RowInsertionCalculator.destinationAfterRemoval(
                Math.max(0, visibleSource), insertionIndex);
        destination = Math.max(0, Math.min(destination, remainingVisible.size()));
        int target;
        if (destination < remainingVisible.size()) {
            target = indexOf(remainingVisible.get(destination));
        } else if (!remainingVisible.isEmpty()) {
            target = indexOf(remainingVisible.get(remainingVisible.size() - 1)) + 1;
        } else {
            target = Math.min(from, records.size());
        }
        records.add(Math.max(0, Math.min(target, records.size())), moving);
        saveRecords();
        return true;
    }

    private int indexOf(String id) {
        for (int i = 0; i < records.size(); i++)
            if (records.get(i).getId().equals(id)) return i;
        return -1;
    }

    public synchronized KeybindRecord find(String id) {
        for (KeybindRecord record : records) if (record.getId().equals(id)) return record;
        return null;
    }

    /** Returns at most one enabled record for an exact canonical chord. */
    public synchronized KeybindRecord findEnabledByChord(String chord) {
        String wanted = InputKeyCatalog.normalizeChord(chord);
        for (KeybindRecord record : records)
            if (record.isEnabled()
                    && wanted.equals(InputKeyCatalog.normalizeChord(record.getKey())))
                return record;
        return null;
    }

    public synchronized KeybindConflict findKeybindSaveConflict(String id, String key,
                                                                 WurmConsole console)
            throws ReflectiveOperationException {
        KeybindRecord edited = find(id);
        if (edited == null)
            throw new IllegalArgumentException(Messages.text("event.record_missing", id));
        String newCommand = dispatcherCommand(edited);
        for (KeybindRecord candidate : records) {
            if (candidate != edited && candidate.isEnabled() && !candidate.getKey().trim().isEmpty()
                    && sameChord(candidate.getKey(), key))
                return conflictFor(key, candidate);
        }
        BindSnapshot live = findLive(console, key);
        if (live == null || live.getCommand().equalsIgnoreCase(newCommand)) return null;
        if (key.equalsIgnoreCase(edited.getKey())
                && live.getCommand().equalsIgnoreCase(commandFor(edited))) return null;
        return new KeybindConflict(key, KeybindConflict.VANILLA_OWNER, live.getCommand());
    }

    public synchronized KeybindConflict findEnableConflict(String id, WurmConsole console)
            throws ReflectiveOperationException {
        KeybindRecord requested = find(id);
        if (requested == null)
            throw new IllegalArgumentException(Messages.text("event.record_missing", id));
        for (KeybindRecord candidate : records) {
            if (candidate != requested && candidate.isEnabled()
                    && !candidate.getKey().trim().isEmpty()
                    && sameChord(candidate.getKey(), requested.getKey()))
                return conflictFor(requested.getKey(), candidate);
        }
        BindSnapshot live = findLive(console, requested.getKey());
        String command = commandFor(requested);
        if (live == null || live.getCommand().equalsIgnoreCase(command)
                || live.getCommand().equalsIgnoreCase(dispatcherCommand(requested))) return null;
        return new KeybindConflict(
                requested.getKey(), KeybindConflict.VANILLA_OWNER, live.getCommand());
    }

    public synchronized void setEnabled(String id, boolean enabled, WurmConsole console, int limit)
            throws IOException, ReflectiveOperationException {
        KeybindRecord record = find(id);
        if (record == null)
            throw new IllegalArgumentException(Messages.text("event.record_missing", id));
        if (enabled) {
            boolean oldEnabled = record.isEnabled();
            String oldReason = record.getDisabledReason();
            KeybindRecord displaced = findManagedConflict(record, record.getKey());
            boolean displacedEnabled = displaced != null && displaced.isEnabled();
            String displacedReason = displaced == null ? "" : displaced.getDisabledReason();
            ManagedBindTransaction live = new ManagedBindTransaction(binds, console);
            BindSnapshot liveBefore = live.capture(record.getKey());
            if (displaced != null) live.capture(displaced.getKey());
            String command = commandFor(record);
            try {
                validate(record);
                record.setEnabled(true);
                applyLimit(record, limit);
                if (!record.isEnabled())
                    throw new IllegalArgumentException(
                            DisableReason.display(record.getDisabledReason()));
                record.setDisabledReason("");
                if (displaced != null) {
                    displaced.setEnabled(false);
                    displaced.setDisabledReason(DisableReason.value("replaced_by", record.getName()));
                }
                saveRecords();
                if (displaced != null)
                    live.removeOwned(displaced.getKey(), commandFor(displaced));
                live.install(record.getKey(), command);
                announceConflict(liveBefore, record.getKey(), command);
                if (displaced != null)
                    log.warning(Messages.text("registry.displaced",
                            record.getKey(), displaced.getName()));
            } catch (RuntimeException | IOException | ReflectiveOperationException e) {
                record.setEnabled(oldEnabled);
                record.setDisabledReason(oldReason);
                if (displaced != null) {
                    displaced.setEnabled(displacedEnabled);
                    displaced.setDisabledReason(displacedReason);
                }
                rollbackStore(e);
                live.rollback(e);
                throw e;
            }
        } else {
            boolean oldEnabled = record.isEnabled();
            String oldReason = record.getDisabledReason();
            String command = commandFor(record);
            ManagedBindTransaction live = new ManagedBindTransaction(binds, console);
            BindSnapshot liveBefore = live.capture(record.getKey());
            boolean releasesChord = liveBefore == null
                    || liveBefore.getCommand().equalsIgnoreCase(command);
            for (KeybindRecord candidate : records)
                if (candidate != record && candidate.isEnabled()
                        && sameChord(candidate.getKey(), record.getKey())) releasesChord = false;
            List<KeybindRecord> unblocked = new ArrayList<KeybindRecord>();
            List<String> unblockedReasons = new ArrayList<String>();
            record.setEnabled(false);
            record.setDisabledReason(DisableReason.value("disabled_by_user"));
            if (releasesChord) {
                for (KeybindRecord candidate : records) {
                    if (candidate == record || candidate.isEnabled()
                            || !sameChord(candidate.getKey(), record.getKey())
                            || !DisableReason.isKeyConflict(candidate.getDisabledReason())) continue;
                    unblocked.add(candidate);
                    unblockedReasons.add(candidate.getDisabledReason());
                    candidate.setDisabledReason(DisableReason.value("disabled_by_user"));
                }
            }
            try {
                saveRecords();
                if (!record.getKey().isEmpty() && !record.getKeybindSteps().isEmpty())
                    live.removeOwned(record.getKey(), command);
            } catch (RuntimeException | IOException | ReflectiveOperationException e) {
                record.setEnabled(oldEnabled);
                record.setDisabledReason(oldReason);
                for (int i = 0; i < unblocked.size(); i++)
                    unblocked.get(i).setDisabledReason(unblockedReasons.get(i));
                rollbackStore(e);
                live.rollback(e);
                throw e;
            }
        }
    }

    public synchronized void add(KeybindRecord record, WurmConsole console, int limit)
            throws IOException, ReflectiveOperationException {
        stamp(record);
        validate(record);
        String command = commandFor(record);
        KeybindRecord displaced = findManagedConflict(record, record.getKey());
        boolean displacedEnabled = displaced != null && displaced.isEnabled();
        String displacedReason = displaced == null ? "" : displaced.getDisabledReason();
        ManagedBindTransaction live = new ManagedBindTransaction(binds, console);
        BindSnapshot liveBefore = live.capture(record.getKey());
        if (displaced != null) live.capture(displaced.getKey());
        if (displaced != null) {
            displaced.setEnabled(false);
            displaced.setDisabledReason(DisableReason.value("replaced_by", record.getName()));
        }
        applyLimit(record, limit);
        records.add(record);
        try {
            saveRecords();
            if (displaced != null)
                live.removeOwned(displaced.getKey(), commandFor(displaced));
            if (record.isEnabled()) live.install(record.getKey(), command);
        } catch (RuntimeException | IOException | ReflectiveOperationException e) {
            records.remove(record);
            if (displaced != null) {
                displaced.setEnabled(displacedEnabled);
                displaced.setDisabledReason(displacedReason);
            }
            rollbackStore(e);
            live.rollback(e);
            throw e;
        }
        announceConflict(liveBefore, record.getKey(), command);
        log.info(Messages.text("registry.added", record.getName(), record.getKey()));
    }

    public synchronized boolean delete(String id, WurmConsole console)
            throws IOException, ReflectiveOperationException {
        KeybindRecord found = null;
        for (KeybindRecord record : records) if (record.getId().equals(id)) found = record;
        if (found == null) return false;
        int index = records.indexOf(found);
        String command = commandFor(found);
        ManagedBindTransaction live = new ManagedBindTransaction(binds, console);
        live.capture(found.getKey());
        records.remove(found);
        try {
            saveRecords();
            live.removeOwned(found.getKey(), command);
        } catch (RuntimeException | IOException | ReflectiveOperationException e) {
            records.add(index, found);
            rollbackStore(e);
            live.rollback(e);
            throw e;
        }
        log.info(Messages.text("registry.deleted", found.getName()));
        return true;
    }

    public synchronized void updateKeybind(String id, String name, String key,
                                           List<org.keybinder.wurm.model.KeybindStep> steps,
                                           String createdByUser, String createdOnServer,
                                           WurmConsole console, int limit)
            throws IOException, ReflectiveOperationException {
        KeybindRecord found = find(id);
        if (found == null)
            throw new IllegalArgumentException(Messages.text("event.record_missing", id));
        KeybindRecord persisted = definitions.replaceSteps(found, name, key, steps,
                createdByUser, createdOnServer);
        replaceEnabledDefinition(found, persisted, console, limit);
        log.info(Messages.text("registry.updated", persisted.getName()));
    }

    public synchronized void updateVariants(String id, String name, String key,
                                            List<org.keybinder.wurm.model.KeybindVariant> variants,
                                            String activeVariantId, boolean hudMulti,
                                            String createdByUser,
                                            String createdOnServer, WurmConsole console, int limit)
            throws IOException, ReflectiveOperationException {
        KeybindRecord found = find(id);
        if (found == null)
            throw new IllegalArgumentException(Messages.text("event.record_missing", id));
        KeybindRecord persisted = definitions.replaceVariants(found, name, key, variants,
                activeVariantId, hudMulti, createdByUser, createdOnServer);
        replaceEnabledDefinition(found, persisted, console, limit);
        log.info(Messages.text("registry.updated", persisted.getDisplayName()));
    }

    public synchronized void updateVariantsDisabled(
            String id, String name, String key,
            List<org.keybinder.wurm.model.KeybindVariant> variants, String activeVariantId,
            boolean hudMulti, String reason, String createdByUser, String createdOnServer,
            WurmConsole console, int limit)
            throws IOException, ReflectiveOperationException {
        KeybindRecord found = find(id);
        if (found == null)
            throw new IllegalArgumentException(Messages.text("event.record_missing", id));
        KeybindRecord persisted = definitions.replaceVariants(found, name, key, variants,
                activeVariantId, hudMulti, createdByUser, createdOnServer);
        replaceDisabledDefinition(found, persisted, reason, console, limit);
        log.info(Messages.text("registry.saved_disabled", persisted.getDisplayName(),
                DisableReason.display(persisted.getDisabledReason())));
    }

    public synchronized boolean selectVariant(String recordId, String variantId) throws IOException {
        KeybindRecord record = find(recordId);
        if (record == null || record.findVariant(variantId) == null) return false;
        if (variantId.equals(record.getActiveVariantId())) return false;
        String previous = record.getActiveVariantId();
        record.setActiveVariantId(variantId);
        try {
            saveRecords();
        } catch (IOException e) {
            record.setActiveVariantId(previous);
            throw e;
        }
        return true;
    }

    public synchronized void updateKeybindDisabled(String id, String name, String key,
                                                    List<org.keybinder.wurm.model.KeybindStep> steps,
                                                    String reason, String createdByUser,
                                                    String createdOnServer, WurmConsole console, int limit)
            throws IOException, ReflectiveOperationException {
        KeybindRecord found = find(id);
        if (found == null)
            throw new IllegalArgumentException(Messages.text("event.record_missing", id));
        KeybindRecord persisted = definitions.replaceSteps(found, name, key, steps,
                createdByUser, createdOnServer);
        replaceDisabledDefinition(found, persisted, reason, console, limit);
        log.info(Messages.text("registry.saved_disabled", persisted.getName(),
                DisableReason.display(persisted.getDisabledReason())));
    }

    private void replaceEnabledDefinition(KeybindRecord found, KeybindRecord persisted,
                                          WurmConsole console, int limit)
            throws IOException, ReflectiveOperationException {
        validate(persisted);
        applyLimit(persisted, limit);
        String oldKey = found.getKey();
        String oldCommand = commandFor(found);
        String newCommand = commandFor(persisted);
        KeybindRecord displaced = findManagedConflict(found, persisted.getKey());
        boolean displacedEnabled = displaced != null && displaced.isEnabled();
        String displacedReason = displaced == null ? "" : displaced.getDisabledReason();
        ManagedBindTransaction live = new ManagedBindTransaction(binds, console);
        live.capture(oldKey);
        live.capture(persisted.getKey());
        if (displaced != null) live.capture(displaced.getKey());
        if (displaced != null) {
            displaced.setEnabled(false);
            displaced.setDisabledReason(DisableReason.value(
                    "replaced_by", persisted.getName()));
            log.warning(Messages.text("registry.displaced",
                    persisted.getKey(), displaced.getName()));
        }
        int index = records.indexOf(found);
        records.set(index, persisted);
        try {
            saveRecords();
            if (displaced != null)
                live.removeOwned(displaced.getKey(), commandFor(displaced));
            if (!sameChord(oldKey, persisted.getKey()))
                live.removeOwned(oldKey, oldCommand);
            if (persisted.isEnabled()) live.install(persisted.getKey(), newCommand);
        } catch (RuntimeException | IOException | ReflectiveOperationException failure) {
            records.set(index, found);
            if (displaced != null) {
                displaced.setEnabled(displacedEnabled);
                displaced.setDisabledReason(displacedReason);
            }
            rollbackStore(failure);
            live.rollback(failure);
            throw failure;
        }
    }

    private void replaceDisabledDefinition(KeybindRecord found, KeybindRecord persisted,
                                           String reason, WurmConsole console, int limit)
            throws IOException, ReflectiveOperationException {
        validate(persisted);
        applyLimit(persisted, limit);
        persisted.setEnabled(false);
        persisted.setDisabledReason(reason == null || reason.trim().isEmpty()
                ? DisableReason.value("key_in_use") : reason);
        String oldKey = found.getKey();
        String oldCommand = commandFor(found);
        ManagedBindTransaction live = new ManagedBindTransaction(binds, console);
        live.capture(oldKey);
        int index = records.indexOf(found);
        records.set(index, persisted);
        try {
            saveRecords();
            live.removeOwned(oldKey, oldCommand);
        } catch (RuntimeException | IOException | ReflectiveOperationException failure) {
            records.set(index, found);
            rollbackStore(failure);
            live.rollback(failure);
            throw failure;
        }
    }

    private KeybindRecord findManagedConflict(KeybindRecord except, String key) {
        for (KeybindRecord candidate : records)
            if (candidate != except && candidate.isEnabled() && sameChord(candidate.getKey(), key))
                return candidate;
        return null;
    }

    private void disableManagedConflict(KeybindRecord except, String key) {
        KeybindRecord conflict = findManagedConflict(except, key);
        if (conflict == null) return;
        conflict.setEnabled(false);
        conflict.setDisabledReason(DisableReason.value("replaced_by", except.getName()));
        log.warning(Messages.text("registry.displaced", key, conflict.getName()));
    }

    private void announceConflict(BindSnapshot conflict, String key, String newCommand) {
        if (conflict != null && !conflict.getCommand().equalsIgnoreCase(newCommand))
            log.warning(Messages.text("registry.foreign_replaced", key, conflict.getCommand()));
    }

    private void rollbackStore(Throwable original) {
        try {
            saveRecords();
        } catch (Throwable rollbackFailure) {
            original.addSuppressed(rollbackFailure);
        }
    }

    private void saveRecords() throws IOException {
        normalizeNames();
        store.save(records);
        observedStoreModified = store.lastModifiedMillis();
        persistAccountState();
    }

    private void normalizeNames() {
        for (KeybindRecord record : records) normalizeName(record);
    }

    private static void normalizeName(KeybindRecord record) {
        record.setName(KeybindNamePrefixes.apply(record.getName(),
                record.getVariants().size(), record.isHudMulti()));
    }

    private void persistAccountState() {
        if (accountStates == null || activeAccount.isEmpty()) return;
        java.util.Set<String> enabled = new java.util.HashSet<String>();
        for (KeybindRecord record : records)
            if (record.isEnabled()) enabled.add(record.getId());
        try {
            accountStates.save(activeAccount, enabled);
        } catch (IOException e) {
            log.error(Messages.text("registry.account_save_failed", activeAccount), e);
        }
    }

    public synchronized List<BindSnapshot> importCandidates(WurmConsole console)
            throws ReflectiveOperationException, IOException {
        java.util.Map<String, String> defaults = new DefaultBindCatalog().load();
        List<BindSnapshot> candidates = new ArrayList<>();
        for (BindSnapshot bind : binds.snapshot(console)) {
            if (!importPolicy.mayImport(bind.getCommand())) continue;
            String defaultCommand = defaults.get(DefaultBindCatalog.normalize(bind.getKey()));
            if (defaultCommand != null && defaultCommand.equalsIgnoreCase(bind.getCommand())) continue;
            boolean managed = false;
            for (KeybindRecord record : records) {
                if (record.getKey().equalsIgnoreCase(bind.getKey())
                        && commandFor(record).equalsIgnoreCase(bind.getCommand())) {
                    managed = true;
                    break;
                }
            }
            if (!managed) candidates.add(bind);
        }
        return candidates;
    }

    public synchronized KeybindRecord duplicate(String sourceId) throws IOException {
        KeybindRecord source = find(sourceId);
        if (source == null)
            throw new IllegalArgumentException(Messages.text("event.record_missing", sourceId));
        String name = Messages.text("keybind.copy_name", source.getName());
        KeybindRecord duplicate = definitions.duplicate(
                source, name, currentUser, currentServer);
        int sourceIndex = indexOf(sourceId);
        records.add(sourceIndex + 1, duplicate);
        try {
            saveRecords();
        } catch (IOException | RuntimeException failure) {
            records.remove(duplicate);
            rollbackStore(failure);
            throw failure;
        }
        log.info(Messages.text("registry.duplicated", source.getName(), duplicate.getName()));
        return duplicate;
    }

    /**
     * Persists the editor's current parent definition without one alternative and inserts a
     * disabled, independently identified copy of that alternative immediately after it.
     */
    public synchronized KeybindRecord extractVariant(
            String sourceId, String name, String key, List<KeybindVariant> variants,
            String activeVariantId, boolean hudMulti, String extractedVariantId,
            String createdByUser, String createdOnServer, String parentDisabledReason,
            WurmConsole console, int limit)
            throws IOException, ReflectiveOperationException {
        KeybindRecord source = find(sourceId);
        if (source == null)
            throw new IllegalArgumentException(Messages.text("event.record_missing", sourceId));
        KeybindDefinitionService.Extraction extraction = definitions.extract(
                source, name, key, variants, activeVariantId, hudMulti, extractedVariantId,
                createdByUser, createdOnServer, currentUser, currentServer,
                parentDisabledReason);
        KeybindRecord parent = extraction.getParent();
        KeybindRecord extracted = extraction.getExtracted();
        String variantName = extraction.getVariantName();
        validate(parent);
        if (parent.isEnabled()) applyLimit(parent, limit);
        validate(extracted);

        String oldKey = source.getKey();
        String oldCommand = commandFor(source);
        String newCommand = commandFor(parent);
        ManagedBindTransaction live = new ManagedBindTransaction(binds, console);
        live.capture(oldKey);
        live.capture(key);
        KeybindRecord displaced = parent.isEnabled() ? findManagedConflict(source, key) : null;
        boolean displacedEnabled = displaced != null && displaced.isEnabled();
        String displacedReason = displaced == null ? "" : displaced.getDisabledReason();
        if (displaced != null) live.capture(displaced.getKey());
        List<KeybindRecord> before = new ArrayList<KeybindRecord>(records);
        int sourceIndex = records.indexOf(source);
        if (displaced != null) {
            displaced.setEnabled(false);
            displaced.setDisabledReason(DisableReason.value("replaced_by", parent.getName()));
        }
        records.set(sourceIndex, parent);
        records.add(sourceIndex + 1, extracted);
        try {
            saveRecords();
            if (displaced != null)
                live.removeOwned(displaced.getKey(), commandFor(displaced));
            if (source.isEnabled() && (!parent.isEnabled() || !sameChord(oldKey, key)))
                live.removeOwned(oldKey, oldCommand);
            if (parent.isEnabled()) live.install(key, newCommand);
        } catch (RuntimeException | IOException | ReflectiveOperationException failure) {
            if (displaced != null) {
                displaced.setEnabled(displacedEnabled);
                displaced.setDisabledReason(displacedReason);
            }
            records.clear();
            records.addAll(before);
            rollbackStore(failure);
            live.rollback(failure);
            throw failure;
        }
        if (displaced != null)
            log.warning(Messages.text("registry.displaced", key, displaced.getName()));
        log.info(Messages.text("registry.extracted", variantName, source.getName(),
                extracted.getName()));
        return extracted;
    }

    public synchronized KeybindRecord merge(String sourceId, String destinationId,
                                             WurmConsole console)
            throws IOException, ReflectiveOperationException {
        if (sourceId == null || sourceId.equals(destinationId))
            throw new IllegalArgumentException(Messages.text("merge.self"));
        KeybindRecord source = find(sourceId);
        KeybindRecord destination = find(destinationId);
        if (source == null || destination == null)
            throw new IllegalArgumentException(Messages.text("merge.record_missing"));
        int resultCount = source.getVariants().size() + destination.getVariants().size();
        if (resultCount > KeybindLimits.MAX_VARIANTS)
            throw new IllegalArgumentException(Messages.text("merge.too_many",
                    destination.getVariants().size(), source.getVariants().size(), resultCount,
                    KeybindLimits.MAX_VARIANTS));

        String sourceCommand = commandFor(source);
        ManagedBindTransaction live = new ManagedBindTransaction(binds, console);
        BindSnapshot sourceLive = live.capture(source.getKey());
        boolean sourceLiveOwned = sourceLive != null
                && sourceLive.getCommand().equalsIgnoreCase(sourceCommand);
        if (!InputKeyCatalog.isVirtual(source.getKey())) {
            if (source.isEnabled() && !sourceLiveOwned)
                throw new IllegalStateException(Messages.text("merge.ownership_mismatch",
                        source.getKey()));
        }
        BindSnapshot destinationLive = live.capture(destination.getKey());
        List<KeybindRecord> before = new ArrayList<KeybindRecord>(records);
        KeybindRecord merged = definitions.merge(source, destination);
        int destinationIndex = records.indexOf(destination);
        records.set(destinationIndex, merged);
        records.remove(source);
        try {
            validate(merged);
            saveRecords();
            if (sourceLiveOwned && !live.removeOwned(source.getKey(), sourceCommand))
                throw new IllegalStateException(Messages.text("merge.ownership_mismatch",
                        source.getKey()));
            BindSnapshot afterSource = findLive(console, source.getKey());
            if (afterSource != null
                    && afterSource.getCommand().equalsIgnoreCase(sourceCommand))
                throw new IllegalStateException(Messages.text("merge.source_bind_remains"));
            BindSnapshot afterDestination = findLive(console, destination.getKey());
            if (!sameBind(destinationLive, afterDestination))
                throw new IllegalStateException(Messages.text("merge.destination_changed"));
        } catch (RuntimeException | IOException | ReflectiveOperationException failure) {
            records.clear();
            records.addAll(before);
            rollbackStore(failure);
            live.rollback(failure);
            throw failure;
        }
        log.info(Messages.text("registry.merged", source.getName(), destination.getName(),
                merged.getVariants().size()));
        return merged;
    }

    public synchronized TransferImportResult importPortable(
            List<PortableKeybindDefinition> definitions) throws IOException {
        return importPortable(definitions, false);
    }

    @Override public synchronized TransferImportResult importValuePack(
            List<PortableKeybindDefinition> definitions) throws IOException {
        return importPortable(definitions, true);
    }

    private TransferImportResult importPortable(
            List<PortableKeybindDefinition> definitions, boolean valuePack) throws IOException {
        java.util.Map<String, KeybindRecord> fingerprints =
                new java.util.HashMap<String, KeybindRecord>();
        for (KeybindRecord record : records)
            fingerprints.put(SemanticFingerprint.of(
                    PortableKeybindDefinition.fromRecord(record)), record);
        List<KeybindRecord> before = new ArrayList<KeybindRecord>(records);
        List<KeybindRecord> newlyMarked = new ArrayList<KeybindRecord>();
        boolean markerBefore = store.wasValuePackProvided();
        int imported = 0;
        int skipped = 0;
        for (PortableKeybindDefinition definition : definitions) {
            String fingerprint = SemanticFingerprint.of(definition);
            KeybindRecord existing = fingerprints.get(fingerprint);
            if (existing != null) {
                if (valuePack && !existing.isValuePack()) {
                    existing.setValuePack(true);
                    newlyMarked.add(existing);
                }
                skipped++;
                continue;
            }
            KeybindRecord record = definition.toRecord(currentUser, currentServer);
            record.setValuePack(valuePack);
            record.setEnabled(false);
            record.setDisabledReason(DisableReason.value(definition.hasExactObject()
                    ? "nonportable_object_review" : "import_review"));
            records.add(record);
            fingerprints.put(fingerprint, record);
            imported++;
        }
        try {
            if (valuePack) store.setValuePackProvided(true);
            saveRecords();
        } catch (IOException | RuntimeException failure) {
            store.setValuePackProvided(markerBefore);
            for (KeybindRecord record : newlyMarked) record.setValuePack(false);
            records.clear();
            records.addAll(before);
            rollbackStore(failure);
            throw failure;
        }
        return new TransferImportResult(imported, skipped, 0);
    }

    private static boolean sameBind(BindSnapshot left, BindSnapshot right) {
        if (left == null || right == null) return left == right;
        return left.getKey().equalsIgnoreCase(right.getKey())
                && left.getCommand().equalsIgnoreCase(right.getCommand());
    }

    public static final class RestoreResult {
        private final int restored;
        private final int removed;
        private final int conflicts;

        private RestoreResult(int restored, int removed, int conflicts) {
            this.restored = restored;
            this.removed = removed;
            this.conflicts = conflicts;
        }

        public int getRestored() { return restored; }
        public int getRemoved() { return removed; }
        public int getConflicts() { return conflicts; }
    }

    /**
     * Removes live dispatchers owned by Keybinder and restores imported
     * commands on free chords. Records stay persisted but disabled so this is
     * safe to run before deleting the mod and reversible after reinstalling it.
     */
    public synchronized RestoreResult prepareForUninstall(WurmConsole console)
            throws ReflectiveOperationException, IOException {
        int restored = 0;
        int removed = 0;
        int conflicts = 0;
        for (KeybindRecord record : records) {
            String managedCommand = commandFor(record);
            BindSnapshot live = findLive(console, record.getKey());
            if (live != null && live.getCommand().equalsIgnoreCase(managedCommand)) {
                if (removeLive(console, record.getKey(), managedCommand)) removed++;
            }

            String originalKey = record.getOriginalKey() == null
                    ? "" : record.getOriginalKey().trim();
            String originalCommand = record.getOriginalCommand() == null
                    ? "" : record.getOriginalCommand().trim();
            if (!originalKey.isEmpty() && !originalCommand.isEmpty()) {
                BindSnapshot originalLive = findLive(console, originalKey);
                if (originalLive == null) {
                    installLive(console, originalKey, originalCommand);
                    restored++;
                } else if (!originalLive.getCommand().equalsIgnoreCase(originalCommand)) {
                    conflicts++;
                    log.warning(Messages.text("registry.restore_conflict",
                            record.getName(), originalKey, originalLive.getCommand()));
                }
            }
            record.setEnabled(false);
            record.setDisabledReason(DisableReason.value("disabled_by_user"));
        }
        saveRecords();
        return new RestoreResult(restored, removed, conflicts);
    }

    public synchronized int importAllReviewed(WurmConsole console, int limit)
            throws ReflectiveOperationException, IOException {
        List<BindSnapshot> candidates = importCandidates(console);
        int imported = 0;
        for (BindSnapshot candidate : candidates) {
            BindSnapshot current = binds.findByKey(console, candidate.getKey());
            if (current == null || !current.getCommand().equalsIgnoreCase(candidate.getCommand())) continue;
            KeybindRecord record;
            if (customActionsImporter.supports(candidate.getCommand())) {
                record = new KeybindRecord(null,
                        Messages.text("registry.imported_name", candidate.getKey()), candidate.getKey(),
                        customActionsImporter.importCommand(candidate.getCommand()));
                applyLimit(record, limit);
            } else if (improvedImproveImporter.supports(candidate.getCommand())) {
                record = new KeybindRecord(null,
                        Messages.text("registry.imported_name", candidate.getKey()), candidate.getKey(),
                        improvedImproveImporter.importCommand(candidate.getCommand()));
                applyLimit(record, limit);
            } else {
                record = new KeybindRecord(null,
                        Messages.text("registry.imported_name", candidate.getKey()), candidate.getKey(),
                        Collections.<org.keybinder.wurm.model.KeybindStep>singletonList(
                                new ConsoleCommandStep(candidate.getCommand(), true)));
            }
            record.setOriginalKey(candidate.getKey());
            record.setOriginalCommand(candidate.getCommand());
            stamp(record);
            records.add(record);
            try {
                saveRecords();
                if (!binds.removeIfOwned(console, candidate.getKey(), candidate.getCommand()))
                    throw new IllegalStateException(Messages.text("error.import_binding_changed"));
                if (record.isEnabled()) binds.install(console, record.getKey(), commandFor(record));
                imported++;
                log.info(Messages.text("registry.imported",
                        candidate.getKey(), candidate.getCommand()));
            } catch (RuntimeException | IOException | ReflectiveOperationException e) {
                records.remove(record);
                saveRecords();
                BindSnapshot after = binds.findByKey(console, candidate.getKey());
                if (after == null) binds.install(console, candidate.getKey(), candidate.getCommand());
                log.error(Messages.text("registry.import_failed", candidate.getKey()), e);
            }
        }
        return imported;
    }

    private void stamp(KeybindRecord record) {
        if (record.getCreatedByUser().isEmpty()) record.setCreatedByUser(currentUser);
        if (record.getCreatedOnServer().isEmpty()) record.setCreatedOnServer(currentServer);
    }

    private KeybindConflict conflictFor(String key, KeybindRecord record) {
        return new KeybindConflict(key, record.getName(), commandFor(record),
                record.getCreatedByUser(), record.getCreatedOnServer());
    }

    public synchronized void enrichCurrentServerName(String shortName, String fullName) {
        if (shortName == null || fullName == null) return;
        String oldValue = shortName.trim();
        String newValue = fullName.trim();
        if (oldValue.isEmpty() || newValue.isEmpty() || oldValue.equalsIgnoreCase(newValue)) return;
        boolean changed = false;
        for (KeybindRecord record : records) {
            if (oldValue.equalsIgnoreCase(record.getCreatedOnServer())) {
                record.setCreatedOnServer(newValue);
                changed = true;
            }
        }
        if (!changed) return;
        try {
            saveRecords();
            log.info(Messages.text("registry.server_expanded", oldValue, newValue));
        } catch (IOException e) {
            log.error(Messages.text("registry.server_save_failed"), e);
        }
    }

    public synchronized void enforceLimit(int limit, WurmConsole console) {
        boolean changed = false;
        for (KeybindRecord record : records) {
            if (!record.getPreviousManagedCommand().isEmpty()) {
                try {
                    BindSnapshot live = findLive(console, record.getKey());
                    if (live != null && live.getCommand().equalsIgnoreCase(
                            record.getPreviousManagedCommand())) {
                        if (!removeLive(console, record.getKey(),
                                record.getPreviousManagedCommand()))
                            throw new IllegalStateException(
                                    Messages.text("error.migration_binding_changed"));
                        if (record.isEnabled())
                            installLive(console, record.getKey(), commandFor(record));
                        log.info(Messages.text("registry.migrated",
                                record.getName(), commandFor(record)));
                    }
                    record.setPreviousManagedCommand("");
                    changed = true;
                } catch (Exception e) {
                    log.error(Messages.text("registry.migrate_failed", record.getName()), e);
                }
            }
            boolean wasEnabled = record.isEnabled();
            applyLimit(record, limit);
            if (wasEnabled && !record.isEnabled()) {
                try { removeLive(console, record.getKey(), commandFor(record)); }
                catch (Exception e) {
                    log.error(Messages.text("registry.disable_failed", record.getName()), e);
                }
                log.warning(Messages.text("registry.disabled", record.getName(),
                        DisableReason.display(record.getDisabledReason())));
                changed = true;
            }
        }
        if (changed) try { saveRecords(); }
        catch (IOException e) { log.error(Messages.text("registry.disabled_save_failed"), e); }
    }

    public synchronized void printAll(EventLogger logger, int limit, boolean includeCommands) {
        List<KeybindRecord> sorted = new ArrayList<>(records);
        sorted.sort(Comparator.comparing(KeybindRecord::getKey, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(KeybindRecord::getName, String.CASE_INSENSITIVE_ORDER));
        logger.info(Messages.text("event.list.header", sorted.size()));
        for (KeybindRecord record : sorted) {
            QueueCost value = costs.keybindCost(record);
            String cost = value.getKind() == QueueCost.Kind.FIXED
                    ? value.getValue() + "/" + limit
                    : Messages.text(value.getKind() == QueueCost.Kind.DYNAMIC
                    ? "event.cost.dynamic" : "event.cost.unknown");
            String status = record.isEnabled() ? Messages.text("event.list.active")
                    : Messages.text("event.list.disabled",
                    DisableReason.display(record.getDisabledReason()));
            logger.info(Messages.text("event.list.item",
                    InputKeyCatalog.displayChord(record.getKey()), record.getDisplayName(),
                    record.isMultiPurpose() ? Messages.text("event.list.multi") : "",
                    contents(record), cost, status));
            if (includeCommands)
                logger.info(Messages.text("event.list.command", commandFor(record)));
        }
        logger.info(Messages.text("event.list.end"));
    }

    private void validate(KeybindRecord record) {
        KeybindValidator.validateConfigured(record);
    }

    private static String validationDisabledReason(KeybindRecord record) {
        return KeybindValidator.disabledReason(record);
    }

    private String commandFor(KeybindRecord record) {
        return dispatcherCommand(record);
    }

    public String dispatcherCommand(KeybindRecord record) {
        return "keybinder_run " + record.getId();
    }

    private static boolean sameChord(String left, String right) {
        return InputKeyCatalog.normalizeChord(left)
                .equals(InputKeyCatalog.normalizeChord(right));
    }

    private BindSnapshot findLive(WurmConsole console, String key)
            throws ReflectiveOperationException {
        return InputKeyCatalog.isVirtual(key) ? null : binds.findByKey(console, key);
    }

    private void installLive(WurmConsole console, String key, String command)
            throws ReflectiveOperationException {
        if (!InputKeyCatalog.isVirtual(key)) binds.install(console, key, command);
    }

    private boolean removeLive(WurmConsole console, String key, String command)
            throws ReflectiveOperationException {
        return InputKeyCatalog.isVirtual(key) || binds.removeIfOwned(console, key, command);
    }

    private void applyLimit(KeybindRecord record, int limit) {
        QueueCost cost = costs.keybindCost(record);
        if (limit > 0 && cost.getKind() == QueueCost.Kind.FIXED && cost.getValue() > limit) {
            record.setEnabled(false);
            record.setDisabledReason(DisableReason.value("queue_exceeded", cost.getValue(), limit));
        }
    }

    private String contents(KeybindRecord record) {
        java.util.Set<org.keybinder.wurm.model.StepKind> kinds = new java.util.HashSet<>();
        for (org.keybinder.wurm.model.KeybindStep step : record.getKeybindSteps()) kinds.add(step.getKind());
        if (kinds.size() > 1) return Messages.text("event.type.mixed");
        if (kinds.isEmpty()) return Messages.text("event.type.empty");
        switch (kinds.iterator().next()) {
            case ACTIVATE_TOOL: return Messages.text("event.type.activate");
            case SMART_IMPROVE: return Messages.text("event.type.improve");
            case VANILLA_ACTION: return Messages.text("event.type.vanilla");
            case CONSOLE_COMMAND: return Messages.text("event.type.console");
            default: return Messages.text("event.type.custom");
        }
    }
}
