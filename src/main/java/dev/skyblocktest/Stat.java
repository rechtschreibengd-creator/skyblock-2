package dev.skyblocktest;

public enum Stat {
    HEALTH("Health", "❤", "&c", 100),
    DEFENSE("Defense", "❈", "&a", 0),
    STRENGTH("Strength", "❁", "&c", 0),
    CRIT_CHANCE("Crit Chance", "☣", "&9", 30),
    CRIT_DAMAGE("Crit Damage", "☠", "&9", 50),
    INTELLIGENCE("Intelligence", "✎", "&b", 100),
    SPEED("Speed", "✦", "&f", 100),
    MAGIC_FIND("Magic Find", "✯", "&b", 0),
    DAMAGE("Damage", "❁", "&c", 0);

    public final String display, symbol, color;
    public final double base;

    Stat(String display, String symbol, String color, double base) {
        this.display = display;
        this.symbol = symbol;
        this.color = color;
        this.base = base;
    }

    public boolean percent() {
        return this == CRIT_CHANCE || this == CRIT_DAMAGE;
    }
}
