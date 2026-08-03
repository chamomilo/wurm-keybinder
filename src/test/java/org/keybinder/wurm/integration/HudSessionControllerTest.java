package org.keybinder.wurm.integration;

import org.junit.Test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

public class HudSessionControllerTest {
    @Test public void onlyAChangedHudSupersedesTheCurrentSession() {
        HudSessionController<Object> sessions = new HudSessionController<Object>();
        Object first = new Object();
        Object second = new Object();
        assertNull(sessions.replace(first));
        assertNull(sessions.replace(first));
        assertSame(first, sessions.replace(second));
        assertSame(second, sessions.current());
        assertSame(second, sessions.clear());
        assertNull(sessions.current());
    }
}
