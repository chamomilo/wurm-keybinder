package org.keybinder.wurm.i18n;

import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class LocalizedTextTest {
    @After public void reset() { Messages.select("en"); }
    @Test public void equalUserTextIsNeverInterpretedAsAMessage() {
        String caption = Messages.text("common.cancel");
        LocalizedText source = LocalizedText.capture(caption);
        assertNotNull(source);
        assertNull(LocalizedText.capture(new String(caption)));
        Messages.select("ru");
        assertEquals(Messages.text("common.cancel"), source.resolve());
    }
    @Test public void nestedMessagesChangeButRecordNamesAndNumbersRemain() {
        String name = new String(Messages.text("multi.mode.hud"));
        LocalizedText nested = LocalizedText.capture(Messages.text("merge.effects", "CTRL+R",
                Messages.text("multi.mode.hud")));
        LocalizedText user = LocalizedText.capture(Messages.text("merge.source", name));
        Messages.select("ru");
        assertEquals(Messages.text("merge.effects", "CTRL+R", Messages.text("multi.mode.hud")), nested.resolve());
        assertEquals(Messages.text("merge.source", name), user.resolve());
    }
}
