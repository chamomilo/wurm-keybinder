package org.keybinder.wurm.command;

import com.wurmonline.client.game.inventory.InventoryMetaItem;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class InventoryFilterResolverTest {
    private final InventoryFilterResolver resolver = new InventoryFilterResolver();

    @Test public void toolbeltWinsBeforeDirectAndNestedInventory() throws Exception {
        InventoryMetaItem beltWater = item(10L, "water, cedarwood");
        InventoryMetaItem directWater = item(20L, "water");
        InventoryMetaItem backpack = item(30L, "backpack");
        backpack.getChildren().add(item(40L, "water, oakenwood"));
        InventoryMetaItem root = item(1L, "inventory");
        root.getChildren().add(directWater);
        root.getChildren().add(backpack);

        assertEquals(10L, resolver.resolve("water",
                Arrays.asList(null, beltWater), root).getId());
    }

    @Test public void directInventoryWinsBeforeArbitrarilyDeepContainer() throws Exception {
        InventoryMetaItem root = item(1L, "inventory");
        InventoryMetaItem satchel = item(2L, "satchel");
        InventoryMetaItem backpack = item(3L, "backpack");
        InventoryMetaItem wallet = item(4L, "wallet");
        wallet.getChildren().add(item(5L, "water"));
        backpack.getChildren().add(wallet);
        root.getChildren().add(satchel);
        root.getChildren().add(backpack);
        root.getChildren().add(item(6L, "water, iron"));

        assertEquals(6L, resolver.resolve("water",
                Collections.<InventoryMetaItem>emptyList(), root).getId());
        root.getChildren().remove(2);
        assertEquals(5L, resolver.resolve("water",
                Collections.<InventoryMetaItem>emptyList(), root).getId());
    }

    @Test public void missingTypeReturnsNull() throws Exception {
        assertNull(resolver.resolve("water", Collections.<InventoryMetaItem>emptyList(),
                item(1L, "inventory")));
    }

    @Test public void waterFilterFindsSaltyWaterInsideNestedBarrel() throws Exception {
        InventoryMetaItem root = item(1L, "inventory");
        InventoryMetaItem backpack = item(2L, "backpack");
        InventoryMetaItem barrel = item(3L, "small barrel, oakenwood");
        barrel.getChildren().add(item(4L, "salty water"));
        backpack.getChildren().add(barrel);
        root.getChildren().add(backpack);

        assertEquals(4L, resolver.resolve("water",
                Collections.<InventoryMetaItem>emptyList(), root).getId());
    }

    @Test public void materialStateAndRarityUseTheSamePortableTypeRule()
            throws Exception {
        InventoryMetaItem root = item(1L, "inventory");
        root.getChildren().add(item(2L, "fantastic log (glowing), cedarwood"));

        assertEquals(2L, resolver.resolve("rare oakenwood log (searing hot)",
                Collections.<InventoryMetaItem>emptyList(), root).getId());
    }

    private static InventoryMetaItem item(long id, String name) throws Exception {
        Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
        InventoryMetaItem item = (InventoryMetaItem) unsafe.allocateInstance(
                InventoryMetaItem.class);
        set(item, "id", id);
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
