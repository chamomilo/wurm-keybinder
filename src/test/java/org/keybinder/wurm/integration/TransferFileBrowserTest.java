package org.keybinder.wurm.integration;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import java.nio.file.*;
import java.io.IOException;
import java.util.*;
import static org.junit.Assert.*;

public class TransferFileBrowserTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();
    @Test public void directoriesPrecedeOnlyTransferFilesInStableOrder() throws Exception {
        Path directory = temp.getRoot().toPath();
        Files.createDirectory(directory.resolve("z-folder")); Files.createDirectory(directory.resolve("a-folder"));
        Files.write(directory.resolve("B.KEYBINDER"),new byte[0]); Files.write(directory.resolve("a.keybinder"),new byte[0]);
        Files.write(directory.resolve("hidden.txt"),new byte[0]);
        List<Path> entries = new TransferFileBrowser(directory).entries();
        assertEquals(Arrays.asList(directory.resolve("a-folder"),directory.resolve("z-folder"),
                directory.resolve("a.keybinder"),directory.resolve("B.KEYBINDER")),entries);
    }
    @Test public void exportAddsExtensionAndAcceptsAbsolutePathsWithoutWriting() throws Exception {
        Path directory = temp.getRoot().toPath(); TransferFileBrowser browser = new TransferFileBrowser(directory);
        assertEquals(directory.resolve("export.keybinder"),browser.resolve("export",true));
        assertEquals(directory.resolve("export.KEYBINDER"),browser.resolve(directory.resolve("export.KEYBINDER").toString(),true));
        assertFalse(Files.exists(directory.resolve("export.keybinder")));
    }
    @Test public void enteringDirectoriesNormalizesTheNextRelativeSelection() throws Exception {
        Path directory = temp.getRoot().toPath(); Path folder = Files.createDirectory(directory.resolve("Archive"));
        Path file = Files.write(folder.resolve("binding.keybinder"),new byte[0]);
        TransferFileBrowser browser = new TransferFileBrowser(directory);browser.enter(folder);
        assertEquals(file,browser.resolve("binding.keybinder",false));
        assertEquals(directory,browser.resolve("..",false));
    }
    @Test public void missingImportAndMissingExportParentAreRejected() throws Exception {
        TransferFileBrowser browser = new TransferFileBrowser(temp.getRoot().toPath());
        for (String invalid : new String[]{"", "missing.keybinder"}) try {
            browser.resolve(invalid,false);fail("Missing import must be rejected");
        } catch (IOException expected) { assertFalse(expected.getMessage().isEmpty()); }
        try { browser.resolve("missing/export",true);fail("Missing export parent must be rejected"); }
        catch (IOException expected) { assertFalse(expected.getMessage().isEmpty()); }
    }
}
