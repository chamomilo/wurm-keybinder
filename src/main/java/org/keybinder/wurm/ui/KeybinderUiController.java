package org.keybinder.wurm.ui;

import org.keybinder.wurm.model.KeybindRecord;

import java.util.List;

public interface KeybinderUiController {
    int getQueueLimit();
    boolean isShowingActionIds();
    void toggleActionIds();
    List<KeybindRecord> getRecords();
    void addNewKeybind();
    void addNewKeybindAfter(String id);
    void moveKeybind(String id, List<String> visibleIds, int insertionIndex);
    void duplicateKeybind(String id);
    void requestMerge(String sourceId, String destinationId);
    void confirmMerge(String sourceId, String destinationId);
    void cancelMerge();
    void editKeybind(String id);
    void closeEditor();
    void deleteKeybind(String id);
    void setKeybindEnabled(String id, boolean enabled);
    void setKeybindsEnabled(List<String> ids, boolean enabled);
    void printAll();
    void toggleShadowRecording();
    boolean isShadowRecording();
    void requestImport();
    void requestImportFile();
    void requestExportAll();
    void confirmImport();
    void restoreOriginalBindings();
    boolean isLegacyActionInstalled();
    void disableLegacyAction();
    void startFromIntro();
    void importDisableAndRestart();
    boolean isSkipIntro();
    void setSkipIntro(boolean skip);
    String getLanguage();
    void setLanguage(String language);
    void requestToolbeltSelection();
    void requestEquipmentSelection();
    void selectTileTarget(String target);
    String getSelectedTarget();
    void openOriginalProject();
    void openImproveProject();
    void openInniriaImproveProject();
    void openMunstaImproveProject();
    void openFromTag();
    void windowClosed();
}
