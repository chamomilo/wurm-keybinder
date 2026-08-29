package org.keybinder.wurm.policy;

import com.wurmonline.client.options.keybinding.PlayerKeybind;
import com.wurmonline.client.options.keybinding.PlayerKeybindCategory;
import com.wurmonline.shared.constants.PlayerAction;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Audited source and target capabilities of the current client's vanilla
 * keybind actions. This package has no dependency on Keybinder's model or UI,
 * so both layers can consume the same snapshot without a dependency cycle.
 */
public final class VanillaActionPolicy {
    /** Confirmed current-client Drink action; it consumes its target, not a tool. */
    private static final short DRINK_ACTION_ID = 183;
    /** Server Actions.OPEN_INVENTORY_CONTAINER; opens only its target. */
    private static final short OPEN_INVENTORY_CONTAINER_ACTION_ID = 568;
    private static final Logger LOGGER = Logger.getLogger("Chamomilo.Keybinder");
    private static final Map<String, Short> ACTION_IDS_BY_BIND = loadActionIdsByBind();
    private static final Map<Short, Capabilities> CAPABILITIES =
            loadCapabilities(ACTION_IDS_BY_BIND);

    private VanillaActionPolicy() { }

    /** Unknown server/mod actions remain fully configurable. */
    public static boolean acceptsSelectableTool(short actionId) {
        if (actionId == DRINK_ACTION_ID
                || actionId == OPEN_INVENTORY_CONTAINER_ACTION_ID
                || actionId == PlayerAction.EMBARK_DRIVER.getId()
                || actionId == PlayerAction.EMBARK_PASSENGER.getId()
                || actionId == PlayerAction.DISEMBARK.getId()) return false;
        Capabilities capabilities = CAPABILITIES.get(actionId);
        return capabilities == null || capabilities.selectableTool;
    }

    /** Unknown server/mod actions retain the ordinary target editor. */
    public static boolean acceptsSelectableTarget(short actionId) {
        Capabilities capabilities = CAPABILITIES.get(actionId);
        return capabilities == null || capabilities.selectableTarget;
    }

    public static boolean usesItemSource(PlayerKeybind keybind) {
        PlayerKeybindCategory category = keybind.getCategory();
        if (category == PlayerKeybindCategory.SPELLS
                || category == PlayerKeybindCategory.TERRAFORM) return true;
        if (category == PlayerKeybindCategory.ACTION) {
            return keybind == PlayerKeybind.DEFAULT_TERRAFORM_ACTION
                    || keybind == PlayerKeybind.FIRSTAID
                    || keybind == PlayerKeybind.TREAT
                    || keybind == PlayerKeybind.FISH
                    || keybind == PlayerKeybind.FILET
                    || keybind == PlayerKeybind.INVESTIGATE;
        }
        if (category == PlayerKeybindCategory.CRAFTING) {
            return keybind == PlayerKeybind.IMPROVE
                    || keybind == PlayerKeybind.CONTINUE
                    || keybind == PlayerKeybind.FINISH
                    || keybind == PlayerKeybind.PLAN_BUILDING;
        }
        if (category == PlayerKeybindCategory.ITEM) {
            return keybind == PlayerKeybind.COMBINE
                    || keybind == PlayerKeybind.LOCK
                    || keybind == PlayerKeybind.UNLOCK
                    || keybind == PlayerKeybind.MEDITATE
                    || keybind == PlayerKeybind.IDENTIFY_FRAGMENT
                    || keybind == PlayerKeybind.COMBINE_FRAGMENT;
        }
        if (category == PlayerKeybindCategory.CREATURE) {
            return keybind == PlayerKeybind.LEAD
                    || keybind == PlayerKeybind.TAME
                    || keybind == PlayerKeybind.FEED
                    || keybind == PlayerKeybind.GROOM
                    || keybind == PlayerKeybind.BUTCHER
                    || keybind == PlayerKeybind.BURY
                    || keybind == PlayerKeybind.BURY_ALL
                    || keybind == PlayerKeybind.SHEAR
                    || keybind == PlayerKeybind.MILK;
        }
        if (category == PlayerKeybindCategory.NATURE) {
            return keybind == PlayerKeybind.HARVEST
                    || keybind == PlayerKeybind.FARM
                    || keybind == PlayerKeybind.SOW
                    || keybind == PlayerKeybind.CULTIVATE
                    || keybind == PlayerKeybind.PICK_SPROUT
                    || keybind == PlayerKeybind.PICK_FLOWERS
                    || keybind == PlayerKeybind.PRUNE
                    || keybind == PlayerKeybind.CUT_DOWN
                    || keybind == PlayerKeybind.CHOP_UP
                    || keybind == PlayerKeybind.PLANT
                    || keybind == PlayerKeybind.PLANT_CENTER
                    || keybind == PlayerKeybind.GATHER
                    || keybind == PlayerKeybind.TRIM;
        }
        if (category == PlayerKeybindCategory.FIGHTING) {
            return keybind == PlayerKeybind.SHOOT
                    || keybind == PlayerKeybind.QUICK_SHOT
                    || keybind == PlayerKeybind.SHOOT_FACE
                    || keybind == PlayerKeybind.SHOOT_TORSO
                    || keybind == PlayerKeybind.SHOOT_LEFTARM
                    || keybind == PlayerKeybind.SHOOT_RIGHTARM
                    || keybind == PlayerKeybind.SHOOT_LEGS;
        }
        return false;
    }

    public static boolean usesTarget(PlayerKeybind keybind) {
        return keybind != PlayerKeybind.STOP && keybind != PlayerKeybind.NO_TARGET;
    }

    public static Map<String, Short> currentActionIdsByBind() {
        return ACTION_IDS_BY_BIND;
    }

    public static Map<String, Short> uniqueActionIds(Map<String, List<Short>> candidates) {
        Map<String, Short> result = new LinkedHashMap<String, Short>();
        for (Map.Entry<String, List<Short>> candidate : candidates.entrySet()) {
            Short unique = null;
            boolean ambiguous = false;
            for (Short actionId : candidate.getValue()) {
                if (actionId == null) continue;
                if (unique == null) unique = actionId;
                else if (!unique.equals(actionId)) {
                    ambiguous = true;
                    break;
                }
            }
            if (!ambiguous && unique != null) result.put(normalize(candidate.getKey()), unique);
        }
        return result;
    }

    public static String actionBindFor(PlayerKeybind keybind) {
        if (keybind == PlayerKeybind.PLANT_SIGN) return PlayerAction.PLANT_SIGN.getBind();
        if (keybind == PlayerKeybind.SIT) return PlayerAction.SIT_ANY.getBind();
        if (keybind == PlayerKeybind.PICK_FLOWERS) return PlayerAction.PICK_SPROUT.getBind();
        if (keybind == PlayerKeybind.MINE_SURFACE) return PlayerAction.MINE_FORWARD.getBind();
        if (keybind == PlayerKeybind.FUNGUS_SPELL) return PlayerAction.FUNGUS.getBind();
        return keybind == PlayerKeybind.ACTIVATE ? null : keybind.getCommand();
    }

    private static Map<String, Short> loadActionIdsByBind() {
        Map<String, List<Short>> candidates = new LinkedHashMap<String, List<Short>>();
        try {
            Field field = PlayerAction.class.getDeclaredField("actionIds");
            @SuppressWarnings("unchecked")
            Map<Short, PlayerAction> source = ReflectionUtil.getPrivateField(null, field);
            for (PlayerAction action : new LinkedHashMap<Short, PlayerAction>(source).values()) {
                if (action == null || action.getBind() == null
                        || action.getBind().trim().isEmpty()) continue;
                String bind = normalize(action.getBind());
                List<Short> ids = candidates.get(bind);
                if (ids == null) {
                    ids = new ArrayList<Short>();
                    candidates.put(bind, ids);
                }
                ids.add(action.getId());
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING,
                    "Unable to match vanilla keybind commands to PlayerAction IDs; "
                            + "capabilities will fail open", e);
        }
        return Collections.unmodifiableMap(uniqueActionIds(candidates));
    }

    private static Map<Short, Capabilities> loadCapabilities(Map<String, Short> actionIdsByBind) {
        Map<Short, Capabilities> result = new LinkedHashMap<Short, Capabilities>();
        for (PlayerKeybind keybind : PlayerKeybind.values()) {
            String bind = actionBindFor(keybind);
            Short actionId = bind == null ? null : actionIdsByBind.get(normalize(bind));
            if (actionId == null) continue;
            Capabilities previous = result.get(actionId);
            boolean source = usesItemSource(keybind)
                    || previous != null && previous.selectableTool;
            boolean target = usesTarget(keybind)
                    || previous != null && previous.selectableTarget;
            result.put(actionId, new Capabilities(source, target));
        }
        return Collections.unmodifiableMap(result);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ENGLISH);
    }

    private static final class Capabilities {
        private final boolean selectableTool;
        private final boolean selectableTarget;

        private Capabilities(boolean selectableTool, boolean selectableTarget) {
            this.selectableTool = selectableTool;
            this.selectableTarget = selectableTarget;
        }
    }
}
