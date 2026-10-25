package dev.skyblocktest;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;

public final class ItemRegistry {
    private final SkyblockPlugin plugin;
    private final Map<String, ItemDef> defs = new LinkedHashMap<>();

    public ItemRegistry(SkyblockPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        defs.clear();
        File f = new File(plugin.getDataFolder(), "items.yml");
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        ConfigurationSection sec = y.getConfigurationSection("items");
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(id);
            if (s == null) continue;
            try {
                Material m = Material.valueOf(s.getString("material", "STONE").toUpperCase());
                Map<Stat, Double> stats = new EnumMap<>(Stat.class);
                ConfigurationSection ss = s.getConfigurationSection("stats");
                if (ss != null) for (String k : ss.getKeys(false)) stats.put(Stat.valueOf(k.toUpperCase()), ss.getDouble(k));
                Map<SkyEnchant, Integer> ens = new EnumMap<>(SkyEnchant.class);
                ConfigurationSection es = s.getConfigurationSection("enchants");
                if (es != null) for (String k : es.getKeys(false)) {
                    SkyEnchant e = SkyEnchant.byName(k);
                    if (e == null) throw new IllegalArgumentException("Unbekanntes Enchantment " + k);
                    ens.put(e, es.getInt(k));
                }
                defs.put(id.toLowerCase(), new ItemDef(id.toLowerCase(), m, s.getString("name", id),
                        Rarity.valueOf(s.getString("rarity", "COMMON").toUpperCase()),
                        s.getString("type", "OTHER").toUpperCase(), stats, ens,
                        s.getString("ability"), s.getStringList("description"),
                        s.getBoolean("glow", false), s.getString("pet"), s.getString("summon"), s.getString("book")));
            } catch (Exception ex) {
                plugin.getLogger().warning("Item '" + id + "' fehlerhaft: " + ex.getMessage());
            }
        }
        plugin.getLogger().info(defs.size() + " Items geladen.");
    }

    public ItemDef get(String id) {
        return id == null ? null : defs.get(id.toLowerCase());
    }

    public Collection<ItemDef> all() {
        return defs.values();
    }
}
