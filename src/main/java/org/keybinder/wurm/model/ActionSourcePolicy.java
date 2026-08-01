package org.keybinder.wurm.model;

import com.wurmonline.shared.constants.PlayerAction;

/** Action-ID rules for whether a step can carry an item source/tool. */
public final class ActionSourcePolicy {
    private ActionSourcePolicy() { }

    public static boolean acceptsSelectableTool(short actionId) {
        return actionId != PlayerAction.TAKE.getId();
    }

    public static ItemSelector normalize(short actionId, ItemSelector requested) {
        if (!acceptsSelectableTool(actionId)) return ItemSelector.emptyHand();
        return requested == null ? ItemSelector.currentActive() : requested;
    }
}
