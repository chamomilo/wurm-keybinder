package org.keybinder.wurm.i18n;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/** Retains the message source of UI captions without interpreting user-entered text. */
public final class LocalizedText {
    private static final ReferenceQueue<String> QUEUE = new ReferenceQueue<>();
    private static final Map<IdentityReference, LocalizedText> SOURCES = new HashMap<>();
    private final String key;
    private final Object[] arguments;

    private LocalizedText(String key, Object[] arguments) {
        this.key = key;
        this.arguments = arguments == null ? new Object[0] : arguments.clone();
        for (int i = 0; i < this.arguments.length; i++) {
            if (this.arguments[i] instanceof String) {
                LocalizedText nested = capture((String) this.arguments[i]);
                if (nested != null) this.arguments[i] = nested;
            }
        }
    }

    static synchronized String remember(String value, String key, Object[] arguments) {
        clean();
        // Identity distinguishes an actual UI message from identical user data.
        String caption = new String(value);
        SOURCES.put(new IdentityReference(caption, QUEUE), new LocalizedText(key, arguments));
        return caption;
    }

    public static synchronized LocalizedText capture(String caption) {
        clean();
        return caption == null ? null : SOURCES.get(new IdentityReference(caption, null));
    }

    public String resolve() {
        Object[] resolved = arguments.clone();
        for (int i = 0; i < resolved.length; i++)
            if (resolved[i] instanceof LocalizedText) resolved[i] = ((LocalizedText) resolved[i]).resolve();
        return Messages.text(key, resolved);
    }

    private static void clean() {
        IdentityReference reference;
        while ((reference = (IdentityReference) QUEUE.poll()) != null) SOURCES.remove(reference);
    }

    private static final class IdentityReference extends WeakReference<String> {
        private final int hash;
        IdentityReference(String value, ReferenceQueue<String> queue) {
            super(value, queue); hash = System.identityHashCode(value);
        }
        @Override public int hashCode() { return hash; }
        @Override public boolean equals(Object other) {
            if (this == other) return true;
            return other instanceof IdentityReference && get() != null
                    && get() == ((IdentityReference) other).get();
        }
    }
}
