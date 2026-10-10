package org.keybinder.wurm.catalog;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A complete-command catalog exported by another client mod. Providers expose
 * a public static {@code String[][] getKeybinderCommandCatalog()} method and
 * therefore need no compile-time dependency on Keybinder.
 */
public final class ModCommandCatalog {
    public static final String PROVIDER_METHOD = "getKeybinderCommandCatalog";
    private static final Pattern COMMAND = Pattern.compile(
            "^[A-Za-z][A-Za-z0-9_-]*(?:[ \\t]+[^\\r\\n]+)?$");

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

    private final String providerClassName;
    private final String displayName;
    private final List<Entry> entries;
    private final Map<String, Entry> byCommand;

    private ModCommandCatalog(String providerClassName, String displayName,
                              List<Entry> entries, Map<String, Entry> byCommand) {
        this.providerClassName = providerClassName;
        this.displayName = displayName;
        this.entries = Collections.unmodifiableList(new ArrayList<Entry>(entries));
        this.byCommand = Collections.unmodifiableMap(
                new LinkedHashMap<String, Entry>(byCommand));
    }

    public static ModCommandCatalog load(Class<?> provider, String displayName) {
        String providerName = provider == null ? "" : provider.getName();
        try {
            if (provider == null) return empty(providerName, displayName);
            Method method = provider.getMethod(PROVIDER_METHOD);
            if (!Modifier.isStatic(method.getModifiers())
                    || method.getReturnType() != String[][].class)
                return empty(providerName, displayName);
            return fromRows(providerName, displayName, (String[][]) method.invoke(null));
        } catch (Exception unavailable) {
            return empty(providerName, displayName);
        } catch (LinkageError unavailable) {
            return empty(providerName, displayName);
        }
    }

    static ModCommandCatalog load(String providerClass, String displayName,
                                  ClassLoader loader) {
        try {
            return load(Class.forName(providerClass, false, loader), displayName);
        } catch (Exception unavailable) {
            return empty(providerClass, displayName);
        } catch (LinkageError unavailable) {
            return empty(providerClass, displayName);
        }
    }

    static ModCommandCatalog fromRows(String providerClassName, String displayName,
                                      String[][] rows) {
        String cleanName = cleanDisplayName(displayName, providerClassName);
        if (rows == null) return empty(providerClassName, cleanName);
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
        return new ModCommandCatalog(providerClassName, cleanName, entries, commands);
    }

    public String getProviderClassName() { return providerClassName; }
    public String getDisplayName() { return displayName; }
    public boolean isVisible() { return !entries.isEmpty(); }
    public List<Entry> getEntries() { return entries; }

    public Entry find(String command) {
        if (command == null) return null;
        return byCommand.get(normalize(command));
    }

    private static ModCommandCatalog empty(String providerClassName, String displayName) {
        return new ModCommandCatalog(providerClassName,
                cleanDisplayName(displayName, providerClassName),
                Collections.<Entry>emptyList(),
                Collections.<String, Entry>emptyMap());
    }

    private static String cleanDisplayName(String value, String providerClassName) {
        String clean = value == null ? "" : value.trim();
        if (!clean.isEmpty()) return clean;
        if (providerClassName == null || providerClassName.trim().isEmpty())
            return "Mod commands";
        int separator = providerClassName.lastIndexOf('.');
        return providerClassName.substring(separator + 1);
    }

    private static boolean isCompleteCommand(String command) {
        if (!COMMAND.matcher(command).matches()) return false;
        return command.indexOf('<') < 0 && command.indexOf('>') < 0
                && command.indexOf('{') < 0 && command.indexOf('}') < 0;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase(Locale.ENGLISH);
    }
}
