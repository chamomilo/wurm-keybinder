package org.keybinder.wurm.integration;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BulkStorageSelectionPolicyTest {
    @Test public void recognizesEverySupportedBulkStorageType() {
        for (String name : Arrays.asList(
                "bulk storage bin", "food storage bin", "small crate",
                "large crate", "bulk container unit")) {
            assertTrue(name, BulkStorageSelectionPolicy.isBulkStorage(
                    name, "", ""));
            assertTrue(name, BulkStorageSelectionPolicy.isBulkStorage(
                    "", name + ", cedarwood", ""));
            assertTrue(name, BulkStorageSelectionPolicy.isBulkStorage(
                    "", "", "rare " + name + " (sorted items)"));
        }
    }

    @Test public void rejectsOrdinaryAndSimilarlyNamedWindows() {
        assertFalse(BulkStorageSelectionPolicy.isBulkStorage(
                "large chest", "large chest", "large chest"));
        assertFalse(BulkStorageSelectionPolicy.isBulkStorage(
                "large crate rack", "large crate rack", "large crate rack"));
        assertFalse(BulkStorageSelectionPolicy.isBulkStorage(
                "backpack", "backpack", "backpack"));
    }
}
