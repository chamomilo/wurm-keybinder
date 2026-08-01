package org.keybinder.wurm.bind;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class MultiKeyControllerTest {
    @Test public void ordinaryTapExecutesActive() {
        MultiKeyController controller = new MultiKeyController();
        assertEquals(MultiKeyController.Event.CONSUME,
                controller.press("record", 42, MultiKeyController.Mode.ORDINARY, 100L));
        assertEquals(MultiKeyController.Event.EXECUTE_ACTIVE, controller.release(42));
    }

    @Test public void ordinaryThresholdOpensOnceAndReleaseOnlyConsumes() {
        MultiKeyController controller = new MultiKeyController();
        controller.press("record", 42, MultiKeyController.Mode.ORDINARY, 100L);
        assertEquals(MultiKeyController.Event.NONE, controller.threshold(999L, 1_000L));
        assertEquals(MultiKeyController.Event.OPEN_ORDINARY_SELECTOR,
                controller.threshold(1_100L, 1_000L));
        assertEquals(MultiKeyController.Event.NONE, controller.threshold(2_000L, 1_000L));
        assertEquals(MultiKeyController.Event.CONSUME, controller.release(42));
    }

    @Test public void hudModeOpensImmediatelyAndConsumesRepeatAndRelease() {
        MultiKeyController controller = new MultiKeyController();
        assertEquals(MultiKeyController.Event.OPEN_HUD_SELECTOR,
                controller.press("record", 42, MultiKeyController.Mode.HUD, 100L));
        assertEquals(MultiKeyController.Event.CONSUME,
                controller.press("record", 42, MultiKeyController.Mode.HUD, 200L));
        assertEquals(MultiKeyController.Event.CONSUME, controller.release(42));
    }

    @Test public void clearResetsHeldState() {
        MultiKeyController controller = new MultiKeyController();
        controller.press("record", 42, MultiKeyController.Mode.ORDINARY, 100L);
        controller.clear();
        assertEquals(MultiKeyController.Event.NONE, controller.release(42));
    }
}
