package org.keybinder.wurm.command;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ExactObjectTargetTest {
    @Test
    public void preservesIdAndUnicodeDisplayName() {
        String target = ExactObjectTarget.encode(1234567890123456789L, "rare кирка");

        assertTrue(ExactObjectTarget.isExact(target));
        assertEquals(1234567890123456789L, ExactObjectTarget.id(target));
        assertEquals("rare кирка", ExactObjectTarget.name(target));
        assertEquals("rare кирка", ExactObjectTarget.display(target));
        assertEquals(1234567890123456789L, TargetCodec.decode(target).getObjectId());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsMalformedId() {
        TargetCodec.decode("@idnot-a-number");
    }
}
