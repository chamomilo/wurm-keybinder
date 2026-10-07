package org.keybinder.wurm.command;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtConstructor;
import javassist.CtMethod;
import javassist.Loader;
import org.junit.Test;

/** Runs the execution service with the real model, preflight and action executor. */
public class KeybindPreflightTest {
    @Test public void checksAllStepsBeforeFirstSendAndSkipsWithoutReservingCapacity() throws Throwable {
        run("order");
    }
    @Test public void freezesTargetsBeforeEarlierActionsCanChangeHud() throws Throwable {
        run("snapshot");
    }
    @Test public void missingItemsNeverReachTheSendPath() throws Throwable {
        run("missing");
    }
    @Test public void simulatesEmptyHandActivationWithoutChangingHudDuringPreflight() throws Throwable {
        run("activation");
    }
    private static void run(String scenario) throws Throwable {
        ClassPool pool = new ClassPool(true);
        CtClass hud = pool.get("com.wurmonline.client.renderer.gui.HeadsUpDisplay");
        // Native HUD fields pull in JavaFX/graphics dependencies absent from the
        // pinned test libraries. Retain only the active-item field used by this probe.
        for (CtField field : hud.getDeclaredFields())
            if (!"activeToolItem".equals(field.getName())) hud.removeField(field);
        for (CtConstructor constructor : hud.getDeclaredConstructors())
            hud.removeConstructor(constructor);
        if (hud.getClassInitializer() != null) hud.removeConstructor(hud.getClassInitializer());
        for (CtMethod method : hud.getDeclaredMethods()) {
            if ("setActiveToolItem".equals(method.getName()))
                method.setBody("{ activeToolItem = $1; }");
            else if ("removeActiveToolItem".equals(method.getName()))
                method.setBody("{ activeToolItem = null; }");
            else hud.removeMethod(method);
        }
        Loader loader = new Loader(pool);
        loader.delegateLoadingOf("org.junit.");
        loader.run("org.keybinder.wurm.command.KeybindPreflightProbe", new String[]{scenario});
    }
}
