package org.keybinder.wurm.ui;

import java.util.List;

/** Pure row geometry used by both Wurm drag-and-drop views. */
public final class RowInsertionCalculator {
    private RowInsertionCalculator() {}

    public static int insertionIndex(int mouseY, List<Integer> tops, List<Integer> heights) {
        if (tops.size() != heights.size())
            throw new IllegalArgumentException("Row geometry sizes differ");
        for (int i = 0; i < tops.size(); i++)
            if (mouseY < tops.get(i) + Math.max(1, heights.get(i)) / 2) return i;
        return tops.size();
    }

    public static int destinationAfterRemoval(int sourceIndex, int insertionIndex) {
        return insertionIndex > sourceIndex ? insertionIndex - 1 : insertionIndex;
    }
}
