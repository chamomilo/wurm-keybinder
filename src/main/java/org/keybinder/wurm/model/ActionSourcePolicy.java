package org.keybinder.wurm.model;

import org.keybinder.wurm.catalog.VanillaKeybindCatalog;

/** Action-ID rules for whether a step can carry an item source/tool. */
public final class ActionSourcePolicy {
    private static final VanillaKeybindCatalog VANILLA_KEYBINDS =
            new VanillaKeybindCatalog();

    private ActionSourcePolicy() { }

    public static boolean acceptsSelectableTool(short actionId) {
        return VANILLA_KEYBINDS.usesSelectableTool(actionId);
    }

    public static ItemSelector normalize(short actionId, ItemSelector requested) {
        if (!acceptsSelectableTool(actionId)) return ItemSelector.emptyHand();
        return requested == null ? ItemSelector.currentActive() : requested;
    }
}
