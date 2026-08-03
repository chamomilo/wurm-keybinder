package org.keybinder.wurm.bind;

import com.wurmonline.client.console.WurmConsole;
import org.keybinder.wurm.event.EventLogger;
import org.keybinder.wurm.i18n.DisableReason;
import org.keybinder.wurm.i18n.Messages;
import org.keybinder.wurm.model.KeybindRecord;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Transaction template for the common add/delete/enable live-bind mutations. */
public final class ManagedKeybindMutationService {
    public interface Context {
        List<KeybindRecord> records();
        KeybindRecord find(String id);
        KeybindRecord findManagedConflict(KeybindRecord except, String key);
        String commandFor(KeybindRecord record);
        boolean sameChord(String left, String right);
        void stamp(KeybindRecord record);
        void validate(KeybindRecord record);
        void applyLimit(KeybindRecord record, int limit);
        void save() throws IOException;
        void rollbackStore(Throwable original);
        void announceConflict(BindSnapshot conflict, String key, String newCommand);
    }

    private final ManagedBindAccess binds;
    private final EventLogger log;

    public ManagedKeybindMutationService(ManagedBindAccess binds, EventLogger log) {
        this.binds = binds;
        this.log = log;
    }

    public void setEnabled(String id, boolean enabled, WurmConsole console, int limit,
                           Context context)
            throws IOException, ReflectiveOperationException {
        KeybindRecord record = context.find(id);
        if (record == null)
            throw new IllegalArgumentException(Messages.text("event.record_missing", id));
        if (enabled) enable(record, console, limit, context);
        else disable(record, console, context);
    }

    private void enable(KeybindRecord record, WurmConsole console, int limit, Context context)
            throws IOException, ReflectiveOperationException {
        boolean oldEnabled = record.isEnabled();
        String oldReason = record.getDisabledReason();
        KeybindRecord displaced = context.findManagedConflict(record, record.getKey());
        boolean displacedEnabled = displaced != null && displaced.isEnabled();
        String displacedReason = displaced == null ? "" : displaced.getDisabledReason();
        ManagedBindTransaction live = new ManagedBindTransaction(binds, console);
        BindSnapshot liveBefore = live.capture(record.getKey());
        if (displaced != null) live.capture(displaced.getKey());
        String command = context.commandFor(record);
        try {
            context.validate(record);
            record.setEnabled(true);
            context.applyLimit(record, limit);
            if (!record.isEnabled())
                throw new IllegalArgumentException(
                        DisableReason.display(record.getDisabledReason()));
            record.setDisabledReason("");
            if (displaced != null) {
                displaced.setEnabled(false);
                displaced.setDisabledReason(
                        DisableReason.value("replaced_by", record.getName()));
            }
            context.save();
            if (displaced != null)
                live.removeOwned(displaced.getKey(), context.commandFor(displaced));
            live.install(record.getKey(), command);
            context.announceConflict(liveBefore, record.getKey(), command);
            if (displaced != null)
                log.warning(Messages.text("registry.displaced",
                        record.getKey(), displaced.getName()));
        } catch (RuntimeException | IOException | ReflectiveOperationException failure) {
            record.setEnabled(oldEnabled);
            record.setDisabledReason(oldReason);
            if (displaced != null) {
                displaced.setEnabled(displacedEnabled);
                displaced.setDisabledReason(displacedReason);
            }
            context.rollbackStore(failure);
            live.rollback(failure);
            throw failure;
        }
    }

    private void disable(KeybindRecord record, WurmConsole console, Context context)
            throws IOException, ReflectiveOperationException {
        boolean oldEnabled = record.isEnabled();
        String oldReason = record.getDisabledReason();
        String command = context.commandFor(record);
        ManagedBindTransaction live = new ManagedBindTransaction(binds, console);
        BindSnapshot liveBefore = live.capture(record.getKey());
        boolean releasesChord = liveBefore == null
                || liveBefore.getCommand().equalsIgnoreCase(command);
        for (KeybindRecord candidate : context.records())
            if (candidate != record && candidate.isEnabled()
                    && context.sameChord(candidate.getKey(), record.getKey()))
                releasesChord = false;
        List<KeybindRecord> unblocked = new ArrayList<KeybindRecord>();
        List<String> unblockedReasons = new ArrayList<String>();
        record.setEnabled(false);
        record.setDisabledReason(DisableReason.value("disabled_by_user"));
        if (releasesChord) {
            for (KeybindRecord candidate : context.records()) {
                if (candidate == record || candidate.isEnabled()
                        || !context.sameChord(candidate.getKey(), record.getKey())
                        || !DisableReason.isKeyConflict(candidate.getDisabledReason())) continue;
                unblocked.add(candidate);
                unblockedReasons.add(candidate.getDisabledReason());
                candidate.setDisabledReason(DisableReason.value("disabled_by_user"));
            }
        }
        try {
            context.save();
            if (!record.getKey().isEmpty() && !record.getKeybindSteps().isEmpty())
                live.removeOwned(record.getKey(), command);
        } catch (RuntimeException | IOException | ReflectiveOperationException failure) {
            record.setEnabled(oldEnabled);
            record.setDisabledReason(oldReason);
            for (int i = 0; i < unblocked.size(); i++)
                unblocked.get(i).setDisabledReason(unblockedReasons.get(i));
            context.rollbackStore(failure);
            live.rollback(failure);
            throw failure;
        }
    }

    public void add(KeybindRecord record, WurmConsole console, int limit, Context context)
            throws IOException, ReflectiveOperationException {
        context.stamp(record);
        context.validate(record);
        String command = context.commandFor(record);
        KeybindRecord displaced = context.findManagedConflict(record, record.getKey());
        boolean displacedEnabled = displaced != null && displaced.isEnabled();
        String displacedReason = displaced == null ? "" : displaced.getDisabledReason();
        ManagedBindTransaction live = new ManagedBindTransaction(binds, console);
        BindSnapshot liveBefore = live.capture(record.getKey());
        if (displaced != null) live.capture(displaced.getKey());
        if (displaced != null) {
            displaced.setEnabled(false);
            displaced.setDisabledReason(DisableReason.value("replaced_by", record.getName()));
        }
        context.applyLimit(record, limit);
        context.records().add(record);
        try {
            context.save();
            if (displaced != null)
                live.removeOwned(displaced.getKey(), context.commandFor(displaced));
            if (record.isEnabled()) live.install(record.getKey(), command);
        } catch (RuntimeException | IOException | ReflectiveOperationException failure) {
            context.records().remove(record);
            if (displaced != null) {
                displaced.setEnabled(displacedEnabled);
                displaced.setDisabledReason(displacedReason);
            }
            context.rollbackStore(failure);
            live.rollback(failure);
            throw failure;
        }
        context.announceConflict(liveBefore, record.getKey(), command);
        log.info(Messages.text("registry.added", record.getName(), record.getKey()));
    }

    public boolean delete(String id, WurmConsole console, Context context)
            throws IOException, ReflectiveOperationException {
        KeybindRecord found = context.find(id);
        if (found == null) return false;
        int index = context.records().indexOf(found);
        String command = context.commandFor(found);
        ManagedBindTransaction live = new ManagedBindTransaction(binds, console);
        live.capture(found.getKey());
        context.records().remove(found);
        try {
            context.save();
            live.removeOwned(found.getKey(), command);
        } catch (RuntimeException | IOException | ReflectiveOperationException failure) {
            context.records().add(index, found);
            context.rollbackStore(failure);
            live.rollback(failure);
            throw failure;
        }
        log.info(Messages.text("registry.deleted", found.getName()));
        return true;
    }
}
