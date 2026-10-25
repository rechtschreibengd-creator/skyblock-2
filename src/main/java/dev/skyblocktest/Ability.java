package dev.skyblocktest;

import org.bukkit.entity.Player;

public final class Ability {
    public interface Action {
        boolean run(Player p);
    }

    public final String id, name, description;
    public final int mana, cooldownTicks;
    public final Action action;

    public Ability(String id, String name, String description, int mana, int cooldownTicks, Action action) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.mana = mana;
        this.cooldownTicks = cooldownTicks;
        this.action = action;
    }
}
