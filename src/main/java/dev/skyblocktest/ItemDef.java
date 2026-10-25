package dev.skyblocktest;

import org.bukkit.Material;

import java.util.List;
import java.util.Map;

public record ItemDef(String id, Material material, String name, Rarity rarity, String type,
                      Map<Stat, Double> stats, Map<SkyEnchant, Integer> enchants,
                      String ability, List<String> description,
                      boolean glow, String pet, String summon, String book) {
}
