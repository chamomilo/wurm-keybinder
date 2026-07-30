package org.keybinder.wurm.i18n;

import org.junit.After;
import org.junit.Test;

import java.util.Map;
import java.util.HashMap;
import java.util.Properties;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class MessagesTest {
    @After public void reset() { Messages.select("en"); }

    @Test public void englishIsDefault() {
        assertEquals("en", Messages.languageCode());
        assertEquals("Language", Messages.text("language.label"));
    }

    @Test public void portugueseLoadsAsUtf8() {
        Messages.select("pt-BR");
        assertEquals("pt-BR", Messages.languageCode());
        assertEquals("Não foi possível criar o atalho", Messages.text("error.create_keybind"));
    }

    @Test public void unknownLanguageFallsBackToEnglish() {
        Messages.select("unknown");
        assertEquals("en", Messages.languageCode());
        assertEquals("Language", Messages.text("language.label"));
        Properties properties = new Properties();
        properties.setProperty(LocalizationSettings.LANGUAGE_KEY, "unknown");
        assertEquals("en", LocalizationSettings.load(properties));
    }

    @Test public void dictionariesHaveIdenticalKeys() {
        Map<String, String> english = Messages.loadDictionary("en");
        Map<String, String> portuguese = Messages.loadDictionary("pt-BR");
        assertEquals(english.keySet(), portuguese.keySet());
    }

    @Test public void parametersAreFormatted() {
        Messages.select("pt-BR");
        assertEquals("Registro não encontrado: abc",
                Messages.text("event.record_missing", "abc"));
    }

    @Test public void oldSettingsDefaultToEnglishAndPortugueseRoundTrips() {
        Properties properties = new Properties();
        properties.setProperty("skipIntroPage", "true");
        assertEquals("en", LocalizationSettings.load(properties));
        LocalizationSettings.save(properties, "pt-BR");
        assertEquals("pt-BR", LocalizationSettings.load(properties));
        assertEquals("true", properties.getProperty("skipIntroPage"));
    }

    @Test public void missingPortugueseEntryFallsBackToEnglishAndWarns() {
        Map<String, String> english = new HashMap<String, String>();
        english.put("test.key", "English fallback");
        final StringBuilder warning = new StringBuilder();
        Logger logger = Logger.getLogger("Chamomilo.Keybinder");
        Handler handler = new Handler() {
            @Override public void publish(LogRecord record) {
                warning.append(record.getMessage());
            }
            @Override public void flush() {}
            @Override public void close() {}
        };
        logger.addHandler(handler);
        try {
            assertEquals("English fallback", Messages.resolvePattern(
                    new HashMap<String, String>(), english, "pt-BR", "test.key"));
        } finally {
            logger.removeHandler(handler);
        }
        assertTrue(warning.toString().contains("using English"));
        assertTrue(warning.toString().contains("test.key"));
    }
}
