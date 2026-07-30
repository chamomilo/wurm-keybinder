package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.backend.Queue;
import org.keybinder.wurm.KeybinderMod;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.queue.ActionQueueCostCalculator;
import org.keybinder.wurm.queue.QueueCost;
import org.keybinder.wurm.ui.KeybinderUiController;
import org.keybinder.wurm.ui.RowInsertionCalculator;
import com.wurmonline.client.resources.textures.KeybinderTextureFactory;
import com.wurmonline.client.resources.textures.ResourceTexture;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class KeybinderWindow extends WWindow implements ButtonListener {
    private static final int WINDOW_CHROME = 38;
    private static final int CONTROLS_WIDTH = 41;
    private static final int CHECK_WIDTH = 34;
    private static final int KEY_WIDTH = 90;
    private static final int EDIT_WIDTH = 58;
    private static final int DELETE_WIDTH = 68;
    private static final int COLUMN_GAP = 8;
    private static final int DEFAULT_HEIGHT = 285;
    private static final int INTRO_MIN_WIDTH = 800;
    private static final int INTRO_MIN_HEIGHT = 700;
    private static final int INTRO_HEIGHT_PADDING = 10;
    private static final int INTRO_HORIZONTAL_CHROME = 0;
    private static final int INTRO_BUTTON_WIDTH = 240;
    private final KeybinderUiController controller;
    private final WurmArrayPanel<FlexComponent> table;
    private final WurmBorderPanel root;
    private final WurmArrayPanel<FlexComponent> listTop;
    private final WurmArrayPanel<FlexComponent> filterControls;
    private final List<TableRow> tableRows = new ArrayList<>();
    private final WButton addButton;
    private WButton enableFiltered;
    private WButton disableFiltered;
    private WurmDropDown userFilter;
    private WurmDropDown serverFilter;
    private String[] userFilterOptions = {"All users"};
    private String[] serverFilterOptions = {"All servers"};
    private int lastUserFilterValue;
    private int lastServerFilterValue;
    private WButton introPrimary;
    private WCheckBox introSkip;
    private WurmArrayPanel<FlexComponent> introContent;
    private IntroBanner introBanner;
    private final List<FlexComponent> introFullWidth = new ArrayList<>();
    private boolean previousIntroSkip;
    private Mode mode = Mode.LIST;
    private KeybinderEditorWindow editor;
    private final Map<WCheckBox, String> checkboxIds = new HashMap<>();
    private final Map<WCheckBox, Boolean> checkboxStates = new HashMap<>();
    private final Map<WButton, RowAction> rowActions = new HashMap<>();
    private final Map<WButton, String> editIds = new HashMap<>();
    private final Map<WButton, String> deleteIds = new HashMap<>();
    private int requiredNameWidth;
    private int requiredKeyWidth;
    private int requiredUserWidth;
    private int requiredServerWidth;
    private int minimumWidth;
    private int minimumHeight = DEFAULT_HEIGHT;
    private int lastLayoutWidth = -1;
    private int lastIntroLayoutWidth = -1;
    private int introFixedHeight = INTRO_MIN_HEIGHT;
    private String selectedRowId;
    private String draggedRowId;
    private int dragPressY;
    private int dragInsertion = -1;
    private boolean rowDragging;

    public KeybinderWindow(KeybinderUiController controller) {
        super("keybinder.window", true);
        this.controller = controller;
        setTitle("Keybinder");
        table = new WurmArrayPanel<>("keybinder.table", WurmArrayPanel.DIR_VERTICAL, true);
        addButton = new WButton("Add", this);

        root = new WurmBorderPanel("keybinder.root");
        listTop =
                new WurmArrayPanel<>("keybinder.list.introduction", WurmArrayPanel.DIR_VERTICAL, true);
        listTop.addComponent(new WurmLabel(
                "REGISTERED KEYBINDS"));
        listTop.addComponent(new WurmLabel(
                "Use the controls on the left to add, remove, or reorder rows."));
        listTop.addComponent(spacer(5));
        filterControls = new WurmArrayPanel<>(
                "keybinder.list.filters", WurmArrayPanel.DIR_HORIZONTAL);
        filterControls.componentWidthOffset = COLUMN_GAP;
        listTop.addComponent(filterControls);
        listTop.addComponent(spacer(5));
        root.setComponent(listTop, WurmBorderPanel.NORTH);
        root.setComponent(new WurmScrollPanel("keybinder.scroll", table, false, true), WurmBorderPanel.CENTER);
        refresh();
        setComponent(root);
        setInitialSize(Math.max(minimumWidth, 420), DEFAULT_HEIGHT, true);
    }

    public void showIntro() {
        mode = Mode.INTRO;
        editor = null;
        resizable = false;
        minimumWidth = INTRO_MIN_WIDTH;
        minimumHeight = INTRO_MIN_HEIGHT;
        setTitle("Welcome to Keybinder");
        setComponent(createIntroContent());
        layoutIntroContent(INTRO_MIN_WIDTH);
        introFixedHeight = Math.max(INTRO_MIN_HEIGHT,
                introContent.calcHeight() + WINDOW_CHROME + INTRO_HEIGHT_PADDING);
        minimumHeight = introFixedHeight;
        setSize(INTRO_MIN_WIDTH, introFixedHeight);
    }

    public void showKeybinds() {
        mode = Mode.LIST;
        editor = null;
        resizable = true;
        minimumHeight = DEFAULT_HEIGHT;
        setTitle("Keybinder");
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
        setTitle("Keybind constructor");
        setComponent(newEditor.getEmbeddedContent());
        if (width < minimumWidth) setSize(minimumWidth, Math.max(height, 430));
        newEditor.embeddedTick(width, height);
    }

    private FlexComponent createIntroContent() {
        introFullWidth.clear();
        // Without the former WurmScrollPanel parent, a flexible vertical panel
        // adopts the width of the longest unwrapped label and then gives that
        // width to every child. Keep the intro column at the window content
        // width so the 800x200 banner is rendered at exactly 800x200.
        introContent = new WurmArrayPanel<>(
                "keybinder.intro.content", WurmArrayPanel.DIR_VERTICAL, INTRO_MIN_WIDTH, 0);
        introBanner = new IntroBanner();
        introBanner.layout(INTRO_MIN_WIDTH - INTRO_HORIZONTAL_CHROME);
        addIntro(introBanner);
        addIntro(spacer(8));
        addIntro(introText("WELCOME TO KEYBINDER"));
        addIntro(introText(
                "Keybinder makes working with your keyboard easier, clearer, and completely at home in Wurm."));
        addIntro(introText(
                "It replaces both vanilla keybind management and complicated console binds used by different mods. This is not automation or scripting - it is simply a much more convenient way to use both simple and complex actions bound to keys."));
        addIntro(spacer(8));
        addIntro(introSection("ONE KEY - MANY ACTIONS, TOOLS, AND TARGETS",
                "A keybind can contain several Wurm commands, up to the length of your character's action queue. For each command, you can pick an action from the vanilla list (boring...), enter a console command (even more boring...), or use Capture Action (!!!). Simply perform an action as usual. Keybinder will remember what you did and add it to your keybind. Bingo!"));
        addIntro(introText(
                "For every action, select the tool and target you need: hovered, selected, exact or nearby objects, your ride, tiles, areas, and many more. This flexible targeting system was inspired by bdew's Custom Actions. If Custom Actions is installed, Keybinder will offer to import its commands on the first run and then disable the old mod."));
        addIntro(introText(
                "There is no reason to run both mods at the same time. Keybinder will also offer to import your existing vanilla keybinds, allowing you to manage everything from one window."));
        addIntro(spacer(8));
        addIntro(introSection("MULTI-KEYBINDS",
                "Stop playing your keyboard like a piano. A multi-keybind lets you assign several keybinds to the same key. To switch between them, just hold the key for one second or longer. You can assign up to 10 keybinds to the same key. Need more? Let me know :)"));
        addIntro(spacer(8));
        addIntro(introSection("DIFFERENT CHARACTERS, DIFFERENT SERVERS",
                "Create separate keybind sets for different characters and servers, then switch between them easily. Your enchanter can use a completely different set from your farmer or woodcutter."));
        addIntro(spacer(8));
        addIntro(introSection("SMART IMPROVE",
                "Keybinder includes an updated version of the famous Improved Improve logic."));
        addIntro(introText(
                "Simply choose the key you want to use for improving - and improve! Just remember to keep all the required tools in your toolbelt. The old Improved Improve mod is no longer required and can be disabled."));
        addIntro(introText(
                "Everything is configured through a clear, Wurm-styled interface. No console work is required."));
        addIntro(introText("I hope you will love this mod as much as I do :)"));
        addIntro(spacer(8));
        addIntro(introText("Cheers! Chamomilo"));
        addIntro(spacer(8));
        addIntro(introSection("SUPPORT YOUR NEW KEYBINDER",
                "If you would like to say THANK YOU or support my work, please join the SKLOTOPOLIS server and send me, Chamomilo, a message - or a few in-game coins. Gold would be wonderful :)"));
        addIntro(introText(
                "Silver or copper - whatever amount feels appropriate. Thank you in advance!"));
        addIntro(spacer(8));
        addIntro(introText("WITH THANKS TO THE MODDERS WHO INSPIRED KEYBINDER"));
        addIntro(link("bdew - Custom Actions: " + KeybinderMod.ORIGINAL_PROJECT,
                controller::openOriginalProject));
        addIntro(link("Munsta0 - the original Improved Improve: "
                + KeybinderMod.MUNSTA_IMPROVE_PROJECT, controller::openMunstaImproveProject));
        addIntro(link("inniria - i2improve: " + KeybinderMod.INNIRIA_IMPROVE_PROJECT,
                controller::openInniriaImproveProject));
        addIntro(link("Snidor - continued i2improve: " + KeybinderMod.IMPROVE_PROJECT,
                controller::openImproveProject));
        addIntro(introText("Keybinder is licensed under GNU LGPL 3.0 or later."));
        addIntro(spacer(8));

        boolean legacy = controller.isLegacyActionInstalled();
        if (legacy) {
            addIntro(introText(
                    "Custom Actions was found. Keybinder can import its binds and disable it safely."));
        }
        introPrimary = new WButton(legacy
                ? "Import binds, disable Custom Actions, and exit game"
                : "Start Keybinding", this);
        addIntro(centeredButton(introPrimary));
        introSkip = new WCheckBox("Skip intro page on next load");
        introSkip.checked = controller.isSkipIntro();
        previousIntroSkip = introSkip.checked;
        addIntro(introSkip);
        addIntro(introText("Keybinder " + KeybinderMod.VERSION));
        lastIntroLayoutWidth = -1;
        return introContent;
    }

    private void addIntro(FlexComponent component) {
        introContent.addComponent(component);
        introFullWidth.add(component);
    }

    private static FlexComponent centeredButton(WButton button) {
        int rowHeight = Math.max(18, button.height);
        int sideWidth = (INTRO_MIN_WIDTH - INTRO_BUTTON_WIDTH) / 2;
        WurmArrayPanel<FlexComponent> row = new WurmArrayPanel<>(
                "keybinder.intro.primary.row", WurmArrayPanel.DIR_HORIZONTAL,
                INTRO_MIN_WIDTH, rowHeight);
        WurmLabel left = new WurmLabel("");
        left.setSize(sideWidth, rowHeight);
        button.setSize(INTRO_BUTTON_WIDTH, rowHeight);
        WurmLabel right = new WurmLabel("");
        right.setSize(sideWidth, rowHeight);
        row.addComponent(left);
        row.addComponent(button);
        row.addComponent(right);
        return row;
    }

    private static FlexComponent spacer(int height) {
        WurmLabel spacer = new WurmLabel("");
        spacer.setSize(1, height);
        return spacer;
    }

    private FlexComponent link(String text, Runnable action) {
        return new IntroText(text, action);
    }

    private static FlexComponent introText(String text) {
        return new IntroText(text, null);
    }

    private static FlexComponent introSection(String heading, String text) {
        return new IntroText(heading + "  " + text, null);
    }

    public void refresh() {
        String selectedUser = selectedOption(userFilterOptions, userFilter, "All users");
        String selectedServer = selectedOption(serverFilterOptions, serverFilter, "All servers");
        rebuildFilters(selectedUser, selectedServer);
        rebuildTable();
    }

    private void rebuildTable() {
        table.removeAllComponents();
        tableRows.clear();
        checkboxIds.clear();
        checkboxStates.clear();
        rowActions.clear();
        editIds.clear();
        deleteIds.clear();
        requiredNameWidth = 0;
        requiredKeyWidth = KEY_WIDTH;
        requiredUserWidth = 90;
        requiredServerWidth = 100;
        table.addComponent(header());
        for (KeybindRecord record : filteredRecords()) table.addComponent(row(record));
        WurmArrayPanel<FlexComponent> addRow =
                new WurmArrayPanel<>("keybinder.add.row", WurmArrayPanel.DIR_HORIZONTAL);
        addRow.addComponent(addButton);
        table.addComponent(addRow);
        minimumWidth = WINDOW_CHROME + CONTROLS_WIDTH + CHECK_WIDTH + EDIT_WIDTH + DELETE_WIDTH
                + requiredNameWidth + requiredKeyWidth + requiredUserWidth + requiredServerWidth
                + COLUMN_GAP * 7;
        minimumWidth = Math.max(360, minimumWidth);
        lastLayoutWidth = -1;
        table.componentResized();
        applyTableLayout();
    }

    private FlexComponent header() {
        return createTableRow("keybinder.header", new WurmLabel(""), new WurmLabel("On"),
                new WurmLabel("Keybind name"), new WurmLabel("Key"),
                new WurmLabel("Created by user"), new WurmLabel("Created on server"),
                new WurmLabel(""), new WurmLabel(""));
    }

    private FlexComponent row(KeybindRecord record) {
        WCheckBox enabled = new WCheckBox("");
        enabled.checked = record.isEnabled();
        checkboxIds.put(enabled, record.getId());
        checkboxStates.put(enabled, record.isEnabled());
        WurmLabel name = new SelectableLabel(record.getName()
                + (record.isMultiPurpose() ? " (multi)" : ""), record.getId());
        WurmLabel key = new SelectableLabel(
                org.keybinder.wurm.catalog.InputKeyCatalog.displayChord(record.getKey()),
                record.getId());
        WurmLabel createdBy = new SelectableLabel(displayOrigin(record.getCreatedByUser()), record.getId());
        WurmLabel createdOn = new SelectableLabel(displayOrigin(record.getCreatedOnServer()), record.getId());

        WButton edit = new WButton("Edit", this);
        edit.setHoverString("Edit keybind");
        editIds.put(edit, record.getId());

        WButton delete = new WButton("Delete", this);
        delete.setHoverString("Delete keybind");
        deleteIds.put(delete, record.getId());
        delete.setConfirm(true);
        delete.setConfirmQuestion("Delete keybind?");
        delete.setConfirmMessage("Delete " + record.getName());
        WurmArrayPanel<FlexComponent> controls =
                new WurmArrayPanel<>("keybinder.controls." + record.getId(), WurmArrayPanel.DIR_HORIZONTAL);
        controls.componentWidthOffset = 1;
        controls.addComponent(rowButton(KeybinderGlyphButton.Kind.PLUS_BOX,
                RowAction.ADD_AFTER, record.getId()));
        controls.addComponent(rowButton(KeybinderGlyphButton.Kind.CLOSE_BOX,
                RowAction.REMOVE, record.getId()));
        RowStatus status = rowStatus(record);
        FlexComponent row = createTableRow("keybinder.row." + record.getId(), record.getId(), status.red,
                controls, enabled, name, key, createdBy, createdOn, edit, delete);
        if (status.red) {
            enabled.setHoverString(status.hoverText);
            edit.setHoverString(status.hoverText);
            delete.setHoverString(status.hoverText);
        }
        return row;
    }

    private WButton rowButton(KeybinderGlyphButton.Kind kind, int action, String recordId) {
        String tooltip = action == RowAction.ADD_AFTER ? "Add keybind below" : "Delete keybind";
        WButton button = new KeybinderGlyphButton(kind, this, tooltip);
        rowActions.put(button, new RowAction(action, recordId));
        if (action == RowAction.REMOVE) {
            button.setConfirm(true);
            button.setConfirmQuestion("Delete keybind?");
            button.setConfirmMessage("Delete this keybind");
        }
        return button;
    }

    private FlexComponent createTableRow(String id, FlexComponent controls, FlexComponent enabled,
                                         FlexComponent name, FlexComponent key,
                                         FlexComponent user, FlexComponent server,
                                         FlexComponent edit, FlexComponent delete) {
        return createTableRow(id, null, false, controls, enabled, name, key,
                user, server, edit, delete);
    }

    private FlexComponent createTableRow(String id, String recordId, FlexComponent controls,
                                         FlexComponent enabled, FlexComponent name,
                                         FlexComponent key,
                                         FlexComponent user, FlexComponent server,
                                         FlexComponent edit, FlexComponent delete) {
        return createTableRow(id, recordId, false, controls, enabled, name, key,
                user, server, edit, delete);
    }

    private FlexComponent createTableRow(String id, String recordId, boolean overLimit,
                                         FlexComponent controls, FlexComponent enabled,
                                         FlexComponent name, FlexComponent key,
                                         FlexComponent user, FlexComponent server,
                                         FlexComponent edit, FlexComponent delete) {
        requiredNameWidth = Math.max(requiredNameWidth, name.width);
        requiredKeyWidth = Math.max(requiredKeyWidth, key.width);
        requiredUserWidth = Math.max(requiredUserWidth, user.width);
        requiredServerWidth = Math.max(requiredServerWidth, server.width);
        SelectableRow panel = new SelectableRow(id, recordId, overLimit);
        panel.componentWidthOffset = COLUMN_GAP;
        panel.addComponent(controls);
        panel.addComponent(enabled);
        panel.addComponent(name);
        panel.addComponent(key);
        panel.addComponent(user);
        panel.addComponent(server);
        panel.addComponent(edit);
        panel.addComponent(delete);
        tableRows.add(new TableRow(recordId, panel, controls, enabled, name, key,
                user, server, edit, delete));
        return panel;
    }

    private RowStatus rowStatus(KeybindRecord record) {
        List<String> reasons = new ArrayList<>();
        if (record.getKey() == null || record.getKey().trim().isEmpty())
            reasons.add("no key is assigned");
        if (record.getKeybindSteps() == null || record.getKeybindSteps().isEmpty())
            reasons.add("no action or command is configured");
        QueueCost cost = new ActionQueueCostCalculator().keybindCost(record);
        if (cost.getKind() == QueueCost.Kind.FIXED && controller.getQueueLimit() > 0
                && cost.getValue() > controller.getQueueLimit())
            reasons.add("action queue cost " + cost.getValue()
                    + " exceeds the current limit " + controller.getQueueLimit());
        if (reasons.isEmpty()) return RowStatus.OK;
        StringBuilder text = new StringBuilder("Cannot enable this keybind: ");
        for (int i = 0; i < reasons.size(); i++) {
            if (i > 0) text.append(i + 1 == reasons.size() ? " and " : ", ");
            text.append(reasons.get(i));
        }
        return new RowStatus(true, text.append('.').toString());
    }

    private void rebuildFilters(String selectedUser, String selectedServer) {
        userFilterOptions = originOptions(controller.getRecords(), true, "All users", "Unknown user");
        serverFilterOptions = originOptions(controller.getRecords(), false, "All servers", "Unknown server");
        userFilter = new WurmDropDown("keybinder.filter.user",
                optionFor(userFilterOptions, selectedUser), userFilterOptions);
        serverFilter = new WurmDropDown("keybinder.filter.server",
                optionFor(serverFilterOptions, selectedServer), serverFilterOptions);
        lastUserFilterValue = userFilter.getValue();
        lastServerFilterValue = serverFilter.getValue();
        enableFiltered = new WButton("Enable filtered", this);
        disableFiltered = new WButton("Disable filtered", this);
        enableFiltered.setHoverString("Enable every keybind visible under the current user and server filters.");
        disableFiltered.setHoverString("Disable every keybind visible under the current user and server filters.");
        filterControls.removeAllComponents();
        filterControls.addComponent(new WurmLabel("User"));
        filterControls.addComponent(userFilter);
        filterControls.addComponent(new WurmLabel("Server"));
        filterControls.addComponent(serverFilter);
        filterControls.addComponent(enableFiltered);
        filterControls.addComponent(disableFiltered);
        filterControls.componentResized();
        listTop.componentResized();
    }

    private List<KeybindRecord> filteredRecords() {
        String user = selectedOption(userFilterOptions, userFilter, "All users");
        String server = selectedOption(serverFilterOptions, serverFilter, "All servers");
        List<KeybindRecord> result = new ArrayList<>();
        for (KeybindRecord record : controller.getRecords()) {
            if (!matchesOrigin(user, record.getCreatedByUser(), "All users", "Unknown user")) continue;
            if (!matchesOrigin(server, record.getCreatedOnServer(), "All servers", "Unknown server")) continue;
            result.add(record);
        }
        return result;
    }

    private static boolean matchesOrigin(String filter, String value, String all, String unknown) {
        if (all.equals(filter)) return true;
        String clean = value == null ? "" : value.trim();
        return unknown.equals(filter) ? clean.isEmpty() : filter.equalsIgnoreCase(clean);
    }

    private static String[] originOptions(List<KeybindRecord> records, boolean user,
                                          String all, String unknown) {
        java.util.Set<String> values = new java.util.TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        boolean hasUnknown = false;
        for (KeybindRecord record : records) {
            String value = user ? record.getCreatedByUser() : record.getCreatedOnServer();
            if (value == null || value.trim().isEmpty()) hasUnknown = true;
            else values.add(value.trim());
        }
        List<String> options = new ArrayList<>();
        options.add(all);
        if (hasUnknown) options.add(unknown);
        options.addAll(values);
        return options.toArray(new String[options.size()]);
    }

    private static int optionFor(String[] options, String value) {
        for (int i = 0; i < options.length; i++)
            if (options[i].equalsIgnoreCase(value)) return i;
        return 0;
    }

    private static String selectedOption(String[] options, WurmDropDown dropdown, String fallback) {
        if (dropdown == null || options == null) return fallback;
        int value = dropdown.getValue();
        return value >= 0 && value < options.length ? options[value] : fallback;
    }

    private static String displayOrigin(String value) {
        return value == null || value.trim().isEmpty() ? "Unknown" : value.trim();
    }

    private List<String> filteredIds() {
        List<String> ids = new ArrayList<>();
        for (KeybindRecord record : filteredRecords()) ids.add(record.getId());
        return ids;
    }

    @Override
    public void gameTick() {
        super.gameTick();
        if (mode == Mode.INTRO) {
            applyIntroLayout();
            if (introSkip != null && introSkip.checked != previousIntroSkip) {
                previousIntroSkip = introSkip.checked;
                controller.setSkipIntro(introSkip.checked);
            }
            return;
        }
        if (mode == Mode.EDITOR) {
            if (editor != null) editor.embeddedTick(width, height);
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
        for (Map.Entry<WCheckBox, String> entry : new ArrayList<>(checkboxIds.entrySet())) {
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
        if (mode == Mode.INTRO) {
            super.setSize(INTRO_MIN_WIDTH, introFixedHeight);
            return;
        }
        // WWindow's resize grip otherwise accepts widths down to 64 and the old
        // gameTick correction fought the active mouse drag, causing flicker.
        super.setSize(Math.max(requestedWidth, minimumWidth),
                Math.max(requestedHeight, minimumHeight));
    }

    private void applyIntroLayout() {
        if (mode != Mode.INTRO || introContent == null || width == lastIntroLayoutWidth) return;
        layoutIntroContent(width);
    }

    private void layoutIntroContent(int windowWidth) {
        if (introContent == null) return;
        lastIntroLayoutWidth = windowWidth;
        int contentWidth = Math.max(300, windowWidth - INTRO_HORIZONTAL_CHROME);
        for (FlexComponent component : introFullWidth) {
            if (component == introBanner) {
                component.setSize(contentWidth,
                        KeybinderIntroLayout.bannerHeight(contentWidth));
            } else if (component instanceof IntroText) {
                ((IntroText) component).layout(contentWidth);
            } else {
                component.setSize(contentWidth, component.height);
            }
        }
        introContent.setSize(contentWidth, introContent.calcHeight());
        introContent.componentResized();
    }

    private void applyTableLayout() {
        if (width == lastLayoutWidth || tableRows.isEmpty()) return;
        lastLayoutWidth = width;
        int available = Math.max(requiredNameWidth + requiredKeyWidth + requiredUserWidth + requiredServerWidth,
                width - WINDOW_CHROME - CONTROLS_WIDTH - CHECK_WIDTH - EDIT_WIDTH - DELETE_WIDTH
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
            row.edit.setSize(EDIT_WIDTH, row.edit.height);
            row.delete.setSize(DELETE_WIDTH, row.delete.height);
            row.panel.componentResized();
        }
        table.componentResized();
        root.componentResized();
    }

    private final class SelectableRow extends WurmArrayPanel<FlexComponent> {
        private final String recordId;
        private final boolean overLimit;

        private SelectableRow(String name, String recordId, boolean overLimit) {
            super(name, WurmArrayPanel.DIR_HORIZONTAL);
            this.recordId = recordId;
            this.overLimit = overLimit;
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
            if (overLimit)
                fillRect(queue, 0.48f, 0.08f, 0.06f, 0.62f, x, y, width, height);
            if (recordId != null && recordId.equals(selectedRowId))
                fillRect(queue, 0.20f, 0.34f, 0.50f, 0.55f, x, y, width, height);
            super.renderComponent(queue, alpha);
            if (recordId != null && rowDragging) {
                int index = draggableRowIndex(recordId);
                if (dragInsertion == index)
                    fillRect(queue, 0.92f, 0.77f, 0.42f, 1.0f, x, y, width, 2);
                if (index >= 0 && index + 1 == draggableRowCount() && dragInsertion == index + 1)
                    fillRect(queue, 0.92f, 0.77f, 0.42f, 1.0f,
                            x, y + height - 2, width, 2);
            }
        }

        @Override
        void leftPressed(int mouseX, int mouseY, int clickCount) {
            if (recordId != null) {
                selectedRowId = recordId;
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

    private final class SelectableLabel extends WurmLabel {
        private final String recordId;

        private SelectableLabel(String text, String recordId) {
            super(text);
            this.recordId = recordId;
        }

        @Override
        void leftPressed(int mouseX, int mouseY, int clickCount) {
            selectedRowId = recordId;
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

    private static final class CheckboxChange {
        private final String id;
        private final boolean enabled;

        private CheckboxChange(String id, boolean enabled) {
            this.id = id;
            this.enabled = enabled;
        }
    }

    private static final class RowStatus {
        private static final RowStatus OK = new RowStatus(false, "");
        private final boolean red;
        private final String hoverText;

        private RowStatus(boolean red, String hoverText) {
            this.red = red;
            this.hoverText = hoverText;
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
        private final FlexComponent delete;

        private TableRow(String recordId, WurmArrayPanel<FlexComponent> panel, FlexComponent controls,
                         FlexComponent enabled, FlexComponent name,
                         FlexComponent key,
                         FlexComponent user, FlexComponent server,
                         FlexComponent edit, FlexComponent delete) {
            this.recordId = recordId;
            this.panel = panel;
            this.controls = controls;
            this.enabled = enabled;
            this.name = name;
            this.key = key;
            this.user = user;
            this.server = server;
            this.edit = edit;
            this.delete = delete;
        }
    }

    @Override
    public void buttonPressed(WButton button) {
        RowAction action = rowActions.get(button);
        if (action != null) selectedRowId = action.recordId;
    }

    @Override
    public void buttonClicked(WButton button) {
        if (button == introPrimary) {
            controller.setSkipIntro(introSkip != null && introSkip.checked);
            if (controller.isLegacyActionInstalled()) controller.importDisableAndRestart();
            else controller.startFromIntro();
        }
        else if (button == enableFiltered)
            controller.setKeybindsEnabled(filteredIds(), true);
        else if (button == disableFiltered)
            controller.setKeybindsEnabled(filteredIds(), false);
        else if (button == addButton) controller.addNewKeybind();
        else if (editIds.containsKey(button)) controller.editKeybind(editIds.get(button));
        else if (deleteIds.containsKey(button)) controller.deleteKeybind(deleteIds.get(button));
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
        draggedRowId = recordId;
        dragPressY = mouseY;
        dragInsertion = -1;
        rowDragging = false;
    }

    private void updateRowDrag(int mouseY) {
        if (draggedRowId == null) return;
        if (!rowDragging && Math.abs(mouseY - dragPressY) < 4) return;
        rowDragging = true;
        List<Integer> tops = new ArrayList<>();
        List<Integer> heights = new ArrayList<>();
        for (TableRow row : tableRows) {
            if (row.recordId == null) continue;
            tops.add(row.panel.y);
            heights.add(row.panel.height);
        }
        dragInsertion = RowInsertionCalculator.insertionIndex(mouseY, tops, heights);
    }

    private void finishRowDrag() {
        String id = draggedRowId;
        int insertion = dragInsertion;
        boolean apply = rowDragging && insertion >= 0;
        draggedRowId = null;
        dragInsertion = -1;
        rowDragging = false;
        if (apply) controller.moveKeybind(id, filteredIds(), insertion);
    }

    private int draggableRowIndex(String id) {
        int index = 0;
        for (TableRow row : tableRows) {
            if (row.recordId == null) continue;
            if (row.recordId.equals(id)) return index;
            index++;
        }
        return -1;
    }

    private int draggableRowCount() {
        int count = 0;
        for (TableRow row : tableRows) if (row.recordId != null) count++;
        return count;
    }

    @Override
    protected void closePressed() {
        if (mode == Mode.INTRO) {
            controller.setSkipIntro(introSkip != null && introSkip.checked);
        }
        controller.windowClosed();
    }

    private enum Mode { INTRO, LIST, EDITOR }

    private static final class IntroText extends FlexComponent {
        private static final int HORIZONTAL_MARGIN = 24;
        private static final int LINE_HEIGHT = 16;
        private final String label;
        private final Runnable action;
        private final List<String> lines = new ArrayList<>();

        private IntroText(String text, Runnable action) {
            super("keybinder.intro.text");
            this.label = text == null ? "" : text;
            this.action = action;
            lines.add(this.label);
            setSize(Math.max(1, this.label.length() * 7), LINE_HEIGHT);
        }

        private void layout(int availableWidth) {
            int lineWidth = Math.max(1, availableWidth - HORIZONTAL_MARGIN * 2);
            lines.clear();
            if (label.isEmpty()) {
                lines.add("");
            } else {
                StringBuilder current = new StringBuilder();
                for (String word : label.split("\\s+")) {
                    String candidate = current.length() == 0
                            ? word : current.toString() + " " + word;
                    if (current.length() > 0 && text.getWidth(candidate) > lineWidth) {
                        lines.add(current.toString());
                        current.setLength(0);
                        current.append(word);
                    } else {
                        current.setLength(0);
                        current.append(candidate);
                    }
                }
                if (current.length() > 0) lines.add(current.toString());
            }
            setSize(availableWidth, Math.max(LINE_HEIGHT, lines.size() * LINE_HEIGHT));
        }

        @Override
        protected void renderComponent(Queue queue, float ignoredAlpha) {
            int baselineOffset = Math.max(text.getAscent(),
                    (LINE_HEIGHT + text.getAscent()) / 2 - 1);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                text.moveTo(x + HORIZONTAL_MARGIN,
                        y + baselineOffset + i * LINE_HEIGHT);
                text.paint(queue, line, 1.0f, 1.0f, 1.0f, 1.0f);
            }
        }

        @Override
        void leftPressed(int mouseX, int mouseY, int clickCount) {
            if (action != null) action.run();
        }
    }

    private static final class IntroBanner extends FlexComponent {
        private final ResourceTexture texture = KeybinderTextureFactory.load("intro-banner.png");

        private IntroBanner() {
            super("keybinder.intro.banner");
        }

        private void layout(int availableWidth) {
            setSize(availableWidth, KeybinderIntroLayout.bannerHeight(availableWidth));
        }

        @Override
        protected void renderComponent(Queue queue, float ignoredAlpha) {
            // Wurm's drawTexture uses a fixed 0..256 UV space. The bundled
            // banner is power-of-two RGBA; draw the complete texture directly
            // into the full-width component, as in the proven 0.5.0 layout.
            drawTexture(queue, texture, 1f, 1f, 1f, 1f,
                    x, y, width, height, 0, 0, 256, 256);
        }
    }
}
