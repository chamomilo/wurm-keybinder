package org.keybinder.wurm.ui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EditorActionNamePolicyTest {
    @Test public void capturedNameIsUsedWhenRuntimeCatalogDoesNotKnowAction() {
        assertEquals("Light", EditorActionNamePolicy.resolve(
                (short) 12, (short) 12, "Light", id -> ""));
    }

    @Test public void runtimeMenuPathTakesPrecedenceWhenAvailable() {
        assertEquals("Fire -> Light", EditorActionNamePolicy.resolve(
                (short) 12, (short) 12, "Light", id -> "Fire -> Light"));
    }

    @Test public void rememberedNameDoesNotLeakAfterActionIdChanges() {
        assertEquals("", EditorActionNamePolicy.resolve(
                (short) 13, (short) 12, "Light", id -> null));
    }
}
