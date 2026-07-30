import java.io.File;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Development helper that snapshots the standard Wurm server action ranges.
 * It has no compile-time dependency on server.jar and is not packaged with the mod.
 */
public final class ActionRangeDump {
    private ActionRangeDump() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: ActionRangeDump <server.jar> <common.jar>");
        }
        URL[] urls = {
                new File(args[0]).toURI().toURL(),
                new File(args[1]).toURI().toURL()
        };
        try (URLClassLoader loader = new URLClassLoader(urls, ClassLoader.getSystemClassLoader())) {
            Class<?> actions = Class.forName(
                    "com.wurmonline.server.behaviours.Actions", true, loader);
            Field entriesField = actions.getField("actionEntrys");
            Object entries = entriesField.get(null);
            Method getNumber = null;
            Method getRange = null;
            Method getActionString = null;
            for (int i = 0; i < Array.getLength(entries); i++) {
                Object entry = Array.get(entries, i);
                if (entry == null) continue;
                if (getNumber == null) {
                    getNumber = entry.getClass().getMethod("getNumber");
                    getRange = entry.getClass().getMethod("getRange");
                    getActionString = entry.getClass().getMethod("getActionString");
                }
                int id = ((Number) getNumber.invoke(entry)).intValue();
                int range = ((Number) getRange.invoke(entry)).intValue();
                String name = String.valueOf(getActionString.invoke(entry));
                System.out.println(id + "\t" + range + "\t" + name);
            }
        }
    }
}
