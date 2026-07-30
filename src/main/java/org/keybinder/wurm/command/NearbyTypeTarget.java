package org.keybinder.wurm.command;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.keybinder.wurm.i18n.Messages;

/** Portable nearby target based on the stable type portion of a Wurm hover name. */
public final class NearbyTypeTarget {
    public static final String PREFIX = "nearby ";
    private static final Set<String> CREATURE_MODIFIERS = new HashSet<>(Arrays.asList(
            "young", "adolescent", "mature", "aged", "old", "venerable",
            "starving", "diseased", "fat", "champion", "alert", "angry", "fierce",
            "raging", "slow", "sly", "hardened", "scared", "lurking"
    ));
    private static final Set<String> MATERIALS = new HashSet<>(Arrays.asList(
            "wood", "wooden", "birchwood", "pinewood", "oakenwood", "cedarwood",
            "willowwood", "maplewood", "applewood", "lemonwood", "olivewood",
            "cherrywood", "chestnutwood", "walnutwood", "firwood", "lindenwood",
            "orangewood", "iron", "steel", "copper", "tin", "lead", "silver",
            "gold", "zinc", "brass", "bronze", "adamantine", "glimmersteel",
            "seryll", "stone", "slate", "marble", "sandstone"
    ));

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
        if (hoverName == null)
            throw new IllegalArgumentException(Messages.text("validation.object_name_missing"));
        String value = hoverName.trim().toLowerCase(Locale.ENGLISH)
                .replaceAll("\\s+", " ");
        value = value.replaceFirst("^(a|an|the)\\s+", "");
        if (value.endsWith(" tree stump")) return "tree stump";
        if (value.endsWith(" felled tree")) return "felled tree";

        String[] words = value.split(" ");
        int first = 0;
        while (first < words.length - 1 && CREATURE_MODIFIERS.contains(words[first])) first++;
        while (first < words.length - 1 && MATERIALS.contains(words[first])) first++;
        StringBuilder result = new StringBuilder();
        for (int i = first; i < words.length; i++) {
            if (result.length() > 0) result.append(' ');
            result.append(words[i]);
        }
        if (result.length() == 0)
            throw new IllegalArgumentException(Messages.text("validation.object_type_missing"));
        return result.toString();
    }
}
