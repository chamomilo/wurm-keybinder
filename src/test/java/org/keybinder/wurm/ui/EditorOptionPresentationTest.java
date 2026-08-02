package org.keybinder.wurm.ui;

import org.junit.Test;
import org.keybinder.wurm.model.ItemSelectorKind;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class EditorOptionPresentationTest {
    @Test public void concreteTargetIsKeptAsTheFirstMenuOption() {
        String[] base = {"hover", "toolbelt"};

        String[] options = EditorOptionPresentation.targetOptions("@tb7", base);

        assertEquals(3, options.length);
        assertEquals(EditorOptionPresentation.targetDisplay("@tb7"), options[0]);
        assertNotEquals(options[0], options[1]);
    }

    @Test public void abstractTargetsMapDirectlyToBaseOptions() {
        String[] base = {"hover", "selected"};

        String[] options = EditorOptionPresentation.targetOptions("hover", base);

        assertEquals(2, options.length);
        assertEquals(EditorOptionPresentation.targetLabel("hover"), options[0]);
        assertEquals(EditorOptionPresentation.targetLabel("selected"), options[1]);
    }

    @Test public void sourceKindsUseTheStableDropdownOrder() {
        assertEquals(0, EditorOptionPresentation.sourceOptionFor(
                ItemSelectorKind.CURRENT_ACTIVE));
        assertEquals(3, EditorOptionPresentation.sourceOptionFor(
                ItemSelectorKind.TOOLBELT_SLOT));
        assertEquals(5, EditorOptionPresentation.sourceOptionFor(
                ItemSelectorKind.EXACT_OBJECT));
    }
}
