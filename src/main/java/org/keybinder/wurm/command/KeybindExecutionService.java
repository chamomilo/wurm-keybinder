package org.keybinder.wurm.command;

import com.wurmonline.client.game.inventory.InventoryMetaItem;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.client.renderer.gui.PaperDollSlot;
import org.keybinder.wurm.event.EventLogger;
import org.keybinder.wurm.i18n.Messages;
import org.keybinder.wurm.integration.ClientAccess;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.ActionSourcePolicy;
import org.keybinder.wurm.model.ActivateToolStep;
import org.keybinder.wurm.model.ArcheologyIdentifyStep;
import org.keybinder.wurm.model.ConsoleCommandStep;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.model.SmartImproveStep;
import org.keybinder.wurm.model.TargetKind;
import org.keybinder.wurm.model.TargetSpec;
import org.keybinder.wurm.model.ItemSelectorKind;
import org.keybinder.wurm.command.ItemSelectorCodec;
import org.keybinder.wurm.model.VanillaActionStep;
import org.keybinder.wurm.model.BulkTransferStep;
import org.keybinder.wurm.integration.ExecutionOriginGuard;
import org.keybinder.wurm.integration.ExecutionHoverOverride;
import org.keybinder.wurm.queue.QueueCapacityException;
import org.keybinder.wurm.queue.QueueCapacityPreflight;

import java.util.List;
import java.util.function.IntSupplier;

public final class KeybindExecutionService {
    private final ActionExecutor actions;
    private final SmartImproveExecutor improve;
    private final ArcheologyIdentifyExecutor archeologyIdentify;
    private final ClientAccess access;
    private final EventLogger log;
    private final BulkTransferExecutor bulk;
    private String activeRecordId;

    public KeybindExecutionService(ActionExecutor actions, ClientAccess access, EventLogger log) {
        this(actions, access, log, new WorldImproveTracker());
    }

    public KeybindExecutionService(ActionExecutor actions, ClientAccess access, EventLogger log,
                                   WorldImproveTracker worldImprove) {
        this(actions, access, log, worldImprove, new BulkTransferCoordinator(log));
    }

    public KeybindExecutionService(ActionExecutor actions, ClientAccess access, EventLogger log,
                                   WorldImproveTracker worldImprove,
                                   BulkTransferCoordinator bulkCoordinator) {
        this.actions = actions;
        this.improve = new SmartImproveExecutor(access, log, worldImprove);
        this.archeologyIdentify = new ArcheologyIdentifyExecutor(access, log);
        this.access = access;
        this.log = log;
        this.bulk = new BulkTransferExecutor(access, log, bulkCoordinator);
    }

    public void execute(KeybindRecord record, HeadsUpDisplay hud, int queueLimit)
            throws ReflectiveOperationException {
        execute(record, hud, queueLimit, () -> 0);
    }

    public void execute(KeybindRecord record, HeadsUpDisplay hud, int queueLimit,
                        IntSupplier occupiedQueueSlots)
            throws ReflectiveOperationException {
        execute(record, hud, queueLimit, occupiedQueueSlots, null);
    }

    public void execute(KeybindRecord record, HeadsUpDisplay hud, int queueLimit,
                        IntSupplier occupiedQueueSlots,
                        ExecutionHoverOverride.Snapshot hoverSnapshot)
            throws ReflectiveOperationException {
        if (!beginExecution(record.getId()))
            throw new IllegalStateException(
                    Messages.text("execution.sequence_pending", record.getName()));
        boolean waitingForBulk = false;
        try {
            waitingForBulk = new ExecutionSequence(
                    record.getId(), hud, record.getKeybindSteps(), hoverSnapshot,
                    queueLimit, occupiedQueueSlots).runFrom(0);
        } finally {
            if (!waitingForBulk) finishExecution(record.getId());
        }
    }

    private synchronized boolean beginExecution(String recordId) {
        if (activeRecordId != null) return false;
        activeRecordId = recordId;
        return true;
    }

    private synchronized void finishExecution(String recordId) {
        if (activeRecordId == null || !activeRecordId.equals(recordId)) return;
        actions.clearPrepared();
        improve.clearPrepared();
        archeologyIdentify.clearPrepared();
        bulk.clearPrepared();
        activeRecordId = null;
    }

    /** Executes until the next asynchronous bulk handshake, then resumes in-order. */
    private final class ExecutionSequence {
        private final String recordId;
        private final HeadsUpDisplay hud;
        private final List<KeybindStep> steps;
        private final ExecutionHoverOverride.Snapshot hoverSnapshot;
        private final int queueLimit;
        private final IntSupplier occupiedQueueSlots;
        private int occupiedBaseline;
        private int locallyReserved;

        private ExecutionSequence(String recordId, HeadsUpDisplay hud,
                                  List<KeybindStep> steps,
                                  ExecutionHoverOverride.Snapshot hoverSnapshot,
                                  int queueLimit,
                                  IntSupplier occupiedQueueSlots) {
            this.recordId = recordId;
            this.hud = hud;
            this.steps = steps;
            this.hoverSnapshot = hoverSnapshot;
            this.queueLimit = queueLimit;
            this.occupiedQueueSlots = occupiedQueueSlots;
            this.occupiedBaseline = reportedOccupied();
        }

        private boolean runFrom(int position) {
            ExecutionOriginGuard.enterInternal();
            try (ExecutionHoverOverride.Scope ignored =
                         ExecutionHoverOverride.push(hoverSnapshot)) {
                for (int current = position; current < steps.size(); current++) {
                    KeybindStep step = steps.get(current);
                    try {
                        int plannedQueueCost = prepareWithinCurrentQueue(step);
                        if (step instanceof BulkTransferStep) {
                            final int resumeAt = current + 1;
                            bulk.execute((BulkTransferStep) step, hud,
                                    proceed -> continueAfterBulk(resumeAt, proceed));
                            return true;
                        }
                        executeStep(step, hud, plannedQueueCost);
                        reserveLocally(plannedQueueCost);
                    } catch (StepUnavailableException unavailable) {
                        log.warning(skipMessage(current, step, unavailable.getMessage()));
                    } catch (QueueCapacityException capacity) {
                        log.warning(skipMessage(current, step, capacity.getMessage()));
                    } catch (RuntimeException failure) {
                        log.error(skipMessage(current, step, safeMessage(failure)), failure);
                    } catch (ReflectiveOperationException failure) {
                        log.error(skipMessage(current, step, safeMessage(failure)), failure);
                    }
                }
                return false;
            } finally {
                ExecutionOriginGuard.exitInternal();
            }
        }

        private void continueAfterBulk(int position, boolean proceed) {
            boolean waitingAgain = false;
            try {
                if (proceed) {
                    rebaseQueueEstimate();
                    waitingAgain = runFrom(position);
                }
            } catch (RuntimeException failure) {
                log.error(Messages.text("error.bulk_continuation"), failure);
            } finally {
                if (!waitingAgain) finishExecution(recordId);
            }
        }

        private int prepareWithinCurrentQueue(KeybindStep step)
                throws ReflectiveOperationException {
            int required = runtimeStepCost(step, hud);
            if (required < 0) throw new IllegalStateException("Negative queue cost");
            int occupied = effectiveOccupied();
            int free = QueueCapacityPreflight.remaining(queueLimit, occupied);
            int allowed = executableQueueCost(required, free, canSplitAcrossTargets(step));
            if (allowed < 0)
                throw new QueueCapacityException(Messages.text(
                        "execution.step_queue_remaining", required, free,
                        queueLimit, occupied));
            if (allowed < required) {
                int prepared = allowed;
                if (step instanceof SmartImproveStep)
                    prepared = improve.prepareWithinBudget((SmartImproveStep) step, hud,
                            allowed, queueLimit, occupiedQueueSlots);
                else if (step instanceof ArcheologyIdentifyStep)
                    prepared = archeologyIdentify.prepareWithinBudget(
                            (ArcheologyIdentifyStep) step, hud, allowed);
                if (prepared <= 0 && required > 0)
                    throw new QueueCapacityException(Messages.text(
                            "execution.step_queue_remaining", required, free,
                            queueLimit, occupied));
                return prepared;
            }
            return allowed;
        }

        private int reportedOccupied() {
            return occupiedQueueSlots == null ? 0
                    : Math.max(0, occupiedQueueSlots.getAsInt());
        }

        private int effectiveOccupied() {
            long local = (long) occupiedBaseline + locallyReserved;
            int localFloor = local > Integer.MAX_VALUE
                    ? Integer.MAX_VALUE : (int) local;
            return Math.max(reportedOccupied(), localFloor);
        }

        private void reserveLocally(int count) {
            if (count <= 0) return;
            locallyReserved = locallyReserved > Integer.MAX_VALUE - count
                    ? Integer.MAX_VALUE : locallyReserved + count;
        }

        private void rebaseQueueEstimate() {
            occupiedBaseline = reportedOccupied();
            locallyReserved = 0;
        }
    }

    static int executableQueueCost(int required, int free, boolean splittable) {
        if (required < 0) throw new IllegalArgumentException("Negative queue cost");
        int available = Math.max(0, free);
        if (required <= available) return required;
        if (splittable && available > 0) return available;
        return -1;
    }

    private static boolean canSplitAcrossTargets(KeybindStep step) {
        if (step instanceof SmartImproveStep || step instanceof ArcheologyIdentifyStep)
            return true;
        if (!(step instanceof ActionStep)) return false;
        TargetKind target = ((ActionStep) step).getTarget().getKind();
        return target == TargetKind.AREA || target == TargetKind.NEARBY_RADIUS
                || target == TargetKind.NEARBY || target == TargetKind.HOVER
                || target == TargetKind.HOVER_TYPE;
    }

    private int runtimeStepCost(KeybindStep step, HeadsUpDisplay hud)
            throws ReflectiveOperationException {
        if (step instanceof ActionStep)
            return actions.runtimeQueueCost((ActionStep) step, hud);
        if (step instanceof SmartImproveStep)
            return improve.runtimeCost((SmartImproveStep) step, hud);
        if (step instanceof ArcheologyIdentifyStep)
            return archeologyIdentify.runtimeCost((ArcheologyIdentifyStep) step, hud);
        if (step instanceof BulkTransferStep)
            return bulk.prepare((BulkTransferStep) step, hud);
        if (step instanceof ActivateToolStep) {
            if (((ActivateToolStep) step).getTarget().getKind() != TargetKind.HOVER)
                resolveActivateItem((ActivateToolStep) step, hud);
            return 0;
        }
        return 0;
    }

    private void executeStep(KeybindStep step, HeadsUpDisplay hud, int plannedQueueCost)
            throws ReflectiveOperationException {
        if (step instanceof ActionStep) {
            actions.executeStep((ActionStep) step, hud, plannedQueueCost);
        } else if (step instanceof ActivateToolStep) {
            activate((ActivateToolStep) step, hud);
        } else if (step instanceof SmartImproveStep) {
            improve.execute((SmartImproveStep) step, hud);
        } else if (step instanceof ArcheologyIdentifyStep) {
            archeologyIdentify.execute((ArcheologyIdentifyStep) step, hud);
        } else if (step instanceof BulkTransferStep) {
            bulk.execute((BulkTransferStep) step, hud);
        } else if (step instanceof VanillaActionStep) {
            executeVanilla((VanillaActionStep) step, hud);
        } else if (step instanceof ConsoleCommandStep) {
            String command = ((ConsoleCommandStep) step).getCommand();
            if (command.trim().toLowerCase(java.util.Locale.ENGLISH).startsWith("keybinder_run "))
                throw new IllegalArgumentException(Messages.text("validation.nested_run"));
            access.console(hud).handleInput(command, false);
        } else {
            throw new IllegalArgumentException(
                    Messages.text("execution.unsupported_step", step.getClass().getName()));
        }
    }

    private void executeVanilla(VanillaActionStep step, HeadsUpDisplay hud)
            throws ReflectiveOperationException {
        String command = step.getCommand().trim();
        if (command.length() >= 2 && command.charAt(0) == '"'
                && command.charAt(command.length() - 1) == '"') {
            access.console(hud).handleInput(command.substring(1, command.length() - 1), false);
            return;
        }
        com.wurmonline.client.console.ActionClass action =
                com.wurmonline.client.console.ActionClass.valueOf(
                        command.toUpperCase(java.util.Locale.ENGLISH));
        // Mirror WurmConsole.toggleKey: instantaneous actions are pressed and
        // released so no movement/key state remains latched.
        hud.getWorld().getPlayer().toggleKey(action, true);
        hud.getWorld().getPlayer().toggleKey(action, false);
    }

    private void activate(ActivateToolStep step, HeadsUpDisplay hud) throws ReflectiveOperationException {
        TargetSpec target = step.getTarget();
        if (target.getKind() == TargetKind.HOVER) {
            hud.getWorld().activateHoveredItem();
            return;
        }
        if (target.getKind() == TargetKind.EMPTY_HAND) {
            access.setActiveTool(hud, null);
            return;
        }
        InventoryMetaItem item = resolveActivateItem(step, hud);
        access.setActiveTool(hud, item);
    }

    private InventoryMetaItem resolveActivateItem(ActivateToolStep step, HeadsUpDisplay hud)
            throws ReflectiveOperationException {
        TargetSpec target = step.getTarget();
        if (target.getKind() == TargetKind.EMPTY_HAND) return null;
        InventoryMetaItem item;
        if (target.getKind() == TargetKind.TOOLBELT_SLOT) {
            item = hud.getToolBelt().getItemInSlot(target.getSlot() - 1);
        } else if (target.getKind() == TargetKind.EQUIPMENT_SLOT) {
            PaperDollSlot frame = access.equipmentSlot(
                    hud.getPaperDollInventory(), (byte) target.getSlot());
            item = frame == null || frame.getEquippedItem() == null
                    ? null : frame.getEquippedItem().getItem();
        } else if (target.getKind() == TargetKind.EXACT_OBJECT) {
            item = access.inventoryItem(hud, target.getObjectId());
        } else {
            throw new IllegalArgumentException(
                    Messages.text("unavailable.unsupported_activate_target", target.getKind()));
        }
        if (item == null)
            throw new StepUnavailableException(
                    Messages.text("unavailable.tool_target", TargetCodec.display(target)));
        return item;
    }

    String skipMessage(int zeroBasedIndex, KeybindStep step, String reason) {
        if (reason != null
                && reason.startsWith(Messages.text("event.command_word") + " ")
                && reason.contains(Messages.text("event.skipped_marker"))) return reason;
        return Messages.text("event.step_skipped",
                zeroBasedIndex + 1, describe(step), reason);
    }

    private String describe(KeybindStep step) {
        if (step instanceof ActionStep) {
            ActionStep action = (ActionStep) step;
            if (ActionSourcePolicy.acceptsSelectableTool(action.getActionId())
                    && action.getSource().getKind() != ItemSelectorKind.CURRENT_ACTIVE)
                return Messages.text("event.describe_action_source",
                        actions.actionName(action.getActionId()),
                        ItemSelectorCodec.display(action.getSource()),
                        TargetCodec.display(action.getTarget()));
            return Messages.text("event.describe_action",
                    actions.actionName(action.getActionId()),
                    TargetCodec.display(action.getTarget()));
        }
        if (step instanceof ActivateToolStep)
            return Messages.text("event.describe_activate",
                    TargetCodec.display(((ActivateToolStep) step).getTarget()));
        if (step instanceof SmartImproveStep)
            return Messages.text("event.describe_improve",
                    TargetCodec.display(((SmartImproveStep) step).getTarget()));
        if (step instanceof ArcheologyIdentifyStep)
            return Messages.text("event.describe_archeology_identify",
                    TargetCodec.display(((ArcheologyIdentifyStep) step).getTarget()));
        if (step instanceof BulkTransferStep) {
            BulkTransferStep transfer = (BulkTransferStep) step;
            String item = transfer.getSource() == null || transfer.getSource().getItem() == null
                    ? "?" : transfer.getSource().getItem().getName();
            return Messages.text("event.describe_bulk", transfer.getQuantity(), item,
                    transfer.getDestinationKind());
        }
        if (step instanceof ConsoleCommandStep)
            return Messages.text("event.describe_console");
        if (step instanceof VanillaActionStep)
            return Messages.text("event.describe_vanilla",
                    ((VanillaActionStep) step).getCommand());
        return Messages.text("event.describe_unknown");
    }

    private static String safeMessage(Throwable error) {
        return error.getMessage() == null || error.getMessage().trim().isEmpty()
                ? error.getClass().getSimpleName() : error.getMessage();
    }

}
