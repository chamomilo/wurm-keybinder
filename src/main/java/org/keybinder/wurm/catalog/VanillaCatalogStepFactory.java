package org.keybinder.wurm.catalog;

import org.keybinder.wurm.i18n.Messages;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.ActivateToolStep;
import org.keybinder.wurm.model.KeybindStep;
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
                && (entry.getActionId() != null || entry.isActivateTool());
    }

    public KeybindStep create(
            VanillaKeybindCatalog.Category category,
            VanillaKeybindCatalog.Entry entry,
            TargetSpec selectedTarget) {
        if (entry == null)
            throw new IllegalArgumentException(Messages.text("validation.vanilla_missing"));
        if (!usesTarget(category, entry))
            return new VanillaActionStep(entry.getCommand());
        if (selectedTarget == null)
            throw new IllegalArgumentException(Messages.text("validation.target_missing"));
        if (entry.isActivateTool())
            return new ActivateToolStep(selectedTarget);
        return new ActionStep(entry.getActionId(), selectedTarget, entry.getDisplayName());
    }
}
