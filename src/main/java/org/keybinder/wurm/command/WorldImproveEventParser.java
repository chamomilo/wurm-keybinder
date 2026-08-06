package org.keybinder.wurm.command;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses only the standard WU Examine/Improve phrases needed by world items. */
final class WorldImproveEventParser {
    private static final Pattern DAMAGE = Pattern.compile(
            "(?i)(?:^|,\\s*)dam(?:age)?\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)");
    private static final Pattern QUALITY = Pattern.compile(
            "(?i)(?:^|[,.]\\s*)ql\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)");
    private static final Pattern RARITY_RUNE = Pattern.compile(
            "(?i)increase the chance of increasing rarity when improved\\s*"
                    + "\\(([0-9]+(?:\\.[0-9]+)?)%\\)");

    /** A server item Examine description carries both quality and damage. */
    static boolean isExamineDescription(String message) {
        if (message == null) return false;
        return QUALITY.matcher(message).find() && DAMAGE.matcher(message).find();
    }

    static Parsed parse(String message) {
        if (message == null || message.trim().isEmpty()) return Parsed.EMPTY;
        String value = message.toLowerCase(Locale.ENGLISH);
        Boolean damaged = null;
        Float quality = null;
        Matcher qualityMatcher = QUALITY.matcher(value);
        if (qualityMatcher.find()) {
            try { quality = Float.parseFloat(qualityMatcher.group(1)); }
            catch (NumberFormatException ignored) { /* leave unknown */ }
        }
        Matcher damage = DAMAGE.matcher(value);
        if (damage.find()) {
            try { damaged = Float.parseFloat(damage.group(1)) > 0f; }
            catch (NumberFormatException ignored) { /* leave unknown */ }
        } else if (value.contains("damage the ")
                || value.contains("before you try to improve")) {
            damaged = Boolean.TRUE;
        } else if (value.contains("you repair the ")) {
            damaged = Boolean.FALSE;
        }

        RequirementFamily requirement = requirement(value);
        Byte rarity = rarity(value);
        Float rarityRuneModifier = rarityRuneModifier(value);
        return new Parsed(requirement, damaged, quality, rarity,
                rarityRuneModifier);
    }

    private static Byte rarity(String value) {
        if (value.contains("this is a fantastic example of the item"))
            return Byte.valueOf((byte) 3);
        if (value.contains("this is a supreme example of the item"))
            return Byte.valueOf((byte) 2);
        if (value.contains("this is a very rare and interesting version of the item"))
            return Byte.valueOf((byte) 1);
        return null;
    }

    private static Float rarityRuneModifier(String value) {
        Matcher matcher = RARITY_RUNE.matcher(value);
        if (!matcher.find()) return null;
        try { return Float.valueOf(Float.parseFloat(matcher.group(1)) / 100.0f); }
        catch (NumberFormatException ignored) { return null; }
    }

    private static RequirementFamily requirement(String value) {
        // Specific names precede broad verbs so overlapping phrases cannot
        // classify leather knife as generic leather or stone chisel as shards.
        if (containsAny(value, "with a clay shaper", "use a clay shaper"))
            return RequirementFamily.CLAY_SHAPER;
        if (containsAny(value, "with a spatula", "use a spatula"))
            return RequirementFamily.SPATULA;
        if (containsAny(value, "with a carving knife", "use a carving knife",
                "carve away", "notches"))
            return RequirementFamily.CARVING_KNIFE;
        if (containsAny(value, "with a stone chisel", "use a stone chisel",
                "stone irregularit", "irregularities in the stone"))
            return RequirementFamily.STONE_CHISEL;
        if (containsAny(value, "with a leather knife", "use a leather knife"))
            return RequirementFamily.LEATHER_KNIFE;
        if (containsAny(value, "with a whetstone", "use a whetstone",
                "to be sharpened"))
            return RequirementFamily.WHETSTONE;
        if (containsAny(value, "with a hammer", "use a hammer",
                "must be flattened", "dents that must"))
            return RequirementFamily.HAMMER;
        if (containsAny(value, "with a mallet", "use a mallet",
                "smooth out a quirk", "smooth a quirk"))
            return RequirementFamily.MALLET;
        if (containsAny(value, "use a file", "with a file", "filing"))
            return RequirementFamily.FILE;
        if (containsAny(value, "with an awl", "use an awl", "punch holes"))
            return RequirementFamily.AWL;
        if (containsAny(value, "with scissors", "use scissors", "must be cut away"))
            return RequirementFamily.SCISSORS;
        if (containsAny(value, "with a needle", "with an iron needle",
                "use a needle", "use an iron needle", "must be backstitched",
                "slipstitch"))
            return RequirementFamily.NEEDLE;
        if (containsAny(value, "by hand", "with your hand"))
            return RequirementFamily.BODY_HAND;
        if (containsAny(value, "water", "some stains", "need to temper",
                "needs to be tempered", "wash the stains"))
            return RequirementFamily.WATER;
        if (containsAny(value, "pelt", "want to polish", "need to polish"))
            return RequirementFamily.PELT;
        if (containsAny(value, "rock shards", "stone shards", "slate shard",
                "marble shard", "sandstone shard", "more shards"))
            return RequirementFamily.SHARD;
        if (containsAny(value, "with a lump", "with more lump", "more lump"))
            return RequirementFamily.LUMP;
        if (containsAny(value, "with a log", "with more log", "more log"))
            return RequirementFamily.LOG;
        if (containsAny(value, "with a string", "with more string", "more string",
                "with wool", "more wool"))
            return RequirementFamily.STRING;
        if (containsAny(value, "with leather", "with more leather", "more leather"))
            return RequirementFamily.LEATHER;
        if (containsAny(value, "with clay", "with more clay", "more clay"))
            return RequirementFamily.CLAY;
        return null;
    }

    private static boolean containsAny(String value, String... phrases) {
        for (String phrase : phrases) if (value.contains(phrase)) return true;
        return false;
    }

    static final class Parsed {
        static final Parsed EMPTY = new Parsed(null, null, null, null, null);
        private final RequirementFamily requirement;
        private final Boolean damaged;
        private final Float quality;
        private final Byte rarity;
        private final Float rarityRuneModifier;

        Parsed(RequirementFamily requirement, Boolean damaged, Float quality,
               Byte rarity, Float rarityRuneModifier) {
            this.requirement = requirement;
            this.damaged = damaged;
            this.quality = quality;
            this.rarity = rarity;
            this.rarityRuneModifier = rarityRuneModifier;
        }

        RequirementFamily getRequirement() { return requirement; }
        Boolean getDamaged() { return damaged; }
        Float getQuality() { return quality; }
        Byte getRarity() { return rarity; }
        Float getRarityRuneModifier() { return rarityRuneModifier; }
    }

    private WorldImproveEventParser() {}
}
