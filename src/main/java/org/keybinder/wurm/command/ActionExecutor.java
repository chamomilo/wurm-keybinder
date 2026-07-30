package org.keybinder.wurm.command;

import com.wurmonline.client.comm.ServerConnectionListenerClass;
import com.wurmonline.client.game.inventory.InventoryMetaItem;
import com.wurmonline.client.renderer.PickableUnit;
import com.wurmonline.client.renderer.cell.CellRenderable;
import com.wurmonline.client.renderer.cell.CreatureCellRenderable;
import com.wurmonline.client.renderer.cell.GroundItemCellRenderable;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.client.renderer.gui.PaperDollSlot;
import com.wurmonline.mesh.Tiles;
import com.wurmonline.shared.constants.PlayerAction;
import org.keybinder.wurm.integration.ClientAccess;
import org.keybinder.wurm.catalog.VanillaPlayerActionCatalog;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.TargetKind;
import org.keybinder.wurm.model.TargetSpec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

public final class ActionExecutor {
    private final ClientAccess access;
    private final ActionRangeResolver ranges;
    private final ActionNameResolver names;
    private final VanillaPlayerActionCatalog vanillaActions;

    public ActionExecutor(ClientAccess access) {
        this(access, ActionExecutor::defaultActionName);
    }

    public ActionExecutor(ClientAccess access, ActionNameResolver names) {
        this(access, new ActionRangeResolver(), names);
    }

    ActionExecutor(ClientAccess access, ActionRangeResolver ranges,
                   ActionNameResolver names) {
        this(access, ranges, names, new VanillaPlayerActionCatalog());
    }

    ActionExecutor(ClientAccess access, ActionRangeResolver ranges,
                   ActionNameResolver names, VanillaPlayerActionCatalog vanillaActions) {
        this.access = access;
        this.ranges = ranges;
        this.names = names;
        this.vanillaActions = vanillaActions;
    }

    public int runtimeQueueCost(ActionStep step, HeadsUpDisplay hud)
            throws ReflectiveOperationException {
        TargetSpec target = step.getTarget();
        if (target.getKind() == TargetKind.NEARBY_RADIUS)
            return nearbyTargets(step, hud).size();
        if (target.getKind() == TargetKind.NEARBY_TYPE)
            return nearbyTargetByType(step, hud) == null ? 0 : 1;
        ensureAvailable(step, hud);
        if (target.getKind() == TargetKind.AREA) return 9;
        return 1;
    }

    public void executeStep(ActionStep step, HeadsUpDisplay hud) throws ReflectiveOperationException {
        short id = step.getActionId();
        TargetSpec target = step.getTarget();
        // A numeric ID that exactly matches a built-in vanilla PlayerAction inherits
        // its real mask/flags. Unknown and mod-provided IDs retain the permissive
        // legacy Custom Actions behavior.
        PlayerAction action = vanillaActions.resolveOrGeneric(id);

        switch (target.getKind()) {
            case HOVER: hud.getWorld().sendHoveredAction(action); return;
            case BODY:
                InventoryMetaItem body = access.bodyItem(hud.getPaperDollInventory());
                if (body == null) throw unavailable("Character body target is unavailable");
                sendObjectAction(action, body.getId(), hud); return;
            case ACTIVE_TOOL:
                InventoryMetaItem tool = access.activeTool(hud);
                if (tool == null) throw unavailable("No active tool was found");
                sendObjectAction(action, tool.getId(), hud); return;
            case SELECTED:
                PickableUnit selected = access.selected(hud.getSelectBar());
                if (selected == null) throw unavailable("No selected target was found");
                sendObjectAction(action, selected.getId(), hud); return;
            case TILE:
                sendTile(action, hud, target.getDx(), target.getDy());
                return;
            case AREA:
                for (int dy = -1; dy <= 1; dy++)
                    for (int dx = -1; dx <= 1; dx++) sendTile(action, hud, dx, dy);
                return;
            case TOOLBELT_SLOT:
                InventoryMetaItem beltItem = hud.getToolBelt().getItemInSlot(target.getSlot() - 1);
                if (beltItem == null)
                    throw unavailable("Toolbelt slot " + target.getSlot() + " is empty");
                sendObjectAction(action, beltItem.getId(), hud);
                return;
            case EQUIPMENT_SLOT:
                byte slot = (byte) target.getSlot();
                PaperDollSlot frame = access.equipmentSlot(hud.getPaperDollInventory(), slot);
                if (frame == null) throw unavailable("Equipment slot " + target.getSlot() + " is unavailable");
                if (frame.getEquippedItem() == null)
                    throw unavailable("Equipment slot " + target.getSlot() + " is empty");
                sendObjectAction(action, frame.getEquippedItem().getId(), hud);
                return;
            case NEARBY_RADIUS:
                List<Long> nearby = nearbyTargets(step, hud);
                for (Long targetId : nearby) sendObjectAction(action, targetId, hud);
                return;
            case NEARBY_TYPE:
                CellRenderable found = nearbyTargetByType(step, hud);
                if (found == null) return;
                access.select(hud.getSelectBar(), found);
                sendObjectAction(action, found.getId(), hud);
                return;
            case EXACT_OBJECT:
                if (!exactObjectAvailable(target, hud))
                    throw unavailable("Exact object " + exactName(target) + " was not found");
                sendObjectAction(action, target.getObjectId(), hud);
                return;
            case CURRENT_RIDE:
                CreatureCellRenderable ride = currentRide(hud);
                if (ride == null)
                    throw unavailable("Character is not currently riding a creature or vehicle");
                sendObjectAction(action, ride.getId(), hud);
                return;
            case UNRESOLVED:
                throw unavailable("Target is unresolved");
            default:
                throw new IllegalArgumentException("Unsupported action target " + target.getKind());
        }
    }

    private void sendTile(PlayerAction action, HeadsUpDisplay hud, int dx, int dy) {
        int x = hud.getWorld().getPlayerCurrentTileX();
        int y = hud.getWorld().getPlayerCurrentTileY();
        hud.sendAction(action, Tiles.getTileId(x + dx, y + dy, hud.getWorld().getPlayerLayer()));
    }

    private void sendObjectAction(PlayerAction action, long targetId, HeadsUpDisplay hud) {
        // Vanilla Pull/Pull gently asks the client to retain SelectBar state before
        // the server removes and re-adds the moved object. Push/Push gently omit
        // that server notification, so compensate only for those two action IDs.
        if (action.getId() == 99 || action.getId() == 696)
            hud.getSelectBar().keepSelectedItem(targetId);
        hud.sendAction(action, targetId);
    }

    private List<Long> nearbyTargets(ActionStep step, HeadsUpDisplay hud)
            throws ReflectiveOperationException {
        TargetSpec target = step.getTarget();
        float actionRange = ranges.actionRange(step.getActionId());
        float scanRange = ranges.scanRange(step.getActionId(), target.getRadius());
        List<CellRenderable> scanned = nearbyCandidates(hud)
                .filter(candidate -> candidate.getSquaredLengthFromPlayer()
                        <= scanRange * scanRange)
                .sorted(java.util.Comparator.comparingDouble(
                        CellRenderable::getSquaredLengthFromPlayer))
                .collect(java.util.stream.Collectors.toList());
        if (scanned.isEmpty()) return java.util.Collections.emptyList();

        List<Long> ids = new ArrayList<Long>();
        double actionRangeSquared = actionRange * actionRange;
        for (CellRenderable candidate : scanned)
            if (candidate.getSquaredLengthFromPlayer() <= actionRangeSquared)
                ids.add(candidate.getId());
        if (ids.isEmpty())
            throw tooFar(step, scanned.get(0), actionRange);
        return ids;
    }

    private CellRenderable nearbyTargetByType(ActionStep step, HeadsUpDisplay hud)
            throws ReflectiveOperationException {
        TargetSpec target = step.getTarget();
        float actionRange = ranges.actionRange(step.getActionId());
        float scanRange = ranges.scanRange(step.getActionId(), 0.0f);
        CellRenderable nearest = nearbyCandidates(hud)
                .filter(x -> x.getSquaredLengthFromPlayer() <= scanRange * scanRange)
                .filter(x -> matchesNearbyType(target, x))
                .min(java.util.Comparator.comparingDouble(CellRenderable::getSquaredLengthFromPlayer))
                .orElse(null);
        if (nearest == null) return null;
        if (!ranges.isWithinActionRange(
                step.getActionId(), nearest.getSquaredLengthFromPlayer()))
            throw tooFar(step, nearest, actionRange);
        return nearest;
    }

    private Stream<CellRenderable> nearbyCandidates(HeadsUpDisplay hud)
            throws ReflectiveOperationException {
        ServerConnectionListenerClass listener =
                hud.getWorld().getServerConnection().getServerConnectionListener();
        Collection<GroundItemCellRenderable> ground = access.groundItems(listener).values();
        Collection<CreatureCellRenderable> creatures = listener.getCreatures().values();
        return Stream.concat(
                ground.stream().map(candidate -> (CellRenderable) candidate),
                creatures.stream().map(candidate -> (CellRenderable) candidate));
    }

    private boolean matchesNearbyType(TargetSpec target, CellRenderable candidate) {
        try {
            return target.getText().equals(NearbyTypeTarget.normalizeType(access.objectType(candidate)));
        } catch (ReflectiveOperationException e) {
            return target.getText().equals(NearbyTypeTarget.normalizeType(candidate.getHoverName()));
        }
    }

    private void ensureAvailable(ActionStep step, HeadsUpDisplay hud)
            throws ReflectiveOperationException {
        TargetSpec target = step.getTarget();
        switch (target.getKind()) {
            case BODY:
                if (access.bodyItem(hud.getPaperDollInventory()) == null)
                    throw unavailable("Character body target is unavailable");
                return;
            case ACTIVE_TOOL:
                if (access.activeTool(hud) == null) throw unavailable("No active tool was found");
                return;
            case SELECTED:
                if (access.selected(hud.getSelectBar()) == null)
                    throw unavailable("No selected target was found");
                return;
            case TOOLBELT_SLOT:
                if (hud.getToolBelt().getItemInSlot(target.getSlot() - 1) == null)
                    throw unavailable("Toolbelt slot " + target.getSlot() + " is empty");
                return;
            case EQUIPMENT_SLOT:
                PaperDollSlot frame = access.equipmentSlot(
                        hud.getPaperDollInventory(), (byte) target.getSlot());
                if (frame == null || frame.getEquippedItem() == null)
                    throw unavailable("Equipment slot " + target.getSlot() + " is empty");
                return;
            case NEARBY_RADIUS:
                nearbyTargets(step, hud);
                return;
            case NEARBY_TYPE:
                nearbyTargetByType(step, hud);
                return;
            case EXACT_OBJECT:
                if (!exactObjectAvailable(target, hud))
                    throw unavailable("Exact object " + exactName(target) + " was not found");
                return;
            case CURRENT_RIDE:
                if (currentRide(hud) == null)
                    throw unavailable("Character is not currently riding a creature or vehicle");
                return;
            case UNRESOLVED:
                throw unavailable("Target is unresolved");
            default:
        }
    }

    private boolean exactObjectAvailable(TargetSpec target, HeadsUpDisplay hud)
            throws ReflectiveOperationException {
        long id = target.getObjectId();
        if (access.inventoryItem(hud, id) != null) return true;
        PickableUnit selected = access.selected(hud.getSelectBar());
        if (selected != null && selected.getId() == id) return true;
        PickableUnit hovered = hud.getWorld().getCurrentHoveredObject();
        if (hovered != null && hovered.getId() == id) return true;
        ServerConnectionListenerClass listener =
                hud.getWorld().getServerConnection().getServerConnectionListener();
        if (access.groundItems(listener).containsKey(id)) return true;
        return listener.getCreatures().containsKey(id);
    }

    private static CreatureCellRenderable currentRide(HeadsUpDisplay hud) {
        return hud.getWorld().getPlayer().getCarrierCreature();
    }

    private static String exactName(TargetSpec target) {
        return target.getText().isEmpty()
                ? Long.toString(target.getObjectId()) : "\"" + target.getText() + "\"";
    }

    private static StepUnavailableException unavailable(String message) {
        return new StepUnavailableException(message);
    }

    private StepUnavailableException tooFar(
            ActionStep step, CellRenderable nearest, float actionRange) {
        double distance = Math.sqrt(nearest.getSquaredLengthFromPlayer());
        return unavailable("Command " + actionName(step.getActionId())
                + " skipped as player is too far from nearest "
                + nearbyDisplayName(nearest) + " (" + formatDistance((float) distance)
                + ", action range " + formatDistance(actionRange)
                + "). Come closer.");
    }

    String actionName(short actionId) {
        String resolved = names.nameOf(actionId);
        if (resolved == null || resolved.trim().isEmpty())
            return defaultActionName(actionId);
        return resolved.trim().replaceFirst("\\s+\\(-?\\d+\\)$", "");
    }

    private static String defaultActionName(short actionId) {
        PlayerAction registered = PlayerAction.getByActionId(actionId);
        if (registered == null || registered.getName() == null
                || registered.getName().trim().isEmpty())
            return "Action " + actionId;
        return registered.getName().trim().replaceFirst("\\s+\\(-?\\d+\\)$", "");
    }

    private static String nearbyDisplayName(CellRenderable target) {
        String name = target.getHoverName();
        try {
            return NearbyTypeTarget.normalizeType(name);
        } catch (RuntimeException ignored) {
            return name == null || name.trim().isEmpty() ? "target" : name.trim();
        }
    }

    private static String formatDistance(float distance) {
        return String.format(java.util.Locale.ENGLISH, "%.1f", distance);
    }
}
