package org.keybinder.wurm.ui;

import org.keybinder.wurm.catalog.InputKeyCatalog;
import org.keybinder.wurm.i18n.DisableReason;
import org.keybinder.wurm.i18n.Messages;
import org.keybinder.wurm.model.KeybindRecord;
import org.keybinder.wurm.queue.ActionQueueCostCalculator;
import org.keybinder.wurm.queue.QueueCost;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Presentation state for the managed-keybind list, independent of Wurm controls. */
public final class KeybindListViewModel {
    public static final String FILTER_ALL = "keybinder.filter:all";
    public static final String FILTER_UNKNOWN = "keybinder.filter:unknown";

    private static final ActionQueueCostCalculator COSTS =
            new ActionQueueCostCalculator();

    private KeybindListViewModel() {}

    public static Status status(
            KeybindRecord record, List<KeybindRecord> records, int queueLimit) {
        List<String> reasons = new ArrayList<String>();
        if (record.getKey() == null || record.getKey().trim().isEmpty())
            reasons.add(Messages.text("status.no_key"));
        if (record.getKeybindSteps() == null || record.getKeybindSteps().isEmpty())
            reasons.add(Messages.text("status.no_steps"));
        KeybindRecord managedBlocker = KeybindStatusResolver.enabledManagedBlocker(
                record, safeRecords(records));
        if (managedBlocker != null)
            reasons.add(Messages.text("reason.key_used",
                    InputKeyCatalog.displayChord(record.getKey()), managedBlocker.getName()));
        if (!record.isEnabled() && DisableReason.blocksEnable(record.getDisabledReason())
                && !DisableReason.isManagedKeyConflict(record.getDisabledReason()))
            reasons.add(DisableReason.display(record.getDisabledReason()));
        QueueCost cost = COSTS.keybindCost(record);
        boolean queueWarning = cost.getKind() == QueueCost.Kind.FIXED && queueLimit > 0
                && cost.getValue() > queueLimit;
        String warningText = queueWarning ? Messages.text("status.queue_warning",
                cost.getValue(), queueLimit) : "";
        if (reasons.isEmpty())
            return queueWarning ? new Status(false, true, warningText) : Status.OK;
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < reasons.size(); i++) {
            if (i > 0) text.append(i + 1 == reasons.size()
                    ? " " + Messages.text("common.and") + " " : ", ");
            text.append(reasons.get(i));
        }
        String errorText = Messages.text("status.cannot_enable", text.toString());
        if (queueWarning) errorText += " " + warningText;
        return new Status(true, queueWarning, errorText);
    }

    public static List<KeybindRecord> filter(
            List<KeybindRecord> records, String userFilter, String serverFilter) {
        List<KeybindRecord> result = new ArrayList<KeybindRecord>();
        for (KeybindRecord record : safeRecords(records)) {
            if (!matchesOrigin(userFilter, record.getCreatedByUser())) continue;
            if (!matchesOrigin(serverFilter, record.getCreatedOnServer())) continue;
            result.add(record);
        }
        return result;
    }

    public static boolean matchesOrigin(String filter, String value) {
        if (FILTER_ALL.equals(filter)) return true;
        String clean = value == null ? "" : value.trim();
        return FILTER_UNKNOWN.equals(filter)
                ? clean.isEmpty() : filter != null && filter.equalsIgnoreCase(clean);
    }

    public static OriginOptions originOptions(
            List<KeybindRecord> records, boolean user, String all, String unknown) {
        Set<String> values = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        boolean hasUnknown = false;
        for (KeybindRecord record : safeRecords(records)) {
            String value = user ? record.getCreatedByUser() : record.getCreatedOnServer();
            if (value == null || value.trim().isEmpty()) hasUnknown = true;
            else values.add(value.trim());
        }
        List<String> labels = new ArrayList<String>();
        List<String> identities = new ArrayList<String>();
        labels.add(all);
        identities.add(FILTER_ALL);
        if (hasUnknown) {
            labels.add(unknown);
            identities.add(FILTER_UNKNOWN);
        }
        labels.addAll(values);
        identities.addAll(values);
        return new OriginOptions(labels.toArray(new String[labels.size()]),
                identities.toArray(new String[identities.size()]));
    }

    public static String displayOrigin(String value) {
        return value == null || value.trim().isEmpty()
                ? Messages.text("list.unknown") : value.trim();
    }

    private static List<KeybindRecord> safeRecords(List<KeybindRecord> records) {
        return records == null ? Collections.<KeybindRecord>emptyList() : records;
    }

    public static final class Status {
        private static final Status OK = new Status(false, false, "");
        private final boolean error;
        private final boolean warning;
        private final String hoverText;

        private Status(boolean error, boolean warning, String hoverText) {
            this.error = error;
            this.warning = warning;
            this.hoverText = hoverText;
        }

        public boolean isError() { return error; }
        public boolean isWarning() { return warning; }
        public String getHoverText() { return hoverText; }
    }

    public static final class OriginOptions {
        private final String[] labels;
        private final String[] values;

        private OriginOptions(String[] labels, String[] values) {
            this.labels = labels;
            this.values = values;
        }

        public String[] getLabels() { return labels.clone(); }
        public String[] getValues() { return values.clone(); }
    }
}
