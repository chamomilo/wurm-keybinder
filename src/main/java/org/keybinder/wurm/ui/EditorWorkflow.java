package org.keybinder.wurm.ui;

import com.wurmonline.client.console.WurmConsole;
import org.keybinder.wurm.KeybindRegistry;
import org.keybinder.wurm.event.EventLogger;
import org.keybinder.wurm.i18n.DisableReason;
import org.keybinder.wurm.i18n.Messages;
import org.keybinder.wurm.model.ConflictResolution;
import org.keybinder.wurm.model.KeybindConflict;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.model.KeybindVariant;
import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.model.BulkTransferStep;
import org.keybinder.wurm.validation.KeybindValidator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Owns pending editor/enable conflicts independently of HUD component lifetime. */
public final class EditorWorkflow {
    public interface Environment {
        WurmConsole console() throws ReflectiveOperationException;
        int queueLimit();
        void showConflict(KeybindConflict conflict);
        void closeConflict();
        void closeEditor();
        void refreshList();
        void enabledStateChanged(String id, boolean enabled);
        void extractedFrom(KeybindRecord parent);
    }

    private final KeybindRegistry registry;
    private final EventLogger log;
    private final Environment environment;
    private PendingSave pendingSave;
    private String pendingEnableId;
    private KeybindConflict pendingConflict;

    public EditorWorkflow(KeybindRegistry registry, EventLogger log,
                          Environment environment) {
        this.registry = registry;
        this.log = log;
        this.environment = environment;
    }

    public void clear() {
        pendingSave = null;
        pendingEnableId = null;
        pendingConflict = null;
    }

    public void setEnabled(String id, boolean enabled) {
        try {
            if (enabled) {
                KeybindConflict conflict = registry.findEnableConflict(id, environment.console());
                if (conflict != null) {
                    pendingEnableId = id;
                    pendingConflict = conflict;
                    environment.showConflict(conflict);
                    return;
                }
            }
            registry.setEnabled(id, enabled, environment.console(), environment.queueLimit());
            environment.enabledStateChanged(id, enabled);
            environment.refreshList();
        } catch (Exception failure) {
            log.error(Messages.text(enabled
                    ? "error.enable_keybind" : "error.disable_keybind"), failure);
            environment.refreshList();
        }
    }

    public boolean saveVariants(String id, String name, String key,
                                List<KeybindVariant> variants, String activeVariantId,
                                boolean hudMulti, String createdByUser,
                                String createdOnServer) {
        try {
            KeybindValidator.validatePendingVariants(name, key, variants);
            diagnosticBulk("validated for save", name, key, variants);
            PendingSave requested = PendingSave.variants(id, name, key, variants,
                    activeVariantId, hudMulti, createdByUser, createdOnServer);
            KeybindConflict conflict = registry.findKeybindSaveConflict(
                    id, key, environment.console());
            if (conflict != null) return awaitConflict(requested, conflict);
            return commit(requested);
        } catch (Exception failure) {
            log.error(Messages.text("error.save_keybind", safeMessage(failure)), failure);
            return false;
        }
    }

    public boolean extractVariant(String id, String name, String key,
                                  List<KeybindVariant> variants, String activeVariantId,
                                  boolean hudMulti, String extractedVariantId,
                                  String createdByUser, String createdOnServer) {
        try {
            KeybindValidator.validatePendingVariants(name, key, variants);
            if (variants == null || variants.size() <= 1)
                throw new IllegalArgumentException(Messages.text("extract.last_variant"));
            PendingSave requested = PendingSave.extract(id, name, key, variants,
                    activeVariantId, hudMulti, extractedVariantId,
                    createdByUser, createdOnServer);
            KeybindRecord source = registry.find(id);
            KeybindConflict conflict = source != null && source.isEnabled()
                    ? registry.findKeybindSaveConflict(id, key, environment.console()) : null;
            if (conflict != null) return awaitConflict(requested, conflict);
            return commit(requested);
        } catch (Exception failure) {
            log.error(Messages.text("error.extract_variant", safeMessage(failure)), failure);
            return false;
        }
    }

    private boolean awaitConflict(PendingSave requested, KeybindConflict conflict) {
        pendingSave = requested;
        pendingConflict = conflict;
        environment.showConflict(conflict);
        return false;
    }

    public void resolve(ConflictResolution resolution) {
        PendingSave requested = pendingSave;
        String enableId = pendingEnableId;
        KeybindConflict conflict = pendingConflict;
        clear();
        environment.closeConflict();
        if (enableId != null) {
            resolveEnable(enableId, resolution);
            return;
        }
        if (resolution == ConflictResolution.CANCEL || requested == null) {
            log.info(Messages.text(requested != null && requested.kind == SaveKind.EXTRACT
                    ? "event.extract_cancelled" : "event.save_cancelled"));
            return;
        }
        if (resolution == ConflictResolution.KEEP_OLD) {
            saveDisabled(requested, conflict);
            return;
        }
        commit(requested);
    }

    private void resolveEnable(String id, ConflictResolution resolution) {
        if (resolution == ConflictResolution.KEEP_NEW) {
            try {
                registry.setEnabled(id, true, environment.console(), environment.queueLimit());
                environment.enabledStateChanged(id, true);
                environment.refreshList();
            } catch (Exception failure) {
                log.error(Messages.text("error.enable_keybind"), failure);
            }
        } else {
            log.info(Messages.text(resolution == ConflictResolution.CANCEL
                    ? "event.enable_cancelled" : "event.existing_kept"));
            environment.refreshList();
        }
    }

    private void saveDisabled(PendingSave requested, KeybindConflict conflict) {
        try {
            String reason = conflict == null
                    ? DisableReason.value("key_used_unknown", requested.key)
                    : conflict.isVanillaOwner()
                    ? DisableReason.value("key_used_vanilla", requested.key)
                    : DisableReason.value("key_used", requested.key, conflict.getOwner());
            if (requested.kind == SaveKind.EXTRACT) {
                commitExtract(requested, reason);
                return;
            }
            registry.updateVariantsDisabled(requested.id, requested.name, requested.key,
                    requested.variants, requested.activeVariantId, requested.hudMulti, reason,
                    requested.createdByUser, requested.createdOnServer,
                    environment.console(), environment.queueLimit());
            environment.closeEditor();
            environment.refreshList();
        } catch (Exception failure) {
            log.error(Messages.text("error.save_disabled", safeMessage(failure)), failure);
        }
    }

    private boolean commit(PendingSave requested) {
        if (requested.kind == SaveKind.EXTRACT) return commitExtract(requested, null);
        try {
            diagnosticBulk("committing", requested.name, requested.key, requested.variants);
            registry.updateVariants(requested.id, requested.name, requested.key,
                    requested.variants, requested.activeVariantId, requested.hudMulti,
                    requested.createdByUser, requested.createdOnServer,
                    environment.console(), environment.queueLimit());
            environment.closeEditor();
            environment.refreshList();
            return true;
        } catch (Exception failure) {
            log.error(Messages.text("error.save_keybind", safeMessage(failure)), failure);
            return false;
        }
    }

    private boolean commitExtract(PendingSave requested, String parentDisabledReason) {
        try {
            KeybindRecord extracted = registry.extractVariant(requested.id, requested.name,
                    requested.key, requested.variants, requested.activeVariantId,
                    requested.hudMulti, requested.extractedVariantId,
                    requested.createdByUser, requested.createdOnServer,
                    parentDisabledReason, environment.console(), environment.queueLimit());
            KeybindRecord parent = registry.find(requested.id);
            environment.extractedFrom(parent);
            log.info(Messages.text("event.extract_complete", extracted.getName()));
            environment.closeEditor();
            environment.refreshList();
            return true;
        } catch (Exception failure) {
            log.error(Messages.text("error.extract_variant", safeMessage(failure)), failure);
            return false;
        }
    }

    private static String safeMessage(Throwable error) {
        return error.getMessage() == null
                ? error.getClass().getSimpleName() : error.getMessage();
    }

    private void diagnosticBulk(String phase, String name, String key,
                                List<KeybindVariant> variants) {
        if (variants == null) return;
        for (int variantIndex = 0; variantIndex < variants.size(); variantIndex++) {
            List<KeybindStep> steps = variants.get(variantIndex).getSteps();
            for (int stepIndex = 0; stepIndex < steps.size(); stepIndex++) {
                if (!(steps.get(stepIndex) instanceof BulkTransferStep)) continue;
                BulkTransferStep bulk = (BulkTransferStep) steps.get(stepIndex);
                log.diagnostic("bulk-transfer " + phase + ": record='" + name
                        + "', key='" + key + "', variant=" + variantIndex
                        + ", step=" + stepIndex + ", storageId="
                        + (bulk.getSource() == null || bulk.getSource().getStorage() == null
                        ? 0L : bulk.getSource().getStorage().getId())
                        + ", itemId="
                        + (bulk.getSource() == null || bulk.getSource().getItem() == null
                        ? 0L : bulk.getSource().getItem().getId())
                        + ", quantity=" + bulk.getQuantity()
                        + ", destinationKind=" + bulk.getDestinationKind()
                        + ", capturedDestinationId="
                        + (bulk.getCapturedDestination() == null
                        ? 0L : bulk.getCapturedDestination().getId()));
            }
        }
    }

    private enum SaveKind { VARIANTS, EXTRACT }

    private static final class PendingSave {
        private final String id;
        private final String name;
        private final String key;
        private final List<KeybindVariant> variants;
        private final String activeVariantId;
        private final String extractedVariantId;
        private final SaveKind kind;
        private final boolean hudMulti;
        private final String createdByUser;
        private final String createdOnServer;

        private PendingSave(String id, String name, String key,
                            List<KeybindVariant> variants, String activeVariantId,
                            String extractedVariantId, SaveKind kind, boolean hudMulti,
                            String createdByUser, String createdOnServer) {
            this.id = id;
            this.name = name;
            this.key = key;
            this.variants = Collections.unmodifiableList(
                    new ArrayList<KeybindVariant>(variants));
            this.activeVariantId = activeVariantId;
            this.extractedVariantId = extractedVariantId;
            this.kind = kind;
            this.hudMulti = hudMulti;
            this.createdByUser = createdByUser == null ? "" : createdByUser;
            this.createdOnServer = createdOnServer == null ? "" : createdOnServer;
        }

        static PendingSave variants(String id, String name, String key,
                                    List<KeybindVariant> variants, String activeVariantId,
                                    boolean hudMulti, String user, String server) {
            return new PendingSave(id, name, key, variants, activeVariantId, "",
                    SaveKind.VARIANTS, hudMulti, user, server);
        }

        static PendingSave extract(String id, String name, String key,
                                   List<KeybindVariant> variants, String activeVariantId,
                                   boolean hudMulti, String extractedVariantId,
                                   String user, String server) {
            return new PendingSave(id, name, key, variants, activeVariantId,
                    extractedVariantId, SaveKind.EXTRACT, hudMulti, user, server);
        }
    }
}
