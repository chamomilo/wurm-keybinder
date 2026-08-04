package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.game.inventory.InventoryMetaItem;
import com.wurmonline.client.game.inventory.InventoryMetaWindowView;
import org.keybinder.wurm.integration.BulkInventoryDestinationPolicy;
import org.keybinder.wurm.integration.InventorySelectionPolicy;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Package-access adapter for resolving the inventory row clicked in Wurm's
 * package-private tree implementation.
 */
public final class KeybinderInventorySelectionBridge {
    private KeybinderInventorySelectionBridge() {
    }

    public static InventoryMetaItem itemAt(Object panel, int x, int y) {
        Row row = rowAt(panel, x, y);
        if (row == null) return null;
        if (!InventorySelectionPolicy.isSelectable(
                row.inventoryGroup, row.item.getBaseName(), row.item.getDisplayName()))
            return null;
        return row.item;
    }

    /**
     * Resolves a strict bulk destination under the global mouse position. A
     * container row resolves to that row; every non-container row and the
     * window background/frame resolve to the window root.
     */
    public static InventoryMetaItem itemUnderMouse(HeadsUpDisplay hud, int x, int y)
            throws ReflectiveOperationException {
        if (hud == null) return null;
        WurmComponent component = hud.getComponentAt(x, y);
        while (component != null) {
            if (component instanceof InventoryListComponent) {
                InventoryListComponent list = (InventoryListComponent) component;
                InventoryListComponent.InventoryTreeListItem row =
                        list.getDraggableComponentAt(x, y);
                InventoryListComponent.InventoryTreeListItem root = list.rootItem;
                return choose(row == null ? null : row.item,
                        row != null && row.isContainer,
                        root == null ? null : root.item);
            }
            if (component instanceof InventoryContainerWindow) {
                InventoryContainerWindow containerWindow =
                        (InventoryContainerWindow) component;
                InventoryContainerWindow.InventoryContainerItem row =
                        containerWindow.getItemAt(x, y);
                return choose(row == null ? null : row.getItem(),
                        row != null && row.getIsContainer(),
                        containerRoot(containerWindow));
            }
            if (component instanceof InventoryWindow) {
                InventoryListComponent list =
                        ((InventoryWindow) component).getInventoryListComponent();
                InventoryListComponent.InventoryTreeListItem root =
                        list == null ? null : list.rootItem;
                return root == null ? null : root.item;
            }
            component = component.parent;
        }
        return null;
    }

    /**
     * Resolves the main player inventory without relying on the parent chain
     * returned by {@link HeadsUpDisplay#getComponentAt(int, int)}. The client
     * can return the inventory's inner tree panel with an incomplete parent
     * chain, especially for the permanently managed player Inventory window.
     *
     * Ownership is still strict: the deepest component returned by the HUD
     * must be the same object returned by the player window or its list. This
     * prevents an obscured player Inventory from winning through geometry when
     * another window is actually on top.
     */
    public static PlayerInventoryHit playerInventoryUnderMouse(
            HeadsUpDisplay hud, int x, int y) {
        if (hud == null)
            return PlayerInventoryHit.miss("hud=null");
        InventoryWindow window = hud.getInventoryWindow();
        InventoryListComponent canonicalList = window == null
                ? null : window.getInventoryListComponent();
        WurmComponent topComponent = null;
        String topProbeFailure = null;
        try {
            topComponent = topComponentAt(hud, x, y);
        } catch (ReflectiveOperationException | RuntimeException failure) {
            topProbeFailure = failure.getClass().getSimpleName() + ":"
                    + String.valueOf(failure.getMessage());
        }
        long playerWindowId = playerWindowId(hud);
        InventoryListComponent topList = null;
        try {
            topList = inventoryListOf(topComponent);
        } catch (ReflectiveOperationException | RuntimeException failure) {
            topProbeFailure = appendFailure(topProbeFailure,
                    "inventoryListOf=" + failure.getClass().getSimpleName() + ":"
                            + String.valueOf(failure.getMessage()));
        }
        InventoryListComponent list = isPlayerList(topList, hud, playerWindowId)
                ? topList : canonicalList;
        if (list == null)
            return PlayerInventoryHit.miss("list=null; " + topProbeFailure);

        WurmComponent hudComponent = hud.getComponentAt(x, y);
        WurmComponent windowComponent = window == null
                ? null : window.getComponentAt(x, y);
        WurmComponent listComponent = list.getComponentAt(x, y);
        boolean topListIsPlayer = isPlayerList(topList, hud, playerWindowId);
        boolean ownsTopComponent = topComponent == window || topListIsPlayer
                || (hudComponent != null
                && (hudComponent == windowComponent || hudComponent == listComponent));

        InventoryListComponent.InventoryTreeListItem root = list.rootItem;
        InventoryListComponent.InventoryTreeListItem row = ownsTopComponent
                ? list.getDraggableComponentAt(x, y) : null;
        long nativeDropTarget = 0L;
        try {
            nativeDropTarget = droppedOnTargetId(list, x, y);
        } catch (ReflectiveOperationException | RuntimeException failure) {
            topProbeFailure = appendFailure(topProbeFailure,
                    "getDroppedOnTargetId=" + failure.getClass().getSimpleName() + ":"
                            + String.valueOf(failure.getMessage()));
        }
        InventoryMetaItem selected = ownsTopComponent
                ? choose(row == null ? null : row.item,
                row != null && row.isContainer,
                root == null ? null : root.item)
                : null;
        return new PlayerInventoryHit(selected, ownsTopComponent,
                row == null || row.item == null ? 0L : row.item.getId(),
                root == null || root.item == null ? 0L : root.item.getId(),
                playerWindowId, inventoryWindowId(list),
                "nativeDropTarget=" + nativeDropTarget
                        + ", rootDropTarget=" + (root == null ? 0L : root.getDropTarget())
                        + ", rowDropTarget=" + (row == null ? 0L : row.getDropTarget())
                        + ", rootItem=" + describeItem(root == null ? null : root.item)
                        + ", rowItem=" + describeItem(row == null ? null : row.item)
                        + ", rootChildren=" + describeChildren(
                        root == null ? null : root.item),
                describe(topComponent), topProbeFailure,
                describe(hudComponent), describe(windowComponent),
                describe(listComponent), topListIsPlayer && topComponent != null
                ? topComponent.contains(x, y) : window != null && window.contains(x, y),
                list.contains(x, y));
    }

    /** Signature-checked access to the HUD's authoritative z-ordered window hit. */
    private static WurmComponent topComponentAt(HeadsUpDisplay hud, int x, int y)
            throws ReflectiveOperationException {
        Method method = HeadsUpDisplay.class.getDeclaredMethod(
                "getTopComponentAt", int.class, int.class);
        if (!WurmComponent.class.isAssignableFrom(method.getReturnType()))
            throw new NoSuchMethodException("HeadsUpDisplay.getTopComponentAt return type changed");
        method.setAccessible(true);
        Object result = method.invoke(hud, x, y);
        return result instanceof WurmComponent ? (WurmComponent) result : null;
    }

    /** Mirrors the client's private native drag-and-drop destination calculation. */
    private static long droppedOnTargetId(InventoryListComponent list, int x, int y)
            throws ReflectiveOperationException {
        Method method = InventoryListComponent.class.getDeclaredMethod(
                "getDroppedOnTargetId", int.class, int.class);
        if (method.getReturnType() != long.class)
            throw new NoSuchMethodException(
                    "InventoryListComponent.getDroppedOnTargetId return type changed");
        method.setAccessible(true);
        return ((Long) method.invoke(list, x, y)).longValue();
    }

    /** Live root represented by the HUD's managed player Inventory window. */
    public static InventoryMetaItem playerInventoryRoot(HeadsUpDisplay hud) {
        if (hud == null) return null;
        InventoryListComponent canonical = hud.getInventoryWindow() == null
                ? null : hud.getInventoryWindow().getInventoryListComponent();
        InventoryMetaItem root = rootOf(canonical);
        if (root != null) return root;
        long playerWindowId = playerWindowId(hud);
        try {
            Field components = HeadsUpDisplay.class.getDeclaredField("components");
            if (!List.class.isAssignableFrom(components.getType()))
                throw new NoSuchFieldException("HeadsUpDisplay.components type changed");
            components.setAccessible(true);
            Object value = components.get(hud);
            if (!(value instanceof List)) return null;
            for (Object component : (List<?>) value) {
                InventoryListComponent list = component instanceof WurmComponent
                        ? inventoryListOf((WurmComponent) component) : null;
                if (!isPlayerList(list, hud, playerWindowId)) continue;
                root = rootOf(list);
                if (root != null) return root;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // Optional compatibility path for server-provided HUD replacements.
        }
        return null;
    }

    private static InventoryListComponent inventoryListOf(WurmComponent component)
            throws ReflectiveOperationException {
        if (component == null) return null;
        if (component instanceof InventoryListComponent)
            return (InventoryListComponent) component;
        if (component instanceof InventoryWindow)
            return ((InventoryWindow) component).getInventoryListComponent();
        Class<?> type = component.getClass();
        while (type != null && WurmComponent.class.isAssignableFrom(type)) {
            for (Field field : type.getDeclaredFields()) {
                if (!InventoryListComponent.class.isAssignableFrom(field.getType())) continue;
                field.setAccessible(true);
                Object value = field.get(component);
                if (value instanceof InventoryListComponent)
                    return (InventoryListComponent) value;
            }
            type = type.getSuperclass();
        }
        return null;
    }

    private static InventoryMetaItem rootOf(InventoryListComponent list) {
        if (list == null) return null;
        InventoryListComponent.InventoryTreeListItem root = list.rootItem;
        if (root != null && root.item != null) return root.item;
        return list.inventoryWindow == null ? null : list.inventoryWindow.getRootItem();
    }

    private static boolean isPlayerList(InventoryListComponent list,
                                        HeadsUpDisplay hud, long playerWindowId) {
        if (list == null || list.inventoryWindow == null || hud == null) return false;
        InventoryMetaWindowView player = hud.getWorld().getInventoryManager()
                .getPlayerInventory();
        if (list.inventoryWindow == player) return true;
        long candidateId = inventoryWindowId(list);
        return playerWindowId != 0L && candidateId == playerWindowId;
    }

    private static long playerWindowId(HeadsUpDisplay hud) {
        if (hud == null || hud.getWorld() == null
                || hud.getWorld().getInventoryManager() == null
                || hud.getWorld().getInventoryManager().getPlayerInventory() == null)
            return 0L;
        return hud.getWorld().getInventoryManager().getPlayerInventory().getWindowId();
    }

    private static long inventoryWindowId(InventoryListComponent list) {
        return list == null || list.inventoryWindow == null ? 0L
                : list.inventoryWindow.getWindowId();
    }

    private static String appendFailure(String current, String addition) {
        return current == null ? addition : current + "; " + addition;
    }

    private static String describeItem(InventoryMetaItem item) {
        if (item == null) return "null";
        List<InventoryMetaItem> children = item.getChildren();
        return "{id=" + item.getId() + ",parent=" + item.getParentId()
                + ",base='" + item.getBaseName() + "',display='"
                + item.getDisplayName() + "',typeBits=" + item.getTypeBits()
                + ",container="
                + com.wurmonline.shared.util.ItemTypeUtilites.isContainer(
                item.getTypeBits())
                + ",children=" + (children == null ? 0 : children.size()) + "}";
    }

    private static String describeChildren(InventoryMetaItem root) {
        if (root == null || root.getChildren() == null) return "[]";
        StringBuilder result = new StringBuilder("[");
        int count = 0;
        for (InventoryMetaItem child : root.getChildren()) {
            if (child == null) continue;
            if (count > 0) result.append(',');
            if (count == 12) {
                result.append("...+").append(root.getChildren().size() - count);
                break;
            }
            result.append(describeItem(child));
            count++;
        }
        return result.append(']').toString();
    }

    private static String describe(WurmComponent component) {
        if (component == null) return "null";
        return component.getClass().getSimpleName() + "(" + component.name
                + "," + component.x + "," + component.y + ","
                + component.width + "x" + component.height + ")";
    }

    private static InventoryMetaItem choose(InventoryMetaItem row, boolean rowIsContainer,
                                            InventoryMetaItem windowRoot) {
        long selected = BulkInventoryDestinationPolicy.resolve(
                row == null ? 0L : row.getId(), rowIsContainer,
                windowRoot == null ? 0L : windowRoot.getId());
        if (selected <= 0L) return null;
        return row != null && selected == row.getId() ? row : windowRoot;
    }

    /** Signature-checked private accessor for the alternate container window. */
    private static InventoryMetaItem containerRoot(InventoryContainerWindow window)
            throws ReflectiveOperationException {
        Field field = InventoryContainerWindow.class.getDeclaredField("rootItem");
        if (!InventoryContainerWindow.InventoryContainerItem.class
                .isAssignableFrom(field.getType()))
            throw new NoSuchFieldException("InventoryContainerWindow.rootItem type changed");
        field.setAccessible(true);
        Object value = field.get(window);
        return value instanceof InventoryContainerWindow.InventoryContainerItem
                ? ((InventoryContainerWindow.InventoryContainerItem) value).getItem() : null;
    }

    /** Raw inventory row used by the dedicated bulk source/destination pickers. */
    public static Row rowAt(Object panel, int x, int y) {
        if (!(panel instanceof WurmTreeList.TreeListPanel)) return null;
        WTreeListNode<?> node = ((WurmTreeList.TreeListPanel) panel).getNodeAt(x, y);
        if (node == null || node.item == null) return null;
        Object row = node.item;
        List<InventoryMetaItem> ancestors = ancestorsOf(node);
        if (row instanceof InventoryListComponent.InventoryTreeListItem) {
            InventoryListComponent.InventoryTreeListItem inventoryRow =
                    (InventoryListComponent.InventoryTreeListItem) row;
            return inventoryRow.item == null ? null
                    : new Row(inventoryRow.item, inventoryRow.isInventoryGroup,
                    ancestors);
        }
        if (row instanceof InventoryContainerWindow.InventoryContainerItem) {
            InventoryMetaItem item =
                    ((InventoryContainerWindow.InventoryContainerItem) row).getItem();
            return item == null ? null : new Row(item, false, ancestors);
        }
        return null;
    }

    private static List<InventoryMetaItem> ancestorsOf(WTreeListNode<?> node) {
        List<InventoryMetaItem> result = new ArrayList<InventoryMetaItem>();
        WTreeListNode<?> parent = node.getParent();
        while (parent != null) {
            InventoryMetaItem item = inventoryItem(parent.item);
            if (item != null) result.add(item);
            parent = parent.getParent();
        }
        return result;
    }

    private static InventoryMetaItem inventoryItem(Object row) {
        if (row instanceof InventoryListComponent.InventoryTreeListItem)
            return ((InventoryListComponent.InventoryTreeListItem) row).item;
        if (row instanceof InventoryContainerWindow.InventoryContainerItem)
            return ((InventoryContainerWindow.InventoryContainerItem) row).getItem();
        return null;
    }

    public static final class Row {
        private final InventoryMetaItem item;
        private final boolean inventoryGroup;
        private final List<InventoryMetaItem> ancestors;

        private Row(InventoryMetaItem item, boolean inventoryGroup,
                    List<InventoryMetaItem> ancestors) {
            this.item = item;
            this.inventoryGroup = inventoryGroup;
            this.ancestors = Collections.unmodifiableList(
                    new ArrayList<InventoryMetaItem>(ancestors));
        }

        public InventoryMetaItem getItem() { return item; }
        public boolean isInventoryGroup() { return inventoryGroup; }
        public List<InventoryMetaItem> getAncestors() { return ancestors; }
    }

    /** Result plus bounded diagnostics for the direct player-inventory probe. */
    public static final class PlayerInventoryHit {
        private final InventoryMetaItem item;
        private final boolean ownsTopComponent;
        private final long rowId;
        private final long rootId;
        private final long playerWindowId;
        private final long resolvedWindowId;
        private final String nativeStructure;
        private final String topComponent;
        private final String topProbeFailure;
        private final String hudComponent;
        private final String windowComponent;
        private final String listComponent;
        private final boolean insideWindow;
        private final boolean insideList;
        private final String failure;

        private PlayerInventoryHit(InventoryMetaItem item, boolean ownsTopComponent,
                                   long rowId, long rootId, long playerWindowId,
                                   long resolvedWindowId, String nativeStructure,
                                   String topComponent,
                                   String topProbeFailure, String hudComponent,
                                   String windowComponent, String listComponent,
                                   boolean insideWindow, boolean insideList) {
            this(item, ownsTopComponent, rowId, rootId, playerWindowId,
                    resolvedWindowId, nativeStructure, topComponent,
                    topProbeFailure, hudComponent,
                    windowComponent, listComponent, insideWindow, insideList, null);
        }

        private PlayerInventoryHit(InventoryMetaItem item, boolean ownsTopComponent,
                                   long rowId, long rootId, long playerWindowId,
                                   long resolvedWindowId, String nativeStructure,
                                   String topComponent,
                                   String topProbeFailure, String hudComponent,
                                   String windowComponent, String listComponent,
                                   boolean insideWindow, boolean insideList,
                                   String failure) {
            this.item = item;
            this.ownsTopComponent = ownsTopComponent;
            this.rowId = rowId;
            this.rootId = rootId;
            this.playerWindowId = playerWindowId;
            this.resolvedWindowId = resolvedWindowId;
            this.nativeStructure = nativeStructure;
            this.topComponent = topComponent;
            this.topProbeFailure = topProbeFailure;
            this.hudComponent = hudComponent;
            this.windowComponent = windowComponent;
            this.listComponent = listComponent;
            this.insideWindow = insideWindow;
            this.insideList = insideList;
            this.failure = failure;
        }

        private static PlayerInventoryHit miss(String failure) {
            return new PlayerInventoryHit(null, false, 0L, 0L,
                    0L, 0L, "", "null", null, "null", "null", "null",
                    false, false, failure);
        }

        public InventoryMetaItem getItem() { return item; }
        public boolean isHit() { return ownsTopComponent && item != null; }

        public String diagnostic() {
            return "failure=" + failure
                    + ", ownsTopComponent=" + ownsTopComponent
                    + ", rowId=" + rowId + ", rootId=" + rootId
                    + ", playerWindowId=" + playerWindowId
                    + ", resolvedWindowId=" + resolvedWindowId
                    + ", nativeStructure={" + nativeStructure + "}"
                    + ", insideWindow=" + insideWindow
                    + ", insideList=" + insideList
                    + ", topComponent=" + topComponent
                    + ", topProbeFailure=" + topProbeFailure
                    + ", hudComponent=" + hudComponent
                    + ", windowComponent=" + windowComponent
                    + ", listComponent=" + listComponent;
        }
    }
}
