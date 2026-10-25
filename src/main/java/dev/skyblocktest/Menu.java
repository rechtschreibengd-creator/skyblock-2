package dev.skyblocktest;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public final class Menu implements InventoryHolder {
    public enum Kind { MAIN, SKILLS, ITEMS, ENCHANTS, ABILITIES, SETTINGS, CRAFT, RECIPES, RECIPE_VIEW, PETS, BOSSES }

    public final Kind kind;
    public int page = 0;
    public int ref = 0;
    private Inventory inv;

    public Menu(Kind kind) {
        this.kind = kind;
    }

    public void set(Inventory inv) {
        this.inv = inv;
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }
}
