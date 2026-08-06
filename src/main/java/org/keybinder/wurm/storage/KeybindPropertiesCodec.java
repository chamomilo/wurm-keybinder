package org.keybinder.wurm.storage;

import org.keybinder.wurm.codec.KeybindStepCodec;
import org.keybinder.wurm.command.ItemSelectorCodec;
import org.keybinder.wurm.command.TargetCodec;
import org.keybinder.wurm.migration.CustomActionsImporter;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.ActivateToolStep;
import org.keybinder.wurm.model.ArcheologyIdentifyStep;
import org.keybinder.wurm.model.BulkTransferStep;
import org.keybinder.wurm.model.ConsoleCommandStep;
import org.keybinder.wurm.model.ItemSelector;
import org.keybinder.wurm.model.KeybindLimits;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.model.KeybindVariant;
import org.keybinder.wurm.model.RecordType;
import org.keybinder.wurm.model.SmartImproveStep;
import org.keybinder.wurm.model.StepKind;
import org.keybinder.wurm.model.TargetSpec;
import org.keybinder.wurm.model.VanillaActionStep;
import org.keybinder.wurm.validation.KeybindValidator;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

/** Encodes and decodes the versioned Properties representation without doing file I/O. */
final class KeybindPropertiesCodec {
    static final int SCHEMA_VERSION = 10;

    static final class Decoded {
        private final List<KeybindRecord> records;
        private final int schema;
        private final boolean valuePackProvided;

        private Decoded(List<KeybindRecord> records, int schema,
                        boolean valuePackProvided) {
            this.records = records;
            this.schema = schema;
            this.valuePackProvided = valuePackProvided;
        }

        List<KeybindRecord> getRecords() { return records; }
        int getSchema() { return schema; }
        boolean wasValuePackProvided() { return valuePackProvided; }
    }

    private final CustomActionsImporter customActionsImporter =
            new CustomActionsImporter();

    Decoded decode(Properties props) throws IOException {
        int schema = Integer.parseInt(props.getProperty("schema", "1"));
        if (schema > SCHEMA_VERSION)
            throw new IOException("Unsupported Keybinder schema " + schema);
        int count = Integer.parseInt(props.getProperty("count", "0"));
        List<KeybindRecord> records = new ArrayList<KeybindRecord>();
        for (int i = 0; i < count; i++) {
            String prefix = "record." + i + ".";
            String id = decodeText(props.getProperty(prefix + "id", ""));
            String name = decodeText(props.getProperty(prefix + "name", ""));
            String key = decodeText(props.getProperty(prefix + "key", ""));
            KeybindRecord record;
            if (schema >= 5) {
                int variantCount = Integer.parseInt(
                        props.getProperty(prefix + "variantCount", "0"));
                List<KeybindVariant> variants = new ArrayList<KeybindVariant>();
                for (int v = 0; v < variantCount; v++) {
                    String variantPrefix = prefix + "variant." + v + ".";
                    String variantId = decodeText(
                            props.getProperty(variantPrefix + "id", ""));
                    String subName = decodeText(
                            props.getProperty(variantPrefix + "subName", ""));
                    int stepCount = Integer.parseInt(
                            props.getProperty(variantPrefix + "stepCount", "0"));
                    variants.add(new KeybindVariant(variantId, subName,
                            readSteps(props, variantPrefix, stepCount, id, schema)));
                }
                record = new KeybindRecord(id, name, key, variants,
                        decodeText(props.getProperty(prefix + "activeVariantId", "")));
            } else if (schema >= 3) {
                int stepCount = Integer.parseInt(
                        props.getProperty(prefix + "stepCount", "0"));
                record = new KeybindRecord(id, name, key,
                        readSteps(props, prefix, stepCount, id, schema));
            } else {
                RecordType type = RecordType.valueOf(
                        props.getProperty(prefix + "type", "ACTION_CHAIN"));
                String command = decodeText(props.getProperty(prefix + "command", ""));
                if (type == RecordType.ACTION_CHAIN && !command.isEmpty()) {
                    record = new KeybindRecord(id, name, key,
                            customActionsImporter.importCommand(command));
                } else {
                    List<KeybindStep> steps = new ArrayList<KeybindStep>();
                    if (!command.isEmpty())
                        steps.add(new ConsoleCommandStep(command,
                                type == RecordType.RAW_VANILLA_COMMAND));
                    record = new KeybindRecord(id, name, key, steps);
                }
                record.setPreviousManagedCommand(command);
            }
            readRecordMetadata(props, prefix, schema, record);
            records.add(record);
        }
        validateRecords(records);
        boolean valuePackProvided = schema >= 9
                && Boolean.parseBoolean(props.getProperty("valuePackProvided", "false"));
        return new Decoded(records, schema, valuePackProvided);
    }

    Properties encode(List<KeybindRecord> records, boolean valuePackProvided)
            throws IOException {
        validateRecords(records);
        Properties props = new Properties();
        props.setProperty("schema", String.valueOf(SCHEMA_VERSION));
        props.setProperty("count", String.valueOf(records.size()));
        props.setProperty("valuePackProvided", String.valueOf(valuePackProvided));
        for (int i = 0; i < records.size(); i++) {
            KeybindRecord record = records.get(i);
            String prefix = "record." + i + ".";
            props.setProperty(prefix + "id", encodeText(record.getId()));
            props.setProperty(prefix + "name", encodeText(record.getName()));
            props.setProperty(prefix + "key", encodeText(record.getKey()));
            props.setProperty(prefix + "variantCount",
                    String.valueOf(record.getVariants().size()));
            props.setProperty(prefix + "activeVariantId",
                    encodeText(record.getActiveVariantId()));
            props.setProperty(prefix + "hudMulti", String.valueOf(record.isHudMulti()));
            props.setProperty(prefix + "valuePack", String.valueOf(record.isValuePack()));
            for (int v = 0; v < record.getVariants().size(); v++) {
                KeybindVariant variant = record.getVariants().get(v);
                String variantPrefix = prefix + "variant." + v + ".";
                props.setProperty(variantPrefix + "id", encodeText(variant.getId()));
                props.setProperty(variantPrefix + "subName", encodeText(variant.getSubName()));
                props.setProperty(variantPrefix + "stepCount",
                        String.valueOf(variant.getSteps().size()));
                writeSteps(props, variantPrefix, variant.getSteps());
            }
            props.setProperty(prefix + "enabled", String.valueOf(record.isEnabled()));
            props.setProperty(prefix + "reason", encodeText(record.getDisabledReason()));
            props.setProperty(prefix + "originalKey", encodeText(record.getOriginalKey()));
            props.setProperty(prefix + "originalCommand",
                    encodeText(record.getOriginalCommand()));
            props.setProperty(prefix + "previousManagedCommand",
                    encodeText(record.getPreviousManagedCommand()));
            props.setProperty(prefix + "createdByUser",
                    encodeText(record.getCreatedByUser()));
            props.setProperty(prefix + "createdOnServer",
                    encodeText(record.getCreatedOnServer()));
        }
        return props;
    }

    private static void readRecordMetadata(Properties props, String prefix, int schema,
                                           KeybindRecord record) {
        record.setEnabled(Boolean.parseBoolean(
                props.getProperty(prefix + "enabled", "true")));
        record.setDisabledReason(decodeText(props.getProperty(prefix + "reason", "")));
        record.setOriginalKey(decodeText(props.getProperty(prefix + "originalKey", "")));
        record.setOriginalCommand(
                decodeText(props.getProperty(prefix + "originalCommand", "")));
        if (schema >= 3)
            record.setPreviousManagedCommand(decodeText(
                    props.getProperty(prefix + "previousManagedCommand", "")));
        if (schema >= 4) {
            record.setCreatedByUser(
                    decodeText(props.getProperty(prefix + "createdByUser", "")));
            record.setCreatedOnServer(
                    decodeText(props.getProperty(prefix + "createdOnServer", "")));
        }
        record.setHudMulti(schema >= 8
                && Boolean.parseBoolean(props.getProperty(prefix + "hudMulti", "false")));
        record.setValuePack(schema >= 9
                && Boolean.parseBoolean(props.getProperty(prefix + "valuePack", "false")));
    }

    private static List<KeybindStep> readSteps(Properties props, String ownerPrefix,
                                                int count, String recordId, int schema)
            throws IOException {
        List<KeybindStep> steps = new ArrayList<KeybindStep>();
        for (int j = 0; j < count; j++) {
            String stepPrefix = ownerPrefix + "step." + j + ".";
            if (schema >= 8) {
                steps.add(KeybindStepCodec.readStore(props, stepPrefix, recordId));
                continue;
            }
            StepKind kind = StepKind.valueOf(
                    props.getProperty(stepPrefix + "kind", "CUSTOM_ACTION"));
            String value = decodeText(props.getProperty(
                    stepPrefix + (schema >= 6 ? "target" : "value"), ""));
            switch (kind) {
                case ACTIVATE_TOOL:
                    steps.add(new ActivateToolStep(TargetCodec.decode(value)));
                    break;
                case SMART_IMPROVE:
                    steps.add(new SmartImproveStep(TargetCodec.decode(value)));
                    break;
                case VANILLA_ACTION:
                    if (schema >= 6)
                        value = decodeText(props.getProperty(stepPrefix + "value", ""));
                    steps.add(new VanillaActionStep(value));
                    break;
                case CONSOLE_COMMAND:
                    if (schema >= 6)
                        value = decodeText(props.getProperty(stepPrefix + "value", ""));
                    steps.add(new ConsoleCommandStep(value, Boolean.parseBoolean(
                            props.getProperty(stepPrefix + "preserveExact", "false"))));
                    break;
                case CUSTOM_ACTION:
                    int actionId = Integer.parseInt(
                            props.getProperty(stepPrefix + "actionId", "0"));
                    if (actionId < Short.MIN_VALUE || actionId > Short.MAX_VALUE)
                        throw new IOException(
                                "Action ID outside short range in record " + recordId);
                    if (schema < 6 && "toolbelt".equalsIgnoreCase(value.trim())) {
                        steps.add(new ActivateToolStep(TargetSpec.toolbeltSlot(actionId)));
                    } else {
                        String lastKnownName = schema >= 7 ? decodeText(
                                props.getProperty(stepPrefix + "lastKnownName", "")) : "";
                        ItemSelector source = ItemSelector.currentActive();
                        steps.add(new ActionStep((short) actionId, source,
                                TargetCodec.decode(value), lastKnownName));
                    }
                    break;
                default:
                    throw new IOException("Unsupported step kind " + kind);
            }
        }
        return steps;
    }

    private static void writeSteps(Properties props, String ownerPrefix,
                                   List<KeybindStep> steps) throws IOException {
        for (int j = 0; j < steps.size(); j++)
            KeybindStepCodec.writeStore(
                    props, ownerPrefix + "step." + j + ".", steps.get(j));
    }

    private static String encodeText(String value) {
        if (value == null) value = "";
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decodeText(String value) {
        if (value == null || value.isEmpty()) return "";
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    static void validateRecords(List<KeybindRecord> records) throws IOException {
        try {
            KeybindValidator.validateStorage(records);
            for (KeybindRecord record : records)
                for (KeybindVariant variant : record.getVariants())
                    for (KeybindStep step : variant.getSteps())
                        validateStepEncoding(step);
        } catch (IllegalArgumentException failure) {
            throw new IOException("Invalid Keybinder records", failure);
        }
    }

    private static void validateStepEncoding(KeybindStep step) throws IOException {
        if (step instanceof ActionStep) {
            ActionStep action = (ActionStep) step;
            requireEncodedLength(TargetCodec.encode(action.getTarget()), "target");
            requireEncodedLength(ItemSelectorCodec.encode(action.getSource()), "source");
            requireLength(action.getLastKnownName(),
                    KeybindLimits.MAX_ENCODED_FIELD_LENGTH, "action name");
        } else if (step instanceof ActivateToolStep) {
            requireEncodedLength(TargetCodec.encode(
                    ((ActivateToolStep) step).getTarget()), "activation target");
        } else if (step instanceof SmartImproveStep) {
            requireEncodedLength(TargetCodec.encode(
                    ((SmartImproveStep) step).getTarget()), "improve target");
        } else if (step instanceof ArcheologyIdentifyStep) {
            requireEncodedLength(TargetCodec.encode(
                    ((ArcheologyIdentifyStep) step).getTarget()),
                    "archeology identify target");
        } else if (step instanceof BulkTransferStep) {
            BulkTransferStep bulk = (BulkTransferStep) step;
            if (bulk.getSource() != null) {
                if (bulk.getSource().getStorage() != null)
                    requireLength(bulk.getSource().getStorage().getName(),
                            KeybindLimits.MAX_ENCODED_FIELD_LENGTH, "bulk storage name");
                if (bulk.getSource().getItem() != null)
                    requireLength(bulk.getSource().getItem().getName(),
                            KeybindLimits.MAX_ENCODED_FIELD_LENGTH, "bulk item name");
            }
            if (bulk.getCapturedDestination() != null)
                requireLength(bulk.getCapturedDestination().getName(),
                        KeybindLimits.MAX_ENCODED_FIELD_LENGTH, "bulk destination name");
        } else if (!(step instanceof VanillaActionStep)
                && !(step instanceof ConsoleCommandStep)) {
            throw new IOException("Unsupported keybind step " + step.getClass().getName());
        }
    }

    private static void requireEncodedLength(String value, String field) throws IOException {
        if (value != null && value.length() > KeybindLimits.MAX_ENCODED_FIELD_LENGTH)
            throw new IOException(field + " is too long");
    }

    private static void requireLength(String value, int maximum, String field)
            throws IOException {
        if (value != null && value.length() > maximum)
            throw new IOException(field + " is too long");
    }

    static boolean sameRecords(List<KeybindRecord> left, List<KeybindRecord> right) {
        if (left.size() != right.size()) return false;
        for (int i = 0; i < left.size(); i++)
            if (!sameRecord(left.get(i), right.get(i))) return false;
        return true;
    }

    private static boolean sameRecord(KeybindRecord left, KeybindRecord right) {
        if (!equal(left.getId(), right.getId()) || !textEqual(left.getName(), right.getName())
                || !textEqual(left.getKey(), right.getKey())
                || !equal(left.getActiveVariantId(), right.getActiveVariantId())
                || left.isEnabled() != right.isEnabled()
                || !textEqual(left.getDisabledReason(), right.getDisabledReason())
                || !textEqual(left.getOriginalKey(), right.getOriginalKey())
                || !textEqual(left.getOriginalCommand(), right.getOriginalCommand())
                || !textEqual(left.getPreviousManagedCommand(), right.getPreviousManagedCommand())
                || !textEqual(left.getCreatedByUser(), right.getCreatedByUser())
                || !textEqual(left.getCreatedOnServer(), right.getCreatedOnServer())
                || left.isHudMulti() != right.isHudMulti()
                || left.isValuePack() != right.isValuePack()
                || left.getVariants().size() != right.getVariants().size()) return false;
        for (int i = 0; i < left.getVariants().size(); i++) {
            KeybindVariant a = left.getVariants().get(i);
            KeybindVariant b = right.getVariants().get(i);
            if (!equal(a.getId(), b.getId()) || !equal(a.getSubName(), b.getSubName())
                    || a.getSteps().size() != b.getSteps().size()) return false;
            for (int j = 0; j < a.getSteps().size(); j++)
                if (!sameStep(a.getSteps().get(j), b.getSteps().get(j))) return false;
        }
        return true;
    }

    private static boolean sameStep(KeybindStep left, KeybindStep right) {
        if (left.getKind() != right.getKind()) return false;
        if (left instanceof ActionStep) {
            ActionStep a = (ActionStep) left;
            ActionStep b = (ActionStep) right;
            return a.getActionId() == b.getActionId() && a.getSource().equals(b.getSource())
                    && a.getTarget().equals(b.getTarget())
                    && equal(a.getLastKnownName(), b.getLastKnownName());
        }
        if (left instanceof ActivateToolStep)
            return ((ActivateToolStep) left).getTarget().equals(
                    ((ActivateToolStep) right).getTarget());
        if (left instanceof SmartImproveStep)
            return ((SmartImproveStep) left).getTarget().equals(
                    ((SmartImproveStep) right).getTarget())
                    && ((SmartImproveStep) left).getSourceMode()
                    == ((SmartImproveStep) right).getSourceMode();
        if (left instanceof ArcheologyIdentifyStep)
            return ((ArcheologyIdentifyStep) left).getTarget().equals(
                    ((ArcheologyIdentifyStep) right).getTarget())
                    && ((ArcheologyIdentifyStep) left).getSourceMode()
                    == ((ArcheologyIdentifyStep) right).getSourceMode();
        if (left instanceof BulkTransferStep) {
            BulkTransferStep a = (BulkTransferStep) left;
            BulkTransferStep b = (BulkTransferStep) right;
            return equal(a.getSource(), b.getSource())
                    && a.getQuantity() == b.getQuantity()
                    && a.getDestinationKind() == b.getDestinationKind()
                    && equal(a.getCapturedDestination(), b.getCapturedDestination());
        }
        if (left instanceof VanillaActionStep)
            return equal(((VanillaActionStep) left).getCommand(),
                    ((VanillaActionStep) right).getCommand());
        if (left instanceof ConsoleCommandStep) {
            ConsoleCommandStep a = (ConsoleCommandStep) left;
            ConsoleCommandStep b = (ConsoleCommandStep) right;
            return equal(a.getCommand(), b.getCommand())
                    && a.isPreserveExactText() == b.isPreserveExactText();
        }
        return false;
    }

    private static boolean equal(Object left, Object right) {
        return left == null ? right == null : left.equals(right);
    }

    private static boolean textEqual(String left, String right) {
        return (left == null ? "" : left).equals(right == null ? "" : right);
    }
}
