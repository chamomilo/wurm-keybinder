package org.keybinder.wurm.command;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BulkTransferExecutorTest {
    @Test public void inventoryWindowOrContainerRowWinsOverWorldContainer() {
        assertEquals(11L, BulkTransferExecutor.chooseHoveredDestination(
                11L, 33L));
        assertEquals(-1L, BulkTransferExecutor.chooseHoveredDestination(
                -1L, 33L));
    }

    @Test public void fallsBackOnlyToKnownWorldContainer() {
        assertEquals(33L, BulkTransferExecutor.chooseHoveredDestination(
                0L, 33L));
        assertEquals(0L, BulkTransferExecutor.chooseHoveredDestination(
                0L, 0L));
    }

    @Test public void worldDestinationRequiresMatchingContainerMetadata() {
        short containerBits = 1 << 3;
        assertFalse(BulkTransferExecutor.isKnownWorldContainer(
                false, 44L, 44L, containerBits, false, true));
        assertFalse(BulkTransferExecutor.isKnownWorldContainer(
                true, 44L, 45L, containerBits, false, false));
        assertFalse(BulkTransferExecutor.isKnownWorldContainer(
                true, 44L, 44L, (short) 0, false, false));
        assertTrue(BulkTransferExecutor.isKnownWorldContainer(
                true, 44L, 44L, containerBits, false, false));
    }

    @Test public void openInventoryProvesSyntheticWorldRootIsAContainer() {
        assertTrue(BulkTransferExecutor.isKnownWorldContainer(
                true, 44L, 44L, (short) 0, true, false));
        assertTrue(BulkTransferExecutor.isKnownWorldContainer(
                true, 44L, 0L, (short) 0, true, false));
        assertFalse(BulkTransferExecutor.isKnownWorldContainer(
                false, 44L, 44L, (short) 0, true, true));
    }

    @Test public void closedWorldItemCanBeValidatedByServerAsDestination() {
        assertTrue(BulkTransferExecutor.isKnownWorldContainer(
                true, 44L, 0L, (short) 0, false, true));
    }
}
