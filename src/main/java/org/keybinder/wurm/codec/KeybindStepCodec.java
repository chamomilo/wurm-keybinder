package org.keybinder.wurm.codec;

import org.keybinder.wurm.command.ItemSelectorCodec;
import org.keybinder.wurm.command.TargetCodec;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.ActivateToolStep;
import org.keybinder.wurm.model.ArcheologyIdentifySourceMode;
import org.keybinder.wurm.model.ArcheologyIdentifyStep;
import org.keybinder.wurm.model.ConsoleCommandStep;
import org.keybinder.wurm.model.BulkDestinationKind;
import org.keybinder.wurm.model.BulkStorageItem;
import org.keybinder.wurm.model.BulkTransferStep;
import org.keybinder.wurm.model.InventoryReference;
import org.keybinder.wurm.model.ItemSelector;
import org.keybinder.wurm.model.KeybindLimits;
import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.model.SmartImproveStep;
import org.keybinder.wurm.model.SmartImproveSourceMode;
import org.keybinder.wurm.model.StepKind;
import org.keybinder.wurm.model.TargetSpec;
import org.keybinder.wurm.model.VanillaActionStep;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Properties;

/** Current-schema step fields shared by persistent and portable stores. */
public final class KeybindStepCodec {
    private KeybindStepCodec() { }

    public static void writeStore(Properties properties, String prefix, KeybindStep step)
            throws IOException {
        write(properties, prefix, step, false);
    }

    public static void writeTransfer(Properties properties, String prefix, KeybindStep step)
            throws IOException {
        write(properties, prefix, step, true);
    }

    public static KeybindStep readStore(Properties properties, String prefix, String recordId)
            throws IOException {
        StepKind kind = kind(properties.getProperty(prefix + "kind", "CUSTOM_ACTION"));
        switch (kind) {
            case CUSTOM_ACTION:
                int actionId = number(properties.getProperty(prefix + "actionId", "0"),
                        "action ID");
                requireShort(actionId, "Action ID outside short range in record " + recordId);
                ItemSelector source = sourceFromFields(properties, prefix, false);
                return new ActionStep((short) actionId, source,
                        TargetCodec.decode(decoded(properties, prefix + "target", false)),
                        decoded(properties, prefix + "lastKnownName", false));
            case ACTIVATE_TOOL:
                return new ActivateToolStep(TargetCodec.decode(
                        decoded(properties, prefix + "target", false)));
            case SMART_IMPROVE:
                return new SmartImproveStep(TargetCodec.decode(
                        decoded(properties, prefix + "target", false)),
                        improveSourceMode(properties, prefix));
            case ARCHEOLOGY_IDENTIFY:
                return new ArcheologyIdentifyStep(TargetCodec.decode(
                        decoded(properties, prefix + "target", false)),
                        archeologySourceMode(properties, prefix));
            case BULK_TRANSFER:
                return readBulk(properties, prefix, false);
            case VANILLA_ACTION:
                return new VanillaActionStep(decoded(properties, prefix + "value", false));
            case CONSOLE_COMMAND:
                return new ConsoleCommandStep(decoded(properties, prefix + "value", false),
                        Boolean.parseBoolean(properties.getProperty(
                                prefix + "preserveExact", "false")));
            default:
                throw new IOException("Unsupported step kind " + kind);
        }
    }

    public static KeybindStep readTransfer(Properties properties, String prefix)
            throws IOException {
        StepKind kind = kind(required(properties, prefix + "kind"));
        switch (kind) {
            case CUSTOM_ACTION:
                int actionId = number(required(properties, prefix + "actionId"), "action ID");
                requireShort(actionId, "Action ID outside short range");
                ItemSelector source = sourceFromFields(properties, prefix, true);
                String encodedSource = decoded(properties, prefix + "source", true);
                if (!ItemSelectorCodec.encode(source).equals(encodedSource))
                    throw new IOException("Inconsistent source fields");
                TargetSpec target = TargetCodec.decode(decoded(
                        properties, prefix + "target", true));
                return new ActionStep((short) actionId, source, target,
                        decoded(properties, prefix + "lastKnownName", true));
            case ACTIVATE_TOOL:
                return new ActivateToolStep(TargetCodec.decode(
                        decoded(properties, prefix + "target", true)));
            case SMART_IMPROVE:
                return new SmartImproveStep(TargetCodec.decode(
                        decoded(properties, prefix + "target", true)),
                        improveSourceMode(properties, prefix));
            case ARCHEOLOGY_IDENTIFY:
                return new ArcheologyIdentifyStep(TargetCodec.decode(
                        decoded(properties, prefix + "target", true)),
                        archeologySourceMode(properties, prefix));
            case BULK_TRANSFER:
                return readBulk(properties, prefix, true);
            case VANILLA_ACTION:
                return new VanillaActionStep(command(properties, prefix));
            case CONSOLE_COMMAND:
                return new ConsoleCommandStep(command(properties, prefix),
                        strictBoolean(properties, prefix + "preserveExact", false));
            default:
                throw new IOException("Unsupported transfer step kind " + kind);
        }
    }

    private static void write(Properties properties, String prefix, KeybindStep step,
                              boolean transfer) throws IOException {
        properties.setProperty(prefix + "kind", step.getKind().name());
        if (step instanceof ActionStep) {
            ActionStep action = (ActionStep) step;
            properties.setProperty(prefix + "actionId", Short.toString(action.getActionId()));
            encoded(properties, prefix + "lastKnownName", action.getLastKnownName(), transfer);
            properties.setProperty(prefix + "sourceKind", action.getSource().getKind().name());
            properties.setProperty(prefix + "sourceSlot",
                    Integer.toString(action.getSource().getSlot()));
            properties.setProperty(prefix + "sourceObjectId",
                    Long.toString(action.getSource().getObjectId()));
            properties.setProperty(prefix + "sourceText", encode(action.getSource().getText()));
            encoded(properties, prefix + "target",
                    TargetCodec.encode(action.getTarget()), transfer);
            if (transfer)
                encoded(properties, prefix + "source",
                        ItemSelectorCodec.encode(action.getSource()), true);
        } else if (step instanceof ActivateToolStep) {
            encoded(properties, prefix + "target",
                    TargetCodec.encode(((ActivateToolStep) step).getTarget()), transfer);
        } else if (step instanceof SmartImproveStep) {
            SmartImproveStep improve = (SmartImproveStep) step;
            encoded(properties, prefix + "target",
                    TargetCodec.encode(improve.getTarget()), transfer);
            properties.setProperty(prefix + "improveSourceMode",
                    improve.getSourceMode().name());
        } else if (step instanceof ArcheologyIdentifyStep) {
            ArcheologyIdentifyStep identify = (ArcheologyIdentifyStep) step;
            encoded(properties, prefix + "target",
                    TargetCodec.encode(identify.getTarget()), transfer);
            properties.setProperty(prefix + "archeologySourceMode",
                    identify.getSourceMode().name());
        } else if (step instanceof BulkTransferStep) {
            writeBulk(properties, prefix, (BulkTransferStep) step, transfer);
        } else if (step instanceof VanillaActionStep) {
            writeCommand(properties, prefix,
                    ((VanillaActionStep) step).getCommand(), false, transfer);
        } else if (step instanceof ConsoleCommandStep) {
            ConsoleCommandStep command = (ConsoleCommandStep) step;
            writeCommand(properties, prefix, command.getCommand(),
                    command.isPreserveExactText(), transfer);
        } else {
            throw new IOException("Unsupported keybind step " + step.getClass().getName());
        }
    }

    private static void writeBulk(Properties properties, String prefix,
                                  BulkTransferStep step, boolean transfer)
            throws IOException {
        BulkStorageItem source = step.getSource();
        if (source == null || source.getStorage() == null || source.getItem() == null)
            throw new IOException("Bulk transfer source is missing");
        properties.setProperty(prefix + "bulkStorageId",
                Long.toString(source.getStorage().getId()));
        encoded(properties, prefix + "bulkStorageName", source.getStorage().getName(), transfer);
        properties.setProperty(prefix + "bulkItemId", Long.toString(source.getItem().getId()));
        encoded(properties, prefix + "bulkItemName", source.getItem().getName(), transfer);
        properties.setProperty(prefix + "quantity", Integer.toString(step.getQuantity()));
        if (step.getDestinationKind() == null)
            throw new IOException("Bulk transfer destination is missing");
        properties.setProperty(prefix + "destinationKind", step.getDestinationKind().name());
        InventoryReference destination = step.getCapturedDestination();
        properties.setProperty(prefix + "destinationId",
                Long.toString(destination == null ? 0L : destination.getId()));
        encoded(properties, prefix + "destinationName",
                destination == null ? "" : destination.getName(), transfer);
    }

    private static BulkTransferStep readBulk(Properties properties, String prefix,
                                             boolean strict) throws IOException {
        long storageId = longNumber(value(properties, prefix + "bulkStorageId", "0", strict),
                "bulk storage ID");
        String storageName = decoded(properties, prefix + "bulkStorageName", strict);
        long itemId = longNumber(value(properties, prefix + "bulkItemId", "0", strict),
                "bulk item ID");
        String itemName = decoded(properties, prefix + "bulkItemName", strict);
        int quantity = number(value(properties, prefix + "quantity", "1", strict),
                "bulk quantity");
        String destinationValue = value(properties, prefix + "destinationKind",
                BulkDestinationKind.PLAYER_INVENTORY.name(), strict);
        final BulkDestinationKind destinationKind;
        try {
            destinationKind = BulkDestinationKind.valueOf(destinationValue);
        } catch (IllegalArgumentException failure) {
            throw new IOException("Unsupported bulk destination " + destinationValue, failure);
        }
        long destinationId = longNumber(value(properties, prefix + "destinationId", "0", strict),
                "bulk destination ID");
        String destinationName = decoded(properties, prefix + "destinationName", strict);
        InventoryReference destination = destinationId == 0L && destinationName.isEmpty()
                ? null : new InventoryReference(destinationId, destinationName);
        return new BulkTransferStep(new BulkStorageItem(
                new InventoryReference(storageId, storageName),
                new InventoryReference(itemId, itemName)), quantity,
                destinationKind, destination);
    }

    private static String value(Properties properties, String key, String fallback,
                                boolean strict) throws IOException {
        return strict ? required(properties, key) : properties.getProperty(key, fallback);
    }

    private static ItemSelector sourceFromFields(Properties properties, String prefix,
                                                 boolean strict) throws IOException {
        String kind = strict ? required(properties, prefix + "sourceKind")
                : properties.getProperty(prefix + "sourceKind", "CURRENT_ACTIVE");
        int slot = number(strict ? required(properties, prefix + "sourceSlot")
                : properties.getProperty(prefix + "sourceSlot", "0"), "source slot");
        long objectId = longNumber(strict ? required(properties, prefix + "sourceObjectId")
                : properties.getProperty(prefix + "sourceObjectId", "0"), "source object ID");
        String text = decoded(properties, prefix + "sourceText", strict);
        ItemSelector source = ItemSelectorCodec.fromFields(kind, slot, objectId, text);
        if (strict && (source.getSlot() != slot || source.getObjectId() != objectId
                || !source.getText().equals(text)))
            throw new IOException("Invalid source parameters");
        return source;
    }

    private static void writeCommand(Properties properties, String prefix, String value,
                                     boolean preserveExact, boolean transfer) throws IOException {
        if (value != null && value.length() > KeybindLimits.MAX_COMMAND_LENGTH)
            throw new IOException("command is too long");
        properties.setProperty(prefix + (transfer ? "command" : "value"), encode(value));
        properties.setProperty(prefix + "preserveExact", Boolean.toString(preserveExact));
    }

    private static String command(Properties properties, String prefix) throws IOException {
        String value = decoded(properties, prefix + "command", true);
        if (value.length() > KeybindLimits.MAX_COMMAND_LENGTH)
            throw new IOException("command is too long");
        return value;
    }

    private static void encoded(Properties properties, String key, String value,
                                boolean enforceLimit) throws IOException {
        String encoded = encode(value);
        if (enforceLimit && encoded.length() > KeybindLimits.MAX_ENCODED_FIELD_LENGTH)
            throw new IOException("Encoded transfer field is too long");
        properties.setProperty(key, encoded);
    }

    private static String decoded(Properties properties, String key, boolean required)
            throws IOException {
        String encoded = required ? required(properties, key) : properties.getProperty(key, "");
        if (required && encoded.length() > KeybindLimits.MAX_ENCODED_FIELD_LENGTH)
            throw new IOException("Encoded transfer field is too long");
        try {
            return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException failure) {
            throw new IOException("Malformed Base64", failure);
        }
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString((value == null ? "" : value)
                .getBytes(StandardCharsets.UTF_8));
    }

    private static StepKind kind(String value) throws IOException {
        try {
            return StepKind.valueOf(value);
        } catch (IllegalArgumentException failure) {
            throw new IOException("Unsupported step kind " + value, failure);
        }
    }

    private static int number(String value, String field) throws IOException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException failure) {
            throw new IOException("Invalid " + field, failure);
        }
    }

    private static long longNumber(String value, String field) throws IOException {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException failure) {
            throw new IOException("Invalid " + field, failure);
        }
    }

    private static void requireShort(int value, String message) throws IOException {
        if (value < Short.MIN_VALUE || value > Short.MAX_VALUE)
            throw new IOException(message);
    }

    private static String required(Properties properties, String key) throws IOException {
        String value = properties.getProperty(key);
        if (value == null) throw new IOException("Missing transfer field " + key);
        return value;
    }

    private static boolean strictBoolean(Properties properties, String key, boolean fallback)
            throws IOException {
        String value = properties.getProperty(key);
        if (value == null) return fallback;
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        throw new IOException("Invalid boolean " + key);
    }

    private static SmartImproveSourceMode improveSourceMode(
            Properties properties, String prefix) throws IOException {
        String value = properties.getProperty(prefix + "improveSourceMode",
                SmartImproveSourceMode.TOOLBELT_THEN_INVENTORY.name());
        try {
            return SmartImproveSourceMode.valueOf(value);
        } catch (IllegalArgumentException failure) {
            throw new IOException("Unsupported Smart Improve source mode " + value, failure);
        }
    }

    private static ArcheologyIdentifySourceMode archeologySourceMode(
            Properties properties, String prefix) throws IOException {
        String value = properties.getProperty(prefix + "archeologySourceMode",
                ArcheologyIdentifySourceMode.TOOLBELT_THEN_INVENTORY.name());
        try {
            return ArcheologyIdentifySourceMode.valueOf(value);
        } catch (IllegalArgumentException failure) {
            throw new IOException("Unsupported Archeology Identify source mode "
                    + value, failure);
        }
    }
}
