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
import org.keybinder.wurm.integration.ActionSourceOverride;
import org.keybinder.wurm.integration.ExecutionHoverOverride;
import org.keybinder.wurm.i18n.Messages;
import org.keybinder.wurm.catalog.VanillaPlayerActionCatalog;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.ObjectTypeNormalizer;
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
    private final PushSelectionRetention pushSelection;
    private final ActionSourceResolver sources;
    private final InventoryFilterResolver inventoryFilters =
            new InventoryFilterResolver();

    public ActionExecutor(ClientAccess access) {
        this(access, ActionExecutor::defaultActionName, new PushSelectionRetention());
    }

    public ActionExecutor(ClientAccess access, ActionNameResolver names) {
        this(access, names, new PushSelectionRetention());
    }

    public ActionExecutor(ClientAccess access, ActionNameResolver names,
                          PushSelectionRetention pushSelection) {
        this(access, new ActionRangeResolver(), names, new VanillaPlayerActionCatalog(),
                pushSelection);
    }

    ActionExecutor(ClientAccess access, ActionRangeResolver ranges,
                   ActionNameResolver names) {
        this(access, ranges, names, new VanillaPlayerActionCatalog(),
                new PushSelectionRetention());
    }

    ActionExecutor(ClientAccess access, ActionRangeResolver ranges,
                   ActionNameResolver names, VanillaPlayerActionCatalog vanillaActions) {
        this(access, ranges, names, vanillaActions, new PushSelectionRetention());
    }

    ActionExecutor(ClientAccess access, ActionRangeResolver ranges,
                   ActionNameResolver names, VanillaPlayerActionCatalog vanillaActions,
                   PushSelectionRetention pushSelection) {
        this.access = access;
        this.ranges = ranges;
        this.names = names;
        this.vanillaActions = vanillaActions;
        this.pushSelection = pushSelection;
        this.sources = new ActionSourceResolver(access);
    }

    public int runtimeQueueCost(ActionStep step, HeadsUpDisplay hud)
            throws ReflectiveOperationException {
        sources.resolve(step.getSource(), hud);
        TargetSpec target = step.getTarget();
        if (target.getKind() == TargetKind.NEARBY
                || target.getKind() == TargetKind.NEARBY_RADIUS)
            return nearbyTargets(step, hud).size();
        if (target.getKind() == TargetKind.NEARBY_TYPE)
            return nearbyTargetByType(step, hud) == null ? 0 : 1;
        if (target.getKind() == TargetKind.HOVER_TYPE)
            return hoverTypeTargets(step, hud).ids.size();
        ensureAvailable(step, hud);
        if (target.getKind() == TargetKind.AREA) return 9;
        return 1;
    }

    public void executeStep(ActionStep step, HeadsUpDisplay hud) throws ReflectiveOperationException {
        executeStep(step, hud, Integer.MAX_VALUE);
    }

    public void executeStep(ActionStep step, HeadsUpDisplay hud, int maxFanOutTargets)
            throws ReflectiveOperationException {
        ActionSourceResolver.ResolvedSource source = sources.resolve(step.getSource(), hud);
        if (!source.hasOverride()) {
            executeTarget(step, hud, maxFanOutTargets);
            return;
        }
        try (ActionSourceOverride.Scope ignored = ActionSourceOverride.push(source.getSourceId())) {
            executeTarget(step, hud, maxFanOutTargets);
        }
    }

    private void executeTarget(ActionStep step, HeadsUpDisplay hud, int maxFanOutTargets)
            throws ReflectiveOperationException {
        short id = step.getActionId();
        TargetSpec target = step.getTarget();
        // A numeric ID that exactly matches a built-in vanilla PlayerAction inherits
        // its real mask/flags. Unknown and mod-provided IDs retain the permissive
        // legacy Custom Actions behavior.
        PlayerAction action = vanillaActions.resolveOrGeneric(id);

        switch (target.getKind()) {
            case HOVER:
                ExecutionHoverOverride.Snapshot hoverOverride =
                        ExecutionHoverOverride.current();
                if (hoverOverride != null && hoverOverride.getWorldObjectId() > 0L)
                    sendObjectAction(action, hoverOverride.getWorldObjectId(), hud);
                else hud.getWorld().sendHoveredAction(action);
                return;
            case BODY:
                InventoryMetaItem body = access.bodyItem(hud.getPaperDollInventory());
                if (body == null) throw unavailable(Messages.text("unavailable.body"));
                sendObjectAction(action, body.getId(), hud); return;
            case ACTIVE_TOOL:
                InventoryMetaItem tool = access.activeTool(hud);
                if (tool == null) throw unavailable(Messages.text("unavailable.active_tool"));
                sendObjectAction(action, tool.getId(), hud); return;
            case SELECTED:
                PickableUnit selected = access.selected(hud.getSelectBar());
                if (selected == null) throw unavailable(Messages.text("unavailable.selected"));
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
                    throw unavailable(Messages.text("unavailable.toolbelt_empty", target.getSlot()));
                sendObjectAction(action, beltItem.getId(), hud);
                return;
            case EQUIPMENT_SLOT:
                byte slot = (byte) target.getSlot();
                PaperDollSlot frame = access.equipmentSlot(hud.getPaperDollInventory(), slot);
                if (frame == null)
                    throw unavailable(Messages.text(
                            "unavailable.equipment_unavailable", target.getSlot()));
                if (frame.getEquippedItem() == null)
                    throw unavailable(Messages.text("unavailable.equipment_empty", target.getSlot()));
                sendObjectAction(action, frame.getEquippedItem().getId(), hud);
                return;
            case NEARBY_RADIUS:
                List<Long> nearby = nearbyTargets(step, hud);
                for (Long targetId : nearby) sendObjectAction(action, targetId, hud);
                return;
            case NEARBY:
                List<Long> automaticNearby = nearbyTargets(step, hud);
                int nearbyCount = Math.min(automaticNearby.size(),
                        Math.max(0, maxFanOutTargets));
                for (int i = 0; i < nearbyCount; i++)
                    sendObjectAction(action, automaticNearby.get(i), hud);
                return;
            case NEARBY_TYPE:
                CellRenderable found = nearbyTargetByType(step, hud);
                if (found == null) return;
                access.select(hud.getSelectBar(), found);
                sendObjectAction(action, found.getId(), hud);
                return;
            case HOVER_TYPE:
                HoverTypeResolution hoverMatches = hoverTypeTargets(step, hud);
                int targetCount = Math.min(hoverMatches.ids.size(),
                        Math.max(0, maxFanOutTargets));
                if (targetCount == 0) return;
                long[] hoverIds = new long[targetCount];
                for (int i = 0; i < hoverIds.length; i++) hoverIds[i] = hoverMatches.ids.get(i);
                hud.sendAction(action, hoverIds);
                return;
            case INVENTORY_FILTER:
                InventoryMetaItem filtered = inventoryFilterItem(target, hud);
                if (filtered == null)
                    throw unavailable(Messages.text(
                            "unavailable.inventory_filter", target.getText()));
                sendObjectAction(action, filtered.getId(), hud);
                return;
            case EXACT_OBJECT:
                if (!exactObjectAvailable(target, hud))
                    throw unavailable(Messages.text("unavailable.exact_object", exactName(target)));
                sendObjectAction(action, target.getObjectId(), hud);
                return;
            case CURRENT_RIDE:
                CreatureCellRenderable ride = currentRide(hud);
                if (ride == null)
                    throw unavailable(Messages.text("unavailable.current_ride"));
                sendObjectAction(action, ride.getId(), hud);
                return;
            case UNRESOLVED:
                throw unavailable(Messages.text("unavailable.unresolved"));
            default:
                throw new IllegalArgumentException(
                        Messages.text("unavailable.unsupported_action_target", target.getKind()));
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
        if (keepsSelectedTarget(action.getId())) {
            pushSelection.arm(targetId);
            hud.getSelectBar().keepSelectedItem(targetId);
        }
        hud.sendAction(action, targetId);
    }

    static boolean keepsSelectedTarget(short actionId) {
        return actionId == PlayerAction.PUSH.getId()
                || actionId == PlayerAction.PUSH_GENTLY.getId();
    }

    private List<Long> nearbyTargets(ActionStep step, HeadsUpDisplay hud)
            throws ReflectiveOperationException {
        TargetSpec target = step.getTarget();
        float actionRange = ranges.actionRange(step.getActionId());
        float scanRange = ranges.scanRange(step.getActionId(), target.getRadius());
        List<CellRenderable> scanned = nearbyCandidates(step, hud)
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
        CellRenderable nearest = nearbyCandidates(step, hud)
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

    private Stream<CellRenderable> nearbyCandidates(ActionStep step, HeadsUpDisplay hud)
            throws ReflectiveOperationException {
        ServerConnectionListenerClass listener =
                hud.getWorld().getServerConnection().getServerConnectionListener();
        Collection<GroundItemCellRenderable> ground = access.groundItems(listener).values();
        Collection<CreatureCellRenderable> creatures = listener.getCreatures().values();
        PlayerAction action = vanillaActions.resolveOrGeneric(step.getActionId());
        return Stream.concat(
                ground.stream().map(candidate -> (CellRenderable) candidate),
                creatures.stream().map(candidate -> (CellRenderable) candidate))
                .filter(candidate -> candidate.targetMatches(action.getTargetMask()));
    }

    private boolean matchesNearbyType(TargetSpec target, CellRenderable candidate) {
        try {
            return ObjectTypeNormalizer.matchesType(
                    target.getText(), access.objectType(candidate));
        } catch (ReflectiveOperationException | RuntimeException e) {
            return ObjectTypeNormalizer.matchesType(
                    target.getText(), candidate.getHoverName());
        }
    }

    private HoverTypeResolution hoverTypeTargets(ActionStep step, HeadsUpDisplay hud)
            throws ReflectiveOperationException {
        PlayerAction action = vanillaActions.resolveOrGeneric(step.getActionId());
        List<Long> candidates = new ArrayList<Long>();
        if ((action.getTargetMask() & 256) != 0) {
            com.wurmonline.client.WurmClientBase client = hud.getWorld().getClient();
            long[] hudTargets = hud.getCommandTargetsFrom(client.getXMouse(), client.getYMouse());
            if (hudTargets != null)
                for (long id : hudTargets) if (!candidates.contains(id)) candidates.add(id);
        }
        if (candidates.isEmpty()) {
            PickableUnit hovered = hud.getWorld().getCurrentHoveredObject();
            if (hovered != null && hovered.targetMatches(action.getTargetMask()))
                candidates.add(hovered.getId());
        }
        List<Long> matches = new ArrayList<Long>();
        List<String> found = new ArrayList<String>();
        for (Long id : candidates) {
            String type = access.objectType(hud, id.longValue());
            String normalized = "";
            try {
                if (type != null) normalized = ObjectTypeNormalizer.normalizeType(type);
            } catch (RuntimeException unresolved) {
                // Preserve the candidate as unresolved for the user-facing mismatch.
            }
            if (!normalized.isEmpty() && !found.contains(normalized)) found.add(normalized);
            if (ObjectTypeNormalizer.matchesType(
                    step.getTarget().getText(), normalized)) matches.add(id);
        }
        if (matches.isEmpty() && shouldReportHoverTypeMismatch(
                candidates.size(), found.size())) {
            String foundText = join(found);
            throw unavailable(Messages.text("event.hover_type_mismatch",
                    actionName(step.getActionId()), step.getTarget().getText(), foundText));
        }
        return new HoverTypeResolution(matches);
    }

    static boolean shouldReportHoverTypeMismatch(int candidateCount, int resolvedTypeCount) {
        return candidateCount > 0 && resolvedTypeCount > 0;
    }

    private static String join(List<String> values) {
        StringBuilder result = new StringBuilder();
        for (String value : values) {
            if (result.length() > 0) result.append(", ");
            result.append(value);
        }
        return result.toString();
    }

    private static final class HoverTypeResolution {
        private final List<Long> ids;
        private HoverTypeResolution(List<Long> ids) { this.ids = ids; }
    }

    private void ensureAvailable(ActionStep step, HeadsUpDisplay hud)
            throws ReflectiveOperationException {
        TargetSpec target = step.getTarget();
        switch (target.getKind()) {
            case BODY:
                if (access.bodyItem(hud.getPaperDollInventory()) == null)
                    throw unavailable(Messages.text("unavailable.body"));
                return;
            case ACTIVE_TOOL:
                if (access.activeTool(hud) == null)
                    throw unavailable(Messages.text("unavailable.active_tool"));
                return;
            case SELECTED:
                if (access.selected(hud.getSelectBar()) == null)
                    throw unavailable(Messages.text("unavailable.selected"));
                return;
            case TOOLBELT_SLOT:
                if (hud.getToolBelt().getItemInSlot(target.getSlot() - 1) == null)
                    throw unavailable(Messages.text("unavailable.toolbelt_empty", target.getSlot()));
                return;
            case EQUIPMENT_SLOT:
                PaperDollSlot frame = access.equipmentSlot(
                        hud.getPaperDollInventory(), (byte) target.getSlot());
                if (frame == null || frame.getEquippedItem() == null)
                    throw unavailable(Messages.text("unavailable.equipment_empty", target.getSlot()));
                return;
            case NEARBY_RADIUS:
            case NEARBY:
                nearbyTargets(step, hud);
                return;
            case NEARBY_TYPE:
                nearbyTargetByType(step, hud);
                return;
            case HOVER_TYPE:
                hoverTypeTargets(step, hud);
                return;
            case INVENTORY_FILTER:
                if (inventoryFilterItem(target, hud) == null)
                    throw unavailable(Messages.text(
                            "unavailable.inventory_filter", target.getText()));
                return;
            case EXACT_OBJECT:
                if (!exactObjectAvailable(target, hud))
                    throw unavailable(Messages.text("unavailable.exact_object", exactName(target)));
                return;
            case CURRENT_RIDE:
                if (currentRide(hud) == null)
                    throw unavailable(Messages.text("unavailable.current_ride"));
                return;
            case UNRESOLVED:
                throw unavailable(Messages.text("unavailable.unresolved"));
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

    private InventoryMetaItem inventoryFilterItem(TargetSpec target,
                                                   HeadsUpDisplay hud) {
        List<InventoryMetaItem> toolbelt = new ArrayList<InventoryMetaItem>(10);
        if (hud.getToolBelt() != null)
            for (int slot = 0; slot < 10; slot++)
                toolbelt.add(hud.getToolBelt().getItemInSlot(slot));
        return inventoryFilters.resolve(target.getText(), toolbelt,
                access.playerInventoryRoot(hud));
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
        return unavailable(Messages.text("event.nearby_too_far",
                actionName(step.getActionId()), nearbyDisplayName(nearest),
                formatDistance((float) distance), formatDistance(actionRange)));
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
            return Messages.text("event.action_number", actionId);
        return registered.getName().trim().replaceFirst("\\s+\\(-?\\d+\\)$", "");
    }

    private static String nearbyDisplayName(CellRenderable target) {
        String name = target.getHoverName();
        try {
            return NearbyTypeTarget.normalizeType(name);
        } catch (RuntimeException ignored) {
            return name == null || name.trim().isEmpty()
                    ? Messages.text("event.generic_target") : name.trim();
        }
    }

    private static String formatDistance(float distance) {
        return String.format(java.util.Locale.ENGLISH, "%.1f", distance);
    }
}
