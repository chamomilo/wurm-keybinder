package org.keybinder.wurm.recording;

import com.wurmonline.shared.constants.PlayerAction;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.ActivateToolStep;
import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.model.TargetSpec;
import org.keybinder.wurm.command.TargetCodec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ShadowRecorder {
    private static final ThreadLocal<Integer> INTERNAL_DEPTH = new ThreadLocal<Integer>() {
        @Override protected Integer initialValue() { return 0; }
    };
    private final List<KeybindStep> recorded = new ArrayList<KeybindStep>();
    private volatile boolean recording;
    private final ThreadLocal<TargetSpec> targetContext = new ThreadLocal<TargetSpec>();

    public void start() {
        synchronized (recorded) { recorded.clear(); }
        recording = true;
    }

    public List<KeybindStep> stop() {
        recording = false;
        synchronized (recorded) { return new ArrayList<>(recorded); }
    }

    public void cancel() {
        recording = false;
        synchronized (recorded) { recorded.clear(); }
        targetContext.remove();
    }

    public boolean isRecording() { return recording; }
    public void setTargetContext(String target) { targetContext.set(TargetCodec.decode(target)); }
    public void clearTargetContext() { targetContext.remove(); }

    public void observe(PlayerAction action) {
        if (!recording || INTERNAL_DEPTH.get() > 0 || action == null) return;
        TargetSpec target = targetContext.get();
        if (target == null) target = TargetCodec.decode("hover");
        synchronized (recorded) {
            recorded.add(new ActionStep(action.getId(), target));
        }
    }

    public void observeToolbeltSlot(int zeroBasedSlot) {
        if (!recording || INTERNAL_DEPTH.get() > 0 || zeroBasedSlot < 0 || zeroBasedSlot >= 10) return;
        synchronized (recorded) {
            recorded.add(new ActivateToolStep(TargetSpec.toolbeltSlot(zeroBasedSlot + 1)));
        }
    }

    public List<KeybindStep> snapshot() {
        synchronized (recorded) { return Collections.unmodifiableList(new ArrayList<>(recorded)); }
    }

    public static void enterInternal() { INTERNAL_DEPTH.set(INTERNAL_DEPTH.get() + 1); }
    public static void exitInternal() { INTERNAL_DEPTH.set(Math.max(0, INTERNAL_DEPTH.get() - 1)); }
}
