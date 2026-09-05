package org.keybinder.wurm.catalog;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Optional catalog supplied by WU-third_person_view through the shared mod
 * class loader. Keybinder intentionally has no compile-time dependency on the
 * camera mod, so the editor category disappears when that mod is unavailable.
 */
public final class ThirdPersonViewCommandCatalog {
    static final String PROVIDER_CLASS =
            "org.wuthirdpersonview.client.WUThirdPersonViewMod";
    static final String PROVIDER_METHOD = "getKeybinderCommandCatalog";
    public static final String DISPLAY_NAME = "3rd Person View";

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

    private final List<Entry> entries;
    private final Map<String, Entry> byCommand;

    private ThirdPersonViewCommandCatalog(List<Entry> entries,
                                          Map<String, Entry> byCommand) {
        this.entries = Collections.unmodifiableList(new ArrayList<Entry>(entries));
        this.byCommand = Collections.unmodifiableMap(
                new LinkedHashMap<String, Entry>(byCommand));
    }

    public static ThirdPersonViewCommandCatalog detect() {
        ClassLoader ownLoader = ThirdPersonViewCommandCatalog.class.getClassLoader();
        ThirdPersonViewCommandCatalog catalog = load(PROVIDER_CLASS, ownLoader);
        if (catalog.isVisible()) return catalog;
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        return contextLoader == ownLoader ? catalog : load(PROVIDER_CLASS, contextLoader);
    }

    static ThirdPersonViewCommandCatalog load(String providerClass, ClassLoader loader) {
        try {
            Class<?> provider = Class.forName(providerClass, false, loader);
            Method method = provider.getMethod(PROVIDER_METHOD);
            if (!Modifier.isStatic(method.getModifiers())
                    || method.getReturnType() != String[][].class) return empty();
            return fromRows((String[][]) method.invoke(null));
        } catch (Exception unavailable) {
            return empty();
        } catch (LinkageError unavailable) {
            return empty();
        }
    }

    static ThirdPersonViewCommandCatalog fromRows(String[][] rows) {
        if (rows == null) return empty();
        List<Entry> entries = new ArrayList<Entry>();
        Map<String, Entry> commands = new LinkedHashMap<String, Entry>();
        for (String[] row : rows) {
            if (row == null || row.length != 2) continue;
            String name = row[0] == null ? "" : row[0].trim();
            String command = row[1] == null ? "" : row[1].trim();
            String normalized = normalize(command);
            if (name.isEmpty() || !isCompleteCommand(command)
                    || commands.containsKey(normalized)) continue;
            Entry entry = new Entry(name, command);
            entries.add(entry);
            commands.put(normalized, entry);
        }
        return new ThirdPersonViewCommandCatalog(entries, commands);
    }

    public boolean isVisible() { return !entries.isEmpty(); }
    public List<Entry> getEntries() { return entries; }

    public Entry find(String command) {
        if (command == null) return null;
        return byCommand.get(normalize(command));
    }

    private static ThirdPersonViewCommandCatalog empty() {
        return new ThirdPersonViewCommandCatalog(
                Collections.<Entry>emptyList(),
                Collections.<String, Entry>emptyMap());
    }

    private static boolean isCompleteCommand(String command) {
        if (!normalize(command).startsWith("TP ")) return false;
        return command.indexOf('<') < 0 && command.indexOf('>') < 0
                && command.indexOf('{') < 0 && command.indexOf('}') < 0
                && command.indexOf('\n') < 0 && command.indexOf('\r') < 0;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ENGLISH);
    }
}
