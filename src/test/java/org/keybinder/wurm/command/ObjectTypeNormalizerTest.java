package org.keybinder.wurm.command;

import static org.junit.Assert.assertEquals;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.keybinder.wurm.model.ObjectTypeNormalizer;

@RunWith(Parameterized.class)
public class ObjectTypeNormalizerTest {
    @Parameterized.Parameters(name = "{0} -> {1}")
    public static Collection<Object[]> values() {
        return Arrays.asList(new Object[][] {
                {"a pickaxe, iron", "pickaxe"},
                {"the pickaxe (steel)", "pickaxe"},
                {"pinewood large barrel", "large barrel"},
                {"aged diseased champion troll", "troll"},
                {"an angry black wolf", "black wolf"},
                {"old oak tree stump", "tree stump"},
                {"pinewood felled tree", "felled tree"},
                {"felled tree", "felled tree"},
                {"rare oak tree", "tree"},
                {"fantastic cedar tree (glowing)", "tree"},
                {"supreme oakenwood log (glowing)", "log"},
                {"rare log (searing hot), cedarwood", "log"},
                {"salty water", "water"},
                {"boiling water", "water"},
                {"rare water (glowing), cedarwood", "water"},
                {"rare iron pickaxe (glowing)", "pickaxe"},
                {"a supreme steel pickaxe (searing hot)", "pickaxe"},
                {"rare\u00a0iron\u202fpickaxe", "pickaxe"},
                {"  THE   LARGE   RAT PELT  ", "large rat pelt"}
        });
    }

    private final String input;
    private final String expected;

    public ObjectTypeNormalizerTest(String input, String expected) {
        this.input = input;
        this.expected = expected;
    }

    @Test public void normalizesPortableObjectType() {
        assertEquals(expected, ObjectTypeNormalizer.normalizeType(input));
    }
}
