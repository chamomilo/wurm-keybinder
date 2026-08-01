package org.keybinder.wurm.ui;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EditorTargetValueTest {
    @Test public void savedNearbyRadiusRemainsAConcreteEditorSelection() {
        assertTrue(EditorTargetValue.isConcrete("@nearby4"));
        assertTrue(EditorTargetValue.isConcrete("@nearby4.5"));
        assertFalse(EditorTargetValue.isConcrete("nearby"));
        assertFalse(EditorTargetValue.isConcrete("hover"));
    }
}
