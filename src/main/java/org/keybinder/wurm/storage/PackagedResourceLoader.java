package org.keybinder.wurm.storage;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Loads mod resources even when Wurm's shared Javassist loader hides them. */
public final class PackagedResourceLoader {
    private PackagedResourceLoader() { }

    public static InputStream open(Class<?> anchor, String resource, Path installedJar)
            throws IOException {
        InputStream stream = anchor.getResourceAsStream(resource);
        if (stream != null) return stream;
        String entryName = resource.startsWith("/") ? resource.substring(1) : resource;
        return openArchive(installedJar, entryName);
    }

    public static InputStream openArchive(Path archive, String entryName) throws IOException {
        Path absolute = archive.toAbsolutePath().normalize();
        if (!Files.isRegularFile(absolute)) return null;
        try (ZipFile zip = new ZipFile(absolute.toFile())) {
            ZipEntry entry = zip.getEntry(entryName);
            if (entry == null || entry.isDirectory()) return null;
            try (InputStream input = zip.getInputStream(entry);
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = input.read(buffer)) >= 0)
                    output.write(buffer, 0, read);
                return new ByteArrayInputStream(output.toByteArray());
            }
        }
    }
}
