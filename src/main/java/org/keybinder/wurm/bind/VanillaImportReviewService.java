package org.keybinder.wurm.bind;

import org.keybinder.wurm.catalog.InputKeyCatalog;
import org.keybinder.wurm.catalog.VanillaKeybindCatalog;
import org.keybinder.wurm.migration.CustomActionsImporter;
import org.keybinder.wurm.migration.ImprovedImproveImporter;
import org.keybinder.wurm.model.KeybindRecord;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Builds a deterministic, non-mutating review of vanilla import candidates. */
public final class VanillaImportReviewService {
    private final CustomActionsImporter customActions;
    private final ImprovedImproveImporter improvedImprove;
    private final VanillaKeybindCatalog vanillaCatalog;

    public VanillaImportReviewService() {
        this(new CustomActionsImporter(), new ImprovedImproveImporter(),
                new VanillaKeybindCatalog());
    }

    VanillaImportReviewService(CustomActionsImporter customActions,
                               ImprovedImproveImporter improvedImprove,
                               VanillaKeybindCatalog vanillaCatalog) {
        this.customActions = customActions;
        this.improvedImprove = improvedImprove;
        this.vanillaCatalog = vanillaCatalog;
    }

    public List<VanillaImportCandidate> review(List<BindSnapshot> bindings,
                                                List<KeybindRecord> records) {
        if (bindings == null || bindings.isEmpty()) return Collections.emptyList();
        List<KeybindRecord> managed = records == null
                ? Collections.<KeybindRecord>emptyList() : records;
        List<VanillaImportCandidate> result = new ArrayList<VanillaImportCandidate>();
        for (BindSnapshot binding : bindings) result.add(inspect(binding, managed));
        return Collections.unmodifiableList(result);
    }

    private VanillaImportCandidate inspect(BindSnapshot binding,
                                             List<KeybindRecord> records) {
        String conflict = conflictingRecord(binding.getKey(), records);
        VanillaImportCandidate.Type type = typeOf(binding.getCommand());
        String invalid = validationError(binding.getCommand(), type);
        VanillaImportCandidate.Status status;
        String detail;
        if (!invalid.isEmpty()) {
            status = VanillaImportCandidate.Status.INVALID;
            detail = invalid;
        } else if (!conflict.isEmpty()) {
            status = VanillaImportCandidate.Status.CONFLICT;
            detail = conflict;
        } else if (type == VanillaImportCandidate.Type.RAW_VANILLA_COMMAND) {
            status = VanillaImportCandidate.Status.NEEDS_REVIEW;
            detail = "";
        } else {
            status = VanillaImportCandidate.Status.READY;
            detail = "";
        }
        return new VanillaImportCandidate(binding, type, status, detail,
                status == VanillaImportCandidate.Status.READY);
    }

    private VanillaImportCandidate.Type typeOf(String command) {
        if (customActions.supports(command))
            return VanillaImportCandidate.Type.ACTION_CHAIN;
        if (improvedImprove.supports(command))
            return VanillaImportCandidate.Type.SMART_IMPROVE;
        return findVanilla(command) == null
                ? VanillaImportCandidate.Type.RAW_VANILLA_COMMAND
                : VanillaImportCandidate.Type.VANILLA_COMMAND;
    }

    private String validationError(String command, VanillaImportCandidate.Type type) {
        try {
            if (type == VanillaImportCandidate.Type.ACTION_CHAIN)
                customActions.importCommand(command);
            else if (type == VanillaImportCandidate.Type.SMART_IMPROVE)
                improvedImprove.importCommand(command);
            return "";
        } catch (IllegalArgumentException invalid) {
            String message = invalid.getMessage();
            return message == null || message.trim().isEmpty()
                    ? invalid.getClass().getSimpleName() : message;
        }
    }

    private VanillaKeybindCatalog.Entry findVanilla(String command) {
        VanillaKeybindCatalog.Entry entry = vanillaCatalog.find(command);
        String unquoted = VanillaImportPolicy.unquote(command);
        if (entry == null) entry = vanillaCatalog.find(unquoted);
        if (entry == null && !unquoted.isEmpty())
            entry = vanillaCatalog.find("\"" + unquoted + "\"");
        return entry;
    }

    private static String conflictingRecord(String key, List<KeybindRecord> records) {
        String normalized = InputKeyCatalog.normalizeChord(key);
        for (KeybindRecord record : records) {
            if (normalized.equals(InputKeyCatalog.normalizeChord(record.getKey())))
                return record.getDisplayName();
        }
        return "";
    }
}
