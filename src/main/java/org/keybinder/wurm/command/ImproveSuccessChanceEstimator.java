package org.keybinder.wurm.command;

/**
 * Client-side approximation of the server's Improve skill check. The server's
 * integer Gaussian chance table is private server state. The probability is
 * evaluated directly from the roll's normal distribution so execution never
 * performs Monte Carlo sampling on the game thread.
 */
final class ImproveSuccessChanceEstimator {
    int estimate(double skill, double parentSkill, double targetQuality,
                 double sourceQuality, double sourceDamage, boolean epic) {
        return estimate(skill, parentSkill, targetQuality, sourceQuality,
                sourceDamage, epic, 0.0, true);
    }

    int estimate(double skill, double parentSkill, double targetQuality,
                 double sourceQuality, double sourceDamage, boolean epic,
                 double actionBonus) {
        return estimate(skill, parentSkill, targetQuality, sourceQuality,
                sourceDamage, epic, actionBonus, true);
    }

    int estimateWithoutToolQuality(double skill, double parentSkill,
                                   double targetQuality, boolean epic,
                                   double actionBonus) {
        return estimate(skill, parentSkill, targetQuality, 0.0, 0.0,
                epic, actionBonus, false);
    }

    private int estimate(double skill, double parentSkill, double targetQuality,
                         double sourceQuality, double sourceDamage,
                         boolean epic, double actionBonus,
                         boolean useToolQuality) {
        double toolQuality = sourceQuality
                * Math.max(1.0, 100.0 - sourceDamage) / 100.0;
        double effective = skill;
        if (useToolQuality) {
            if (toolQuality <= skill) effective = (skill + toolQuality) / 2.0;
            else effective = skill + skill * (toolQuality - skill) / 100.0;
        }

        double totalBonus = actionBonus
                + (epic ? epicValue(parentSkill) : parentSkill);
        totalBonus = Math.min(70.0, totalBonus);
        if (totalBonus != 0.0) {
            double linearMaximum = (100.0 + effective) / 2.0;
            double change = Math.min(linearMaximum - effective, effective);
            effective += change * totalBonus / 100.0;
        }
        if (epic) effective = epicValue(effective);
        return gaussianChance(Math.max(1.0, effective),
                Math.max(1.0, targetQuality));
    }

    private int gaussianChance(double skill, double difficulty) {
        if (skill > 99.0 || difficulty > 99.0) {
            double chance = 50.0 + skill - difficulty
                    + (cube(skill) - cube(difficulty)) / 100000.0;
            return (int) Math.max(0.0, Math.min(100.0, chance));
        }
        double skillIndex = (int) skill;
        double difficultyIndex = (int) difficulty;
        double slide = (cube(skillIndex) - cube(difficultyIndex)) / 50000.0
                + skillIndex - difficultyIndex;
        double width = 30.0 - Math.abs(skillIndex - difficultyIndex) / 4.0;
        double deviation = width + Math.abs(slide) / 6.0;
        int chance = (int) (100.0 * normalCdf(slide / deviation));
        return Math.max(0, Math.min(100, chance));
    }

    /** Abramowitz-Stegun normal-CDF approximation; maximum error is < 8e-8. */
    private static double normalCdf(double value) {
        double x = Math.abs(value);
        double t = 1.0 / (1.0 + 0.2316419 * x);
        double density = 0.3989422804014327 * Math.exp(-x * x / 2.0);
        double tail = density * t * (0.319381530 + t * (-0.356563782
                + t * (1.781477937 + t * (-1.821255978
                + t * 1.330274429))));
        return value >= 0.0 ? 1.0 - tail : tail;
    }

    private static double epicValue(double value) {
        if (value < 100.0) return (10000.0 - square(100.0 - value)) / 100.0;
        if (value <= 7000.0) return 1.3571428060531616 * value;
        return 95.0 + (value - 7000.0) * 0.1666666716337204;
    }

    private static double square(double value) { return value * value; }
    private static double cube(double value) { return value * value * value; }
}
