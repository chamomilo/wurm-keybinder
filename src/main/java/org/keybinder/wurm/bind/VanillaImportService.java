package org.keybinder.wurm.bind;

import com.wurmonline.client.console.WurmConsole;
import org.keybinder.wurm.event.EventLogger;
import org.keybinder.wurm.i18n.Messages;
import org.keybinder.wurm.migration.CustomActionsImporter;
import org.keybinder.wurm.migration.ImprovedImproveImporter;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.model.RecordType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Candidate discovery and per-entry transactional vanilla import. */
public final class VanillaImportService {
    public interface Context {
        List<KeybindRecord> records();
        String commandFor(KeybindRecord record);
        void stamp(KeybindRecord record);
        void applyLimit(KeybindRecord record, int limit);
        void save() throws IOException;
    }

    private final ManagedBindAccess binds;
    private final CustomActionsImporter customActions;
    private final ImprovedImproveImporter improvedImprove = new ImprovedImproveImporter();
    private final VanillaImportPolicy policy = new VanillaImportPolicy();
    private final VanillaImportReviewService review = new VanillaImportReviewService();
    private final EventLogger log;

    public VanillaImportService(ManagedBindAccess binds,
                                CustomActionsImporter customActions,
                                EventLogger log) {
        this.binds = binds;
        this.customActions = customActions;
        this.log = log;
    }

    public List<BindSnapshot> candidates(WurmConsole console, Context context)
            throws ReflectiveOperationException, IOException {
        Map<String, String> defaults = new DefaultBindCatalog().load();
        List<BindSnapshot> result = new ArrayList<BindSnapshot>();
        for (BindSnapshot bind : binds.snapshot(console)) {
            if (!policy.mayImport(bind.getCommand())) continue;
            String defaultCommand = defaults.get(DefaultBindCatalog.normalize(bind.getKey()));
            if (defaultCommand != null && defaultCommand.equalsIgnoreCase(bind.getCommand()))
                continue;
            boolean managed = false;
            for (KeybindRecord record : context.records()) {
                if (record.getKey().equalsIgnoreCase(bind.getKey())
                        && context.commandFor(record).equalsIgnoreCase(bind.getCommand())) {
                    managed = true;
                    break;
                }
            }
            if (!managed) result.add(bind);
        }
        return result;
    }

    public int importSelected(WurmConsole console, List<BindSnapshot> selected,
                              int limit, Context context)
            throws ReflectiveOperationException, IOException {
        List<BindSnapshot> candidates = selected == null
                ? Collections.<BindSnapshot>emptyList()
                : new ArrayList<BindSnapshot>(selected);
        int imported = 0;
        for (BindSnapshot candidate : candidates) {
            BindSnapshot current = binds.findByKey(console, candidate.getKey());
            if (current == null
                    || !current.getCommand().equalsIgnoreCase(candidate.getCommand())) continue;
            List<VanillaImportCandidate> inspectedRows = review.review(
                    Collections.singletonList(candidate), context.records());
            if (inspectedRows.isEmpty() || !inspectedRows.get(0).isImportable()) continue;
            KeybindRecord record = record(candidate, inspectedRows.get(0), limit, context);
            context.records().add(record);
            try {
                context.save();
                if (!binds.removeIfOwned(console, candidate.getKey(), candidate.getCommand()))
                    throw new IllegalStateException(Messages.text("error.import_binding_changed"));
                if (record.isEnabled())
                    binds.install(console, record.getKey(), context.commandFor(record));
                imported++;
                log.info(Messages.text("registry.imported",
                        candidate.getKey(), candidate.getCommand()));
            } catch (RuntimeException | IOException | ReflectiveOperationException failure) {
                context.records().remove(record);
                context.save();
                BindSnapshot after = binds.findByKey(console, candidate.getKey());
                if (after == null)
                    binds.install(console, candidate.getKey(), candidate.getCommand());
                log.error(Messages.text("registry.import_failed", candidate.getKey()), failure);
            }
        }
        return imported;
    }

    private KeybindRecord record(BindSnapshot candidate, VanillaImportCandidate inspected,
                                 int limit, Context context) {
        String name = Messages.text("registry.imported_name", candidate.getKey());
        KeybindRecord record;
        if (inspected.getType() == VanillaImportCandidate.Type.ACTION_CHAIN) {
            record = new KeybindRecord(null, name, candidate.getKey(),
                    customActions.importCommand(candidate.getCommand()));
            context.applyLimit(record, limit);
        } else if (inspected.getType() == VanillaImportCandidate.Type.SMART_IMPROVE) {
            record = new KeybindRecord(null, name, candidate.getKey(),
                    improvedImprove.importCommand(candidate.getCommand()));
            context.applyLimit(record, limit);
        } else {
            RecordType type = inspected.getType() == VanillaImportCandidate.Type.VANILLA_COMMAND
                    ? RecordType.VANILLA_COMMAND : RecordType.RAW_VANILLA_COMMAND;
            record = new KeybindRecord(null, name, candidate.getKey(), type,
                    Collections.<ActionStep>emptyList(), candidate.getCommand());
        }
        record.setOriginalKey(candidate.getKey());
        record.setOriginalCommand(candidate.getCommand());
        context.stamp(record);
        return record;
    }
}
