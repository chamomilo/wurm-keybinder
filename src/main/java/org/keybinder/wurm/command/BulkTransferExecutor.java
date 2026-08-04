package org.keybinder.wurm.command;

import com.wurmonline.client.game.inventory.InventoryMetaItem;
import com.wurmonline.client.renderer.PickableUnit;
import com.wurmonline.client.renderer.cell.GroundItemCellRenderable;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.client.renderer.gui.KeybinderInventorySelectionBridge;
import com.wurmonline.shared.util.ItemTypeUtilites;
import org.keybinder.wurm.event.EventLogger;
import org.keybinder.wurm.i18n.Messages;
import org.keybinder.wurm.integration.ClientAccess;
import org.keybinder.wurm.integration.ExecutionHoverOverride;
import org.keybinder.wurm.integration.BulkInventoryDestinationPolicy;
import org.keybinder.wurm.model.BulkDestinationKind;
import org.keybinder.wurm.model.BulkQuantityLimit;
import org.keybinder.wurm.model.BulkTransferStep;
import org.keybinder.wurm.model.InventoryReference;

import java.util.IdentityHashMap;
import java.util.Map;

/** Runtime destination resolution and exact bulk move dispatch. */
final class BulkTransferExecutor {
    private final ClientAccess access;
    private final EventLogger log;
    private final BulkTransferCoordinator coordinator;
    private final Map<BulkTransferStep, Long> prepared =
            new IdentityHashMap<BulkTransferStep, Long>();

    BulkTransferExecutor(ClientAccess access, EventLogger log,
                         BulkTransferCoordinator coordinator) {
        this.access = access;
        this.log = log;
        this.coordinator = coordinator;
    }

    int prepare(BulkTransferStep step, HeadsUpDisplay hud) {
        validate(step);
        coordinator.requireIdle();
        long destinationId = resolveDestination(step, hud);
        prepared.put(step, destinationId);
        String sourceVisibility = "unknown";
        int liveMaximum = 0;
        try {
            InventoryMetaItem visibleSource = access.inventoryItem(
                    hud, step.getSource().getItem().getId());
            sourceVisibility = Boolean.toString(visibleSource != null);
            if (visibleSource != null) {
                liveMaximum = BulkQuantityLimit.fromName(
                        preferredName(visibleSource));
            }
        } catch (ReflectiveOperationException | RuntimeException failure) {
            log.diagnostic("bulk-transfer source visibility check failed open: "
                    + failure.getClass().getSimpleName() + ": "
                    + String.valueOf(failure.getMessage()));
        }
        if (liveMaximum > 0 && step.getQuantity() > liveMaximum)
            throw new StepUnavailableException(Messages.text(
                    "unavailable.bulk_quantity_changed", step.getQuantity(), liveMaximum,
                    step.getSource().getItem().getName()));
        log.diagnostic("bulk-transfer preflight: storageId="
                + step.getSource().getStorage().getId()
                + ", itemId=" + step.getSource().getItem().getId()
                + ", sourceVisible=" + sourceVisibility
                + ", capturedMaximum="
                + BulkQuantityLimit.capturedMaximum(step.getSource())
                + ", liveMaximum=" + liveMaximum
                + ", destinationKind=" + step.getDestinationKind()
                + ", capturedDestinationId="
                + (step.getCapturedDestination() == null ? 0L
                : step.getCapturedDestination().getId())
                + ", destinationId=" + destinationId + ", quantity="
                + step.getQuantity());
        return 0;
    }

    void execute(final BulkTransferStep step, final HeadsUpDisplay hud) {
        execute(step, hud, null);
    }

    void execute(final BulkTransferStep step, final HeadsUpDisplay hud,
                 BulkTransferCoordinator.Completion completion) {
        Long destination = prepared.remove(step);
        if (destination == null)
            throw new StepUnavailableException(Messages.text("unavailable.bulk_not_prepared"));
        final long destinationId = destination;
        coordinator.begin(step, destinationId, (target, source) ->
                hud.getWorld().getServerConnection().sendMoveSomeItems(
                        target, new long[]{source}), completion);
    }

    void clearPrepared() { prepared.clear(); }

    private static void validate(BulkTransferStep step) {
        if (step.getSource() == null || step.getSource().getStorage() == null
                || step.getSource().getItem() == null
                || step.getSource().getStorage().getId() <= 0L
                || step.getSource().getItem().getId() <= 0L)
            throw new StepUnavailableException(Messages.text("validation.bulk_source_missing"));
        int maximum = BulkQuantityLimit.capturedMaximum(step.getSource());
        if (!BulkQuantityLimit.accepts(step.getSource(), step.getQuantity()))
            throw new StepUnavailableException(Messages.text(
                    "validation.bulk_quantity_range", maximum,
                    step.getSource().getItem().getName()));
        if (step.getDestinationKind() == null)
            throw new StepUnavailableException(Messages.text("validation.bulk_destination_missing"));
    }

    private static String preferredName(InventoryMetaItem item) {
        String display = item == null ? null : item.getDisplayName();
        if (display != null && !display.trim().isEmpty()) return display.trim();
        String base = item == null ? null : item.getBaseName();
        return base == null ? "" : base.trim();
    }

    private long resolveDestination(BulkTransferStep step, HeadsUpDisplay hud) {
        BulkDestinationKind kind = step.getDestinationKind();
        if (kind == BulkDestinationKind.PLAYER_INVENTORY) {
            InventoryMetaItem root = access.playerInventoryRoot(hud);
            if (root == null || !BulkInventoryDestinationPolicy.isValid(root.getId()))
                throw new StepUnavailableException(Messages.text(
                        "unavailable.bulk_player_inventory"));
            return root.getId();
        }
        if (kind == BulkDestinationKind.HOVERED_INVENTORY) {
            int mouseX = hud.getWorld().getClient().getXMouse();
            int mouseY = hud.getWorld().getClient().getYMouse();
            InventoryMetaItem inventoryRow = null;
            boolean playerInventoryWindowHit = false;
            String playerInventoryDiagnostic = "not-probed";
            try {
                inventoryRow = KeybinderInventorySelectionBridge.itemUnderMouse(
                        hud, mouseX, mouseY);
                if (inventoryRow == null) {
                    KeybinderInventorySelectionBridge.PlayerInventoryHit playerHit =
                            KeybinderInventorySelectionBridge.playerInventoryUnderMouse(
                                    hud, mouseX, mouseY);
                    playerInventoryWindowHit = playerHit.isHit();
                    playerInventoryDiagnostic = playerHit.diagnostic();
                    inventoryRow = playerHit.getItem();
                }
            } catch (ReflectiveOperationException | RuntimeException failure) {
                log.diagnostic("bulk-transfer hovered GUI resolution failed open: "
                        + failure.getClass().getSimpleName() + ": "
                        + String.valueOf(failure.getMessage()));
            }
            PickableUnit worldHovered = hud.getWorld().getCurrentHoveredObject();
            ExecutionHoverOverride.Snapshot hoverOverride =
                    ExecutionHoverOverride.current();
            long worldHoveredId = hoverOverride != null
                    && hoverOverride.getWorldObjectId() > 0L
                    ? hoverOverride.getWorldObjectId()
                    : worldHovered == null ? 0L : worldHovered.getId();
            boolean groundItemCandidate = hoverOverride != null
                    && hoverOverride.getWorldObjectId() > 0L
                    ? hoverOverride.isGroundItem()
                    : worldHovered instanceof GroundItemCellRenderable;
            InventoryMetaItem worldInventory = null;
            InventoryReference openWorldInventory = null;
            if (groundItemCandidate) {
                try {
                    worldInventory = access.inventoryItem(hud, worldHoveredId);
                } catch (ReflectiveOperationException | RuntimeException failure) {
                    log.diagnostic("bulk-transfer world-container metadata lookup failed open: "
                            + failure.getClass().getSimpleName() + ": "
                            + String.valueOf(failure.getMessage()));
                }
                try {
                    openWorldInventory = access.openWorldInventory(
                            hud, worldHoveredId);
                } catch (ReflectiveOperationException | RuntimeException failure) {
                    log.diagnostic("bulk-transfer open world-inventory lookup failed open: "
                            + failure.getClass().getSimpleName() + ": "
                            + String.valueOf(failure.getMessage()));
                }
            }
            boolean locallyKnownWorldContainer = groundItemCandidate
                    && (openWorldInventory != null || (worldInventory != null
                    && worldHoveredId == worldInventory.getId()
                    && ItemTypeUtilites.isContainer(worldInventory.getTypeBits())));
            boolean worldContainer = isKnownWorldContainer(
                    groundItemCandidate, worldHoveredId,
                    worldInventory == null ? 0L : worldInventory.getId(),
                    worldInventory == null ? (short) 0 : worldInventory.getTypeBits(),
                    openWorldInventory != null, true);
            long destinationId = chooseHoveredDestination(
                    inventoryRow == null ? 0L : inventoryRow.getId(),
                    worldContainer ? worldHoveredId : 0L);
            log.diagnostic("bulk-transfer hovered destination: mouseX=" + mouseX
                    + ", mouseY=" + mouseY + ", mouseUnavailable="
                    + hud.getWorld().getClient().isMouseUnavailable()
                    + ", playerInventoryWindowHit=" + playerInventoryWindowHit
                    + ", playerInventoryProbe={" + playerInventoryDiagnostic + "}"
                    + ", inventoryRowId="
                    + (inventoryRow == null ? 0L : inventoryRow.getId())
                    + ", worldHoveredId="
                    + worldHoveredId
                    + ", hoverOverrideUsed=" + (hoverOverride != null)
                    + ", worldInventoryMetadataId="
                    + (worldInventory == null ? 0L : worldInventory.getId())
                    + ", openWorldInventory="
                    + (openWorldInventory == null ? "none"
                    : "{id=" + openWorldInventory.getId() + ",name='"
                    + openWorldInventory.getName() + "'}")
                    + ", locallyKnownWorldContainer=" + locallyKnownWorldContainer
                    + ", worldHoveredIsKnownContainer=" + worldContainer
                    + ", resolvedDestinationId=" + destinationId);
            if (!BulkInventoryDestinationPolicy.isValid(destinationId))
                throw new StepUnavailableException(Messages.text(
                        "unavailable.bulk_hovered_inventory"));
            return destinationId;
        }
        InventoryReference captured = step.getCapturedDestination();
        if (kind != BulkDestinationKind.CAPTURED_INVENTORY
                || captured == null
                || !BulkInventoryDestinationPolicy.isValid(captured.getId()))
            throw new StepUnavailableException(Messages.text(
                    "validation.bulk_destination_missing"));
        log.diagnostic("bulk-transfer captured destination resolved: capturedId="
                + captured.getId() + ", capturedName='" + captured.getName() + "'");
        return captured.getId();
    }

    static long chooseHoveredDestination(long inventoryDestinationId,
                                         long knownWorldContainerId) {
        if (BulkInventoryDestinationPolicy.isValid(inventoryDestinationId))
            return inventoryDestinationId;
        return knownWorldContainerId > 0L ? knownWorldContainerId : 0L;
    }

    static boolean isKnownWorldContainer(boolean groundItemRenderable, long candidateId,
                                         long metadataId, short typeBits,
                                         boolean matchingInventoryOpen,
                                         boolean allowServerValidation) {
        return groundItemRenderable && candidateId > 0L
                && (allowServerValidation || matchingInventoryOpen || (candidateId == metadataId
                && ItemTypeUtilites.isContainer(typeBits)));
    }
}
