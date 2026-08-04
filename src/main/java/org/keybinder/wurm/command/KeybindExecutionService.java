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
import org.keybinder.wurm.queue.QueueCapacityPreflight;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;

public final class KeybindExecutionService {
    private final ActionExecutor actions;
    private final SmartImproveExecutor improve;
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
            int occupied = occupiedQueueSlots == null ? 0
                    : Math.max(0, occupiedQueueSlots.getAsInt());
            int free = Math.max(0, queueLimit - occupied);
            ExecutionPlan plan;
            try (ExecutionHoverOverride.Scope ignored =
                         ExecutionHoverOverride.push(hoverSnapshot)) {
                plan = planWithinQueue(record.getKeybindSteps(), hud, free,
                        queueLimit, occupiedQueueSlots);
            }
            for (ExecutionPlan.Entry entry : plan.getEntries()) {
                if (!entry.isSkipped()) continue;
                Throwable failure = entry.getSkippedBy();
                String message = skipMessage(entry.getIndex(), entry.getStep(),
                        safeMessage(failure));
                if (failure instanceof StepUnavailableException) log.warning(message);
                else log.error(message, failure);
            }
            QueueCapacityPreflight.requireFits(plan.getQueueCost(), queueLimit, occupied);
            waitingForBulk = new ExecutionSequence(
                    record.getId(), hud, plan, hoverSnapshot).runFrom(0);
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
        improve.clearPrepared();
        bulk.clearPrepared();
        activeRecordId = null;
    }

    /** Executes until the next asynchronous bulk handshake, then resumes in-order. */
    private final class ExecutionSequence {
        private final String recordId;
        private final HeadsUpDisplay hud;
        private final ExecutionPlan plan;
        private final ExecutionHoverOverride.Snapshot hoverSnapshot;

        private ExecutionSequence(String recordId, HeadsUpDisplay hud,
                                  ExecutionPlan plan,
                                  ExecutionHoverOverride.Snapshot hoverSnapshot) {
            this.recordId = recordId;
            this.hud = hud;
            this.plan = plan;
            this.hoverSnapshot = hoverSnapshot;
        }

        private boolean runFrom(int position) {
            ExecutionOriginGuard.enterInternal();
            try (ExecutionHoverOverride.Scope ignored =
                         ExecutionHoverOverride.push(hoverSnapshot)) {
                List<ExecutionPlan.Entry> entries = plan.getEntries();
                for (int current = position; current < entries.size(); current++) {
                    ExecutionPlan.Entry entry = entries.get(current);
                    if (entry.isSkipped()) continue;
                    int index = entry.getIndex();
                    KeybindStep step = entry.getStep();
                    try {
                        if (step instanceof BulkTransferStep) {
                            final int resumeAt = current + 1;
                            bulk.execute((BulkTransferStep) step, hud,
                                    proceed -> continueAfterBulk(resumeAt, proceed));
                            return true;
                        }
                        executeStep(step, hud, entry.getQueueCost());
                    } catch (StepUnavailableException unavailable) {
                        log.warning(skipMessage(index, step, unavailable.getMessage()));
                    } catch (RuntimeException failure) {
                        log.error(skipMessage(index, step, safeMessage(failure)), failure);
                    } catch (ReflectiveOperationException failure) {
                        log.error(skipMessage(index, step, safeMessage(failure)), failure);
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
                if (proceed) waitingAgain = runFrom(position);
            } catch (RuntimeException failure) {
                log.error(Messages.text("error.bulk_continuation"), failure);
            } finally {
                if (!waitingAgain) finishExecution(recordId);
            }
        }
    }

    private ExecutionPlan planWithinQueue(List<KeybindStep> steps, HeadsUpDisplay hud,
                                          int freeQueueSlots, int queueLimit,
                                          IntSupplier occupiedQueueSlots) {
        List<ExecutionPlan.Entry> entries = new ArrayList<ExecutionPlan.Entry>(steps.size());
        int nonImproveCost = 0;
        for (int index = 0; index < steps.size(); index++) {
            KeybindStep step = steps.get(index);
            if (step instanceof SmartImproveStep) {
                entries.add(null);
                continue;
            }
            try {
                int cost = runtimeStepCost(step, hud);
                if (cost < 0) throw new IllegalStateException("Negative queue cost");
                entries.add(new ExecutionPlan.Entry(index, step, cost, null));
                nonImproveCost += cost;
            } catch (Exception failure) {
                entries.add(new ExecutionPlan.Entry(index, step, 0, failure));
            }
        }
        ExecutionPlan nonImprovePlan = capDynamicFanOutWithinQueue(
                new ExecutionPlan(entries, nonImproveCost), freeQueueSlots);
        entries = new ArrayList<ExecutionPlan.Entry>(nonImprovePlan.getEntries());
        nonImproveCost = nonImprovePlan.getQueueCost();
        int remaining = Math.max(0, freeQueueSlots - nonImproveCost);
        int improveCost = 0;
        for (int index = 0; index < steps.size(); index++) {
            KeybindStep step = steps.get(index);
            if (!(step instanceof SmartImproveStep)) continue;
            try {
                int cost = improve.prepareWithinBudget((SmartImproveStep) step, hud,
                        remaining, queueLimit, occupiedQueueSlots);
                entries.set(index, new ExecutionPlan.Entry(index, step, cost, null));
                remaining = Math.max(0, remaining - cost);
                improveCost += cost;
            } catch (Exception failure) {
                entries.set(index, new ExecutionPlan.Entry(index, step, 0, failure));
            }
        }
        return new ExecutionPlan(entries, nonImproveCost + improveCost);
    }

    /**
     * An automatic Nearby or filtered Hover action is one keybind step that can
     * expand to many server queue entries. Preserve at least one target per available
     * fan-out step so the keybind itself remains atomic, then spend the remaining
     * queue capacity on additional targets in step order. Targets beyond that budget
     * are intentionally silent; only failure of the minimum keybind itself reaches
     * the ordinary one-line queue-capacity warning.
     */
    static ExecutionPlan capDynamicFanOutWithinQueue(ExecutionPlan plan,
                                                      int freeQueueSlots) {
        int minimumCost = 0;
        for (ExecutionPlan.Entry entry : plan.getEntries()) {
            if (entry == null || entry.isSkipped()) continue;
            minimumCost += isBudgetedFanOut(entry.getStep()) && entry.getQueueCost() > 0
                    ? 1 : entry.getQueueCost();
        }
        boolean minimumFits = minimumCost <= freeQueueSlots;
        int extraCapacity = minimumFits ? freeQueueSlots - minimumCost : 0;
        int cappedCost = 0;
        List<ExecutionPlan.Entry> capped =
                new ArrayList<ExecutionPlan.Entry>(plan.getEntries().size());
        for (ExecutionPlan.Entry entry : plan.getEntries()) {
            if (entry == null || entry.isSkipped() || !isBudgetedFanOut(entry.getStep())
                    || entry.getQueueCost() <= 0) {
                capped.add(entry);
                if (entry != null) cappedCost += entry.getQueueCost();
                continue;
            }
            int extraTargets = minimumFits
                    ? Math.min(entry.getQueueCost() - 1, extraCapacity) : 0;
            int allowedTargets = 1 + extraTargets;
            extraCapacity -= extraTargets;
            capped.add(new ExecutionPlan.Entry(entry.getIndex(), entry.getStep(),
                    allowedTargets, null));
            cappedCost += allowedTargets;
        }
        return new ExecutionPlan(capped, cappedCost);
    }

    private static boolean isBudgetedFanOut(KeybindStep step) {
        if (!(step instanceof ActionStep)) return false;
        TargetKind target = ((ActionStep) step).getTarget().getKind();
        return target == TargetKind.HOVER_TYPE || target == TargetKind.NEARBY;
    }

    private int runtimeStepCost(KeybindStep step, HeadsUpDisplay hud)
            throws ReflectiveOperationException {
        if (step instanceof ActionStep)
            return actions.runtimeQueueCost((ActionStep) step, hud);
        if (step instanceof SmartImproveStep)
            return improve.runtimeCost((SmartImproveStep) step, hud);
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
