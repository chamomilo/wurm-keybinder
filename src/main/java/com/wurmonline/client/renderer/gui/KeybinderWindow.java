package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.backend.Queue;
import org.keybinder.wurm.KeybinderMod;
import org.keybinder.wurm.i18n.Language;
import org.keybinder.wurm.i18n.Messages;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.model.KeybindNamePrefixes;
import org.keybinder.wurm.ui.KeybinderWindowController;
import org.keybinder.wurm.ui.KeybindListViewModel;
import org.keybinder.wurm.ui.LocalizedLayout;
import org.keybinder.wurm.ui.QueueMonitorSide;
import org.keybinder.wurm.ui.RowInsertionCalculator;
import org.keybinder.wurm.ui.DropZoneClassifier;
import org.keybinder.wurm.model.KeybindLimits;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class KeybinderWindow extends KeybinderUiWindow implements ButtonListener {
    private static final int WINDOW_CHROME = 38;
    private static final int CONTROLS_WIDTH = 41;
    private static final int CHECK_WIDTH = 34;
    private static final int KEY_WIDTH = 90;
    private static final int EDIT_MIN_WIDTH = 58;
    private static final int DUPLICATE_MIN_WIDTH = 82;
    private static final int COLUMN_GAP = 8;
    private static final int DEFAULT_HEIGHT = 285;
    private static final String FILTER_ALL = KeybindListViewModel.FILTER_ALL;
    private final KeybinderWindowController controller;
    private final WurmArrayPanel<FlexComponent> table;
    private final WurmScrollPanel listScroll;
    private final WurmBorderPanel root;
    private final WurmArrayPanel<FlexComponent> listTop;
    private final WurmArrayPanel<FlexComponent> filterControls;
    private final List<TableRow> tableRows = new ArrayList<>();
    private final WButton instructionButton;
    private KeybinderUiDropDown languageSelector;
    private int previousLanguage;
    private final WButton addButton;
    private final WButton importButton;
    private final WButton importFileButton;
    private final WButton exportAllButton;
    private final WButton restoreButton;
    private int displayedQueueLimit = -1;
    private KeybinderUiDropDown queueMonitorSide;
    private int previousQueueMonitorSide;
    private WButton enableFiltered;
    private WButton disableFiltered;
    private KeybinderUiDropDown userFilter;
    private KeybinderUiDropDown serverFilter;
    private String[] userFilterOptions = {Messages.text("list.all_users")};
    private String[] serverFilterOptions = {Messages.text("list.all_servers")};
    private String[] userFilterValues = {FILTER_ALL};
    private String[] serverFilterValues = {FILTER_ALL};
    private int lastUserFilterValue;
    private int lastServerFilterValue;
    private Mode mode = Mode.LIST;
    private KeybinderEditorWindow editor;
    private final Map<KeybinderUiCheckBox, String> checkboxIds = new HashMap<>();
    private final Map<KeybinderUiCheckBox, Boolean> checkboxStates = new HashMap<>();
    private final Map<WButton, RowAction> rowActions = new HashMap<>();
    private final Map<WButton, String> editIds = new HashMap<>();
    private final Map<WButton, String> duplicateIds = new HashMap<>();
    private int requiredNameWidth;
    private int requiredKeyWidth;
    private int requiredUserWidth;
    private int requiredServerWidth;
    private int requiredEditWidth;
    private int requiredDuplicateWidth;
    private int requiredFilterWidth;
    private int minimumWidth;
    private int minimumHeight = DEFAULT_HEIGHT;
    private int lastLayoutWidth = -1;
    private String selectedRowId;
    private String draggedRowId;
    private int dragPressY;
    private int dragInsertion = -1;
    private boolean rowDragging;
    private String dragMergeDestinationId;
    private boolean dragMergeOverLimit;
    private final KeybinderDragIndicator.InsertionGap rowInsertionGap =
            new KeybinderDragIndicator.InsertionGap("keybinder.drag.keybind.gap", 0);

    public KeybinderWindow(KeybinderWindowController controller) {
        super("keybinder.window", true);
        this.controller = controller;
        setTitle(Messages.text("window.title"));
        table = createScrollableTable();
        listScroll = new KeybinderUiScrollPanel("keybinder.scroll", table);
        instructionButton = new KeybinderUiButton(Messages.text("help.read"), this);
        Language language = Language.fromCode(controller.getLanguage());
        languageSelector = new KeybinderUiDropDown("keybinder.header.language", language.ordinal(), Language.displayNames());
        languageSelector.parent = this;
        previousLanguage = language.ordinal();
        addButton = new KeybinderUiButton(Messages.text("list.add"), this);
        importButton = new KeybinderUiButton(Messages.text("list.import"), this);
        importFileButton = new KeybinderUiButton(Messages.text("list.import_file"), this);
        exportAllButton = new KeybinderUiButton(Messages.text("list.export_all"), this);
        importButton.setHoverString(Messages.text("list.import.tip"));
        importFileButton.setHoverString(Messages.text("list.import_file.tip"));
        exportAllButton.setHoverString(Messages.text("list.export_all.tip"));
        restoreButton = new KeybinderUiButton(Messages.text("list.restore_originals"), this);
        KeybinderUi.confirm(restoreButton, Messages.text("list.restore_originals.question"),
                Messages.text("list.restore_originals.confirm"));
        root = new WurmBorderPanel("keybinder.root");
        listTop =
                new KeybinderUiArrayPanel<>("keybinder.list.introduction", WurmArrayPanel.DIR_VERTICAL, true);
        filterControls = new KeybinderUiArrayPanel<>(
                "keybinder.list.filters", WurmArrayPanel.DIR_HORIZONTAL);
        filterControls.componentWidthOffset = COLUMN_GAP;
        rebuildListTop();
        root.setComponent(listTop, WurmBorderPanel.NORTH);
        root.setComponent(listScroll, WurmBorderPanel.CENTER);
        refresh();
        setComponent(root);
        setInitialSize(Math.max(minimumWidth, 420), DEFAULT_HEIGHT, true);
    }

    private static WurmArrayPanel<FlexComponent> createScrollableTable() {
        // WurmArrayPanel's auto-width pass transiently reports height 0 while
        // WurmScrollPanel moves its content. The scroll panel then clamps yo
        // back to 0 inside the same wheel event. Column widths are already
        // applied explicitly by applyTableLayout(), so auto-width is neither
        // necessary nor safe for this scroll content.
        return new KeybinderUiArrayPanel<>("keybinder.table", WurmArrayPanel.DIR_VERTICAL);
    }

    public void showKeybinds() {
        mode = Mode.LIST;
        editor = null;
        resizable = true;
        minimumHeight = DEFAULT_HEIGHT;
        setTitle(Messages.text("window.title"));
        rebuildListTop();
        setComponent(root);
        refresh();
        if (width < minimumWidth) setSize(minimumWidth, height);
    }

    public void showEditor(KeybinderEditorWindow newEditor) {
        mode = Mode.EDITOR;
        editor = newEditor;
        resizable = true;
        minimumWidth = KeybinderEditorWindow.MIN_WIDTH;
        minimumHeight = 430;
        setTitle(Messages.text("window.editor.title"));
        setComponent(newEditor.getEmbeddedContent());
        if (width < minimumWidth) setSize(minimumWidth, Math.max(height, 430));
        newEditor.embeddedTick(width, height);
    }

    public boolean isEditorOpen() { return mode == Mode.EDITOR; }

    public void relocalize() {
        if (mode == Mode.EDITOR) return;
        setTitle(Messages.text("window.title"));
        addButton.setLabel(Messages.text("list.add"));
        importButton.setLabel(Messages.text("list.import"));
        importFileButton.setLabel(Messages.text("list.import_file"));
        exportAllButton.setLabel(Messages.text("list.export_all"));
        restoreButton.setLabel(Messages.text("list.restore_originals"));
        instructionButton.setLabel(Messages.text("help.read"));
        previousLanguage = Language.fromCode(controller.getLanguage()).ordinal();
        languageSelector = new KeybinderUiDropDown("keybinder.header.language", previousLanguage, Language.displayNames());
        languageSelector.parent = this;
        rebuildListTop();
        refresh();
        if (width < minimumWidth) setSize(minimumWidth, height);
    }

    private void rebuildListTop() {
        listTop.removeAllComponents();
        listTop.addComponent(instructionButton);
        listTop.addComponent(spacer(5));
        listTop.addComponent(new KeybinderUiLabel(Messages.text("list.heading")));
        WurmArrayPanel<FlexComponent> runtimeControls =
                new KeybinderUiArrayPanel<>("keybinder.list.runtime", WurmArrayPanel.DIR_HORIZONTAL);
        runtimeControls.componentWidthOffset = COLUMN_GAP;
        displayedQueueLimit = controller.getQueueLimit();
        runtimeControls.addComponent(new KeybinderUiLabel(
                Messages.text("list.queue_limit", displayedQueueLimit)));
        runtimeControls.addComponent(new KeybinderUiLabel(
                Messages.text("queue.monitor.side.label")));
        QueueMonitorSide selectedSide = controller.getQueueMonitorSide();
        queueMonitorSide = new KeybinderUiDropDown("keybinder.queue.monitor.side",
                selectedSide.ordinal(), new String[]{
                Messages.text("queue.monitor.side.right"),
                Messages.text("queue.monitor.side.left")});
        previousQueueMonitorSide = selectedSide.ordinal();
        runtimeControls.addComponent(queueMonitorSide);
        listTop.addComponent(runtimeControls);
        listTop.addComponent(spacer(5));
        listTop.addComponent(filterControls);
        listTop.addComponent(spacer(5));
        WurmArrayPanel<FlexComponent> migrationControls =
                new KeybinderUiArrayPanel<>("keybinder.list.migration", WurmArrayPanel.DIR_HORIZONTAL);
        migrationControls.componentWidthOffset = COLUMN_GAP;
        migrationControls.addComponent(addButton);
        migrationControls.addComponent(importButton);
        migrationControls.addComponent(importFileButton);
        migrationControls.addComponent(exportAllButton);
        migrationControls.addComponent(restoreButton);
        listTop.addComponent(migrationControls);
        listTop.addComponent(spacer(5));
        listTop.componentResized();
    }

    private static FlexComponent spacer(int height) {
        WurmLabel spacer = new KeybinderUiLabel("");
        spacer.setSize(1, height);
        return spacer;
    }

    public void refresh() {
        String selectedUser = selectedOption(userFilterValues, userFilter, FILTER_ALL);
        String selectedServer = selectedOption(serverFilterValues, serverFilter, FILTER_ALL);
        rebuildFilters(selectedUser, selectedServer);
        rebuildTable();
    }

    private void rebuildTable() {
        table.removeAllComponents();
        rowInsertionGap.setIndicatorVisible(true);
        tableRows.clear();
        checkboxIds.clear();
        checkboxStates.clear();
        rowActions.clear();
        editIds.clear();
        duplicateIds.clear();
        requiredNameWidth = 0;
        requiredKeyWidth = KEY_WIDTH;
        requiredUserWidth = 90;
        requiredServerWidth = 100;
        requiredEditWidth = localizedButtonColumnWidth("common.edit", EDIT_MIN_WIDTH);
        requiredDuplicateWidth = localizedButtonColumnWidth("common.duplicate", DUPLICATE_MIN_WIDTH);
        table.addComponent(header());
        for (KeybindRecord record : filteredRecords()) {
            table.addComponent(row(record));
            table.addComponent(spacer(2));
        }
        minimumWidth = WINDOW_CHROME + CONTROLS_WIDTH + CHECK_WIDTH
                + requiredEditWidth + requiredDuplicateWidth
                + requiredNameWidth + requiredKeyWidth + requiredUserWidth + requiredServerWidth
                + COLUMN_GAP * 7;
        minimumWidth = Math.max(Math.max(360, minimumWidth),
                WINDOW_CHROME + Math.max(requiredFilterWidth, LocalizedLayout.horizontalRowWidth(COLUMN_GAP,
                        addButton.width, importButton.width, importFileButton.width, exportAllButton.width, restoreButton.width)));
        lastLayoutWidth = -1;
        table.componentResized();
        applyTableLayout();
    }

    private FlexComponent header() {
        return createTableRow("keybinder.header", KeybinderUiLabel.header(""),
                KeybinderUiLabel.header(Messages.text("list.on")),
                KeybinderUiLabel.header(Messages.text("list.name")), KeybinderUiLabel.header(Messages.text("list.key")),
                KeybinderUiLabel.header(Messages.text("list.user")), KeybinderUiLabel.header(Messages.text("list.server")),
                KeybinderUiLabel.header(""), KeybinderUiLabel.header(""));
    }

    private FlexComponent row(KeybindRecord record) {
        KeybinderUiCheckBox enabled = new KeybinderUiCheckBox("");
        enabled.checked = record.isEnabled();
        checkboxIds.put(enabled, record.getId());
        checkboxStates.put(enabled, record.isEnabled());
        WurmLabel name = new ModeNameLabel(record);
        WurmLabel key = new SelectableLabel(
                org.keybinder.wurm.catalog.InputKeyCatalog.displayChord(record.getKey()),
                record.getId());
        WurmLabel createdBy = new SelectableLabel(
                KeybindListViewModel.displayOrigin(record.getCreatedByUser()), record.getId());
        WurmLabel createdOn = new SelectableLabel(
                KeybindListViewModel.displayOrigin(record.getCreatedOnServer()), record.getId());

        WButton edit = new KeybinderUiButton(Messages.text("common.edit"), this);
        edit.setHoverString(Messages.text("keybind.edit.tip"));
        editIds.put(edit, record.getId());

        WButton duplicate = new KeybinderUiButton(Messages.text("common.duplicate"), this);
        duplicate.setHoverString(Messages.text("keybind.duplicate.tip"));
        duplicateIds.put(duplicate, record.getId());

        WurmArrayPanel<FlexComponent> controls =
                new KeybinderUiArrayPanel<>("keybinder.controls." + record.getId(), WurmArrayPanel.DIR_HORIZONTAL);
        controls.componentWidthOffset = 1;
        controls.addComponent(rowButton(KeybinderGlyphButton.Kind.PLUS_BOX,
                RowAction.ADD_AFTER, record.getId()));
        controls.addComponent(rowButton(KeybinderGlyphButton.Kind.CLOSE_BOX,
                RowAction.REMOVE, record.getId()));
        KeybindListViewModel.Status status = KeybindListViewModel.status(
                record, controller.getRecords(), controller.getQueueLimit());
        FlexComponent row = createTableRow("keybinder.row." + record.getId(), record.getId(),
                status.isError(), status.isWarning() && record.isEnabled(),
                record.isValuePack(), controls, enabled, name, key,
                createdBy, createdOn, edit, duplicate);
        if (record.isValuePack()) {
            String valuePackTip = Messages.text("keybind.value_pack.tip");
            enabled.setHoverString(status.isError() || status.isWarning()
                    ? valuePackTip + " " + status.getHoverText() : valuePackTip);
        } else if (status.isError() || status.isWarning()) {
            enabled.setHoverString(status.getHoverText());
        }
        if (status.isError() || status.isWarning()) {
            edit.setHoverString(status.getHoverText());
            duplicate.setHoverString(Messages.text("keybind.duplicate.tip"));
        }
        return row;
    }

    private WButton rowButton(KeybinderGlyphButton.Kind kind, int action, String recordId) {
        String tooltip = action == RowAction.ADD_AFTER
                ? Messages.text("keybind.add_below") : Messages.text("keybind.delete.tip");
        WButton button = new KeybinderGlyphButton(kind, this, tooltip);
        rowActions.put(button, new RowAction(action, recordId));
        if (action == RowAction.REMOVE) {
            KeybinderUi.confirm(button, Messages.text("keybind.delete.question"),
                    Messages.text("keybind.delete.this"));
        }
        return button;
    }

    private FlexComponent createTableRow(String id, FlexComponent controls, FlexComponent enabled,
                                         FlexComponent name, FlexComponent key,
                                         FlexComponent user, FlexComponent server,
                                         FlexComponent edit, FlexComponent duplicate) {
        return createTableRow(id, null, false, false, false, controls, enabled, name, key,
                user, server, edit, duplicate);
    }

    private FlexComponent createTableRow(String id, String recordId, FlexComponent controls,
                                         FlexComponent enabled, FlexComponent name,
                                         FlexComponent key,
                                         FlexComponent user, FlexComponent server,
                                         FlexComponent edit, FlexComponent duplicate) {
        return createTableRow(id, recordId, false, false, false, controls, enabled, name, key,
                user, server, edit, duplicate);
    }

    private FlexComponent createTableRow(String id, String recordId, boolean statusError,
                                         boolean queueWarning,
                                         boolean valuePack,
                                         FlexComponent controls, FlexComponent enabled,
                                         FlexComponent name, FlexComponent key,
                                         FlexComponent user, FlexComponent server,
                                         FlexComponent edit, FlexComponent duplicate) {
        requiredNameWidth = Math.max(requiredNameWidth, name.width);
        requiredKeyWidth = Math.max(requiredKeyWidth, key.width);
        requiredUserWidth = Math.max(requiredUserWidth, user.width);
        requiredServerWidth = Math.max(requiredServerWidth, server.width);
        SelectableRow panel = new SelectableRow(
                id, recordId, statusError, queueWarning, valuePack);
        panel.componentWidthOffset = COLUMN_GAP;
        panel.addComponent(controls);
        panel.addComponent(enabled);
        panel.addComponent(name);
        panel.addComponent(key);
        panel.addComponent(user);
        panel.addComponent(server);
        panel.addComponent(edit);
        panel.addComponent(duplicate);
        tableRows.add(new TableRow(recordId, panel, controls, enabled, name, key,
                user, server, edit, duplicate));
        return panel;
    }

    private void rebuildFilters(String selectedUser, String selectedServer) {
        KeybindListViewModel.OriginOptions users = KeybindListViewModel.originOptions(
                controller.getRecords(), true,
                Messages.text("list.all_users"), Messages.text("list.unknown_user"));
        KeybindListViewModel.OriginOptions servers = KeybindListViewModel.originOptions(
                controller.getRecords(), false,
                Messages.text("list.all_servers"), Messages.text("list.unknown_server"));
        userFilterOptions = users.getLabels();
        userFilterValues = users.getValues();
        serverFilterOptions = servers.getLabels();
        serverFilterValues = servers.getValues();
        userFilter = new KeybinderUiDropDown("keybinder.filter.user",
                optionFor(userFilterValues, selectedUser), userFilterOptions);
        serverFilter = new KeybinderUiDropDown("keybinder.filter.server",
                optionFor(serverFilterValues, selectedServer), serverFilterOptions);
        lastUserFilterValue = userFilter.getValue();
        lastServerFilterValue = serverFilter.getValue();
        enableFiltered = new KeybinderUiButton(Messages.text("list.enable_filtered"), this);
        disableFiltered = new KeybinderUiButton(Messages.text("list.disable_filtered"), this);
        enableFiltered.setHoverString(Messages.text("list.enable_filtered.tip"));
        disableFiltered.setHoverString(Messages.text("list.disable_filtered.tip"));
        WurmLabel userLabel = new KeybinderUiLabel(Messages.text("common.user"));
        WurmLabel serverLabel = new KeybinderUiLabel(Messages.text("common.server"));
        // WurmArrayPanel's runtime width is mutable and may retain a prior
        // layout width after its children are rebuilt. Never feed that width
        // back into the window minimum or every refresh can grow the window.
        requiredFilterWidth = LocalizedLayout.horizontalRowWidth(COLUMN_GAP,
                userLabel.width, userFilter.width, serverLabel.width, serverFilter.width,
                enableFiltered.width, disableFiltered.width);
        filterControls.removeAllComponents();
        filterControls.addComponent(userLabel);
        filterControls.addComponent(userFilter);
        filterControls.addComponent(serverLabel);
        filterControls.addComponent(serverFilter);
        filterControls.addComponent(enableFiltered);
        filterControls.addComponent(disableFiltered);
        filterControls.componentResized();
        listTop.componentResized();
    }

    private List<KeybindRecord> filteredRecords() {
        String user = selectedOption(userFilterValues, userFilter, FILTER_ALL);
        String server = selectedOption(serverFilterValues, serverFilter, FILTER_ALL);
        return KeybindListViewModel.filter(controller.getRecords(), user, server);
    }

    private static int optionFor(String[] options, String value) {
        for (int i = 0; i < options.length; i++)
            if (options[i].equalsIgnoreCase(value)) return i;
        return 0;
    }

    private static String selectedOption(String[] options, KeybinderUiDropDown dropdown, String fallback) {
        if (dropdown == null || options == null) return fallback;
        int value = dropdown.getValue();
        return value >= 0 && value < options.length ? options[value] : fallback;
    }

    private List<String> filteredIds() {
        List<String> ids = new ArrayList<>();
        for (KeybindRecord record : filteredRecords()) ids.add(record.getId());
        return ids;
    }

    @Override
    public void gameTick() {
        super.gameTick();
        if (languageSelector.getValue() != previousLanguage) {
            previousLanguage = languageSelector.getValue();
            Language[] languages = Language.values();
            if (previousLanguage >= 0 && previousLanguage < languages.length)
                controller.setLanguage(languages[previousLanguage].getCode());
            return;
        }
        if (mode == Mode.EDITOR) {
            if (editor != null) editor.embeddedTick(width, height);
            return;
        }
        if (queueMonitorSide != null
                && queueMonitorSide.getValue() != previousQueueMonitorSide) {
            previousQueueMonitorSide = queueMonitorSide.getValue();
            QueueMonitorSide[] sides = QueueMonitorSide.values();
            if (previousQueueMonitorSide >= 0 && previousQueueMonitorSide < sides.length)
                controller.setQueueMonitorSide(sides[previousQueueMonitorSide]);
            return;
        }
        int currentLimit = controller.getQueueLimit();
        if (currentLimit != displayedQueueLimit) {
            KeybinderMod.deferUi(() -> {
                rebuildListTop();
                rebuildTable();
            });
            return;
        }
        applyTableLayout();
        if (userFilter != null && serverFilter != null
                && (userFilter.getValue() != lastUserFilterValue
                || serverFilter.getValue() != lastServerFilterValue)) {
            lastUserFilterValue = userFilter.getValue();
            lastServerFilterValue = serverFilter.getValue();
            KeybinderMod.deferUi(this::rebuildTable);
            return;
        }
        List<CheckboxChange> changes = new ArrayList<>();
        for (Map.Entry<KeybinderUiCheckBox, String> entry : new ArrayList<>(checkboxIds.entrySet())) {
            boolean current = entry.getKey().checked;
            Boolean previous = checkboxStates.get(entry.getKey());
            if (previous != null && previous != current) {
                checkboxStates.put(entry.getKey(), current);
                selectedRowId = entry.getValue();
                changes.add(new CheckboxChange(entry.getValue(), current));
            }
        }
        for (CheckboxChange change : changes)
            KeybinderMod.deferUi(() -> controller.setKeybindEnabled(change.id, change.enabled));
    }

    @Override
    void setSize(int requestedWidth, int requestedHeight) {
        // WWindow's resize grip otherwise accepts widths down to 64 and the old
        // gameTick correction fought the active mouse drag, causing flicker.
        super.setSize(Math.max(requestedWidth, minimumWidth),
                Math.max(requestedHeight, minimumHeight));
    }

    private void applyTableLayout() {
        if (width == lastLayoutWidth || tableRows.isEmpty()) return;
        lastLayoutWidth = width;
        int available = Math.max(requiredNameWidth + requiredKeyWidth + requiredUserWidth + requiredServerWidth,
                width - WINDOW_CHROME - CONTROLS_WIDTH - CHECK_WIDTH
                        - requiredEditWidth - requiredDuplicateWidth
                        - COLUMN_GAP * 7);
        int extra = Math.max(0,
                available - requiredNameWidth - requiredKeyWidth - requiredUserWidth - requiredServerWidth);
        int nameWidth = requiredNameWidth + extra / 3;
        int userWidth = requiredUserWidth + extra / 3;
        int serverWidth = requiredServerWidth + extra - extra / 3 * 2;
        for (TableRow row : tableRows) {
            row.controls.setSize(CONTROLS_WIDTH, row.controls.height);
            row.enabled.setSize(CHECK_WIDTH, row.enabled.height);
            row.name.setSize(nameWidth, row.name.height);
            row.key.setSize(requiredKeyWidth, row.key.height);
            row.user.setSize(userWidth, row.user.height);
            row.server.setSize(serverWidth, row.server.height);
            row.edit.setSize(requiredEditWidth, row.edit.height);
            row.duplicate.setSize(requiredDuplicateWidth, row.duplicate.height);
            row.panel.componentResized();
        }
        table.componentResized();
        root.componentResized();
    }

    static int localizedButtonColumnWidth(String messageKey, int minimum) {
        return LocalizedLayout.controlWidth(
                Messages.text(messageKey), minimum, 18, text -> new KeybinderUiLabel(text).width);
    }

    private final class SelectableRow extends KeybinderUiArrayPanel<FlexComponent> {
        private final ChamomiloUiV1Canvas canvas = new ChamomiloUiV1Canvas(this);
        private final String recordId;
        private final boolean statusError;
        private final boolean queueWarning;
        private final boolean valuePack;

        private SelectableRow(String name, String recordId, boolean statusError,
                              boolean queueWarning, boolean valuePack) {
            super(name, WurmArrayPanel.DIR_HORIZONTAL);
            this.recordId = recordId;
            this.statusError = statusError;
            this.queueWarning = queueWarning;
            this.valuePack = valuePack;
        }

        @Override
        void componentResized() {
            super.componentResized();
            centerChildrenVertically();
        }

        private void centerChildrenVertically() {
            for (FlexComponent component : components)
                component.setPosition(component.x, y + Math.max(0, (height - component.height) / 2));
        }

        @Override
        protected void renderComponent(Queue queue, float alpha) {
            if (KeybinderUi.id(this).equals("keybinder.header"))
                org.chamomilo.wurm.ui.v1.UiPainter.background(canvas.begin(queue),
                        org.chamomilo.wurm.ui.v1.UiBackground.LEATHER, 1f, x, y, width, height);
            if (statusError)
                fillRect(queue, 0.48f, 0.08f, 0.06f, 0.62f, x, y, width, height);
            else if (queueWarning)
                fillRect(queue, 0.62f, 0.28f, 0.03f, 1.0f, x, y, width, height);
            else if (valuePack)
                fillRect(queue, 0.20f, 0.42f, 0.22f, 1.0f, x, y, width, height);
            if (recordId != null && recordId.equals(selectedRowId))
                fillRect(queue, 0.20f, 0.34f, 0.50f, 0.55f, x, y, width, height);
            super.renderComponent(queue, 1.0f);
            if (KeybinderUi.id(this).equals("keybinder.header"))
                org.chamomilo.wurm.ui.v1.UiPainter.frame(canvas, 3, 1f, x, y, width, height);
            if (recordId != null && recordId.equals(dragMergeDestinationId))
                KeybinderDragIndicator.paintMerge(this, queue,
                        x, y, width, height, dragMergeOverLimit);
        }

        @Override
        void leftPressed(int mouseX, int mouseY, int clickCount) {
            if (recordId != null) {
                selectedRowId = recordId;
                if (clickCount == 2) {
                    cancelRowDrag();
                    KeybinderMod.deferUi(new Runnable() {
                        @Override public void run() { controller.editKeybind(recordId); }
                    });
                    return;
                }
                beginRowDrag(recordId, mouseY);
            }
            super.leftPressed(mouseX, mouseY, clickCount);
        }

        @Override void mouseDragged(int mouseX, int mouseY) {
            updateRowDrag(mouseY);
            super.mouseDragged(mouseX, mouseY);
        }

        @Override void leftReleased(int mouseX, int mouseY) {
            finishRowDrag();
            super.leftReleased(mouseX, mouseY);
        }
    }

    private class SelectableLabel extends KeybinderUiLabel {
        private final String recordId;

        private SelectableLabel(String text, String recordId) {
            super(text);
            this.recordId = recordId;
        }

        @Override
        void leftPressed(int mouseX, int mouseY, int clickCount) {
            selectedRowId = recordId;
            if (clickCount == 2) {
                cancelRowDrag();
                KeybinderMod.deferUi(new Runnable() {
                    @Override public void run() { controller.editKeybind(recordId); }
                });
                return;
            }
            beginRowDrag(recordId, mouseY);
            super.leftPressed(mouseX, mouseY, clickCount);
        }

        @Override void mouseDragged(int mouseX, int mouseY) {
            updateRowDrag(mouseY);
            super.mouseDragged(mouseX, mouseY);
        }

        @Override void leftReleased(int mouseX, int mouseY) {
            finishRowDrag();
            super.leftReleased(mouseX, mouseY);
        }
    }

    /** Name label with fixed-alpha, high-contrast mode tags. */
    private final class ModeNameLabel extends SelectableLabel {
        private static final float HUD_RED = 0.35f;
        private static final float HUD_GREEN = 0.68f;
        private static final float HUD_BLUE = 1.00f;
        private static final float MULTI_RED = 0.35f;
        private static final float MULTI_GREEN = 1.00f;
        private static final float MULTI_BLUE = 0.45f;

        private final boolean hudMode;
        private final boolean multiMode;
        private final String baseName;

        private ModeNameLabel(KeybindRecord record) {
            super(KeybindNamePrefixes.apply(record.getName(), record.getVariants().size(),
                    record.isHudMulti()), record.getId());
            hudMode = record.isHudMulti();
            multiMode = record.isMultiPurpose();
            baseName = KeybindNamePrefixes.baseName(record.getName());
        }

        @Override
        protected void renderComponent(Queue queue, float ignoredAlpha) {
            int drawX = x + 4;
            int baseline = y + (height - text.getHeight()) / 2 + text.getAscent();
            if (hudMode)
                drawX += paintSegment(queue, KeybindNamePrefixes.HUD, drawX, baseline,
                        HUD_RED, HUD_GREEN, HUD_BLUE);
            if (multiMode)
                drawX += paintSegment(queue, KeybindNamePrefixes.MULTI, drawX, baseline,
                        MULTI_RED, MULTI_GREEN, MULTI_BLUE);
            paintSegment(queue, baseName, drawX, baseline, 1.0f, 1.0f, 1.0f);
        }

        private int paintSegment(Queue queue, String value, int drawX, int baseline,
                                 float red, float green, float blue) {
            value = KeybinderUi.fit(text, value, Math.max(0, x + width - 4 - drawX));
            text.moveTo(drawX, baseline);
            return text.paint(queue, value, red, green, blue, 1.0f);
        }
    }

    private static final class CheckboxChange {
        private final String id;
        private final boolean enabled;

        private CheckboxChange(String id, boolean enabled) {
            this.id = id;
            this.enabled = enabled;
        }
    }

    private static final class TableRow {
        private final String recordId;
        private final WurmArrayPanel<FlexComponent> panel;
        private final FlexComponent controls;
        private final FlexComponent enabled;
        private final FlexComponent name;
        private final FlexComponent key;
        private final FlexComponent user;
        private final FlexComponent server;
        private final FlexComponent edit;
        private final FlexComponent duplicate;

        private TableRow(String recordId, WurmArrayPanel<FlexComponent> panel, FlexComponent controls,
                         FlexComponent enabled, FlexComponent name,
                         FlexComponent key,
                         FlexComponent user, FlexComponent server,
                         FlexComponent edit, FlexComponent duplicate) {
            this.recordId = recordId;
            this.panel = panel;
            this.controls = controls;
            this.enabled = enabled;
            this.name = name;
            this.key = key;
            this.user = user;
            this.server = server;
            this.edit = edit;
            this.duplicate = duplicate;
        }
    }

    @Override
    public void buttonPressed(WButton button) {
        RowAction action = rowActions.get(button);
        if (action != null) selectedRowId = action.recordId;
    }

    @Override
    public void buttonClicked(WButton button) {
        if (button == instructionButton) KeybinderInstructionWindow.open(false);
        else if (button == enableFiltered)
            controller.setKeybindsEnabled(filteredIds(), true);
        else if (button == disableFiltered)
            controller.setKeybindsEnabled(filteredIds(), false);
        else if (button == addButton) controller.addNewKeybind();
        else if (button == importButton) controller.requestImport();
        else if (button == importFileButton) controller.requestImportFile();
        else if (button == exportAllButton) controller.requestExportAll();
        else if (button == restoreButton) controller.restoreOriginalBindings();
        else if (editIds.containsKey(button)) controller.editKeybind(editIds.get(button));
        else if (duplicateIds.containsKey(button))
            controller.duplicateKeybind(duplicateIds.get(button));
        else {
            RowAction action = rowActions.get(button);
            if (action == null) return;
            if (action.kind == RowAction.ADD_AFTER) controller.addNewKeybindAfter(action.recordId);
            else if (action.kind == RowAction.REMOVE) controller.deleteKeybind(action.recordId);
        }
    }

    private static final class RowAction {
        private static final int ADD_AFTER = 1;
        private static final int REMOVE = 2;
        private final int kind;
        private final String recordId;
        private RowAction(int kind, String recordId) { this.kind = kind; this.recordId = recordId; }
    }

    private void beginRowDrag(String recordId, int mouseY) {
        hideRowInsertionGap();
        draggedRowId = recordId;
        dragPressY = mouseY;
        dragInsertion = -1;
        rowDragging = false;
        dragMergeDestinationId = null;
        dragMergeOverLimit = false;
    }

    private void updateRowDrag(int mouseY) {
        if (draggedRowId == null) return;
        if (!rowDragging && Math.abs(mouseY - dragPressY) < 4) return;
        rowDragging = true;
        dragMergeDestinationId = null;
        dragMergeOverLimit = false;
        for (TableRow row : tableRows) {
            if (row.recordId == null || row.recordId.equals(draggedRowId)) continue;
            if (DropZoneClassifier.classify(mouseY, row.panel.y, row.panel.height)
                    == DropZoneClassifier.Zone.MERGE) {
                dragMergeDestinationId = row.recordId;
                dragMergeOverLimit = variantCount(draggedRowId) + variantCount(row.recordId)
                        > KeybindLimits.MAX_VARIANTS;
                dragInsertion = -1;
                // Preserve the space already opened for insertion so the row
                // under the pointer cannot jump while entering merge mode.
                rowInsertionGap.setIndicatorVisible(false);
                return;
            }
        }
        List<Integer> tops = new ArrayList<>();
        List<Integer> heights = new ArrayList<>();
        for (TableRow row : tableRows) {
            if (row.recordId == null) continue;
            tops.add(row.panel.y);
            heights.add(row.panel.height);
        }
        dragInsertion = RowInsertionCalculator.insertionIndex(mouseY, tops, heights);
        showRowInsertionGap(dragInsertion);
    }

    private void finishRowDrag() {
        String id = draggedRowId;
        String mergeDestination = dragMergeDestinationId;
        int insertion = dragInsertion;
        boolean merge = rowDragging && mergeDestination != null;
        boolean apply = rowDragging && !merge && insertion >= 0;
        cancelRowDrag();
        if (merge) controller.requestMerge(id, mergeDestination);
        else if (apply) controller.moveKeybind(id, filteredIds(), insertion);
    }

    private void cancelRowDrag() {
        hideRowInsertionGap();
        draggedRowId = null;
        dragInsertion = -1;
        rowDragging = false;
        dragMergeDestinationId = null;
        dragMergeOverLimit = false;
    }

    private void showRowInsertionGap(int insertion) {
        if (insertion < 0) {
            hideRowInsertionGap();
            return;
        }
        rowInsertionGap.setIndicatorVisible(true);
        KeybinderDragIndicator.place(table, rowInsertionGap,
                KeybinderDragIndicator.keybindGapComponentIndex(insertion));
    }

    private void hideRowInsertionGap() {
        KeybinderDragIndicator.remove(table, rowInsertionGap);
        rowInsertionGap.setIndicatorVisible(true);
    }

    private int variantCount(String id) {
        for (KeybindRecord record : controller.getRecords())
            if (record.getId().equals(id)) return record.getVariants().size();
        return 0;
    }

    @Override
    protected void closePressed() {
        controller.windowClosed();
    }

    private enum Mode { LIST, EDITOR }

    @Override public FlexComponent getComponentAt(int mx, int my) {
        positionLanguageSelector();
        if (languageSelector != null && languageSelector.contains(mx, my)) return languageSelector;
        return super.getComponentAt(mx, my);
    }

    private void positionLanguageSelector() {
        WButton maximize = KeybinderUi.maximizeControl(this);
        if (languageSelector != null && maximize != null)
            languageSelector.setPosition(maximize.x - 28 - 8 - languageSelector.width,
                    maximize.y + (maximize.height - languageSelector.height) / 2);
    }

    @Override protected void renderComponent(Queue queue, float ignoredAlpha) {
        super.renderComponent(queue, 1f);
        positionLanguageSelector();
        if (languageSelector != null) languageSelector.render(queue, 1f);
    }
}
