package org.keybinder.wurm.command;

/** Rarity estimate conditional on an active window and successful Improve. */
final class ImproveRarityChanceEstimator {
    Range estimate(byte targetRarity,
                   double minimumRuneModifier, double maximumRuneModifier,
                   byte sourceRarity, boolean sourceMayBeConsumed) {
        double lowAction = higherRarityProbability(targetRarity, false)
                * transferChance(minimumRuneModifier);
        double highAction = higherRarityProbability(targetRarity, true)
                * transferChance(maximumRuneModifier);

        double low = lowAction;
        double high = highAction;
        if (sourceMayBeConsumed && sourceRarity > targetRarity) {
            // Whether the last grams are deleted is not known before send. If
            // they are, the rarer source gets a separate 1% transfer roll.
            high = highAction + (1.0 - highAction) * 0.01;
        }
        return new Range(low * 100.0, high * 100.0);
    }

    private static double higherRarityProbability(byte targetRarity,
                                                   boolean paying) {
        double fantastic = paying ? 0.000103 : 0.000100;
        double supreme = paying ? 0.04 : 0.01;
        if (targetRarity <= 0)
            return fantastic + (1.0 - fantastic)
                    * (supreme + (1.0 - supreme) * 0.5);
        if (targetRarity == 1)
            return fantastic + (1.0 - fantastic) * supreme;
        if (targetRarity == 2) return fantastic;
        return 0.0;
    }

    private static double transferChance(double runeModifier) {
        return clamp(0.2 * (1.0 + Math.max(0.0, runeModifier)), 0.0, 1.0);
    }

    private static double clamp(double value, double minimum, double maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    static final class Range {
        private final double minimum;
        private final double maximum;

        Range(double minimum, double maximum) {
            this.minimum = minimum;
            this.maximum = Math.max(minimum, maximum);
        }

        double getMinimum() { return minimum; }
        double getMaximum() { return maximum; }
    }
}
