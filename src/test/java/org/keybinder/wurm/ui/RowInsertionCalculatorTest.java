package org.keybinder.wurm.ui;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import static org.junit.Assert.assertEquals;

public class RowInsertionCalculatorTest {
    @Test public void calculatesEveryBoundary() {
        assertEquals(0, RowInsertionCalculator.insertionIndex(5,
                Arrays.asList(10, 30), Arrays.asList(20, 20)));
        assertEquals(0, RowInsertionCalculator.insertionIndex(19,
                Arrays.asList(10, 30), Arrays.asList(20, 20)));
        assertEquals(1, RowInsertionCalculator.insertionIndex(20,
                Arrays.asList(10, 30), Arrays.asList(20, 20)));
        assertEquals(2, RowInsertionCalculator.insertionIndex(41,
                Arrays.asList(10, 30), Arrays.asList(20, 20)));
        assertEquals(0, RowInsertionCalculator.insertionIndex(10,
                Collections.<Integer>emptyList(), Collections.<Integer>emptyList()));
    }

    @Test public void adjustsDestinationAfterSourceRemoval() {
        assertEquals(0, RowInsertionCalculator.destinationAfterRemoval(2, 0));
        assertEquals(2, RowInsertionCalculator.destinationAfterRemoval(0, 3));
        assertEquals(1, RowInsertionCalculator.destinationAfterRemoval(1, 1));
        assertEquals(1, RowInsertionCalculator.destinationAfterRemoval(1, 2));
    }
}
