package org.keybinder.wurm.integration;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class DeferredUiQueueTest {
    @Test public void drainHonorsPerTickLimitAndPreservesOrder() {
        List<Integer> calls = new ArrayList<Integer>();
        DeferredUiQueue queue = new DeferredUiQueue(2, null);
        queue.defer(() -> calls.add(1));
        queue.defer(() -> calls.add(2));
        queue.defer(() -> calls.add(3));

        assertEquals(2, queue.drain());
        assertEquals(Arrays.asList(1, 2), calls);
        assertEquals(1, queue.size());
        assertEquals(1, queue.drain());
        assertEquals(Arrays.asList(1, 2, 3), calls);
    }

    @Test public void failedOperationDoesNotBlockFollowingOperations() {
        List<Throwable> failures = new ArrayList<Throwable>();
        List<Integer> calls = new ArrayList<Integer>();
        DeferredUiQueue queue = new DeferredUiQueue(4, failures::add);
        queue.defer(() -> { throw new IllegalStateException("broken"); });
        queue.defer(() -> calls.add(2));

        assertEquals(2, queue.drain());
        assertEquals(1, failures.size());
        assertEquals(Arrays.asList(2), calls);
    }
}
