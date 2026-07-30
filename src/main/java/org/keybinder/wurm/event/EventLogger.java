package org.keybinder.wurm.event;

import com.wurmonline.client.renderer.gui.HeadsUpDisplay;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.keybinder.wurm.i18n.Messages;

public final class EventLogger {
    private static final int BUFFER_LIMIT = 100;
    /*
     * Wurm's built-in event tabs use colon-prefixed internal names. "Event"
     * is treated as a normal chat tab and appears in the left chat window;
     * ":Event" is routed to the system Event window.
     */
    private static final String SYSTEM_EVENT_TAB = ":Event";
    private final Logger logger;
    private final Deque<String> pending = new ArrayDeque<>();
    private volatile HeadsUpDisplay hud;
    private volatile boolean eventEnabled = true;
    private volatile boolean executionEnabled;
    private volatile boolean debugEnabled;

    public EventLogger(Logger logger) {
        this.logger = logger;
    }

    public synchronized void attach(HeadsUpDisplay hud) {
        this.hud = hud;
        while (!pending.isEmpty()) send(pending.removeFirst(), 0.8f, 0.9f, 1f);
    }

    public synchronized void info(String text) { publish(text, 0.8f, 0.9f, 1f); }
    public synchronized void warning(String text) { publish(text, 1f, 0.75f, 0.2f); }
    public synchronized void error(String text, Throwable error) {
        logger.log(Level.SEVERE, text, error);
        publish(Messages.text("event.prefix.error", text), 1f, 0.35f, 0.35f);
    }
    public synchronized void debug(String text) {
        if (debugEnabled)
            publish(Messages.text("event.prefix.debug", text), 0.65f, 0.65f, 0.65f);
    }
    public synchronized void execution(String text) {
        if (executionEnabled) publish(text, 0.7f, 1f, 0.7f);
    }

    private void publish(String text, float r, float g, float b) {
        String line = "[Keybinder] " + text;
        logger.info(line);
        if (!eventEnabled) return;
        if (hud == null) {
            if (pending.size() == BUFFER_LIMIT) pending.removeFirst();
            pending.addLast(line);
        } else {
            send(line, r, g, b);
        }
    }

    private void send(String line, float r, float g, float b) {
        try {
            hud.textMessage(SYSTEM_EVENT_TAB, r, g, b, line);
        } catch (Throwable e) {
            logger.log(Level.WARNING, "Unable to write to the system Event tab", e);
        }
    }

    public void setEventEnabled(boolean value) { eventEnabled = value; }
    public void setExecutionEnabled(boolean value) { executionEnabled = value; }
    public void setDebugEnabled(boolean value) { debugEnabled = value; }
}
