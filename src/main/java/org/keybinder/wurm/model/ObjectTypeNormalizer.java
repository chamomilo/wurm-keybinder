package org.keybinder.wurm.model;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.keybinder.wurm.i18n.Messages;

/** Canonical material-independent textual object type used by portable filters. */
public final class ObjectTypeNormalizer {
    private static final Set<String> RARITY_MODIFIERS = new HashSet<String>(Arrays.asList(
            "rare", "supreme", "fantastic"));
    private static final Set<String> CREATURE_MODIFIERS = new HashSet<String>(Arrays.asList(
            "young", "adolescent", "mature", "aged", "old", "venerable",
            "starving", "diseased", "fat", "champion", "alert", "angry", "fierce",
            "raging", "slow", "sly", "hardened", "scared", "lurking"));
    private static final Set<String> MATERIALS = new HashSet<String>(Arrays.asList(
            "wood", "wooden", "birchwood", "pinewood", "oakenwood", "cedarwood",
            "willowwood", "maplewood", "applewood", "lemonwood", "olivewood",
            "cherrywood", "chestnutwood", "walnutwood", "firwood", "lindenwood",
            "orangewood", "lavenderwood", "rosewood", "thornwood", "grapewood",
            "camelliawood", "oleanderwood", "ivywood", "hazelnutwood",
            "raspberrywood", "blueberrywood", "lingonberrywood",
            "iron", "steel", "copper", "tin", "lead", "silver", "gold", "zinc",
            "brass", "bronze", "adamantine", "glimmersteel", "seryll", "electrum",
            "stone", "slate", "marble", "sandstone", "flesh", "meat", "leather",
            "cotton", "clay", "pottery", "glass", "magic", "vegetarian", "fire",
            "oil", "water", "charcoal", "coal", "dairy", "honey", "fat", "paper",
            "bone", "salt", "crystal", "wemp", "diamond", "animal", "tar", "peat",
            "reed", "wool", "straw", "rye", "oat", "barley", "wheat"));
    private static final Set<String> ITEM_STATE_MODIFIERS =
            new HashSet<String>(Arrays.asList(
                    "salty", "fresh", "frozen", "warm", "hot", "boiling", "searing",
                    "glowing", "burning", "lit", "unlit", "open", "closed", "locked",
                    "unlocked", "unfinished", "damaged",
                    // Item.getName(boolean) prefixes both ordinary and wild bee
                    // hives with one of these live colony states. Creation-list
                    // recipes retain the stable template name ("bee hive").
                    "empty", "active", "dormant", "noisy"));
    private static final String STATE_PATTERN =
            "frozen|very warm|warm|hot|boiling|searing(?: hot)?|glowing(?: from heat)?"
                    + "|burning|lit|unlit|open|closed|locked|unlocked|unfinished|damaged"
                    + "|empty|active|dormant|noisy";

    private ObjectTypeNormalizer() { }

    public static String normalizeType(String name) {
        if (name == null)
            throw new IllegalArgumentException(Messages.text("validation.object_name_missing"));
        String value = name.replace('\u00a0', ' ').replace('\u2007', ' ')
                .replace('\u202f', ' ').trim().toLowerCase(Locale.ENGLISH)
                .replaceAll("\\s+", " ");
        value = value.replaceFirst("^(a|an|the)\\s+", "");
        value = stripLeadingRarity(value);
        value = stripTrailingDecorations(value);
        if (value.equals("tree stump") || value.endsWith(" tree stump"))
            return "tree stump";
        if (value.equals("felled tree") || value.endsWith(" felled tree"))
            return "felled tree";
        if (value.equals("tree") || value.endsWith(" tree")) return "tree";
        if (value.equals("water") || value.endsWith(" water")) return "water";
        if (value.equals("log") || value.endsWith(" log")) return "log";

        String[] words = value.split(" ");
        int first = 0;
        while (first < words.length - 1
                && (RARITY_MODIFIERS.contains(words[first])
                || CREATURE_MODIFIERS.contains(words[first])
                || ITEM_STATE_MODIFIERS.contains(words[first])
                || MATERIALS.contains(words[first])))
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

    /** One comparison path shared by hover, nearby, and inventory filters. */
    public static boolean matchesType(String wanted, String candidate) {
        try {
            return normalizeType(wanted).equals(normalizeType(candidate));
        } catch (RuntimeException invalidType) {
            return false;
        }
    }

    private static String stripLeadingRarity(String value) {
        String current = value;
        boolean changed;
        do {
            changed = false;
            for (String rarity : RARITY_MODIFIERS) {
                String prefix = rarity + " ";
                if (current.startsWith(prefix)) {
                    current = current.substring(prefix.length()).trim();
                    changed = true;
                    break;
                }
            }
        } while (changed);
        return current;
    }

    private static String stripTrailingDecorations(String value) {
        String current = value;
        String previous;
        do {
            previous = current;
            current = current.replaceFirst(
                    "\\s*\\((?:" + STATE_PATTERN + "|" + materialPattern() + ")\\)$", "")
                    .trim();
            current = current.replaceFirst(
                    ",\\s*(?:" + materialPattern() + ")$", "").trim();
        } while (!current.equals(previous));
        return current;
    }

    private static String materialPattern() {
        return "wood|wooden|birchwood|pinewood|oakenwood|cedarwood|willowwood|maplewood"
                + "|applewood|lemonwood|olivewood|cherrywood|chestnutwood|walnutwood"
                + "|firwood|lindenwood|orangewood|lavenderwood|rosewood|thornwood"
                + "|grapewood|camelliawood|oleanderwood|ivywood|hazelnutwood"
                + "|raspberrywood|blueberrywood|lingonberrywood"
                + "|iron|steel|copper|tin|lead|silver|gold|zinc|brass|bronze"
                + "|adamantine|glimmersteel|seryll|electrum|stone|slate|marble"
                + "|sandstone|flesh|meat|leather|cotton|clay|pottery|glass|magic"
                + "|vegetarian|fire|oil|water|charcoal|coal|dairy|honey|fat|paper"
                + "|bone|salt|crystal|wemp|diamond|animal|tar|peat|reed|wool|straw"
                + "|rye|oat|barley|wheat";
    }
}
