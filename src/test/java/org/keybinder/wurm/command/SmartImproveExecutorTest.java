package org.keybinder.wurm.command;

import com.wurmonline.client.game.inventory.InventoryMetaItem;
import org.junit.Test;
import org.keybinder.wurm.i18n.Messages;
import org.keybinder.wurm.model.SmartImproveSourceMode;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SmartImproveExecutorTest {
    @Test
    public void inventoryBatchCostsRepairPlusImproveOnlyForDamagedTargets() {
        assertEquals(2, SmartImproveQueuePlanner.itemCost(true));
        assertEquals(1, SmartImproveQueuePlanner.itemCost(false));
        assertEquals(1, SmartImproveQueuePlanner.itemCost(true, false));
        assertEquals(0, SmartImproveQueuePlanner.itemCost(false, false));
        assertEquals(7, SmartImproveQueuePlanner.batchCost(true, true, true, false));
    }

    @Test
    public void inventoryBatchFitsAnOrderedPrefixInsideFreeQueueSlots() {
        assertEquals(7, SmartImproveQueuePlanner.fittedPrefixCost(
                8, 2, 2, 2, 1));
        assertEquals(8, SmartImproveQueuePlanner.fittedPrefixCost(
                8, 2, 2, 1, 2, 1));
        assertEquals(0, SmartImproveQueuePlanner.fittedPrefixCost(1, 2, 1));
        assertEquals(1, SmartImproveQueuePlanner.fittedPrefixCost(1, 0, 1, 2));
        assertEquals(4, SmartImproveQueuePlanner.fittedPrefixLength(
                8, 2, 2, 2, 2, 2));
        assertEquals(5, SmartImproveQueuePlanner.fittedPrefixLength(
                8, 2, 2, 2, 1, 1, 2));
    }

    @Test
    public void repairIsRequiredOnlyWhileItemDamageIsPositive() throws Exception {
        InventoryMetaItem item = item(1, 50f, (short) 10, "rake");
        set(item, "damage", 0.0f);
        assertFalse(SmartImproveInventoryPolicy.needsRepair(item));

        set(item, "damage", 0.01f);
        assertTrue(SmartImproveInventoryPolicy.needsRepair(item));

        set(item, "damage", 0.0f);
        assertFalse(SmartImproveInventoryPolicy.needsRepair(item));
    }

    @Test
    public void nestedInventorySelectionNamesItsContainer() {
        assertEquals("Smart Improve: using large rat pelt from "
                        + "\"backpack\" in inventory to improve \"rare pickaxe\"",
                Messages.text("improve.using_from_inventory_container",
                        "large rat pelt", "backpack", "rare pickaxe"));
    }

    @Test
    public void missingResourceMessageNamesTheSelectedSourceMode() {
        String toolbeltOnly = SmartImproveExecutor.sourceModeLabel(
                SmartImproveSourceMode.TOOLBELT_ONLY);
        String fallback = SmartImproveExecutor.sourceModeLabel(
                SmartImproveSourceMode.TOOLBELT_THEN_INVENTORY);

        assertEquals("Take tools only from toolbelt", toolbeltOnly);
        assertEquals("First toolbelt, then inventory", fallback);
        assertEquals("Required WATER for \"rare carving knife (glowing), steel\" "
                        + "was not found using Smart Improve source mode "
                        + "\"Take tools only from toolbelt\". No Improve action was sent.",
                Messages.text("improve.exact_resource_missing", "WATER",
                        "rare carving knife (glowing), steel", toolbeltOnly));
    }

    @Test
    public void usesImproveIconSentinelForEveryInventoryItemType() throws Exception {
        InventoryMetaItem nonImprovable = item(1, 36f, (short) 44,
                "lump (glowing), steel");
        InventoryMetaItem improvable = item(2, 50f, (short) 10, "pickaxe");
        set(nonImprovable, "improveIconId", (short) -1);
        set(improvable, "improveIconId", (short) 672);

        assertFalse(SmartImproveInventoryPolicy.isImprovable(nonImprovable));
        assertTrue(SmartImproveInventoryPolicy.isImprovable(improvable));
        assertEquals("The \"lump (glowing), steel\" cannot be improved",
                Messages.text("improve.cannot_improve", nonImprovable.getDisplayName()));
    }

    @Test
    public void openedWorldContainerRootIsNotMistakenForRealItemMetadata()
            throws Exception {
        for (String name : Arrays.asList("forge", "food storage bin",
                "wagon hitched to horses")) {
            InventoryMetaItem syntheticRoot = item(1, 0f, (short) -1, name);
            set(syntheticRoot, "improveIconId", (short) -1);
            assertFalse(name, SmartImproveExecutor.hasLocalImproveMetadata(
                    syntheticRoot));
        }

        InventoryMetaItem realChild = item(2, 50f, (short) 803,
                "baking stone, marble");
        set(realChild, "improveIconId", (short) 570);
        assertTrue(SmartImproveExecutor.hasLocalImproveMetadata(realChild));
    }

    @Test
    public void metalTargetMustBeGlowingButNonMetalTargetDoesNot() throws Exception {
        InventoryMetaItem steelRake = item(1, 50f, (short) 10, "rake, steel");
        set(steelRake, "materialId", (byte) 9);
        set(steelRake, "temperatureState", (byte) 0);
        assertFalse(SmartImproveInventoryPolicy.isTargetTemperatureReady(steelRake));

        set(steelRake, "temperatureState", (byte) 5);
        assertTrue(SmartImproveInventoryPolicy.isTargetTemperatureReady(steelRake));

        InventoryMetaItem woodenRake = item(2, 50f, (short) 10, "rake, birchwood");
        set(woodenRake, "materialId", (byte) 14);
        set(woodenRake, "temperatureState", (byte) 0);
        assertTrue(SmartImproveInventoryPolicy.isTargetTemperatureReady(woodenRake));
    }

    @Test
    public void worldMetalTargetNeedsGlowingNameButOtherMaterialsDoNot() {
        assertFalse(SmartImproveExecutor.worldTemperatureReady(
                (byte) 9, "rare lamp, steel"));
        assertTrue(SmartImproveExecutor.worldTemperatureReady(
                (byte) 9, "rare lamp (glowing), steel"));
        assertTrue(SmartImproveExecutor.worldTemperatureReady(
                (byte) 14, "wagon, birchwood"));
    }

    @Test
    public void ordersStackTargetsByAscendingQualityWithoutMutatingSource() throws Exception {
        InventoryMetaItem high = item(1, 70f, (short) 10, "high");
        InventoryMetaItem lowB = item(3, 20f, (short) 10, "low b");
        InventoryMetaItem lowA = item(2, 20f, (short) 10, "low a");
        List<InventoryMetaItem> visibleOrder = Arrays.asList(high, lowB, lowA);

        List<InventoryMetaItem> queued = SmartImproveInventoryPolicy.orderedTargets(visibleOrder);

        assertEquals(Arrays.asList(lowA, lowB, high), queued);
        assertEquals(Arrays.asList(high, lowB, lowA), visibleOrder);
    }

    @Test
    public void inventorySnapshotTraversesEveryNestedContainer() throws Exception {
        InventoryMetaItem root = item(1, 0f, (short) 0, "inventory");
        InventoryMetaItem satchel = item(2, 0f, (short) 2, "satchel");
        InventoryMetaItem backpack = item(3, 0f, (short) 1, "backpack");
        InventoryMetaItem wallet = item(4, 0f, (short) 3, "wallet");
        wallet.getChildren().add(item(6, 0f, (short) 672, "deep steel lump"));
        satchel.getChildren().add(wallet);
        backpack.getChildren().add(item(5, 0f, (short) 672, "visible steel lump"));
        root.getChildren().add(satchel);
        root.getChildren().add(backpack);

        ImproveResourceCandidate snapshot = SmartImproveExecutor.inventoryCandidate(
                root, Collections.<Long>emptySet());

        assertEquals(2, snapshot.getChildren().size());
        assertEquals(1, snapshot.getChildren().get(0).getChildren().size());
        assertEquals(1, snapshot.getChildren().get(0).getChildren()
                .get(0).getChildren().size());
        assertEquals(6L, snapshot.getChildren().get(0).getChildren()
                .get(0).getChildren().get(0).getId());
        assertEquals(1, snapshot.getChildren().get(1).getChildren().size());
        assertEquals(5L, snapshot.getChildren().get(1).getChildren().get(0).getId());
    }

    private static InventoryMetaItem item(long id, float quality, short type,
                                          String name) throws Exception {
        Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
        InventoryMetaItem item = (InventoryMetaItem) unsafe.allocateInstance(
                InventoryMetaItem.class);
        set(item, "id", id);
        set(item, "quality", quality);
        set(item, "type", type);
        set(item, "baseName", name);
        set(item, "displayName", name);
        set(item, "children", new java.util.ArrayList<InventoryMetaItem>());
        return item;
    }

    private static void set(InventoryMetaItem item, String fieldName,
                            Object value) throws Exception {
        Field field = InventoryMetaItem.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(item, value);
    }
}
