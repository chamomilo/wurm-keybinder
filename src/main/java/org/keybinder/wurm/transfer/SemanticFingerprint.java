package org.keybinder.wurm.transfer;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.keybinder.wurm.command.ItemSelectorCodec;
import org.keybinder.wurm.command.TargetCodec;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.ActivateToolStep;
import org.keybinder.wurm.model.ConsoleCommandStep;
import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.model.SmartImproveStep;
import org.keybinder.wurm.model.VanillaActionStep;

/** Deterministic fingerprint of portable semantics, excluding runtime identity. */
public final class SemanticFingerprint {
    private SemanticFingerprint() { }

    public static String of(PortableKeybindDefinition definition) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            add(digest, definition.getName());
            add(digest, definition.getIntendedKey());
            add(digest, Boolean.toString(definition.isHudMulti()));
            add(digest, Integer.toString(definition.getActiveVariantIndex()));
            add(digest, Integer.toString(definition.getVariants().size()));
            for (PortableKeybindDefinition.Variant variant : definition.getVariants()) {
                add(digest, variant.getName());
                add(digest, Integer.toString(variant.getSteps().size()));
                for (KeybindStep step : variant.getSteps()) addStep(digest, step);
            }
            byte[] bytes = digest.digest();
            StringBuilder result = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) result.append(String.format("%02x", value & 0xff));
            return result.toString();
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static void addStep(MessageDigest digest, KeybindStep step) {
        add(digest, step.getKind().name());
        if (step instanceof ActionStep) {
            ActionStep action = (ActionStep) step;
            add(digest, Short.toString(action.getActionId()));
            add(digest, action.getLastKnownName());
            add(digest, ItemSelectorCodec.encode(action.getSource()));
            add(digest, TargetCodec.encode(action.getTarget()));
        } else if (step instanceof ActivateToolStep) {
            add(digest, TargetCodec.encode(((ActivateToolStep) step).getTarget()));
        } else if (step instanceof SmartImproveStep) {
            add(digest, TargetCodec.encode(((SmartImproveStep) step).getTarget()));
        } else if (step instanceof VanillaActionStep) {
            add(digest, ((VanillaActionStep) step).getCommand());
        } else if (step instanceof ConsoleCommandStep) {
            ConsoleCommandStep command = (ConsoleCommandStep) step;
            add(digest, command.getCommand());
            add(digest, Boolean.toString(command.isPreserveExactText()));
        }
    }

    private static void add(MessageDigest digest, String value) {
        byte[] bytes = (value == null ? "" : value).getBytes(StandardCharsets.UTF_8);
        digest.update((byte) (bytes.length >>> 24));
        digest.update((byte) (bytes.length >>> 16));
        digest.update((byte) (bytes.length >>> 8));
        digest.update((byte) bytes.length);
        digest.update(bytes);
    }
}
