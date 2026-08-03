package org.keybinder.wurm.bind;

import org.junit.Test;
import org.keybinder.wurm.model.ConsoleCommandStep;
import org.keybinder.wurm.model.KeybindRecord;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class VanillaImportReviewServiceTest {
    private final VanillaImportReviewService service = new VanillaImportReviewService();

    @Test public void selectsKnownConversionsButLeavesUnknownCommandsForReview() {
        List<VanillaImportCandidate> rows = service.review(Arrays.asList(
                bind("R", "act 163 tool"),
                bind("I", "improve"),
                bind("E", "EXAMINE"),
                bind("X", "exec custom.txt")), Collections.<KeybindRecord>emptyList());

        assertEquals(VanillaImportCandidate.Type.ACTION_CHAIN, rows.get(0).getType());
        assertEquals(VanillaImportCandidate.Type.SMART_IMPROVE, rows.get(1).getType());
        assertEquals(VanillaImportCandidate.Type.VANILLA_COMMAND, rows.get(2).getType());
        assertEquals(VanillaImportCandidate.Type.RAW_VANILLA_COMMAND, rows.get(3).getType());
        assertTrue(rows.get(0).isSelectedByDefault());
        assertTrue(rows.get(1).isSelectedByDefault());
        assertTrue(rows.get(2).isSelectedByDefault());
        assertFalse(rows.get(3).isSelectedByDefault());
        assertEquals(VanillaImportCandidate.Status.NEEDS_REVIEW, rows.get(3).getStatus());
    }

    @Test public void conflictAndMalformedActionAreVisibleAndNotImportable() {
        KeybindRecord existing = new KeybindRecord("managed", "Existing", "CTRL+R",
                Collections.singletonList(new ConsoleCommandStep("say managed")));
        List<VanillaImportCandidate> rows = service.review(Arrays.asList(
                bind("ctrl+r", "EXAMINE"), bind("T", "act nope hover")),
                Collections.singletonList(existing));

        assertEquals(VanillaImportCandidate.Status.CONFLICT, rows.get(0).getStatus());
        assertEquals("Existing", rows.get(0).getDetail());
        assertFalse(rows.get(0).isImportable());
        assertEquals(VanillaImportCandidate.Status.INVALID, rows.get(1).getStatus());
        assertFalse(rows.get(1).isImportable());
    }

    private static BindSnapshot bind(String key, String command) {
        return new BindSnapshot(0, key, command);
    }
}
