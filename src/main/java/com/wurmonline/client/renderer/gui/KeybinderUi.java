package com.wurmonline.client.renderer.gui;

import com.wurmonline.client.renderer.gui.text.ChamomiloUiV1Fonts;
import com.wurmonline.client.renderer.gui.text.TextFont;
import org.chamomilo.wurm.ui.v1.UiDensity;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/** View-only adaptation of the shared SDK to Wurm's package-private layout tree. */
final class KeybinderUi {
    private static final Logger LOG = Logger.getLogger(KeybinderUi.class.getName());
    private static final Map<Class<?>, List<Field>> CHILD_FIELDS = new HashMap<>();
    private static final Map<WurmComponent, String> IDS = new WeakHashMap<>();
    private static boolean reflectionReported;
    private KeybinderUi() {}

    static void identify(WurmComponent component, String id) { IDS.put(component, id); }
    static String id(WurmComponent component) {
        String id = IDS.get(component);
        return id != null ? id : component.name != null ? component.name : component.getClass().getName();
    }

    static TextFont body() { return ChamomiloUiV1Fonts.caption(16, false, UiDensity.LOW); }
    static TextFont strong() { return ChamomiloUiV1Fonts.caption(16, true, UiDensity.LOW); }
    static TextFont heading() { return ChamomiloUiV1Fonts.caption(18, true, UiDensity.HIGH); }

    static String fit(TextFont font, String source, int width) {
        if (source == null || source.isEmpty()) return "";
        if (font.getWidth(source) <= width) return source;
        if (font.getWidth("...") > width) return "";
        int end = source.length();
        while (end > 0 && font.getWidth(source.substring(0, end) + "...") > width)
            end = source.offsetByCodePoints(end, -1);
        return source.substring(0, end) + "...";
    }

    static void fonts(WurmComponent component) {
        component.text = body(); component.textBold = strong();
    }

    /** Includes SDK private header, editing child and popup option rows. No game-wide font changes. */
    static void theme(WurmComponent root) {
        List<WurmComponent> nodes = tree(root);
        Map<String, List<KeybinderUiButton>> groups = new LinkedHashMap<>();
        for (WurmComponent node : nodes) {
            if (!(node instanceof WButton)) fonts(node);
            if (node instanceof WurmInputField) ((WurmInputField) node).setLineHeight(node.text.getHeight());
            if (id(node).endsWith(".header") || node instanceof KeybinderUiWindow
                    || node.getClass().getName().equals(ChamomiloUiV1Window.class.getName() + "$Header")) {
                node.text = heading(); node.textBold = heading();
            }
            if (node.parent != null && id(node.parent).endsWith(".header")) {
                node.text = heading(); node.textBold = heading();
            }
            if (node instanceof KeybinderUiButton && !(node instanceof KeybinderGlyphButton)) {
                KeybinderUiButton button = (KeybinderUiButton) node;
                String parent = node.parent == null ? id(root) : id(node.parent);
                if (parent.startsWith("keybinder.row.")) parent = "keybinder.list.row-actions";
                String id = parent + ".actions." + button.height;
                groups.computeIfAbsent(id, key -> new ArrayList<>()).add(button);
            }
        }
        for (Map.Entry<String, List<KeybinderUiButton>> entry : groups.entrySet())
            KeybinderUiButton.fitGroup(entry.getKey(), entry.getValue());
    }

    static List<WurmComponent> tree(WurmComponent root) {
        List<WurmComponent> result = new ArrayList<>();
        Set<WurmComponent> seen = Collections.newSetFromMap(new IdentityHashMap<WurmComponent, Boolean>());
        visit(root, seen, result);
        return result;
    }

    private static void visit(WurmComponent node, Set<WurmComponent> seen, List<WurmComponent> result) {
        if (node == null || !seen.add(node)) return;
        result.add(node);
        for (Field field : childFields(node.getClass())) try {
            Object value = field.get(node);
            if (value instanceof WurmComponent) visit((WurmComponent) value, seen, result);
            else if (value instanceof Iterable) {
                for (Object child : (Iterable<?>) value)
                    if (child instanceof WurmComponent) visit((WurmComponent) child, seen, result);
            } else if (value instanceof WurmComponent[]) {
                for (WurmComponent child : (WurmComponent[]) value) visit(child, seen, result);
            }
        } catch (ReflectiveOperationException | RuntimeException failure) { report(failure); }
    }

    private static List<Field> childFields(Class<?> type) {
        List<Field> fields = CHILD_FIELDS.get(type);
        if (fields != null) return fields;
        fields = new ArrayList<>();
        for (Class<?> current = type; current != null && current != WurmComponent.class;
             current = current.getSuperclass()) for (Field field : current.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) continue;
            Class<?> value = field.getType();
            if (!WurmComponent.class.isAssignableFrom(value) && !Iterable.class.isAssignableFrom(value)
                    && !WurmComponent[].class.isAssignableFrom(value)) continue;
            try { field.setAccessible(true); fields.add(field); }
            catch (RuntimeException failure) { report(failure); }
        }
        CHILD_FIELDS.put(type, fields);
        return fields;
    }

    static void themePopup(ChamomiloUiV1DropDown owner) {
        try {
            Field field = HeadsUpDisplay.class.getDeclaredField("dropdownPopups");
            field.setAccessible(true);
            for (Object value : new ArrayList<>((List<?>) field.get(WurmComponent.hud))) {
                WurmDropdownPopup popup = (WurmDropdownPopup) value;
                if (popup.dropDown == owner) theme(popup);
            }
        } catch (ReflectiveOperationException | RuntimeException failure) { report(failure); }
    }

    static void confirm(WButton button, String question, String message) {
        ((KeybinderUiButton) button).confirmation(question, message);
    }

    static WButton maximizeControl(WurmComponent window) {
        // The embedded editor is also a window object. A recursive tree search
        // can accidentally return its unused chrome instead of the host header.
        try {
            Field headerField = ChamomiloUiV1Window.class.getDeclaredField("header");
            if (!FlexComponent.class.isAssignableFrom(headerField.getType()))
                throw new NoSuchFieldException("header signature");
            headerField.setAccessible(true);
            Object header = headerField.get(window);
            Field field = header.getClass().getDeclaredField("maximize");
            if (!WButton.class.isAssignableFrom(field.getType())) throw new NoSuchFieldException("maximize signature");
            field.setAccessible(true);
            return (WButton) field.get(header);
        } catch (ReflectiveOperationException | RuntimeException failure) { report(failure); }
        return null;
    }

    private static void report(Throwable failure) {
        if (!reflectionReported) {
            reflectionReported = true;
            LOG.log(Level.WARNING, "Keybinder Chamomilo view adaptation failed; native input remains available", failure);
        }
    }
}
