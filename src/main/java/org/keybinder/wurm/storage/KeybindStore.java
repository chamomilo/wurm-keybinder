package org.keybinder.wurm.storage;

import org.keybinder.wurm.command.TargetCodec;
import org.keybinder.wurm.command.ItemSelectorCodec;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.ActivateToolStep;
import org.keybinder.wurm.model.ConsoleCommandStep;
import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.model.KeybindVariant;
import org.keybinder.wurm.model.ItemSelector;
import org.keybinder.wurm.model.KeybindLimits;
import org.keybinder.wurm.model.RecordType;
import org.keybinder.wurm.model.SmartImproveStep;
import org.keybinder.wurm.model.StepKind;
import org.keybinder.wurm.model.TargetSpec;
import org.keybinder.wurm.model.VanillaActionStep;
import org.keybinder.wurm.migration.CustomActionsImporter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

public final class KeybindStore {
    public static final int SCHEMA_VERSION = 8;
    private final Path file;
    private final CustomActionsImporter customActionsImporter = new CustomActionsImporter();
    private volatile boolean recoveredFromBackup;
    private volatile int loadedSchema = SCHEMA_VERSION;
    private volatile Path loadedSource;

    public KeybindStore(Path file) {
        this.file = file;
    }

    public long lastModifiedMillis() {
        try {
            return Files.exists(file) ? Files.getLastModifiedTime(file).toMillis() : 0L;
        } catch (IOException ignored) {
            return 0L;
        }
    }

    public List<KeybindRecord> load() throws IOException {
        recoveredFromBackup = false;
        loadedSchema = SCHEMA_VERSION;
        loadedSource = null;
        if (!Files.exists(file)) return new ArrayList<KeybindRecord>();
        try {
            return loadFrom(file, true);
        } catch (IOException | RuntimeException primaryFailure) {
            Path backup = backupFile();
            if (!Files.isRegularFile(backup))
                throw asIo("Unable to read Keybinder data", primaryFailure);
            try {
                List<KeybindRecord> recovered = loadFrom(backup, true);
                recoveredFromBackup = true;
                return recovered;
            } catch (IOException | RuntimeException backupFailure) {
                IOException failure = asIo("Unable to read Keybinder data or its backup",
                        primaryFailure);
                failure.addSuppressed(backupFailure);
                throw failure;
            }
        }
    }

    public boolean wasRecoveredFromBackup() {
        return recoveredFromBackup;
    }

    private List<KeybindRecord> loadFrom(Path source, boolean rememberSource) throws IOException {
        Properties props = new Properties();
        try (InputStream in = Files.newInputStream(source)) {
            props.load(in);
        }
        int schema = Integer.parseInt(props.getProperty("schema", "1"));
        if (schema > SCHEMA_VERSION) throw new IOException("Unsupported Keybinder schema " + schema);
        int count = Integer.parseInt(props.getProperty("count", "0"));
        List<KeybindRecord> records = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String prefix = "record." + i + ".";
            String id = decode(props.getProperty(prefix + "id", ""));
            String name = decode(props.getProperty(prefix + "name", ""));
            String key = decode(props.getProperty(prefix + "key", ""));
            KeybindRecord record;
            if (schema >= 5) {
                int variantCount = Integer.parseInt(props.getProperty(prefix + "variantCount", "0"));
                List<KeybindVariant> variants = new ArrayList<KeybindVariant>();
                for (int v = 0; v < variantCount; v++) {
                    String variantPrefix = prefix + "variant." + v + ".";
                    String variantId = decode(props.getProperty(variantPrefix + "id", ""));
                    String subName = decode(props.getProperty(variantPrefix + "subName", ""));
                    int stepCount = Integer.parseInt(props.getProperty(variantPrefix + "stepCount", "0"));
                    variants.add(new KeybindVariant(variantId, subName,
                            readSteps(props, variantPrefix, stepCount, id, schema)));
                }
                record = new KeybindRecord(id, name, key, variants,
                        decode(props.getProperty(prefix + "activeVariantId", "")));
            } else if (schema >= 3) {
                int stepCount = Integer.parseInt(props.getProperty(prefix + "stepCount", "0"));
                List<KeybindStep> steps = readSteps(props, prefix, stepCount, id, schema);
                record = new KeybindRecord(id, name, key, steps);
            } else {
                RecordType type = RecordType.valueOf(props.getProperty(prefix + "type", "ACTION_CHAIN"));
                String command = decode(props.getProperty(prefix + "command", ""));
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
            record.setEnabled(Boolean.parseBoolean(props.getProperty(prefix + "enabled", "true")));
            record.setDisabledReason(decode(props.getProperty(prefix + "reason", "")));
            record.setOriginalKey(decode(props.getProperty(prefix + "originalKey", "")));
            record.setOriginalCommand(decode(props.getProperty(prefix + "originalCommand", "")));
            if (schema >= 3)
                record.setPreviousManagedCommand(decode(
                        props.getProperty(prefix + "previousManagedCommand", "")));
            if (schema >= 4) {
                record.setCreatedByUser(decode(props.getProperty(prefix + "createdByUser", "")));
                record.setCreatedOnServer(decode(props.getProperty(prefix + "createdOnServer", "")));
            }
            record.setHudMulti(schema >= 8
                    && Boolean.parseBoolean(props.getProperty(prefix + "hudMulti", "false")));
            records.add(record);
        }
        validateRecords(records);
        if (rememberSource) {
            loadedSchema = schema;
            loadedSource = source;
        }
        return records;
    }

    public void save(List<KeybindRecord> records) throws IOException {
        validateRecords(records);
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        createPreV8BackupIfNeeded();
        Properties props = new Properties();
        props.setProperty("schema", String.valueOf(SCHEMA_VERSION));
        props.setProperty("count", String.valueOf(records.size()));
        for (int i = 0; i < records.size(); i++) {
            KeybindRecord record = records.get(i);
            String prefix = "record." + i + ".";
            props.setProperty(prefix + "id", encode(record.getId()));
            props.setProperty(prefix + "name", encode(record.getName()));
            props.setProperty(prefix + "key", encode(record.getKey()));
            props.setProperty(prefix + "variantCount", String.valueOf(record.getVariants().size()));
            props.setProperty(prefix + "activeVariantId", encode(record.getActiveVariantId()));
            record.setHudMulti(record.isHudMulti());
            props.setProperty(prefix + "hudMulti", String.valueOf(record.isHudMulti()));
            for (int v = 0; v < record.getVariants().size(); v++) {
                KeybindVariant variant = record.getVariants().get(v);
                String variantPrefix = prefix + "variant." + v + ".";
                props.setProperty(variantPrefix + "id", encode(variant.getId()));
                props.setProperty(variantPrefix + "subName", encode(variant.getSubName()));
                props.setProperty(variantPrefix + "stepCount", String.valueOf(variant.getSteps().size()));
                writeSteps(props, variantPrefix, variant.getSteps());
            }
            props.setProperty(prefix + "enabled", String.valueOf(record.isEnabled()));
            props.setProperty(prefix + "reason", encode(record.getDisabledReason()));
            props.setProperty(prefix + "originalKey", encode(record.getOriginalKey()));
            props.setProperty(prefix + "originalCommand", encode(record.getOriginalCommand()));
            props.setProperty(prefix + "previousManagedCommand",
                    encode(record.getPreviousManagedCommand()));
            props.setProperty(prefix + "createdByUser", encode(record.getCreatedByUser()));
            props.setProperty(prefix + "createdOnServer", encode(record.getCreatedOnServer()));
        }

        Path temp = file.resolveSibling(file.getFileName() + "."
                + java.util.UUID.randomUUID().toString() + ".tmp");
        Path backup = backupFile();
        try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE);
             OutputStream out = Channels.newOutputStream(channel)) {
            props.store(out, "Keybinder data");
            out.flush();
            channel.force(true);
        }
        boolean hadPrevious = Files.exists(file);
        boolean savingRecoveredState = recoveredFromBackup && loadedSource != null
                && Files.isRegularFile(loadedSource);
        try {
            // When load recovered from .bak, the main file is known-bad. Do not
            // replace the good rolling backup with that corrupt main file.
            if (Files.exists(file) && !savingRecoveredState)
                Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
            List<KeybindRecord> verified = loadFrom(file, false);
            if (!sameRecords(records, verified))
                throw new IOException("Keybinder save verification failed");
            loadedSchema = SCHEMA_VERSION;
            loadedSource = file;
            recoveredFromBackup = false;
        } catch (IOException | RuntimeException failure) {
            try {
                if (hadPrevious && savingRecoveredState)
                    Files.copy(loadedSource, file, StandardCopyOption.REPLACE_EXISTING);
                else if (hadPrevious && Files.isRegularFile(backup))
                    Files.copy(backup, file, StandardCopyOption.REPLACE_EXISTING);
                else if (!hadPrevious)
                    Files.deleteIfExists(file);
            } catch (Throwable rollbackFailure) {
                failure.addSuppressed(rollbackFailure);
            }
            throw asIo("Unable to save Keybinder data", failure);
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private static List<KeybindStep> readSteps(Properties props, String ownerPrefix, int count,
                                                String recordId, int schema) throws IOException {
        List<KeybindStep> steps = new ArrayList<KeybindStep>();
        for (int j = 0; j < count; j++) {
            String stepPrefix = ownerPrefix + "step." + j + ".";
            StepKind kind = StepKind.valueOf(props.getProperty(stepPrefix + "kind", "CUSTOM_ACTION"));
            String value = decode(props.getProperty(
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
                        value = decode(props.getProperty(stepPrefix + "value", ""));
                    steps.add(new VanillaActionStep(value));
                    break;
                case CONSOLE_COMMAND:
                    if (schema >= 6)
                        value = decode(props.getProperty(stepPrefix + "value", ""));
                    steps.add(new ConsoleCommandStep(value,
                            Boolean.parseBoolean(props.getProperty(stepPrefix + "preserveExact", "false"))));
                    break;
                case CUSTOM_ACTION:
                    int actionId = Integer.parseInt(props.getProperty(stepPrefix + "actionId", "0"));
                    if (actionId < Short.MIN_VALUE || actionId > Short.MAX_VALUE)
                        throw new IOException("Action ID outside short range in record " + recordId);
                    if (schema < 6 && "toolbelt".equalsIgnoreCase(value.trim())) {
                        steps.add(new ActivateToolStep(TargetSpec.toolbeltSlot(actionId)));
                    } else {
                        String lastKnownName = schema >= 7
                                ? decode(props.getProperty(stepPrefix + "lastKnownName", "")) : "";
                        ItemSelector source = schema >= 8
                                ? ItemSelectorCodec.fromFields(
                                props.getProperty(stepPrefix + "sourceKind", "CURRENT_ACTIVE"),
                                Integer.parseInt(props.getProperty(stepPrefix + "sourceSlot", "0")),
                                Long.parseLong(props.getProperty(stepPrefix + "sourceObjectId", "0")),
                                decode(props.getProperty(stepPrefix + "sourceText", "")))
                                : ItemSelector.currentActive();
                        steps.add(new ActionStep((short) actionId, source,
                                TargetCodec.decode(value), lastKnownName));
                    }
                    break;
                default: throw new IOException("Unsupported step kind " + kind);
            }
        }
        return steps;
    }

    private static void writeSteps(Properties props, String ownerPrefix, List<KeybindStep> steps) {
        for (int j = 0; j < steps.size(); j++) {
            KeybindStep step = steps.get(j);
            String stepPrefix = ownerPrefix + "step." + j + ".";
            props.setProperty(stepPrefix + "kind", step.getKind().name());
            if (step instanceof ActionStep) {
                ActionStep action = (ActionStep) step;
                props.setProperty(stepPrefix + "actionId", String.valueOf(action.getActionId()));
                props.setProperty(stepPrefix + "target",
                        encode(TargetCodec.encode(action.getTarget())));
                props.setProperty(stepPrefix + "lastKnownName",
                        encode(action.getLastKnownName()));
                props.setProperty(stepPrefix + "sourceKind", action.getSource().getKind().name());
                props.setProperty(stepPrefix + "sourceSlot",
                        String.valueOf(action.getSource().getSlot()));
                props.setProperty(stepPrefix + "sourceObjectId",
                        String.valueOf(action.getSource().getObjectId()));
                props.setProperty(stepPrefix + "sourceText",
                        encode(action.getSource().getText()));
            } else if (step instanceof ActivateToolStep) {
                props.setProperty(stepPrefix + "target", encode(TargetCodec.encode(
                        ((ActivateToolStep) step).getTarget())));
            } else if (step instanceof SmartImproveStep) {
                props.setProperty(stepPrefix + "target", encode(TargetCodec.encode(
                        ((SmartImproveStep) step).getTarget())));
            } else if (step instanceof VanillaActionStep) {
                props.setProperty(stepPrefix + "value",
                        encode(((VanillaActionStep) step).getCommand()));
            } else if (step instanceof ConsoleCommandStep) {
                ConsoleCommandStep command = (ConsoleCommandStep) step;
                props.setProperty(stepPrefix + "value", encode(command.getCommand()));
                props.setProperty(stepPrefix + "preserveExact",
                        String.valueOf(command.isPreserveExactText()));
            }
        }
    }

    private static String encode(String value) {
        if (value == null) value = "";
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        if (value == null || value.isEmpty()) return "";
        return new String(Base64.getDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private Path backupFile() {
        return file.resolveSibling(file.getFileName() + ".bak");
    }

    public Path preV8BackupFile() {
        String name = file.getFileName().toString();
        if (name.endsWith(".properties"))
            name = name.substring(0, name.length() - ".properties".length());
        return file.resolveSibling(name + ".pre-v8.properties");
    }

    private void createPreV8BackupIfNeeded() throws IOException {
        Path source = loadedSource;
        if (loadedSchema >= SCHEMA_VERSION || source == null || !Files.isRegularFile(source)) return;
        Path destination = preV8BackupFile();
        if (Files.exists(destination)) return;
        try (InputStream input = Files.newInputStream(source);
             FileChannel output = FileChannel.open(destination, StandardOpenOption.CREATE_NEW,
                     StandardOpenOption.WRITE)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) >= 0) {
                if (count == 0) continue;
                ByteBuffer bytes = ByteBuffer.wrap(buffer, 0, count);
                while (bytes.hasRemaining()) output.write(bytes);
            }
            output.force(true);
        }
    }

    private static void validateRecords(List<KeybindRecord> records) throws IOException {
        if (records == null) throw new IOException("Keybind records are missing");
        if (records.size() > KeybindLimits.MAX_RECORDS_IN_TRANSFER)
            throw new IOException("Too many Keybinder records");
        for (KeybindRecord record : records) {
            requireLength(record.getName(), KeybindLimits.MAX_RECORD_NAME_LENGTH, "record name");
            if (record.getVariants().isEmpty()
                    || record.getVariants().size() > KeybindLimits.MAX_VARIANTS)
                throw new IOException("Invalid variant count in record " + record.getId());
            for (KeybindVariant variant : record.getVariants()) {
                requireLength(variant.getSubName(), KeybindLimits.MAX_VARIANT_NAME_LENGTH,
                        "variant name");
                if (variant.getSteps().size() > KeybindLimits.MAX_STEPS_PER_VARIANT)
                    throw new IOException("Too many steps in record " + record.getId());
                for (KeybindStep step : variant.getSteps()) validateStep(step);
            }
        }
    }

    private static void validateStep(KeybindStep step) throws IOException {
        try {
            if (step instanceof ActionStep) {
                ActionStep action = (ActionStep) step;
                requireEncodedLength(TargetCodec.encode(action.getTarget()), "target");
                requireEncodedLength(ItemSelectorCodec.encode(action.getSource()), "source");
                requireLength(action.getLastKnownName(), KeybindLimits.MAX_ENCODED_FIELD_LENGTH,
                        "action name");
            } else if (step instanceof ActivateToolStep) {
                requireEncodedLength(TargetCodec.encode(((ActivateToolStep) step).getTarget()),
                        "activation target");
            } else if (step instanceof SmartImproveStep) {
                requireEncodedLength(TargetCodec.encode(((SmartImproveStep) step).getTarget()),
                        "improve target");
            } else if (step instanceof VanillaActionStep) {
                requireLength(((VanillaActionStep) step).getCommand(),
                        KeybindLimits.MAX_COMMAND_LENGTH, "vanilla command");
            } else if (step instanceof ConsoleCommandStep) {
                requireLength(((ConsoleCommandStep) step).getCommand(),
                        KeybindLimits.MAX_COMMAND_LENGTH, "console command");
            } else throw new IOException("Unsupported keybind step " + step.getClass().getName());
        } catch (IllegalArgumentException failure) {
            throw new IOException("Invalid Keybinder step", failure);
        }
    }

    private static void requireEncodedLength(String value, String field) throws IOException {
        if (value != null && value.length() > KeybindLimits.MAX_ENCODED_FIELD_LENGTH)
            throw new IOException(field + " is too long");
    }

    private static void requireLength(String value, int maximum, String field) throws IOException {
        if (value != null && value.length() > maximum)
            throw new IOException(field + " is too long");
    }

    private static boolean sameRecords(List<KeybindRecord> left, List<KeybindRecord> right) {
        if (left.size() != right.size()) return false;
        for (int i = 0; i < left.size(); i++) if (!sameRecord(left.get(i), right.get(i))) return false;
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
                || left.getVariants().size() != right.getVariants().size()) return false;
        for (int i = 0; i < left.getVariants().size(); i++) {
            KeybindVariant a = left.getVariants().get(i), b = right.getVariants().get(i);
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
            ActionStep a = (ActionStep) left, b = (ActionStep) right;
            return a.getActionId() == b.getActionId() && a.getSource().equals(b.getSource())
                    && a.getTarget().equals(b.getTarget())
                    && equal(a.getLastKnownName(), b.getLastKnownName());
        }
        if (left instanceof ActivateToolStep)
            return ((ActivateToolStep) left).getTarget().equals(((ActivateToolStep) right).getTarget());
        if (left instanceof SmartImproveStep)
            return ((SmartImproveStep) left).getTarget().equals(((SmartImproveStep) right).getTarget());
        if (left instanceof VanillaActionStep)
            return equal(((VanillaActionStep) left).getCommand(),
                    ((VanillaActionStep) right).getCommand());
        if (left instanceof ConsoleCommandStep) {
            ConsoleCommandStep a = (ConsoleCommandStep) left, b = (ConsoleCommandStep) right;
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

    private static IOException asIo(String message, Throwable cause) {
        return cause instanceof IOException
                ? (IOException) cause : new IOException(message, cause);
    }
}
