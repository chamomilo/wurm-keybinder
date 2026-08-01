package org.keybinder.wurm.ui;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class DropZoneClassifierTest {
    @Test public void classifiesQuarterBoundaries() {
        assertEquals(DropZoneClassifier.Zone.BEFORE,
                DropZoneClassifier.classify(24, 20, 20));
        assertEquals(DropZoneClassifier.Zone.MERGE,
                DropZoneClassifier.classify(25, 20, 20));
        assertEquals(DropZoneClassifier.Zone.MERGE,
                DropZoneClassifier.classify(34, 20, 20));
        assertEquals(DropZoneClassifier.Zone.AFTER,
                DropZoneClassifier.classify(35, 20, 20));
        assertEquals(DropZoneClassifier.Zone.NONE,
                DropZoneClassifier.classify(40, 20, 20));
    }
}
