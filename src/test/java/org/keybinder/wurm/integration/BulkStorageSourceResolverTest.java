package org.keybinder.wurm.integration;

import com.wurmonline.client.game.inventory.InventoryMetaItem;
import org.junit.Test;
import org.keybinder.wurm.model.InventoryReference;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class BulkStorageSourceResolverTest {
    private final BulkStorageSourceResolver resolver =
            new BulkStorageSourceResolver();

    @Test public void nestedBulkContainerWinsOverTransportWindow() throws Exception {
        InventoryMetaItem transport = item(10L, 0L, "Raritet");
        InventoryMetaItem unit = item(20L, 10L, "bulk container unit");
        InventoryMetaItem fibre = item(30L, 20L, "reed fibre");
        final Map<Long, InventoryMetaItem> items = map(transport, unit, fibre);

        InventoryReference result = resolver.resolve(fibre,
                Collections.<InventoryMetaItem>emptyList(),
                new InventoryReference(10L, "Raritet"), transport, items::get);

        assertEquals(20L, result.getId());
        assertEquals("bulk container unit", result.getName());
    }

    @Test public void walksThroughSyntheticRowsToNearestBulkAncestor() throws Exception {
        InventoryMetaItem vehicle = item(10L, 0L, "wagon");
        InventoryMetaItem crate = item(20L, 10L, "large crate");
        InventoryMetaItem group = item(25L, 20L, "sorted items");
        InventoryMetaItem barley = item(30L, 25L, "barley");
        final Map<Long, InventoryMetaItem> items = map(vehicle, crate, group, barley);

        InventoryReference result = resolver.resolve(barley,
                Collections.<InventoryMetaItem>emptyList(),
                new InventoryReference(10L, "wagon"), vehicle, items::get);

        assertEquals(20L, result.getId());
    }

    @Test public void standaloneBulkWindowStillUsesWindowFallback() throws Exception {
        InventoryMetaItem root = item(10L, 0L, "food storage bin");
        InventoryMetaItem cheese = item(30L, 0L, "cheese");

        InventoryReference result = resolver.resolve(cheese,
                Collections.<InventoryMetaItem>emptyList(),
                new InventoryReference(10L, "food storage bin"), root,
                id -> null);

        assertEquals(10L, result.getId());
    }

    @Test public void ordinaryVehicleWithoutBulkAncestorIsRejected() throws Exception {
        InventoryMetaItem vehicle = item(10L, 0L, "wagon");
        InventoryMetaItem rope = item(30L, 10L, "rope");
        final Map<Long, InventoryMetaItem> items = map(vehicle, rope);

        assertNull(resolver.resolve(rope,
                Collections.<InventoryMetaItem>emptyList(),
                new InventoryReference(10L, "wagon"), vehicle, items::get));
    }

    @Test public void visibleTreeAncestorWorksWhenParentIdsAreSynthetic()
            throws Exception {
        InventoryMetaItem vehicle = item(10L, 0L, "Raritet");
        InventoryMetaItem unit = item(20L, 0L, "bulk container unit");
        InventoryMetaItem group = item(25L, 0L, "sorted items");
        InventoryMetaItem fibre = item(30L, 0L, "reed fibre");

        InventoryReference result = resolver.resolve(fibre,
                Arrays.asList(group, unit, vehicle),
                new InventoryReference(10L, "Raritet"), vehicle,
                id -> null);

        assertEquals(20L, result.getId());
    }

    private static Map<Long, InventoryMetaItem> map(InventoryMetaItem... items) {
        Map<Long, InventoryMetaItem> result = new HashMap<Long, InventoryMetaItem>();
        for (InventoryMetaItem item : items) result.put(item.getId(), item);
        return result;
    }

    private static InventoryMetaItem item(long id, long parentId, String name)
            throws Exception {
        Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
        InventoryMetaItem item = (InventoryMetaItem) unsafe.allocateInstance(
                InventoryMetaItem.class);
        set(item, "id", id);
        set(item, "parentId", parentId);
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
