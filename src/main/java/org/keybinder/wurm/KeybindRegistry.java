package org.keybinder.wurm;

import com.wurmonline.client.console.WurmConsole;
import org.keybinder.wurm.bind.BindSnapshot;
import org.keybinder.wurm.bind.ManagedBindAccess;
import org.keybinder.wurm.bind.ManagedBindTransaction;
import org.keybinder.wurm.bind.AccountBindingCoordinator;
import org.keybinder.wurm.bind.AccountBindingRestorer;
import org.keybinder.wurm.bind.ExternalBindingSynchronizer;
import org.keybinder.wurm.bind.VanillaImportService;
import org.keybinder.wurm.bind.ManagedKeybindMutationService;
import org.keybinder.wurm.event.EventLogger;
import org.keybinder.wurm.i18n.DisableReason;
import org.keybinder.wurm.i18n.Messages;
import org.keybinder.wurm.migration.CustomActionsImporter;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.model.KeybindConflict;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.model.KeybindLimits;
import org.keybinder.wurm.model.KeybindNamePrefixes;
import org.keybinder.wurm.model.KeybindDefinitionService;
import org.keybinder.wurm.model.KeybindVariant;
import org.keybinder.wurm.queue.ActionQueueCostCalculator;
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
import java.util.List;

public final class KeybindRegistry implements ValuePackTarget {
    private final List<KeybindRecord> records = new ArrayList<>();
    private final KeybindStore store;
    private final AccountBindingCoordinator accountBindings;
    private final AccountBindingRestorer accountRestorer;
    private final ExternalBindingSynchronizer externalSynchronizer;
    private final ManagedBindAccess binds;
    private final VanillaImportService vanillaImports;
    private final ManagedKeybindMutationService mutations;
    private final EventLogger log;
    private final KeybindDefinitionService definitions = new KeybindDefinitionService();
    private String currentUser = "";
    private String currentServer = "";
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
        this.accountBindings = new AccountBindingCoordinator(accountStates, log);
        this.accountRestorer = new AccountBindingRestorer(
                binds, accountBindings, log);
        this.externalSynchronizer = new ExternalBindingSynchronizer(store, binds, log);
        this.binds = binds;
        this.vanillaImports = new VanillaImportService(binds, customActionsImporter, log);
        this.mutations = new ManagedKeybindMutationService(binds, log);
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
        ExternalBindingSynchronizer.Result result = externalSynchronizer.synchronize(
                records, observedStoreModified, console, this::commandFor);
        observedStoreModified = result.getObservedModified();
        if (!result.isChanged()) return false;
        records.clear();
        records.addAll(result.getRecords());
        normalizeNames();
        accountBindings.persist(records);
        return true;
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
    public synchronized boolean restoreAccountBindings(
            String account, WurmConsole console) {
        return accountRestorer.restore(account, records, console, this::commandFor);
    }

    public synchronized void persistAccountBindings() {
        accountBindings.persist(records);
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
        mutations.setEnabled(id, enabled, console, limit, mutationContext());
    }

    public synchronized void add(KeybindRecord record, WurmConsole console, int limit)
            throws IOException, ReflectiveOperationException {
        mutations.add(record, console, limit, mutationContext());
    }

    public synchronized boolean delete(String id, WurmConsole console)
            throws IOException, ReflectiveOperationException {
        return mutations.delete(id, console, mutationContext());
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
        clearLegacyQueueDisable(persisted);
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
        clearLegacyQueueDisable(persisted);
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
        accountBindings.persist(records);
    }

    private void normalizeNames() {
        for (KeybindRecord record : records) normalizeName(record);
    }

    private static void normalizeName(KeybindRecord record) {
        record.setName(KeybindNamePrefixes.apply(record.getName(),
                record.getVariants().size(), record.isHudMulti()));
    }

    public synchronized List<BindSnapshot> importCandidates(WurmConsole console)
            throws ReflectiveOperationException, IOException {
        return vanillaImports.candidates(console, importContext());
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
        if (parent.isEnabled()) clearLegacyQueueDisable(parent);
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
        return importReviewed(console, importCandidates(console), limit);
    }

    /** Imports only rows explicitly selected in the review window. */
    public synchronized int importReviewed(WurmConsole console,
                                           List<BindSnapshot> selected,
                                           int limit)
            throws ReflectiveOperationException, IOException {
        return vanillaImports.importSelected(console, selected, limit, importContext());
    }

    private VanillaImportService.Context importContext() {
        return new VanillaImportService.Context() {
            @Override public List<KeybindRecord> records() { return records; }
            @Override public String commandFor(KeybindRecord record) {
                return KeybindRegistry.this.commandFor(record);
            }
            @Override public void stamp(KeybindRecord record) {
                KeybindRegistry.this.stamp(record);
            }
            @Override public void save() throws IOException { saveRecords(); }
        };
    }

    private ManagedKeybindMutationService.Context mutationContext() {
        return new ManagedKeybindMutationService.Context() {
            @Override public List<KeybindRecord> records() { return records; }
            @Override public KeybindRecord find(String id) {
                return KeybindRegistry.this.find(id);
            }
            @Override public KeybindRecord findManagedConflict(
                    KeybindRecord except, String key) {
                return KeybindRegistry.this.findManagedConflict(except, key);
            }
            @Override public String commandFor(KeybindRecord record) {
                return KeybindRegistry.this.commandFor(record);
            }
            @Override public boolean sameChord(String left, String right) {
                return KeybindRegistry.sameChord(left, right);
            }
            @Override public void stamp(KeybindRecord record) {
                KeybindRegistry.this.stamp(record);
            }
            @Override public void validate(KeybindRecord record) {
                KeybindRegistry.this.validate(record);
            }
            @Override public void save() throws IOException { saveRecords(); }
            @Override public void rollbackStore(Throwable original) {
                KeybindRegistry.this.rollbackStore(original);
            }
            @Override public void announceConflict(
                    BindSnapshot conflict, String key, String command) {
                KeybindRegistry.this.announceConflict(conflict, key, command);
            }
        };
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

    public synchronized void reconcileManagedBindings(WurmConsole console) {
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
            clearLegacyQueueDisable(record);
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

    private void clearLegacyQueueDisable(KeybindRecord record) {
        // Queue capacity is runtime state, not a validity or activation rule.
        // The editor/list still calculate and display the warning, while the
        // executor budgets each concrete step immediately before dispatch.
        if (record.isEnabled()
                && DisableReason.isQueueExceeded(record.getDisabledReason()))
            record.setDisabledReason("");
    }

    private String contents(KeybindRecord record) {
        java.util.Set<org.keybinder.wurm.model.StepKind> kinds = new java.util.HashSet<>();
        for (org.keybinder.wurm.model.KeybindStep step : record.getKeybindSteps()) kinds.add(step.getKind());
        if (kinds.size() > 1) return Messages.text("event.type.mixed");
        if (kinds.isEmpty()) return Messages.text("event.type.empty");
        switch (kinds.iterator().next()) {
            case ACTIVATE_TOOL: return Messages.text("event.type.activate");
            case SMART_IMPROVE: return Messages.text("event.type.improve");
            case ARCHEOLOGY_IDENTIFY:
                return Messages.text("event.type.archeology_identify");
            case VANILLA_ACTION: return Messages.text("event.type.vanilla");
            case CONSOLE_COMMAND: return Messages.text("event.type.console");
            default: return Messages.text("event.type.custom");
        }
    }
}
