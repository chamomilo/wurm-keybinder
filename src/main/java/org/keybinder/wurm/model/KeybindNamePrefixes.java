package org.keybinder.wurm.model;

import java.util.Locale;

/** Stable, language-independent prefixes derived from a keybind's current mode. */
public final class KeybindNamePrefixes {
    public static final String HUD = "(HUD) ";
    public static final String MULTI = "(Multi) ";
    private static final String LEGACY_HUD = "HUD ";
    private static final String LEGACY_MULTI = "Multi ";

    private KeybindNamePrefixes() {}

    public static String apply(String name, int variantCount, boolean hudMulti) {
        String base = baseName(name);
        if (base.isEmpty()) return "";
        boolean multi = variantCount > 1;
        String prefix = (hudMulti ? HUD : "") + (multi ? MULTI : "");
        int available = Math.max(0, KeybindLimits.MAX_RECORD_NAME_LENGTH - prefix.length());
        if (base.length() > available) base = base.substring(0, available).trim();
        return prefix + base;
    }

    public static String baseName(String name) {
        String result = name == null ? "" : name.trim();
        boolean removed;
        do {
            removed = false;
            String lower = result.toLowerCase(Locale.ENGLISH);
            if (lower.startsWith(HUD.toLowerCase(Locale.ENGLISH))) {
                result = result.substring(HUD.length()).trim();
                removed = true;
            }
            lower = result.toLowerCase(Locale.ENGLISH);
            if (lower.startsWith(MULTI.toLowerCase(Locale.ENGLISH))) {
                result = result.substring(MULTI.length()).trim();
                removed = true;
            }
            lower = result.toLowerCase(Locale.ENGLISH);
            if (lower.startsWith(LEGACY_HUD.toLowerCase(Locale.ENGLISH))) {
                result = result.substring(LEGACY_HUD.length()).trim();
                removed = true;
            }
            lower = result.toLowerCase(Locale.ENGLISH);
            if (lower.startsWith(LEGACY_MULTI.toLowerCase(Locale.ENGLISH))) {
                result = result.substring(LEGACY_MULTI.length()).trim();
                removed = true;
            }
        } while (removed);
        return result;
    }
}
