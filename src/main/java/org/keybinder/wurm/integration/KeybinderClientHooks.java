package org.keybinder.wurm.integration;

import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.shared.constants.PlayerAction;
import javassist.ClassPool;
import javassist.CtClass;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.keybinder.wurm.KeybinderMod;

import java.util.logging.Level;
import java.util.logging.Logger;

/** Installs the pinned Wurm client hooks while keeping feature callbacks elsewhere. */
public final class KeybinderClientHooks {
    private final FailOpenHookInstaller hooks;
    private final Logger logger;

    public KeybinderClientHooks(FailOpenHookInstaller hooks, Logger logger) {
        this.hooks = hooks;
        this.logger = logger;
    }

    public void install() {
        final ClassPool pool = HookManager.getInstance().getClassPool();
        hooks.install("action name cache", () -> hookActionNameCache(pool));
        hooks.install("per-action source override", () -> hookActionSource(pool));
        hooks.install("action menu paths", () -> hookActionMenuPaths(pool));
        hooks.install("console commands", () -> hookConsole(pool));
        hooks.install("multi-purpose long press", () -> hookLongPress(pool));
        hooks.install("mouse wheel keybinds", () -> hookMouseWheel(pool));
        hooks.install("connection lifecycle", () -> hookConnectionLifecycle(pool));
        hooks.install("embark heading", () -> hookEmbarkHeading(pool));
        hooks.install("HUD lifecycle", KeybinderClientHooks::hookHud);
        hooks.install("action queue occupancy", () -> hookActionQueue(pool));
        hooks.install("one-shot action capture", KeybinderClientHooks::hookActionCapture);
        hooks.install("world Improve Examine metadata", () -> hookWorldImprove(pool));
        hooks.install("creation skill catalog", () -> hookCreationSkillCatalog(pool));
        hooks.install("toolbelt target selection", KeybinderClientHooks::hookToolbeltSelection);
        hooks.install("equipment target selection", KeybinderClientHooks::hookEquipmentSelection);
        hooks.install("world target selection", () -> hookWorldSelection(pool));
        hooks.install("inventory target selection", KeybinderClientHooks::hookInventorySelection);
        hooks.install("bulk transfer quantity response", KeybinderClientHooks::hookBulkTransferBml);
        hooks.install("push selection retention", () -> hookPushSelection(pool));
        hooks.install("server identity", () -> hookSelectedServer(pool));
    }

    private static void hookActionNameCache(ClassPool pool) throws Exception {
        CtClass type = pool.getCtClass("com.wurmonline.shared.constants.PlayerAction");
        type.getMethod("getName", "()Ljava/lang/String;").insertAfter(
                "org.keybinder.wurm.KeybinderMod.rememberActionName(this.id, $_);");
    }

    private static void hookActionMenuPaths(ClassPool pool) throws Exception {
        CtClass abstractButton = pool.getCtClass(
                "com.wurmonline.client.renderer.gui.WurmPopup$WPopupAbstractButton");
        abstractButton.getDeclaredMethod("mouseMoved").insertBefore(
                "org.keybinder.wurm.KeybinderMod.rememberPopupSubmenu("
                        + "this.this$0, this.getSubmenu(), this.getLabel());");
        CtClass button = pool.getCtClass(
                "com.wurmonline.client.renderer.gui.WurmPopup$WPopupActionButton");
        button.getDeclaredMethod("handleLeftClick").insertBefore(
                "org.keybinder.wurm.KeybinderMod.rememberActionMenuPath("
                        + "this.this$0, this.action);");
    }

    private static void hookConsole(ClassPool pool) throws Exception {
        CtClass console = pool.getCtClass("com.wurmonline.client.console.WurmConsole");
        console.getMethod("handleDevInput", "(Ljava/lang/String;[Ljava/lang/String;)Z")
                .insertBefore(
                        "if (org.keybinder.wurm.KeybinderMod.handleCommand($1,$2)) return true;");
    }

    private static void hookLongPress(ClassPool pool) throws Exception {
        CtClass console = pool.getCtClass("com.wurmonline.client.console.WurmConsole");
        console.getMethod("toggleKey", "(IZ)V").insertBefore(
                "if (org.keybinder.wurm.KeybinderMod.handleKeyToggle(this,$1,$2)) return;");
        CtClass eventHandler = pool.getCtClass("com.wurmonline.client.WurmEventHandler");
        eventHandler.getDeclaredMethod("keyPressed",
                new CtClass[]{CtClass.intType, CtClass.charType}).insertBefore(
                "org.keybinder.wurm.KeybinderMod.observeKeyPressed($1);");
        eventHandler.getDeclaredMethod("keyReleased",
                new CtClass[]{CtClass.intType, CtClass.charType}).insertBefore(
                "org.keybinder.wurm.KeybinderMod.observeKeyReleased($1);");
    }

    private static void hookMouseWheel(ClassPool pool) throws Exception {
        pool.getCtClass("com.wurmonline.client.WurmEventHandler")
                .getMethod("mouseWheeled", "(III)V").insertBefore(
                "org.keybinder.wurm.KeybinderMod.handleMouseWheel($1,$2,$3);");
    }

    private static void hookConnectionLifecycle(ClassPool pool) throws Exception {
        CtClass connection = pool.getCtClass(
                "com.wurmonline.client.comm.SimpleServerConnectionClass");
        connection.getMethod("disconnect", "(Ljava/lang/String;)V").insertBefore(
                "org.keybinder.wurm.KeybinderMod.onConnectionEnded();");
        connection.getMethod("disconnectAndConnectTo", "(Ljava/lang/String;I)V")
                .insertBefore(
                        "org.keybinder.wurm.KeybinderMod.onServerTransfer($1, $2);");
        pool.getCtClass("com.wurmonline.client.game.World")
                .getMethod("setServerInformation", "(IZLjava/lang/String;)V")
                .insertAfter(
                        "org.keybinder.wurm.KeybinderMod.onServerInformation($3);");
    }

    private static void hookActionSource(ClassPool pool) throws Exception {
        pool.getCtClass("com.wurmonline.client.renderer.gui.HeadsUpDisplay")
                .getMethod("getSourceItemId", "()J").insertAfter(
                "{ $_ = org.keybinder.wurm.integration.ActionSourceOverride.overrideOr($_); }");
        ActionSourceOverride.markHookAvailable();
    }

    private static void hookEmbarkHeading(ClassPool pool) throws Exception {
        pool.getCtClass("com.wurmonline.client.game.PlayerObj").getMethod("setController",
                "(Lcom/wurmonline/client/renderer/cell/CreatureCellRenderable;FFFFFFFB)V")
                .insertAfter(
                        "org.keybinder.wurm.KeybinderMod.alignViewAfterEmbark(this, $8);");
    }

    private static void hookHud() {
        HookManager manager = HookManager.getInstance();
        manager.registerHook("com.wurmonline.client.renderer.gui.HeadsUpDisplay",
                "init", "(II)V", () -> (proxy, method, args) -> {
                    Object result = method.invoke(proxy, args);
                    KeybinderMod.onHudReady((HeadsUpDisplay) proxy);
                    return result;
                });
        manager.registerHook("com.wurmonline.client.renderer.gui.HeadsUpDisplay",
                "gameTick", "()V", () -> (proxy, method, args) -> {
                    Object result = method.invoke(proxy, args);
                    KeybinderMod.onHudTick((HeadsUpDisplay) proxy);
                    return result;
                });
    }

    private void hookSelectedServer(ClassPool pool) {
        try {
            CtClass browser = pool.getCtClass(
                    "com.wurmonline.client.startup.ServerBrowserFX");
            browser.getMethod("ConnectTo", "(Ljavafx/scene/control/TableView;)V")
                    .insertBefore(
                            "org.keybinder.wurm.KeybinderMod.rememberSelectedServer($1);");
            browser.getMethod("ConnectWithPassword",
                    "(Ljavafx/scene/control/TableView;Ljava/lang/String;Ljava/lang/String;)V")
                    .insertBefore(
                            "org.keybinder.wurm.KeybinderMod.rememberSelectedServer($1);");
        } catch (Throwable failure) {
            logger.log(Level.WARNING, "Full server-name capture is unavailable", failure);
        }
    }

    private static void hookActionCapture() {
        HookManager manager = HookManager.getInstance();
        manager.registerHook(
                "com.wurmonline.client.comm.SimpleServerConnectionClass",
                "sendAction", "(J[JLcom/wurmonline/shared/constants/PlayerAction;)V",
                () -> (proxy, method, args) -> {
                    PlayerAction action = (PlayerAction) args[2];
                    KeybinderMod.observeCapturedAction(action);
                    KeybinderMod.observeWorldImproveAction((long[]) args[1], action);
                    return method.invoke(proxy, args);
                });
        manager.registerHook("com.wurmonline.client.game.World",
                "sendHoveredAction", "(Lcom/wurmonline/shared/constants/PlayerAction;)V",
                () -> (proxy, method, args) ->
                        KeybinderMod.withCapturedTarget("hover", proxy, method, args));
        manager.registerHook("com.wurmonline.client.game.World",
                "sendLocalAction", "(Lcom/wurmonline/shared/constants/PlayerAction;)V",
                () -> (proxy, method, args) ->
                        KeybinderMod.withCapturedTarget("tile", proxy, method, args));
    }

    private static void hookWorldImprove(ClassPool pool) throws Exception {
        pool.getCtClass("com.wurmonline.client.renderer.gui.ChatPanelComponent")
                .getMethod("addText", "(Ljava/lang/String;Ljava/lang/String;FFFZ)V")
                .insertBefore(
                        "org.keybinder.wurm.KeybinderMod.observeWorldImproveEvent($1, $2);");
        CtClass selectBar = pool.getCtClass(
                "com.wurmonline.client.renderer.gui.SelectBar");
        selectBar.getDeclaredMethod("setSelected").insertAfter(
                "org.keybinder.wurm.KeybinderMod.observeWorldImproveSelection($1);");
        selectBar.getDeclaredMethod("clearSelectedItem").insertAfter(
                "org.keybinder.wurm.KeybinderMod.observeWorldImproveSelection(null);");
    }

    private static void hookActionQueue(ClassPool pool) throws Exception {
        CtClass connection = pool.getCtClass(
                "com.wurmonline.client.comm.SimpleServerConnectionClass");
        connection.getMethod("sendAction",
                "(J[JLcom/wurmonline/shared/constants/PlayerAction;)V").insertAfter(
                "org.keybinder.wurm.KeybinderMod.observeActionSent($2, $3);");
        connection.getMethod("sendSingleAction",
                "(JJLcom/wurmonline/shared/constants/PlayerAction;)V").insertAfter(
                "org.keybinder.wurm.KeybinderMod.observeSingleActionSent();");
        pool.getCtClass("com.wurmonline.client.renderer.gui.HeadsUpDisplay")
                .getMethod("setAction", "(Ljava/lang/String;F)V").insertAfter(
                "org.keybinder.wurm.KeybinderMod.observeActionState($1, $2);");
    }

    private static void hookToolbeltSelection() {
        HookManager.getInstance().registerHook(
                "com.wurmonline.client.renderer.gui.ToolBeltComponent",
                "leftPressed", "(III)V", () -> KeybinderMod::interceptToolbeltSelection);
    }

    private static void hookEquipmentSelection() {
        HookManager.getInstance().registerHook(
                "com.wurmonline.client.renderer.gui.PaperDollInventory",
                "leftPressed", "(III)V", () -> KeybinderMod::interceptEquipmentSelection);
    }

    private static void hookWorldSelection(ClassPool pool) throws Exception {
        CtClass eventHandler = pool.getCtClass("com.wurmonline.client.WurmEventHandler");
        eventHandler.getMethod("mousePressed", "(IIII)V").insertBefore(
                "org.keybinder.wurm.KeybinderMod.observeMousePressed($1, $2, $3);");
        eventHandler.getMethod("mouseDragged", "(II)V").insertBefore(
                "org.keybinder.wurm.KeybinderMod.observeMouseDragged();");
        eventHandler.getMethod("mouseReleased", "(III)V").insertBefore(
                "org.keybinder.wurm.KeybinderMod.observeMouseReleased(this, $1, $2, $3);");
    }

    private static void hookInventorySelection() {
        HookManager.getInstance().registerHook(
                "com.wurmonline.client.renderer.gui.WurmTreeList$TreeListPanel",
                "leftPressed", "(III)V", () -> (proxy, method, args) -> {
                    KeybinderMod.captureInventoryTarget(
                            proxy, (Integer) args[0], (Integer) args[1]);
                    return method.invoke(proxy, args);
                });
    }

    private static void hookCreationSkillCatalog(ClassPool pool) throws Exception {
        pool.getCtClass("com.wurmonline.client.comm.ServerConnectionListenerClass")
                .getDeclaredMethod("addItemToCreationList").insertAfter(
                "org.keybinder.wurm.integration.CreationSkillRegistry.observe($1);");
    }

    private static void hookBulkTransferBml() {
        HookManager.getInstance().registerHook(
                "com.wurmonline.client.renderer.gui.HeadsUpDisplay", "showBml",
                "(SLjava/lang/String;IIFFZZFFFLjava/lang/String;)V",
                () -> (proxy, method, args) -> {
                    if (KeybinderMod.interceptBulkTransferBml(
                            (HeadsUpDisplay) proxy, (String) args[1], (String) args[11]))
                        return null;
                    return method.invoke(proxy, args);
                });
    }

    private static void hookPushSelection(ClassPool pool) throws Exception {
        pool.getCtClass("com.wurmonline.client.renderer.gui.SelectBar")
                .getMethod("setNewSelectedIfKeepId",
                        "(Lcom/wurmonline/client/renderer/PickableUnit;)V")
                .insertAfter(
                        "org.keybinder.wurm.KeybinderMod.afterPushTargetRecreated(this, $1);");
    }
}
