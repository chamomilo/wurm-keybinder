package org.keybinder.wurm.policy;

import javassist.ClassPool;
import javassist.Loader;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Locale;
import com.wurmonline.client.options.keybinding.PlayerKeybind;
import com.wurmonline.shared.constants.PlayerAction;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class VanillaActionPolicyTest {
    @Test
    public void everyBuiltInCommandRetainsItsCapabilitiesWithoutRuntimeEntries() throws Exception {
        Loader loader = new Loader(new ClassPool(true));
        Class<?> actions = loader.loadClass(PlayerAction.class.getName());
        Field registry = actions.getDeclaredField("actionIds");
        registry.setAccessible(true);
        ((Map<?, ?>) registry.get(null)).clear();
        Class<?> policy = loader.loadClass(VanillaActionPolicy.class.getName());
        Map<?, ?> isolatedIds = (Map<?, ?>) policy.getMethod("currentActionIdsByBind")
                .invoke(null);
        int checked = 0;
        for (PlayerKeybind command : PlayerKeybind.values()) {
            String bind = VanillaActionPolicy.actionBindFor(command);
            if (bind == null) continue;
            bind = bind.toUpperCase(Locale.ENGLISH);
            Short id = VanillaActionPolicy.currentActionIdsByBind().get(bind);
            if (id == null) continue;
            assertEquals(command.name(), id, isolatedIds.get(bind));
            assertEquals(command.name(), VanillaActionPolicy.usesItemSource(command),
                    policy.getMethod("acceptsSelectableTool", short.class).invoke(null, id));
            assertEquals(command.name(), VanillaActionPolicy.usesTarget(command),
                    policy.getMethod("acceptsSelectableTarget", short.class).invoke(null, id));
            checked++;
        }
        assertTrue("Audit must cover the entire gameplay catalog", checked > 200);
    }

    @Test
    public void explicitServerFlagsCoverActionsOutsideTheKeybindCatalog() {
        // ActionEntry.NEVER_USE_ACTIVE_ITEM: eating, plant botanizing and renaming.
        for (short id : new short[]{182, 574, 59})
            assertFalse("No item source for action " + id,
                    VanillaActionPolicy.acceptsSelectableTool(id));
        assertFalse(VanillaActionPolicy.usesItemSource(PlayerKeybind.ANALYSE));
        assertFalse(VanillaActionPolicy.usesItemSource(PlayerKeybind.COLLECT));
        assertTrue(VanillaActionPolicy.usesItemSource(PlayerKeybind.FINALIZE_BUILDING));
        assertTrue(VanillaActionPolicy.acceptsSelectableTool(Short.MAX_VALUE));
    }

    @Test
    public void prayNeedsOnlyATargetWhenItsRuntimeCatalogEntryIsMissing() throws Exception {
        // Isolate initialization: other tests must retain their normal client registry.
        Loader loader = new Loader(new ClassPool(true));
        Class<?> actions = loader.loadClass("com.wurmonline.shared.constants.PlayerAction");
        Object pray = actions.getField("PRAY").get(null);
        short id = ((Number) actions.getMethod("getId").invoke(pray)).shortValue();
        Field actionIds = actions.getDeclaredField("actionIds");
        actionIds.setAccessible(true);
        ((Map<?, ?>) actionIds.get(null)).remove(id);

        Class<?> policy = loader.loadClass(VanillaActionPolicy.class.getName());
        assertFalse((Boolean) policy.getMethod("acceptsSelectableTool", short.class)
                .invoke(null, id));
        assertTrue((Boolean) policy.getMethod("acceptsSelectableTarget", short.class)
                .invoke(null, id));
        assertTrue("Unknown actions must retain their selectable tools",
                (Boolean) policy.getMethod("acceptsSelectableTool", short.class)
                        .invoke(null, Short.MAX_VALUE));
    }
}
