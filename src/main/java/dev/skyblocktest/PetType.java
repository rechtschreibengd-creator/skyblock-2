package dev.skyblocktest;

import org.bukkit.Material;

public enum PetType {
    WOLF("Wolf", Material.BONE, SkillType.COMBAT, "Rudelfuehrer: Stats wirken staerker im Kampf.",
            new Stat[]{Stat.STRENGTH, Stat.CRIT_DAMAGE}, new double[]{40, 30}, 0),
    TIGER("Tiger", Material.COOKED_CHICKEN, SkillType.COMBAT, "Raubtier: hohe Crit-Werte.",
            new Stat[]{Stat.CRIT_CHANCE, Stat.CRIT_DAMAGE, Stat.STRENGTH}, new double[]{15, 50, 20}, 0),
    ENDERMAN("Enderman", Material.ENDER_PEARL, SkillType.COMBAT, "Zero Point: +25% Schaden gegen Ender-Monster.",
            new Stat[]{Stat.INTELLIGENCE, Stat.CRIT_DAMAGE}, new double[]{60, 40}, 0),
    PHOENIX("Phoenix", Material.BLAZE_POWDER, SkillType.COMBAT, "Wiedergeburt: Ueberlebe einmal alle 5 Minuten toedlichen Schaden.",
            new Stat[]{Stat.STRENGTH, Stat.INTELLIGENCE, Stat.MAGIC_FIND}, new double[]{50, 80, 10}, 0),
    ZOMBIE("Zombie", Material.ROTTEN_FLESH, SkillType.COMBAT, "Untote Zaehigkeit.",
            new Stat[]{Stat.HEALTH, Stat.DEFENSE, Stat.STRENGTH}, new double[]{120, 30, 20}, 0),
    SPIDER("Spider", Material.SPIDER_EYE, SkillType.COMBAT, "Netzlaeufer: schnell und kritisch.",
            new Stat[]{Stat.CRIT_CHANCE, Stat.STRENGTH, Stat.SPEED}, new double[]{10, 25, 10}, 0),
    SKELETON("Skeleton", Material.BONE_MEAL, SkillType.COMBAT, "Scharfschuetze: starke Crits.",
            new Stat[]{Stat.CRIT_DAMAGE, Stat.STRENGTH}, new double[]{40, 10}, 0),
    GOLEM("Golem", Material.IRON_BLOCK, SkillType.COMBAT, "Eisenhaut: extrem robust.",
            new Stat[]{Stat.HEALTH, Stat.DEFENSE, Stat.STRENGTH}, new double[]{200, 80, 15}, 0),
    BEE("Bee", Material.HONEYCOMB, SkillType.FARMING, "Fleissig: +20% Farming-XP.",
            new Stat[]{Stat.SPEED, Stat.HEALTH}, new double[]{15, 100}, 0.20),
    ELEPHANT("Elephant", Material.LEATHER, SkillType.FARMING, "Dickhaeuter: +15% Farming-XP.",
            new Stat[]{Stat.HEALTH, Stat.DEFENSE}, new double[]{150, 40}, 0.15),
    ROCK("Rock", Material.COBBLESTONE, SkillType.MINING, "Felsenfest: +20% Mining-XP.",
            new Stat[]{Stat.DEFENSE, Stat.STRENGTH}, new double[]{60, 15}, 0.20),
    SQUID("Squid", Material.INK_SAC, SkillType.FISHING, "Tintenfisch: +20% Fishing-XP.",
            new Stat[]{Stat.HEALTH, Stat.INTELLIGENCE}, new double[]{100, 50}, 0.20),
    MONKEY("Monkey", Material.JUNGLE_LEAVES, SkillType.FORAGING, "Kletterer: +20% Foraging-XP.",
            new Stat[]{Stat.STRENGTH, Stat.SPEED}, new double[]{25, 20}, 0.20);

    public final String display, perk;
    public final Material icon;
    public final SkillType skill;
    public final Stat[] stats;
    public final double[] at100;
    public final double xpBoost;

    PetType(String display, Material icon, SkillType skill, String perk, Stat[] stats, double[] at100, double xpBoost) {
        this.display = display;
        this.icon = icon;
        this.skill = skill;
        this.perk = perk;
        this.stats = stats;
        this.at100 = at100;
        this.xpBoost = xpBoost;
    }

    public static PetType byId(String id) {
        if (id == null) return null;
        for (PetType t : values()) if (t.name().equalsIgnoreCase(id)) return t;
        return null;
    }
}
