package org.keybinder.wurm.transfer;

import java.io.IOException;
import java.util.List;

/** Minimal application boundary required by one-time Value Pack provisioning. */
public interface ValuePackTarget {
    boolean isLoadedSuccessfully();
    boolean wasValuePackProvided();
    TransferImportResult importValuePack(List<PortableKeybindDefinition> definitions)
            throws IOException;
}
