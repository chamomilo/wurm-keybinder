package org.keybinder.wurm.command;

import com.wurmonline.shared.constants.ItemMaterials;
import com.wurmonline.shared.util.MaterialUtilities;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Exact candidate predicate produced by ImproveMaterialCompatibilityTable. */
public final class ResourceRequirement {
    private final RequirementFamily family;
    private final Set<String> baseNames;
    private final Byte exactMaterialId;
    private final Short exactImageId;
    private final boolean rejectRareLeather;

    ResourceRequirement(RequirementFamily family, Byte exactMaterialId,
                        Short exactImageId, boolean rejectRareLeather,
                        String... baseNames) {
        this.family = family;
        this.exactMaterialId = exactMaterialId;
        this.exactImageId = exactImageId;
        this.rejectRareLeather = rejectRareLeather;
        LinkedHashSet<String> normalized = new LinkedHashSet<String>();
        if (baseNames != null) for (String name : baseNames)
            normalized.add(ImproveTargetFingerprint.normalize(name));
        this.baseNames = Collections.unmodifiableSet(normalized);
    }

    public RequirementFamily getFamily() { return family; }
    public Byte getExactMaterialId() { return exactMaterialId; }
    public Short getExactImageId() { return exactImageId; }
    public Set<String> getBaseNames() { return baseNames; }

    /** Precise resource description for missing-source Event messages. */
    public String getMissingResourceLabel() {
        String label;
        if (family == RequirementFamily.LUMP && exactMaterialId != null) {
            label = MaterialUtilities.getMaterialString(exactMaterialId) + " lump";
        } else if (family == RequirementFamily.STRING && exactMaterialId != null) {
            label = exactMaterialId == ItemMaterials.MATERIAL_COTTON
                    ? "cotton string" : firstBaseName();
        } else {
            label = firstBaseName();
        }
        if (label.isEmpty()) label = family.name().replace('_', ' ');
        return label.toUpperCase(Locale.ENGLISH);
    }

    private String firstBaseName() {
        return baseNames.isEmpty() ? "" : baseNames.iterator().next();
    }

    public ResourceMatch match(ImproveResourceCandidate candidate) {
        if (candidate == null) return ResourceMatch.rejected("candidate is missing");
        if (!nameMatches(candidate))
            return ResourceMatch.rejected("base name is not one of " + Arrays.toString(baseNames.toArray()));
        if (exactMaterialId != null && candidate.getMaterialId() != exactMaterialId.byteValue())
            return ResourceMatch.rejected("material " + (candidate.getMaterialId() & 0xff)
                    + " does not equal " + (exactMaterialId & 0xff));
        if (family == RequirementFamily.LUMP
                && MaterialUtilities.isMetal(candidate.getMaterialId())
                && candidate.getTemperature() != 5)
            return ResourceMatch.rejected("candidate is not currently usable");
        if (exactImageId != null && candidate.getImageId() != exactImageId.shortValue())
            return ResourceMatch.rejected("inventory image does not match");
        if (rejectRareLeather) {
            if (candidate.getMaterialId() != ItemMaterials.MATERIAL_LEATHER)
                return ResourceMatch.rejected("not ordinary leather material");
            String base = ImproveTargetFingerprint.normalize(candidate.getBaseName());
            String display = ImproveTargetFingerprint.normalize(candidate.getDisplayName());
            if (isPremiumDragonLeatherName(base)
                    || isPremiumDragonLeatherName(display))
                return ResourceMatch.rejected("protected rare leather resource");
            if (!candidate.dragonLeatherColour().isEmpty())
                return ResourceMatch.rejected("dragon leather colour signature");
        }
        return ResourceMatch.accepted();
    }

    private static boolean isPremiumDragonLeatherName(String value) {
        return value != null && (value.contains("dragon leather")
                || value.contains("drake hide")
                || value.matches(".*\\bdragon scales?\\b.*")
                || value.matches(".*\\bscales?\\b.*"));
    }

    private boolean nameMatches(ImproveResourceCandidate candidate) {
        // The source image is not a unique tool identifier (for example carving
        // knife and stone chisel both use 1201). The semantic base name is the
        // one client field that identifies the concrete tool type. Tolerate
        // server-added decorations around the complete type phrase.
        if (family.getKind() == RequirementKind.TOOL) {
            String base = ImproveTargetFingerprint.normalize(candidate.getBaseName());
            for (String expected : baseNames)
                if (semanticBaseNameMatches(base, expected)) return true;
            return false;
        }
        // InventoryMetaItem.type is an image number too. Most consumable images
        // are unique, while ordinary leather still needs its canonical name.
        if (exactImageId != null && !requiresCanonicalBaseName()) return true;
        String base = ImproveTargetFingerprint.normalize(candidate.getBaseName());
        if (requiresCanonicalBaseName()) {
            for (String expected : baseNames)
                if (semanticBaseNameMatches(base, expected)) return true;
            return false;
        }
        if (family != RequirementFamily.LUMP)
            return baseNames.contains(base);
        String display = ImproveTargetFingerprint.normalize(candidate.getDisplayName());
        return base.matches(".*\\blumps?\\b.*")
                || display.matches(".*\\blumps?\\b.*");
    }

    private boolean requiresCanonicalBaseName() {
        return family == RequirementFamily.LEATHER
                || family == RequirementFamily.PELT
                || family == RequirementFamily.SHARD;
    }

    private static boolean semanticBaseNameMatches(String value,
                                                   String expected) {
        if (value == null || expected == null || value.isEmpty()
                || expected.isEmpty()) return false;
        // The phrase may be preceded by rarity/material words. After it, accept
        // only the end of the name, a parenthesized state such as (glowing), or
        // a comma introducing a material suffix. This deliberately rejects
        // different item types such as "carving knife blade".
        String expression = "(?:^|\\s)" + Pattern.quote(expected)
                + "(?=$|\\s*\\(|\\s*,)";
        return Pattern.compile(expression).matcher(value).find();
    }

    @Override public String toString() {
        return family + " names=" + baseNames + (exactMaterialId == null ? ""
                : " material=" + (exactMaterialId & 0xff))
                + (exactImageId == null ? "" : " image=" + exactImageId);
    }
}
