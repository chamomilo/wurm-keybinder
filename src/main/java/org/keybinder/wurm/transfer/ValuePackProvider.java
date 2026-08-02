package org.keybinder.wurm.transfer;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import org.keybinder.wurm.KeybindRegistry;

/** Installs the bundled examples once, then persists the completion flag. */
public final class ValuePackProvider {
    public static final String PROVIDED_SETTING = "valuePackProvided";
    public static final String RESOURCE = "/keybinder/value-pack.keybinder";

    private final KeybindTransferStore transfer;

    public ValuePackProvider() {
        this(new KeybindTransferStore());
    }

    ValuePackProvider(KeybindTransferStore transfer) {
        this.transfer = transfer;
    }

    public ProvisionResult provideIfNeeded(Properties settings, InputStream bundle,
                                           KeybindRegistry registry,
                                           SettingsSaver settingsSaver) throws IOException {
        if (!registry.isLoadedSuccessfully())
            throw new IOException("Keybinder records were not loaded successfully");
        if (registry.wasValuePackProvided()) {
            if (!Boolean.parseBoolean(settings.getProperty(PROVIDED_SETTING, "false")))
                persistProvidedSetting(settings, settingsSaver);
            return ProvisionResult.alreadyProvided();
        }
        List<PortableKeybindDefinition> definitions = transfer.read(bundle);
        TransferImportResult imported = registry.importValuePack(definitions);
        persistProvidedSetting(settings, settingsSaver);
        return ProvisionResult.provided(imported);
    }

    private static void persistProvidedSetting(Properties settings,
                                               SettingsSaver settingsSaver) throws IOException {
        String previous = settings.getProperty(PROVIDED_SETTING);
        settings.setProperty(PROVIDED_SETTING, "true");
        try {
            settingsSaver.save(settings);
        } catch (IOException | RuntimeException failure) {
            if (previous == null) settings.remove(PROVIDED_SETTING);
            else settings.setProperty(PROVIDED_SETTING, previous);
            throw failure;
        }
    }

    public interface SettingsSaver {
        void save(Properties settings) throws IOException;
    }

    public static final class ProvisionResult {
        private final boolean providedNow;
        private final TransferImportResult importResult;

        private ProvisionResult(boolean providedNow, TransferImportResult importResult) {
            this.providedNow = providedNow;
            this.importResult = importResult;
        }

        private static ProvisionResult alreadyProvided() {
            return new ProvisionResult(false, new TransferImportResult(0, 0, 0));
        }

        private static ProvisionResult provided(TransferImportResult result) {
            return new ProvisionResult(true, result);
        }

        public boolean wasProvidedNow() { return providedNow; }
        public TransferImportResult getImportResult() { return importResult; }
    }
}
