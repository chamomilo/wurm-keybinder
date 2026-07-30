package org.keybinder.wurm.command;

import com.wurmonline.client.game.inventory.InventoryMetaItem;
import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.client.renderer.gui.PaperDollSlot;
import org.keybinder.wurm.event.EventLogger;
import org.keybinder.wurm.integration.ClientAccess;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.ActivateToolStep;
import org.keybinder.wurm.model.ConsoleCommandStep;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.model.SmartImproveStep;
import org.keybinder.wurm.model.TargetKind;
import org.keybinder.wurm.model.TargetSpec;
import org.keybinder.wurm.model.VanillaActionStep;
import org.keybinder.wurm.recording.ShadowRecorder;

import java.util.HashSet;
import java.util.Set;

public final class KeybindExecutionService {
    private static final ThreadLocal<Set<String>> ACTIVE = new ThreadLocal<Set<String>>() {
        @Override protected Set<String> initialValue() { return new HashSet<String>(); }
    };

    private final ActionExecutor actions;
    private final SmartImproveExecutor improve;
    private final ClientAccess access;
    private final EventLogger log;
    private final ExecutionPlanner planner = new ExecutionPlanner();

    public KeybindExecutionService(ActionExecutor actions, ClientAccess access, EventLogger log) {
        this(actions, access, log, new ImproveRequirementTracker(), Runnable::run);
    }

    public KeybindExecutionService(ActionExecutor actions, ClientAccess access, EventLogger log,
                                   ImproveRequirementTracker tracker) {
        this(actions, access, log, tracker, Runnable::run);
    }

    public KeybindExecutionService(ActionExecutor actions, ClientAccess access, EventLogger log,
                                   ImproveRequirementTracker tracker,
                                   java.util.function.Consumer<Runnable> scheduler) {
        this.actions = actions;
        this.improve = new SmartImproveExecutor(access, tracker, log, scheduler);
        this.access = access;
        this.log = log;
    }

    public void execute(KeybindRecord record, HeadsUpDisplay hud, int queueLimit)
            throws ReflectiveOperationException {
        if (!ACTIVE.get().add(record.getId()))
            throw new IllegalStateException("Recursive keybind invocation: " + record.getName());
        try {
            ExecutionPlan plan = planner.plan(record.getKeybindSteps(),
                    step -> runtimeStepCost(step, hud));
            for (ExecutionPlan.Entry entry : plan.getEntries()) {
                if (!entry.isSkipped()) continue;
                Throwable failure = entry.getSkippedBy();
                String message = skipMessage(entry.getIndex(), entry.getStep(),
                        safeMessage(failure));
                if (failure instanceof StepUnavailableException) log.warning(message);
                else log.error(message, failure);
            }
            if (queueLimit > 0 && plan.getQueueCost() > queueLimit)
                throw new IllegalStateException("Executable steps require " + plan.getQueueCost()
                        + " queued actions; current limit is " + queueLimit);

            ShadowRecorder.enterInternal();
            try {
                for (ExecutionPlan.Entry entry : plan.getEntries()) {
                    if (entry.isSkipped()) continue;
                    int index = entry.getIndex();
                    KeybindStep step = entry.getStep();
                    try {
                        executeStep(step, hud);
                    } catch (StepUnavailableException unavailable) {
                        log.warning(skipMessage(index, step, unavailable.getMessage()));
                    } catch (RuntimeException failure) {
                        log.error(skipMessage(index, step, safeMessage(failure)), failure);
                    } catch (ReflectiveOperationException failure) {
                        log.error(skipMessage(index, step, safeMessage(failure)), failure);
                    }
                }
            } finally {
                ShadowRecorder.exitInternal();
            }
        } finally {
            ACTIVE.get().remove(record.getId());
            if (ACTIVE.get().isEmpty()) ACTIVE.remove();
        }
    }

    private int runtimeStepCost(KeybindStep step, HeadsUpDisplay hud)
            throws ReflectiveOperationException {
        if (step instanceof ActionStep)
            return actions.runtimeQueueCost((ActionStep) step, hud);
        if (step instanceof SmartImproveStep)
            return improve.runtimeCost((SmartImproveStep) step, hud);
        if (step instanceof ActivateToolStep) {
            resolveActivateItem((ActivateToolStep) step, hud);
            return 0;
        }
        return 0;
    }

    private void executeStep(KeybindStep step, HeadsUpDisplay hud) throws ReflectiveOperationException {
        if (step instanceof ActionStep) {
            actions.executeStep((ActionStep) step, hud);
        } else if (step instanceof ActivateToolStep) {
            activate((ActivateToolStep) step, hud);
        } else if (step instanceof SmartImproveStep) {
            improve.execute((SmartImproveStep) step, hud);
        } else if (step instanceof VanillaActionStep) {
            executeVanilla((VanillaActionStep) step, hud);
        } else if (step instanceof ConsoleCommandStep) {
            String command = ((ConsoleCommandStep) step).getCommand();
            if (command.trim().toLowerCase(java.util.Locale.ENGLISH).startsWith("keybinder_run "))
                throw new IllegalArgumentException("keybinder_run is not allowed inside a console step");
            access.console(hud).handleInput(command, false);
        } else {
            throw new IllegalArgumentException("Unsupported keybind step " + step.getClass().getName());
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
            throw new IllegalArgumentException("Unsupported activate-tool target "
                    + target.getKind());
        }
        if (item == null)
            throw new StepUnavailableException("Tool target "
                    + TargetCodec.display(target) + " was not found");
        return item;
    }

    String skipMessage(int zeroBasedIndex, KeybindStep step, String reason) {
        if (reason != null && reason.startsWith("Command ")
                && reason.contains(" skipped ")) return reason;
        return "Step " + (zeroBasedIndex + 1) + " (" + describe(step) + "): "
                + reason + ", skipping.";
    }

    private String describe(KeybindStep step) {
        if (step instanceof ActionStep) {
            ActionStep action = (ActionStep) step;
            return actions.actionName(action.getActionId()) + " on "
                    + TargetCodec.display(action.getTarget());
        }
        if (step instanceof ActivateToolStep)
            return "activate " + TargetCodec.display(((ActivateToolStep) step).getTarget());
        if (step instanceof SmartImproveStep)
            return "smart improve " + TargetCodec.display(((SmartImproveStep) step).getTarget());
        if (step instanceof ConsoleCommandStep) return "console command";
        if (step instanceof VanillaActionStep)
            return "vanilla " + ((VanillaActionStep) step).getCommand();
        return step.getKind().name().toLowerCase(java.util.Locale.ENGLISH);
    }

    private static String safeMessage(Throwable error) {
        return error.getMessage() == null || error.getMessage().trim().isEmpty()
                ? error.getClass().getSimpleName() : error.getMessage();
    }
}
