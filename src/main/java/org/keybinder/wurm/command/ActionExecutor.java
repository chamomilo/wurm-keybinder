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
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
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
    private final ThreadLocal<Map<ActionStep, ResolvedActionPlan>> prepared =
            new ThreadLocal<Map<ActionStep, ResolvedActionPlan>>() {
                @Override protected Map<ActionStep, ResolvedActionPlan> initialValue() {
                    return new IdentityHashMap<ActionStep, ResolvedActionPlan>();
                }
            };

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
        ResolvedActionPlan plan = prepare(step, hud);
        prepared.get().put(step, plan);
        return plan.getQueueCost();
    }

    public void executeStep(ActionStep step, HeadsUpDisplay hud) throws ReflectiveOperationException {
        executeStep(step, hud, Integer.MAX_VALUE);
    }

    public void executeStep(ActionStep step, HeadsUpDisplay hud, int maxFanOutTargets)
            throws ReflectiveOperationException {
        ResolvedActionPlan plan = prepared.get().remove(step);
        if (plan == null) plan = prepare(step, hud);
        execute(plan, hud, maxFanOutTargets);
    }

    void clearPrepared() { prepared.remove(); }

    private ResolvedActionPlan prepare(ActionStep step, HeadsUpDisplay hud)
            throws ReflectiveOperationException {
        ActionSourceResolver.ResolvedSource source = sources.resolve(step.getSource(), hud);
        short id = step.getActionId();
        TargetSpec target = step.getTarget();
        // A numeric ID that exactly matches a built-in vanilla PlayerAction inherits
        // its real mask/flags. Unknown and mod-provided IDs retain the permissive
        // legacy Custom Actions behavior.
        PlayerAction action = vanillaActions.resolveOrGeneric(id);

        switch (target.getKind()) {
            case HOVER: {
                ExecutionHoverOverride.Snapshot hoverOverride =
                        ExecutionHoverOverride.current();
                if (hoverOverride != null && hoverOverride.getWorldObjectId() > 0L)
                    return individual(source, action,
                            new long[]{hoverOverride.getWorldObjectId()}, 1, true, false, null);
                return batch(source, action, hoveredTargets(action, hud), 1, false);
            }
            case BODY: {
                InventoryMetaItem body = access.bodyItem(hud.getPaperDollInventory());
                if (body == null) throw unavailable(Messages.text("unavailable.body"));
                return object(source, action, body.getId());
            }
            case ACTIVE_TOOL: {
                InventoryMetaItem tool = access.activeTool(hud);
                if (tool == null) throw unavailable(Messages.text("unavailable.active_tool"));
                return object(source, action, tool.getId());
            }
            case SELECTED: {
                PickableUnit selected = access.selected(hud.getSelectBar());
                if (selected == null) throw unavailable(Messages.text("unavailable.selected"));
                return object(source, action, selected.getId());
            }
            case TILE:
                return individual(source, action,
                        new long[]{tileId(hud, target.getDx(), target.getDy())},
                        1, false, false, null);
            case AREA: {
                long[] tiles = new long[9];
                int index = 0;
                for (int dy = -1; dy <= 1; dy++)
                    for (int dx = -1; dx <= 1; dx++)
                        tiles[index++] = tileId(hud, dx, dy);
                return individual(source, action, tiles, tiles.length,
                        false, false, null);
            }
            case TOOLBELT_SLOT: {
                InventoryMetaItem beltItem = hud.getToolBelt().getItemInSlot(target.getSlot() - 1);
                if (beltItem == null)
                    throw unavailable(Messages.text("unavailable.toolbelt_empty", target.getSlot()));
                return object(source, action, beltItem.getId());
            }
            case EQUIPMENT_SLOT: {
                byte slot = (byte) target.getSlot();
                PaperDollSlot frame = access.equipmentSlot(hud.getPaperDollInventory(), slot);
                if (frame == null)
                    throw unavailable(Messages.text(
                            "unavailable.equipment_unavailable", target.getSlot()));
                if (frame.getEquippedItem() == null)
                    throw unavailable(Messages.text("unavailable.equipment_empty", target.getSlot()));
                return object(source, action, frame.getEquippedItem().getId());
            }
            case NEARBY_RADIUS: {
                long[] nearby = ids(nearbyTargets(step, hud));
                return individual(source, action, nearby, nearby.length,
                        true, false, null);
            }
            case NEARBY: {
                long[] nearby = ids(nearbyTargets(step, hud));
                return individual(source, action, nearby, nearby.length,
                        true, true, null);
            }
            case NEARBY_TYPE: {
                CellRenderable found = nearbyTargetByType(step, hud);
                if (found == null)
                    return individual(source, action, new long[0], 0,
                            true, false, null);
                return individual(source, action, new long[]{found.getId()}, 1,
                        true, false, found);
            }
            case HOVER_TYPE: {
                HoverTypeResolution hoverMatches = hoverTypeTargets(step, hud);
                long[] hoverIds = ids(hoverMatches.ids);
                return batch(source, action, hoverIds, hoverIds.length, true);
            }
            case INVENTORY_FILTER: {
                InventoryMetaItem filtered = inventoryFilterItem(target, hud);
                if (filtered == null)
                    throw unavailable(Messages.text(
                            "unavailable.inventory_filter", target.getText()));
                return object(source, action, filtered.getId());
            }
            case EXACT_OBJECT:
                if (!exactObjectAvailable(target, hud))
                    throw unavailable(Messages.text("unavailable.exact_object", exactName(target)));
                return object(source, action, target.getObjectId());
            case CURRENT_RIDE: {
                CreatureCellRenderable ride = currentRide(hud);
                if (ride == null)
                    throw unavailable(Messages.text("unavailable.current_ride"));
                return object(source, action, ride.getId());
            }
            case UNRESOLVED:
                throw unavailable(Messages.text("unavailable.unresolved"));
            default:
                throw new IllegalArgumentException(
                        Messages.text("unavailable.unsupported_action_target", target.getKind()));
        }
    }

    private void execute(ResolvedActionPlan plan, HeadsUpDisplay hud,
                         int plannedQueueCost) throws ReflectiveOperationException {
        ActionSourceResolver.ResolvedSource source = plan.getSource();
        if (!source.hasOverride()) {
            dispatch(plan, hud, plannedQueueCost);
            return;
        }
        try (ActionSourceOverride.Scope ignored = ActionSourceOverride.push(source.getSourceId())) {
            dispatch(plan, hud, plannedQueueCost);
        }
    }

    private void dispatch(ResolvedActionPlan plan, HeadsUpDisplay hud,
                          int plannedQueueCost) throws ReflectiveOperationException {
        long[] targets = plan.targetsForExecution(plannedQueueCost);
        if (targets.length == 0) return;
        if (plan.getSelectBeforeDispatch() != null)
            access.select(hud.getSelectBar(), plan.getSelectBeforeDispatch());
        if (plan.isBatchDispatch()) {
            hud.sendAction(plan.getAction(), targets);
            return;
        }
        for (long targetId : targets) {
            if (plan.hasObjectTargets()) sendObjectAction(plan.getAction(), targetId, hud);
            else hud.sendAction(plan.getAction(), targetId);
        }
    }

    private static ResolvedActionPlan object(ActionSourceResolver.ResolvedSource source,
                                             PlayerAction action, long targetId) {
        return individual(source, action, new long[]{targetId}, 1,
                true, false, null);
    }

    private static ResolvedActionPlan individual(
            ActionSourceResolver.ResolvedSource source, PlayerAction action,
            long[] targets, int queueCost, boolean objectTargets,
            boolean budgetedFanOut, PickableUnit selectBeforeDispatch) {
        return new ResolvedActionPlan(source, action, targets, queueCost,
                false, objectTargets, budgetedFanOut, selectBeforeDispatch);
    }

    private static ResolvedActionPlan batch(
            ActionSourceResolver.ResolvedSource source, PlayerAction action,
            long[] targets, int queueCost, boolean budgetedFanOut) {
        return new ResolvedActionPlan(source, action, targets, queueCost,
                true, false, budgetedFanOut, null);
    }

    private static long[] ids(List<Long> values) {
        long[] result = new long[values.size()];
        for (int i = 0; i < values.size(); i++) result[i] = values.get(i);
        return result;
    }

    private static long tileId(HeadsUpDisplay hud, int dx, int dy) {
        int x = hud.getWorld().getPlayerCurrentTileX();
        int y = hud.getWorld().getPlayerCurrentTileY();
        return Tiles.getTileId(x + dx, y + dy, hud.getWorld().getPlayerLayer());
    }

    private static long[] hoveredTargets(PlayerAction action, HeadsUpDisplay hud) {
        if (hud == null) return new long[0];
        if (hud.getWorld().getClient().isMouseUnavailable()) return new long[0];
        long[] targets = null;
        if ((action.getTargetMask() & 256) != 0) {
            com.wurmonline.client.WurmClientBase client = hud.getWorld().getClient();
            targets = hud.getCommandTargetsFrom(client.getXMouse(), client.getYMouse());
        }
        if (targets != null) return targets;
        PickableUnit hovered = hud.getWorld().getCurrentHoveredObject();
        if (hovered == null || !hovered.targetMatches(action.getTargetMask()))
            return new long[0];
        return new long[]{hovered.getId()};
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
