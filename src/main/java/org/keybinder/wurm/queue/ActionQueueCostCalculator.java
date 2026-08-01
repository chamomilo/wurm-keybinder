package org.keybinder.wurm.queue;

import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.ConsoleCommandStep;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.model.SmartImproveStep;
import org.keybinder.wurm.model.TargetKind;

import java.util.List;

public final class ActionQueueCostCalculator {
    /**
     * Returns the design-time cost used by editor, save, enable and status checks.
     * Automatic and filtered nearby/hover targets deliberately contribute zero here
     * because their actual target count is known only during execution. ActionExecutor
     * performs that runtime resolution before QueueCapacityPreflight permits the first
     * action to be sent.
     */
    public QueueCost stepCost(ActionStep step) {
        TargetKind target = step.getTarget().getKind();
        if (target == TargetKind.AREA) return QueueCost.fixed(9);
        if (target == TargetKind.NEARBY || target == TargetKind.NEARBY_TYPE
                || target == TargetKind.HOVER_TYPE)
            return QueueCost.fixed(0);
        if (target == TargetKind.NEARBY_RADIUS) return QueueCost.dynamic();
        return QueueCost.fixed(1);
    }

    public QueueCost chainCost(List<ActionStep> steps) {
        int total = 0;
        for (ActionStep step : steps) {
            QueueCost cost = stepCost(step);
            if (cost.getKind() == QueueCost.Kind.UNKNOWN) return QueueCost.unknown();
            if (cost.getKind() == QueueCost.Kind.DYNAMIC) return QueueCost.dynamic();
            total += cost.getValue();
        }
        return QueueCost.fixed(total);
    }

    public QueueCost keybindCost(KeybindRecord record) {
        int total = 0;
        for (KeybindStep step : record.getKeybindSteps()) {
            QueueCost cost;
            if (step instanceof ActionStep) cost = stepCost((ActionStep) step);
            else if (step instanceof SmartImproveStep) cost = QueueCost.dynamic();
            else if (step instanceof ConsoleCommandStep) cost = QueueCost.unknown();
            else cost = QueueCost.fixed(0);
            if (cost.getKind() == QueueCost.Kind.UNKNOWN) return QueueCost.unknown();
            if (cost.getKind() == QueueCost.Kind.DYNAMIC) return QueueCost.dynamic();
            total += cost.getValue();
        }
        return QueueCost.fixed(total);
    }
}
