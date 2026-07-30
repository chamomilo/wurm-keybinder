package org.keybinder.wurm.catalog;

import com.wurmonline.client.options.keybinding.PlayerKeybind;
import com.wurmonline.client.options.keybinding.PlayerKeybindCategory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Defensive, ordered snapshot of the vanilla client's own keybind catalog.
 * No command names or category membership are duplicated in Keybinder.
 */
public final class VanillaKeybindCatalog {
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
    }

    public static final class Entry {
        private final String displayName;
        private final String command;

        private Entry(String displayName, String command) {
            this.displayName = displayName;
            this.command = command;
        }

        public String getDisplayName() { return displayName; }
        public String getCommand() { return command; }
    }

    private final List<Category> categories;
    private final Map<String, Entry> byCommand;

    public VanillaKeybindCatalog() {
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
            Entry entry = new Entry(keybind.getDisplayName(), command);
            entries.add(entry);
            commands.put(command.toUpperCase(java.util.Locale.ENGLISH), entry);
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
        return byCommand.get(command.trim().toUpperCase(java.util.Locale.ENGLISH));
    }

    public Category categoryFor(String command) {
        Entry entry = find(command);
        if (entry == null) return null;
        for (Category category : categories)
            if (category.getEntries().contains(entry)) return category;
        return null;
    }
}
