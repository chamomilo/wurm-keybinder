package org.keybinder.wurm.integration;

import com.wurmonline.client.comm.ServerConnectionListenerClass;
import com.wurmonline.client.console.WurmConsole;
import com.wurmonline.client.game.inventory.InventoryMetaItem;
import com.wurmonline.client.renderer.PickableUnit;
import com.wurmonline.client.renderer.ObjectData;
import com.wurmonline.client.renderer.cell.GroundItemCellRenderable;
import com.wurmonline.client.renderer.cell.CreatureCellRenderable;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.client.renderer.gui.PaperDollInventory;
import com.wurmonline.client.renderer.gui.PaperDollSlot;
import com.wurmonline.client.renderer.gui.SelectBar;
import com.wurmonline.client.renderer.gui.KeybinderSelectionBridge;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("unchecked")
public final class ClientAccess {
    private Field bodyItem;
    private Field activeToolItem;
    private Field selectedUnit;
    private Field groundItems;
    private Field groundItemData;
    private Field console;
    private Field hudSettings;
    private Field mainMenu;
    private Field savePosManager;
    private Field components;
    private Field toolbeltComponent;
    private Field paperDollInventory;
    private Method isComponentEnabled;
    private Method hideComponent;
    private Method toggleComponent;
    private Method frameFromSlot;
    private Method setActiveToolItem;
    private Method removeActiveToolItem;
    private Field inventoryWindows;

    public void setup() throws ReflectiveOperationException {
        // Core HUD access is resolved strictly. Optional gameplay capabilities are
        // resolved independently so a client change in equipment/selection does not
        // prevent the Keybinder window and console dispatcher from loading.
        console = HeadsUpDisplay.class.getDeclaredField("console");
        hudSettings = HeadsUpDisplay.class.getDeclaredField("hudSettings");
        mainMenu = HeadsUpDisplay.class.getDeclaredField("mainMenu");
        savePosManager = HeadsUpDisplay.class.getDeclaredField("savePosManager");
        components = HeadsUpDisplay.class.getDeclaredField("components");
        isComponentEnabled = ReflectionUtil.getMethod(HeadsUpDisplay.class, "isComponentEnabled",
                new Class[]{com.wurmonline.client.renderer.gui.WurmComponent.class});
        hideComponent = ReflectionUtil.getMethod(HeadsUpDisplay.class, "hideComponent",
                new Class[]{com.wurmonline.client.renderer.gui.WurmComponent.class});
        toggleComponent = ReflectionUtil.getMethod(HeadsUpDisplay.class, "toggleComponent",
                new Class[]{com.wurmonline.client.renderer.gui.WurmComponent.class});
        bodyItem = optionalDeclaredField(PaperDollInventory.class, "bodyItem");
        activeToolItem = optionalDeclaredField(HeadsUpDisplay.class, "activeToolItem");
        selectedUnit = optionalDeclaredField(SelectBar.class, "selectedUnit");
        groundItems = optionalField(ServerConnectionListenerClass.class, "groundItems");
        groundItemData = optionalDeclaredField(GroundItemCellRenderable.class, "item");
        toolbeltComponent = optionalDeclaredField(HeadsUpDisplay.class, "toolbeltComponent");
        paperDollInventory = optionalDeclaredField(HeadsUpDisplay.class, "paperdollInventory");
        frameFromSlot = optionalMethod(PaperDollInventory.class,
                "getFrameFromSlotnumber", new Class[]{byte.class});
        setActiveToolItem = optionalMethod(HeadsUpDisplay.class,
                "setActiveToolItem", new Class[]{InventoryMetaItem.class});
        removeActiveToolItem = optionalMethod(HeadsUpDisplay.class,
                "removeActiveToolItem", new Class[]{InventoryMetaItem.class});
        inventoryWindows = optionalDeclaredField(
                com.wurmonline.client.game.inventory.InventoryMetaWindowManager.class,
                "inventoryWindows");
    }

    public InventoryMetaItem bodyItem(PaperDollInventory paperDoll) throws ReflectiveOperationException {
        PaperDollSlot slot = ReflectionUtil.getPrivateField(paperDoll,
                required(bodyItem, "body target"));
        return slot == null ? null : slot.getItem();
    }

    public InventoryMetaItem activeTool(HeadsUpDisplay hud) throws ReflectiveOperationException {
        return ReflectionUtil.getPrivateField(hud, required(activeToolItem, "active tool"));
    }

    public void setActiveTool(HeadsUpDisplay hud, InventoryMetaItem item) throws ReflectiveOperationException {
        InventoryMetaItem current = activeTool(hud);
        if (item == null) {
            if (current != null) ReflectionUtil.callPrivateMethod(hud,
                    required(removeActiveToolItem, "remove active tool"), current);
        } else {
            ReflectionUtil.callPrivateMethod(hud,
                    required(setActiveToolItem, "set active tool"), item);
        }
    }

    public InventoryMetaItem inventoryItem(HeadsUpDisplay hud, long id) throws ReflectiveOperationException {
        com.wurmonline.client.game.inventory.InventoryMetaWindowManager manager =
                hud.getWorld().getInventoryManager();
        InventoryMetaItem item = manager.getPlayerInventory().getItem(id);
        if (item != null) return item;
        item = findInventoryItem(manager.getPlayerInventory().getRootItem(), id);
        if (item != null) return item;
        item = manager.getPlayerEquipment().getItem(id);
        if (item != null) return item;
        item = findInventoryItem(manager.getPlayerEquipment().getRootItem(), id);
        if (item != null) return item;
        Map<Long, com.wurmonline.client.game.inventory.InventoryMetaWindowView> windows =
                ReflectionUtil.getPrivateField(manager,
                        required(inventoryWindows, "inventory window lookup"));
        for (com.wurmonline.client.game.inventory.InventoryMetaWindowView window : windows.values()) {
            if (window == null) continue;
            item = window.getItem(id);
            if (item != null) return item;
            item = findInventoryItem(window.getRootItem(), id);
            if (item != null) return item;
        }
        return null;
    }

    private static InventoryMetaItem findInventoryItem(InventoryMetaItem root, long id) {
        if (root == null) return null;
        ArrayDeque<InventoryMetaItem> pending = new ArrayDeque<InventoryMetaItem>();
        Set<Long> visited = new HashSet<Long>();
        pending.push(root);
        while (!pending.isEmpty()) {
            InventoryMetaItem item = pending.pop();
            if (!visited.add(item.getId())) continue;
            if (item.getId() == id) return item;
            java.util.List<InventoryMetaItem> children = item.getChildren();
            if (children == null) continue;
            for (InventoryMetaItem child : children) {
                if (child != null) pending.push(child);
            }
        }
        return null;
    }

    public PickableUnit selected(SelectBar bar) throws ReflectiveOperationException {
        return ReflectionUtil.getPrivateField(bar, required(selectedUnit, "selected target"));
    }

    public void select(SelectBar bar, PickableUnit unit) throws ReflectiveOperationException {
        KeybinderSelectionBridge.select(bar, unit);
    }

    public PaperDollSlot equipmentSlot(PaperDollInventory paperDoll, byte slot) throws ReflectiveOperationException {
        return ReflectionUtil.callPrivateMethod(paperDoll,
                required(frameFromSlot, "equipment slot"), slot);
    }

    public Map<Long, GroundItemCellRenderable> groundItems(ServerConnectionListenerClass listener)
            throws ReflectiveOperationException {
        return ReflectionUtil.getPrivateField(listener,
                required(groundItems, "nearby ground items"));
    }

    public String objectType(PickableUnit unit) throws ReflectiveOperationException {
        if (unit instanceof GroundItemCellRenderable) {
            ObjectData data = ReflectionUtil.getPrivateField(unit,
                    required(groundItemData, "ground item type"));
            if (data != null && data.getName() != null && !data.getName().trim().isEmpty())
                return data.getName();
        }
        if (unit instanceof CreatureCellRenderable) {
            ObjectData data = ((CreatureCellRenderable) unit).getCreatureData();
            if (data != null && data.getName() != null && !data.getName().trim().isEmpty())
                return data.getName();
        }
        return unit.getHoverName();
    }

    public WurmConsole console(HeadsUpDisplay hud) throws ReflectiveOperationException {
        return ReflectionUtil.getPrivateField(hud, console);
    }

    public Object hudSettings(HeadsUpDisplay hud) throws ReflectiveOperationException {
        return ReflectionUtil.getPrivateField(hud, hudSettings);
    }

    public Object mainMenu(HeadsUpDisplay hud) throws ReflectiveOperationException {
        return ReflectionUtil.getPrivateField(hud, mainMenu);
    }

    public Object savePosManager(HeadsUpDisplay hud) throws ReflectiveOperationException {
        return ReflectionUtil.getPrivateField(hud, savePosManager);
    }

    public java.util.List<Object> components(HeadsUpDisplay hud) throws ReflectiveOperationException {
        return (java.util.List<Object>) ReflectionUtil.getPrivateField(hud, components);
    }

    public void ensureToolbeltVisible(HeadsUpDisplay hud) throws ReflectiveOperationException {
        com.wurmonline.client.renderer.gui.WurmComponent component = toolbeltComponent(hud);
        boolean enabled = ReflectionUtil.callPrivateMethod(hud, isComponentEnabled, component);
        if (!enabled) hud.toggleToolbeltVisible();
    }

    public void ensurePaperDollVisible(HeadsUpDisplay hud) throws ReflectiveOperationException {
        com.wurmonline.client.renderer.gui.WurmComponent component = paperDollComponent(hud);
        boolean enabled = ReflectionUtil.callPrivateMethod(hud, isComponentEnabled, component);
        if (!enabled) hud.togglePaperdollInventoryVisible();
    }

    public boolean isToolbeltVisible(HeadsUpDisplay hud) throws ReflectiveOperationException {
        return ReflectionUtil.callPrivateMethod(hud, isComponentEnabled, toolbeltComponent(hud));
    }

    public boolean isPaperDollVisible(HeadsUpDisplay hud) throws ReflectiveOperationException {
        return ReflectionUtil.callPrivateMethod(hud, isComponentEnabled, paperDollComponent(hud));
    }

    public com.wurmonline.client.renderer.gui.WurmComponent toolbeltComponent(HeadsUpDisplay hud)
            throws ReflectiveOperationException {
        return ReflectionUtil.getPrivateField(hud,
                required(toolbeltComponent, "toolbelt component"));
    }

    public com.wurmonline.client.renderer.gui.WurmComponent paperDollComponent(HeadsUpDisplay hud)
            throws ReflectiveOperationException {
        return ReflectionUtil.getPrivateField(hud,
                required(paperDollInventory, "paper doll component"));
    }

    public void hideComponent(HeadsUpDisplay hud, com.wurmonline.client.renderer.gui.WurmComponent component)
            throws ReflectiveOperationException {
        ReflectionUtil.callPrivateMethod(hud, hideComponent, component);
        com.wurmonline.client.renderer.gui.HudSettings settings =
                (com.wurmonline.client.renderer.gui.HudSettings) hudSettings(hud);
        settings.setEnabled(component, false);
    }

    public void ensureComponentVisible(HeadsUpDisplay hud,
                                       com.wurmonline.client.renderer.gui.WurmComponent component)
            throws ReflectiveOperationException {
        boolean enabled = ReflectionUtil.callPrivateMethod(hud, isComponentEnabled, component);
        if (!enabled) ReflectionUtil.callPrivateMethod(hud, toggleComponent, component);
    }

    public void setComponentVisible(HeadsUpDisplay hud,
                                    com.wurmonline.client.renderer.gui.WurmComponent component,
                                    boolean visible) throws ReflectiveOperationException {
        boolean enabled = ReflectionUtil.callPrivateMethod(hud, isComponentEnabled, component);
        if (enabled != visible) ReflectionUtil.callPrivateMethod(hud, toggleComponent, component);
    }

    private static Field optionalDeclaredField(Class<?> type, String name) {
        try {
            return type.getDeclaredField(name);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Field optionalField(Class<?> type, String name) {
        try {
            return ReflectionUtil.getField(type, name);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Method optionalMethod(Class<?> type, String name, Class<?>[] parameters) {
        try {
            return ReflectionUtil.getMethod(type, name, parameters);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static <T> T required(T value, String capability)
            throws ReflectiveOperationException {
        if (value == null)
            throw new NoSuchFieldException("Client capability is unavailable: " + capability);
        return value;
    }
}
