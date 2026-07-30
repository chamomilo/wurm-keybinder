package org.keybinder.wurm.storage;

import org.keybinder.wurm.command.TargetCodec;
import org.keybinder.wurm.model.ActionStep;
import org.keybinder.wurm.model.ActivateToolStep;
import org.keybinder.wurm.model.ConsoleCommandStep;
import org.keybinder.wurm.model.KeybindStep;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.model.KeybindVariant;
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
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;

public final class KeybindStore {
    public static final int SCHEMA_VERSION = 6;
    private final Path file;
    private final CustomActionsImporter customActionsImporter = new CustomActionsImporter();
    private volatile boolean recoveredFromBackup;

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
        if (!Files.exists(file)) return new ArrayList<KeybindRecord>();
        try {
            return loadFrom(file);
        } catch (IOException | RuntimeException primaryFailure) {
            Path backup = backupFile();
            if (!Files.isRegularFile(backup))
                throw asIo("Unable to read Keybinder data", primaryFailure);
            try {
                List<KeybindRecord> recovered = loadFrom(backup);
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

    private List<KeybindRecord> loadFrom(Path source) throws IOException {
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
            records.add(record);
        }
        return records;
    }

    public void save(List<KeybindRecord> records) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
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
        try {
            if (Files.exists(file))
                Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
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
                        steps.add(new ActionStep((short) actionId, TargetCodec.decode(value)));
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

    private static IOException asIo(String message, Throwable cause) {
        return cause instanceof IOException
                ? (IOException) cause : new IOException(message, cause);
    }
}
