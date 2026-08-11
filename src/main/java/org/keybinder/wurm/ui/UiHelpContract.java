package org.keybinder.wurm.ui;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Required localized help/hover entries for schema-8 interactive controls. */
public final class UiHelpContract {
    private static final List<String> KEYS = Collections.unmodifiableList(Arrays.asList(
            "keybind.duplicate.tip", "keybind.row.tip",
            "list.import.tip", "list.import_file.tip", "list.export_all.tip",
            "editor.hud_action.tip", "editor.hud_action.help",
            "help.source.current_active", "help.source.empty_hand",
            "help.source.hovered_item", "help.source.toolbelt_slot",
            "help.source.equipment_slot", "help.source.exact_object",
            "help.source.inventory_filter",
            "editor.help.activate", "selection.hover_type", "selection.nearby_type",
            "editor.variant.extract", "merge.confirm.tip", "merge.cancel.tip",
            "reason.duplicate_review", "reason.extracted_review",
            "reason.nonportable_object_review"));

    private UiHelpContract() {}

    public static List<String> requiredMessageKeys() { return KEYS; }
}
