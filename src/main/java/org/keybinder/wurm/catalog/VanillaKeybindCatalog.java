package org.keybinder.wurm.catalog;

import com.wurmonline.client.options.keybinding.PlayerKeybind;
import com.wurmonline.client.options.keybinding.PlayerKeybindCategory;
import com.wurmonline.shared.constants.PlayerAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Defensive, ordered snapshot of the vanilla client's own keybind catalog.
 * No command names or category membership are duplicated in Keybinder.
 */
public final class VanillaKeybindCatalog {
    private static final Logger LOGGER = Logger.getLogger("Chamomilo.Keybinder");

    public static final class Category {
        private final String id;
        private final String displayName;
        private final List<Entry> entries;

        private Category(String id, String displayName, List<Entry> entries) {
            this.id = id;
            this.displayName = displayName;
            this.entries = Collections.unmodifiableList(new ArrayList<Entry>(entries));
        }

        public String getId() { return id; }
        public String getDisplayName() { return displayName; }
        public List<Entry> getEntries() { return entries; }
        public boolean usesNativeCompatibility() {
            return PlayerKeybindCategory.HUD.name().equals(id)
                    || PlayerKeybindCategory.MOVEMENT.name().equals(id);
        }
    }

    public static final class Entry {
        private final String displayName;
        private final String command;
        private final Short actionId;
        private final boolean activateTool;
        private final boolean selectableTool;
        private final boolean selectableTarget;

        private Entry(String displayName, String command, Short actionId,
                      boolean activateTool, boolean selectableTool,
                      boolean selectableTarget) {
            this.displayName = displayName;
            this.command = command;
            this.actionId = actionId;
            this.activateTool = activateTool;
            this.selectableTool = selectableTool;
            this.selectableTarget = selectableTarget;
        }

        public String getDisplayName() { return displayName; }
        public String getCommand() { return command; }
        public Short getActionId() { return actionId; }
        public boolean isActivateTool() { return activateTool; }
        public boolean usesSelectableTool() { return selectableTool; }
        public boolean usesSelectableTarget() { return selectableTarget; }
    }

    private final List<Category> categories;
    private final Map<String, Entry> byCommand;

    public VanillaKeybindCatalog() {
        this(currentActionIdsByBind());
    }

    VanillaKeybindCatalog(Map<String, Short> actionIdsByBind) {
        Map<PlayerKeybindCategory, List<Entry>> grouped =
                new LinkedHashMap<PlayerKeybindCategory, List<Entry>>();
        for (PlayerKeybindCategory category : PlayerKeybindCategory.values()) {
            if (category.getIsVisible()) grouped.put(category, new ArrayList<Entry>());
        }
        Map<String, Entry> commands = new LinkedHashMap<String, Entry>();
        for (PlayerKeybind keybind : PlayerKeybind.values()) {
            List<Entry> entries = grouped.get(keybind.getCategory());
            if (entries == null) continue;
            String command = keybind.getCommand();
            if (command == null || command.trim().isEmpty()) continue;
            String actionBind = actionBindFor(keybind);
            Short actionId = actionBind == null
                    ? null : actionIdsByBind.get(normalize(actionBind));
            boolean activateTool = keybind == PlayerKeybind.ACTIVATE;
            Entry entry = new Entry(keybind.getDisplayName(), command, actionId,
                    activateTool, actionId != null && usesItemSource(keybind),
                    activateTool || actionId != null && usesTarget(keybind));
            entries.add(entry);
            commands.put(normalize(command), entry);
        }
        List<Category> snapshot = new ArrayList<Category>();
        for (Map.Entry<PlayerKeybindCategory, List<Entry>> group : grouped.entrySet()) {
            if (!group.getValue().isEmpty()) {
                snapshot.add(new Category(group.getKey().name(), group.getKey().getName(),
                        group.getValue()));
            }
        }
        categories = Collections.unmodifiableList(snapshot);
        byCommand = Collections.unmodifiableMap(commands);
    }

    public List<Category> getCategories() {
        return categories;
    }

    public Entry find(String command) {
        if (command == null) return null;
        return byCommand.get(normalize(command));
    }

    public Category categoryFor(String command) {
        Entry entry = find(command);
        if (entry == null) return null;
        for (Category category : categories)
            if (category.getEntries().contains(entry)) return category;
        return null;
    }

    /** Returns the audited item-source capability for a known vanilla action.
     * Unknown server/mod actions remain source-capable. */
    public boolean usesSelectableTool(short actionId) {
        for (Category category : categories) {
            for (Entry entry : category.getEntries())
                if (entry.getActionId() != null
                        && entry.getActionId().shortValue() == actionId
                        && entry.usesSelectableTool()) return true;
        }
        for (Category category : categories)
            for (Entry entry : category.getEntries())
                if (entry.getActionId() != null
                        && entry.getActionId().shortValue() == actionId) return false;
        return true;
    }

    /** Returns whether a known vanilla action requires a user-selected target.
     * Unknown server/mod actions retain the normal target editor. */
    public boolean usesSelectableTarget(short actionId) {
        for (Category category : categories) {
            for (Entry entry : category.getEntries())
                if (entry.getActionId() != null
                        && entry.getActionId().shortValue() == actionId
                        && entry.usesSelectableTarget()) return true;
        }
        for (Category category : categories)
            for (Entry entry : category.getEntries())
                if (entry.getActionId() != null
                        && entry.getActionId().shortValue() == actionId) return false;
        return true;
    }

    /**
     * Full audit of the visible current-client catalog. Most vanilla commands
     * use the hovered object only and must send an empty item source. The
     * entries below are the commands whose server action actually consumes an
     * activated tool, material, healing item, fishing implement, bow, or
     * statuette.
     */
    private static boolean usesItemSource(PlayerKeybind keybind) {
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

    /** Stop and No target are the two visible mapped actions dispatched by
     * PlayerObj.toggleKey through World.sendLocalAction rather than a selected
     * hovered target. HUD, movement, and unmapped local commands never reach
     * this method because they remain native compatibility steps. */
    private static boolean usesTarget(PlayerKeybind keybind) {
        return keybind != PlayerKeybind.STOP && keybind != PlayerKeybind.NO_TARGET;
    }

    static Map<String, Short> uniqueActionIds(Map<String, List<Short>> candidates) {
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

    private static Map<String, Short> currentActionIdsByBind() {
        Map<String, List<Short>> candidates = new LinkedHashMap<String, List<Short>>();
        try {
            for (PlayerAction action : new PlayerActionCatalog().snapshot()) {
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
                            + "compatibility mode will be used", e);
        }
        return uniqueActionIds(candidates);
    }

    /**
     * Uses the same command-to-action aliases as this client build's
     * PlayerObj.toggleKey switch. The values remain live PlayerAction binds,
     * never display names or copied numeric IDs.
     */
    private static String actionBindFor(PlayerKeybind keybind) {
        if (keybind == PlayerKeybind.PLANT_SIGN) return PlayerAction.PLANT_SIGN.getBind();
        if (keybind == PlayerKeybind.SIT) return PlayerAction.SIT_ANY.getBind();
        if (keybind == PlayerKeybind.PICK_FLOWERS) return PlayerAction.PICK_SPROUT.getBind();
        if (keybind == PlayerKeybind.MINE_SURFACE) return PlayerAction.MINE_FORWARD.getBind();
        if (keybind == PlayerKeybind.FUNGUS_SPELL) return PlayerAction.FUNGUS.getBind();
        return keybind == PlayerKeybind.ACTIVATE ? null : keybind.getCommand();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ENGLISH);
    }
}
