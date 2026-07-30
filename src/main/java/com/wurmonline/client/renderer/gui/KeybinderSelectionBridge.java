package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.PickableUnit;

/** Package-access bridge for performing the same SelectBar transition as a user selection. */
public final class KeybinderSelectionBridge {
    private KeybinderSelectionBridge() {}

    public static void select(SelectBar bar, PickableUnit unit) {
        bar.setSelected(unit);
    }
}
