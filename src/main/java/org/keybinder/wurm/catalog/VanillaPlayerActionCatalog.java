package org.keybinder.wurm.catalog;

import com.wurmonline.shared.constants.PlayerAction;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Snapshot of PlayerAction instances declared by the vanilla client itself.
 *
 * <p>Unlike PlayerAction.actionIds, public static fields do not absorb actions
 * registered later by server/client mods. An ID is accepted only when all
 * vanilla fields declaring it agree on its action metadata.</p>
 */
public final class VanillaPlayerActionCatalog {
    private final Map<Short, PlayerAction> byId;

    public VanillaPlayerActionCatalog() {
        Map<Short, PlayerAction> actions = new HashMap<Short, PlayerAction>();
        Set<Short> ambiguous = new HashSet<Short>();
        for (Field field : PlayerAction.class.getFields()) {
            if (field.getType() != PlayerAction.class
                    || !Modifier.isStatic(field.getModifiers())) continue;
            try {
                PlayerAction action = (PlayerAction) field.get(null);
                if (action == null) continue;
                short id = action.getId();
                PlayerAction previous = actions.get(id);
                if (previous == null) {
                    actions.put(id, action);
                } else if (!sameMetadata(previous, action)) {
                    ambiguous.add(id);
                }
            } catch (IllegalAccessException ignored) {
                // Public fields should be readable. Fail closed for this field.
            }
        }
        for (Short id : ambiguous) actions.remove(id);
        byId = Collections.unmodifiableMap(actions);
    }

    public PlayerAction find(short actionId) {
        return byId.get(actionId);
    }

    public PlayerAction resolveOrGeneric(short actionId) {
        PlayerAction vanilla = find(actionId);
        return vanilla != null
                ? vanilla
                : new PlayerAction(actionId, PlayerAction.ANYTHING, "", false);
    }

    private static boolean sameMetadata(PlayerAction left, PlayerAction right) {
        return left.getId() == right.getId()
                && left.getTargetMask() == right.getTargetMask()
                && left.isInstant() == right.isInstant()
                && left.isAtomic() == right.isAtomic();
    }
}
