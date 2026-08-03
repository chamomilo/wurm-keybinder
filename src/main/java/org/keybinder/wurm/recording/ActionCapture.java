package org.keybinder.wurm.recording;

import com.wurmonline.shared.constants.PlayerAction;
import org.keybinder.wurm.command.TargetCodec;
import org.keybinder.wurm.integration.ExecutionOriginGuard;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.TargetSpec;

/** Observes exactly one player action while the editor's Capture Action dialog is open. */
public final class ActionCapture {
    private volatile boolean armed;
    private ActionStep captured;
    private final ThreadLocal<TargetSpec> targetContext = new ThreadLocal<TargetSpec>();

    public synchronized void arm() {
        captured = null;
        armed = true;
    }

    public synchronized void cancel() {
        armed = false;
        captured = null;
        targetContext.remove();
    }

    public synchronized boolean isArmed() { return armed; }

    public void setTargetContext(String target) {
        if (armed) targetContext.set(TargetCodec.decode(target));
    }

    public void clearTargetContext() { targetContext.remove(); }

    public synchronized void observe(PlayerAction action) {
        if (!armed || captured != null || ExecutionOriginGuard.isInternal() || action == null)
            return;
        TargetSpec target = targetContext.get();
        if (target == null) target = TargetCodec.decode("hover");
        captured = new ActionStep(action.getId(), target, action.getName());
        armed = false;
    }

    public synchronized ActionStep poll() {
        ActionStep result = captured;
        captured = null;
        return result;
    }
}
