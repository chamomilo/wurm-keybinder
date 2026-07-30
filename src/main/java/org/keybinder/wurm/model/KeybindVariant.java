package org.keybinder.wurm.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** One selectable action-chain definition inside a managed keybind. */
public final class KeybindVariant {
    private final String id;
    private String subName;
    private final List<KeybindStep> steps;

    public KeybindVariant(String id, String subName, List<? extends KeybindStep> steps) {
        this.id = id == null || id.trim().isEmpty() ? UUID.randomUUID().toString() : id;
        this.subName = subName == null ? "" : subName.trim();
        this.steps = new ArrayList<KeybindStep>(
                steps == null ? Collections.<KeybindStep>emptyList() : steps);
    }

    public String getId() { return id; }
    public String getSubName() { return subName; }
    public void setSubName(String value) { subName = value == null ? "" : value.trim(); }
    public List<KeybindStep> getSteps() { return Collections.unmodifiableList(steps); }
}
