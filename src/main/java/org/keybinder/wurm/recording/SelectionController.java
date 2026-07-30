package org.keybinder.wurm.recording;

import org.keybinder.wurm.command.ExactObjectTarget;
import org.keybinder.wurm.command.NearbyTypeTarget;
import org.keybinder.wurm.event.EventLogger;

public final class SelectionController {
    public enum Mode { NONE, TOOLBELT, EQUIPMENT, EXACT_OBJECT, NEARBY_TYPE }
    private final EventLogger log;
    private volatile Mode mode = Mode.NONE;
    private volatile String selectedTarget = "tile";
    private volatile boolean selectionComplete;

    public SelectionController(EventLogger log) {
        this.log = log;
    }

    public void requestToolbelt() {
        mode = Mode.TOOLBELT;
        selectionComplete = false;
        log.info("Select a toolbelt slot.");
    }

    public void requestEquipment() {
        mode = Mode.EQUIPMENT;
        selectionComplete = false;
        log.info("Select an equipment slot.");
    }

    public void requestExactObject() {
        mode = Mode.EXACT_OBJECT;
        selectionComplete = false;
        log.info("Select target object.");
    }

    public void requestNearbyType() {
        mode = Mode.NEARBY_TYPE;
        selectionComplete = false;
        log.info("Select any object nearby. Keybinder will remember type of this object and "
                + "will be able to select it automatically as a nearby target (i.e. any stump).");
    }

    public boolean acceptToolbelt(int zeroBasedSlot) {
        if (mode != Mode.TOOLBELT || zeroBasedSlot < 0 || zeroBasedSlot >= 10) return false;
        selectedTarget = "@tb" + (zeroBasedSlot + 1);
        selectionComplete = true;
        mode = Mode.NONE;
        log.info("Selected target " + selectedTarget + ".");
        return true;
    }

    public boolean acceptEquipment(byte slot) {
        if (mode != Mode.EQUIPMENT || slot < 0) return false;
        selectedTarget = "@eq" + slot;
        selectionComplete = true;
        mode = Mode.NONE;
        log.info("Selected target " + selectedTarget + ".");
        return true;
    }

    public boolean acceptExactObject(long id, String name) {
        if (mode != Mode.EXACT_OBJECT) return false;
        selectedTarget = ExactObjectTarget.encode(id, name);
        selectionComplete = true;
        mode = Mode.NONE;
        log.info("Selected exact object " + ExactObjectTarget.display(selectedTarget) + ".");
        return true;
    }

    public boolean acceptNearbyType(String name) {
        if (mode != Mode.NEARBY_TYPE) return false;
        selectedTarget = NearbyTypeTarget.encode(name);
        selectionComplete = true;
        mode = Mode.NONE;
        log.info("Selected target " + selectedTarget + ".");
        return true;
    }

    public void selectTile(String target) {
        selectedTarget = target;
        selectionComplete = true;
        mode = Mode.NONE;
        log.info("Selected target " + selectedTarget + ".");
    }

    public String getSelectedTarget() { return selectedTarget; }
    public String consumeSelectedTarget() {
        if (!selectionComplete) return null;
        selectionComplete = false;
        return selectedTarget;
    }
    public Mode getMode() { return mode; }
    public void cancel() { mode = Mode.NONE; selectionComplete = false; }
}
