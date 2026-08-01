package org.keybinder.wurm.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.keybinder.wurm.i18n.Messages;

public final class KeybindRecord {
    private final String id;
    private String name;
    private String key;
    private final List<KeybindVariant> variants;
    private String activeVariantId;
    private boolean enabled;
    private String disabledReason;
    private String originalKey;
    private String originalCommand;
    private String previousManagedCommand = "";
    private String createdByUser = "";
    private String createdOnServer = "";
    private boolean hudMulti;

    public KeybindRecord(String id, String name, String key, RecordType type,
                         List<ActionStep> steps, String command) {
        this.id = id == null || id.trim().isEmpty() ? UUID.randomUUID().toString() : id;
        this.name = name;
        this.key = key;
        List<KeybindStep> initial = new ArrayList<KeybindStep>();
        if (type == RecordType.ACTION_CHAIN) {
            initial.addAll(steps == null ? Collections.<ActionStep>emptyList() : steps);
        } else if (command != null && !command.isEmpty()) {
            initial.add(new ConsoleCommandStep(command, type == RecordType.RAW_VANILLA_COMMAND));
        }
        KeybindVariant variant = new KeybindVariant(null, "", initial);
        this.variants = new ArrayList<KeybindVariant>();
        this.variants.add(variant);
        this.activeVariantId = variant.getId();
        this.enabled = true;
        this.disabledReason = "";
        this.hudMulti = false;
    }

    public KeybindRecord(String id, String name, String key, List<? extends KeybindStep> steps) {
        this.id = id == null || id.trim().isEmpty() ? UUID.randomUUID().toString() : id;
        this.name = name;
        this.key = key;
        KeybindVariant variant = new KeybindVariant(null, "", steps);
        this.variants = new ArrayList<KeybindVariant>();
        this.variants.add(variant);
        this.activeVariantId = variant.getId();
        this.enabled = true;
        this.disabledReason = "";
        this.hudMulti = false;
    }

    public KeybindRecord(String id, String name, String key, List<KeybindVariant> variants,
                         String activeVariantId) {
        this.id = id == null || id.trim().isEmpty() ? UUID.randomUUID().toString() : id;
        this.name = name;
        this.key = key;
        this.variants = new ArrayList<KeybindVariant>(
                variants == null ? Collections.<KeybindVariant>emptyList() : variants);
        if (this.variants.isEmpty()) this.variants.add(new KeybindVariant(null, "", null));
        this.activeVariantId = findVariant(activeVariantId) == null
                ? this.variants.get(0).getId() : activeVariantId;
        this.enabled = true;
        this.disabledReason = "";
        this.hudMulti = false;
    }

    public static KeybindRecord actionChain(String name, String key, List<ActionStep> steps) {
        return new KeybindRecord(null, name, key, RecordType.ACTION_CHAIN, steps, "");
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    /** Action-only view for older editor code. Mixed keybinds must use getKeybindSteps(). */
    public List<ActionStep> getSteps() {
        List<ActionStep> result = new ArrayList<ActionStep>();
        for (KeybindStep step : getKeybindSteps()) if (step instanceof ActionStep) result.add((ActionStep) step);
        return Collections.unmodifiableList(result);
    }
    public List<KeybindStep> getKeybindSteps() {
        KeybindVariant active = getActiveVariant();
        return active == null ? Collections.<KeybindStep>emptyList() : active.getSteps();
    }
    public List<KeybindVariant> getVariants() { return Collections.unmodifiableList(variants); }
    public boolean isMultiPurpose() { return variants.size() > 1; }
    public boolean isHudMulti() { return isMultiPurpose() && hudMulti; }
    public void setHudMulti(boolean value) { hudMulti = value && isMultiPurpose(); }
    public String getActiveVariantId() { return activeVariantId; }
    public KeybindVariant getActiveVariant() {
        KeybindVariant found = findVariant(activeVariantId);
        return found == null && !variants.isEmpty() ? variants.get(0) : found;
    }
    public KeybindVariant findVariant(String variantId) {
        if (variantId != null)
            for (KeybindVariant variant : variants)
                if (variantId.equals(variant.getId())) return variant;
        return null;
    }
    public void setActiveVariantId(String variantId) {
        if (findVariant(variantId) == null)
            throw new IllegalArgumentException(Messages.text("validation.variant_unknown"));
        activeVariantId = variantId;
    }
    /** Read-only execution view used when a HUD choice must not become the default. */
    public KeybindRecord executionViewForVariant(String variantId) {
        if (findVariant(variantId) == null)
            throw new IllegalArgumentException(Messages.text("validation.variant_unknown"));
        KeybindRecord result = new KeybindRecord(id, name, key, variants, variantId);
        result.setEnabled(enabled);
        result.setDisabledReason(disabledReason);
        result.setOriginalKey(originalKey);
        result.setOriginalCommand(originalCommand);
        result.setPreviousManagedCommand(previousManagedCommand);
        result.setCreatedByUser(createdByUser);
        result.setCreatedOnServer(createdOnServer);
        result.setHudMulti(hudMulti);
        return result;
    }
    public String getDisplayName() {
        KeybindVariant active = getActiveVariant();
        String suffix = active == null ? "" : active.getSubName().trim();
        return suffix.isEmpty() ? name : name + "-" + suffix;
    }
    public RecordType getType() {
        List<KeybindStep> steps = getKeybindSteps();
        if (steps.size() == 1 && steps.get(0) instanceof VanillaActionStep)
            return RecordType.VANILLA_COMMAND;
        if (steps.size() == 1 && steps.get(0) instanceof ConsoleCommandStep)
            return ((ConsoleCommandStep) steps.get(0)).isPreserveExactText()
                    ? RecordType.RAW_VANILLA_COMMAND : RecordType.VANILLA_COMMAND;
        return RecordType.ACTION_CHAIN;
    }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getDisabledReason() { return disabledReason; }
    public void setDisabledReason(String disabledReason) { this.disabledReason = disabledReason == null ? "" : disabledReason; }
    public String getOriginalKey() { return originalKey; }
    public void setOriginalKey(String originalKey) { this.originalKey = originalKey; }
    public String getOriginalCommand() { return originalCommand; }
    public void setOriginalCommand(String originalCommand) { this.originalCommand = originalCommand; }
    public String getPreviousManagedCommand() { return previousManagedCommand; }
    public void setPreviousManagedCommand(String value) {
        this.previousManagedCommand = value == null ? "" : value;
    }
    public String getCreatedByUser() { return createdByUser; }
    public void setCreatedByUser(String value) { createdByUser = value == null ? "" : value; }
    public String getCreatedOnServer() { return createdOnServer; }
    public void setCreatedOnServer(String value) { createdOnServer = value == null ? "" : value; }
}
