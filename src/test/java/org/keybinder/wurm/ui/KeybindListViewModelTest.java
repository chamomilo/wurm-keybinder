package org.keybinder.wurm.ui;

import org.junit.Test;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.model.TargetKind;
import org.keybinder.wurm.model.TargetSpec;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class KeybindListViewModelTest {
    @Test public void originOptionsAreStableSortedAndIncludeUnknown() {
        KeybindRecord beta = record("Beta", "B");
        beta.setCreatedByUser(" beta ");
        KeybindRecord alpha = record("Alpha", "A");
        alpha.setCreatedByUser("Alpha");
        KeybindRecord unknown = record("Unknown", "U");

        KeybindListViewModel.OriginOptions options = KeybindListViewModel.originOptions(
                Arrays.asList(beta, unknown, alpha), true, "All", "Unknown");

        assertArrayEquals(new String[]{"All", "Unknown", "Alpha", "beta"},
                options.getLabels());
        assertArrayEquals(new String[]{KeybindListViewModel.FILTER_ALL,
                        KeybindListViewModel.FILTER_UNKNOWN, "Alpha", "beta"},
                options.getValues());
    }

    @Test public void filteringCombinesUserAndServerWithoutChangingInputOrder() {
        KeybindRecord first = record("First", "A");
        first.setCreatedByUser("Alice");
        first.setCreatedOnServer("North");
        KeybindRecord second = record("Second", "B");
        second.setCreatedByUser("Alice");
        second.setCreatedOnServer("South");
        KeybindRecord third = record("Third", "C");

        List<KeybindRecord> filtered = KeybindListViewModel.filter(
                Arrays.asList(first, second, third), "alice", "South");

        assertEquals(Collections.singletonList(second), filtered);
        assertEquals(Collections.singletonList(third), KeybindListViewModel.filter(
                Arrays.asList(first, second, third),
                KeybindListViewModel.FILTER_UNKNOWN,
                KeybindListViewModel.FILTER_ALL));
    }

    @Test public void statusReportsManagedConflictButAcceptsConfiguredRecord() {
        KeybindRecord owner = record("Owner", "R");
        KeybindRecord candidate = record("Candidate", "R");
        candidate.setEnabled(false);

        assertTrue(KeybindListViewModel.status(candidate,
                Arrays.asList(owner, candidate), 10).isError());
        assertFalse(KeybindListViewModel.status(owner,
                Collections.singletonList(owner), 10).isError());
    }

    private static KeybindRecord record(String name, String key) {
        return new KeybindRecord(null, name, key,
                Collections.singletonList(new ActionStep((short) 1,
                        TargetSpec.simple(TargetKind.HOVER))));
    }
}
