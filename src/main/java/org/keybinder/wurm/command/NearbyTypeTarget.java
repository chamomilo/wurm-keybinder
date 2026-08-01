package org.keybinder.wurm.command;

import java.util.Locale;
import org.keybinder.wurm.i18n.Messages;

/** Portable nearby target based on the stable type portion of a Wurm hover name. */
public final class NearbyTypeTarget {
    public static final String PREFIX = "nearby ";

    private NearbyTypeTarget() {}

    public static boolean isNearbyType(String target) {
        return target != null
                && target.toLowerCase(Locale.ENGLISH).startsWith(PREFIX)
                && !"nearby by type".equalsIgnoreCase(target.trim());
    }

    public static String encode(String hoverName) {
        return PREFIX + normalizeType(hoverName);
    }

    public static String type(String target) {
        if (!isNearbyType(target))
            throw new IllegalArgumentException(Messages.text("validation.nearby_not_type"));
        String value = target.substring(PREFIX.length()).trim().toLowerCase(Locale.ENGLISH);
        if (value.isEmpty())
            throw new IllegalArgumentException(Messages.text("validation.nearby_type"));
        return value;
    }

    public static boolean matches(String target, String hoverName) {
        return type(target).equals(normalizeType(hoverName));
    }

    public static String normalizeType(String hoverName) {
        return ObjectTypeNormalizer.normalizeType(hoverName);
    }
}
