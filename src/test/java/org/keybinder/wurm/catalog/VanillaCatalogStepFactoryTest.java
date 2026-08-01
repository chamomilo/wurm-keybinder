package org.keybinder.wurm.catalog;

import com.wurmonline.shared.constants.PlayerAction;
import javassist.ClassPool;
import javassist.CtMethod;
import javassist.bytecode.CodeIterator;
import javassist.bytecode.ConstPool;
import javassist.bytecode.Opcode;
import org.junit.Test;
import org.keybinder.wurm.command.ActionExecutor;
import org.keybinder.wurm.command.KeybindExecutionService;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.ActivateToolStep;
import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.model.ItemSelector;
import org.keybinder.wurm.model.ItemSelectorKind;
import org.keybinder.wurm.model.TargetKind;
import org.keybinder.wurm.model.TargetSpec;
import org.keybinder.wurm.model.VanillaActionStep;
import org.keybinder.wurm.storage.KeybindStore;
import org.keybinder.wurm.model.KeybindRecord;

import java.nio.file.Files;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class VanillaCatalogStepFactoryTest {
    private final VanillaCatalogStepFactory factory = new VanillaCatalogStepFactory();

    @Test
    public void gameplaySelectionCreatesActionStepWithSelectedTarget() throws Exception {
        VanillaKeybindCatalog catalog = new VanillaKeybindCatalog();
        VanillaKeybindCatalog.Entry entry = catalog.find("EXAMINE");
        TargetSpec target = TargetSpec.simple(TargetKind.HOVER);

        KeybindStep created = factory.create(catalog.categoryFor("EXAMINE"), entry, target);

        assertTrue(created instanceof ActionStep);
        ActionStep action = (ActionStep) created;
        assertEquals(PlayerAction.EXAMINE.getId(), action.getActionId());
        assertSame(target, action.getTarget());
        assertEquals(1, new ActionExecutor(null).runtimeQueueCost(action, null));
    }

    @Test
    public void gameplayCatalogSelectionPreservesSelectedToolSource() {
        VanillaKeybindCatalog catalog = new VanillaKeybindCatalog();
        ItemSelector source = ItemSelector.toolbeltSlot(4);

        KeybindStep created = factory.create(catalog.categoryFor("EXAMINE"),
                catalog.find("EXAMINE"), source,
                TargetSpec.simple(TargetKind.HOVER));

        assertTrue(created instanceof ActionStep);
        assertEquals(ItemSelectorKind.TOOLBELT_SLOT,
                ((ActionStep) created).getSource().getKind());
        assertEquals(4, ((ActionStep) created).getSource().getSlot());
    }

    @Test
    public void hudAndMovementRemainNativeCompatibilityStepsWithoutTargets() {
        VanillaKeybindCatalog catalog = new VanillaKeybindCatalog();

        KeybindStep hud = factory.create(catalog.categoryFor("MAIN_MENU"),
                catalog.find("MAIN_MENU"), null);
        KeybindStep movement = factory.create(catalog.categoryFor("MOVE_FORWARD"),
                catalog.find("MOVE_FORWARD"), null);

        assertTrue(hud instanceof VanillaActionStep);
        assertEquals("MAIN_MENU", ((VanillaActionStep) hud).getCommand());
        assertTrue(movement instanceof VanillaActionStep);
        assertEquals("MOVE_FORWARD", ((VanillaActionStep) movement).getCommand());
    }

    @Test
    public void aliasedGameplayActionUsesOrdinaryActionStepAndTarget() {
        VanillaKeybindCatalog catalog = new VanillaKeybindCatalog();
        TargetSpec selected = TargetSpec.simple(TargetKind.SELECTED);

        KeybindStep created = factory.create(
                catalog.categoryFor("SIT"), catalog.find("SIT"), selected);

        assertTrue(created instanceof ActionStep);
        assertEquals(PlayerAction.SIT_ANY.getId(), ((ActionStep) created).getActionId());
        assertSame(selected, ((ActionStep) created).getTarget());
    }

    @Test
    public void vanillaActivateUsesExistingActivationStepWithHoverTarget() {
        VanillaKeybindCatalog catalog = new VanillaKeybindCatalog();
        TargetSpec hover = TargetSpec.simple(TargetKind.HOVER);

        KeybindStep created = factory.create(
                catalog.categoryFor("ACTIVATE"), catalog.find("ACTIVATE"), hover);

        assertTrue(factory.usesTarget(
                catalog.categoryFor("ACTIVATE"), catalog.find("ACTIVATE")));
        assertTrue(created instanceof ActivateToolStep);
        assertSame(hover, ((ActivateToolStep) created).getTarget());
    }

    @Test
    public void unresolvedMatchSafelyRemainsCompatibility() {
        VanillaKeybindCatalog catalog =
                new VanillaKeybindCatalog(Collections.<String, Short>emptyMap());

        KeybindStep created = factory.create(catalog.categoryFor("EXAMINE"),
                catalog.find("EXAMINE"), null);

        assertTrue(created instanceof VanillaActionStep);
        assertEquals("EXAMINE", ((VanillaActionStep) created).getCommand());
    }

    @Test
    public void structuredVanillaActionUsesExistingStorageRoundTrip() throws Exception {
        VanillaKeybindCatalog catalog = new VanillaKeybindCatalog();
        ActionStep created = (ActionStep) factory.create(catalog.categoryFor("EXAMINE"),
                catalog.find("EXAMINE"), TargetSpec.simple(TargetKind.HOVER));
        KeybindRecord record = new KeybindRecord("catalog-action", "Examine", "E",
                Collections.<KeybindStep>singletonList(created));
        KeybindStore store = new KeybindStore(Files.createTempDirectory(
                "keybinder-catalog-action").resolve("records.properties"));

        store.save(Collections.singletonList(record));

        ActionStep loaded = (ActionStep) store.load().get(0).getKeybindSteps().get(0);
        assertEquals(created.getActionId(), loaded.getActionId());
        assertEquals(TargetKind.HOVER, loaded.getTarget().getKind());
    }

    @Test
    public void takeWithAutomaticNearbyPreservesItsTargetAcrossStorageRoundTrip()
            throws Exception {
        VanillaKeybindCatalog catalog = new VanillaKeybindCatalog();
        TargetSpec nearby = TargetSpec.simple(TargetKind.NEARBY);
        KeybindStep selected = factory.create(catalog.categoryFor("TAKE"),
                catalog.find("TAKE"), nearby);
        assertTrue(selected instanceof ActionStep);

        KeybindRecord record = new KeybindRecord("take-nearby", "Take nearby", "T",
                Collections.singletonList(selected));
        KeybindStore store = new KeybindStore(Files.createTempDirectory(
                "keybinder-take-nearby").resolve("records.properties"));
        store.save(Collections.singletonList(record));

        ActionStep loaded = (ActionStep) store.load().get(0).getKeybindSteps().get(0);
        assertEquals(PlayerAction.TAKE.getId(), loaded.getActionId());
        assertEquals(ItemSelectorKind.EMPTY_HAND, loaded.getSource().getKind());
        assertEquals(TargetKind.NEARBY, loaded.getTarget().getKind());
        assertEquals("nearby", org.keybinder.wurm.command.TargetCodec.encode(
                loaded.getTarget()));
    }

    @Test
    public void ordinaryActionStepsDispatchToActionExecutor() throws Exception {
        CtMethod method = ClassPool.getDefault()
                .get(KeybindExecutionService.class.getName())
                .getDeclaredMethod("executeStep");
        CodeIterator code = method.getMethodInfo().getCodeAttribute().iterator();
        ConstPool constants = method.getMethodInfo().getConstPool();
        boolean callsActionExecutor = false;
        while (code.hasNext()) {
            int position = code.next();
            if (code.byteAt(position) != Opcode.INVOKEVIRTUAL) continue;
            int methodRef = code.u16bitAt(position + 1);
            if (ActionExecutor.class.getName().equals(
                    constants.getMethodrefClassName(methodRef))
                    && "executeStep".equals(constants.getMethodrefName(methodRef))) {
                callsActionExecutor = true;
                break;
            }
        }
        assertTrue(callsActionExecutor);
    }
}
