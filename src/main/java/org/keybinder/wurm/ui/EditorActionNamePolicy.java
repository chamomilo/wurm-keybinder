package org.keybinder.wurm.ui;

/**
 * Resolves presentation metadata for an action row without changing the
 * numeric action ID that identifies the action.
 */
public final class EditorActionNamePolicy {
    private EditorActionNamePolicy() {}

    public static String resolve(short actionId, short rememberedActionId,
                                 String rememberedName,
                                 EditorStepDraft.ActionNameLookup names) {
        String resolved = names == null ? "" : clean(names.find(actionId));
        if (!resolved.isEmpty()) return resolved;
        if (actionId == rememberedActionId) return clean(rememberedName);
        return "";
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
