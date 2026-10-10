package org.keybinder.wurm.integration;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.CtNewMethod;
import javassist.Loader;
import org.junit.Test;

/** Runs the real console lookup bytecode with graphics-free world fixtures. */
public class FreeCameraKeybindTest {
    @Test public void reproducesVanillaModifierLoss() throws Throwable { run("baseline", false); }
    @Test public void distinguishesEveryModifierCombinationInBothCameraModes() throws Throwable {
        run("modifiers", true);
    }
    @Test public void preservesNativeCameraMovement() throws Throwable { run("native", true); }
    @Test public void unassignedModifiedChordDoesNotRunPlainManagedBind() throws Throwable {
        run("missing", true);
    }
    @Test public void multiSelectorsUseTheExactModifiedBinding() throws Throwable {
        run("selectors", true);
    }
    @Test public void foreignStaleAndDisabledBindingsDoNotOverrideNativeInput() throws Throwable {
        run("ownership", true);
    }
    @Test public void lookupFailureKeepsNativeInput() throws Throwable { run("failure", true); }

    private static void run(String scenario, boolean patched) throws Throwable {
        ClassPool pool = new ClassPool(true);
        stub(pool, "com.wurmonline.client.game.PlayerObj",
                "public boolean shift;", "public boolean alt;", "public boolean ctrl;",
                "public boolean isShiftDown() { return shift; }",
                "public boolean isAltDown() { return alt; }",
                "public boolean isControlDown() { return ctrl; }");
        stub(pool, "com.wurmonline.client.renderer.WorldRender",
                "public boolean freeCamera;",
                "public boolean isFreeCamera() { return freeCamera; }");
        stub(pool, "com.wurmonline.client.game.World",
                "public com.wurmonline.client.game.PlayerObj player;",
                "public com.wurmonline.client.renderer.WorldRender renderer;",
                "public com.wurmonline.client.game.PlayerObj getPlayer() { return player; }",
                "public com.wurmonline.client.renderer.WorldRender getWorldRenderer() { return renderer; }");
        stub(pool, "com.wurmonline.client.renderer.gui.HeadsUpDisplay");
        // Keep the production input coordinator; replace only its heavyweight
        // static mod entry point with the same one-line delegation to that coordinator.
        stub(pool, "org.keybinder.wurm.KeybinderMod",
                "public static com.wurmonline.client.console.KeyBinding resolveFreeCameraBinding("
                        + "com.wurmonline.client.console.KeyBinding exact,"
                        + "com.wurmonline.client.console.KeyBinding nativeBinding) { return "
                        + "org.keybinder.wurm.integration.FreeCameraKeybindProbe.input"
                        + ".resolveFreeCameraBinding(exact, nativeBinding); }");

        CtClass console = pool.get("com.wurmonline.client.console.WurmConsole");
        for (CtField field : console.getDeclaredFields())
            if (!"world".equals(field.getName()) && !"keyBinds".equals(field.getName()))
                console.removeField(field);
        for (CtConstructor constructor : console.getDeclaredConstructors())
            console.removeConstructor(constructor);
        if (console.getClassInitializer() != null) console.removeConstructor(console.getClassInitializer());
        for (CtMethod method : console.getDeclaredMethods())
            if (!"getCurrentBinding".equals(method.getName())
                    && !"getBinding".equals(method.getName())
                    && !"getMetaCode".equals(method.getName())) console.removeMethod(method);
        console.addConstructor(CtNewConstructor.make(
                "public WurmConsole(com.wurmonline.client.game.World world) {"
                        + "this.world = world; this.keyBinds = new java.util.HashMap(); }", console));
        if (patched) KeybinderClientHooks.hookFreeCameraBindings(pool);
        Loader loader = new Loader(pool);
        loader.delegateLoadingOf("org.junit.");
        loader.run(FreeCameraKeybindProbe.class.getName(), new String[]{scenario});
    }

    private static void stub(ClassPool pool, String name, String... members) throws Exception {
        pool.get(name).detach();
        CtClass type = pool.makeClass(name);
        type.addConstructor(CtNewConstructor.defaultConstructor(type));
        for (String member : members) {
            if (member.contains("(")) type.addMethod(CtNewMethod.make(member, type));
            else type.addField(CtField.make(member, type));
        }
    }
}
