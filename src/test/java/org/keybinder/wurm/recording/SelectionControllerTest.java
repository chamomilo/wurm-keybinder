package org.keybinder.wurm.recording;

import org.junit.Test;
import org.keybinder.wurm.command.ExactObjectTarget;
import org.keybinder.wurm.event.EventLogger;

import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SelectionControllerTest {
    @Test
    public void exactObjectSelectionStoresClickedItemIdentity() {
        SelectionController controller =
                new SelectionController(new EventLogger(Logger.getAnonymousLogger()));

        controller.requestExactObject();
        assertTrue(controller.acceptExactObject(123456L, "steel pickaxe"));

        String selected = controller.consumeSelectedTarget();
        assertTrue(ExactObjectTarget.isExact(selected));
        assertEquals(123456L, ExactObjectTarget.id(selected));
        assertEquals("steel pickaxe", ExactObjectTarget.name(selected));
        assertFalse(controller.acceptExactObject(7L, "second item"));
    }
}
