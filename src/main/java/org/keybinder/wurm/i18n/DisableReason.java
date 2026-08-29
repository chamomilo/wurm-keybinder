package org.keybinder.wurm.i18n;

import java.util.ArrayList;
import java.util.List;

/** Stable persisted reason values. Localization happens only at presentation boundaries. */
public final class DisableReason {
    private static final String PREFIX = "keybinder.reason:";

    private DisableReason() {}

    public static String value(String code, Object... arguments) {
        StringBuilder result = new StringBuilder(PREFIX).append(code);
        if (arguments != null)
            for (Object argument : arguments)
                result.append('|').append(escape(String.valueOf(argument)));
        return result.toString();
    }

    public static String display(String stored) {
        if (stored == null || stored.trim().isEmpty()) return "";
        if (!stored.startsWith(PREFIX)) return displayLegacy(stored);
        String[] fields = split(stored.substring(PREFIX.length()));
        Object[] arguments = new Object[Math.max(0, fields.length - 1)];
        for (int i = 1; i < fields.length; i++) arguments[i - 1] = fields[i];
        return Messages.text("reason." + fields[0], arguments);
    }

    /** User intent is an inactive state, not an error that prevents re-enabling. */
    public static boolean blocksEnable(String stored) {
        if (stored == null || stored.trim().isEmpty()) return false;
        return !"disabled by user".equals(stored)
                && !value("disabled_by_user").equals(stored)
                && !isQueueExceeded(stored);
    }

    /** Legacy automatic queue-limit disables are warnings in current builds. */
    public static boolean isQueueExceeded(String stored) {
        if (stored == null || stored.trim().isEmpty()) return false;
        if (stored.startsWith(PREFIX)) {
            String[] fields = split(stored.substring(PREFIX.length()));
            return fields.length > 0 && "queue_exceeded".equals(fields[0]);
        }
        return stored.matches(
                "^execution cost \\d+ exceeds (?:the )?current limit \\d+$");
    }

    /** True when the stored error is only caused by another owner of the same chord. */
    public static boolean isKeyConflict(String stored) {
        if (stored == null || stored.trim().isEmpty()) return false;
        if (stored.startsWith(PREFIX)) {
            String[] fields = split(stored.substring(PREFIX.length()));
            if (fields.length == 0) return false;
            String code = fields[0];
            return "replaced_by".equals(code) || "key_used".equals(code)
                    || "key_used_unknown".equals(code) || "key_used_vanilla".equals(code)
                    || "key_in_use".equals(code) || "extracted_review".equals(code);
        }
        return stored.startsWith("replaced by ")
                || stored.startsWith("key ") && stored.contains(" is already used by ")
                || "disabled because the selected key is already in use".equals(stored);
    }

    /** True when the stored conflict can be recomputed from managed records alone. */
    public static boolean isManagedKeyConflict(String stored) {
        if (stored == null || stored.trim().isEmpty()) return false;
        if (stored.startsWith(PREFIX)) {
            String[] fields = split(stored.substring(PREFIX.length()));
            if (fields.length == 0) return false;
            String code = fields[0];
            return "replaced_by".equals(code) || "key_used".equals(code)
                    || "extracted_review".equals(code);
        }
        return stored.startsWith("replaced by ")
                || stored.startsWith("key ") && stored.contains(" is already used by ");
    }

    private static String displayLegacy(String stored) {
        if ("disabled by user".equals(stored)) return Messages.text("reason.disabled_by_user");
        if ("not configured".equals(stored)) return Messages.text("reason.not_configured");
        if ("not enabled for this account".equals(stored))
            return Messages.text("reason.not_enabled_account");
        if ("local binding changed; synchronized definition not applied".equals(stored)
                || "local binding changed; synchronized definition was not applied".equals(stored))
            return Messages.text("reason.sync_changed");
        if ("disabled because the selected key is already in use".equals(stored))
            return Messages.text("reason.key_in_use");
        if ("invalid keybind".equals(stored)) return Messages.text("reason.invalid");
        if ("Name is missing".equals(stored)) return Messages.text("reason.invalid_name");
        if ("Key is missing".equals(stored)) return Messages.text("reason.invalid_key");
        if ("At least one step is required".equals(stored))
            return Messages.text("reason.invalid_steps");
        if (stored.startsWith("replaced by "))
            return Messages.text("reason.replaced_by",
                    stored.substring("replaced by ".length()));
        if (stored.startsWith("unable to restore binding: "))
            return Messages.text("reason.restore_failed",
                    stored.substring("unable to restore binding: ".length()));
        String keyPrefix = "key ";
        String keyMarker = " is already used by ";
        if (stored.startsWith(keyPrefix) && stored.contains(keyMarker)) {
            int marker = stored.indexOf(keyMarker);
            return Messages.text("reason.key_used",
                    stored.substring(keyPrefix.length(), marker),
                    stored.substring(marker + keyMarker.length()));
        }
        java.util.regex.Matcher queue = java.util.regex.Pattern.compile(
                "^execution cost (\\d+) exceeds (?:the )?current limit (\\d+)$")
                .matcher(stored);
        if (queue.matches())
            return Messages.text("reason.queue_exceeded", queue.group(1), queue.group(2));
        return stored;
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("|", "\\|");
    }

    private static String[] split(String encoded) {
        List<String> result = new ArrayList<String>();
        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        for (int i = 0; i < encoded.length(); i++) {
            char character = encoded.charAt(i);
            if (escaped) { value.append(character); escaped = false; }
            else if (character == '\\') escaped = true;
            else if (character == '|') { result.add(value.toString()); value.setLength(0); }
            else value.append(character);
        }
        if (escaped) value.append('\\');
        result.add(value.toString());
        return result.toArray(new String[result.size()]);
    }
}
