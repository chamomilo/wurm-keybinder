package org.keybinder.wurm.command;

import static org.junit.Assert.assertEquals;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.keybinder.wurm.model.ItemSelector;

@RunWith(Parameterized.class)
public class ItemSelectorCodecTest {
    @Parameterized.Parameters(name = "{1}")
    public static Collection<Object[]> values() {
        return Arrays.asList(new Object[][] {
                {ItemSelector.currentActive(), "current-active"},
                {ItemSelector.emptyHand(), "empty-hand"},
                {ItemSelector.hoveredItem(), "hovered-item"},
                {ItemSelector.toolbeltSlot(10), "@tb10"},
                {ItemSelector.equipmentSlot(7), "@eq7"},
                {ItemSelector.exactObject(123L, "rare hammer"),
                        "@id123:cmFyZSBoYW1tZXI"}
        });
    }

    private final ItemSelector selector;
    private final String encoded;

    public ItemSelectorCodecTest(ItemSelector selector, String encoded) {
        this.selector = selector;
        this.encoded = encoded;
    }

    @Test public void roundTripsEverySelectorKind() {
        assertEquals(encoded, ItemSelectorCodec.encode(selector));
        assertEquals(selector, ItemSelectorCodec.decode(encoded));
    }
}
