package org.keybinder.wurm.ui;

import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.KeybindVariant;
import org.keybinder.wurm.model.ConflictResolution;
import org.keybinder.wurm.queue.QueueCost;

import java.util.List;

public interface KeybindEditorController {
    int getQueueLimit();
    QueueCost getKeybindCost(KeybindRecord record);
    KeybindRecord getRecord(String id);
    String getActionName(short actionId);
    boolean saveVariants(String id, String name, String key, List<KeybindVariant> variants,
                         String activeVariantId, boolean hudMulti,
                         String createdByUser, String createdOnServer);
    boolean extractVariant(String id, String name, String key, List<KeybindVariant> variants,
                           String activeVariantId, boolean hudMulti,
                           String extractedVariantId,
                           String createdByUser, String createdOnServer);
    String currentUser();
    String currentServer();
    void resolveKeybindConflict(ConflictResolution resolution);
    void showEditorError(String message);
    void beginCapture();
    void cancelCapture();
    ActionStep pollCapturedAction();
    void requestTargetSelection(String kind);
    void cancelTargetSelection();
    String consumeSelectedTarget();
    void closeEditor();
}
