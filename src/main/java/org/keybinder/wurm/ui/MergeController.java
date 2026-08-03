package org.keybinder.wurm.ui;

public interface MergeController {
    void confirmMerge(String sourceId, String destinationId);
    void cancelMerge();
}
