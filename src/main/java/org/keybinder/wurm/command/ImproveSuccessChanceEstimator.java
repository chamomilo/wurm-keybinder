package org.keybinder.wurm.command;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Client-side approximation of the server's Improve skill check. The server's
 * integer Gaussian chance table is private server state, so missing cells are
 * reproduced deterministically from the same roll algorithm.
 */
final class ImproveSuccessChanceEstimator {
    private static final int SAMPLES = 30000;
    private final Map<Integer, Integer> chanceCache = new HashMap<Integer, Integer>();

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
        int skillIndex = (int) skill;
        int difficultyIndex = (int) difficulty;
        int key = skillIndex * 101 + difficultyIndex;
        Integer cached = chanceCache.get(key);
        if (cached != null) return cached;

        Random random = new Random(0x4b455942494e444cL ^ key);
        int successes = 0;
        for (int i = 0; i < SAMPLES; i++)
            if (rollGaussian(skillIndex, difficultyIndex, random) > 0.0f)
                successes++;
        int chance = successes / 300;
        chanceCache.put(key, chance);
        return chance;
    }

    private static float rollGaussian(float skill, float difficulty, Random random) {
        float slide = (skill * skill * skill
                - difficulty * difficulty * difficulty) / 50000.0f
                + skill - difficulty;
        float width = 30.0f - Math.abs(skill - difficulty) / 4.0f;
        int attempts = 0;
        while (true) {
            float result = (float) random.nextGaussian()
                    * (width + Math.abs(slide) / 6.0f) + slide;
            float reject = (float) random.nextGaussian()
                    * (width - Math.abs(slide) / 6.0f) + slide;
            if (slide > 0.0f
                    && result > reject + Math.max(100.0f - slide, 0.0f))
                result = -1000.0f;
            else if (slide <= 0.0f
                    && result < reject - Math.max(100.0f + slide, 0.0f))
                result = -1000.0f;
            attempts++;
            if (attempts == 100) {
                if (result > 100.0f) return 90.0f;
                if (result < -100.0f) return -90.0f;
            }
            if (result >= -100.0f && result <= 100.0f) return result;
        }
    }

    private static double epicValue(double value) {
        if (value < 100.0) return (10000.0 - square(100.0 - value)) / 100.0;
        if (value <= 7000.0) return 1.3571428060531616 * value;
        return 95.0 + (value - 7000.0) * 0.1666666716337204;
    }

    private static double square(double value) { return value * value; }
    private static double cube(double value) { return value * value * value; }
}
