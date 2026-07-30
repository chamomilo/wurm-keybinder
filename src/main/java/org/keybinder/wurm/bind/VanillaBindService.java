package org.keybinder.wurm.bind;

import com.wurmonline.client.console.KeyBinding;
import com.wurmonline.client.console.WurmConsole;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class VanillaBindService {
    private Field keyBindsField;

    @SuppressWarnings("unchecked")
    private Map<Integer, KeyBinding> map(WurmConsole console) throws ReflectiveOperationException {
        if (keyBindsField == null) keyBindsField = WurmConsole.class.getDeclaredField("keyBinds");
        return ReflectionUtil.getPrivateField(console, keyBindsField);
    }

    public List<BindSnapshot> snapshot(WurmConsole console) throws ReflectiveOperationException {
        List<BindSnapshot> result = new ArrayList<>();
        for (Map.Entry<Integer, KeyBinding> entry : new ArrayList<>(map(console).entrySet())) {
            KeyBinding bind = entry.getValue();
            if (bind != null) result.add(new BindSnapshot(entry.getKey(), bind.getStrName(), bind.getStrCommand()));
        }
        result.sort(Comparator.comparing(BindSnapshot::getKey, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    public void install(WurmConsole console, String key, String command) {
        if (key == null || key.trim().isEmpty()) throw new IllegalArgumentException("Key is missing");
        if (command == null || command.trim().isEmpty()) throw new IllegalArgumentException("Command is missing");
        if (command.contains("\"")) throw new IllegalArgumentException("Commands containing quotes are not supported");
        console.handleInput("bind " + key + " \"" + command + "\"", false);
        console.saveKeyBindings();
    }

    public boolean removeIfOwned(WurmConsole console, String key, String expectedCommand)
            throws ReflectiveOperationException {
        BindSnapshot current = findByKey(console, key);
        if (current == null || !current.getCommand().equalsIgnoreCase(expectedCommand)) return false;
        console.handleInput("bind " + key + " \"\"", false);
        boolean removed = findByKey(console, key) == null;
        if (removed) console.saveKeyBindings();
        return removed;
    }

    public BindSnapshot findByKey(WurmConsole console, String key) throws ReflectiveOperationException {
        for (BindSnapshot bind : snapshot(console))
            if (bind.getKey().equalsIgnoreCase(key)) return bind;
        return null;
    }
}
