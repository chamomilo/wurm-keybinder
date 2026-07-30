package org.keybinder.wurm.migration;

/** Detects predecessor improve mods only to offer native-step migration. */
public final class ImprovedImproveMigrationService {
    public boolean isInstalled() {
        return classExists("improvemod.ImproveMod")
                || classExists("net.inniria.wurm.i2improve.I2Improve");
    }

    private boolean classExists(String name) {
        try {
            Class.forName(name, false, getClass().getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        } catch (LinkageError e) {
            return true;
        }
    }
}
