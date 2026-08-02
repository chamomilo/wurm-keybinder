package org.keybinder.wurm.command;

import org.junit.Test;
import org.keybinder.wurm.event.EventLogger;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.model.TargetKind;
import org.keybinder.wurm.model.TargetSpec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class KeybinderCommandRouterTest {
    @Test public void unrelatedCommandIsNotConsumed() {
        FakeContext context = new FakeContext();

        assertFalse(router().route("bind", new String[]{"bind", "R"}, context));
        assertEquals(0, context.ensureCalls);
    }

    @Test public void addSelectedBuildsARecordAndRefreshesTheWindow() {
        FakeContext context = new FakeContext();
        context.selectedTarget = "selected";

        assertTrue(router().route("keybinder_add_selected",
                new String[]{"keybinder_add_selected", "K", "Hello_World", "32000"},
                context));

        assertEquals(1, context.ensureCalls);
        assertEquals(1, context.refreshCalls);
        assertEquals("Hello World", context.added.getName());
        assertEquals("K", context.added.getKey());
        ActionStep step = (ActionStep) context.added.getKeybindSteps().get(0);
        assertEquals(32000, step.getActionId());
        assertEquals(TargetKind.SELECTED, step.getTarget().getKind());
    }

    @Test public void invalidActionIdIsConsumedWithoutMutatingContext() {
        FakeContext context = new FakeContext();

        assertTrue(router().route("keybinder_add_selected",
                new String[]{"keybinder_add_selected", "K", "Bad", "not-a-number"},
                context));

        assertNull(context.added);
        assertEquals(0, context.refreshCalls);
    }

    @Test public void commitRejectsAnUnresolvedCapturedAction() {
        FakeContext context = new FakeContext();
        context.recorded = Collections.<KeybindStep>singletonList(
                new ActionStep((short) 32000, TargetSpec.simple(TargetKind.UNRESOLVED)));

        assertTrue(router().route("keybinder_commit",
                new String[]{"keybinder_commit", "R", "Recorded"}, context));

        assertNull(context.added);
        assertEquals(0, context.refreshCalls);
    }

    private static KeybinderCommandRouter router() {
        Logger logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        return new KeybinderCommandRouter(new EventLogger(logger));
    }

    private static final class FakeContext implements KeybinderCommandRouter.Context {
        private int ensureCalls;
        private int refreshCalls;
        private String selectedTarget = "hover";
        private KeybindRecord added;
        private List<KeybindStep> recorded = new ArrayList<KeybindStep>();

        @Override public void ensureReady() { ensureCalls++; }
        @Override public KeybindRecord find(String id) { return null; }
        @Override public void execute(KeybindRecord record) { }
        @Override public void printAll(boolean includeCommands) { }
        @Override public String selectedTarget() { return selectedTarget; }
        @Override public void add(KeybindRecord record) { added = record; }
        @Override public List<KeybindStep> recordedSteps() { return recorded; }
        @Override public int importAllReviewed() { return 0; }
        @Override public boolean delete(String id) { return false; }
        @Override public void restoreOriginalBindings() { }
        @Override public void refreshWindow() { refreshCalls++; }
    }
}
