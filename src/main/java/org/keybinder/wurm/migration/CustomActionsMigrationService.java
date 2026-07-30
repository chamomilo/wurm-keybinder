package org.keybinder.wurm.migration;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/** Detects and disables Custom Actions only as part of the migration workflow. */
public final class CustomActionsMigrationService {
    private static final Path PROPERTIES = Paths.get("mods", "action.properties");

    public boolean isInstalled() {
        if (Files.isRegularFile(PROPERTIES)) return true;
        try {
            Class.forName("net.bdew.wurm.action.ActionMod", false, getClass().getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    public Path disableForNextLaunch() throws IOException {
        if (!Files.isRegularFile(PROPERTIES))
            throw new IOException("mods/action.properties was not found; disable the old action mod manually");
        Path disabled = PROPERTIES.resolveSibling("action.properties.disabled");
        int suffix = 1;
        while (Files.exists(disabled))
            disabled = PROPERTIES.resolveSibling("action.properties.disabled." + suffix++);
        try {
            return Files.move(PROPERTIES, disabled, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            return Files.move(PROPERTIES, disabled);
        }
    }
}
