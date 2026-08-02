package org.keybinder.wurm.model;

import org.keybinder.wurm.catalog.VanillaKeybindCatalog;

/** Action-ID rules for whether a step exposes a user-selected target. */
public final class ActionTargetPolicy {
    private static final VanillaKeybindCatalog VANILLA_KEYBINDS =
            new VanillaKeybindCatalog();

    private ActionTargetPolicy() { }

    public static boolean acceptsSelectableTarget(short actionId) {
        return VANILLA_KEYBINDS.usesSelectableTarget(actionId);
    }

    /**
     * Target-free server actions still need a protocol target ID. The vanilla
     * client dispatches them through World.sendLocalAction, which uses the
     * player's current tile. Keeping that tile as an internal implementation
     * detail lets the editor omit a meaningless Target field.
     */
    public static TargetSpec normalize(short actionId, TargetSpec requested) {
        if (!acceptsSelectableTarget(actionId)) return TargetSpec.tile(0, 0);
        return requested;
    }
}
