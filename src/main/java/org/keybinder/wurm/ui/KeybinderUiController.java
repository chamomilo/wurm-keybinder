package org.keybinder.wurm.ui;

/** Aggregate implemented by the mod; individual windows depend on narrower contracts. */
public interface KeybinderUiController extends KeybinderWindowController,
        ImportReviewController, LegacyMigrationController, MergeController,
        TagController, ActionQueueMonitorController {
}
