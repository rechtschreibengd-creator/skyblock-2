package dev.skyblocktest;

public enum SkyEnchant {
    // Waffen
    SHARPNESS("Sharpness", 7, Cat.WEAPON, null, 0, "Erhoeht den Nahkampfschaden um 5% pro Level."),
    POWER("Power", 7, Cat.WEAPON, null, 0, "Erhoeht den Bogenschaden um 8% pro Level."),
    CRITICAL("Critical", 7, Cat.WEAPON, Stat.CRIT_DAMAGE, 10, "Gewaehrt +10% Crit Damage pro Level."),
    EXECUTE("Execute", 6, Cat.WEAPON, null, 0, "Mehr Schaden je weniger Leben der Gegner hat."),
    GIANT_KILLER("Giant Killer", 7, Cat.WEAPON, null, 0, "Mehr Schaden gegen Gegner mit mehr Leben als du."),
    FIRST_STRIKE("First Strike", 5, Cat.WEAPON, null, 0, "Erster Treffer auf einen Gegner: +25% Schaden pro Level."),
    VAMPIRISM("Vampirism", 6, Cat.WEAPON, null, 0, "Heilt einen Teil deines fehlenden Lebens beim Treffer."),
    LIFE_STEAL("Life Steal", 5, Cat.WEAPON, null, 0, "Heilt 0.5% deiner maximalen Gesundheit pro Level beim Treffer."),
    ENDER_SLAYER("Ender Slayer", 7, Cat.WEAPON, null, 0, "+12% Schaden gegen Ender-Monster pro Level."),
    // Ruestung
    PROTECTION("Protection", 7, Cat.ARMOR, Stat.DEFENSE, 3, "Gewaehrt +3 Defense pro Level."),
    GROWTH("Growth", 7, Cat.ARMOR, Stat.HEALTH, 15, "Gewaehrt +15 Health pro Level."),
    BIG_BRAIN("Big Brain", 5, Cat.ARMOR, Stat.INTELLIGENCE, 5, "Gewaehrt +5 Intelligence pro Level."),
    THORNS("Thorns", 3, Cat.ARMOR, null, 0, "Reflektiert Schaden auf Angreifer."),
    SUGAR_RUSH("Sugar Rush", 3, Cat.BOOTS, Stat.SPEED, 2, "Gewaehrt +2 Speed pro Level."),
    // Werkzeuge
    EFFICIENCY("Efficiency", 5, Cat.TOOL, null, 0, "Gewaehrt Haste, solange du das Werkzeug haeltst."),
    FORTUNE("Fortune", 4, Cat.TOOL, null, 0, "+25% Erz-Drops pro Level."),
    HARVESTING("Harvesting", 6, Cat.TOOL, null, 0, "+25% Ernte-Drops pro Level."),
    SMELTING_TOUCH("Smelting Touch", 1, Cat.TOOL, null, 0, "Schmilzt abgebaute Erze automatisch."),
    TELEKINESIS("Telekinesis", 1, Cat.TOOL, null, 0, "Drops landen direkt in deinem Inventar."),
    EXPERIENCE("Experience", 4, Cat.TOOL, null, 0, "12.5% Chance pro Level, Skill-XP zu verdoppeln.");

    public enum Cat { WEAPON, ARMOR, BOOTS, TOOL }

    public final String display;
    public final int maxLevel;
    public final Cat cat;
    public final Stat stat;
    public final double perLevel;
    public final String description;

    SkyEnchant(String display, int maxLevel, Cat cat, Stat stat, double perLevel, String description) {
        this.display = display;
        this.maxLevel = maxLevel;
        this.cat = cat;
        this.stat = stat;
        this.perLevel = perLevel;
        this.description = description;
    }

    public static SkyEnchant byName(String n) {
        String k = n.toUpperCase().replace(' ', '_');
        for (SkyEnchant e : values()) if (e.name().equals(k)) return e;
        return null;
    }
}
