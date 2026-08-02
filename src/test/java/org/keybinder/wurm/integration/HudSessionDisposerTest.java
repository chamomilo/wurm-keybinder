package org.keybinder.wurm.integration;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

public class HudSessionDisposerTest {
    @Test public void skipsNullAndContinuesAfterOneComponentFails() throws Exception {
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        Object hud = new Object();
        Object first = new Object();
        Object second = new Object();
        List<Object> attempts = new ArrayList<Object>();
        HudSessionDisposer<Object, Object> disposer =
                new HudSessionDisposer<Object, Object>(logger, (ignored, component) -> {
            attempts.add(component);
            if (component == first) throw new ReflectiveOperationException("stale HUD");
        });

        disposer.hideAll(hud, null, first, second);

        assertEquals(2, attempts.size());
        assertEquals(first, attempts.get(0));
        assertEquals(second, attempts.get(1));
    }
}
