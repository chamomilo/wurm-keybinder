package org.keybinder.wurm;

import org.junit.Test;
import org.keybinder.wurm.bind.VanillaBindService;
import org.keybinder.wurm.event.EventLogger;
import org.keybinder.wurm.migration.CustomActionsImporter;
import org.keybinder.wurm.model.ConsoleCommandStep;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.model.KeybindVariant;
import org.keybinder.wurm.queue.ActionQueueCostCalculator;
import org.keybinder.wurm.storage.KeybindStore;

import java.nio.file.Files;
import java.util.Collections;
import java.util.Arrays;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class KeybindRegistryWheelTest {
    @Test public void virtualKeysNeverRequireAConsoleAndMatchExactChord() throws Exception {
        KeybindRegistry registry = registry();
        KeybindRecord plain = record("plain", "MOUSE_WHEEL_UP");
        KeybindRecord modified = record("modified", "CTRL+MOUSE_WHEEL_UP");
        registry.add(plain, null, 10);
        registry.add(modified, null, 10);
        assertSame(plain, registry.findEnabledByChord("mouse_wheel_up"));
        assertSame(modified, registry.findEnabledByChord("CTRL+MOUSE_WHEEL_UP"));
        assertNull(registry.findEnabledByChord("SHIFT+MOUSE_WHEEL_UP"));
    }

    @Test public void identicalVirtualChordDisplacesPreviousRecord() throws Exception {
        KeybindRegistry registry = registry();
        KeybindRecord first = record("first", "SHIFT+MOUSE_WHEEL_DOWN");
        KeybindRecord second = record("second", "shift+mouse_wheel_down");
        registry.add(first, null, 10);
        registry.add(second, null, 10);
        assertFalse(first.isEnabled());
        assertEquals("replaced by second", first.getDisabledReason());
        assertSame(second, registry.findEnabledByChord("SHIFT+MOUSE_WHEEL_DOWN"));
    }

    @Test(expected = NullPointerException.class)
    public void mouse2UsesVanillaBindService() throws Exception {
        registry().add(record("middle", "MOUSE2"), null, 10);
    }

    @Test public void wheelLookupReturnsMultiPurposeRecordWithItsActiveVariant() throws Exception {
        KeybindRegistry registry = registry();
        KeybindVariant first = new KeybindVariant("first", "First",
                Collections.<KeybindStep>singletonList(new ConsoleCommandStep("say first")));
        KeybindVariant active = new KeybindVariant("active", "Active",
                Collections.<KeybindStep>singletonList(new ConsoleCommandStep("say active")));
        KeybindRecord multi = new KeybindRecord(null, "Multi", "MOUSE_WHEEL_DOWN",
                Arrays.asList(first, active), "active");
        registry.add(multi, null, 10);
        KeybindRecord resolved = registry.findEnabledByChord("MOUSE_WHEEL_DOWN");
        assertSame(multi, resolved);
        assertEquals("say active",
                ((ConsoleCommandStep) resolved.getKeybindSteps().get(0)).getCommand());
    }

    private static KeybindRegistry registry() throws Exception {
        return new KeybindRegistry(new KeybindStore(Files.createTempDirectory("wheel-registry")
                .resolve("records.properties")), new VanillaBindService(),
                new CustomActionsImporter(), new ActionQueueCostCalculator(),
                new EventLogger(Logger.getAnonymousLogger()));
    }

    private static KeybindRecord record(String name, String key) {
        return new KeybindRecord(null, name, key,
                Collections.<KeybindStep>singletonList(new ConsoleCommandStep("say " + name)));
    }
}
