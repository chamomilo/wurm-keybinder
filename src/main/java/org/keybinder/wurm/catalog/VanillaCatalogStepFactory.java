package org.keybinder.wurm.catalog;

import org.keybinder.wurm.i18n.Messages;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.ActivateToolStep;
import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.model.ItemSelector;
import org.keybinder.wurm.model.TargetSpec;
import org.keybinder.wurm.model.VanillaActionStep;

/**
 * Converts a vanilla catalog selection into the existing Keybinder step
 * models. The catalog remains presentation data; resolved gameplay actions
 * become ordinary ActionStep instances.
 */
public final class VanillaCatalogStepFactory {
    public boolean usesStructuredAction(
            VanillaKeybindCatalog.Category category, VanillaKeybindCatalog.Entry entry) {
        return category != null && entry != null
                && !category.usesNativeCompatibility() && entry.getActionId() != null;
    }

    public boolean usesTarget(
            VanillaKeybindCatalog.Category category, VanillaKeybindCatalog.Entry entry) {
        return category != null && entry != null && !category.usesNativeCompatibility()
                && entry.usesSelectableTarget();
    }

    public KeybindStep create(
            VanillaKeybindCatalog.Category category,
            VanillaKeybindCatalog.Entry entry,
            TargetSpec selectedTarget) {
        return create(category, entry, ItemSelector.currentActive(), selectedTarget);
    }

    public KeybindStep create(
            VanillaKeybindCatalog.Category category,
            VanillaKeybindCatalog.Entry entry,
            ItemSelector selectedSource,
            TargetSpec selectedTarget) {
        if (entry == null)
            throw new IllegalArgumentException(Messages.text("validation.vanilla_missing"));
        if (category == null || category.usesNativeCompatibility()
                || entry.getActionId() == null && !entry.isActivateTool())
            return new VanillaActionStep(entry.getCommand());
        if (usesTarget(category, entry) && selectedTarget == null)
            throw new IllegalArgumentException(Messages.text("validation.target_missing"));
        if (entry.isActivateTool())
            return new ActivateToolStep(selectedTarget);
        return new ActionStep(entry.getActionId(),
                selectedSource == null ? ItemSelector.currentActive() : selectedSource,
                usesTarget(category, entry) ? selectedTarget : TargetSpec.tile(0, 0),
                entry.getDisplayName());
    }
}
