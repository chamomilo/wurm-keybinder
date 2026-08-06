package org.keybinder.wurm.command;

import com.wurmonline.client.renderer.PickableUnit;
import com.wurmonline.shared.constants.PlayerAction;

import java.util.Arrays;

/** Immutable source and target snapshot prepared before queue preflight. */
final class ResolvedActionPlan {
    private final ActionSourceResolver.ResolvedSource source;
    private final PlayerAction action;
    private final long[] targetIds;
    private final int queueCost;
    private final boolean batchDispatch;
    private final boolean objectTargets;
    private final boolean budgetedFanOut;
    private final PickableUnit selectBeforeDispatch;

    ResolvedActionPlan(ActionSourceResolver.ResolvedSource source,
                       PlayerAction action, long[] targetIds, int queueCost,
                       boolean batchDispatch, boolean objectTargets,
                       boolean budgetedFanOut,
                       PickableUnit selectBeforeDispatch) {
        this.source = source;
        this.action = action;
        this.targetIds = targetIds == null
                ? new long[0] : Arrays.copyOf(targetIds, targetIds.length);
        this.queueCost = queueCost;
        this.batchDispatch = batchDispatch;
        this.objectTargets = objectTargets;
        this.budgetedFanOut = budgetedFanOut;
        this.selectBeforeDispatch = selectBeforeDispatch;
    }

    ActionSourceResolver.ResolvedSource getSource() { return source; }
    PlayerAction getAction() { return action; }
    int getQueueCost() { return queueCost; }
    boolean isBatchDispatch() { return batchDispatch; }
    boolean hasObjectTargets() { return objectTargets; }
    PickableUnit getSelectBeforeDispatch() { return selectBeforeDispatch; }

    long[] targetsForExecution(int plannedQueueCost) {
        int count = targetIds.length;
        if (budgetedFanOut)
            count = Math.min(count, Math.max(0, plannedQueueCost));
        return Arrays.copyOf(targetIds, count);
    }
}
