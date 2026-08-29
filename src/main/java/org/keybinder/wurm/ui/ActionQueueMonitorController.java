package org.keybinder.wurm.ui;

import org.keybinder.wurm.queue.ActionQueueEntry;

import java.util.List;

/** Narrow contract used by the always-visible action-queue HUD strip. */
public interface ActionQueueMonitorController {
    int getQueueMonitorSlots();
    List<ActionQueueEntry> getMonitoredActions();
    void cancelMonitoredAction(long sequence);
}
