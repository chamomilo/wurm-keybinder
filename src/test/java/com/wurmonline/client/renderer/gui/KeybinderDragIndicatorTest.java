package com.wurmonline.client.renderer.gui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class KeybinderDragIndicatorTest {
    @Test
    public void keybindGapIsPlacedBetweenHeaderRowsAndAddButton() {
        assertEquals(1, KeybinderDragIndicator.keybindGapComponentIndex(0));
        assertEquals(4, KeybinderDragIndicator.keybindGapComponentIndex(3));
    }

    @Test
    public void actionGapIsPlacedAfterVariantTitleLimitAndHeader() {
        assertEquals(3, KeybinderDragIndicator.actionGapComponentIndex(0));
        assertEquals(6, KeybinderDragIndicator.actionGapComponentIndex(3));
    }

    @Test
    public void variantGapAccountsForSectionSpacers() {
        assertEquals(2, KeybinderDragIndicator.variantGapComponentIndex(0));
        assertEquals(8, KeybinderDragIndicator.variantGapComponentIndex(3));
    }

}
