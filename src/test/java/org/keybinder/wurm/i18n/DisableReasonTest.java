package org.keybinder.wurm.i18n;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.*;

public class DisableReasonTest {
    @After public void reset() { Messages.select("en"); }

    @Test public void stableReasonLocalizesWithoutChangingStoredValue() {
        String stored = DisableReason.value("replaced_by", "Lenhador");
        assertTrue(stored.startsWith("keybinder.reason:replaced_by|"));
        assertEquals("replaced by Lenhador", DisableReason.display(stored));
        Messages.select("pt-BR");
        assertEquals("substituído por Lenhador", DisableReason.display(stored));
    }

    @Test public void legacyEnglishReasonStillLoads() {
        Messages.select("pt-BR");
        assertEquals("desativado pelo usuário",
                DisableReason.display("disabled by user"));
        assertEquals("substituído por Lenhador",
                DisableReason.display("replaced by Lenhador"));
        assertEquals("o custo de execução 12 excede o limite atual de 4",
                DisableReason.display("execution cost 12 exceeds current limit 4"));
    }

    @Test public void userDisabledStateDoesNotBlockReEnable() {
        assertFalse(DisableReason.blocksEnable(
                DisableReason.value("disabled_by_user")));
        assertFalse(DisableReason.blocksEnable("disabled by user"));
        assertFalse(DisableReason.blocksEnable(""));
        assertTrue(DisableReason.blocksEnable(
                DisableReason.value("queue_exceeded", 12, 4)));
        assertTrue(DisableReason.blocksEnable(
                DisableReason.value("key_in_use")));
    }

    @Test public void identifiesOnlyKeyConflictReasonsAsReleasable() {
        assertTrue(DisableReason.isKeyConflict(
                DisableReason.value("replaced_by", "Another bind")));
        assertTrue(DisableReason.isKeyConflict(
                DisableReason.value("extracted_review", "F4")));
        assertFalse(DisableReason.isKeyConflict(
                DisableReason.value("duplicate_review")));
        assertFalse(DisableReason.isKeyConflict(
                DisableReason.value("queue_exceeded", 9, 5)));
    }

    @Test public void distinguishesManagedConflictsFromExternalOwners() {
        assertTrue(DisableReason.isManagedKeyConflict(
                DisableReason.value("replaced_by", "Another bind")));
        assertTrue(DisableReason.isManagedKeyConflict(
                DisableReason.value("key_used", "R", "Another bind")));
        assertTrue(DisableReason.isManagedKeyConflict(
                DisableReason.value("extracted_review", "R")));
        assertFalse(DisableReason.isManagedKeyConflict(
                DisableReason.value("key_used_vanilla", "R", "bind R examine")));
        assertFalse(DisableReason.isManagedKeyConflict(
                DisableReason.value("key_used_unknown", "R")));
        assertFalse(DisableReason.isManagedKeyConflict(
                DisableReason.value("key_in_use")));
    }
}
