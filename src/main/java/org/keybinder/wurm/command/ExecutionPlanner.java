package org.keybinder.wurm.command;

import org.keybinder.wurm.model.KeybindStep;

import java.util.ArrayList;
import java.util.List;

/** Resolves runtime availability without turning an unavailable step into a chain failure. */
public final class ExecutionPlanner {
    public interface CostResolver {
        int cost(KeybindStep step) throws Exception;
    }

    public ExecutionPlan plan(List<KeybindStep> steps, CostResolver resolver) {
        List<ExecutionPlan.Entry> entries = new ArrayList<ExecutionPlan.Entry>(steps.size());
        int total = 0;
        for (int index = 0; index < steps.size(); index++) {
            KeybindStep step = steps.get(index);
            try {
                int cost = resolver.cost(step);
                if (cost < 0) throw new IllegalStateException("Negative queue cost");
                entries.add(new ExecutionPlan.Entry(index, step, cost, null));
                total += cost;
            } catch (Exception failure) {
                entries.add(new ExecutionPlan.Entry(index, step, 0, failure));
            }
        }
        return new ExecutionPlan(entries, total);
    }
}
