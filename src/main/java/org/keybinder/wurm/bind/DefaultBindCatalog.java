package org.keybinder.wurm.bind;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class DefaultBindCatalog {
    public Map<String, String> load() throws IOException {
        InputStream in = DefaultBindCatalog.class.getResourceAsStream(
                "/com/wurmonline/client/defaults/keybindings.txt");
        Map<String, String> result = new HashMap<>();
        if (in == null) return result;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("//") || !line.toLowerCase(Locale.ENGLISH).startsWith("bind "))
                    continue;
                String rest = line.substring(5).trim();
                int separator = rest.indexOf(' ');
                if (separator <= 0) continue;
                String key = normalize(rest.substring(0, separator));
                String command = rest.substring(separator + 1).trim();
                if (command.startsWith("\"") && command.endsWith("\"") && command.length() >= 2)
                    command = command.substring(1, command.length() - 1);
                result.put(key, command);
            }
        }
        return result;
    }

    public static String normalize(String key) {
        return key == null ? "" : key.trim().replace('-', '+').toUpperCase(Locale.ENGLISH);
    }
}
