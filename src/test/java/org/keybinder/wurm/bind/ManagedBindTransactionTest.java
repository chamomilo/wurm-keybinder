package org.keybinder.wurm.bind;

import com.wurmonline.client.console.WurmConsole;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ManagedBindTransactionTest {
    @Test
    public void rollbackRestoresForeignBindingReplacedByInstall() throws Exception {
        FakeBinds binds = new FakeBinds();
        binds.put("R", "EXAMINE");
        ManagedBindTransaction transaction = new ManagedBindTransaction(binds, null);

        transaction.install("R", "keybinder_run record");
        RuntimeException failure = new RuntimeException("save failed");
        transaction.rollback(failure);

        assertEquals("EXAMINE", binds.command("R"));
        assertEquals(0, failure.getSuppressed().length);
    }

    @Test
    public void rollbackRestoresOwnedBindingAfterRemoval() throws Exception {
        FakeBinds binds = new FakeBinds();
        binds.put("R", "keybinder_run record");
        ManagedBindTransaction transaction = new ManagedBindTransaction(binds, null);

        assertTrue(transaction.removeOwned("R", "keybinder_run record"));
        assertNull(binds.command("R"));
        transaction.rollback(new RuntimeException("store failed"));

        assertEquals("keybinder_run record", binds.command("R"));
    }

    @Test
    public void rollbackNeverOverwritesAConcurrentForeignChange() throws Exception {
        FakeBinds binds = new FakeBinds();
        binds.put("R", "EXAMINE");
        ManagedBindTransaction transaction = new ManagedBindTransaction(binds, null);
        transaction.install("R", "keybinder_run record");
        binds.put("R", "OPEN");
        RuntimeException failure = new RuntimeException("later failure");

        transaction.rollback(failure);

        assertEquals("OPEN", binds.command("R"));
        assertEquals(1, failure.getSuppressed().length);
    }

    private static final class FakeBinds implements ManagedBindAccess {
        private final Map<String, String> values = new LinkedHashMap<String, String>();

        private void put(String key, String command) {
            values.put(normalize(key), command);
        }

        private String command(String key) {
            return values.get(normalize(key));
        }

        @Override public List<BindSnapshot> snapshot(WurmConsole console) {
            List<BindSnapshot> result = new ArrayList<BindSnapshot>();
            for (Map.Entry<String, String> value : values.entrySet())
                result.add(new BindSnapshot(0, value.getKey(), value.getValue()));
            return result;
        }

        @Override public BindSnapshot findByKey(WurmConsole console, String key) {
            String command = command(key);
            return command == null ? null : new BindSnapshot(0, key, command);
        }

        @Override public void install(WurmConsole console, String key, String command) {
            put(key, command);
        }

        @Override public boolean removeIfOwned(WurmConsole console, String key,
                                               String expectedCommand) {
            String current = command(key);
            if (current == null || !current.equalsIgnoreCase(expectedCommand)) return false;
            values.remove(normalize(key));
            return true;
        }

        private static String normalize(String key) {
            return key.toUpperCase(Locale.ENGLISH);
        }
    }
}
