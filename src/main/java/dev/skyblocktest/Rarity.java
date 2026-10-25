package dev.skyblocktest;

public enum Rarity {
    COMMON("&f"), UNCOMMON("&a"), RARE("&9"), EPIC("&5"), LEGENDARY("&6"), MYTHIC("&d"), SPECIAL("&c");

    public final String color;

    Rarity(String color) {
        this.color = color;
    }
}
