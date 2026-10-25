package dev.skyblocktest;

public final class PetData {
    public final String type;
    public final Rarity rarity;
    public double xp;

    public PetData(String type, Rarity rarity, double xp) {
        this.type = type;
        this.rarity = rarity;
        this.xp = xp;
    }
}
