package org.keybinder.wurm.storage;

import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.model.KeybindVariant;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.ActivateToolStep;
import org.keybinder.wurm.model.ConsoleCommandStep;
import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.model.SmartImproveStep;
import org.keybinder.wurm.model.TargetKind;
import org.keybinder.wurm.model.TargetSpec;
import org.keybinder.wurm.i18n.Messages;
import org.keybinder.wurm.model.VanillaActionStep;
import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.io.OutputStream;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Arrays;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class KeybindStoreTest {
    @Test
    public void roundTripsVirtualWheelDirectionsAndModifiers() throws Exception {
        Path file = Files.createTempDirectory("keybinder-wheel-test").resolve("wheel.properties");
        KeybindStore store = new KeybindStore(file);
        KeybindRecord up = new KeybindRecord("up", "Up", "MOUSE_WHEEL_UP",
                Collections.<KeybindStep>singletonList(new ConsoleCommandStep("say up")));
        KeybindRecord down = new KeybindRecord("down", "Down", "CTRL+SHIFT+MOUSE_WHEEL_DOWN",
                Collections.<KeybindStep>singletonList(new ConsoleCommandStep("say down")));
        store.save(Arrays.asList(up, down));
        List<KeybindRecord> loaded = store.load();
        assertEquals("MOUSE_WHEEL_UP", loaded.get(0).getKey());
        assertEquals("CTRL+SHIFT+MOUSE_WHEEL_DOWN", loaded.get(1).getKey());
    }

    @Test
    public void roundTripsMultipleVariantsAndActiveSelection() throws Exception {
        Path file = Files.createTempDirectory("keybinder-multi-test").resolve("multi.properties");
        KeybindStore store = new KeybindStore(file);
        KeybindVariant inspect = new KeybindVariant("inspect", "Inspect",
                Arrays.asList(new ActionStep((short) 1,
                        TargetSpec.simple(TargetKind.HOVER))));
        KeybindVariant repair = new KeybindVariant("repair", "Repair",
                Arrays.asList(new ActionStep((short) 2,
                        TargetSpec.simple(TargetKind.ACTIVE_TOOL))));
        KeybindRecord record = new KeybindRecord("multi-id", "Superpower", "R",
                Arrays.asList(inspect, repair), "repair");

        store.save(Arrays.asList(record));
        KeybindRecord loaded = store.load().get(0);

        assertEquals(2, loaded.getVariants().size());
        assertTrue(loaded.isMultiPurpose());
        assertEquals("repair", loaded.getActiveVariantId());
        assertEquals("Superpower-Repair", loaded.getDisplayName());
        assertEquals(2, loaded.getKeybindSteps().get(0) instanceof ActionStep
                ? ((ActionStep) loaded.getKeybindSteps().get(0)).getActionId() : -1);
    }
    @Test
    public void roundTripsUnicodeAndChain() throws Exception {
        Path dir = Files.createTempDirectory("keybinder-test");
        Path file = dir.resolve("records.properties");
        KeybindStore store = new KeybindStore(file);
        KeybindRecord record = KeybindRecord.actionChain("Ремонт", "CTRL+R",
                Collections.singletonList(new ActionStep((short) 163,
                        TargetSpec.simple(TargetKind.ACTIVE_TOOL))));
        store.save(Collections.singletonList(record));
        List<KeybindRecord> loaded = store.load();
        assertEquals(1, loaded.size());
        assertEquals("Ремонт", loaded.get(0).getName());
        assertEquals(TargetKind.ACTIVE_TOOL,
                loaded.get(0).getSteps().get(0).getTarget().getKind());
    }

    @Test
    public void roundTripsHeterogeneousV4StepsAndCreationContext() throws Exception {
        Path dir = Files.createTempDirectory("keybinder-v4-test");
        KeybindStore store = new KeybindStore(dir.resolve("records.properties"));
        List<KeybindStep> steps = Arrays.<KeybindStep>asList(
                new ActivateToolStep(TargetSpec.toolbeltSlot(2)),
                new SmartImproveStep(TargetSpec.simple(TargetKind.HOVER)),
                new VanillaActionStep("MAIN_MENU"),
                new ConsoleCommandStep("toggle inventory"),
                new ActionStep((short) 192, TargetSpec.simple(TargetKind.SELECTED)));
        KeybindRecord record = new KeybindRecord("stable-id", "Mixed", "R", steps);
        record.setCreatedByUser("Chamomilo");
        record.setCreatedOnServer("Sklotopolis - Novus");
        store.save(Collections.singletonList(record));

        KeybindRecord loaded = store.load().get(0);
        assertEquals("stable-id", loaded.getId());
        assertEquals(5, loaded.getKeybindSteps().size());
        assertEquals(2, ((ActivateToolStep) loaded.getKeybindSteps().get(0))
                .getTarget().getSlot());
        assertEquals("toggle inventory",
                ((ConsoleCommandStep) loaded.getKeybindSteps().get(3)).getCommand());
        assertEquals("MAIN_MENU",
                ((VanillaActionStep) loaded.getKeybindSteps().get(2)).getCommand());
        assertEquals((short) 192, ((ActionStep) loaded.getKeybindSteps().get(4)).getActionId());
        assertEquals("Chamomilo", loaded.getCreatedByUser());
        assertEquals("Sklotopolis - Novus", loaded.getCreatedOnServer());
    }

    @Test
    public void roundTripsCurrentRideTarget() throws Exception {
        Path file = Files.createTempDirectory("keybinder-ride-test")
                .resolve("records.properties");
        KeybindStore store = new KeybindStore(file);
        KeybindRecord record = new KeybindRecord("ride-id", "Open ride", "O",
                Collections.<KeybindStep>singletonList(new ActionStep((short) 3,
                        TargetSpec.simple(TargetKind.CURRENT_RIDE))));

        store.save(Collections.singletonList(record));

        ActionStep loaded = (ActionStep) store.load().get(0)
                .getKeybindSteps().get(0);
        assertEquals(TargetKind.CURRENT_RIDE, loaded.getTarget().getKind());
    }

    @Test
    public void recoversFromBackupWhenPrimaryIsMalformed() throws Exception {
        Path dir = Files.createTempDirectory("keybinder-recovery-test");
        Path file = dir.resolve("records.properties");
        KeybindStore store = new KeybindStore(file);
        KeybindRecord original = new KeybindRecord("stable-id", "Original", "R",
                Collections.<KeybindStep>singletonList(new ActionStep((short) 1,
                        TargetSpec.simple(TargetKind.HOVER))));
        store.save(Collections.singletonList(original));
        KeybindRecord newer = new KeybindRecord("stable-id", "Newer", "R",
                Collections.<KeybindStep>singletonList(new ActionStep((short) 2,
                        TargetSpec.simple(TargetKind.HOVER))));
        store.save(Collections.singletonList(newer));
        Files.write(file, Arrays.asList("schema=not-a-number"));

        List<KeybindRecord> recovered = store.load();

        assertEquals(1, recovered.size());
        assertEquals("Original", recovered.get(0).getName());
        assertTrue(store.wasRecoveredFromBackup());
    }

    @Test
    public void migratesLegacyToolbeltActionToNativeActivationStep() throws Exception {
        Path dir = Files.createTempDirectory("keybinder-v5-toolbelt-test");
        Path file = dir.resolve("records.properties");
        Properties props = new Properties();
        props.setProperty("schema", "5");
        props.setProperty("count", "1");
        props.setProperty("record.0.id", encoded("legacy-toolbelt"));
        props.setProperty("record.0.name", encoded("Activate slot"));
        props.setProperty("record.0.key", encoded("R"));
        props.setProperty("record.0.variantCount", "1");
        props.setProperty("record.0.activeVariantId", encoded("default"));
        props.setProperty("record.0.variant.0.id", encoded("default"));
        props.setProperty("record.0.variant.0.subName", encoded(""));
        props.setProperty("record.0.variant.0.stepCount", "1");
        props.setProperty("record.0.variant.0.step.0.kind", "CUSTOM_ACTION");
        props.setProperty("record.0.variant.0.step.0.actionId", "3");
        props.setProperty("record.0.variant.0.step.0.value", encoded("toolbelt"));
        try (OutputStream out = Files.newOutputStream(file)) {
            props.store(out, "legacy Keybinder data");
        }

        KeybindStep migrated;
        Messages.select("pt-BR");
        try {
            migrated = new KeybindStore(file).load().get(0)
                    .getKeybindSteps().get(0);
        } finally {
            Messages.select("en");
        }

        assertTrue(migrated instanceof ActivateToolStep);
        assertEquals(3, ((ActivateToolStep) migrated).getTarget().getSlot());
    }

    private static String encoded(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
