package org.keybinder.wurm.i18n;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class Messages {
    private static final Logger LOGGER = Logger.getLogger("Chamomilo.Keybinder");
    private static final String ROOT = "/keybinder/i18n/messages_";
    private static final Path INSTALLED_JAR =
            Paths.get("mods", "keybinder", "keybinder.jar");
    private static final Map<String, String> ENGLISH = loadDictionary("en");
    private static volatile Language language = Language.ENGLISH;
    private static volatile Map<String, String> active = ENGLISH;

    private Messages() {}

    public static synchronized void select(String code) {
        Language selected = Language.fromCode(code);
        language = selected;
        active = selected == Language.ENGLISH
                ? ENGLISH : loadDictionary(selected.getCode());
    }

    public static Language language() { return language; }
    public static String languageCode() { return language.getCode(); }

    public static String text(String key, Object... arguments) {
        String pattern = resolvePattern(active, ENGLISH, language.getCode(), key);
        return arguments == null || arguments.length == 0
                ? pattern : new MessageFormat(pattern).format(arguments);
    }

    static String resolvePattern(
            Map<String, String> selected, Map<String, String> english,
            String languageCode, String key) {
        String pattern = selected.get(key);
        if (pattern != null) return pattern;
        pattern = english.get(key);
        if (pattern == null) {
            LOGGER.warning("Missing English Keybinder translation: " + key);
            return key;
        }
        if (selected != english)
            LOGGER.warning("Missing " + languageCode
                    + " Keybinder translation; using English: " + key);
        return pattern;
    }

    public static String englishText(String key, Object... arguments) {
        String pattern = ENGLISH.get(key);
        if (pattern == null) {
            LOGGER.warning("Missing English Keybinder translation: " + key);
            return key;
        }
        return arguments == null || arguments.length == 0
                ? pattern : new MessageFormat(pattern).format(arguments);
    }

    public static Set<String> englishKeys() {
        return Collections.unmodifiableSet(ENGLISH.keySet());
    }

    static Map<String, String> loadDictionary(String code) {
        String resource = ROOT + code + ".properties";
        InputStream stream = Messages.class.getResourceAsStream(resource);
        if (stream == null)
            stream = openArchiveResource(INSTALLED_JAR, resource.substring(1));
        if (stream == null)
            throw new IllegalStateException("Missing localization dictionary " + resource);
        return readDictionary(resource, stream);
    }

    private static Map<String, String> readDictionary(String resource, InputStream stream) {
        Properties properties = new Properties();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"))) {
            properties.load(reader);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load localization dictionary " + resource, e);
        }
        Map<String, String> result = new LinkedHashMap<String, String>();
        for (String key : properties.stringPropertyNames())
            result.put(key, properties.getProperty(key));
        return Collections.unmodifiableMap(result);
    }

    /**
     * Wurm's shared Javassist loader resolves classes through its ClassPool but
     * does not expose non-class resources through Class.getResourceAsStream().
     * Read the same packaged dictionary directly from the installed mod JAR in
     * that runtime. The normal classpath path remains first for tests and IDEs.
     */
    static InputStream openArchiveResource(Path archive, String entryName) {
        Path absolute = archive.toAbsolutePath().normalize();
        if (!Files.isRegularFile(absolute)) return null;
        try (ZipFile zip = new ZipFile(absolute.toFile())) {
            ZipEntry entry = zip.getEntry(entryName);
            if (entry == null || entry.isDirectory()) return null;
            try (InputStream input = zip.getInputStream(entry);
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int read;
                while ((read = input.read(buffer)) >= 0)
                    output.write(buffer, 0, read);
                return new ByteArrayInputStream(output.toByteArray());
            }
        } catch (IOException e) {
            LOGGER.warning("Unable to read Keybinder localization dictionary from "
                    + absolute + ": " + e.getMessage());
            return null;
        }
    }
}
