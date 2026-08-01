package org.keybinder.wurm.ui;

/** Pure top/center/bottom classification for list-row drag and drop. */
public final class DropZoneClassifier {
    public enum Zone { BEFORE, MERGE, AFTER, NONE }

    private DropZoneClassifier() { }

    public static Zone classify(int mouseY, int rowTop, int rowHeight) {
        if (rowHeight <= 0 || mouseY < rowTop || mouseY >= rowTop + rowHeight)
            return Zone.NONE;
        int offset = mouseY - rowTop;
        if (offset * 4 < rowHeight) return Zone.BEFORE;
        if (offset * 4 >= rowHeight * 3) return Zone.AFTER;
        return Zone.MERGE;
    }
}
