package com.wurmonline.client.resources;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;

public final class KeybinderFileResourceUrl extends ResourceUrl {
    private final Path file;

    public KeybinderFileResourceUrl(String filePath) {
        this(Paths.get(filePath));
    }

    private KeybinderFileResourceUrl(Path file) {
        super(file.toString());
        this.file = file.toAbsolutePath().normalize();
    }

    @Override
    public ResourceUrl derive(String relative) {
        Path parent = file.getParent();
        return new KeybinderFileResourceUrl(parent == null ? Paths.get(relative) : parent.resolve(relative));
    }

    @Override
    public ResourceUrl changeFilePath(String filePath) {
        return new KeybinderFileResourceUrl(filePath);
    }

    @Override public InputStream openStream() throws IOException { return Files.newInputStream(file); }
    @Override public boolean exists() { return Files.isRegularFile(file); }
    @Override public String getFilePath() { return file.toString(); }
    @Override public Map<String, String> getOverrides() { return Collections.emptyMap(); }

    @Override
    long getSize() {
        try {
            return Files.size(file);
        } catch (IOException ignored) {
            return 0;
        }
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof KeybinderFileResourceUrl
                && file.equals(((KeybinderFileResourceUrl) other).file);
    }

    @Override public String toString() { return "keybinder-file:" + file; }
}
