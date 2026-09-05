package org.keybinder.wurm.command;

import com.wurmonline.shared.constants.ItemMaterials;
import com.wurmonline.shared.util.MaterialUtilities;

/**
 * The single local Smart Improve lookup table. improveIconId contains a source
 * template's imageNumber, not its template ID. Images are not unique: some metal
 * lumps, shard families, leather/pelt, and carving knife/stone chisel collide.
 * Therefore no image is interpreted without the improved item's material.
 */
public final class ImproveMaterialCompatibilityTable {
    // ItemTemplate.imageNumber values sent both as InventoryMetaItem.type for
    // inventory candidates and as InventoryMetaItem.improveIconId for targets.
    private static final short IMAGE_HAND = 4;
    private static final short IMAGE_WATER = 540;
    private static final short IMAGE_CLAY = 591;
    private static final short IMAGE_LEATHER_OR_PELT = 602;
    private static final short IMAGE_LOG = 606;
    private static final short IMAGE_SHARDS = 610;
    private static final short IMAGE_CLOTH_OR_WOOL = 620;
    private static final short IMAGE_LUMP_SERYLL = 630;
    private static final short IMAGE_LUMP_GOLD_OR_ELECTRUM = 631;
    private static final short IMAGE_LUMP_SILVER = 632;
    private static final short IMAGE_LUMP_IRON = 633;
    private static final short IMAGE_LUMP_LEAD = 634;
    private static final short IMAGE_LUMP_ZINC = 635;
    private static final short IMAGE_LUMP_COPPER = 636;
    private static final short IMAGE_LUMP_TIN = 637;
    private static final short IMAGE_LUMP_GLIMMERSTEEL = 638;
    private static final short IMAGE_LUMP_ADAMANTINE = 639;
    private static final short IMAGE_LUMP_BRONZE = 671;
    private static final short IMAGE_LUMP_STEEL = 672;
    private static final short IMAGE_LUMP_BRASS = 673;
    private static final short IMAGE_MALLET = 741;
    private static final short IMAGE_HAMMER = 742;
    private static final short IMAGE_SCISSORS = 748;
    private static final short IMAGE_FILE = 749;
    private static final short IMAGE_AWL = 754;
    private static final short IMAGE_LEATHER_KNIFE = 766;
    private static final short IMAGE_NEEDLE = 788;
    private static final short IMAGE_CLAY_SHAPER = 802;
    private static final short IMAGE_WHETSTONE = 803;
    private static final short IMAGE_SPATULA = 808;
    private static final short IMAGE_CARVING_KNIFE_OR_STONE_CHISEL = 1201;
    private static final short IMAGE_SANDSTONE_SHARDS = 1449;

    public ResourceRequirement resolve(short improveIconId, byte targetMaterial,
                                       String targetType)
            throws UnsupportedImproveMaterialException {
        if (improveIconId == IMAGE_LOG && MaterialUtilities.isWood(targetMaterial))
            // Any wood species is valid as the Improve log source.
            return new ResourceRequirement(RequirementFamily.LOG, null,
                    IMAGE_LOG, false, "log");
        if (isLumpIndicator(improveIconId) && MaterialUtilities.isMetal(targetMaterial))
            return lump(targetMaterial, targetType, improveIconId);

        // These two indicators are deliberately interpreted as one ambiguous
        // group. The target material decides which actual source template to use.
        if (improveIconId == IMAGE_LEATHER_OR_PELT) {
            if (MaterialUtilities.isLeather(targetMaterial))
                return new ResourceRequirement(RequirementFamily.LEATHER,
                        ItemMaterials.MATERIAL_LEATHER, IMAGE_LEATHER_OR_PELT,
                        true, "leather");
            if (MaterialUtilities.isMetal(targetMaterial)
                    || MaterialUtilities.isWood(targetMaterial))
                return tool(RequirementFamily.PELT,
                        "pelt", "large rat pelt");
            throw unsupported(improveIconId, targetMaterial, targetType);
        }

        if (improveIconId == IMAGE_CLOTH_OR_WOOL
                && MaterialUtilities.isCloth(targetMaterial))
            return cloth(targetMaterial, targetType, improveIconId);
        if (improveIconId == IMAGE_CLAY && MaterialUtilities.isClay(targetMaterial))
            return material(RequirementFamily.CLAY, ItemMaterials.MATERIAL_CLAY,
                    IMAGE_CLAY, "clay");
        if (isShardIndicator(improveIconId) && MaterialUtilities.isStone(targetMaterial))
            return shard(targetMaterial, targetType, improveIconId);

        if (improveIconId == IMAGE_CARVING_KNIFE_OR_STONE_CHISEL) {
            if (MaterialUtilities.isWood(targetMaterial))
                return tool(RequirementFamily.CARVING_KNIFE,
                        "carving knife");
            if (MaterialUtilities.isStone(targetMaterial))
                return tool(RequirementFamily.STONE_CHISEL,
                        "stone chisel");
            throw unsupported(improveIconId, targetMaterial, targetType);
        }
        if (improveIconId == IMAGE_MALLET
                && (MaterialUtilities.isWood(targetMaterial)
                || MaterialUtilities.isLeather(targetMaterial)))
            return tool(RequirementFamily.MALLET, "mallet");
        if (improveIconId == IMAGE_FILE && MaterialUtilities.isWood(targetMaterial))
            return tool(RequirementFamily.FILE, "file");
        if (improveIconId == IMAGE_WHETSTONE && MaterialUtilities.isMetal(targetMaterial))
            return tool(RequirementFamily.WHETSTONE, "whetstone");
        if (improveIconId == IMAGE_HAMMER && MaterialUtilities.isMetal(targetMaterial))
            return tool(RequirementFamily.HAMMER, "hammer");
        if (improveIconId == IMAGE_WATER && (MaterialUtilities.isMetal(targetMaterial)
                || MaterialUtilities.isCloth(targetMaterial)
                || MaterialUtilities.isClay(targetMaterial)))
            return new ResourceRequirement(RequirementFamily.WATER,
                    ItemMaterials.MATERIAL_WATER, IMAGE_WATER, false, "water");
        if (improveIconId == IMAGE_NEEDLE && (MaterialUtilities.isLeather(targetMaterial)
                || MaterialUtilities.isCloth(targetMaterial)))
            return tool(RequirementFamily.NEEDLE, "needle");
        if (improveIconId == IMAGE_AWL && MaterialUtilities.isLeather(targetMaterial))
            return tool(RequirementFamily.AWL, "awl");
        if (improveIconId == IMAGE_LEATHER_KNIFE
                && MaterialUtilities.isLeather(targetMaterial))
            return tool(RequirementFamily.LEATHER_KNIFE,
                    "leather knife");
        if (improveIconId == IMAGE_SCISSORS && MaterialUtilities.isCloth(targetMaterial))
            return tool(RequirementFamily.SCISSORS, "scissors");
        if (improveIconId == IMAGE_HAND && MaterialUtilities.isClay(targetMaterial))
            return new ResourceRequirement(RequirementFamily.BODY_HAND, null,
                    IMAGE_HAND, false);
        if (improveIconId == IMAGE_CLAY_SHAPER
                && MaterialUtilities.isClay(targetMaterial))
            return tool(RequirementFamily.CLAY_SHAPER,
                    "clay shaper");
        if (improveIconId == IMAGE_SPATULA && MaterialUtilities.isClay(targetMaterial))
            return tool(RequirementFamily.SPATULA, "spatula");

        throw unsupported(improveIconId, targetMaterial, targetType);
    }

    /** Resolves an Examine phrase for a world item through the same strict table. */
    public ResourceRequirement resolve(RequirementFamily family, byte targetMaterial,
                                       String targetType)
            throws UnsupportedImproveMaterialException {
        if (family == null) throw unsupported((short) -1, targetMaterial, targetType);
        switch (family) {
            case LOG:
                return resolve(IMAGE_LOG, targetMaterial, targetType);
            case LUMP:
                if (MaterialUtilities.isMetal(targetMaterial))
                    return lump(targetMaterial, targetType, IMAGE_LUMP_IRON);
                break;
            case LEATHER:
                if (MaterialUtilities.isLeather(targetMaterial))
                    return new ResourceRequirement(RequirementFamily.LEATHER,
                            ItemMaterials.MATERIAL_LEATHER,
                            IMAGE_LEATHER_OR_PELT, true, "leather");
                break;
            case PELT:
                if (MaterialUtilities.isMetal(targetMaterial)
                        || MaterialUtilities.isWood(targetMaterial))
                    return tool(RequirementFamily.PELT, "pelt", "large rat pelt");
                break;
            case STRING:
                return cloth(targetMaterial, targetType, IMAGE_CLOTH_OR_WOOL);
            case CLAY:
                return resolve(IMAGE_CLAY, targetMaterial, targetType);
            case SHARD:
                return shard(targetMaterial, targetType, IMAGE_SHARDS);
            case CARVING_KNIFE:
                if (MaterialUtilities.isWood(targetMaterial))
                    return tool(family, "carving knife");
                break;
            case STONE_CHISEL:
                if (MaterialUtilities.isStone(targetMaterial))
                    return tool(family, "stone chisel");
                break;
            case MALLET:
                if (MaterialUtilities.isWood(targetMaterial)
                        || MaterialUtilities.isLeather(targetMaterial))
                    return tool(family, "mallet");
                break;
            case FILE:
                if (MaterialUtilities.isWood(targetMaterial)) return tool(family, "file");
                break;
            case WHETSTONE:
                if (MaterialUtilities.isMetal(targetMaterial))
                    return tool(family, "whetstone");
                break;
            case HAMMER:
                if (MaterialUtilities.isMetal(targetMaterial)) return tool(family, "hammer");
                break;
            case WATER:
                return resolve(IMAGE_WATER, targetMaterial, targetType);
            case NEEDLE:
                if (MaterialUtilities.isLeather(targetMaterial)
                        || MaterialUtilities.isCloth(targetMaterial))
                    return tool(family, "needle");
                break;
            case AWL:
                if (MaterialUtilities.isLeather(targetMaterial)) return tool(family, "awl");
                break;
            case LEATHER_KNIFE:
                if (MaterialUtilities.isLeather(targetMaterial))
                    return tool(family, "leather knife");
                break;
            case SCISSORS:
                if (MaterialUtilities.isCloth(targetMaterial))
                    return tool(family, "scissors");
                break;
            case BODY_HAND:
                return resolve(IMAGE_HAND, targetMaterial, targetType);
            case CLAY_SHAPER:
                if (MaterialUtilities.isClay(targetMaterial))
                    return tool(family, "clay shaper");
                break;
            case SPATULA:
                if (MaterialUtilities.isClay(targetMaterial))
                    return tool(family, "spatula");
                break;
            default:
                break;
        }
        throw unsupported((short) -1, targetMaterial, targetType);
    }

    private static ResourceRequirement shard(byte material, String targetType,
                                             short icon)
            throws UnsupportedImproveMaterialException {
        switch (material) {
            case ItemMaterials.MATERIAL_STONE:
                return material(RequirementFamily.SHARD, material, IMAGE_SHARDS,
                        "rock shards");
            case ItemMaterials.MATERIAL_SLATE:
                return material(RequirementFamily.SHARD, material, IMAGE_SHARDS,
                        "slate shard", "slate shards", "shards");
            case ItemMaterials.MATERIAL_MARBLE:
                return material(RequirementFamily.SHARD, material, IMAGE_SHARDS,
                        "marble shard", "marble shards", "shards");
            case ItemMaterials.MATERIAL_SANDSTONE:
                return material(RequirementFamily.SHARD, material,
                        IMAGE_SANDSTONE_SHARDS,
                        "sandstone shard", "sandstone shards", "sandstone",
                        "shards");
            default:
                throw unsupported(icon, material, targetType);
        }
    }

    private static ResourceRequirement lump(byte material, String targetType,
                                            short icon)
            throws UnsupportedImproveMaterialException {
        short sourceImage;
        switch (material) {
            case ItemMaterials.MATERIAL_GOLD:
            case ItemMaterials.MATERIAL_ELECTRUM:
                sourceImage = IMAGE_LUMP_GOLD_OR_ELECTRUM; break;
            case ItemMaterials.MATERIAL_SILVER: sourceImage = IMAGE_LUMP_SILVER; break;
            case ItemMaterials.MATERIAL_STEEL: sourceImage = IMAGE_LUMP_STEEL; break;
            case ItemMaterials.MATERIAL_COPPER: sourceImage = IMAGE_LUMP_COPPER; break;
            case ItemMaterials.MATERIAL_IRON: sourceImage = IMAGE_LUMP_IRON; break;
            case ItemMaterials.MATERIAL_LEAD: sourceImage = IMAGE_LUMP_LEAD; break;
            case ItemMaterials.MATERIAL_ZINC: sourceImage = IMAGE_LUMP_ZINC; break;
            case ItemMaterials.MATERIAL_BRASS: sourceImage = IMAGE_LUMP_BRASS; break;
            case ItemMaterials.MATERIAL_BRONZE: sourceImage = IMAGE_LUMP_BRONZE; break;
            case ItemMaterials.MATERIAL_TIN: sourceImage = IMAGE_LUMP_TIN; break;
            case ItemMaterials.MATERIAL_ADAMANTINE:
                sourceImage = IMAGE_LUMP_ADAMANTINE; break;
            case ItemMaterials.MATERIAL_GLIMMERSTEEL:
                sourceImage = IMAGE_LUMP_GLIMMERSTEEL; break;
            case ItemMaterials.MATERIAL_SERYLL: sourceImage = IMAGE_LUMP_SERYLL; break;
            default: throw unsupported(icon, material, targetType);
        }
        return material(RequirementFamily.LUMP, material, sourceImage,
                "lump", "metal lump");
    }

    private static ResourceRequirement cloth(byte material, String targetType,
                                             short icon)
            throws UnsupportedImproveMaterialException {
        switch (material) {
            case ItemMaterials.MATERIAL_COTTON:
                return material(RequirementFamily.STRING, material,
                        IMAGE_CLOTH_OR_WOOL,
                        "string", "string of cloth");
            case ItemMaterials.MATERIAL_WOOL:
                return material(RequirementFamily.STRING, material,
                        IMAGE_CLOTH_OR_WOOL, "wool");
            default:
                throw unsupported(icon, material, targetType);
        }
    }

    private static boolean isLumpIndicator(short icon) {
        return icon == IMAGE_LUMP_GOLD_OR_ELECTRUM
                || icon == IMAGE_LUMP_SILVER || icon == IMAGE_LUMP_STEEL
                || icon == IMAGE_LUMP_COPPER || icon == IMAGE_LUMP_IRON
                || icon == IMAGE_LUMP_LEAD || icon == IMAGE_LUMP_ZINC
                || icon == IMAGE_LUMP_BRASS || icon == IMAGE_LUMP_BRONZE
                || icon == IMAGE_LUMP_TIN || icon == IMAGE_LUMP_ADAMANTINE
                || icon == IMAGE_LUMP_GLIMMERSTEEL || icon == IMAGE_LUMP_SERYLL;
    }

    private static boolean isShardIndicator(short icon) {
        return icon == IMAGE_SHARDS || icon == IMAGE_SANDSTONE_SHARDS;
    }

    private static ResourceRequirement material(RequirementFamily family,
                                                byte material, short image,
                                                String... names) {
        return new ResourceRequirement(family, material, image, false, names);
    }

    private static ResourceRequirement tool(RequirementFamily family,
                                            String... names) {
        // Tool images can collide. The canonical inventory base name is the
        // stable local tool type; tool material and state are irrelevant.
        return new ResourceRequirement(family, null, null, false, names);
    }

    private static UnsupportedImproveMaterialException unsupported(
            short icon, byte material, String targetType) {
        return new UnsupportedImproveMaterialException(
                "Unsupported local Improve combination: icon " + icon
                        + ", material " + (material & 0xff)
                        + ", target " + ImproveTargetFingerprint.normalize(targetType));
    }
}
