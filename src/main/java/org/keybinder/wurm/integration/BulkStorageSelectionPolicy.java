package org.keybinder.wurm.integration;

import java.util.Locale;
import java.util.regex.Pattern;

/** Fail-closed recognition for the synthetic root of a supported WU bulk window. */
public final class BulkStorageSelectionPolicy {
    private static final Pattern SUPPORTED_NAME = Pattern.compile(
            "(?:^|\\b)(?:bulk storage bin|food storage bin|small crate|large crate|"
                    + "bulk container unit)(?=$|\\s*[,\\[(])");

    private BulkStorageSelectionPolicy() { }

    public static boolean isBulkStorage(String baseName, String displayName,
                                        String windowName) {
        return containsSupportedName(baseName)
                || containsSupportedName(displayName)
                || containsSupportedName(windowName);
    }

    private static boolean containsSupportedName(String value) {
        if (value == null) return false;
        String normalized = value.trim().toLowerCase(Locale.ENGLISH)
                .replaceAll("\\s+", " ");
        return SUPPORTED_NAME.matcher(normalized).find();
    }
}
