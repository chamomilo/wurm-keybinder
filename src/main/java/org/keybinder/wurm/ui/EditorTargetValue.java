package org.keybinder.wurm.ui;

import org.keybinder.wurm.command.ExactObjectTarget;
import org.keybinder.wurm.command.NearbyTypeTarget;

/** Identifies editor target tokens that carry a resolved parameter or capture. */
public final class EditorTargetValue {
    private EditorTargetValue() {}

    public static boolean isConcrete(String target) {
        return target != null && (target.startsWith("@tb") || target.startsWith("@eq")
                || target.startsWith("@nearby")
                || ExactObjectTarget.isExact(target)
                || NearbyTypeTarget.isNearbyType(target)
                || target.startsWith("hover-type ")
                || target.equals("tile") || target.startsWith("tile_")
                || target.equals("area"));
    }
}
