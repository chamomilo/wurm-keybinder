package org.keybinder.wurm.ui;

import org.keybinder.wurm.model.KeybindRecord;

import java.util.List;

/** Operations used by the persistent Keybinder list/intro window. */
public interface KeybinderWindowController {
    int getQueueLimit();
    List<KeybindRecord> getRecords();
    void addNewKeybind();
    void addNewKeybindAfter(String id);
    void moveKeybind(String id, List<String> visibleIds, int insertionIndex);
    void duplicateKeybind(String id);
    void requestMerge(String sourceId, String destinationId);
    void editKeybind(String id);
    void deleteKeybind(String id);
    void setKeybindEnabled(String id, boolean enabled);
    void setKeybindsEnabled(List<String> ids, boolean enabled);
    void requestImport();
    void requestImportFile();
    void requestExportAll();
    void restoreOriginalBindings();
    boolean isLegacyActionInstalled();
    void startFromIntro();
    void importDisableAndRestart();
    boolean isSkipIntro();
    void setSkipIntro(boolean skip);
    String getLanguage();
    void setLanguage(String language);
    QueueMonitorSide getQueueMonitorSide();
    void setQueueMonitorSide(QueueMonitorSide side);
    void openOriginalProject();
    void openImproveProject();
    void openInniriaImproveProject();
    void openMunstaImproveProject();
    void windowClosed();
}
