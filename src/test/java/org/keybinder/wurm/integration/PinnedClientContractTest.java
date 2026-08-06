package org.keybinder.wurm.integration;

import javassist.ClassPool;
import javassist.CtClass;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/** Fails early when a pinned Wurm client update changes a required hook signature. */
public class PinnedClientContractTest {
    @Test public void requiredLifecycleConsoleAndSendSignaturesRemainAvailable()
            throws Exception {
        ClassPool pool = clientPool();
        assertMethod(pool, "com.wurmonline.client.renderer.gui.HeadsUpDisplay",
                "init", "(II)V");
        assertMethod(pool, "com.wurmonline.client.renderer.gui.HeadsUpDisplay",
                "textMessage", "(Ljava/lang/String;FFFLjava/lang/String;)V");
        assertMethod(pool, "com.wurmonline.client.renderer.gui.HeadsUpDisplay",
                "textMessage", "(Ljava/lang/String;Ljava/util/List;)V");
        assertMethod(pool, "com.wurmonline.client.console.WurmConsole",
                "handleDevInput", "(Ljava/lang/String;[Ljava/lang/String;)Z");
        assertMethod(pool, "com.wurmonline.shared.constants.PlayerAction",
                "getName", "()Ljava/lang/String;");
        assertMethod(pool, "com.wurmonline.client.comm.SimpleServerConnectionClass",
                "sendAction", "(J[JLcom/wurmonline/shared/constants/PlayerAction;)V");
        CtClass eventHandler = pool.getCtClass("com.wurmonline.client.WurmEventHandler");
        assertNotNull(eventHandler.getDeclaredMethod("keyPressed",
                new CtClass[]{CtClass.intType, CtClass.charType}));
        assertNotNull(eventHandler.getDeclaredMethod("keyReleased",
                new CtClass[]{CtClass.intType, CtClass.charType}));
    }

    @Test public void requiredSelectionHelpersRemainAvailable() throws Exception {
        ClassPool pool = clientPool();
        assertMethod(pool, "com.wurmonline.client.renderer.gui.ToolBeltComponent",
                "leftPressed", "(III)V");
        assertMethod(pool, "com.wurmonline.client.renderer.gui.ToolBeltComponent",
                "getSlotNumberAt", "(IIZ)I");
        assertMethod(pool, "com.wurmonline.client.renderer.gui.PaperDollInventory",
                "leftPressed", "(III)V");
        assertMethod(pool, "com.wurmonline.client.renderer.gui.PaperDollInventory",
                "getSlotNumberAt", "(II)B");
    }

    @Test public void smartImproveMaterialAndLifecycleContractsRemainAvailable()
            throws Exception {
        ClassPool pool = clientPool();
        assertMethod(pool, "com.wurmonline.client.game.inventory.InventoryMetaItem",
                "getMaterialId", "()B");
        assertMethod(pool, "com.wurmonline.client.renderer.ObjectData",
                "getMaterialId", "()B");
        assertMethod(pool, "com.wurmonline.client.renderer.gui.PaperDollInventory",
                "getHandItem",
                "()Lcom/wurmonline/client/game/inventory/InventoryMetaItem;");
        assertNotNull(pool.getCtClass(
                "com.wurmonline.client.renderer.cell.GroundItemCellRenderable")
                .getDeclaredField("item"));
        assertMethod(pool, "com.wurmonline.client.comm.SimpleServerConnectionClass",
                "disconnect", "(Ljava/lang/String;)V");
        assertMethod(pool, "com.wurmonline.client.comm.SimpleServerConnectionClass",
                "disconnectAndConnectTo", "(Ljava/lang/String;I)V");
        assertMethod(pool, "com.wurmonline.client.game.World",
                "setServerInformation", "(IZLjava/lang/String;)V");
    }

    @Test public void worldObjectsDoNotExposeInventoryImproveMetadata()
            throws Exception {
        ClassPool pool = clientPool();
        CtClass objectData = pool.getCtClass(
                "com.wurmonline.client.renderer.ObjectData");
        // Applies equally to the requested forge, altar, FSB, and a wagon
        // hitched to horses: hitching changes vehicle state, not ObjectData's
        // item metadata contract.
        assertNoDeclaredMethod(objectData, "getImproveIconId");
        assertNoDeclaredMethod(objectData, "getQuality");
        assertNoDeclaredMethod(objectData, "getDamage");
        assertNotNull(objectData.getDeclaredMethod("getMaterialId"));
        assertNotNull(objectData.getDeclaredMethod("getIconId"));
    }

    @Test public void worldImproveExamineHooksRemainAvailable() throws Exception {
        ClassPool pool = clientPool();
        assertMethod(pool, "com.wurmonline.client.renderer.gui.ChatPanelComponent",
                "addText", "(Ljava/lang/String;Ljava/lang/String;FFFZ)V");
        CtClass selectBar = pool.getCtClass(
                "com.wurmonline.client.renderer.gui.SelectBar");
        assertNotNull(selectBar.getDeclaredMethod("setSelected"));
        assertNotNull(selectBar.getDeclaredMethod("clearSelectedItem"));
    }

    private static ClassPool clientPool() throws Exception {
        ClassPool pool = new ClassPool(false);
        pool.appendClassPath("libs/client-patched.jar");
        pool.appendClassPath("libs/common.jar");
        return pool;
    }

    private static void assertMethod(ClassPool pool, String className,
                                     String method, String descriptor) throws Exception {
        CtClass type = pool.getCtClass(className);
        assertNotNull(type.getMethod(method, descriptor));
    }

    private static void assertNoDeclaredMethod(CtClass type, String method) {
        try {
            type.getDeclaredMethod(method);
            fail(type.getName() + " unexpectedly exposes " + method);
        } catch (javassist.NotFoundException expected) {
            // Expected client contract: world ObjectData is only render data.
        }
    }
}
