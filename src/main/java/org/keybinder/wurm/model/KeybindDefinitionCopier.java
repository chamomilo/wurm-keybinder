package org.keybinder.wurm.model;

import java.util.ArrayList;
import java.util.List;

/** The single deep-copy path for duplicate, merge and portable import. */
public final class KeybindDefinitionCopier {
    public KeybindRecord copyRecordWithNewIdentity(KeybindRecord source, String name) {
        List<KeybindVariant> variants = copyVariantsWithNewIdentity(source.getVariants());
        int activeIndex = activeIndex(source);
        KeybindRecord result = new KeybindRecord(null, name, source.getKey(), variants,
                variants.get(Math.min(activeIndex, variants.size() - 1)).getId());
        result.setHudMulti(source.isHudMulti());
        return result;
    }

    public List<KeybindVariant> copyVariantsWithNewIdentity(List<KeybindVariant> source) {
        List<KeybindVariant> result = new ArrayList<KeybindVariant>();
        for (KeybindVariant variant : source)
            result.add(new KeybindVariant(null, variant.getSubName(), copySteps(variant.getSteps())));
        return result;
    }

    public List<KeybindStep> copySteps(List<? extends KeybindStep> source) {
        List<KeybindStep> result = new ArrayList<KeybindStep>();
        for (KeybindStep step : source) result.add(copyStep(step));
        return result;
    }

    public KeybindStep copyStep(KeybindStep step) {
        if (step instanceof ActionStep) {
            ActionStep action = (ActionStep) step;
            return new ActionStep(action.getActionId(), ItemSelector.copyOf(action.getSource()),
                    TargetSpec.copyOf(action.getTarget()), action.getLastKnownName());
        }
        if (step instanceof ActivateToolStep)
            return new ActivateToolStep(TargetSpec.copyOf(((ActivateToolStep) step).getTarget()));
        if (step instanceof SmartImproveStep)
            return new SmartImproveStep(TargetSpec.copyOf(((SmartImproveStep) step).getTarget()));
        if (step instanceof VanillaActionStep)
            return new VanillaActionStep(((VanillaActionStep) step).getCommand());
        if (step instanceof ConsoleCommandStep) {
            ConsoleCommandStep command = (ConsoleCommandStep) step;
            return new ConsoleCommandStep(command.getCommand(), command.isPreserveExactText());
        }
        throw new IllegalArgumentException("Unsupported keybind step " + step.getClass().getName());
    }

    private static int activeIndex(KeybindRecord record) {
        for (int i = 0; i < record.getVariants().size(); i++)
            if (record.getVariants().get(i).getId().equals(record.getActiveVariantId())) return i;
        return 0;
    }
}
