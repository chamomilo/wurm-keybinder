package org.keybinder.wurm.model;

import org.keybinder.wurm.i18n.DisableReason;
import org.keybinder.wurm.i18n.Messages;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Pure construction and transformation of keybind definitions. */
public final class KeybindDefinitionService {
    private final KeybindDefinitionCopier copier = new KeybindDefinitionCopier();

    public KeybindRecord replaceSteps(KeybindRecord existing, String name, String key,
                                      List<? extends KeybindStep> steps,
                                      String createdByUser, String createdOnServer) {
        KeybindRecord replacement = new KeybindRecord(existing.getId(), name, key, steps);
        preserveOwnership(existing, replacement, createdByUser, createdOnServer);
        return replacement;
    }

    public KeybindRecord replaceVariants(KeybindRecord existing, String name, String key,
                                         List<KeybindVariant> variants, String activeVariantId,
                                         boolean hudMulti, String createdByUser,
                                         String createdOnServer) {
        KeybindRecord replacement = new KeybindRecord(existing.getId(), name, key,
                variants, activeVariantId);
        replacement.setHudMulti(hudMulti);
        preserveOwnership(existing, replacement, createdByUser, createdOnServer);
        return replacement;
    }

    public KeybindRecord duplicate(KeybindRecord source, String name,
                                   String currentUser, String currentServer) {
        KeybindRecord duplicate = copier.copyRecordWithNewIdentity(source, name);
        duplicate.setEnabled(false);
        duplicate.setDisabledReason(DisableReason.value("duplicate_review"));
        duplicate.setOriginalKey("");
        duplicate.setOriginalCommand("");
        duplicate.setPreviousManagedCommand("");
        duplicate.setCreatedByUser(currentUser);
        duplicate.setCreatedOnServer(currentServer);
        duplicate.setValuePack(false);
        return duplicate;
    }

    public KeybindRecord merge(KeybindRecord source, KeybindRecord destination) {
        List<KeybindVariant> variants = new ArrayList<KeybindVariant>(
                destination.getVariants());
        String sourceBaseName = KeybindNamePrefixes.baseName(source.getName());
        Set<String> names = new HashSet<String>();
        for (KeybindVariant variant : variants)
            names.add(variant.getSubName().toLowerCase(Locale.ENGLISH));
        for (KeybindVariant copied : copier.copyVariantsWithNewIdentity(source.getVariants())) {
            String base = copied.getSubName().trim().isEmpty() ? sourceBaseName
                    : sourceBaseName + " — " + copied.getSubName();
            String candidate = base;
            int suffix = 2;
            while (names.contains(candidate.toLowerCase(Locale.ENGLISH)))
                candidate = base + Messages.text("merge.name_suffix", suffix++);
            copied.setSubName(candidate);
            names.add(candidate.toLowerCase(Locale.ENGLISH));
            variants.add(copied);
        }
        KeybindRecord merged = new KeybindRecord(destination.getId(), destination.getName(),
                destination.getKey(), variants, destination.getActiveVariantId());
        merged.setHudMulti(destination.isHudMulti());
        normalizeName(merged);
        merged.setEnabled(destination.isEnabled());
        merged.setDisabledReason(destination.getDisabledReason());
        preserveOwnership(destination, merged, destination.getCreatedByUser(),
                destination.getCreatedOnServer());
        merged.setValuePack(source.isValuePack() || destination.isValuePack());
        return merged;
    }

    public Extraction extract(KeybindRecord source, String name, String key,
                              List<KeybindVariant> variants, String activeVariantId,
                              boolean hudMulti, String extractedVariantId,
                              String createdByUser, String createdOnServer,
                              String currentUser, String currentServer,
                              String parentDisabledReason) {
        if (variants == null || variants.size() <= 1)
            throw new IllegalArgumentException(Messages.text("extract.last_variant"));

        KeybindVariant extractedVariant = null;
        int extractedIndex = -1;
        List<KeybindVariant> remaining = new ArrayList<KeybindVariant>();
        for (int i = 0; i < variants.size(); i++) {
            KeybindVariant variant = variants.get(i);
            if (variant.getId().equals(extractedVariantId)) {
                extractedVariant = variant;
                extractedIndex = i;
            } else {
                remaining.add(variant);
            }
        }
        if (extractedVariant == null)
            throw new IllegalArgumentException(Messages.text("validation.variant_unknown"));
        if (extractedIndex == 0)
            throw new IllegalArgumentException(Messages.text("extract.default_variant"));

        String remainingActive = activeVariantId;
        boolean activeRemains = false;
        for (KeybindVariant variant : remaining)
            if (variant.getId().equals(remainingActive)) activeRemains = true;
        if (!activeRemains) remainingActive = remaining.get(0).getId();

        KeybindRecord parent = new KeybindRecord(source.getId(), name, key,
                remaining, remainingActive);
        parent.setHudMulti(hudMulti);
        normalizeName(parent);
        preserveOwnership(source, parent, createdByUser, createdOnServer);
        parent.setEnabled(source.isEnabled());
        parent.setDisabledReason(source.getDisabledReason());
        if (parentDisabledReason != null && !parentDisabledReason.trim().isEmpty()) {
            parent.setEnabled(false);
            parent.setDisabledReason(parentDisabledReason);
        }

        String variantName = extractedVariant.getSubName().trim();
        if (variantName.isEmpty())
            variantName = Messages.text("editor.variant.number", extractedIndex);
        List<KeybindVariant> copied = copier.copyVariantsWithNewIdentity(
                Collections.singletonList(extractedVariant));
        KeybindRecord extracted = new KeybindRecord(null,
                Messages.text("keybind.extracted_name", variantName,
                        KeybindNamePrefixes.baseName(parent.getName())), key,
                copied, copied.get(0).getId());
        extracted.setEnabled(false);
        extracted.setDisabledReason(DisableReason.value("extracted_review", key));
        extracted.setOriginalKey("");
        extracted.setOriginalCommand("");
        extracted.setPreviousManagedCommand("");
        extracted.setCreatedByUser(currentUser);
        extracted.setCreatedOnServer(currentServer);
        normalizeName(extracted);
        return new Extraction(parent, extracted, variantName);
    }

    private static void preserveOwnership(KeybindRecord source, KeybindRecord target,
                                          String createdByUser, String createdOnServer) {
        target.setOriginalKey(source.getOriginalKey());
        target.setOriginalCommand(source.getOriginalCommand());
        target.setPreviousManagedCommand(source.getPreviousManagedCommand());
        target.setCreatedByUser(createdByUser);
        target.setCreatedOnServer(createdOnServer);
        target.setValuePack(source.isValuePack());
    }

    private static void normalizeName(KeybindRecord record) {
        record.setName(KeybindNamePrefixes.apply(record.getName(),
                record.getVariants().size(), record.isHudMulti()));
    }

    public static final class Extraction {
        private final KeybindRecord parent;
        private final KeybindRecord extracted;
        private final String variantName;

        private Extraction(KeybindRecord parent, KeybindRecord extracted, String variantName) {
            this.parent = parent;
            this.extracted = extracted;
            this.variantName = variantName;
        }

        public KeybindRecord getParent() { return parent; }
        public KeybindRecord getExtracted() { return extracted; }
        public String getVariantName() { return variantName; }
    }
}
