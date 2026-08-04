package org.keybinder.wurm.model;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Quantity bound captured from Wurm's bulk-row display name, e.g. {@code (619x)}. */
public final class BulkQuantityLimit {
    private static final Pattern COUNT_SUFFIX = Pattern.compile(
            "\\(([0-9][0-9\\s\\u00a0,.]*)x\\)\\s*$",
            Pattern.CASE_INSENSITIVE);

    private BulkQuantityLimit() { }

    /** Returns zero when the name does not expose a trustworthy bulk count. */
    public static int fromName(String name) {
        if (name == null) return 0;
        Matcher matcher = COUNT_SUFFIX.matcher(name.trim());
        if (!matcher.find()) return 0;
        String digits = matcher.group(1).replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return 0;
        try {
            long value = Long.parseLong(digits);
            return value > 0L && value <= Integer.MAX_VALUE ? (int) value : 0;
        } catch (NumberFormatException invalid) {
            return 0;
        }
    }

    /** Legacy sources without a count retain their formerly valid quantity of one. */
    public static int capturedMaximum(BulkStorageItem source) {
        if (source == null || source.getItem() == null) return 1;
        int parsed = fromName(source.getItem().getName());
        return parsed > 0 ? parsed : 1;
    }

    public static boolean accepts(BulkStorageItem source, int quantity) {
        return quantity > 0 && quantity <= capturedMaximum(source);
    }
}
