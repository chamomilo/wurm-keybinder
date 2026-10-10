package org.keybinder.wurm.transfer;

import java.io.IOException;
import java.util.List;

/** Minimal application boundary required by one-time provisioning of each pack revision. */
public interface ValuePackTarget {
    boolean isLoadedSuccessfully();
    boolean wasValuePackProvided();
    int getValuePackRevision();
    TransferImportResult importValuePack(List<PortableKeybindDefinition> definitions)
            throws IOException;
}
