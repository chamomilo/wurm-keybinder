package org.keybinder.wurm.model;

import org.keybinder.wurm.policy.VanillaActionPolicy;

/** Action-ID rules for whether a step can carry an item source/tool. */
public final class ActionSourcePolicy {
    private ActionSourcePolicy() { }

    public static boolean acceptsSelectableTool(short actionId) {
        return VanillaActionPolicy.acceptsSelectableTool(actionId);
    }

    public static ItemSelector normalize(short actionId, ItemSelector requested) {
        if (!acceptsSelectableTool(actionId)) return ItemSelector.emptyHand();
        return requested == null ? ItemSelector.currentActive() : requested;
    }
}
