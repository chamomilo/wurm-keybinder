package org.keybinder.wurm.integration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BulkInventoryDestinationPolicyTest {
    @Test public void containerRowTargetsItself() {
        assertEquals(22L, BulkInventoryDestinationPolicy.resolve(22L, true, 11L));
    }

    @Test public void ordinaryRowAndWindowBackgroundTargetWindowRoot() {
        assertEquals(11L, BulkInventoryDestinationPolicy.resolve(22L, false, 11L));
        assertEquals(11L, BulkInventoryDestinationPolicy.resolve(0L, false, 11L));
    }

    @Test public void nativePlayerInventorySentinelIsAValidWindowRoot() {
        assertEquals(-1L, BulkInventoryDestinationPolicy.resolve(22L, false, -1L));
        assertEquals(-1L, BulkInventoryDestinationPolicy.resolve(0L, false, -1L));
        assertEquals(22L, BulkInventoryDestinationPolicy.resolve(22L, true, -1L));
    }

    @Test public void anythingWithoutInventoryWindowRootIsRejected() {
        assertEquals(0L, BulkInventoryDestinationPolicy.resolve(22L, true, 0L));
        assertEquals(0L, BulkInventoryDestinationPolicy.resolve(22L, false, 0L));
    }
}
