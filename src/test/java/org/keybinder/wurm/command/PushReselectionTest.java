package org.keybinder.wurm.command;

import com.wurmonline.shared.constants.PlayerAction;
import org.junit.Test;
import org.keybinder.wurm.catalog.VanillaCatalogStepFactory;
import org.keybinder.wurm.catalog.VanillaKeybindCatalog;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.model.TargetKind;
import org.keybinder.wurm.model.TargetSpec;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PushReselectionTest {
    @Test
    public void catalogPushUsesActionExecutorSelectionRetentionPath() {
        VanillaKeybindCatalog catalog = new VanillaKeybindCatalog();
        VanillaKeybindCatalog.Entry push = catalog.find(PlayerAction.PUSH.getBind());
        KeybindStep step = new VanillaCatalogStepFactory().create(
                catalog.categoryFor(push.getCommand()), push,
                TargetSpec.simple(TargetKind.SELECTED));

        assertTrue(step instanceof ActionStep);
        assertEquals(PlayerAction.PUSH.getId(), ((ActionStep) step).getActionId());
        assertTrue(ActionExecutor.keepsSelectedTarget(((ActionStep) step).getActionId()));
        assertTrue(ActionExecutor.keepsSelectedTarget(PlayerAction.PUSH_GENTLY.getId()));
    }

    @Test
    public void queuedPushesRearmSelectionUntilEveryRecreationArrives() {
        AtomicLong now = new AtomicLong(100L);
        PushSelectionRetention retention =
                new PushSelectionRetention(now::get, 1_000L);
        retention.arm(42L);
        retention.arm(42L);
        retention.arm(42L);

        assertFalse(retention.afterRecreated(99L, true));
        assertEquals(3, retention.pendingCount());
        assertTrue(retention.afterRecreated(42L, true));
        assertEquals(2, retention.pendingCount());
        assertTrue(retention.afterRecreated(42L, true));
        assertEquals(1, retention.pendingCount());
        assertFalse(retention.afterRecreated(42L, true));
        assertEquals(0, retention.pendingCount());
    }

    @Test
    public void changedSelectionOrExpiredSeriesCannotStealSelectionBack() {
        AtomicLong now = new AtomicLong(100L);
        PushSelectionRetention retention =
                new PushSelectionRetention(now::get, 1_000L);
        retention.arm(42L);
        retention.arm(42L);

        assertFalse(retention.afterRecreated(42L, false));
        assertEquals(0, retention.pendingCount());

        retention.arm(42L);
        retention.arm(42L);
        now.set(1_100L);
        assertFalse(retention.afterRecreated(42L, true));
        assertEquals(0, retention.pendingCount());
    }
}
