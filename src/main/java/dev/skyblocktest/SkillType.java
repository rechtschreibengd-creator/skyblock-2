package dev.skyblocktest;

import org.bukkit.Material;

/** Skills + Boni pro Level (an Hypixel angelehnt, in den Tiers unten anpassbar). */
public enum SkillType {
    COMBAT("Combat", Material.STONE_SWORD, Stat.CRIT_CHANCE, new double[][]{{50, 0.5}}),
    MINING("Mining", Material.STONE_PICKAXE, Stat.DEFENSE, new double[][]{{14, 1}, {50, 2}}),
    FARMING("Farming", Material.GOLDEN_HOE, Stat.HEALTH, new double[][]{{14, 2}, {19, 3}, {25, 4}, {50, 5}}),
    FORAGING("Foraging", Material.JUNGLE_SAPLING, Stat.STRENGTH, new double[][]{{14, 1}, {50, 2}}),
    FISHING("Fishing", Material.FISHING_ROD, Stat.HEALTH, new double[][]{{14, 2}, {19, 3}, {25, 4}, {50, 5}}),
    ENCHANTING("Enchanting", Material.ENCHANTING_TABLE, Stat.INTELLIGENCE, new double[][]{{14, 1}, {50, 2}}),
    ALCHEMY("Alchemy", Material.BREWING_STAND, Stat.INTELLIGENCE, new double[][]{{14, 1}, {50, 2}}),
    CARPENTRY("Carpentry", Material.CRAFTING_TABLE, Stat.HEALTH, new double[][]{{50, 1}});

    /** XP, die fuer jedes einzelne Level benoetigt wird (Level 1..50). */
    public static final long[] XP = {
            50, 125, 200, 300, 500, 750, 1000, 1500, 2000, 3500,
            5000, 7500, 10000, 15000, 20000, 30000, 50000, 75000, 100000, 200000,
            300000, 400000, 500000, 600000, 700000, 800000, 900000, 1000000, 1100000, 1200000,
            1300000, 1400000, 1500000, 1600000, 1700000, 1800000, 1900000, 2000000, 2100000, 2200000,
            2300000, 2400000, 2500000, 2600000, 2750000, 2900000, 3100000, 3400000, 3700000, 4000000};

    public final String display;
    public final Material icon;
    public final Stat stat;
    private final double[][] tiers;

    SkillType(String display, Material icon, Stat stat, double[][] tiers) {
        this.display = display;
        this.icon = icon;
        this.stat = stat;
        this.tiers = tiers;
    }

    public double bonus(int level) {
        double total = 0;
        for (int l = 1; l <= level; l++) {
            for (double[] t : tiers) {
                if (l <= t[0]) {
                    total += t[1];
                    break;
                }
            }
        }
        return total;
    }

    public static int levelOf(double xp) {
        double rem = xp;
        int lvl = 0;
        for (long need : XP) {
            if (rem >= need) {
                rem -= need;
                lvl++;
            } else break;
        }
        return lvl;
    }

    public static double progress(double xp) {
        double rem = xp;
        for (long need : XP) {
            if (rem >= need) rem -= need;
            else return rem / need * 100.0;
        }
        return 100;
    }

    public static double xpForLevel(int level) {
        double sum = 0;
        for (int i = 0; i < Math.min(level, XP.length); i++) sum += XP[i];
        return sum;
    }

    public static SkillType byName(String n) {
        for (SkillType s : values()) if (s.name().equalsIgnoreCase(n)) return s;
        return null;
    }
}
