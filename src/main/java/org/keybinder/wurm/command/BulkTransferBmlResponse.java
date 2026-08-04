package org.keybinder.wurm.command;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Strict recognizer for the installed server's RemoveItemQuestion BML form. */
public final class BulkTransferBmlResponse {
    private static final Pattern QUESTION_ID = Pattern.compile(
            "passthrough\\s*\\{[^}]*id\\s*=\\s*['\"]id['\"][^}]*"
                    + "text\\s*=\\s*['\"]([0-9]+)['\"]",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern NUMBER_FIELD = Pattern.compile(
            "id\\s*=\\s*['\"]numstext['\"]", Pattern.CASE_INSENSITIVE);
    private static final Pattern ITEMS_GROUP = Pattern.compile(
            "group\\s*=\\s*['\"]items['\"]", Pattern.CASE_INSENSITIVE);

    private final String questionId;

    private BulkTransferBmlResponse(String questionId) {
        this.questionId = questionId;
    }

    public static BulkTransferBmlResponse parse(String title, String bml) {
        if (title == null || !"Removing items".equalsIgnoreCase(title.trim()) || bml == null)
            return null;
        if (!bml.contains("How many items do you wish to remove?")
                || !NUMBER_FIELD.matcher(bml).find()
                || !ITEMS_GROUP.matcher(bml).find()) return null;
        Matcher id = QUESTION_ID.matcher(bml);
        return id.find() ? new BulkTransferBmlResponse(id.group(1)) : null;
    }

    public String getQuestionId() { return questionId; }

    public Map<String, String> fieldsForQuantity(int quantity) {
        if (quantity <= 0)
            throw new IllegalArgumentException("quantity must be positive");
        Map<String, String> result = new LinkedHashMap<String, String>();
        result.put("id", questionId);
        result.put("numstext", Integer.toString(quantity));
        result.put("items", "1");
        return result;
    }

    public Map<String, String> fieldsForOneItem() {
        return fieldsForQuantity(1);
    }
}
