package org.keybinder.wurm.recording;

import org.keybinder.wurm.model.ActivateToolStep;
import org.keybinder.wurm.model.KeybindStep;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ShadowRecorderTest {
    @Test
    public void capturesToolbeltSelectionAsNativeActivateStep() {
        ShadowRecorder recorder = new ShadowRecorder();
        recorder.start();

        recorder.observeToolbeltSlot(9);

        List<KeybindStep> captured = recorder.stop();
        assertEquals(1, captured.size());
        assertEquals(ActivateToolStep.class, captured.get(0).getClass());
        assertEquals(10, ((ActivateToolStep) captured.get(0)).getTarget().getSlot());
    }

    @Test
    public void ignoresToolbeltClicksOutsideCaptureAndInvalidSlots() {
        ShadowRecorder recorder = new ShadowRecorder();
        recorder.observeToolbeltSlot(0);
        assertTrue(recorder.snapshot().isEmpty());

        recorder.start();
        recorder.observeToolbeltSlot(-1);
        recorder.observeToolbeltSlot(10);
        assertTrue(recorder.stop().isEmpty());
    }
}
