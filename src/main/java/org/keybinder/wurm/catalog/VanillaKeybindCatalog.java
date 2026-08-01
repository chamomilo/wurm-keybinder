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

        private Entry(String displayName, String command, Short actionId,
                      boolean activateTool) {
            this.displayName = displayName;
            this.command = command;
            this.actionId = actionId;
            this.activateTool = activateTool;
        }

        public String getDisplayName() { return displayName; }
        public String getCommand() { return command; }
        public Short getActionId() { return actionId; }
        public boolean isActivateTool() { return activateTool; }
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
            Entry entry = new Entry(keybind.getDisplayName(), command,
                    actionBind == null ? null : actionIdsByBind.get(normalize(actionBind)),
                    keybind == PlayerKeybind.ACTIVATE);
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
