package org.keybinder.wurm.bind;

import com.wurmonline.client.console.WurmConsole;

import java.util.List;

/** Testable boundary around Wurm's live keybinding table. */
public interface ManagedBindAccess {
    List<BindSnapshot> snapshot(WurmConsole console) throws ReflectiveOperationException;
    BindSnapshot findByKey(WurmConsole console, String key) throws ReflectiveOperationException;
    void install(WurmConsole console, String key, String command);
    boolean removeIfOwned(WurmConsole console, String key, String expectedCommand)
            throws ReflectiveOperationException;
}
