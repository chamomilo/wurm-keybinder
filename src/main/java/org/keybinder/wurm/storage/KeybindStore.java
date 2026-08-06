package org.keybinder.wurm.storage;

import org.keybinder.wurm.model.KeybindRecord;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.ByteBuffer;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public final class KeybindStore {
    public static final int SCHEMA_VERSION = KeybindPropertiesCodec.SCHEMA_VERSION;
    private final Path file;
    private final KeybindPropertiesCodec codec = new KeybindPropertiesCodec();
    private volatile boolean recoveredFromBackup;
    private volatile int loadedSchema = SCHEMA_VERSION;
    private volatile Path loadedSource;
    private volatile boolean valuePackProvided;

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
        valuePackProvided = false;
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
        KeybindPropertiesCodec.Decoded decoded = codec.decode(props);
        if (rememberSource) {
            loadedSchema = decoded.getSchema();
            loadedSource = source;
            valuePackProvided = decoded.wasValuePackProvided();
        }
        return decoded.getRecords();
    }

    public boolean wasValuePackProvided() {
        return valuePackProvided;
    }

    public void setValuePackProvided(boolean value) {
        valuePackProvided = value;
    }

    public void save(List<KeybindRecord> records) throws IOException {
        Properties props = codec.encode(records, valuePackProvided);
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        createPreV8BackupIfNeeded();
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
            if (!KeybindPropertiesCodec.sameRecords(records, verified))
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
        if (loadedSchema >= 8 || source == null || !Files.isRegularFile(source)) return;
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

    private static IOException asIo(String message, Throwable cause) {
        return cause instanceof IOException
                ? (IOException) cause : new IOException(message, cause);
    }
}
