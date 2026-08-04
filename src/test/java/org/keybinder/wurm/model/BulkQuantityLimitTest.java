package org.keybinder.wurm.model;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BulkQuantityLimitTest {
    @Test public void readsWurmBulkCountSuffix() {
        assertEquals(619, BulkQuantityLimit.fromName("beeswax (619x)"));
        assertEquals(63, BulkQuantityLimit.fromName("tooth (63x)"));
        assertEquals(1234, BulkQuantityLimit.fromName("barley (1 234x)"));
        assertEquals(1234, BulkQuantityLimit.fromName("barley (1\u00a0234x)"));
    }

    @Test public void doesNotGuessFromOrdinaryItemNames() {
        assertEquals(0, BulkQuantityLimit.fromName("barley"));
        assertEquals(0, BulkQuantityLimit.fromName("barley (rare)"));
    }

    @Test public void enforcesCapturedMaximumAndKeepsLegacyQuantityOne() {
        BulkStorageItem counted = source("barley (43x)");
        assertTrue(BulkQuantityLimit.accepts(counted, 1));
        assertTrue(BulkQuantityLimit.accepts(counted, 43));
        assertFalse(BulkQuantityLimit.accepts(counted, 44));
        assertFalse(BulkQuantityLimit.accepts(counted, 0));

        BulkStorageItem legacy = source("barley");
        assertTrue(BulkQuantityLimit.accepts(legacy, 1));
        assertFalse(BulkQuantityLimit.accepts(legacy, 2));
    }

    private static BulkStorageItem source(String name) {
        return new BulkStorageItem(new InventoryReference(1L, "bulk storage bin"),
                new InventoryReference(2L, name));
    }
}
