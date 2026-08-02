package org.keybinder.wurm.transfer;

import org.keybinder.wurm.codec.KeybindStepCodec;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import org.keybinder.wurm.model.KeybindLimits;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.model.KeybindStep;

/** Strict codec and atomic file store for portable .keybinder bundles. */
public final class KeybindTransferStore {
    public static final String EXTENSION = ".keybinder";
    public static final int VERSION = 1;
    public static final int DEFINITION_SCHEMA = 8;

    public void write(Path file, List<KeybindRecord> records, String originUser,
                      String originServer, String keybinderVersion) throws IOException {
        if (records.size() > KeybindLimits.MAX_RECORDS_IN_TRANSFER)
            throw new IOException("Too many records for transfer");
        Properties properties = new Properties();
        properties.setProperty("format", "keybinder-transfer");
        properties.setProperty("version", Integer.toString(VERSION));
        properties.setProperty("definitionSchema", Integer.toString(DEFINITION_SCHEMA));
        properties.setProperty("count", Integer.toString(records.size()));
        properties.setProperty("originUser", encode(originUser));
        properties.setProperty("originServer", encode(originServer));
        properties.setProperty("keybinderVersion", encode(keybinderVersion));
        properties.setProperty("exportedAt", encode(Instant.now().toString()));
        for (int i = 0; i < records.size(); i++)
            writeDefinition(properties, "record." + i + ".",
                    PortableKeybindDefinition.fromRecord(records.get(i)));
        writeAtomic(file, properties);
    }

    public List<PortableKeybindDefinition> read(Path file) throws IOException {
        if (!Files.isRegularFile(file)) throw new IOException("Transfer file does not exist");
        if (Files.size(file) > KeybindLimits.MAX_TRANSFER_FILE_BYTES)
            throw new IOException("Transfer file exceeds 10 MiB");
        try (InputStream input = Files.newInputStream(file)) { return read(input); }
    }

    /** Reads a bundled transfer while enforcing the same limit as file imports. */
    public List<PortableKeybindDefinition> read(InputStream input) throws IOException {
        if (input == null) throw new IOException("Transfer input is missing");
        Properties properties = new Properties();
        properties.load(new SizeLimitedInputStream(input,
                KeybindLimits.MAX_TRANSFER_FILE_BYTES));
        return read(properties);
    }

    private List<PortableKeybindDefinition> read(Properties properties) throws IOException {
        if (!"keybinder-transfer".equals(properties.getProperty("format")))
            throw new IOException("Invalid Keybinder transfer format");
        if (parse(properties, "version") != VERSION)
            throw new IOException("Unsupported Keybinder transfer version");
        if (parse(properties, "definitionSchema") != DEFINITION_SCHEMA)
            throw new IOException("Unsupported Keybinder definition schema");
        int count = parse(properties, "count");
        if (count < 0 || count > KeybindLimits.MAX_RECORDS_IN_TRANSFER)
            throw new IOException("Invalid transfer record count");
        List<PortableKeybindDefinition> result = new ArrayList<PortableKeybindDefinition>();
        try {
            for (int i = 0; i < count; i++)
                result.add(readDefinition(properties, "record." + i + "."));
        } catch (IllegalArgumentException failure) {
            throw new IOException("Malformed Keybinder transfer", failure);
        }
        return result;
    }

    private static final class SizeLimitedInputStream extends InputStream {
        private final InputStream delegate;
        private final long maximum;
        private long count;

        private SizeLimitedInputStream(InputStream delegate, long maximum) {
            this.delegate = delegate;
            this.maximum = maximum;
        }

        @Override public int read() throws IOException {
            int value = delegate.read();
            if (value >= 0) increment(1);
            return value;
        }

        @Override public int read(byte[] bytes, int offset, int length) throws IOException {
            int read = delegate.read(bytes, offset, length);
            if (read > 0) increment(read);
            return read;
        }

        private void increment(int amount) throws IOException {
            count += amount;
            if (count > maximum) throw new IOException("Transfer file exceeds 10 MiB");
        }
    }

    private static void writeDefinition(Properties properties, String prefix,
                                        PortableKeybindDefinition definition) throws IOException {
        requireLength(definition.getName(), KeybindLimits.MAX_RECORD_NAME_LENGTH, "name");
        requireLength(definition.getIntendedKey(), KeybindLimits.MAX_ENCODED_FIELD_LENGTH, "key");
        if (definition.getVariants().isEmpty()
                || definition.getVariants().size() > KeybindLimits.MAX_VARIANTS)
            throw new IOException("Invalid variant count");
        if (definition.getActiveVariantIndex() < 0
                || definition.getActiveVariantIndex() >= definition.getVariants().size())
            throw new IOException("Invalid active variant");
        properties.setProperty(prefix + "name", encode(definition.getName()));
        properties.setProperty(prefix + "key", encode(definition.getIntendedKey()));
        properties.setProperty(prefix + "hudMulti", Boolean.toString(definition.isHudMulti()));
        properties.setProperty(prefix + "activeVariantIndex",
                Integer.toString(definition.getActiveVariantIndex()));
        properties.setProperty(prefix + "variantCount",
                Integer.toString(definition.getVariants().size()));
        for (int v = 0; v < definition.getVariants().size(); v++) {
            PortableKeybindDefinition.Variant variant = definition.getVariants().get(v);
            String vp = prefix + "variant." + v + ".";
            requireLength(variant.getName(), KeybindLimits.MAX_VARIANT_NAME_LENGTH, "variant name");
            properties.setProperty(vp + "name", encode(variant.getName()));
            if (variant.getSteps().size() > KeybindLimits.MAX_STEPS_PER_VARIANT)
                throw new IOException("Invalid step count");
            properties.setProperty(vp + "stepCount", Integer.toString(variant.getSteps().size()));
            for (int s = 0; s < variant.getSteps().size(); s++)
                writeStep(properties, vp + "step." + s + ".", variant.getSteps().get(s));
        }
    }

    private static PortableKeybindDefinition readDefinition(Properties p, String prefix)
            throws IOException {
        String name = decoded(p, prefix + "name");
        String key = decoded(p, prefix + "key");
        requireLength(name, KeybindLimits.MAX_RECORD_NAME_LENGTH, "name");
        if (name.trim().isEmpty()) throw new IOException("Transfer record name is missing");
        requireLength(key, KeybindLimits.MAX_ENCODED_FIELD_LENGTH, "key");
        int variants = parse(p, prefix + "variantCount");
        if (variants < 1 || variants > KeybindLimits.MAX_VARIANTS)
            throw new IOException("Invalid variant count");
        int active = parse(p, prefix + "activeVariantIndex");
        if (active < 0 || active >= variants) throw new IOException("Invalid active variant");
        List<PortableKeybindDefinition.Variant> values =
                new ArrayList<PortableKeybindDefinition.Variant>();
        for (int v = 0; v < variants; v++) {
            String vp = prefix + "variant." + v + ".";
            String variantName = decoded(p, vp + "name");
            requireLength(variantName, KeybindLimits.MAX_VARIANT_NAME_LENGTH, "variant name");
            int stepCount = parse(p, vp + "stepCount");
            if (stepCount < 0 || stepCount > KeybindLimits.MAX_STEPS_PER_VARIANT)
                throw new IOException("Invalid step count");
            List<KeybindStep> steps = new ArrayList<KeybindStep>();
            for (int s = 0; s < stepCount; s++)
                steps.add(readStep(p, vp + "step." + s + "."));
            values.add(new PortableKeybindDefinition.Variant(variantName, steps));
        }
        return new PortableKeybindDefinition(name, key,
                strictBoolean(p, prefix + "hudMulti", false), active, values);
    }

    private static void writeStep(Properties p, String prefix, KeybindStep step) throws IOException {
        KeybindStepCodec.writeTransfer(p, prefix, step);
    }

    private static KeybindStep readStep(Properties p, String prefix) throws IOException {
        return KeybindStepCodec.readTransfer(p, prefix);
    }

    private static void encodedField(Properties p, String key, String value) throws IOException {
        String encoded = encode(value);
        if (encoded.length() > KeybindLimits.MAX_ENCODED_FIELD_LENGTH)
            throw new IOException("Encoded transfer field is too long");
        p.setProperty(key, encoded);
    }

    private static String decodedField(Properties p, String key) throws IOException {
        String encoded = required(p, key);
        if (encoded.length() > KeybindLimits.MAX_ENCODED_FIELD_LENGTH)
            throw new IOException("Encoded transfer field is too long");
        return decode(encoded);
    }

    private static void writeAtomic(Path file, Properties properties) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        Path temp = file.resolveSibling(file.getFileName() + "." + UUID.randomUUID() + ".tmp");
        try {
            try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.WRITE);
                 OutputStream output = Channels.newOutputStream(channel)) {
                properties.store(output, "Keybinder portable transfer");
                output.flush();
                channel.force(true);
            }
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException unsupported) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally { Files.deleteIfExists(temp); }
    }

    private static int parse(Properties p, String key) throws IOException {
        try { return Integer.parseInt(required(p, key)); }
        catch (NumberFormatException failure) { throw new IOException("Invalid number " + key, failure); }
    }

    private static String required(Properties p, String key) throws IOException {
        String value = p.getProperty(key);
        if (value == null) throw new IOException("Missing transfer field " + key);
        return value;
    }

    private static boolean strictBoolean(Properties p, String key, boolean fallback)
            throws IOException {
        String value = p.getProperty(key);
        if (value == null) return fallback;
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        throw new IOException("Invalid boolean " + key);
    }

    private static String decoded(Properties p, String key) throws IOException {
        return decode(required(p, key));
    }

    private static String encode(String value) {
        return Base64.getEncoder().encodeToString((value == null ? "" : value)
                .getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String encoded) throws IOException {
        try { return new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8); }
        catch (IllegalArgumentException failure) { throw new IOException("Malformed Base64", failure); }
    }

    private static void requireLength(String value, int maximum, String field) throws IOException {
        if (value != null && value.length() > maximum)
            throw new IOException(field + " exceeds " + maximum + " characters");
    }
}
