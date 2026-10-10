package org.keybinder.wurm.integration;

import com.wurmonline.client.console.KeyBinding;
import com.wurmonline.client.console.WurmConsole;
import com.wurmonline.client.game.PlayerObj;
import com.wurmonline.client.game.World;
import com.wurmonline.client.renderer.WorldRender;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import org.keybinder.wurm.model.ConsoleCommandStep;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.model.KeybindVariant;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import static org.junit.Assert.*;

public final class FreeCameraKeybindProbe {
    public static ManagedInputCoordinator input;
    private final Environment environment = new Environment();
    private final World world = fixture(World.class);
    private final PlayerObj player = fixture(PlayerObj.class);
    private final WorldRender renderer = fixture(WorldRender.class);
    private final WurmConsole console;
    private final Map<Integer, KeyBinding> bindings;
    private static final int SPACE = 57;
    private static final int MOUSE2 = 4098;
    private static final String[] MODIFIERS = {
            "", "SHIFT+", "ALT+", "SHIFT+ALT+", "CTRL+", "CTRL+SHIFT+", "CTRL+ALT+", "CTRL+SHIFT+ALT+"
    };

    @SuppressWarnings("unchecked")
    private FreeCameraKeybindProbe() throws Exception {
        set(world, "player", player);
        set(world, "renderer", renderer);
        set(renderer, "freeCamera", true);
        console = new WurmConsole(world);
        Field field = WurmConsole.class.getDeclaredField("keyBinds");
        field.setAccessible(true);
        bindings = (Map<Integer, KeyBinding>) field.get(console);
        input = new ManagedInputCoordinator(environment);
    }

    public static void main(String[] arguments) throws Exception {
        FreeCameraKeybindProbe probe = new FreeCameraKeybindProbe();
        String scenario = arguments[0];
        if ("baseline".equals(scenario)) probe.baseline();
        else if ("modifiers".equals(scenario)) probe.modifiers();
        else if ("native".equals(scenario)) probe.nativeMovement();
        else if ("missing".equals(scenario)) probe.missing();
        else if ("selectors".equals(scenario)) probe.selectors();
        else if ("ownership".equals(scenario)) probe.ownership();
        else if ("failure".equals(scenario)) probe.failure();
        else throw new AssertionError(scenario);
    }

    private void baseline() throws Exception {
        KeyBinding plain = managed(SPACE, 0);
        managed(SPACE, 1);
        modifiers(1);
        assertSame("Vanilla free camera selects Space instead of Shift+Space",
                plain, console.getCurrentBinding(SPACE));
    }

    private void modifiers() throws Exception {
        for (int key : new int[]{SPACE, MOUSE2}) {
            KeyBinding[] expected = new KeyBinding[8];
            for (int mask = 0; mask < 8; mask++) expected[mask] = managed(key, mask);
            for (boolean freeCamera : new boolean[]{true, false}) {
                set(renderer, "freeCamera", freeCamera);
                for (int mask = 0; mask < 8; mask++) {
                    modifiers(mask);
                    assertSame("mode=" + freeCamera + " chord=" + MODIFIERS[mask] + key,
                            expected[mask], console.getCurrentBinding(key));
                }
            }
        }
    }

    private void nativeMovement() throws Exception {
        KeyBinding movement = binding("W", "move_forward");
        KeyBinding modified = binding("Shift+W", "custom_command");
        bindings.put(17, movement);
        bindings.put(WurmConsole.getMetaCode(17, true, false, false), modified);
        for (int mask = 0; mask < 8; mask++) {
            modifiers(mask);
            assertSame(movement, console.getCurrentBinding(17));
        }
        set(renderer, "freeCamera", false);
        modifiers(1);
        assertSame(modified, console.getCurrentBinding(17));
    }

    private void missing() throws Exception {
        KeyBinding plain = managed(SPACE, 0);
        for (int mask = 1; mask < 8; mask++) {
            modifiers(mask);
            assertNull("No plain managed fallback for " + MODIFIERS[mask],
                    console.getCurrentBinding(SPACE));
        }
        modifiers(0);
        assertSame(plain, console.getCurrentBinding(SPACE));
    }

    private void selectors() throws Exception {
        managed(SPACE, 0);
        KeyBinding shifted = managed(SPACE, 1);
        KeybindRecord original = owner(shifted);
        KeybindRecord record = new KeybindRecord(original.getId(), original.getName(), original.getKey(),
                Arrays.asList(new KeybindVariant("first", "First", original.getKeybindSteps()),
                        new KeybindVariant("second", "Second", original.getKeybindSteps())), "first");
        environment.records.put(record.getId(), record);
        modifiers(1);
        assertTrue(input.handleKeyToggle(console, SPACE, true));
        // Releasing Shift before Space must still execute the pressed record.
        modifiers(0);
        assertTrue(input.handleKeyToggle(console, SPACE, false));
        assertSame(record, environment.executed);

        KeyBinding controlled = managed(SPACE, 4);
        KeybindRecord hudMulti = owner(controlled);
        hudMulti.setHudMulti(true);
        modifiers(4);
        assertTrue(input.handleKeyToggle(console, SPACE, true));
        assertSame(hudMulti, environment.selector);
        assertTrue(input.handleKeyToggle(console, SPACE, false));
    }

    private void ownership() throws Exception {
        KeyBinding nativeBinding = binding("Space", "native_command");
        bindings.put(SPACE, nativeBinding);
        KeyBinding shifted = managed(SPACE, 1);
        KeybindRecord record = owner(shifted);
        modifiers(1);
        record.setEnabled(false);
        assertSame(nativeBinding, console.getCurrentBinding(SPACE));
        record.setEnabled(true);
        record.setKey("SHIFT+Q");
        assertSame(nativeBinding, console.getCurrentBinding(SPACE));
        environment.records.clear();
        assertSame(nativeBinding, console.getCurrentBinding(SPACE));
        KeyBinding foreign = binding("Shift+Space", "foreign_command");
        bindings.put(WurmConsole.getMetaCode(SPACE, true, false, false), foreign);
        assertSame(nativeBinding, console.getCurrentBinding(SPACE));
    }

    private void failure() throws Exception {
        KeyBinding plain = managed(SPACE, 0);
        managed(SPACE, 1);
        modifiers(1);
        environment.failLookup = true;
        assertSame(plain, console.getCurrentBinding(SPACE));
        assertEquals(1, environment.warnings);
    }

    private KeyBinding managed(int key, int mask) {
        String chord = MODIFIERS[mask] + (key == SPACE ? "SPACE" : "MOUSE2");
        String id = "record-" + key + "-" + mask;
        KeybindRecord record = new KeybindRecord(id, chord, chord,
                Collections.<KeybindStep>singletonList(new ConsoleCommandStep("test " + id, false)));
        environment.records.put(id, record);
        KeyBinding binding = binding(chord, "keybinder_run " + id);
        bindings.put(WurmConsole.getMetaCode(key, (mask & 1) != 0,
                (mask & 2) != 0, (mask & 4) != 0), binding);
        return binding;
    }

    private KeybindRecord owner(KeyBinding binding) {
        return environment.records.get(binding.getStrCommand().substring("keybinder_run ".length()));
    }
    private static KeyBinding binding(String chord, String command) {
        return new KeyBinding(chord, "\"" + command + "\"");
    }
    private void modifiers(int mask) throws Exception {
        set(player, "shift", (mask & 1) != 0);
        set(player, "alt", (mask & 2) != 0);
        set(player, "ctrl", (mask & 4) != 0);
    }
    private static <T> T fixture(Class<T> type) {
        try { return type.newInstance(); }
        catch (Exception failure) { throw new AssertionError(failure); }
    }
    private static void set(Object target, String field, Object value) throws Exception {
        target.getClass().getField(field).set(target, value);
    }

    private static final class Environment implements ManagedInputCoordinator.Environment {
        final Map<String, KeybindRecord> records = new HashMap<String, KeybindRecord>();
        KeybindRecord executed;
        KeybindRecord selector;
        boolean failLookup;
        int warnings;
        @Override public HeadsUpDisplay hud() { return null; }
        @Override public KeybindRecord findEnabledByChord(String chord) { return null; }
        @Override public KeybindRecord findById(String id) {
            if (failLookup) throw new IllegalStateException("lookup failure");
            return records.get(id);
        }
        @Override public void execute(KeybindRecord record, HeadsUpDisplay hud) { executed = record; }
        @Override public void openSelector(KeybindRecord record, boolean hud, int key) { selector = record; }
        @Override public void closeSelector() {}
        @Override public void warning(String message, Throwable failure) { warnings++; }
        @Override public void fine(String message, Throwable failure) {}
        @Override public void reportWheelFailure(String message, Throwable failure) {}
        @Override public long nanoTime() { return 0L; }
        @Override public long currentTimeMillis() { return 0L; }
    }
}
