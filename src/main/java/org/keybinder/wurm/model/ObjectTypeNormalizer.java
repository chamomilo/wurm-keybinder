package org.keybinder.wurm.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.keybinder.wurm.i18n.Messages;

/** Canonical material-independent textual object type used by portable filters. */
public final class ObjectTypeNormalizer {
    private static final Set<String> CREATURE_MODIFIERS = new HashSet<String>(Arrays.asList(
            "young", "adolescent", "mature", "aged", "old", "venerable",
            "starving", "diseased", "fat", "champion", "alert", "angry", "fierce",
            "raging", "slow", "sly", "hardened", "scared", "lurking"));
    private static final Set<String> MATERIALS = new HashSet<String>(Arrays.asList(
            "wood", "wooden", "birchwood", "pinewood", "oakenwood", "cedarwood",
            "willowwood", "maplewood", "applewood", "lemonwood", "olivewood",
            "cherrywood", "chestnutwood", "walnutwood", "firwood", "lindenwood",
            "orangewood", "iron", "steel", "copper", "tin", "lead", "silver",
            "gold", "zinc", "brass", "bronze", "adamantine", "glimmersteel",
            "seryll", "stone", "slate", "marble", "sandstone"));

    private ObjectTypeNormalizer() { }

    public static String normalizeType(String name) {
        if (name == null)
            throw new IllegalArgumentException(Messages.text("validation.object_name_missing"));
        String value = name.trim().toLowerCase(Locale.ENGLISH).replaceAll("\\s+", " ");
        value = value.replaceFirst("^(a|an|the)\\s+", "");
        value = value.replaceFirst("\\s*\\((?:" + materialPattern() + ")\\)$", "");
        value = value.replaceFirst(",\\s*(?:" + materialPattern() + ")$", "");
        if (value.endsWith(" tree stump")) return "tree stump";
        if (value.endsWith(" felled tree")) return "felled tree";

        String[] words = value.split(" ");
        int first = 0;
        while (first < words.length - 1
                && (CREATURE_MODIFIERS.contains(words[first]) || MATERIALS.contains(words[first])))
            first++;
        int last = words.length;
        while (last - first > 1 && MATERIALS.contains(words[last - 1])) last--;
        StringBuilder result = new StringBuilder();
        for (int i = first; i < last; i++) {
            if (result.length() > 0) result.append(' ');
            result.append(words[i]);
        }
        if (result.length() == 0)
            throw new IllegalArgumentException(Messages.text("validation.object_type_missing"));
        return result.toString();
    }

    private static String materialPattern() {
        return "wood|wooden|birchwood|pinewood|oakenwood|cedarwood|willowwood|maplewood"
                + "|applewood|lemonwood|olivewood|cherrywood|chestnutwood|walnutwood"
                + "|firwood|lindenwood|orangewood|iron|steel|copper|tin|lead|silver"
                + "|gold|zinc|brass|bronze|adamantine|glimmersteel|seryll|stone|slate"
                + "|marble|sandstone";
    }
}
