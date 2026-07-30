package org.keybinder.wurm.command;

import org.keybinder.wurm.model.KeybindStep;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable preflight result for best-effort keybind execution. */
public final class ExecutionPlan {
    public static final class Entry {
        private final int index;
        private final KeybindStep step;
        private final int queueCost;
        private final Throwable skippedBy;

        Entry(int index, KeybindStep step, int queueCost, Throwable skippedBy) {
            this.index = index;
            this.step = step;
            this.queueCost = queueCost;
            this.skippedBy = skippedBy;
        }

        public int getIndex() { return index; }
        public KeybindStep getStep() { return step; }
        public int getQueueCost() { return queueCost; }
        public boolean isSkipped() { return skippedBy != null; }
        public Throwable getSkippedBy() { return skippedBy; }
    }

    private final List<Entry> entries;
    private final int queueCost;

    ExecutionPlan(List<Entry> entries, int queueCost) {
        this.entries = Collections.unmodifiableList(new ArrayList<Entry>(entries));
        this.queueCost = queueCost;
    }

    public List<Entry> getEntries() { return entries; }
    public int getQueueCost() { return queueCost; }
}
