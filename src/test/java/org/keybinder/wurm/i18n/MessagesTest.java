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

    @Test public void germanLoadsAsUtf8() {
        Messages.select("de");
        assertEquals("de", Messages.languageCode());
        assertEquals("Tastenbelegung konnte nicht erstellt werden",
                Messages.text("error.create_keybind"));
        assertArrayEquals(
                new String[] {"Englisch", "Portugiesisch (Brasilien)", "Deutsch"},
                Language.displayNames());
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
        for (Language language : Language.values())
            assertEquals(language.getCode(), english.keySet(),
                    Messages.loadDictionary(language.getCode()).keySet());
    }

    @Test public void dictionariesHaveNonEmptyValuesAndMatchingPlaceholders() {
        Map<String, String> english = Messages.loadDictionary("en");
        java.util.regex.Pattern placeholder = java.util.regex.Pattern.compile("\\{\\d+}");
        for (Language language : Language.values()) {
            Map<String, String> dictionary = Messages.loadDictionary(language.getCode());
            for (String key : english.keySet()) {
                assertFalse(language.getCode() + " " + key,
                        english.get(key).trim().isEmpty());
                assertFalse(language.getCode() + " " + key,
                        dictionary.get(key).trim().isEmpty());
                assertEquals(language.getCode() + " " + key,
                        placeholders(placeholder, english.get(key)),
                        placeholders(placeholder, dictionary.get(key)));
            }
        }
    }

    @Test public void parametersAreFormatted() {
        Messages.select("pt-BR");
        assertEquals("Registro não encontrado: abc",
                Messages.text("event.record_missing", "abc"));
    }

    @Test public void listInstructionsUseTheClientFontSafeMinusCharacter() {
        for (Language language : Language.values()) {
            Messages.select(language.getCode());
            String instructions = Messages.text("list.instructions.merge");
            assertTrue(language.getCode(), instructions.contains("+/-"));
            assertFalse(language.getCode(), instructions.contains("−"));
        }
    }

    @Test public void dragIndicatorsExplainTheirDropOperation() {
        assertEquals("insert here", Messages.text("drag.insert_here"));
        assertEquals("merge with this keybind",
                Messages.text("drag.merge_with_keybind"));
        Messages.select("pt-BR");
        assertEquals("Inserir aqui", Messages.text("drag.insert_here"));
        assertEquals("Mesclar com este atalho",
                Messages.text("drag.merge_with_keybind"));
    }

    @Test public void importAndTransferDialogsHaveUiTextInEveryLanguage() {
        for (Language language : Language.values()) {
            Messages.select(language.getCode());
            assertFalse(Messages.text("list.import.question").trim().isEmpty());
            assertFalse(Messages.text("list.import.confirm").trim().isEmpty());
            assertFalse(Messages.text("transfer.import.title").trim().isEmpty());
            assertFalse(Messages.text("transfer.export.title").trim().isEmpty());
            assertFalse(Messages.text("error.transfer_chooser").trim().isEmpty());
            assertFalse(Messages.text("event.import_no_changes")
                    .contains("keybinder_import_confirm"));
        }
    }

    @Test public void oldSettingsDefaultToEnglishAndPortugueseRoundTrips() {
        Properties properties = new Properties();
        properties.setProperty("skipIntroPage", "true");
        assertEquals("en", LocalizationSettings.load(properties));
        LocalizationSettings.save(properties, "pt-BR");
        assertEquals("pt-BR", LocalizationSettings.load(properties));
        assertEquals("true", properties.getProperty("skipIntroPage"));
    }

    @Test public void germanSettingRoundTrips() {
        Properties properties = new Properties();
        LocalizationSettings.save(properties, "de");
        assertEquals("de", LocalizationSettings.load(properties));
    }

    private static java.util.Set<String> placeholders(java.util.regex.Pattern pattern,
                                                       String value) {
        java.util.Set<String> result = new java.util.TreeSet<String>();
        java.util.regex.Matcher matcher = pattern.matcher(value);
        while (matcher.find()) result.add(matcher.group());
        return result;
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
