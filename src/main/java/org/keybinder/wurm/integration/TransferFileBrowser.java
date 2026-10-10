package org.keybinder.wurm.integration;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.keybinder.wurm.transfer.KeybindTransferStore;
import org.keybinder.wurm.i18n.Messages;

/** Filesystem rules shared by the branded import/export picker and its tests. */
public final class TransferFileBrowser {
    private Path directory;
    public TransferFileBrowser(Path initial) throws IOException {
        Files.createDirectories(initial);
        directory = initial.toAbsolutePath().normalize();
    }
    public Path directory() { return directory; }
    public void enter(Path path) throws IOException {
        Path resolved = path.toAbsolutePath().normalize();
        if (!Files.isDirectory(resolved)) throw new IOException(Messages.text("transfer.directory_missing", resolved));
        directory = resolved;
    }
    public List<Path> entries() throws IOException {
        List<Path> result = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path path : stream) if (Files.isDirectory(path) || path.getFileName().toString()
                    .toLowerCase(Locale.ROOT).endsWith(KeybindTransferStore.EXTENSION)) result.add(path);
        }
        result.sort(Comparator.comparing((Path path) -> !Files.isDirectory(path))
                .thenComparing(path -> path.getFileName().toString().toLowerCase(Locale.ROOT))
                .thenComparing(Path::toString));
        return result;
    }
    public Path resolve(String input, boolean export) throws IOException {
        if (input == null || input.trim().isEmpty()) throw new IOException(Messages.text("transfer.file_required"));
        Path path = directory.resolve(input.trim()).toAbsolutePath().normalize();
        if (Files.isDirectory(path)) return path;
        if (export && !path.getFileName().toString().toLowerCase(Locale.ROOT)
                .endsWith(KeybindTransferStore.EXTENSION))
            path = path.resolveSibling(path.getFileName() + KeybindTransferStore.EXTENSION);
        if (!export && !Files.isRegularFile(path)) throw new IOException(Messages.text("transfer.file_missing", path));
        if (export && !Files.isDirectory(path.getParent())) throw new IOException(Messages.text("transfer.directory_missing", path.getParent()));
        return path;
    }
}
