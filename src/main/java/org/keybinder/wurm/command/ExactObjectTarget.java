package org.keybinder.wurm.command;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class ExactObjectTarget {
    private static final String PREFIX = "@id";

    private ExactObjectTarget() {}

    public static boolean isExact(String target) {
        return target != null && target.startsWith(PREFIX);
    }

    public static String encode(long id, String name) {
        String safeName = name == null ? "" : name.trim();
        String encodedName = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(safeName.getBytes(StandardCharsets.UTF_8));
        return PREFIX + id + (encodedName.isEmpty() ? "" : ":" + encodedName);
    }

    public static long id(String target) {
        if (!isExact(target)) throw new IllegalArgumentException("Exact object target is missing");
        int separator = target.indexOf(':', PREFIX.length());
        String value = separator < 0 ? target.substring(PREFIX.length())
                : target.substring(PREFIX.length(), separator);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid exact object ID: " + value, e);
        }
    }

    public static String name(String target) {
        if (!isExact(target)) return "";
        int separator = target.indexOf(':', PREFIX.length());
        if (separator < 0 || separator + 1 >= target.length()) return "";
        try {
            return new String(Base64.getUrlDecoder().decode(target.substring(separator + 1)),
                    StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    public static String display(String target) {
        String name = name(target);
        return name.isEmpty() ? "Exact object" : name;
    }
}
