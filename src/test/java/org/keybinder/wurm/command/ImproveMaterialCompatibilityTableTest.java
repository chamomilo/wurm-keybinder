package org.keybinder.wurm.command;

import com.wurmonline.shared.constants.ItemMaterials;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class ImproveMaterialCompatibilityTableTest {
    private final ImproveMaterialCompatibilityTable table =
            new ImproveMaterialCompatibilityTable();
    private final ImproveResourceResolver resolver = new ImproveResourceResolver();

    @Test
    public void worldEventFamiliesUseTheSameMaterialSpecificRules()
            throws Exception {
        assertEquals("MARBLE SHARD", table.resolve(RequirementFamily.SHARD,
                ItemMaterials.MATERIAL_MARBLE, "baking stone")
                .getMissingResourceLabel());
        assertEquals("STEEL LUMP", table.resolve(RequirementFamily.LUMP,
                ItemMaterials.MATERIAL_STEEL, "lamp")
                .getMissingResourceLabel());
        assertEquals(RequirementFamily.CARVING_KNIFE,
                table.resolve(RequirementFamily.CARVING_KNIFE,
                        ItemMaterials.MATERIAL_WOOD_CHESTNUT, "wagon")
                        .getFamily());
        assertEquals(RequirementFamily.HAMMER,
                table.resolve(RequirementFamily.HAMMER,
                        ItemMaterials.MATERIAL_GOLD, "golden altar")
                        .getFamily());
    }

    @Test
    public void missingResourceLabelsNameMaterialSpecificConsumables()
            throws Exception {
        assertEquals("MARBLE SHARD", rule((short) 610,
                ItemMaterials.MATERIAL_MARBLE, "baking stone")
                .getMissingResourceLabel());
        assertEquals("ROCK SHARDS", rule((short) 610,
                ItemMaterials.MATERIAL_STONE, "stone item")
                .getMissingResourceLabel());
        assertEquals("SLATE SHARD", rule((short) 610,
                ItemMaterials.MATERIAL_SLATE, "slate item")
                .getMissingResourceLabel());
        assertEquals("SANDSTONE SHARD", rule((short) 1449,
                ItemMaterials.MATERIAL_SANDSTONE, "sandstone item")
                .getMissingResourceLabel());
        assertEquals("STEEL LUMP", rule((short) 672,
                ItemMaterials.MATERIAL_STEEL, "steel item")
                .getMissingResourceLabel());
        assertEquals("COTTON STRING", rule((short) 620,
                ItemMaterials.MATERIAL_COTTON, "cloth item")
                .getMissingResourceLabel());
        assertEquals("WOOL", rule((short) 620,
                ItemMaterials.MATERIAL_WOOL, "wool item")
                .getMissingResourceLabel());
    }

    @Test
    public void improveLogAcceptsAnyWoodSpeciesAndUsesGenericLabel()
            throws Exception {
        ResourceRequirement log = rule((short) 606,
                ItemMaterials.MATERIAL_WOOD_CHESTNUT, "rope tool");
        ImproveResourceCandidate birchLog = item(19, "log", "log, birchwood",
                ItemMaterials.MATERIAL_WOOD_BIRCH, (short) 606, (byte) 0);

        assertNull(log.getExactMaterialId());
        assertEquals("LOG", log.getMissingResourceLabel());
        assertTrue(log.match(birchLog).isAccepted());
    }

    @Test
    public void sharedLumpIndicatorUsesTargetMaterialAndSkipsColdCandidates()
            throws Exception {
        ResourceRequirement steel = rule((short) 672, ItemMaterials.MATERIAL_STEEL,
                "carving knife");
        ImproveResourceCandidate iron = item(1, "lump", "lump (glowing), iron",
                ItemMaterials.MATERIAL_IRON, (short) 633, (byte) 5);
        ImproveResourceCandidate coldSteel = item(2, "lump", "lump, steel",
                ItemMaterials.MATERIAL_STEEL, (short) 672, (byte) 0);
        ImproveResourceCandidate hotSteel = item(3, "lump", "lump (glowing), steel",
                ItemMaterials.MATERIAL_STEEL, (short) 672, (byte) 5);

        assertEquals(RequirementFamily.LUMP, steel.getFamily());
        assertEquals(Short.valueOf((short) 672), steel.getExactImageId());
        assertFalse(steel.match(iron).isAccepted());
        assertFalse(steel.match(coldSteel).isAccepted());
        assertSame(hotSteel, resolver.resolve(
                inventory(iron, coldSteel, hotSteel), steel, null).getCandidate());
    }

    @Test
    public void everyLumpIndicatorStillSelectsImageFromTargetMaterial()
            throws Exception {
        ResourceRequirement steelFromGoldIndicator = rule((short) 631,
                ItemMaterials.MATERIAL_STEEL, "carving knife");
        ResourceRequirement electrumFromSteelIndicator = rule((short) 672,
                ItemMaterials.MATERIAL_ELECTRUM, "sword");

        assertEquals(Short.valueOf((short) 672),
                steelFromGoldIndicator.getExactImageId());
        assertEquals(Short.valueOf((short) 631),
                electrumFromSteelIndicator.getExactImageId());
    }

    @Test
    public void allServerLumpMaterialsMapToTheirExactSourceImage()
            throws Exception {
        byte[] materials = new byte[]{
                ItemMaterials.MATERIAL_GOLD,
                ItemMaterials.MATERIAL_SILVER,
                ItemMaterials.MATERIAL_STEEL,
                ItemMaterials.MATERIAL_COPPER,
                ItemMaterials.MATERIAL_IRON,
                ItemMaterials.MATERIAL_LEAD,
                ItemMaterials.MATERIAL_ZINC,
                ItemMaterials.MATERIAL_BRASS,
                ItemMaterials.MATERIAL_BRONZE,
                ItemMaterials.MATERIAL_TIN,
                ItemMaterials.MATERIAL_ADAMANTINE,
                ItemMaterials.MATERIAL_GLIMMERSTEEL,
                ItemMaterials.MATERIAL_SERYLL,
                ItemMaterials.MATERIAL_ELECTRUM
        };
        short[] imageIds = new short[]{
                631, 632, 672, 636, 633, 634, 635, 673, 671, 637,
                639, 638, 630, 631
        };

        for (int i = 0; i < materials.length; i++) {
            ResourceRequirement requirement = rule(imageIds[i], materials[i],
                    "metal target");
            assertEquals(Byte.valueOf(materials[i]), requirement.getExactMaterialId());
            assertEquals(Short.valueOf(imageIds[i]),
                    requirement.getExactImageId());
        }
    }

    @Test
    public void shardSourceImageComesFromTargetMaterialNotTheSharedIndicator()
            throws Exception {
        ResourceRequirement marble = rule((short) 610,
                ItemMaterials.MATERIAL_MARBLE, "marble slab");
        ResourceRequirement stone = rule((short) 610,
                ItemMaterials.MATERIAL_STONE, "stone slab");

        assertEquals(Short.valueOf((short) 610), marble.getExactImageId());
        assertEquals(Short.valueOf((short) 610), stone.getExactImageId());
        assertTrue(marble.match(item(1, "marble shard", "marble shard",
                ItemMaterials.MATERIAL_MARBLE, (short) 610, (byte) 0)).isAccepted());
        assertFalse(marble.match(item(2, "rock shards", "rock shards",
                ItemMaterials.MATERIAL_STONE, (short) 610, (byte) 0)).isAccepted());

        assertTrue(stone.match(item(3, "rock shards", "rock shards",
                ItemMaterials.MATERIAL_STONE, (short) 610, (byte) 0)).isAccepted());
        assertFalse(stone.match(item(4, "rift stone shard", "rift stone shard",
                ItemMaterials.MATERIAL_STONE, (short) 610, (byte) 0)).isAccepted());

        assertEquals(Short.valueOf((short) 610), rule((short) 610,
                ItemMaterials.MATERIAL_SLATE, "slate slab").getExactImageId());
        assertEquals(Short.valueOf((short) 1449), rule((short) 1449,
                ItemMaterials.MATERIAL_SANDSTONE,
                "sandstone slab").getExactImageId());
    }

    @Test
    public void ambiguousLeatherPeltIndicatorsAreDisambiguatedByTargetMaterial()
            throws Exception {
        ResourceRequirement metalPelt = rule((short) 602,
                ItemMaterials.MATERIAL_IRON, "longsword");
        ResourceRequirement woodPelt = rule((short) 602,
                ItemMaterials.MATERIAL_WOOD_BIRCH, "rope tool");
        ResourceRequirement leather = rule((short) 602,
                ItemMaterials.MATERIAL_LEATHER, "leather glove");

        assertEquals(RequirementFamily.PELT, metalPelt.getFamily());
        assertNull(metalPelt.getExactImageId());
        assertEquals(RequirementFamily.PELT, woodPelt.getFamily());
        assertEquals(RequirementFamily.LEATHER, leather.getFamily());
        assertEquals(Short.valueOf((short) 602), leather.getExactImageId());

        ImproveResourceCandidate pelt = item(10, "pelt", "large rat pelt",
                ItemMaterials.MATERIAL_LEATHER, (short) 602, (byte) 0);
        ImproveResourceCandidate ordinaryLeather = item(11, "leather", "leather",
                ItemMaterials.MATERIAL_LEATHER, (short) 602, (byte) 0);
        assertSame(pelt, resolver.resolve(inventory(ordinaryLeather, pelt),
                metalPelt, null).getCandidate());
        assertSame(ordinaryLeather, resolver.resolve(inventory(pelt, ordinaryLeather),
                leather, null).getCandidate());
    }

    @Test
    public void peltLeatherAndDrakeHideAreSeparatedByNormalizedBaseName()
            throws Exception {
        ResourceRequirement peltRequirement = rule((short) 602,
                ItemMaterials.MATERIAL_STEEL, "longsword");
        ResourceRequirement leatherRequirement = rule((short) 602,
                ItemMaterials.MATERIAL_LEATHER, "leather glove");
        ImproveResourceCandidate decoratedPelt = item(12,
                "large mountain lion pelt (glowing)",
                "rare large mountain lion pelt (glowing)", (byte) 55,
                (short) 602, (byte) 5);
        ImproveResourceCandidate decoratedLeather = item(13,
                "leather (glowing)", "rare leather (glowing)",
                ItemMaterials.MATERIAL_LEATHER, (short) 602, (byte) 5);
        ImproveResourceCandidate redDrakeHide = coloured(14,
                "drake hide", "rare drake hide",
                ItemMaterials.MATERIAL_LEATHER, (short) 602,
                215, 40, 40);

        assertTrue(peltRequirement.match(decoratedPelt).isAccepted());
        assertFalse(peltRequirement.match(decoratedLeather).isAccepted());
        assertFalse(peltRequirement.match(redDrakeHide).isAccepted());
        assertTrue(leatherRequirement.match(decoratedLeather).isAccepted());
        assertFalse(leatherRequirement.match(decoratedPelt).isAccepted());
        assertFalse(leatherRequirement.match(redDrakeHide).isAccepted());
    }

    @Test
    public void dragonDrakeAndScaleLeatherNeverReplaceOrdinaryLeather()
            throws Exception {
        ResourceRequirement leather = rule((short) 602,
                ItemMaterials.MATERIAL_LEATHER, "dragon leather jacket");
        ImproveResourceCandidate ordinary = coloured(1, "leather", "leather",
                ItemMaterials.MATERIAL_LEATHER, (short) 602, 0, 0, 0);
        ImproveResourceCandidate dragonName = coloured(2, "leather",
                "red dragon leather", ItemMaterials.MATERIAL_LEATHER,
                (short) 602, 0, 0, 0);
        ImproveResourceCandidate drake = coloured(3, "leather", "drake hide",
                ItemMaterials.MATERIAL_LEATHER, (short) 602, 0, 0, 0);
        ImproveResourceCandidate realDrake = coloured(10, "drake hide",
                "drake hide", ItemMaterials.MATERIAL_LEATHER,
                (short) 602, 215, 40, 40);
        ImproveResourceCandidate scale = coloured(4, "leather", "dragon scale",
                ItemMaterials.MATERIAL_LEATHER, (short) 602, 0, 0, 0);
        ImproveResourceCandidate greenDragon = coloured(5, "leather", "leather",
                ItemMaterials.MATERIAL_LEATHER, (short) 602, 10, 210, 10);
        ImproveResourceCandidate blackDragon = coloured(6, "leather", "leather",
                ItemMaterials.MATERIAL_LEATHER, (short) 602, 10, 10, 10);
        ImproveResourceCandidate whiteDragon = coloured(7, "leather", "leather",
                ItemMaterials.MATERIAL_LEATHER, (short) 602, 255, 255, 255);
        ImproveResourceCandidate redDragon = coloured(8, "leather", "leather",
                ItemMaterials.MATERIAL_LEATHER, (short) 602, 215, 40, 40);
        ImproveResourceCandidate blueDragon = coloured(9, "leather", "leather",
                ItemMaterials.MATERIAL_LEATHER, (short) 602, 40, 40, 215);

        assertTrue(leather.match(ordinary).isAccepted());
        assertFalse(leather.match(dragonName).isAccepted());
        assertFalse(leather.match(drake).isAccepted());
        assertFalse(leather.match(realDrake).isAccepted());
        assertFalse(leather.match(scale).isAccepted());
        assertFalse(leather.match(greenDragon).isAccepted());
        assertFalse(leather.match(blackDragon).isAccepted());
        assertFalse(leather.match(whiteDragon).isAccepted());
        assertFalse(leather.match(redDragon).isAccepted());
        assertFalse(leather.match(blueDragon).isAccepted());
        assertSame(ordinary, resolver.resolve(inventory(dragonName, drake, realDrake,
                scale, greenDragon, blackDragon, whiteDragon, redDragon,
                blueDragon, ordinary), leather, null).getCandidate());
        assertNull(resolver.resolve(inventory(dragonName, drake, realDrake, scale,
                greenDragon, blackDragon, whiteDragon, redDragon, blueDragon),
                leather, null));
    }

    @Test
    public void everyInstalledDragonHideAndScaleColourIsExcluded()
            throws Exception {
        ResourceRequirement leather = rule((short) 602,
                ItemMaterials.MATERIAL_LEATHER, "leather glove");
        int[][] colours = new int[][] {
                {10, 210, 10}, {10, 10, 10}, {255, 255, 255},
                {215, 40, 40}, {40, 40, 215}
        };
        for (int i = 0; i < colours.length; i++) {
            ImproveResourceCandidate drakeHide = coloured(100 + i,
                    "drake hide", "drake hide",
                    ItemMaterials.MATERIAL_LEATHER, (short) 602,
                    colours[i][0], colours[i][1], colours[i][2]);
            ImproveResourceCandidate dragonScale = coloured(110 + i,
                    "scale", "dragon scale",
                    ItemMaterials.MATERIAL_LEATHER, (short) 554,
                    colours[i][0], colours[i][1], colours[i][2]);
            ImproveResourceCandidate disguisedDragonLeather = coloured(120 + i,
                    "leather", "leather",
                    ItemMaterials.MATERIAL_LEATHER, (short) 602,
                    colours[i][0], colours[i][1], colours[i][2]);

            assertFalse(leather.match(drakeHide).isAccepted());
            assertFalse(leather.match(dragonScale).isAccepted());
            assertFalse(leather.match(disguisedDragonLeather).isAccepted());
        }
    }

    @Test
    public void clothAndPotteryConsumablesUseTheirActualSourceImages()
            throws Exception {
        ResourceRequirement wool = rule((short) 620, ItemMaterials.MATERIAL_WOOL,
                "wool glove");
        ResourceRequirement potteryClay = rule((short) 591,
                ItemMaterials.MATERIAL_POTTERY, "pottery bowl");

        assertEquals(Short.valueOf((short) 620), wool.getExactImageId());
        assertEquals(Byte.valueOf(ItemMaterials.MATERIAL_WOOL),
                wool.getExactMaterialId());
        assertEquals(Short.valueOf((short) 591), potteryClay.getExactImageId());
        assertEquals(Byte.valueOf(ItemMaterials.MATERIAL_CLAY),
                potteryClay.getExactMaterialId());
    }

    @Test
    public void toolsRequireCanonicalTypeAndValidTargetMaterialCombination()
            throws Exception {
        ResourceRequirement hammer = rule((short) 742,
                ItemMaterials.MATERIAL_STEEL, "longsword");
        ImproveResourceCandidate renamedHammer = item(1, "custom tool", "rare custom tool",
                ItemMaterials.MATERIAL_GOLD, (short) 742, (byte) 0);
        assertFalse(hammer.match(renamedHammer).isAccepted());

        try {
            rule((short) 742, ItemMaterials.MATERIAL_WOOD_BIRCH, "rope tool");
            fail("metal hammer plus wood must fail closed");
        } catch (UnsupportedImproveMaterialException expected) {
            assertTrue(expected.getMessage().contains("icon 742"));
        }
    }

    @Test
    public void realWhetstoneImage803AndSteelLumpImage672ResolveCorrectly()
            throws Exception {
        ResourceRequirement whetstone = rule((short) 803,
                ItemMaterials.MATERIAL_STEEL, "shovel");
        ResourceRequirement lump = rule((short) 672,
                ItemMaterials.MATERIAL_STEEL, "shovel");

        assertEquals(RequirementFamily.WHETSTONE, whetstone.getFamily());
        assertNull(whetstone.getExactImageId());
        assertEquals(RequirementFamily.LUMP, lump.getFamily());
        assertEquals(Short.valueOf((short) 672), lump.getExactImageId());
    }

    @Test
    public void goldLumpAndAnyMaterialHammerAreFoundInInventoryAndBackpack()
            throws Exception {
        ResourceRequirement goldLump = rule((short) 631,
                ItemMaterials.MATERIAL_GOLD, "ring");
        ResourceRequirement hammer = rule((short) 742,
                ItemMaterials.MATERIAL_GOLD, "ring");
        ImproveResourceCandidate hotGold = item(40, "lump",
                "lump (glowing), gold", ItemMaterials.MATERIAL_GOLD,
                (short) 631, (byte) 5);
        ImproveResourceCandidate bronzeHammer = item(41, "hammer",
                "rare hammer, bronze", ItemMaterials.MATERIAL_BRONZE,
                (short) 1999, (byte) 0);
        ImproveResourceCandidate inventory = inventory(hotGold,
                backpack(42, bronzeHammer));

        assertSame(hotGold, resolver.resolve(inventory, goldLump, null).getCandidate());
        assertSame(bronzeHammer,
                resolver.resolve(inventory, hammer, null).getCandidate());
    }

    @Test
    public void sharedCarvingKnifeAndStoneChiselImageUsesCanonicalBaseName()
            throws Exception {
        ResourceRequirement carving = rule((short) 1201,
                ItemMaterials.MATERIAL_WOOD_BIRCH, "wooden item");
        ResourceRequirement chisel = rule((short) 1201,
                ItemMaterials.MATERIAL_STONE, "stone item");
        ImproveResourceCandidate carvingKnife = item(50, "carving knife",
                "carving knife, iron", ItemMaterials.MATERIAL_IRON,
                (short) 1201, (byte) 0);
        ImproveResourceCandidate stoneChisel = item(51, "stone chisel",
                "stone chisel, iron", ItemMaterials.MATERIAL_IRON,
                (short) 1201, (byte) 0);

        assertSame(carvingKnife, resolver.resolve(inventory(stoneChisel,
                carvingKnife), carving, null).getCandidate());
        assertSame(stoneChisel, resolver.resolve(inventory(carvingKnife,
                stoneChisel), chisel, null).getCandidate());
    }

    @Test
    public void craftedRareCarvingKnifeMatchesByToolTypeDespiteRecipeIcon()
            throws Exception {
        ResourceRequirement carving = rule((short) 1201,
                ItemMaterials.MATERIAL_WOOD_CHESTNUT, "rope tool");
        ImproveResourceCandidate rareSteelKnife = item(52, "carving knife",
                "rare carving knife, steel", ItemMaterials.MATERIAL_STEEL,
                (short) 1998, (byte) 0);

        assertSame(rareSteelKnife, resolver.resolve(inventory(rareSteelKnife),
                carving, null).getCandidate());
    }

    @Test
    public void glowingAndDecoratedCarvingKnifeNameStillMatchesToolType()
            throws Exception {
        ResourceRequirement carving = rule((short) 1201,
                ItemMaterials.MATERIAL_WOOD_CHESTNUT, "rope tool");
        ImproveResourceCandidate decorated = item(53,
                "carving knife (glowing)",
                "rare carving knife (glowing), steel",
                ItemMaterials.MATERIAL_STEEL, (short) 1997, (byte) 5);
        ImproveResourceCandidate differentType = item(54,
                "carving knife blade", "rare carving knife blade, steel",
                ItemMaterials.MATERIAL_STEEL, (short) 1201, (byte) 0);

        assertTrue(carving.match(decorated).isAccepted());
        assertFalse(carving.match(differentType).isAccepted());
        assertSame(decorated, resolver.resolve(inventory(differentType, decorated),
                carving, null).getCandidate());
    }

    @Test
    public void carvingKnifeNormalizationTableAcceptsOnlyTheWholeTypePhrase()
            throws Exception {
        ResourceRequirement carving = rule((short) 1201,
                ItemMaterials.MATERIAL_WOOD_CHESTNUT, "rope tool");
        String[] accepted = new String[] {
                "carving knife",
                "Carving Knife",
                "  carving\u00a0knife  ",
                "rare carving knife, steel",
                "rare steel carving knife",
                "carving knife (glowing)",
                "rare steel carving knife (glowing), custom"
        };
        String[] rejected = new String[] {
                "stone chisel",
                "carving knife blade",
                "leather knife",
                "carving knives",
                "carving knifed",
                "knife"
        };
        long id = 200;
        for (String baseName : accepted)
            assertTrue(baseName, carving.match(item(id++, baseName, baseName,
                    ItemMaterials.MATERIAL_STEEL, (short) 1201,
                    (byte) 0)).isAccepted());
        for (String baseName : rejected)
            assertFalse(baseName, carving.match(item(id++, baseName, baseName,
                    ItemMaterials.MATERIAL_STEEL, (short) 1201,
                    (byte) 0)).isAccepted());
    }

    @Test
    public void sharedImageCannotIdentifyCarvingKnifeWithoutBaseName()
            throws Exception {
        ResourceRequirement carving = rule((short) 1201,
                ItemMaterials.MATERIAL_WOOD_CHESTNUT, "rope tool");
        ImproveResourceCandidate chisel = item(55, "stone chisel",
                "rare stone chisel, steel", ItemMaterials.MATERIAL_STEEL,
                (short) 1201, (byte) 0);

        assertFalse(carving.match(chisel).isAccepted());
        assertNull(resolver.resolve(inventory(chisel), carving, null));
    }

    @Test
    public void completeServerCreationStateMatrixResolvesLocally()
            throws Exception {
        assertFamilies(ItemMaterials.MATERIAL_WOOD_BIRCH,
                new short[]{1201, 741, 749, 602},
                new RequirementFamily[]{RequirementFamily.CARVING_KNIFE,
                        RequirementFamily.MALLET, RequirementFamily.FILE,
                        RequirementFamily.PELT});
        assertFamilies(ItemMaterials.MATERIAL_STEEL,
                new short[]{803, 742, 540, 602},
                new RequirementFamily[]{RequirementFamily.WHETSTONE,
                        RequirementFamily.HAMMER, RequirementFamily.WATER,
                        RequirementFamily.PELT});
        assertFamilies(ItemMaterials.MATERIAL_LEATHER,
                new short[]{788, 754, 766, 741},
                new RequirementFamily[]{RequirementFamily.NEEDLE,
                        RequirementFamily.AWL, RequirementFamily.LEATHER_KNIFE,
                        RequirementFamily.MALLET});
        assertFamilies(ItemMaterials.MATERIAL_COTTON,
                new short[]{788, 748, 540, 788},
                new RequirementFamily[]{RequirementFamily.NEEDLE,
                        RequirementFamily.SCISSORS, RequirementFamily.WATER,
                        RequirementFamily.NEEDLE});
        assertFamilies(ItemMaterials.MATERIAL_STONE,
                new short[]{1201, 1201, 1201, 1201},
                new RequirementFamily[]{RequirementFamily.STONE_CHISEL,
                        RequirementFamily.STONE_CHISEL,
                        RequirementFamily.STONE_CHISEL,
                        RequirementFamily.STONE_CHISEL});
        assertFamilies(ItemMaterials.MATERIAL_CLAY,
                new short[]{4, 540, 802, 808},
                new RequirementFamily[]{RequirementFamily.BODY_HAND,
                        RequirementFamily.WATER, RequirementFamily.CLAY_SHAPER,
                        RequirementFamily.SPATULA});
    }

    @Test
    public void waterAndBuiltInHandAreExactLocalSources() throws Exception {
        ResourceRequirement water = rule((short) 540,
                ItemMaterials.MATERIAL_CLAY, "clay bowl");
        assertTrue(water.match(item(1, "water", "water",
                ItemMaterials.MATERIAL_WATER, (short) 540, (byte) 0)).isAccepted());

        ResourceRequirement hand = rule((short) 4,
                ItemMaterials.MATERIAL_CLAY, "clay bowl");
        assertEquals(RequirementFamily.BODY_HAND, hand.getFamily());
        assertTrue(hand.match(item(2, "", "hands", (byte) 0,
                (short) 4, (byte) 0)).isAccepted());
    }

    @Test
    public void nestedInventoryAndBackpackResourcesRemainDiscoverable()
            throws Exception {
        ResourceRequirement steel = rule((short) 672, ItemMaterials.MATERIAL_STEEL,
                "needle");
        ImproveResourceCandidate lump = item(4, "lump", "lump (glowing), steel",
                ItemMaterials.MATERIAL_STEEL, (short) 672, (byte) 5);
        ImproveResourceCandidate backpack = new ImproveResourceCandidate(
                3, "backpack", "backpack, leather", (byte) 0, (short) 0,
                0f, 0f, 0f, (byte) 0,
                java.util.Collections.singletonList(lump));
        ImproveResourceCandidate inventory = container(2, "inventory", backpack);

        ResolvedImproveResource selected = resolver.resolve(inventory, steel, null);

        assertSame(lump, selected.getCandidate());
        assertTrue(selected.isNested());
        assertEquals("backpack", selected.getContainerName());
    }

    @Test
    public void nearestRealContainerIsReportedInsteadOfBodyTreeNode()
            throws Exception {
        ResourceRequirement wood = rule((short) 741,
                ItemMaterials.MATERIAL_WOOD_BIRCH, "huge tub");
        ImproveResourceCandidate mallet = item(4, "mallet",
                "mallet, birchwood", ItemMaterials.MATERIAL_WOOD_BIRCH,
                (short) 741, (byte) 0);
        ImproveResourceCandidate backpack = backpack(3, mallet);
        ImproveResourceCandidate body = container(2, "body", backpack);

        ResolvedImproveResource selected = resolver.resolve(
                inventory(body), wood, null);

        assertSame(mallet, selected.getCandidate());
        assertEquals("backpack", selected.getContainerName());
    }

    @Test
    public void bodyAndInventoryTreeNodesAreNotReportedAsContainers()
            throws Exception {
        ResourceRequirement stone = rule((short) 610,
                ItemMaterials.MATERIAL_STONE, "forge");
        ImproveResourceCandidate shards = item(4, "rock shards", "rock shards",
                ItemMaterials.MATERIAL_STONE, (short) 610, (byte) 0);
        ImproveResourceCandidate inventoryNode = container(3, "inventory", shards);
        ImproveResourceCandidate body = container(2, "body", inventoryNode);

        ResolvedImproveResource selected = resolver.resolve(
                inventory(body), stone, null);

        assertSame(shards, selected.getCandidate());
        assertFalse(selected.isNested());
    }

    @Test
    public void searchTraversesAllNestedContainersWhenDirectTierHasNoMatch()
            throws Exception {
        ResourceRequirement steel = rule((short) 672, ItemMaterials.MATERIAL_STEEL,
                "needle");
        ImproveResourceCandidate hidden = item(4, "lump", "lump (glowing), steel",
                ItemMaterials.MATERIAL_STEEL, (short) 672, (byte) 5);
        ImproveResourceCandidate wallet = container(5, "wallet", hidden);
        ImproveResourceCandidate box = container(6, "box", wallet);
        ImproveResourceCandidate satchel = container(3, "satchel", box);

        assertSame(hidden, resolver.resolve(
                inventory(satchel), steel, null).getCandidate());
    }

    @Test
    public void directInventoryAndBackpackItemsWinBeforeNestedContainers()
            throws Exception {
        ResourceRequirement steel = rule((short) 672, ItemMaterials.MATERIAL_STEEL,
                "needle");
        ImproveResourceCandidate nested = item(20, "lump", "nested steel lump",
                ItemMaterials.MATERIAL_STEEL, (short) 672, (byte) 5);
        ImproveResourceCandidate directInventory = item(21, "lump",
                "direct inventory steel lump", ItemMaterials.MATERIAL_STEEL,
                (short) 672, (byte) 5);
        ImproveResourceCandidate directBackpack = item(22, "lump",
                "direct backpack steel lump", ItemMaterials.MATERIAL_STEEL,
                (short) 672, (byte) 5);
        ImproveResourceCandidate satchel = container(23, "satchel", nested);
        ImproveResourceCandidate backpack = backpack(24, directBackpack);

        assertSame(directInventory, resolver.resolve(inventory(satchel,
                backpack, directInventory), steel, null).getCandidate());
        assertSame(directBackpack, resolver.resolve(inventory(satchel,
                backpack), steel, null).getCandidate());
    }

    @Test
    public void rejectedColdDirectLumpDoesNotStopRecursiveSearch() throws Exception {
        ResourceRequirement steel = rule((short) 672, ItemMaterials.MATERIAL_STEEL,
                "needle");
        ImproveResourceCandidate cold = item(30, "lump", "cold steel lump",
                ItemMaterials.MATERIAL_STEEL, (short) 672, (byte) 0);
        ImproveResourceCandidate hot = item(31, "lump", "hot nested steel lump",
                ItemMaterials.MATERIAL_STEEL, (short) 672, (byte) 5);
        ImproveResourceCandidate wallet = container(32, "wallet", hot);

        assertSame(hot, resolver.resolve(inventory(cold, wallet),
                steel, null).getCandidate());
    }

    private ResourceRequirement rule(short icon, byte material, String target)
            throws UnsupportedImproveMaterialException {
        return table.resolve(icon, material, target);
    }

    private void assertFamilies(byte material, short[] icons,
                                RequirementFamily[] families) throws Exception {
        assertEquals(icons.length, families.length);
        for (int i = 0; i < icons.length; i++)
            assertEquals(families[i], rule(icons[i], material,
                    "matrix target").getFamily());
    }

    private static ImproveResourceCandidate inventory(
            ImproveResourceCandidate... children) {
        return container(10_000, "inventory", children);
    }

    private static ImproveResourceCandidate container(long id, String name,
                                                       ImproveResourceCandidate... children) {
        return new ImproveResourceCandidate(id, name, name, (byte) 0, (short) id,
                0, 0, 0, (byte) 0, Arrays.asList(children));
    }

    private static ImproveResourceCandidate backpack(long id,
                                                      ImproveResourceCandidate... children) {
        return new ImproveResourceCandidate(id, "backpack", "backpack", (byte) 0,
                (short) 1, 0, 0, 0, (byte) 0, Arrays.asList(children));
    }

    private static ImproveResourceCandidate item(long id, String base, String display,
                                                  byte material, short type,
                                                  byte temperature) {
        return coloured(id, base, display, material, type, 0, 0, 0, temperature);
    }

    private static ImproveResourceCandidate coloured(long id, String base,
                                                       String display, byte material,
                                                       short type, float r, float g,
                                                       float b) {
        return coloured(id, base, display, material, type, r, g, b, (byte) 0);
    }

    private static ImproveResourceCandidate coloured(long id, String base,
                                                       String display, byte material,
                                                       short type, float r, float g,
                                                       float b, byte temperature) {
        return new ImproveResourceCandidate(id, base, display, material, type,
                r, g, b, temperature,
                Collections.<ImproveResourceCandidate>emptyList());
    }
}
