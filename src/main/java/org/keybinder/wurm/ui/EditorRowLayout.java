package org.keybinder.wurm.ui;

import org.keybinder.wurm.model.StepKind;

/** Pure width and presentation policy for one editor action row. */
public final class EditorRowLayout {
    public enum Shape { SINGLE_FIELD, THREE_COLUMNS }

    public static final class Plan {
        private final Shape shape;
        private final boolean captureEnabled;
        private final int actionWidth;
        private final int sourceWidth;
        private final int targetWidth;

        private Plan(Shape shape, boolean captureEnabled,
                     int actionWidth, int sourceWidth, int targetWidth) {
            this.shape = shape;
            this.captureEnabled = captureEnabled;
            this.actionWidth = actionWidth;
            this.sourceWidth = sourceWidth;
            this.targetWidth = targetWidth;
        }

        public Shape getShape() { return shape; }
        public boolean isCaptureEnabled() { return captureEnabled; }
        public int getActionWidth() { return actionWidth; }
        public int getSourceWidth() { return sourceWidth; }
        public int getTargetWidth() { return targetWidth; }
    }

    private EditorRowLayout() {}

    public static Plan plan(StepKind kind, boolean vanilla, boolean vanillaUsesTarget,
                            boolean usesActionTarget, int contentWidth, int controlsWidth,
                            int stepTypeWidth, int helpWidth, int captureWidth,
                            int separatorWidth, int columnGap,
                            int actionMinimum, int sourceMinimum) {
        boolean captureEnabled = !vanilla
                && kind != StepKind.CONSOLE_COMMAND
                && kind != StepKind.BULK_TRANSFER;
        boolean threeColumns = kind == StepKind.BULK_TRANSFER
                || (vanilla && vanillaUsesTarget)
                || (!vanilla && kind != StepKind.CONSOLE_COMMAND
                && (kind != StepKind.CUSTOM_ACTION || usesActionTarget));
        if (!threeColumns) {
            int width = Math.max(160,
                    contentWidth - controlsWidth - stepTypeWidth - helpWidth
                            - captureWidth - separatorWidth - columnGap * 5);
            return new Plan(Shape.SINGLE_FIELD, captureEnabled, width, 0, 0);
        }
        int remaining = Math.max(420,
                contentWidth - controlsWidth - stepTypeWidth - helpWidth - captureWidth
                        - separatorWidth * 3 - columnGap * 9);
        int actionWidth = Math.max(actionMinimum, remaining / 3);
        int sourceWidth = Math.max(sourceMinimum, remaining / 3);
        return new Plan(Shape.THREE_COLUMNS, captureEnabled,
                actionWidth, sourceWidth, remaining - actionWidth - sourceWidth);
    }

    public static int[] columns(int contentWidth, int controlsWidth,
                                int stepTypeWidth, int helpWidth, int captureWidth,
                                int separatorWidth, int columnGap,
                                int actionMinimum, int sourceMinimum) {
        Plan plan = plan(StepKind.CUSTOM_ACTION, false, false, true,
                contentWidth, controlsWidth, stepTypeWidth, helpWidth, captureWidth,
                separatorWidth, columnGap, actionMinimum, sourceMinimum);
        return new int[]{plan.getActionWidth(), plan.getSourceWidth(), plan.getTargetWidth()};
    }
}
