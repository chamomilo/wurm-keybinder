package org.keybinder.wurm.ui;

import org.keybinder.wurm.catalog.VanillaCatalogStepFactory;
import org.keybinder.wurm.catalog.VanillaKeybindCatalog;
import org.keybinder.wurm.command.TargetCodec;
import org.keybinder.wurm.i18n.Messages;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.ActivateToolStep;
import org.keybinder.wurm.model.ConsoleCommandStep;
import org.keybinder.wurm.model.ItemSelector;
import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.model.SmartImproveStep;
import org.keybinder.wurm.model.SmartImproveSourceMode;
import org.keybinder.wurm.model.StepKind;
import org.keybinder.wurm.model.BulkDestinationKind;
import org.keybinder.wurm.model.BulkStorageItem;
import org.keybinder.wurm.model.BulkTransferStep;
import org.keybinder.wurm.model.InventoryReference;

/**
 * UI-independent snapshot of one editor row.
 *
 * <p>Wurm controls are mutable and polled on HUD ticks. Taking a small value
 * snapshot before conversion keeps parsing and validation out of the GUI
 * bridge and makes the exact save behavior unit-testable.</p>
 */
public final class EditorStepDraft {
    public interface ActionNameLookup {
        String find(short actionId);
    }

    private static final VanillaCatalogStepFactory VANILLA_STEPS =
            new VanillaCatalogStepFactory();

    private final StepKind kind;
    private final String actionId;
    private final String command;
    private final ItemSelector source;
    private final String target;
    private final VanillaKeybindCatalog.Category vanillaCategory;
    private final VanillaKeybindCatalog.Entry vanillaEntry;
    private final BulkStorageItem bulkSource;
    private final String bulkQuantity;
    private final BulkDestinationKind bulkDestinationKind;
    private final InventoryReference bulkCapturedDestination;
    private final SmartImproveSourceMode smartImproveSourceMode;

    public EditorStepDraft(
            StepKind kind,
            String actionId,
            String command,
            ItemSelector source,
            String target,
            VanillaKeybindCatalog.Category vanillaCategory,
            VanillaKeybindCatalog.Entry vanillaEntry) {
        this(kind, actionId, command, source, target, vanillaCategory, vanillaEntry,
                null, "1", BulkDestinationKind.PLAYER_INVENTORY, null,
                SmartImproveSourceMode.TOOLBELT_THEN_INVENTORY);
    }

    public EditorStepDraft(
            StepKind kind,
            String actionId,
            String command,
            ItemSelector source,
            String target,
            VanillaKeybindCatalog.Category vanillaCategory,
            VanillaKeybindCatalog.Entry vanillaEntry,
            BulkStorageItem bulkSource,
            String bulkQuantity,
            BulkDestinationKind bulkDestinationKind,
            InventoryReference bulkCapturedDestination) {
        this(kind, actionId, command, source, target, vanillaCategory, vanillaEntry,
                bulkSource, bulkQuantity, bulkDestinationKind, bulkCapturedDestination,
                SmartImproveSourceMode.TOOLBELT_THEN_INVENTORY);
    }

    public EditorStepDraft(
            StepKind kind,
            String actionId,
            String command,
            ItemSelector source,
            String target,
            VanillaKeybindCatalog.Category vanillaCategory,
            VanillaKeybindCatalog.Entry vanillaEntry,
            BulkStorageItem bulkSource,
            String bulkQuantity,
            BulkDestinationKind bulkDestinationKind,
            InventoryReference bulkCapturedDestination,
            SmartImproveSourceMode smartImproveSourceMode) {
        this.kind = kind;
        this.actionId = actionId == null ? "" : actionId;
        this.command = command == null ? "" : command;
        this.source = source == null ? ItemSelector.currentActive() : source;
        this.target = target == null ? "" : target;
        this.vanillaCategory = vanillaCategory;
        this.vanillaEntry = vanillaEntry;
        this.bulkSource = bulkSource;
        this.bulkQuantity = bulkQuantity == null ? "" : bulkQuantity;
        this.bulkDestinationKind = bulkDestinationKind;
        this.bulkCapturedDestination = bulkCapturedDestination;
        this.smartImproveSourceMode = smartImproveSourceMode == null
                ? SmartImproveSourceMode.TOOLBELT_THEN_INVENTORY
                : smartImproveSourceMode;
    }

    public KeybindStep toStep(ActionNameLookup names) {
        if (kind == null)
            throw new IllegalArgumentException(Messages.text("validation.action_missing"));
        if (kind == StepKind.ACTIVATE_TOOL)
            return new ActivateToolStep(TargetCodec.decode(target));
        if (kind == StepKind.SMART_IMPROVE)
            return new SmartImproveStep(TargetCodec.decode(target), smartImproveSourceMode);
        if (kind == StepKind.BULK_TRANSFER) {
            final int quantity;
            try {
                quantity = Integer.parseInt(bulkQuantity.trim());
            } catch (NumberFormatException invalid) {
                throw new IllegalArgumentException(Messages.text(
                        "validation.bulk_quantity_number"));
            }
            return new BulkTransferStep(BulkStorageItem.copyOf(bulkSource), quantity,
                    bulkDestinationKind,
                    InventoryReference.copyOf(bulkCapturedDestination));
        }
        if (kind == StepKind.CONSOLE_COMMAND) {
            if (command.trim().isEmpty())
                throw new IllegalArgumentException(Messages.text("validation.console_missing"));
            return new ConsoleCommandStep(command);
        }
        if (kind == StepKind.VANILLA_ACTION) {
            if (vanillaCategory == null || vanillaCategory.getEntries().isEmpty())
                throw new IllegalArgumentException(
                        Messages.text("validation.vanilla_category_empty"));
            if (vanillaEntry == null)
                throw new IllegalArgumentException(Messages.text("validation.vanilla_missing"));
            return VANILLA_STEPS.create(vanillaCategory, vanillaEntry, source,
                    TargetCodec.decode(target));
        }

        String value = actionId.trim();
        if (value.isEmpty())
            throw new IllegalArgumentException(Messages.text("validation.capture_required"));
        final int parsed;
        try {
            parsed = Integer.parseInt(value);
        } catch (NumberFormatException invalid) {
            throw new IllegalArgumentException(Messages.text("validation.action_id_invalid", value));
        }
        if (parsed < Short.MIN_VALUE || parsed > Short.MAX_VALUE)
            throw new IllegalArgumentException(Messages.text("validation.action_id_range"));
        short id = (short) parsed;
        String name = names == null ? "" : names.find(id);
        return new ActionStep(id, source, TargetCodec.decode(target), name);
    }
}
