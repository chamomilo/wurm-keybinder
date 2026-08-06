package org.keybinder.wurm.bind;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class SelectorSessionControllerTest {
    @Test public void unrelatedKeyDismissesButOnlyTriggerKeyIsSuppressed() {
        SelectorSessionController controller = new SelectorSessionController();
        controller.open(42);
        assertEquals(SelectorSessionController.KeyDecision.NONE,
                controller.keyPressed(43));
        assertTrue(controller.isActive());
        assertTrue(controller.triggerReleased(42));
        assertEquals(SelectorSessionController.KeyDecision.DISMISS,
                controller.keyPressed(43));
        assertFalse(controller.isActive());

        controller.open(42);
        assertTrue(controller.triggerReleased(42));
        assertEquals(SelectorSessionController.KeyDecision.DISMISS_AND_SUPPRESS_TRIGGER,
                controller.keyPressed(42));
        assertFalse(controller.isActive());
    }

    @Test public void onlySameVariantLeftPressAndReleaseRemainAChoice() {
        SelectorSessionController controller = new SelectorSessionController();
        controller.open(-1);
        assertFalse(controller.pointerPressed(0, "first"));
        assertFalse(controller.pointerReleased(0, "first"));
        assertTrue(controller.isActive());

        assertFalse(controller.pointerPressed(0, "first"));
        assertTrue(controller.pointerReleased(0, "second"));
        assertFalse(controller.isActive());
    }

    @Test public void outsideOrNonLeftPressDismissesImmediately() {
        SelectorSessionController controller = new SelectorSessionController();
        controller.open(-1);
        assertTrue(controller.pointerPressed(0, null));
        assertFalse(controller.isActive());

        controller.open(-1);
        assertTrue(controller.pointerPressed(1, "first"));
        assertFalse(controller.isActive());
    }

    @Test public void dismissalInvalidatesDeferredOpenToken() {
        SelectorSessionController controller = new SelectorSessionController();
        long token = controller.open(42);
        assertTrue(controller.isCurrent(token));
        assertFalse(controller.otherPointerAction());
        assertTrue(controller.isCurrent(token));
        assertTrue(controller.triggerReleased(42));
        assertTrue(controller.otherPointerAction());
        assertFalse(controller.isCurrent(token));
    }

    @Test public void heldTriggerAutoRepeatAndOutsideClicksCannotCancel() {
        SelectorSessionController controller = new SelectorSessionController();
        controller.open(42);
        assertEquals(SelectorSessionController.KeyDecision.NONE,
                controller.keyPressed(42));
        assertFalse(controller.pointerPressed(0, null));
        assertTrue(controller.isActive());

        assertFalse(controller.triggerReleased(43));
        assertTrue(controller.isActive());
        assertTrue(controller.triggerReleased(42));
        assertTrue(controller.pointerPressed(0, null));
        assertFalse(controller.isActive());
    }
}
