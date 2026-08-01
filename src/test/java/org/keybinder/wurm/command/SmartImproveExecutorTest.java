package org.keybinder.wurm.command;

import com.wurmonline.client.game.inventory.InventoryMetaItem;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class SmartImproveExecutorTest {
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
