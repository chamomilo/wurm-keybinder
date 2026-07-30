package org.keybinder.wurm.bind;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class LongPressControllerTest {
    @Test
    public void shortReleaseIsTap() {
        LongPressController controller = new LongPressController();
        controller.press("record", 42, 100L);

        LongPressController.Release release = controller.release(42);

        assertEquals("record", release.getRecordId());
        assertTrue(release.isTap());
    }

    @Test
    public void longPressTriggersOnceAndReleaseDoesNotTap() {
        LongPressController controller = new LongPressController();
        controller.press("record", 42, 100L);

        assertNull(controller.triggerIfElapsed(999L, 1_000L));
        assertEquals("record", controller.triggerIfElapsed(1_100L, 1_000L));
        assertNull(controller.triggerIfElapsed(2_000L, 1_000L));
        assertFalse(controller.release(42).isTap());
    }
}
