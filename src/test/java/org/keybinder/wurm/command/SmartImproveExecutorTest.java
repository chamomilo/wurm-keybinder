package org.keybinder.wurm.command;

import com.wurmonline.client.game.inventory.InventoryMetaItem;
import org.junit.Test;
import org.keybinder.wurm.i18n.Messages;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class SmartImproveExecutorTest {
    @Test
    public void inventoryBatchCostsRepairPlusImproveOnlyForDamagedTargets() {
        assertEquals(2, SmartImproveExecutor.inventoryItemCost(true));
        assertEquals(1, SmartImproveExecutor.inventoryItemCost(false));
        assertEquals(1, SmartImproveExecutor.inventoryItemCost(true, false));
        assertEquals(0, SmartImproveExecutor.inventoryItemCost(false, false));
        assertEquals(7, SmartImproveExecutor.inventoryBatchCost(true, true, true, false));
    }

    @Test
    public void inventoryBatchFitsAnOrderedPrefixInsideFreeQueueSlots() {
        assertEquals(7, SmartImproveExecutor.fittedInventoryPrefixCost(
                8, 2, 2, 2, 1));
        assertEquals(8, SmartImproveExecutor.fittedInventoryPrefixCost(
                8, 2, 2, 1, 2, 1));
        assertEquals(0, SmartImproveExecutor.fittedInventoryPrefixCost(1, 2, 1));
        assertEquals(1, SmartImproveExecutor.fittedInventoryPrefixCost(1, 0, 1, 2));
        assertEquals(4, SmartImproveExecutor.fittedInventoryPrefixLength(
                8, 2, 2, 2, 2, 2));
        assertEquals(5, SmartImproveExecutor.fittedInventoryPrefixLength(
                8, 2, 2, 2, 1, 1, 2));
    }

    @Test
    public void repairIsRequiredOnlyWhileItemDamageIsPositive() throws Exception {
        InventoryMetaItem item = item(1, 50f, (short) 10, "rake");
        set(item, "damage", 0.0f);
        assertFalse(SmartImproveExecutor.needsRepair(item));

        set(item, "damage", 0.01f);
        assertTrue(SmartImproveExecutor.needsRepair(item));

        set(item, "damage", 0.0f);
        assertFalse(SmartImproveExecutor.needsRepair(item));
    }

    @Test
    public void damagedItemMessageAnnouncesRepairBeforeImprove() {
        assertEquals("Smart Improve: Repairing damage and then using large rat pelt from "
                        + "\"large barrel, pinewood (blades)\" in toolbelt slot 1 to improve "
                        + "\"rare pickaxe\"",
                Messages.text("improve.repairing_using_from_container",
                        "large rat pelt", "large barrel, pinewood (blades)", 1,
                        "rare pickaxe"));
    }

    @Test
    public void usesImproveIconSentinelForEveryInventoryItemType() throws Exception {
        InventoryMetaItem nonImprovable = item(1, 36f, (short) 44,
                "lump (glowing), steel");
        InventoryMetaItem improvable = item(2, 50f, (short) 10, "pickaxe");
        set(nonImprovable, "improveIconId", (short) -1);
        set(improvable, "improveIconId", (short) 44);

        assertFalse(SmartImproveExecutor.isImprovable(nonImprovable));
        assertTrue(SmartImproveExecutor.isImprovable(improvable));
        assertEquals("The \"lump (glowing), steel\" cannot be improved",
                Messages.text("improve.cannot_improve", nonImprovable.getDisplayName()));
    }

    @Test
    public void metalTargetMustBeGlowingButNonMetalTargetDoesNot() throws Exception {
        InventoryMetaItem steelRake = item(1, 50f, (short) 10, "rake, steel");
        set(steelRake, "materialId", (byte) 9);
        set(steelRake, "temperatureState", (byte) 0);
        assertFalse(SmartImproveExecutor.isTemperatureReady(steelRake));

        set(steelRake, "temperatureState", (byte) 5);
        assertTrue(SmartImproveExecutor.isTemperatureReady(steelRake));

        InventoryMetaItem woodenRake = item(2, 50f, (short) 10, "rake, birchwood");
        set(woodenRake, "materialId", (byte) 14);
        set(woodenRake, "temperatureState", (byte) 0);
        assertTrue(SmartImproveExecutor.isTemperatureReady(woodenRake));
    }

    @Test
    public void metalLumpToolMustBeGlowingButOrdinaryMetalToolNeedNotBe() throws Exception {
        InventoryMetaItem lump = item(1, 50f, (short) 44, "lump");
        set(lump, "materialId", (byte) 12);
        set(lump, "temperatureState", (byte) 0);
        assertFalse(SmartImproveExecutor.isImproveToolTemperatureReady(lump));

        set(lump, "temperatureState", (byte) 5);
        assertTrue(SmartImproveExecutor.isImproveToolTemperatureReady(lump));

        InventoryMetaItem hammer = item(2, 50f, (short) 10, "hammer");
        set(hammer, "materialId", (byte) 12);
        set(hammer, "temperatureState", (byte) 0);
        assertTrue(SmartImproveExecutor.isImproveToolTemperatureReady(hammer));
    }

    @Test
    public void coldMetalMessagesDistinguishRepairOnlyFromFullSkip() {
        assertEquals("Smart Improve: Repairing damage on \"rake, steel\", but not "
                        + "improving it because it is not glowing",
                Messages.text("improve.repairing_not_glowing", "rake, steel"));
        assertEquals("Smart Improve: Not improving \"rake, steel\" because it is not glowing",
                Messages.text("improve.skipping_not_glowing", "rake, steel"));
        assertEquals("Smart Improve: Not improving \"shovel (glowing), iron\" because lump "
                        + "from \"large barrel, pinewood (blades)\" in toolbelt slot 1 "
                        + "is not glowing",
                Messages.text("improve.skipping_cold_tool_from_container",
                        "shovel (glowing), iron", "lump",
                        "large barrel, pinewood (blades)", 1));
    }

    @Test
    public void ordersStackTargetsByAscendingQualityWithoutMutatingSource() throws Exception {
        InventoryMetaItem high = item(1, 70f, (short) 10, "high");
        InventoryMetaItem lowB = item(3, 20f, (short) 10, "low b");
        InventoryMetaItem lowA = item(2, 20f, (short) 10, "low a");
        List<InventoryMetaItem> visibleOrder = Arrays.asList(high, lowB, lowA);

        List<InventoryMetaItem> queued = SmartImproveExecutor.orderedTargets(visibleOrder);

        assertEquals(Arrays.asList(lowA, lowB, high), queued);
        assertEquals(Arrays.asList(high, lowB, lowA), visibleOrder);
    }

    @Test
    public void findsConsumableInsideNestedToolbeltContainer() throws Exception {
        InventoryMetaItem beltContainer = item(1, 1f, (short) 1, "backpack");
        InventoryMetaItem innerContainer = item(2, 1f, (short) 2, "satchel");
        InventoryMetaItem lump = item(3, 50f, (short) 44, "iron lump");
        beltContainer.getChildren().add(innerContainer);
        innerContainer.getChildren().add(lump);

        InventoryMetaItem found = SmartImproveExecutor.findDescendant(
                beltContainer, candidate -> candidate.getType() == 44);

        assertSame(lump, found);
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
