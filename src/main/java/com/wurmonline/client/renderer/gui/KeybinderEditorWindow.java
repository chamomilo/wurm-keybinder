package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.backend.Queue;
import org.keybinder.wurm.KeybinderMod;
import org.keybinder.wurm.catalog.VanillaCatalogStepFactory;
import org.keybinder.wurm.catalog.VanillaKeybindCatalog;
import org.keybinder.wurm.catalog.InputKeyCatalog;
import org.keybinder.wurm.command.TargetCodec;
import org.keybinder.wurm.command.ItemSelectorCodec;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.ActionSourcePolicy;
import org.keybinder.wurm.model.ActionTargetPolicy;
import org.keybinder.wurm.model.ActivateToolStep;
import org.keybinder.wurm.model.ConsoleCommandStep;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.model.KeybindVariant;
import org.keybinder.wurm.model.KeybindLimits;
import org.keybinder.wurm.model.KeybindNamePrefixes;
import org.keybinder.wurm.model.ItemSelector;
import org.keybinder.wurm.model.SmartImproveStep;
import org.keybinder.wurm.model.SmartImproveSourceMode;
import org.keybinder.wurm.model.StepKind;
import org.keybinder.wurm.model.TargetKind;
import org.keybinder.wurm.model.TargetSpec;
import org.keybinder.wurm.model.VanillaActionStep;
import org.keybinder.wurm.model.BulkDestinationKind;
import org.keybinder.wurm.model.BulkStorageItem;
import org.keybinder.wurm.model.BulkTransferStep;
import org.keybinder.wurm.integration.BulkInventoryDestinationPolicy;
import org.keybinder.wurm.model.InventoryReference;
import org.keybinder.wurm.queue.QueueCost;
import org.keybinder.wurm.ui.EditorOptionPresentation;
import org.keybinder.wurm.ui.EditorStepDraft;
import org.keybinder.wurm.ui.EditorActionNamePolicy;
import org.keybinder.wurm.ui.KeybindEditorController;
import org.keybinder.wurm.ui.EditorTargetValue;
import org.keybinder.wurm.ui.EditorStepType;
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
    static final int MIN_WIDTH = 920;
    private static final int WINDOW_CHROME = 32;
    private static final int ACTION_NAME_MIN_WIDTH = 145;
    private static final int SOURCE_MIN_WIDTH = 150;
    private static final int HELP_WIDTH = 22;
    private static final int CAPTURE_WIDTH = 24;
    private static final int SEPARATOR_WIDTH = 9;
    private static final int COLUMN_GAP = 8;
    private static final int SECTION_GAP = 7;
    private static final int ACTION_ROW_INDENT = 24;
    private static final int TITLE_TO_SUBNAME_GAP = 12;
    private static final String[] BASE_TARGET_OPTIONS = {
            "hover", "body", "tool", "selected", "current ride", "tiles",
            "toolbelt", "equipment", "exact object", "nearby", "nearby by type",
            "hover by type", "inventory+filter"
    };
    private static final String[] SOURCE_OPTIONS = {
            "current-active", "empty-hand", "hovered-item",
            "toolbelt", "equipment", "exact-object"
    };
    private static final VanillaKeybindCatalog VANILLA_CATALOG = new VanillaKeybindCatalog();
    private static final VanillaCatalogStepFactory VANILLA_STEPS =
            new VanillaCatalogStepFactory();
    private static final int VANILLA_TYPE_OFFSET = 5;
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

    private static String[] stepTypeOptions() {
        List<VanillaKeybindCatalog.Category> categories = VANILLA_CATALOG.getCategories();
        String[] options = new String[VANILLA_TYPE_OFFSET + categories.size()];
        options[0] = Messages.text("editor.step_type.activate");
        options[1] = Messages.text("editor.step_type.improve");
        options[2] = Messages.text("editor.step_type.console");
        options[3] = Messages.text("editor.step_type.custom");
        options[4] = Messages.text("editor.step_type.bulk_transfer");
        for (int i = 0; i < categories.size(); i++)
            options[VANILLA_TYPE_OFFSET + i] =
                    Messages.text("editor.step_type.vanilla", categories.get(i).getDisplayName());
        return options;
    }

    static int stepTypeWidth() { return maximumOptionWidth(stepTypeOptions()); }

    static int[] actionColumnWidths(int contentWidth, int controlsWidth) {
        int remaining = Math.max(420,
                contentWidth - controlsWidth - stepTypeWidth() - HELP_WIDTH - CAPTURE_WIDTH
                        - SEPARATOR_WIDTH * 3 - COLUMN_GAP * 9);
        int actionWidth = Math.max(ACTION_NAME_MIN_WIDTH, remaining / 3);
        int sourceWidth = Math.max(SOURCE_MIN_WIDTH, remaining / 3);
        return new int[] {actionWidth, sourceWidth, remaining - actionWidth - sourceWidth};
    }

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
    private final WCheckBox hudMulti;
    private final List<VariantZone> zones = new ArrayList<>();
    private String activeVariantId;
    private final WurmArrayPanel<FlexComponent> actions;
    private final List<ActionRow> rows = new ArrayList<>();
    private final WButton done;
    private final WButton cancel;
    private final WurmArrayPanel<FlexComponent> footer;
    private final WurmArrayPanel<FlexComponent> footerArea;
    private ActionRow pendingTargetRow;
    private ActionRow pendingSourceRow;
    private ActionRow pendingBulkSourceRow;
    private ActionRow pendingBulkDestinationRow;
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
    private boolean lastHudMultiChecked;
    private final KeybinderDragIndicator.InsertionGap actionInsertionGap =
            new KeybinderDragIndicator.InsertionGap(
                    "keybinder.drag.action.gap", ACTION_ROW_INDENT);
    private VariantZone actionGapZone;
    private final KeybinderDragIndicator.InsertionGap zoneInsertionGap =
            new KeybinderDragIndicator.InsertionGap(
                    "keybinder.drag.variant.gap", 0);

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
                Messages.text("editor.action_selection_help")));
        top.addComponent(new WurmLabel(
                Messages.text("editor.long_press")));
        top.addComponent(new WurmLabel(
                Messages.text("editor.mouse_help")));
        top.addComponent(new WurmLabel(
                Messages.text("editor.extract_help")));
        top.addComponent(verticalSpacer(SECTION_GAP));
        top.addComponent(new WurmLabel(Messages.text("editor.name")));
        nameField = new WurmInputField("keybinder.editor.name", this);
        nameField.prompt = "";
        nameField.setText(record == null ? Messages.text("editor.new") : record.getName());
        nameField.setMaxInput(KeybindLimits.MAX_RECORD_NAME_LENGTH);
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
        hudMulti = new WCheckBox(Messages.text("editor.hud_action"));
        hudMulti.checked = record != null && record.isHudMulti();
        hudMulti.setHoverString(Messages.text("editor.hud_action.tip"));
        top.addComponent(hudMulti);
        top.addComponent(new WurmLabel(Messages.text("editor.hud_action.help")));
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
        updateHudMultiState();
        lastHudMultiChecked = hudMulti.checked;
        refreshRows();
        selectedRow = rows.get(0);
        rebuildActions();
        applyLayout();
    }

    public boolean editsRecord(String id) {
        return id != null && id.equals(recordId);
    }

    public String getEditedRecordId() { return recordId; }

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
            if (zones.size() >= KeybindLimits.MAX_VARIANTS) {
                controller.showEditorError(Messages.text("editor.max_variants",
                        KeybindLimits.MAX_VARIANTS));
                return;
            }
            VariantZone zone = new VariantZone(new KeybindVariant(null, "", null), zones.size());
            zones.add(zone);
            updateHudMultiState();
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
                updateHudMultiState();
                refreshRows();
                selectedRow = rows.isEmpty() ? null : rows.get(0);
                rebuildActions();
                return;
            }
            if (button == zone.extract) {
                extractVariant(zone);
                return;
            }
        }
        for (int i = 0; i < rows.size(); i++) {
            ActionRow row = rows.get(i);
            if (button == row.bulkSourceButton) {
                pendingTargetRow = null;
                pendingSourceRow = null;
                pendingBulkDestinationRow = null;
                pendingBulkSourceRow = row;
                controller.requestTargetSelection("bulk source");
                return;
            }
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
                if (zone.rows.size() >= KeybindLimits.MAX_STEPS_PER_VARIANT) {
                    controller.showEditorError(Messages.text("validation.steps_too_many",
                            KeybindLimits.MAX_STEPS_PER_VARIANT));
                    return;
                }
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
            if (button == row.add || button == row.remove || button == row.capture
                    || button == row.bulkSourceButton) return row;
        return null;
    }

    private void save() {
        try {
            // Wurm dropdowns are polled on HUD ticks. Flush the click that opened
            // Done itself so a just-selected Nearby value cannot save the previous
            // Hover token when both clicks happen between two ticks.
            updateEditorState();
            applyCompletedTargetSelection(false);
            applyCompletedBulkSelections(false);
            if (pendingSourceRow != null || pendingTargetRow != null
                    || pendingBulkSourceRow != null || pendingBulkDestinationRow != null)
                throw new IllegalArgumentException(Messages.text("validation.selection_pending"));
            List<KeybindVariant> variants = new ArrayList<KeybindVariant>();
            for (VariantZone zone : zones) variants.add(zone.toVariant());
            controller.saveVariants(recordId, nameField.getText(), selectedKey(),
                    variants,
                    activeVariantId, hudMulti.checked,
                    createdByUser, createdOnServer);
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
        if (hudMulti.checked != lastHudMultiChecked) {
            lastHudMultiChecked = hudMulti.checked;
            normalizeNamePrefix();
        }
        for (ActionRow row : new ArrayList<>(rows)) {
            int typeValue = row.type.getValue();
            if (typeValue != row.lastTypeValue) {
                selectedRow = row;
                row.lastTypeValue = typeValue;
                row.resetForType();
                row.rebuildPanel();
                lastLayoutWidth = -1;
                applyLayout();
                row.zone.updateActionLimit();
            }
            row.updateActionName();
            if (row.kind() == StepKind.BULK_TRANSFER) {
                int destinationValue = row.bulkDestination.getValue();
                if (destinationValue != row.lastBulkDestinationValue) {
                    selectedRow = row;
                    row.lastBulkDestinationValue = destinationValue;
                    if (!(row.hasCapturedBulkDestination && destinationValue == 0)) {
                        int baseIndex = row.hasCapturedBulkDestination
                                ? destinationValue - 1 : destinationValue;
                        if (baseIndex == 0) {
                            boolean removeCapturedOption = row.hasCapturedBulkDestination;
                            if (pendingBulkDestinationRow == row) {
                                pendingBulkDestinationRow = null;
                                controller.cancelTargetSelection();
                            }
                            row.bulkDestinationKind = BulkDestinationKind.PLAYER_INVENTORY;
                            row.capturedBulkDestination = null;
                            if (removeCapturedOption)
                                KeybinderMod.deferUi(row::refreshBulkDestination);
                        } else if (baseIndex == 1) {
                            boolean removeCapturedOption = row.hasCapturedBulkDestination;
                            if (pendingBulkDestinationRow == row) {
                                pendingBulkDestinationRow = null;
                                controller.cancelTargetSelection();
                            }
                            row.bulkDestinationKind = BulkDestinationKind.HOVERED_INVENTORY;
                            row.capturedBulkDestination = null;
                            if (removeCapturedOption)
                                KeybinderMod.deferUi(row::refreshBulkDestination);
                        } else if (baseIndex == 2) {
                            pendingTargetRow = null;
                            pendingSourceRow = null;
                            pendingBulkSourceRow = null;
                            pendingBulkDestinationRow = row;
                            controller.requestTargetSelection("bulk destination");
                        }
                    }
                }
                continue;
            }
            if (row.usesActionSource()) {
                int sourceValue = row.source.getValue();
                if (sourceValue != row.lastSourceValue) {
                    selectedRow = row;
                    row.lastSourceValue = sourceValue;
                    if (!(row.hasConcreteSource && sourceValue == 0)) {
                        int sourceIndex = row.hasConcreteSource ? sourceValue - 1 : sourceValue;
                        if (sourceIndex >= 0 && sourceIndex < SOURCE_OPTIONS.length) {
                            String chosenSource = SOURCE_OPTIONS[sourceIndex];
                            if ("toolbelt".equals(chosenSource)
                                    || "equipment".equals(chosenSource)
                                    || "exact-object".equals(chosenSource)) {
                                pendingTargetRow = null;
                                pendingSourceRow = row;
                                controller.requestTargetSelection(
                                        "exact-object".equals(chosenSource)
                                                ? "exact object" : chosenSource);
                            } else {
                                row.selectedSource = ItemSelectorCodec.decode(chosenSource);
                                row.refreshSelectedSource();
                            }
                        }
                    }
                }
            }
            if (row.kind() == StepKind.CONSOLE_COMMAND) continue;
            if (row.isVanilla()) {
                int value = row.vanillaAction.getValue();
                if (value != row.lastVanillaValue) {
                    selectedRow = row;
                    row.lastVanillaValue = value;
                    row.createTarget();
                    row.rebuildPanel();
                    lastLayoutWidth = -1;
                    applyLayout();
                    row.zone.updateActionLimit();
                    continue;
                }
                if (!row.vanillaUsesTarget()) continue;
            }
            if (!row.usesActionTarget()) continue;
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
                    || "nearby by type".equals(selected)
                    || "hover by type".equals(selected)
                    || "inventory+filter".equals(selected)) {
                pendingSourceRow = null;
                pendingTargetRow = row;
                // A concrete target belongs only to the completed one-shot
                // selection. Clear it before every picker so Save can never
                // retain an old exact object/slot/type if the new capture has
                // not yet been applied. The dropdown itself remains visible.
                row.selectedTarget = "unresolved";
                controller.requestTargetSelection(selected);
            } else {
                row.selectedTarget = selected;
            }
        }
        String selected = controller.consumeSelectedTarget();
        if (selected != null && (pendingTargetRow != null || pendingSourceRow != null)) {
            applyCompletedTargetSelection(selected, true);
        }
        BulkStorageItem bulkSource = controller.consumeSelectedBulkSource();
        if (bulkSource != null && pendingBulkSourceRow != null) {
            ActionRow row = pendingBulkSourceRow;
            pendingBulkSourceRow = null;
            row.selectedBulkSource = bulkSource;
            KeybinderMod.deferUi(row::refreshBulkSource);
        }
        InventoryReference bulkDestination = controller.consumeSelectedBulkDestination();
        if (bulkDestination != null && pendingBulkDestinationRow != null) {
            ActionRow row = pendingBulkDestinationRow;
            pendingBulkDestinationRow = null;
            row.bulkDestinationKind = BulkDestinationKind.CAPTURED_INVENTORY;
            row.capturedBulkDestination = bulkDestination;
            KeybinderMod.deferUi(row::refreshBulkDestination);
        }
    }

    private void extractVariant(VariantZone extracted) {
        try {
            applyCompletedTargetSelection(false);
            applyCompletedBulkSelections(false);
            if (pendingSourceRow != null || pendingTargetRow != null
                    || pendingBulkSourceRow != null || pendingBulkDestinationRow != null)
                throw new IllegalArgumentException(Messages.text("validation.selection_pending"));
            if (extracted.defaultZone || zones.size() <= 1) return;
            List<KeybindVariant> variants = new ArrayList<KeybindVariant>();
            for (VariantZone zone : zones) variants.add(zone.toVariant());
            controller.extractVariant(recordId, nameField.getText(), selectedKey(), variants,
                    activeVariantId, hudMulti.checked, extracted.id,
                    createdByUser, createdOnServer);
        } catch (RuntimeException e) {
            controller.showEditorError(Messages.text("editor.cannot_extract",
                    e.getMessage() == null ? Messages.text("editor.invalid_value")
                            : e.getMessage()));
        }
    }

    private void updateHudMultiState() {
        boolean available = zones.size() > 1;
        hudMulti.enabled = available;
        if (!available) hudMulti.checked = false;
        lastHudMultiChecked = hudMulti.checked;
        normalizeNamePrefix();
    }

    private void normalizeNamePrefix() {
        String normalized = KeybindNamePrefixes.apply(
                nameField.getText(), zones.size(), hudMulti.checked);
        if (!normalized.equals(nameField.getText())) nameField.setText(normalized);
    }

    private void applyCompletedTargetSelection(boolean rebuild) {
        String selected = controller.consumeSelectedTarget();
        if (selected != null) applyCompletedTargetSelection(selected, rebuild);
    }

    private void applyCompletedTargetSelection(String selected, boolean rebuild) {
        ActionRow sourceRow = pendingSourceRow;
        if (sourceRow != null) {
            pendingSourceRow = null;
            sourceRow.selectedSource = ItemSelectorCodec.decode(selected);
            if (rebuild) KeybinderMod.deferUi(sourceRow::refreshSelectedSource);
            return;
        }
        ActionRow targetRow = pendingTargetRow;
        if (targetRow == null) return;
        pendingTargetRow = null;
        // Update editor state synchronously. Rebuilding Wurm controls is deferred,
        // but Save must never observe the old target in the intervening frame.
        targetRow.selectedTarget = selected;
        if (rebuild) KeybinderMod.deferUi(targetRow::refreshSelectedTarget);
    }

    private void applyCompletedBulkSelections(boolean rebuild) {
        BulkStorageItem source = controller.consumeSelectedBulkSource();
        if (source != null && pendingBulkSourceRow != null) {
            ActionRow row = pendingBulkSourceRow;
            pendingBulkSourceRow = null;
            row.selectedBulkSource = source;
            if (rebuild) KeybinderMod.deferUi(row::refreshBulkSource);
        }
        InventoryReference destination = controller.consumeSelectedBulkDestination();
        if (destination != null && pendingBulkDestinationRow != null) {
            ActionRow row = pendingBulkDestinationRow;
            pendingBulkDestinationRow = null;
            row.bulkDestinationKind = BulkDestinationKind.CAPTURED_INVENTORY;
            row.capturedBulkDestination = destination;
            if (rebuild) KeybinderMod.deferUi(row::refreshBulkDestination);
        }
    }

    @Override
    void setSize(int requestedWidth, int requestedHeight) {
        // Clamp inside the resize operation. Correcting the width from gameTick
        // fights WWindow's active resize/hover state and makes custom content flicker.
        super.setSize(Math.max(requestedWidth, MIN_WIDTH), requestedHeight);
    }

    private void rebuildActions() {
        actions.removeAllComponents();
        actionGapZone = null;
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
        return EditorTargetValue.isConcrete(target);
    }

    private void beginRowDrag(ActionRow row, int mouseY) {
        hideActionInsertionGap();
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
        showActionInsertionGap(draggedRow.zone, dragInsertion);
    }

    private void finishRowDrag() {
        ActionRow moving = draggedRow;
        int insertion = dragInsertion;
        boolean apply = rowDragging && moving != null && insertion >= 0;
        hideActionInsertionGap();
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
        hideZoneInsertionGap();
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
        showZoneInsertionGap(zoneDragInsertion);
    }

    private void finishZoneDrag() {
        VariantZone moving = draggedZone;
        int insertion = zoneDragInsertion;
        boolean apply = zoneDragging && moving != null && insertion >= 0;
        hideZoneInsertionGap();
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

    private void showActionInsertionGap(VariantZone zone, int insertion) {
        if (zone == null || insertion < 0) {
            hideActionInsertionGap();
            return;
        }
        if (actionGapZone != null && actionGapZone != zone)
            KeybinderDragIndicator.remove(actionGapZone.panel, actionInsertionGap);
        actionGapZone = zone;
        KeybinderDragIndicator.place(zone.panel, actionInsertionGap,
                KeybinderDragIndicator.actionGapComponentIndex(insertion));
    }

    private void hideActionInsertionGap() {
        if (actionGapZone != null)
            KeybinderDragIndicator.remove(actionGapZone.panel, actionInsertionGap);
        actionGapZone = null;
    }

    private void showZoneInsertionGap(int insertion) {
        if (insertion < 0) {
            hideZoneInsertionGap();
            return;
        }
        KeybinderDragIndicator.place(actions, zoneInsertionGap,
                KeybinderDragIndicator.variantGapComponentIndex(insertion));
    }

    private void hideZoneInsertionGap() {
        KeybinderDragIndicator.remove(actions, zoneInsertionGap);
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
        private final KeybinderGlyphButton extract = new KeybinderGlyphButton(
                KeybinderGlyphButton.Kind.EXTRACT_UP, KeybinderEditorWindow.this,
                Messages.text("editor.variant.extract"));
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
            subName.setMaxInput(KeybindLimits.MAX_VARIANT_NAME_LENGTH);
            subName.setText(variant.getSubName());
            remove.setEnabled(!defaultZone);
            extract.setEnabled(!defaultZone);
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
            if (!defaultZone) {
                titleRow.addComponent(remove);
                titleRow.addComponent(extract);
            }
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
            int componentCount = defaultZone ? 5 : 7;
            int fixed = collapse.width + title.width + titleToSubNameGap.width
                    + subNameLabel.width
                    + (defaultZone ? 0 : remove.width + extract.width)
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
            }

            @Override
            void leftPressed(int mouseX, int mouseY, int clickCount) {
                if (!inside(collapse, mouseX, mouseY)
                        && (defaultZone || !inside(remove, mouseX, mouseY))
                        && (defaultZone || !inside(extract, mouseX, mouseY))
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
        private final WurmLabel source = new WurmLabel(Messages.text("editor.tool"));
        private final VerticalSeparator sourceSeparator = new VerticalSeparator();
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
            panel.addComponent(sourceSeparator);
            panel.addComponent(source);
            panel.addComponent(targetSeparator);
            panel.addComponent(target);
        }

        private void resize(int contentWidth) {
            int controlsWidth = zone.rows.isEmpty() ? 100 : zone.rows.get(0).controls.width;
            int[] columns = actionColumnWidths(contentWidth, controlsWidth);
            controls.setSize(controlsWidth, controls.height);
            controlsSeparator.setSize(SEPARATOR_WIDTH, controlsSeparator.height);
            type.setSize(stepTypeWidth(), type.height);
            help.setSize(HELP_WIDTH, help.height);
            capture.setSize(CAPTURE_WIDTH, capture.height);
            action.setSize(columns[0], action.height);
            sourceSeparator.setSize(SEPARATOR_WIDTH, sourceSeparator.height);
            source.setSize(columns[1], source.height);
            targetSeparator.setSize(SEPARATOR_WIDTH, targetSeparator.height);
            target.setSize(columns[2], target.height);
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
        private final WButton bulkSourceButton = new WButton(
                Messages.text("editor.bulk.select_item"), KeybinderEditorWindow.this);
        private final WurmInputField bulkQuantity = new WurmInputField(
                "keybinder.bulk.quantity", KeybinderEditorWindow.this);
        private final WurmArrayPanel<FlexComponent> bulkQuantityPanel =
                new WurmArrayPanel<>("keybinder.bulk.quantity.panel",
                        WurmArrayPanel.DIR_HORIZONTAL);
        private final WurmLabel bulkQuantityLabel =
                new WurmLabel(Messages.text("editor.bulk.quantity"));
        private WurmDropDown bulkDestination;
        private int lastBulkDestinationValue;
        private boolean hasCapturedBulkDestination;
        private BulkStorageItem selectedBulkSource;
        private BulkDestinationKind bulkDestinationKind =
                BulkDestinationKind.PLAYER_INVENTORY;
        private InventoryReference capturedBulkDestination;
        private String actionIdValue = "";
        private short rememberedActionNameId;
        private String rememberedActionName = "";
        private final WurmInputField command =
                new WurmInputField("keybinder.command.value", KeybinderEditorWindow.this);
        private final SelectableActionLabel actionName;
        private final VerticalSeparator sourceSeparator = new VerticalSeparator();
        private final WurmLabel sourceGap = horizontalSpacer(SOURCE_MIN_WIDTH);
        private SelectableSourceDropDown source;
        private WurmDropDown smartImproveSource;
        private SmartImproveSourceMode smartImproveSourceMode =
                SmartImproveSourceMode.TOOLBELT_THEN_INVENTORY;
        private ItemSelector selectedSource;
        private int lastSourceValue;
        private boolean hasConcreteSource;
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
                    ? ((VanillaActionStep) step).getCommand() : null;
            int initialType = EditorStepType.initialIndex(step);
            if (initialType < 0) initialType = selectedVanillaCommand != null
                    ? vanillaTypeFor(selectedVanillaCommand) : 3;
            type = new WurmDropDown("keybinder.step.type", initialType, stepTypeOptions());
            lastTypeValue = initialType;
            controls.componentWidthOffset = 1;
            controls.addComponent(add);
            controls.addComponent(remove);
            command.prompt = "";
            command.setMaxInput(KeybindLimits.MAX_COMMAND_LENGTH);
            if (step instanceof ActionStep) {
                actionIdValue = String.valueOf(((ActionStep) step).getActionId());
                rememberActionName((ActionStep) step);
            }
            selectedSource = step instanceof ActionStep
                    ? ((ActionStep) step).getSource() : ItemSelector.currentActive();
            createSource();
            if (step instanceof SmartImproveStep)
                smartImproveSourceMode = ((SmartImproveStep) step).getSourceMode();
            createSmartImproveSource();
            if (step instanceof ConsoleCommandStep)
                command.setText(((ConsoleCommandStep) step).getCommand());
            else command.setText("");
            bulkQuantity.prompt = "";
            bulkQuantity.setMaxInput(10);
            bulkQuantity.setText(step instanceof BulkTransferStep
                    ? Integer.toString(((BulkTransferStep) step).getQuantity()) : "1");
            bulkQuantityPanel.componentWidthOffset = 4;
            bulkQuantityPanel.addComponent(bulkQuantityLabel);
            bulkQuantityPanel.addComponent(bulkQuantity);
            if (step instanceof BulkTransferStep) {
                BulkTransferStep bulk = (BulkTransferStep) step;
                selectedBulkSource = BulkStorageItem.copyOf(bulk.getSource());
                bulkDestinationKind = bulk.getDestinationKind() == null
                        ? BulkDestinationKind.PLAYER_INVENTORY : bulk.getDestinationKind();
                capturedBulkDestination = InventoryReference.copyOf(
                        bulk.getCapturedDestination());
            }
            refreshBulkSourceLabel();
            createBulkDestination();
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

        private boolean vanillaUsesStructuredAction() {
            return VANILLA_STEPS.usesStructuredAction(vanillaCategory(), vanillaEntry());
        }

        private boolean usesActionSource() {
            if (!isVanilla() && kind() == StepKind.CUSTOM_ACTION) {
                try {
                    int value = Integer.parseInt(actionIdValue.trim());
                    return value < Short.MIN_VALUE || value > Short.MAX_VALUE
                            || ActionSourcePolicy.acceptsSelectableTool((short) value);
                } catch (NumberFormatException invalid) {
                    return true;
                }
            }
            if (isVanilla() && vanillaUsesStructuredAction()) {
                return vanillaEntry().usesSelectableTool();
            }
            return false;
        }

        private boolean usesActionTarget() {
            if (isVanilla()) return vanillaUsesTarget();
            if (kind() == StepKind.BULK_TRANSFER) return false;
            if (kind() != StepKind.CUSTOM_ACTION) return kind() != StepKind.CONSOLE_COMMAND;
            try {
                int value = Integer.parseInt(actionIdValue.trim());
                return value < Short.MIN_VALUE || value > Short.MAX_VALUE
                        || ActionTargetPolicy.acceptsSelectableTarget((short) value);
            } catch (NumberFormatException invalid) {
                return true;
            }
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
            if (!usesActionTarget()) {
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
            String[] options = EditorOptionPresentation.targetOptions(selectedTarget, base);
            lastDropdownValue = hasConcreteTarget ? 0 : optionFor(base, selectedTarget);
            target = new SelectableTargetDropDown(this, lastDropdownValue, options);
        }

        private void createBulkDestination() {
            String[] base = {
                    Messages.text("editor.bulk.destination.player"),
                    Messages.text("editor.bulk.destination.hovered"),
                    Messages.text("editor.bulk.destination.captured")
            };
            hasCapturedBulkDestination = bulkDestinationKind
                    == BulkDestinationKind.CAPTURED_INVENTORY
                    && capturedBulkDestination != null
                    && BulkInventoryDestinationPolicy.isValid(
                    capturedBulkDestination.getId());
            String[] options = new String[base.length
                    + (hasCapturedBulkDestination ? 1 : 0)];
            int offset = hasCapturedBulkDestination ? 1 : 0;
            if (hasCapturedBulkDestination)
                options[0] = Messages.text("editor.bulk.destination.selected",
                        capturedBulkDestination.getName(), capturedBulkDestination.getId());
            System.arraycopy(base, 0, options, offset, base.length);
            int selected = hasCapturedBulkDestination ? 0
                    : bulkDestinationKind == BulkDestinationKind.HOVERED_INVENTORY ? 1 : 0;
            bulkDestination = new WurmDropDown(
                    "keybinder.bulk.destination", selected, options);
            lastBulkDestinationValue = selected;
        }

        private void refreshBulkSourceLabel() {
            if (selectedBulkSource == null || selectedBulkSource.getItem() == null
                    || selectedBulkSource.getStorage() == null) {
                bulkSourceButton.setLabel(Messages.text("editor.bulk.select_item"));
                bulkSourceButton.setHoverString(Messages.text("editor.help.bulk"));
                return;
            }
            bulkSourceButton.setLabel(Messages.text("editor.bulk.source.selected",
                    selectedBulkSource.getItem().getName()));
            bulkSourceButton.setHoverString(Messages.text("editor.bulk.source.details",
                    selectedBulkSource.getItem().getName(),
                    selectedBulkSource.getItem().getId(),
                    selectedBulkSource.getStorage().getName(),
                    selectedBulkSource.getStorage().getId()));
        }

        private void refreshBulkSource() {
            refreshBulkSourceLabel();
            rebuildPanel();
            applyActionLayout(Math.max(300, width - WINDOW_CHROME));
            zone.updateActionLimit();
        }

        private void refreshBulkDestination() {
            createBulkDestination();
            rebuildPanel();
            applyActionLayout(Math.max(300, width - WINDOW_CHROME));
            zone.updateActionLimit();
        }

        private void createSource() {
            hasConcreteSource = selectedSource.getKind()
                    == org.keybinder.wurm.model.ItemSelectorKind.TOOLBELT_SLOT
                    || selectedSource.getKind()
                    == org.keybinder.wurm.model.ItemSelectorKind.EQUIPMENT_SLOT
                    || selectedSource.getKind()
                    == org.keybinder.wurm.model.ItemSelectorKind.EXACT_OBJECT;
            String[] labels = new String[SOURCE_OPTIONS.length + (hasConcreteSource ? 1 : 0)];
            int offset = hasConcreteSource ? 1 : 0;
            if (hasConcreteSource) labels[0] = ItemSelectorCodec.display(selectedSource);
            for (int i = 0; i < SOURCE_OPTIONS.length; i++)
                labels[i + offset] = EditorOptionPresentation.sourceLabel(SOURCE_OPTIONS[i]);
            int selected = hasConcreteSource ? 0
                    : EditorOptionPresentation.sourceOptionFor(selectedSource.getKind());
            source = new SelectableSourceDropDown(this, selected, labels);
            lastSourceValue = selected;
        }

        private void createSmartImproveSource() {
            smartImproveSource = new WurmDropDown("keybinder.improve.source",
                    smartImproveSourceMode == SmartImproveSourceMode.TOOLBELT_ONLY ? 0 : 1,
                    new String[]{
                            Messages.text("editor.improve.source.toolbelt_only"),
                            Messages.text("editor.improve.source.toolbelt_inventory")
                    });
        }

        private void rebuildPanel() {
            panel.removeAllComponents();
            panel.addComponent(controls);
            panel.addComponent(controlsSeparator);
            panel.addComponent(type);
            panel.addComponent(help);
            panel.addComponent(EditorStepType.supportsCapture(kind()) ? capture : captureGap);
            if (kind() == StepKind.CONSOLE_COMMAND) {
                panel.addComponent(command);
                return;
            }
            if (kind() == StepKind.BULK_TRANSFER) {
                panel.addComponent(bulkSourceButton);
                panel.addComponent(sourceSeparator);
                panel.addComponent(bulkQuantityPanel);
                panel.addComponent(targetSeparator);
                panel.addComponent(bulkDestination);
                return;
            }
            if (isVanilla()) {
                panel.addComponent(vanillaAction);
                if (vanillaUsesTarget()) {
                    panel.addComponent(sourceSeparator);
                    panel.addComponent(usesActionSource() ? source : sourceGap);
                    panel.addComponent(targetSeparator);
                    panel.addComponent(target);
                }
                return;
            }
            panel.addComponent(actionName);
            if (kind() == StepKind.CUSTOM_ACTION && !usesActionTarget()) return;
            if (kind() == StepKind.CUSTOM_ACTION) {
                panel.addComponent(sourceSeparator);
                panel.addComponent(usesActionSource() ? source : sourceGap);
            } else {
                panel.addComponent(sourceSeparator);
                panel.addComponent(kind() == StepKind.SMART_IMPROVE
                        ? smartImproveSource : sourceGap);
            }
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

        private void refreshSelectedSource() {
            createSource();
            updateHelpText();
            rebuildPanel();
            applyActionLayout(Math.max(300, width - WINDOW_CHROME));
        }

        private void setActionStep(ActionStep step) {
            actionIdValue = String.valueOf(step.getActionId());
            rememberActionName(step);
            lastIdText = null;
            // Capture identifies the action the player performed. It must not
            // replace an explicit portable target already chosen in the editor.
            updateActionName();
            createTarget();
            rebuildPanel();
            lastLayoutWidth = -1;
            applyLayout();
            zone.updateActionLimit();
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
            if (kind() == StepKind.BULK_TRANSFER) return;
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
                    actionName.setLabel(nonEmpty(resolveActionName((short) parsed),
                            Messages.text("editor.unknown_action", parsed)));
            } catch (NumberFormatException e) {
                actionName.setLabel(Messages.text("editor.invalid_id"));
            }
        }

        private void rememberActionName(ActionStep step) {
            rememberedActionNameId = step.getActionId();
            rememberedActionName = step.getLastKnownName();
        }

        private String resolveActionName(short actionId) {
            return EditorActionNamePolicy.resolve(actionId, rememberedActionNameId,
                    rememberedActionName, id -> controller.getActionName(id));
        }

        private void resize(int contentWidth) {
            type.setSize(stepTypeWidth(), type.height);
            help.setSize(HELP_WIDTH, help.height);
            if (kind() == StepKind.CONSOLE_COMMAND) {
                int commandWidth = Math.max(160, contentWidth - controls.width - stepTypeWidth()
                        - HELP_WIDTH - CAPTURE_WIDTH - SEPARATOR_WIDTH - COLUMN_GAP * 5);
                controlsSeparator.setSize(SEPARATOR_WIDTH, controlsSeparator.height);
                captureGap.setSize(CAPTURE_WIDTH, captureGap.height);
                command.setSize(commandWidth, command.height);
                capture.setEnabled(false);
                updateControlStates();
                panel.componentResized();
                return;
            }
            if (kind() == StepKind.BULK_TRANSFER) {
                int[] columns = actionColumnWidths(contentWidth, controls.width);
                controlsSeparator.setSize(SEPARATOR_WIDTH, controlsSeparator.height);
                captureGap.setSize(CAPTURE_WIDTH, captureGap.height);
                bulkSourceButton.setSize(columns[0], bulkSourceButton.height);
                sourceSeparator.setSize(SEPARATOR_WIDTH, sourceSeparator.height);
                bulkQuantityPanel.setSize(columns[1], bulkQuantityPanel.height);
                int quantityWidth = Math.max(32, columns[1] - bulkQuantityLabel.width - 4);
                bulkQuantity.setSize(quantityWidth, bulkQuantity.height);
                bulkQuantityPanel.componentResized();
                targetSeparator.setSize(SEPARATOR_WIDTH, targetSeparator.height);
                bulkDestination.setSize(columns[2], bulkDestination.height);
                capture.setEnabled(false);
                updateControlStates();
                panel.componentResized();
                return;
            }
            if (isVanilla()) {
                controlsSeparator.setSize(SEPARATOR_WIDTH, controlsSeparator.height);
                captureGap.setSize(CAPTURE_WIDTH, captureGap.height);
                if (vanillaUsesTarget()) {
                    int[] columns = actionColumnWidths(contentWidth, controls.width);
                    vanillaAction.setSize(columns[0], vanillaAction.height);
                    sourceSeparator.setSize(SEPARATOR_WIDTH, sourceSeparator.height);
                    if (usesActionSource())
                        source.setSize(columns[1], source.height);
                    else sourceGap.setSize(columns[1], sourceGap.height);
                    targetSeparator.setSize(SEPARATOR_WIDTH, targetSeparator.height);
                    target.setSize(columns[2], target.height);
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
            if (kind() != StepKind.CUSTOM_ACTION) {
                int[] columns = actionColumnWidths(contentWidth, controls.width);
                controlsSeparator.setSize(SEPARATOR_WIDTH, controlsSeparator.height);
                capture.setSize(CAPTURE_WIDTH, capture.height);
                capture.setEnabled(true);
                actionName.setSize(columns[0], actionName.height);
                sourceSeparator.setSize(SEPARATOR_WIDTH, sourceSeparator.height);
                if (kind() == StepKind.SMART_IMPROVE)
                    smartImproveSource.setSize(columns[1], smartImproveSource.height);
                else sourceGap.setSize(columns[1], sourceGap.height);
                targetSeparator.setSize(SEPARATOR_WIDTH, targetSeparator.height);
                target.setSize(columns[2], target.height);
                updateControlStates();
                panel.componentResized();
                return;
            }
            if (!usesActionTarget()) {
                controlsSeparator.setSize(SEPARATOR_WIDTH, controlsSeparator.height);
                capture.setSize(CAPTURE_WIDTH, capture.height);
                capture.setEnabled(true);
                int actionWidth = Math.max(160,
                        contentWidth - controls.width - stepTypeWidth()
                                - HELP_WIDTH - CAPTURE_WIDTH
                                - SEPARATOR_WIDTH - COLUMN_GAP * 5);
                actionName.setSize(actionWidth, actionName.height);
                updateControlStates();
                panel.componentResized();
                return;
            }
            int[] columns = actionColumnWidths(contentWidth, controls.width);
            controlsSeparator.setSize(SEPARATOR_WIDTH, controlsSeparator.height);
            capture.setSize(CAPTURE_WIDTH, capture.height);
            capture.setEnabled(true);
            actionName.setSize(columns[0], actionName.height);
            sourceSeparator.setSize(SEPARATOR_WIDTH, sourceSeparator.height);
            if (usesActionSource()) source.setSize(columns[1], source.height);
            else sourceGap.setSize(columns[1], sourceGap.height);
            targetSeparator.setSize(SEPARATOR_WIDTH, targetSeparator.height);
            target.setSize(columns[2], target.height);
            updateControlStates();
            panel.componentResized();
        }

        private void updateControlStates() {
            remove.setEnabled(zone.rows.size() > 1);
        }

        private KeybindStep toStep() {
            EditorStepDraft draft = new EditorStepDraft(kind(), actionIdValue,
                    command.getText(), selectedSource, selectedTarget,
                    vanillaCategory(), vanillaEntry(), selectedBulkSource,
                    bulkQuantity.getText(), bulkDestinationKind,
                    capturedBulkDestination, selectedSmartImproveSourceMode());
            return draft.toStep(actionId -> resolveActionName(actionId));
        }

        private SmartImproveSourceMode selectedSmartImproveSourceMode() {
            if (kind() != StepKind.SMART_IMPROVE || smartImproveSource == null)
                return SmartImproveSourceMode.TOOLBELT_THEN_INVENTORY;
            return smartImproveSource.getValue() == 0
                    ? SmartImproveSourceMode.TOOLBELT_ONLY
                    : SmartImproveSourceMode.TOOLBELT_THEN_INVENTORY;
        }

        private StepKind kind() {
            switch (type.getValue()) {
                case 0: return StepKind.ACTIVATE_TOOL;
                case 1: return StepKind.SMART_IMPROVE;
                case 2: return StepKind.CONSOLE_COMMAND;
                case 3: return StepKind.CUSTOM_ACTION;
                case 4: return StepKind.BULK_TRANSFER;
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
            if (pendingTargetRow == this) pendingTargetRow = null;
            if (pendingSourceRow == this) pendingSourceRow = null;
            if (pendingBulkSourceRow == this) pendingBulkSourceRow = null;
            if (pendingBulkDestinationRow == this) pendingBulkDestinationRow = null;
            selectedTarget = baseTargetOptions()[0];
            selectedSource = ItemSelector.currentActive();
            smartImproveSourceMode = SmartImproveSourceMode.TOOLBELT_THEN_INVENTORY;
            selectedBulkSource = null;
            rememberedActionName = "";
            bulkDestinationKind = BulkDestinationKind.PLAYER_INVENTORY;
            capturedBulkDestination = null;
            bulkQuantity.setText("1");
            lastIdText = null;
            createVanillaAction(null);
            createTarget();
            createSource();
            createSmartImproveSource();
            refreshBulkSourceLabel();
            createBulkDestination();
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
            } else if (kind() == StepKind.BULK_TRANSFER) {
                text = Messages.text("editor.help.bulk");
            } else if (isVanilla()) {
                text = Messages.text("editor.help.vanilla", vanillaCategory().getDisplayName());
                if (usesActionSource())
                    text += " " + Messages.text(EditorOptionPresentation.sourceHelpKey(
                            selectedSource.getKind()));
            } else {
                text = Messages.text("editor.help.custom") + " "
                        + Messages.text(EditorOptionPresentation.sourceHelpKey(
                        selectedSource.getKind()));
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

    private final class SelectableSourceDropDown extends WurmDropDown {
        private final ActionRow owner;

        private SelectableSourceDropDown(ActionRow owner, int selectedValue, String[] options) {
            super("keybinder.source", selectedValue, options);
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
