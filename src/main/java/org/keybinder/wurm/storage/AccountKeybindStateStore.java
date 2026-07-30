package org.keybinder.wurm.storage;

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
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Stores the desired enabled managed-keybind IDs separately for each Wurm
 * player profile. Definitions remain shared; activation is account-local.
 */
public final class AccountKeybindStateStore {
    public static final int SCHEMA_VERSION = 1;

    public static final class State {
        private final boolean present;
        private final Set<String> enabledIds;

        private State(boolean present, Set<String> enabledIds) {
            this.present = present;
            this.enabledIds = Collections.unmodifiableSet(
                    new HashSet<String>(enabledIds));
        }

        public boolean isPresent() {
            return present;
        }

        public Set<String> getEnabledIds() {
            return enabledIds;
        }
    }

    private final Path file;

    public AccountKeybindStateStore(Path file) {
        this.file = file;
    }

    public synchronized State load(String account) throws IOException {
        Map<String, Set<String>> all = readRecovering();
        String key = normalizeAccount(account);
        Set<String> ids = all.get(key);
        return new State(ids != null, ids == null
                ? Collections.<String>emptySet() : ids);
    }

    public synchronized void save(String account, Set<String> enabledIds)
            throws IOException {
        String key = normalizeAccount(account);
        Map<String, Set<String>> all = readRecovering();
        all.put(key, new HashSet<String>(enabledIds));
        write(all);
    }

    private Map<String, Set<String>> readRecovering() throws IOException {
        if (!Files.isRegularFile(file)) return new HashMap<String, Set<String>>();
        try {
            return read(file);
        } catch (IOException | RuntimeException primary) {
            Path backup = backupFile();
            if (!Files.isRegularFile(backup)) throw asIo(primary);
            try {
                return read(backup);
            } catch (IOException | RuntimeException backupFailure) {
                IOException failure = asIo(primary);
                failure.addSuppressed(backupFailure);
                throw failure;
            }
        }
    }

    private static Map<String, Set<String>> read(Path source) throws IOException {
        Properties properties = new Properties();
        try (InputStream in = Files.newInputStream(source)) {
            properties.load(in);
        }
        int schema = Integer.parseInt(properties.getProperty("schema", "1"));
        if (schema > SCHEMA_VERSION)
            throw new IOException("Unsupported account keybind state schema " + schema);
        Map<String, Set<String>> result = new HashMap<String, Set<String>>();
        for (String property : properties.stringPropertyNames()) {
            if (!property.startsWith("account.")) continue;
            String account = decode(property.substring("account.".length()));
            Set<String> ids = new HashSet<String>();
            String value = properties.getProperty(property, "");
            if (!value.trim().isEmpty())
                for (String encoded : value.split(","))
                    if (!encoded.trim().isEmpty()) ids.add(decode(encoded.trim()));
            result.put(account, ids);
        }
        return result;
    }

    private void write(Map<String, Set<String>> all) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        Properties properties = new Properties();
        properties.setProperty("schema", String.valueOf(SCHEMA_VERSION));
        for (Map.Entry<String, Set<String>> entry : all.entrySet()) {
            java.util.List<String> ids = new java.util.ArrayList<String>(entry.getValue());
            Collections.sort(ids);
            StringBuilder encodedIds = new StringBuilder();
            for (String id : ids) {
                if (encodedIds.length() > 0) encodedIds.append(',');
                encodedIds.append(encode(id));
            }
            properties.setProperty("account." + encode(entry.getKey()),
                    encodedIds.toString());
        }

        Path temp = file.resolveSibling(file.getFileName() + "."
                + java.util.UUID.randomUUID().toString() + ".tmp");
        try (FileChannel channel = FileChannel.open(temp,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
             OutputStream out = Channels.newOutputStream(channel)) {
            properties.store(out, "Keybinder account activation state");
            out.flush();
            channel.force(true);
        }
        try {
            if (Files.exists(file))
                Files.copy(file, backupFile(), StandardCopyOption.REPLACE_EXISTING);
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

    private Path backupFile() {
        return file.resolveSibling(file.getFileName() + ".bak");
    }

    private static String normalizeAccount(String account) {
        if (account == null || account.trim().isEmpty())
            throw new IllegalArgumentException("Account name is missing");
        return account.trim().toLowerCase(java.util.Locale.ENGLISH);
    }

    private static String encode(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(
                value.getBytes(StandardCharsets.UTF_8));
    }

    private static String decode(String value) {
        return new String(Base64.getUrlDecoder().decode(value),
                StandardCharsets.UTF_8);
    }

    private static IOException asIo(Throwable cause) {
        return cause instanceof IOException
                ? (IOException) cause
                : new IOException("Unable to read account keybind state", cause);
    }
}
