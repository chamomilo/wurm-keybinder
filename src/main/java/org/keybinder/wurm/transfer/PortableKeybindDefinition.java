package org.keybinder.wurm.transfer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.ItemSelectorKind;
import org.keybinder.wurm.model.KeybindDefinitionCopier;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.model.KeybindVariant;
import org.keybinder.wurm.model.ActivateToolStep;
import org.keybinder.wurm.model.SmartImproveStep;
import org.keybinder.wurm.model.TargetKind;

/** Runtime-identity-free definition stored in a .keybinder bundle. */
public final class PortableKeybindDefinition {
    public static final class Variant {
        private final String name;
        private final List<KeybindStep> steps;

        public Variant(String name, List<? extends KeybindStep> steps) {
            this.name = name == null ? "" : name;
            this.steps = Collections.unmodifiableList(new ArrayList<KeybindStep>(steps));
        }

        public String getName() { return name; }
        public List<KeybindStep> getSteps() { return steps; }
    }

    private final String name;
    private final String intendedKey;
    private final boolean hudMulti;
    private final int activeVariantIndex;
    private final List<Variant> variants;

    public PortableKeybindDefinition(String name, String intendedKey, boolean hudMulti,
                                     int activeVariantIndex, List<Variant> variants) {
        this.name = name == null ? "" : name;
        this.intendedKey = intendedKey == null ? "" : intendedKey;
        this.variants = Collections.unmodifiableList(new ArrayList<Variant>(variants));
        this.activeVariantIndex = activeVariantIndex;
        this.hudMulti = hudMulti && variants.size() > 1;
    }

    public static PortableKeybindDefinition fromRecord(KeybindRecord record) {
        KeybindDefinitionCopier copier = new KeybindDefinitionCopier();
        List<Variant> variants = new ArrayList<Variant>();
        int active = 0;
        for (int i = 0; i < record.getVariants().size(); i++) {
            KeybindVariant variant = record.getVariants().get(i);
            if (variant.getId().equals(record.getActiveVariantId())) active = i;
            variants.add(new Variant(variant.getSubName(), copier.copySteps(variant.getSteps())));
        }
        return new PortableKeybindDefinition(record.getName(), record.getKey(),
                record.isHudMulti(), active, variants);
    }

    public KeybindRecord toRecord(String user, String server) {
        KeybindDefinitionCopier copier = new KeybindDefinitionCopier();
        List<KeybindVariant> runtimeVariants = new ArrayList<KeybindVariant>();
        for (Variant variant : variants)
            runtimeVariants.add(new KeybindVariant(null, variant.name,
                    copier.copySteps(variant.steps)));
        KeybindRecord record = new KeybindRecord(null, name, intendedKey, runtimeVariants,
                runtimeVariants.get(activeVariantIndex).getId());
        record.setHudMulti(hudMulti);
        record.setCreatedByUser(user);
        record.setCreatedOnServer(server);
        record.setOriginalKey("");
        record.setOriginalCommand("");
        record.setPreviousManagedCommand("");
        return record;
    }

    public boolean hasExactObject() {
        for (Variant variant : variants)
            for (KeybindStep step : variant.steps) {
                if (step instanceof ActionStep) {
                    ActionStep action = (ActionStep) step;
                    if (action.getSource().getKind() == ItemSelectorKind.EXACT_OBJECT
                            || action.getTarget().getKind() == TargetKind.EXACT_OBJECT) return true;
                } else if (step instanceof ActivateToolStep
                        && ((ActivateToolStep) step).getTarget().getKind()
                        == TargetKind.EXACT_OBJECT) return true;
                else if (step instanceof SmartImproveStep
                        && ((SmartImproveStep) step).getTarget().getKind()
                        == TargetKind.EXACT_OBJECT) return true;
            }
        return false;
    }

    public String getName() { return name; }
    public String getIntendedKey() { return intendedKey; }
    public boolean isHudMulti() { return hudMulti; }
    public int getActiveVariantIndex() { return activeVariantIndex; }
    public List<Variant> getVariants() { return variants; }
}
