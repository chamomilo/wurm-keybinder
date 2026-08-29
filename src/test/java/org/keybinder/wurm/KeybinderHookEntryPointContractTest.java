package org.keybinder.wurm;

import com.wurmonline.client.console.WurmConsole;
import com.wurmonline.client.game.PlayerObj;
import com.wurmonline.client.renderer.PickableUnit;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.client.renderer.gui.SelectBar;
import com.wurmonline.shared.constants.PlayerAction;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/** Keeps the public static callbacks referenced by injected client bytecode stable. */
public class KeybinderHookEntryPointContractTest {
    @Test public void injectedCallbacksRemainPublicAndStatic() throws Exception {
        assertCallback("rememberActionName", void.class, short.class, String.class);
        assertCallback("rememberPopupSubmenu", void.class,
                Object.class, Object.class, String.class);
        assertCallback("rememberActionMenuPath", void.class, Object.class, PlayerAction.class);
        assertCallback("handleCommand", boolean.class, String.class, String[].class);
        assertCallback("handleKeyToggle", boolean.class,
                WurmConsole.class, int.class, boolean.class);
        assertCallback("observeKeyPressed", void.class, int.class);
        assertCallback("observeKeyReleased", void.class, int.class);
        assertCallback("handleMouseWheel", void.class, int.class, int.class, int.class);
        assertCallback("onConnectionEnded", void.class);
        assertCallback("onServerTransfer", void.class, String.class, int.class);
        assertCallback("onServerInformation", void.class, String.class);
        assertCallback("alignViewAfterEmbark", void.class, PlayerObj.class, float.class);
        assertCallback("rememberSelectedServer", void.class, Object.class);
        assertCallback("observeCapturedAction", void.class, PlayerAction.class);
        assertCallback("withCapturedTarget", Object.class,
                String.class, Object.class, Method.class, Object[].class);
        assertCallback("observeWorldImproveEvent", void.class, String.class, String.class);
        assertCallback("observeWorldImproveAction", void.class,
                long[].class, PlayerAction.class);
        assertCallback("observeWorldImproveSelection", void.class, PickableUnit.class);
        assertCallback("observeActionSent", void.class,
                long.class, long[].class, PlayerAction.class);
        assertCallback("observeSingleActionSent", void.class,
                long.class, long.class, PlayerAction.class);
        assertCallback("observeActionState", void.class, String.class, float.class);
        assertCallback("observeMousePressed", void.class,
                int.class, int.class, int.class);
        assertCallback("observeMouseDragged", void.class);
        assertCallback("observeMouseReleased", void.class,
                Object.class, int.class, int.class, int.class);
        assertCallback("afterPushTargetRecreated", void.class,
                SelectBar.class, PickableUnit.class);
        assertCallback("onHudReady", void.class, HeadsUpDisplay.class);
        assertCallback("onHudTick", void.class, HeadsUpDisplay.class);
        assertCallback("captureInventoryTarget", void.class,
                Object.class, int.class, int.class);
        assertCallback("interceptToolbeltSelection", Object.class,
                Object.class, Method.class, Object[].class);
        assertCallback("interceptEquipmentSelection", Object.class,
                Object.class, Method.class, Object[].class);
        assertCallback("interceptBulkTransferBml", boolean.class,
                HeadsUpDisplay.class, String.class, String.class);
    }

    private static void assertCallback(String name, Class<?> returnType,
                                       Class<?>... parameters) throws Exception {
        Method method = KeybinderMod.class.getDeclaredMethod(name, parameters);
        assertTrue(name + " must remain public", Modifier.isPublic(method.getModifiers()));
        assertTrue(name + " must remain static", Modifier.isStatic(method.getModifiers()));
        assertEquals(name + " return type", returnType, method.getReturnType());
    }
}
