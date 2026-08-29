package org.keybinder.wurm.integration;

import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.client.renderer.gui.KeybinderActionQueueMonitor;
import com.wurmonline.client.renderer.gui.KeybinderWindow;
import com.wurmonline.client.renderer.gui.KeybinderTagWindow;
import com.wurmonline.client.renderer.gui.MainMenu;
import com.wurmonline.client.settings.SavePosManager;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;

import java.lang.reflect.Method;

public final class HudIntegration {
    private final ClientAccess access;

    public HudIntegration(ClientAccess access) {
        this.access = access;
    }

    public void register(HeadsUpDisplay hud, KeybinderWindow window) throws ReflectiveOperationException {
        add(hud, window);
        MainMenu menu = (MainMenu) access.mainMenu(hud);
        menu.registerComponent("Keybinder", window);
        access.hideComponent(hud, window);
        SavePosManager positions = (SavePosManager) access.savePosManager(hud);
        positions.registerAndRefresh(window, "keybinder");
    }

    public void registerTag(HeadsUpDisplay hud, KeybinderTagWindow tag) throws ReflectiveOperationException {
        add(hud, tag);
        SavePosManager positions = (SavePosManager) access.savePosManager(hud);
        positions.registerAndRefresh(tag, "keybinder.tag");
    }

    public void registerQueueMonitor(HeadsUpDisplay hud,
                                     KeybinderActionQueueMonitor monitor)
            throws ReflectiveOperationException {
        add(hud, monitor);
        access.ensureComponentVisible(hud, monitor);
    }

    public void add(HeadsUpDisplay hud, com.wurmonline.client.renderer.gui.WurmComponent component)
            throws ReflectiveOperationException {
        Method addComponent = ReflectionUtil.getMethod(HeadsUpDisplay.class, "addComponent",
                new Class[]{com.wurmonline.client.renderer.gui.WurmComponent.class});
        ReflectionUtil.callPrivateMethod(hud, addComponent, component);
    }
}
