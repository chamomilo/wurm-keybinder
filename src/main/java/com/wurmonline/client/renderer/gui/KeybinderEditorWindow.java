package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.backend.Queue;
import org.keybinder.wurm.KeybinderMod;
import org.keybinder.wurm.catalog.VanillaCatalogStepFactory;
import org.keybinder.wurm.catalog.VanillaKeybindCatalog;
import org.keybinder.wurm.catalog.InputKeyCatalog;
import org.keybinder.wurm.command.ExactObjectTarget;
import org.keybinder.wurm.command.NearbyTypeTarget;
import org.keybinder.wurm.command.TargetCodec;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.ActivateToolStep;
import org.keybinder.wurm.model.ConsoleCommandStep;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.model.KeybindVariant;
import org.keybinder.wurm.model.SmartImproveStep;
import org.keybinder.wurm.model.StepKind;
import org.keybinder.wurm.model.TargetKind;
import org.keybinder.wurm.model.TargetSpec;
import org.keybinder.wurm.model.VanillaActionStep;
import org.keybinder.wurm.queue.QueueCost;
import org.keybinder.wurm.ui.KeybindEditorController;
import org.keybinder.wurm.ui.LocalizedLayout;
import org.keybinder.wurm.ui.RowInsertionCalculator;
import org.keybinder.wurm.i18n.Messages;

import java.util.ArrayList;
import java.util.List;

public final class KeybinderEditorWindow extends WWindow implements ButtonListener, InputFieldListener {
    /*
     * The action-chain row has eight independently sized columns. 560 px was
     * enough to create the controls, but not enough to display Action and
     * Target without clipping. The primary Keybinder window expands to this
     * width when it enters constructor mode.
     */
    static final int MIN_WIDTH = 760;
    private static final int WINDOW_CHROME = 32;
    private static final int ACTION_NAME_MIN_WIDTH = 145;
    private static final int HELP_WIDTH = 22;
    private static final int CAPTURE_WIDTH = 24;
    private static final int SEPARATOR_WIDTH = 9;
    private static final int COLUMN_GAP = 8;
    private static final int SECTION_GAP = 7;
    private static final int ACTION_ROW_INDENT = 24;
    private static final int TITLE_TO_SUBNAME_GAP = 12;
    private static final String[] BASE_TARGET_OPTIONS = {
            "hover", "body", "tool", "selected", "current ride", "tiles",
            "toolbelt", "equipment", "exact object", "nearby", "nearby by type"
    };
    private static final VanillaKeybindCatalog VANILLA_CATALOG = new VanillaKeybindCatalog();
    private static final VanillaCatalogStepFactory VANILLA_STEPS =
            new VanillaCatalogStepFactory();
    private static final int VANILLA_TYPE_OFFSET = 4;
    private static final String[] ACTIVATE_TARGET_OPTIONS = {
            "hand", "hover", "toolbelt", "equipment", "exact object"
    };
    private static final String[] VANILLA_ACTIVATE_TARGET_OPTIONS = {
            "hover", "hand", "toolbelt", "equipment", "exact object"
    };
    private static final String[] SMART_IMPROVE_TARGET_OPTIONS = {
            "hover", "tool", "selected", "toolbelt", "equipment", "exact object"
    };
    private static final InputKeyCatalog KEY_CATALOG = InputKeyCatalog.system();
    private static final String[] MOD_OPTIONS = {
            "", "SHIFT", "CTRL", "ALT", "CTRL+SHIFT", "CTRL+ALT", "SHIFT+ALT",
            "CTRL+SHIFT+ALT"
    };
    private static final int MAX_VARIANTS = 15;

    private static String[] stepTypeOptions() {
        List<VanillaKeybindCatalog.Category> categories = VANILLA_CATALOG.getCategories();
        String[] options = new String[VANILLA_TYPE_OFFSET + categories.size()];
        options[0] = Messages.text("editor.step_type.activate");
        options[1] = Messages.text("editor.step_type.improve");
        options[2] = Messages.text("editor.step_type.console");
        options[3] = Messages.text("editor.step_type.custom");
        for (int i = 0; i < categories.size(); i++)
            options[VANILLA_TYPE_OFFSET + i] =
                    Messages.text("editor.step_type.vanilla", categories.get(i).getDisplayName());
        return options;
    }

    static int stepTypeWidth() { return maximumOptionWidth(stepTypeOptions()); }

    private final KeybindEditorController controller;
    private final String recordId;
    private final String[] keyOptions;
    private final WurmInputField nameField;
    private final WurmArrayPanel<FlexComponent> keySelectors;
    private final WurmArrayPanel<FlexComponent> creationFields;
    private final MetadataValueField createdByValue;
    private final MetadataValueField createdOnValue;
    private final WurmLabel createdByCaption;
    private final WurmLabel createdOnCaption;
    private final WButton refreshCreatedBy;
    private final WButton refreshCreatedOn;
    private String createdByUser;
    private String createdOnServer;
    private final WurmLabel keyLabel;
    private final WurmLabel modifierLabel;
    private final WurmDropDown keyDropDown;
    private final WurmDropDown modifierDropDown;
    private final int keySelectorWidth;
    private final int modifierSelectorWidth;
    private final WurmBorderPanel root;
    private final WurmArrayPanel<FlexComponent> top;
    private final WButton addVariant;
    private final List<VariantZone> zones = new ArrayList<>();
    private String activeVariantId;
    private final WurmArrayPanel<FlexComponent> actions;
    private final List<ActionRow> rows = new ArrayList<>();
    private final WButton done;
    private final WButton cancel;
    private final WurmArrayPanel<FlexComponent> footer;
    private final WurmArrayPanel<FlexComponent> footerArea;
    private ActionRow pendingTargetRow;
    private ActionRow selectedRow;
    private ActionRow captureTargetRow;
    private int lastLayoutWidth = -1;
    private ActionRow draggedRow;
    private int dragPressY;
    private int dragInsertion = -1;
    private boolean rowDragging;
    private VariantZone draggedZone;
    private int zoneDragPressY;
    private int zoneDragInsertion = -1;
    private boolean zoneDragging;

    public KeybinderEditorWindow(KeybindEditorController controller, String recordId) {
        super("keybinder.editor", true);
        this.controller = controller;
        this.recordId = recordId;
        this.keyOptions = KEY_CATALOG.displayOptions();
        setTitle(Messages.text("window.editor.title"));
        KeybindRecord record = controller.getRecord(recordId);
        createdByUser = record == null ? controller.currentUser() : record.getCreatedByUser();
        createdOnServer = record == null ? controller.currentServer() : record.getCreatedOnServer();

        root = new WurmBorderPanel("keybinder.editor.root");
        top = new WurmArrayPanel<>("keybinder.editor.top", WurmArrayPanel.DIR_VERTICAL, true);
        top.addComponent(verticalSpacer(3));
        top.addComponent(new WurmLabel(
                Messages.text("editor.instructions")));
        top.addComponent(new WurmLabel(
                Messages.text("editor.long_press")));
        top.addComponent(verticalSpacer(SECTION_GAP));
        top.addComponent(new WurmLabel(Messages.text("editor.name")));
        nameField = new WurmInputField("keybinder.editor.name", this);
        nameField.prompt = "";
        nameField.setText(record == null ? Messages.text("editor.new") : record.getName());
        nameField.setMaxInput(80);
        top.addComponent(nameField);
        top.addComponent(verticalSpacer(SECTION_GAP));

        creationFields = new WurmArrayPanel<>("keybinder.editor.creation", WurmArrayPanel.DIR_HORIZONTAL);
        creationFields.componentWidthOffset = COLUMN_GAP;
        createdByValue = new MetadataValueField("keybinder.editor.createdBy", createdByUser);
        createdOnValue = new MetadataValueField("keybinder.editor.createdOn", createdOnServer);
        refreshCreatedBy = new KeybinderGlyphButton(KeybinderGlyphButton.Kind.REFRESH, this,
                Messages.text("editor.current_user"));
        refreshCreatedOn = new KeybinderGlyphButton(KeybinderGlyphButton.Kind.REFRESH, this,
                Messages.text("editor.current_server"));
        createdByCaption = new WurmLabel(Messages.text("editor.created_user"));
        createdOnCaption = new WurmLabel(Messages.text("editor.created_server"));
        creationFields.addComponent(createdByCaption);
        creationFields.addComponent(refreshCreatedBy);
        creationFields.addComponent(createdByValue);
        creationFields.addComponent(createdOnCaption);
        creationFields.addComponent(refreshCreatedOn);
        creationFields.addComponent(createdOnValue);
        top.addComponent(creationFields);
        top.addComponent(verticalSpacer(SECTION_GAP));

        keySelectors = new WurmArrayPanel<>("keybinder.editor.key", WurmArrayPanel.DIR_HORIZONTAL);
        keySelectors.componentWidthOffset = COLUMN_GAP;
        keyLabel = new WurmLabel(Messages.text("editor.key"));
        modifierLabel = new WurmLabel(Messages.text("editor.modifier"));
        keyDropDown = new WurmDropDown("keybinder.editor.key.value",
                optionFor(keyOptions, KEY_CATALOG.displayName(baseKey(
                        record == null ? "" : record.getKey()))), keyOptions);
        modifierDropDown = new WurmDropDown("keybinder.editor.key.modifier",
                optionFor(MOD_OPTIONS, modifier(record == null ? "" : record.getKey())), MOD_OPTIONS);
        keySelectorWidth = maximumOptionWidth(keyOptions);
        modifierSelectorWidth = maximumOptionWidth(MOD_OPTIONS);
        String storedKey = record == null ? "" : record.getKey();
        keyDropDown.setValue(optionFor(
                keyOptions, KEY_CATALOG.displayName(baseKey(storedKey))));
        modifierDropDown.setValue(optionFor(MOD_OPTIONS, modifier(storedKey)));
        keySelectors.addComponent(keyLabel);
        keySelectors.addComponent(keyDropDown);
        keySelectors.addComponent(modifierLabel);
        keySelectors.addComponent(modifierDropDown);
        top.addComponent(keySelectors);
        top.addComponent(verticalSpacer(SECTION_GAP));
        addVariant = new WButton(Messages.text("editor.add_variant"), this);

        actions = new WurmArrayPanel<>("keybinder.editor.actions", WurmArrayPanel.DIR_VERTICAL, true);
        done = new WButton(Messages.text("common.done"), this);
        cancel = new WButton(Messages.text("common.cancel"), this);
        footer = new WurmArrayPanel<>("keybinder.editor.footer", WurmArrayPanel.DIR_HORIZONTAL);
        footer.componentWidthOffset = COLUMN_GAP;
        footer.addComponent(done);
        footer.addComponent(cancel);
        footerArea = new WurmArrayPanel<>("keybinder.editor.footer.area",
                WurmArrayPanel.DIR_VERTICAL, true);
        footerArea.addComponent(verticalSpacer(SECTION_GAP));
        footerArea.addComponent(footer);
        footerArea.addComponent(verticalSpacer(3));
        root.setComponent(top, WurmBorderPanel.NORTH);
        root.setComponent(new WurmScrollPanel("keybinder.editor.scroll", actions, false, true),
                WurmBorderPanel.CENTER);
        root.setComponent(footerArea, WurmBorderPanel.SOUTH);
        setComponent(root);
        setInitialSize(MIN_WIDTH, 430, true);

        List<KeybindVariant> initialVariants = record == null
                ? java.util.Collections.singletonList(new KeybindVariant(null, "", null))
                : record.getVariants();
        activeVariantId = record == null
                ? initialVariants.get(0).getId() : record.getActiveVariantId();
        for (int i = 0; i < initialVariants.size(); i++)
            zones.add(new VariantZone(initialVariants.get(i), i));
        refreshRows();
        selectedRow = rows.get(0);
        rebuildActions();
        applyLayout();
    }

    public void addCapturedAction(ActionStep step) {
        KeybinderMod.deferUi(() -> {
            ActionRow destination = captureTargetRow;
            captureTargetRow = null;
            if (destination != null && rows.contains(destination)) {
                selectedRow = destination;
                destination.setActionStep(step);
            } else {
                VariantZone zone = zones.get(0);
                selectedRow = new ActionRow(zone, step);
                zone.rows.add(selectedRow);
                refreshRows();
            }
            rebuildActions();
        });
    }

    @Override
    public void buttonPressed(WButton button) {
        ActionRow owner = ownerOf(button);
        if (owner != null) selectedRow = owner;
    }

    @Override
    public void buttonClicked(WButton button) {
        if (button == done) {
            save();
            return;
        }
        if (button == cancel) {
            controller.closeEditor();
            return;
        }
        if (button == refreshCreatedBy) {
            String current = controller.currentUser();
            if (!current.equals(createdByUser)) {
                createdByUser = current;
                updateMetadataField(createdByValue, current);
            }
            return;
        }
        if (button == refreshCreatedOn) {
            String current = controller.currentServer();
            if (!current.equals(createdOnServer)) {
                createdOnServer = current;
                updateMetadataField(createdOnValue, current);
            }
            return;
        }
        if (button == addVariant) {
            if (zones.size() >= MAX_VARIANTS) {
                controller.showEditorError(Messages.text("editor.max_variants", MAX_VARIANTS));
                return;
            }
            VariantZone zone = new VariantZone(new KeybindVariant(null, "", null), zones.size());
            zones.add(zone);
            refreshRows();
            selectedRow = zone.rows.get(0);
            rebuildActions();
            return;
        }
        for (VariantZone zone : new ArrayList<VariantZone>(zones)) {
            if (button == zone.collapse) {
                zone.collapsed = !zone.collapsed;
                zone.rebuildPanel();
                applyActionLayout(Math.max(300, width - WINDOW_CHROME));
                return;
            }
            if (button == zone.remove) {
                if (zone.defaultZone || zones.size() <= 1) return;
                if (zone.id.equals(activeVariantId)) activeVariantId = zones.get(0).id;
                zones.remove(zone);
                refreshRows();
                selectedRow = rows.isEmpty() ? null : rows.get(0);
                rebuildActions();
                return;
            }
        }
        for (int i = 0; i < rows.size(); i++) {
            ActionRow row = rows.get(i);
            if (button == row.capture) {
                captureTargetRow = row;
                if (row.kind() == StepKind.CUSTOM_ACTION) controller.beginCapture();
                else {
                    pendingTargetRow = row;
                    controller.requestTargetSelection("exact object");
                }
                return;
            }
            if (button == row.add) {
                final VariantZone zone = row.zone;
                final int index = zone.rows.indexOf(row) + 1;
                KeybinderMod.deferUi(() -> {
                    ActionRow inserted = new ActionRow(zone, null);
                    zone.rows.add(index, inserted);
                    refreshRows();
                    selectedRow = inserted;
                    rebuildActions();
                });
                return;
            }
            if (button == row.remove) {
                KeybinderMod.deferUi(() -> {
                    VariantZone zone = row.zone;
                    int oldIndex = zone.rows.indexOf(row);
                    zone.rows.remove(row);
                    if (zone.rows.isEmpty()) zone.rows.add(new ActionRow(zone, null));
                    refreshRows();
                    if (selectedRow == row)
                        selectedRow = zone.rows.get(Math.min(oldIndex, zone.rows.size() - 1));
                    rebuildActions();
                });
                return;
            }
        }
    }

    private ActionRow ownerOf(WButton button) {
        for (ActionRow row : rows)
            if (button == row.add || button == row.remove || button == row.capture) return row;
        return null;
    }

    private void save() {
        try {
            applyCompletedTargetSelection(false);
            List<KeybindVariant> variants = new ArrayList<KeybindVariant>();
            for (VariantZone zone : zones) variants.add(zone.toVariant());
            controller.saveVariants(recordId, nameField.getText(), selectedKey(),
                    variants,
                    activeVariantId, createdByUser, createdOnServer);
        } catch (RuntimeException e) {
            controller.showEditorError(Messages.text("editor.cannot_save",
                    e.getMessage() == null ? Messages.text("editor.invalid_value") : e.getMessage()));
        }
    }

    @Override
    public void gameTick() {
        super.gameTick();
        updateEditorState();
    }

    FlexComponent getEmbeddedContent() {
        return root;
    }

    void embeddedTick(int hostWidth, int hostHeight) {
        // This object owns the editor controls and listeners but its content is
        // displayed by the one persistent Keybinder window.
        width = hostWidth;
        height = hostHeight;
        updateEditorState();
    }

    private void updateEditorState() {
        applyLayout();
        for (ActionRow row : new ArrayList<>(rows)) {
            int typeValue = row.type.getValue();
            if (typeValue != row.lastTypeValue) {
                selectedRow = row;
                row.lastTypeValue = typeValue;
                row.resetForType();
                row.rebuildPanel();
                applyActionLayout(Math.max(300, width - WINDOW_CHROME));
                row.zone.updateActionLimit();
            }
            row.updateActionName();
            if (row.kind() == StepKind.CONSOLE_COMMAND) continue;
            if (row.isVanilla()) {
                int value = row.vanillaAction.getValue();
                if (value != row.lastVanillaValue) {
                    selectedRow = row;
                    row.lastVanillaValue = value;
                    row.createTarget();
                    row.rebuildPanel();
                    applyActionLayout(Math.max(300, width - WINDOW_CHROME));
                    row.zone.updateActionLimit();
                    continue;
                }
                if (!row.vanillaUsesTarget()) continue;
            }
            int value = row.target.getValue();
            if (value == row.lastDropdownValue) continue;
            selectedRow = row;
            row.lastDropdownValue = value;
            if (row.hasConcreteTarget && value == 0) continue;
            int baseIndex = row.hasConcreteTarget ? value - 1 : value;
            String[] baseOptions = row.baseTargetOptions();
            if (baseIndex < 0 || baseIndex >= baseOptions.length) continue;
            String selected = baseOptions[baseIndex];
            if ("toolbelt".equals(selected) || "equipment".equals(selected)
                    || "tiles".equals(selected) || "exact object".equals(selected)
                    || "nearby by type".equals(selected)) {
                pendingTargetRow = row;
                if ("toolbelt".equals(selected)) {
                    // A concrete @tbN value belongs only to the completed
                    // one-shot selection. Clear it before every new request so
                    // choosing "Toolbelt slot..." again cannot silently retain
                    // and compile the previously selected slot. Keep the menu
                    // selection visible while the click capture is pending,
                    // but use a valid unresolved model token for previews.
                    row.selectedTarget = "unresolved";
                }
                controller.requestTargetSelection(selected);
            } else {
                row.selectedTarget = "nearby".equals(selected) ? "@nearby4" : selected;
            }
        }
        String selected = controller.consumeSelectedTarget();
        if (selected != null && pendingTargetRow != null) {
            applyCompletedTargetSelection(selected, true);
        }
    }

    private void applyCompletedTargetSelection(boolean rebuild) {
        String selected = controller.consumeSelectedTarget();
        if (selected != null) applyCompletedTargetSelection(selected, rebuild);
    }

    private void applyCompletedTargetSelection(String selected, boolean rebuild) {
        ActionRow targetRow = pendingTargetRow;
        if (targetRow == null) return;
        pendingTargetRow = null;
        // Update editor state synchronously. Rebuilding Wurm controls is deferred,
        // but Save must never observe the old target in the intervening frame.
        targetRow.selectedTarget = selected;
        if (rebuild) KeybinderMod.deferUi(targetRow::refreshSelectedTarget);
    }

    @Override
    void setSize(int requestedWidth, int requestedHeight) {
        // Clamp inside the resize operation. Correcting the width from gameTick
        // fights WWindow's active resize/hover state and makes custom content flicker.
        super.setSize(Math.max(requestedWidth, MIN_WIDTH), requestedHeight);
    }

    private void rebuildActions() {
        actions.removeAllComponents();
        for (int i = 0; i < zones.size(); i++) {
            VariantZone zone = zones.get(i);
            zone.displayIndex = i;
            zone.rebuildPanel();
            actions.addComponent(zone.panel);
            actions.addComponent(verticalSpacer(SECTION_GAP));
        }
        actions.addComponent(addVariant);
        actions.componentResized();
        applyActionLayout(Math.max(300, width - WINDOW_CHROME));
    }

    private void applyLayout() {
        if (lastLayoutWidth == width || zones.isEmpty()) return;
        lastLayoutWidth = width;
        int contentWidth = Math.max(300, width - WINDOW_CHROME);
        nameField.setSize(contentWidth, nameField.height);
        keyDropDown.setSize(keySelectorWidth, keyDropDown.height);
        modifierDropDown.setSize(modifierSelectorWidth, modifierDropDown.height);
        int metadataFixed = createdByCaption.width + createdOnCaption.width
                + refreshCreatedBy.width + refreshCreatedOn.width + COLUMN_GAP * 5;
        int metadataValueWidth = Math.max(120, (contentWidth - metadataFixed) / 2);
        createdByValue.setSize(metadataValueWidth, createdByValue.height);
        createdOnValue.setSize(metadataValueWidth, createdOnValue.height);
        creationFields.componentResized();
        int footerButtonWidth = Math.max(80, (contentWidth - COLUMN_GAP) / 2);
        done.setSize(footerButtonWidth, done.height);
        cancel.setSize(contentWidth - COLUMN_GAP - footerButtonWidth, cancel.height);
        applyActionLayout(contentWidth);
        keySelectors.componentResized();
        footer.componentResized();
        footerArea.componentResized();
        top.componentResized();
        root.componentResized();
    }

    private void applyActionLayout(int contentWidth) {
        for (VariantZone zone : zones) zone.resize(contentWidth);
        addVariant.setSize(contentWidth, addVariant.height);
        actions.componentResized();
    }

    private void refreshRows() {
        rows.clear();
        for (VariantZone zone : zones) rows.addAll(zone.rows);
    }

    @Override protected void closePressed() { controller.closeEditor(); }
    @Override public void handleInput(String input) {}
    @Override public void handleInputChanged(WurmInputField field, String input) {}
    @Override public void handleEscape(WurmInputField field) {}

    private String selectedKey() {
        String key = KEY_CATALOG.persistedName(keyOptions[keyDropDown.getValue()]);
        String mod = MOD_OPTIONS[modifierDropDown.getValue()];
        if (key.isEmpty()) return "";
        return InputKeyCatalog.normalizeChord(mod.isEmpty() ? key : mod + "+" + key);
    }

    private static String baseKey(String value) {
        return InputKeyCatalog.baseKey(value);
    }

    private static String modifier(String value) {
        return InputKeyCatalog.modifiers(value);
    }

    private static String safe(String value) { return value == null ? "" : value; }

    private static void updateMetadataField(MetadataValueField field, String value) {
        field.setValue(value);
    }

    /**
     * Read-only field-shaped metadata. Unlike WurmInputField it never accepts
     * focus, selection, cursor placement, mouse events, or tooltip picking.
     */
    private static final class MetadataValueField extends FlexComponent {
        private String value;

        private MetadataValueField(String name, String value) {
            super(name);
            this.value = safe(value);
            setSize(120, 20);
        }

        private void setValue(String value) {
            this.value = safe(value);
        }

        @Override
        protected void renderComponent(Queue queue, float ignoredAlpha) {
            drawTexture(queue, panelTexture, r, g, b, 1.0f,
                    x, y, 8, height, 64, 48, 8, 16);
            drawTexture(queue, panelTexture, r, g, b, 1.0f,
                    x + width - 8, y, 8, height, 88, 48, 8, 16);
            if (width > 16)
                drawTexTilingH(queue, panelTextureTilingH, r, g, b, 1.0f,
                        x + 8, y, width - 16, height, 109, 16);
            text.moveTo(x + 4, y + Math.max(text.getAscent(), (height + text.getAscent()) / 2 - 1));
            text.paint(queue, value, 1.0f, 1.0f, 1.0f, 1.0f);
        }
    }

    private static FlexComponent verticalSpacer(int height) {
        FlexComponent spacer = new WurmLabel("");
        spacer.setSize(1, height);
        return spacer;
    }

    private static WurmLabel horizontalSpacer(int width) {
        WurmLabel spacer = new WurmLabel("");
        spacer.setSize(width, 1);
        return spacer;
    }

    private static boolean inside(FlexComponent component, int mouseX, int mouseY) {
        return mouseX >= component.x && mouseX < component.x + component.width
                && mouseY >= component.y && mouseY < component.y + component.height;
    }

    private static int optionFor(String[] options, String value) {
        for (int i = 0; i < options.length; i++)
            if (options[i].equalsIgnoreCase(value)) return i;
        return 0;
    }

    private static int maximumOptionWidth(String[] options) {
        return LocalizedLayout.maximumOptionWidth(
                options, 30, text -> new WurmLabel(text).width);
    }

    private static boolean concreteTarget(String target) {
        return target != null && (target.startsWith("@tb") || target.startsWith("@eq")
                || ExactObjectTarget.isExact(target)
                || NearbyTypeTarget.isNearbyType(target)
                || target.equals("tile") || target.startsWith("tile_") || target.equals("area"));
    }

    private void beginRowDrag(ActionRow row, int mouseY) {
        draggedRow = row;
        dragPressY = mouseY;
        dragInsertion = -1;
        rowDragging = false;
    }

    private void updateRowDrag(int mouseY) {
        if (draggedRow == null) return;
        if (!rowDragging && Math.abs(mouseY - dragPressY) < 4) return;
        rowDragging = true;
        List<Integer> tops = new ArrayList<>();
        List<Integer> heights = new ArrayList<>();
        for (ActionRow row : draggedRow.zone.rows) {
            tops.add(row.panel.y);
            heights.add(row.panel.height);
        }
        dragInsertion = RowInsertionCalculator.insertionIndex(mouseY, tops, heights);
    }

    private void finishRowDrag() {
        ActionRow moving = draggedRow;
        int insertion = dragInsertion;
        boolean apply = rowDragging && moving != null && insertion >= 0;
        draggedRow = null;
        dragInsertion = -1;
        rowDragging = false;
        if (!apply) return;
        List<ActionRow> zoneRows = moving.zone.rows;
        int source = zoneRows.indexOf(moving);
        int destination = RowInsertionCalculator.destinationAfterRemoval(source, insertion);
        if (source < 0 || source == destination) return;
        zoneRows.remove(source);
        zoneRows.add(Math.max(0, Math.min(destination, zoneRows.size())), moving);
        refreshRows();
        selectedRow = moving;
        rebuildActions();
    }

    private void beginZoneDrag(VariantZone zone, int mouseY) {
        if (zone.defaultZone) return;
        draggedZone = zone;
        zoneDragPressY = mouseY;
        zoneDragInsertion = -1;
        zoneDragging = false;
    }

    private void updateZoneDrag(int mouseY) {
        if (draggedZone == null) return;
        if (!zoneDragging && Math.abs(mouseY - zoneDragPressY) < 4) return;
        zoneDragging = true;
        List<Integer> tops = new ArrayList<Integer>();
        List<Integer> heights = new ArrayList<Integer>();
        for (VariantZone zone : zones) {
            if (zone.defaultZone) continue;
            tops.add(zone.panel.y);
            heights.add(zone.panel.height);
        }
        zoneDragInsertion = RowInsertionCalculator.insertionIndex(mouseY, tops, heights);
    }

    private void finishZoneDrag() {
        VariantZone moving = draggedZone;
        int insertion = zoneDragInsertion;
        boolean apply = zoneDragging && moving != null && insertion >= 0;
        draggedZone = null;
        zoneDragInsertion = -1;
        zoneDragging = false;
        if (!apply) return;

        List<VariantZone> alternatives = new ArrayList<VariantZone>();
        for (VariantZone zone : zones)
            if (!zone.defaultZone) alternatives.add(zone);
        int source = alternatives.indexOf(moving);
        int destination = RowInsertionCalculator.destinationAfterRemoval(source, insertion);
        if (source < 0 || source == destination) return;
        zones.remove(moving);
        zones.add(1 + Math.max(0, Math.min(destination, zones.size() - 1)), moving);
        rebuildActions();
    }

    private static String targetDisplay(String target) {
        if (target.startsWith("@tb"))
            return Messages.text("target.slot.toolbelt", target.substring(3));
        if (target.startsWith("@eq"))
            return Messages.text("target.slot.equipment", target.substring(3));
        if (ExactObjectTarget.isExact(target)) return ExactObjectTarget.display(target);
        if (NearbyTypeTarget.isNearbyType(target)) return target;
        if (target.equals("tile")) return Messages.text("target.tile", "C");
        if (target.startsWith("tile_"))
            return Messages.text("target.tile", target.substring(5).toUpperCase());
        if (target.equals("area")) return Messages.text("target.area");
        return targetLabel(target);
    }

    private static String targetLabel(String token) {
        if ("hover".equals(token)) return Messages.text("target.hover");
        if ("body".equals(token)) return Messages.text("target.body");
        if ("tool".equals(token)) return Messages.text("target.tool");
        if ("selected".equals(token)) return Messages.text("target.selected");
        if ("current ride".equals(token)) return Messages.text("target.current_ride");
        if ("tiles".equals(token)) return Messages.text("target.tiles");
        if ("toolbelt".equals(token)) return Messages.text("target.toolbelt");
        if ("equipment".equals(token)) return Messages.text("target.equipment");
        if ("exact object".equals(token)) return Messages.text("target.exact_object");
        if ("nearby".equals(token)) return Messages.text("target.nearby");
        if ("nearby by type".equals(token)) return Messages.text("target.nearby_type");
        if ("hand".equals(token)) return Messages.text("target.hand");
        return token;
    }

    private static String[] targetOptions(String target, String[] baseOptions) {
        if (!concreteTarget(target)) {
            String[] labels = new String[baseOptions.length];
            for (int i = 0; i < baseOptions.length; i++) labels[i] = targetLabel(baseOptions[i]);
            return labels;
        }
        String[] options = new String[baseOptions.length + 1];
        options[0] = targetDisplay(target);
        for (int i = 0; i < baseOptions.length; i++)
            options[i + 1] = targetLabel(baseOptions[i]);
        return options;
    }

    private final class VariantZone {
        private final String id;
        private final boolean defaultZone;
        private final ZonePanel panel;
        private final WurmArrayPanel<FlexComponent> titleRow =
                new WurmArrayPanel<>("keybinder.variant.title", WurmArrayPanel.DIR_HORIZONTAL);
        private final KeybinderGlyphButton collapse = new KeybinderGlyphButton(
                KeybinderGlyphButton.Kind.MINUS_BOX, KeybinderEditorWindow.this,
                Messages.text("editor.variant.collapse"));
        private final ZoneDragLabel title = new ZoneDragLabel("");
        private final WurmLabel titleToSubNameGap = horizontalSpacer(TITLE_TO_SUBNAME_GAP);
        private final ZoneDragLabel subNameLabel =
                new ZoneDragLabel(Messages.text("editor.variant.subname"));
        private final WurmInputField subName =
                new WurmInputField("keybinder.variant.subname", KeybinderEditorWindow.this);
        private final KeybinderGlyphButton remove = new KeybinderGlyphButton(
                KeybinderGlyphButton.Kind.CLOSE_BOX, KeybinderEditorWindow.this,
                Messages.text("editor.variant.remove"));
        private final WurmLabel actionLimit = new WurmLabel("");
        private final HeaderRow header;
        private final List<ActionRow> rows = new ArrayList<ActionRow>();
        private final List<WurmArrayPanel<FlexComponent>> indentedRows =
                new ArrayList<WurmArrayPanel<FlexComponent>>();
        private WurmArrayPanel<FlexComponent> indentedHeader;
        private boolean collapsed;
        private int displayIndex;

        private VariantZone(KeybindVariant variant, int displayIndex) {
            this.id = variant.getId();
            this.defaultZone = displayIndex == 0;
            this.displayIndex = displayIndex;
            this.panel = new ZonePanel();
            this.header = new HeaderRow(this);
            titleRow.componentWidthOffset = COLUMN_GAP;
            subName.prompt = "";
            subName.setMaxInput(40);
            subName.setText(variant.getSubName());
            remove.setEnabled(!defaultZone);
            for (KeybindStep step : variant.getSteps()) rows.add(new ActionRow(this, step));
            if (rows.isEmpty()) rows.add(new ActionRow(this, null));
            rebuildPanel();
        }

        private void rebuildPanel() {
            collapse.setKind(collapsed
                    ? KeybinderGlyphButton.Kind.PLUS_BOX
                    : KeybinderGlyphButton.Kind.MINUS_BOX);
            collapse.setHoverString(collapsed
                    ? Messages.text("editor.variant.expand")
                    : Messages.text("editor.variant.collapse"));
            String titleText = defaultZone ? Messages.text("editor.variant.default")
                    : Messages.text("editor.variant.number", displayIndex);
            title.setLabel(titleText);
            WurmLabel titleMeasure = new WurmLabel(titleText);
            title.setSize(titleMeasure.width, titleMeasure.height);
            panel.removeAllComponents();
            titleRow.removeAllComponents();
            titleRow.addComponent(collapse);
            if (!defaultZone) titleRow.addComponent(remove);
            titleRow.addComponent(title);
            titleRow.addComponent(titleToSubNameGap);
            titleRow.addComponent(subNameLabel);
            titleRow.addComponent(subName);
            panel.addComponent(titleRow);
            indentedRows.clear();
            indentedHeader = null;
            if (!collapsed) {
                panel.addComponent(actionLimit);
                indentedHeader = indented(header.panel);
                panel.addComponent(indentedHeader);
                for (ActionRow row : rows) {
                    row.rebuildPanel();
                    WurmArrayPanel<FlexComponent> inset = indented(row.panel);
                    indentedRows.add(inset);
                    panel.addComponent(inset);
                }
            }
            updateActionLimit();
            panel.componentResized();
        }

        private void resize(int contentWidth) {
            panel.setSize(contentWidth, panel.height);
            int componentCount = defaultZone ? 5 : 6;
            int fixed = collapse.width + title.width + titleToSubNameGap.width
                    + subNameLabel.width + (defaultZone ? 0 : remove.width)
                    + COLUMN_GAP * (componentCount - 1);
            subName.setSize(Math.max(120, contentWidth - fixed), subName.height);
            if (!collapsed) {
                actionLimit.setSize(contentWidth, actionLimit.height);
                int actionWidth = Math.max(1, contentWidth - ACTION_ROW_INDENT);
                header.resize(actionWidth);
                for (ActionRow row : rows) row.resize(actionWidth);
            }
            panel.componentResized();
            titleRow.componentResized();
            if (!collapsed) {
                if (indentedHeader != null) {
                    indentedHeader.componentResized();
                    header.panel.componentResized();
                }
                for (WurmArrayPanel<FlexComponent> inset : indentedRows)
                    inset.componentResized();
                // Parent and inset layout both move the row after its first
                // layout pass. Center controls only after those final moves.
                for (ActionRow row : rows) row.panel.componentResized();
            }
        }

        private WurmArrayPanel<FlexComponent> indented(FlexComponent component) {
            WurmArrayPanel<FlexComponent> inset =
                    new WurmArrayPanel<>("keybinder.variant.inset", WurmArrayPanel.DIR_HORIZONTAL);
            inset.componentWidthOffset = 0;
            WurmLabel spacer = new WurmLabel("");
            spacer.setSize(ACTION_ROW_INDENT, 1);
            component.setSize(Math.max(1, panel.width - ACTION_ROW_INDENT), component.height);
            inset.addComponent(spacer);
            inset.addComponent(component);
            return inset;
        }

        private void updateActionLimit() {
            List<KeybindStep> previewSteps = new ArrayList<KeybindStep>();
            for (ActionRow row : rows) {
                try {
                    previewSteps.add(row.toStep());
                } catch (RuntimeException ignored) {
                    // Editor rows can be temporarily incomplete while a
                    // one-shot target picker is open. Queue preview must never
                    // decode a menu label such as "toolbelt" on the HUD tick.
                    previewSteps.add(new ActionStep((short) 0,
                            TargetSpec.simple(TargetKind.UNRESOLVED)));
                }
            }
            KeybindRecord preview = new KeybindRecord("preview", "Preview", "", previewSteps);
            QueueCost cost = controller.getKeybindCost(preview);
            String length = cost.getKind() == QueueCost.Kind.FIXED
                    ? String.valueOf(cost.getValue())
                    : cost.getKind() == QueueCost.Kind.DYNAMIC
                    ? Messages.text("editor.queue.runtime") : Messages.text("editor.queue.unknown");
            actionLimit.setLabel(Messages.text(
                    "editor.queue.summary", controller.getQueueLimit(), length)
                    + (cost.getKind() == QueueCost.Kind.FIXED
                    && cost.getValue() > controller.getQueueLimit()
                    ? Messages.text("editor.queue.warning") : ""));
        }

        private KeybindVariant toVariant() {
            List<KeybindStep> result = new ArrayList<KeybindStep>();
            for (ActionRow row : rows) result.add(row.toStep());
            return new KeybindVariant(id, subName.getText(), result);
        }

        private final class ZonePanel extends WurmArrayPanel<FlexComponent> {
            private ZonePanel() {
                super("keybinder.variant.zone", WurmArrayPanel.DIR_VERTICAL, true);
            }

            @Override
            protected void renderComponent(Queue queue, float alpha) {
                fillRect(queue, defaultZone ? 0.16f : 0.12f, 0.20f, 0.24f,
                        0.78f, x, y, width, height);
                super.renderComponent(queue, alpha);
                if (zoneDragging && !defaultZone) {
                    int alternativeIndex = zones.indexOf(VariantZone.this) - 1;
                    if (zoneDragInsertion == alternativeIndex)
                        fillRect(queue, 0.92f, 0.77f, 0.42f, 1.0f, x, y, width, 2);
                    if (alternativeIndex + 1 == zones.size() - 1
                            && zoneDragInsertion == zones.size() - 1)
                        fillRect(queue, 0.92f, 0.77f, 0.42f, 1.0f,
                                x, y + height - 2, width, 2);
                }
            }

            @Override
            void leftPressed(int mouseX, int mouseY, int clickCount) {
                if (!inside(collapse, mouseX, mouseY)
                        && (defaultZone || !inside(remove, mouseX, mouseY))
                        && !inside(subName, mouseX, mouseY))
                    beginZoneDrag(VariantZone.this, mouseY);
                super.leftPressed(mouseX, mouseY, clickCount);
            }

            @Override
            void mouseDragged(int mouseX, int mouseY) {
                updateZoneDrag(mouseY);
                super.mouseDragged(mouseX, mouseY);
            }

            @Override
            void leftReleased(int mouseX, int mouseY) {
                finishZoneDrag();
                super.leftReleased(mouseX, mouseY);
            }
        }

        private final class ZoneDragLabel extends WurmLabel {
            private ZoneDragLabel(String text) {
                super(text);
            }

            @Override
            void leftPressed(int mouseX, int mouseY, int clickCount) {
                beginZoneDrag(VariantZone.this, mouseY);
                super.leftPressed(mouseX, mouseY, clickCount);
            }

            @Override
            void mouseDragged(int mouseX, int mouseY) {
                updateZoneDrag(mouseY);
                super.mouseDragged(mouseX, mouseY);
            }

            @Override
            void leftReleased(int mouseX, int mouseY) {
                finishZoneDrag();
                super.leftReleased(mouseX, mouseY);
            }
        }
    }

    private final class HeaderRow {
        private final WurmArrayPanel<FlexComponent> panel =
                new WurmArrayPanel<>("keybinder.action.header", WurmArrayPanel.DIR_HORIZONTAL);
        private final WurmLabel controls = new WurmLabel("");
        private final VerticalSeparator controlsSeparator = new VerticalSeparator();
        private final WurmLabel type = new WurmLabel(Messages.text("editor.step_type"));
        private final WurmLabel help = new WurmLabel("");
        private final WurmLabel capture = new WurmLabel("");
        private final WurmLabel action = new WurmLabel(Messages.text("editor.action"));
        private final VerticalSeparator targetSeparator = new VerticalSeparator();
        private final WurmLabel target = new WurmLabel(Messages.text("editor.target"));

        private final VariantZone zone;

        private HeaderRow(VariantZone zone) {
            this.zone = zone;
            panel.componentWidthOffset = COLUMN_GAP;
            panel.addComponent(controls);
            panel.addComponent(controlsSeparator);
            panel.addComponent(type);
            panel.addComponent(help);
            panel.addComponent(capture);
            panel.addComponent(action);
            panel.addComponent(targetSeparator);
            panel.addComponent(target);
        }

        private void resize(int contentWidth) {
            int controlsWidth = zone.rows.isEmpty() ? 100 : zone.rows.get(0).controls.width;
            int remaining = Math.max(240,
                    contentWidth - controlsWidth - stepTypeWidth() - HELP_WIDTH - CAPTURE_WIDTH
                            - SEPARATOR_WIDTH * 2 - COLUMN_GAP * 7);
            int actionWidth = Math.max(ACTION_NAME_MIN_WIDTH, remaining * 2 / 5);
            controls.setSize(controlsWidth, controls.height);
            controlsSeparator.setSize(SEPARATOR_WIDTH, controlsSeparator.height);
            type.setSize(stepTypeWidth(), type.height);
            help.setSize(HELP_WIDTH, help.height);
            capture.setSize(CAPTURE_WIDTH, capture.height);
            action.setSize(actionWidth, action.height);
            targetSeparator.setSize(SEPARATOR_WIDTH, targetSeparator.height);
            target.setSize(remaining - actionWidth, target.height);
            panel.componentResized();
        }
    }

    private final class ActionRow {
        private final VariantZone zone;
        private final SelectableActionPanel panel;
        private final WurmArrayPanel<FlexComponent> controls =
                new WurmArrayPanel<>("keybinder.action.controls", WurmArrayPanel.DIR_HORIZONTAL);
        private final WButton add = new KeybinderGlyphButton(
                KeybinderGlyphButton.Kind.PLUS_BOX, KeybinderEditorWindow.this,
                Messages.text("editor.add_step"));
        private final WButton remove = new KeybinderGlyphButton(
                KeybinderGlyphButton.Kind.CLOSE_BOX, KeybinderEditorWindow.this,
                Messages.text("editor.remove_step"));
        private final VerticalSeparator controlsSeparator = new VerticalSeparator();
        private final WurmDropDown type;
        private final WButton help = new WButton("?", KeybinderEditorWindow.this);
        private final WButton capture = new KeybinderGlyphButton(
                KeybinderGlyphButton.Kind.RECORD_DOT, KeybinderEditorWindow.this,
                Messages.text("editor.capture_step"));
        private final WurmLabel captureGap = horizontalSpacer(CAPTURE_WIDTH);
        private String actionIdValue = "";
        private final WurmInputField command =
                new WurmInputField("keybinder.command.value", KeybinderEditorWindow.this);
        private final SelectableActionLabel actionName;
        private WurmDropDown vanillaAction;
        private final VerticalSeparator targetSeparator = new VerticalSeparator();
        private SelectableTargetDropDown target;
        private int lastDropdownValue;
        private String selectedTarget;
        private String lastIdText = null;
        private boolean hasConcreteTarget;
        private int lastTypeValue;
        private int lastVanillaValue;

        private ActionRow(VariantZone zone, KeybindStep step) {
            this.zone = zone;
            panel = new SelectableActionPanel(this);
            actionName = new SelectableActionLabel(this);
            String selectedVanillaCommand = step instanceof VanillaActionStep
                    ? ((VanillaActionStep) step).getCommand()
                    : step instanceof ActionStep
                    ? vanillaCommandFor(((ActionStep) step).getActionId()) : null;
            int initialType = step instanceof ActivateToolStep ? 0
                    : step instanceof SmartImproveStep ? 1
                    : step instanceof ConsoleCommandStep ? 2
                    : selectedVanillaCommand != null
                    ? vanillaTypeFor(selectedVanillaCommand) : 3;
            type = new WurmDropDown("keybinder.step.type", initialType, stepTypeOptions());
            lastTypeValue = initialType;
            controls.componentWidthOffset = 1;
            controls.addComponent(add);
            controls.addComponent(remove);
            command.prompt = "";
            command.setMaxInput(500);
            if (step instanceof ActionStep)
                actionIdValue = String.valueOf(((ActionStep) step).getActionId());
            if (step instanceof ConsoleCommandStep)
                command.setText(((ConsoleCommandStep) step).getCommand());
            else command.setText("");
            createVanillaAction(selectedVanillaCommand);
            selectedTarget = step instanceof ActionStep
                    ? TargetCodec.encode(((ActionStep) step).getTarget())
                    : step instanceof ActivateToolStep
                    ? TargetCodec.encode(((ActivateToolStep) step).getTarget())
                    : step instanceof SmartImproveStep
                    ? TargetCodec.encode(((SmartImproveStep) step).getTarget())
                    : "hover";
            createTarget();
            panel.componentWidthOffset = COLUMN_GAP;
            updateActionName();
            updateHelpText();
            updateControlStates();
        }

        private int vanillaTypeFor(String command) {
            VanillaKeybindCatalog.Category category = VANILLA_CATALOG.categoryFor(command);
            if (category == null) return VANILLA_TYPE_OFFSET;
            int index = VANILLA_CATALOG.getCategories().indexOf(category);
            return VANILLA_TYPE_OFFSET + Math.max(0, index);
        }

        private boolean isVanilla() {
            return type.getValue() >= VANILLA_TYPE_OFFSET;
        }

        private VanillaKeybindCatalog.Category vanillaCategory() {
            int index = type.getValue() - VANILLA_TYPE_OFFSET;
            List<VanillaKeybindCatalog.Category> categories = VANILLA_CATALOG.getCategories();
            if (index < 0 || index >= categories.size()) return null;
            return categories.get(index);
        }

        private String vanillaCommandFor(short actionId) {
            VanillaKeybindCatalog.Entry entry = VANILLA_CATALOG.findByActionId(actionId);
            return entry == null ? null : entry.getCommand();
        }

        private VanillaKeybindCatalog.Entry vanillaEntry() {
            VanillaKeybindCatalog.Category category = vanillaCategory();
            if (category == null || category.getEntries().isEmpty() || vanillaAction == null)
                return null;
            int selected = vanillaAction.getValue();
            return selected < 0 || selected >= category.getEntries().size()
                    ? null : category.getEntries().get(selected);
        }

        private boolean vanillaUsesTarget() {
            return VANILLA_STEPS.usesTarget(vanillaCategory(), vanillaEntry());
        }

        private void createVanillaAction(String selectedCommand) {
            VanillaKeybindCatalog.Category category = vanillaCategory();
            if (category == null) {
                vanillaAction = new WurmDropDown(
                        "keybinder.vanilla.action", 0, new String[]{""});
                lastVanillaValue = 0;
                return;
            }
            List<VanillaKeybindCatalog.Entry> entries = category.getEntries();
            String[] options = new String[entries.size()];
            int selected = 0;
            for (int i = 0; i < entries.size(); i++) {
                VanillaKeybindCatalog.Entry entry = entries.get(i);
                options[i] = entry.getDisplayName();
                if (selectedCommand != null
                        && entry.getCommand().equalsIgnoreCase(selectedCommand)) selected = i;
            }
            vanillaAction = new WurmDropDown("keybinder.vanilla.action", selected, options);
            lastVanillaValue = selected;
        }

        private void createTarget() {
            String[] base = baseTargetOptions();
            if (kind() == StepKind.CONSOLE_COMMAND
                    || (isVanilla() && !vanillaUsesTarget())) {
                hasConcreteTarget = false;
                lastDropdownValue = 0;
                target = new SelectableTargetDropDown(this, 0, new String[]{""});
                return;
            }
            if (!concreteTarget(selectedTarget)
                    && optionFor(base, selectedTarget) == 0
                    && !base[0].equalsIgnoreCase(selectedTarget)) {
                selectedTarget = base[0];
            }
            hasConcreteTarget = concreteTarget(selectedTarget);
            String[] options = targetOptions(selectedTarget, base);
            lastDropdownValue = hasConcreteTarget ? 0 : optionFor(base, selectedTarget);
            target = new SelectableTargetDropDown(this, lastDropdownValue, options);
        }

        private void rebuildPanel() {
            panel.removeAllComponents();
            panel.addComponent(controls);
            panel.addComponent(controlsSeparator);
            panel.addComponent(type);
            panel.addComponent(help);
            if (kind() == StepKind.CONSOLE_COMMAND) {
                panel.addComponent(capture);
                panel.addComponent(command);
                return;
            }
            if (isVanilla()) {
                panel.addComponent(captureGap);
                panel.addComponent(vanillaAction);
                if (vanillaUsesTarget()) {
                    panel.addComponent(targetSeparator);
                    panel.addComponent(target);
                }
                return;
            }
            panel.addComponent(capture);
            panel.addComponent(actionName);
            panel.addComponent(targetSeparator);
            panel.addComponent(target);
        }

        private void setSelectedTarget(String selected) {
            selectedTarget = selected;
            refreshSelectedTarget();
        }

        private void refreshSelectedTarget() {
            createTarget();
            rebuildPanel();
            applyActionLayout(Math.max(300, width - WINDOW_CHROME));
            zone.updateActionLimit();
        }

        private void setActionStep(ActionStep step) {
            actionIdValue = String.valueOf(step.getActionId());
            lastIdText = null;
            // Capture identifies the action the player performed. It must not
            // replace an explicit portable target already chosen in the editor.
            updateActionName();
        }

        private void updateActionName() {
            if (kind() == StepKind.ACTIVATE_TOOL) {
                actionName.setLabel(Messages.text("editor.activate_label"));
                return;
            }
            if (kind() == StepKind.SMART_IMPROVE) {
                actionName.setLabel(Messages.text("editor.step_type.improve"));
                return;
            }
            if (kind() == StepKind.CONSOLE_COMMAND) return;
            if (isVanilla()) return;
            String value = actionIdValue.trim();
            if (value.equals(lastIdText)) return;
            lastIdText = value;
            if (value.isEmpty()) {
                actionName.setLabel("");
                return;
            }
            try {
                int parsed = Integer.parseInt(value);
                if (parsed < Short.MIN_VALUE || parsed > Short.MAX_VALUE)
                    actionName.setLabel(Messages.text("editor.invalid_id"));
                else
                    actionName.setLabel(nonEmpty(controller.getActionName((short) parsed),
                            Messages.text("editor.unknown_action", parsed)));
            } catch (NumberFormatException e) {
                actionName.setLabel(Messages.text("editor.invalid_id"));
            }
        }

        private void resize(int contentWidth) {
            type.setSize(stepTypeWidth(), type.height);
            help.setSize(HELP_WIDTH, help.height);
            if (kind() == StepKind.CONSOLE_COMMAND) {
                int commandWidth = Math.max(160, contentWidth - controls.width - stepTypeWidth()
                        - HELP_WIDTH - CAPTURE_WIDTH - SEPARATOR_WIDTH - COLUMN_GAP * 5);
                controlsSeparator.setSize(SEPARATOR_WIDTH, controlsSeparator.height);
                capture.setSize(CAPTURE_WIDTH, capture.height);
                command.setSize(commandWidth, command.height);
                capture.setEnabled(false);
                updateControlStates();
                panel.componentResized();
                return;
            }
            if (isVanilla()) {
                controlsSeparator.setSize(SEPARATOR_WIDTH, controlsSeparator.height);
                captureGap.setSize(CAPTURE_WIDTH, captureGap.height);
                if (vanillaUsesTarget()) {
                    int remaining = Math.max(240,
                            contentWidth - controls.width - stepTypeWidth() - CAPTURE_WIDTH
                                    - HELP_WIDTH - SEPARATOR_WIDTH * 2 - COLUMN_GAP * 7);
                    int actionWidth = Math.max(ACTION_NAME_MIN_WIDTH, remaining * 2 / 5);
                    vanillaAction.setSize(actionWidth, vanillaAction.height);
                    targetSeparator.setSize(SEPARATOR_WIDTH, targetSeparator.height);
                    target.setSize(remaining - actionWidth, target.height);
                } else {
                    int actionWidth = Math.max(160,
                            contentWidth - controls.width - stepTypeWidth()
                                    - HELP_WIDTH - CAPTURE_WIDTH
                                    - SEPARATOR_WIDTH - COLUMN_GAP * 5);
                    vanillaAction.setSize(actionWidth, vanillaAction.height);
                }
                capture.setEnabled(false);
                updateControlStates();
                panel.componentResized();
                return;
            }
            int remaining = Math.max(240,
                    contentWidth - controls.width - stepTypeWidth() - CAPTURE_WIDTH
                            - HELP_WIDTH - SEPARATOR_WIDTH * 2 - COLUMN_GAP * 7);
            int actionWidth = Math.max(ACTION_NAME_MIN_WIDTH, remaining * 2 / 5);
            controlsSeparator.setSize(SEPARATOR_WIDTH, controlsSeparator.height);
            capture.setSize(CAPTURE_WIDTH, capture.height);
            capture.setEnabled(true);
            actionName.setSize(actionWidth, actionName.height);
            targetSeparator.setSize(SEPARATOR_WIDTH, targetSeparator.height);
            target.setSize(remaining - actionWidth, target.height);
            updateControlStates();
            panel.componentResized();
        }

        private void updateControlStates() {
            remove.setEnabled(zone.rows.size() > 1);
        }

        private KeybindStep toStep() {
            if (kind() == StepKind.ACTIVATE_TOOL)
                return new ActivateToolStep(TargetCodec.decode(selectedTarget));
            if (kind() == StepKind.SMART_IMPROVE)
                return new SmartImproveStep(TargetCodec.decode(selectedTarget));
            if (kind() == StepKind.CONSOLE_COMMAND) {
                if (command.getText().trim().isEmpty())
                    throw new IllegalArgumentException(Messages.text("validation.console_missing"));
                return new ConsoleCommandStep(command.getText());
            }
            if (isVanilla()) {
                VanillaKeybindCatalog.Category category = vanillaCategory();
                if (category == null || category.getEntries().isEmpty())
                    throw new IllegalArgumentException(
                            Messages.text("validation.vanilla_category_empty"));
                int selected = vanillaAction.getValue();
                if (selected < 0 || selected >= category.getEntries().size())
                    throw new IllegalArgumentException(Messages.text("validation.vanilla_missing"));
                return VANILLA_STEPS.create(category, category.getEntries().get(selected),
                        TargetCodec.decode(selectedTarget));
            }
            String valueText = actionIdValue.trim();
            if (valueText.isEmpty())
                throw new IllegalArgumentException(Messages.text("validation.capture_required"));
            int value = Integer.parseInt(valueText);
            if (value < Short.MIN_VALUE || value > Short.MAX_VALUE)
                throw new IllegalArgumentException(Messages.text("validation.action_id_range"));
            return new ActionStep((short) value, TargetCodec.decode(selectedTarget),
                    controller.getActionName((short) value));
        }

        private StepKind kind() {
            switch (type.getValue()) {
                case 0: return StepKind.ACTIVATE_TOOL;
                case 1: return StepKind.SMART_IMPROVE;
                case 2: return StepKind.CONSOLE_COMMAND;
                case 3: return StepKind.CUSTOM_ACTION;
                default: return StepKind.VANILLA_ACTION;
            }
        }

        private String[] baseTargetOptions() {
            if (isVanilla() && vanillaEntry() != null && vanillaEntry().isActivateTool())
                return VANILLA_ACTIVATE_TARGET_OPTIONS;
            if (kind() == StepKind.ACTIVATE_TOOL) return ACTIVATE_TARGET_OPTIONS;
            if (kind() == StepKind.SMART_IMPROVE) return SMART_IMPROVE_TARGET_OPTIONS;
            return BASE_TARGET_OPTIONS;
        }

        private void resetForType() {
            controller.cancelCapture();
            controller.cancelTargetSelection();
            selectedTarget = baseTargetOptions()[0];
            lastIdText = null;
            createVanillaAction(null);
            createTarget();
            updateActionName();
            updateHelpText();
        }

        private void updateHelpText() {
            String text;
            if (kind() == StepKind.ACTIVATE_TOOL) {
                text = Messages.text("editor.help.activate");
            } else if (kind() == StepKind.SMART_IMPROVE) {
                text = Messages.text("editor.help.improve");
            } else if (kind() == StepKind.CONSOLE_COMMAND) {
                text = Messages.text("editor.help.console");
            } else if (isVanilla()) {
                text = Messages.text("editor.help.vanilla", vanillaCategory().getDisplayName());
            } else {
                text = Messages.text("editor.help.custom");
            }
            help.setHoverString(text);
        }
    }

    private static final class VerticalSeparator extends FlexComponent {
        private VerticalSeparator() {
            super("keybinder.action.separator");
            setSize(SEPARATOR_WIDTH, 20);
        }

        @Override
        protected void renderComponent(Queue queue, float ignoredAlpha) {
            fillRect(queue, 0.48f, 0.42f, 0.33f, 1.0f,
                    x + width / 2, y + 3, 1, Math.max(1, height - 6));
        }
    }

    private final class SelectableActionPanel extends WurmArrayPanel<FlexComponent> {
        private final ActionRow owner;

        private SelectableActionPanel(ActionRow owner) {
            super("keybinder.action.row", WurmArrayPanel.DIR_HORIZONTAL);
            this.owner = owner;
        }

        @Override
        void componentResized() {
            super.componentResized();
            for (FlexComponent component : components)
                component.setPosition(component.x, y + Math.max(0, (height - component.height) / 2));
        }

        @Override
        protected void renderComponent(Queue queue, float alpha) {
            if (owner == selectedRow)
                fillRect(queue, 0.20f, 0.34f, 0.50f, 0.55f, x, y, width, height);
            super.renderComponent(queue, alpha);
            if (rowDragging && draggedRow != null && draggedRow.zone == owner.zone) {
                int index = owner.zone.rows.indexOf(owner);
                if (dragInsertion == index)
                    fillRect(queue, 0.92f, 0.77f, 0.42f, 1.0f, x, y, width, 2);
                if (index + 1 == owner.zone.rows.size() && dragInsertion == owner.zone.rows.size())
                    fillRect(queue, 0.92f, 0.77f, 0.42f, 1.0f,
                            x, y + height - 2, width, 2);
            }
        }

        @Override
        void leftPressed(int mouseX, int mouseY, int clickCount) {
            selectedRow = owner;
            beginRowDrag(owner, mouseY);
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

    private final class SelectableActionLabel extends WurmLabel {
        private final ActionRow owner;
        private String currentLabel = "";

        private SelectableActionLabel(ActionRow owner) {
            super("");
            this.owner = owner;
        }

        @Override
        void setLabel(String label) {
            String next = label == null ? "" : label;
            if (next.equals(currentLabel)) return;
            currentLabel = next;
            super.setLabel(next);
        }

        @Override
        void leftPressed(int mouseX, int mouseY, int clickCount) {
            selectedRow = owner;
            beginRowDrag(owner, mouseY);
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

    private final class SelectableTargetDropDown extends WurmDropDown {
        private final ActionRow owner;

        private SelectableTargetDropDown(ActionRow owner, int selectedValue, String[] options) {
            super("keybinder.target", selectedValue, options);
            this.owner = owner;
        }

        @Override
        protected void leftPressed(int mouseX, int mouseY, int clickCount) {
            selectedRow = owner;
            super.leftPressed(mouseX, mouseY, clickCount);
        }
    }

    private static String nonEmpty(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value;
    }
}
