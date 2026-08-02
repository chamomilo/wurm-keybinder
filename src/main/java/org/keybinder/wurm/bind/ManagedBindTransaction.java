package org.keybinder.wurm.bind;

import com.wurmonline.client.console.WurmConsole;
import org.keybinder.wurm.catalog.InputKeyCatalog;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Captures live binds before mutation and conditionally restores only values
 * still owned by this transaction. Foreign changes are never overwritten.
 */
public final class ManagedBindTransaction {
    private final ManagedBindAccess binds;
    private final WurmConsole console;
    private final Map<String, State> states = new LinkedHashMap<String, State>();

    public ManagedBindTransaction(ManagedBindAccess binds, WurmConsole console) {
        this.binds = binds;
        this.console = console;
    }

    public BindSnapshot capture(String key) throws ReflectiveOperationException {
        if (InputKeyCatalog.isVirtual(key)) return null;
        State state = state(key);
        return state.before;
    }

    public boolean removeOwned(String key, String expectedCommand)
            throws ReflectiveOperationException {
        if (InputKeyCatalog.isVirtual(key)) return true;
        State state = state(key);
        state.touched = true;
        state.expectedAfter = null;
        boolean removed = binds.removeIfOwned(console, key, expectedCommand);
        return removed;
    }

    public void install(String key, String command) throws ReflectiveOperationException {
        if (InputKeyCatalog.isVirtual(key)) return;
        State state = state(key);
        state.touched = true;
        state.expectedAfter = command;
        binds.install(console, key, command);
    }

    public void rollback(Throwable original) {
        State[] captured = states.values().toArray(new State[states.size()]);
        for (int i = captured.length - 1; i >= 0; i--) {
            State state = captured[i];
            if (!state.touched) continue;
            try {
                BindSnapshot current = binds.findByKey(console, state.key);
                if (same(current, state.before)) continue;
                if (current != null) {
                    if (state.expectedAfter == null
                            || !current.getCommand().equalsIgnoreCase(state.expectedAfter)) {
                        original.addSuppressed(new IllegalStateException(
                                "Live bind changed during rollback: " + state.key));
                        continue;
                    }
                    if (!binds.removeIfOwned(console, state.key, state.expectedAfter)) {
                        original.addSuppressed(new IllegalStateException(
                                "Unable to remove transaction-owned bind: " + state.key));
                        continue;
                    }
                }
                if (state.before != null
                        && binds.findByKey(console, state.key) == null)
                    binds.install(console, state.before.getKey(), state.before.getCommand());
            } catch (Throwable rollbackFailure) {
                original.addSuppressed(rollbackFailure);
            }
        }
    }

    private State state(String key) throws ReflectiveOperationException {
        String normalized = key == null ? "" : key.trim().toUpperCase(Locale.ENGLISH);
        State state = states.get(normalized);
        if (state == null) {
            state = new State(key, binds.findByKey(console, key));
            states.put(normalized, state);
        }
        return state;
    }

    private static boolean same(BindSnapshot left, BindSnapshot right) {
        if (left == null || right == null) return left == right;
        return left.getKey().equalsIgnoreCase(right.getKey())
                && left.getCommand().equalsIgnoreCase(right.getCommand());
    }

    private static final class State {
        private final String key;
        private final BindSnapshot before;
        private boolean touched;
        private String expectedAfter;

        private State(String key, BindSnapshot before) {
            this.key = key;
            this.before = before;
        }
    }
}
