package org.keybinder.wurm.catalog;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Immutable catalog and canonical representation of keys accepted by Keybinder. */
public final class InputKeyCatalog {
    public static final String WHEEL_UP = "MOUSE_WHEEL_UP";
    public static final String WHEEL_DOWN = "MOUSE_WHEEL_DOWN";

    public enum InputKind { KEYBOARD, MOUSE_BUTTON, MOUSE_WHEEL_DIRECTION }

    public static final class Entry {
        private final String persistedName;
        private final String displayName;
        private final InputKind inputKind;
        private final int sortGroup;
        private final int sortOrder;

        private Entry(String persistedName, String displayName, InputKind inputKind,
                      int sortGroup, int sortOrder) {
            this.persistedName = persistedName;
            this.displayName = displayName;
            this.inputKind = inputKind;
            this.sortGroup = sortGroup;
            this.sortOrder = sortOrder;
        }

        public String getPersistedName() { return persistedName; }
        public String getDisplayName() { return displayName; }
        public InputKind getInputKind() { return inputKind; }
    }

    private static final class SystemHolder {
        private static final InputKeyCatalog INSTANCE = runtimeSnapshot();
    }
    private final List<Entry> entries;
    private final Map<String, Entry> byPersisted;
    private final Map<String, Entry> byDisplay;

    private InputKeyCatalog(List<Entry> source) {
        List<Entry> copy = new ArrayList<Entry>(source);
        Collections.sort(copy, new Comparator<Entry>() {
            @Override public int compare(Entry left, Entry right) {
                int group = Integer.compare(left.sortGroup, right.sortGroup);
                if (group != 0) return group;
                int order = Integer.compare(left.sortOrder, right.sortOrder);
                if (order != 0) return order;
                return left.persistedName.compareTo(right.persistedName);
            }
        });
        entries = Collections.unmodifiableList(copy);
        Map<String, Entry> persisted = new LinkedHashMap<String, Entry>();
        Map<String, Entry> display = new LinkedHashMap<String, Entry>();
        for (Entry entry : entries) {
            persisted.put(entry.persistedName.toUpperCase(Locale.ENGLISH), entry);
            display.put(entry.displayName.toUpperCase(Locale.ENGLISH), entry);
        }
        byPersisted = Collections.unmodifiableMap(persisted);
        byDisplay = Collections.unmodifiableMap(display);
    }

    /** Built lazily so mouse button discovery happens when the editor/list first needs it. */
    public static InputKeyCatalog system() { return SystemHolder.INSTANCE; }
    public List<Entry> entries() { return entries; }

    public String[] displayOptions() {
        String[] options = new String[entries.size() + 1];
        options[0] = "";
        for (int i = 0; i < entries.size(); i++) options[i + 1] = entries.get(i).displayName;
        return options;
    }

    public Entry findPersisted(String value) {
        if (value == null) return null;
        return byPersisted.get(value.trim().toUpperCase(Locale.ENGLISH));
    }

    public Entry findDisplay(String value) {
        if (value == null) return null;
        return byDisplay.get(value.trim().toUpperCase(Locale.ENGLISH));
    }

    public String persistedName(String displayOrPersisted) {
        Entry entry = findDisplay(displayOrPersisted);
        if (entry == null) entry = findPersisted(displayOrPersisted);
        return entry == null ? normalizeBase(displayOrPersisted) : entry.persistedName;
    }

    public String displayName(String persisted) {
        Entry entry = findPersisted(persisted);
        return entry == null ? normalizeBase(persisted) : entry.displayName;
    }

    public static boolean isVirtual(String chord) {
        String base = baseKey(normalizeChord(chord));
        return WHEEL_UP.equals(base) || WHEEL_DOWN.equals(base);
    }

    public static String normalizeChord(String chord) {
        if (chord == null || chord.trim().isEmpty()) return "";
        boolean ctrl = false, shift = false, alt = false;
        String base = "";
        for (String part : chord.trim().split("\\+")) {
            String token = part.trim().toUpperCase(Locale.ENGLISH);
            if ("CTRL".equals(token) || "CONTROL".equals(token)) ctrl = true;
            else if ("SHIFT".equals(token)) shift = true;
            else if ("ALT".equals(token)) alt = true;
            else if (!token.isEmpty()) base = normalizeBase(token);
        }
        if (base.isEmpty()) return "";
        StringBuilder result = new StringBuilder();
        if (ctrl) result.append("CTRL+");
        if (shift) result.append("SHIFT+");
        if (alt) result.append("ALT+");
        return result.append(base).toString();
    }

    public static String baseKey(String chord) {
        String normalized = normalizeChord(chord);
        int separator = normalized.lastIndexOf('+');
        return separator < 0 ? normalized : normalized.substring(separator + 1);
    }

    public static String modifiers(String chord) {
        String normalized = normalizeChord(chord);
        int separator = normalized.lastIndexOf('+');
        return separator < 0 ? "" : normalized.substring(0, separator);
    }

    public static String displayChord(String chord) {
        String normalized = normalizeChord(chord);
        String modifiers = modifiers(normalized);
        String display = system().displayName(baseKey(normalized));
        return modifiers.isEmpty() ? display : modifiers + "+" + display;
    }

    public static String wheelChord(int delta, boolean ctrl, boolean shift, boolean alt) {
        if (delta == 0) return "";
        StringBuilder chord = new StringBuilder();
        if (ctrl) chord.append("CTRL+");
        if (shift) chord.append("SHIFT+");
        if (alt) chord.append("ALT+");
        return chord.append(delta < 0 ? WHEEL_UP : WHEEL_DOWN).toString();
    }

    private static String normalizeBase(String value) {
        if (value == null) return "";
        String base = value.trim().toUpperCase(Locale.ENGLISH);
        if ("MOUSE2".equals(base)) return "MOUSE2";
        return base;
    }

    private static InputKeyCatalog runtimeSnapshot() {
        Map<String, Entry> unique = new LinkedHashMap<String, Entry>();
        for (int code = 1; code < Keyboard.KEYBOARD_SIZE; code++) {
            String name = Keyboard.getKeyName(code);
            if (name == null || name.trim().isEmpty() || Keyboard.getKeyIndex(name) != code) continue;
            String persisted = name.toUpperCase(Locale.ENGLISH);
            if (!unique.containsKey(persisted))
                unique.put(persisted, new Entry(persisted, name, InputKind.KEYBOARD,
                        keyboardGroup(persisted), code));
        }
        int buttons;
        try { buttons = Mouse.getButtonCount(); }
        catch (Throwable unavailable) { buttons = 3; }
        buttons = Math.max(3, buttons);
        for (int index = 0; index < buttons; index++) {
            String persisted = "MOUSE" + index;
            String display = index == 2 ? "Mouse Wheel Button" : "Mouse Button " + (index + 1);
            unique.put(persisted, new Entry(persisted, display, InputKind.MOUSE_BUTTON, 10, index));
        }
        unique.put(WHEEL_UP, new Entry(WHEEL_UP, "Mouse Wheel Up",
                InputKind.MOUSE_WHEEL_DIRECTION, 11, 0));
        unique.put(WHEEL_DOWN, new Entry(WHEEL_DOWN, "Mouse Wheel Down",
                InputKind.MOUSE_WHEEL_DIRECTION, 12, 0));
        return new InputKeyCatalog(new ArrayList<Entry>(unique.values()));
    }

    private static int keyboardGroup(String name) {
        if (name.length() == 1 && name.charAt(0) >= 'A' && name.charAt(0) <= 'Z') return 1;
        if (name.length() == 1 && Character.isDigit(name.charAt(0))) return 2;
        if (name.matches("F\\d+")) return 3;
        if (name.startsWith("NUMPAD")) return 7;
        if (name.contains("SHIFT") || name.contains("CONTROL") || name.contains("MENU")
                || name.contains("META") || name.contains("WIN") || name.contains("CAPITAL")
                || name.contains("LOCK") || name.contains("SYSRQ")) return 8;
        return 6;
    }
}
