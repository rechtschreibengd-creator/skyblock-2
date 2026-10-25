package dev.skyblocktest;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PlayerDataManager {
    private final SkyblockPlugin plugin;
    private final File dir;
    private final Map<UUID, PlayerData> map = new ConcurrentHashMap<>();

    public PlayerDataManager(SkyblockPlugin plugin) {
        this.plugin = plugin;
        this.dir = new File(plugin.getDataFolder(), "players");
        dir.mkdirs();
    }

    public PlayerData get(Player p) {
        return map.computeIfAbsent(p.getUniqueId(), this::load);
    }

    private PlayerData load(UUID id) {
        PlayerData d = new PlayerData(id);
        File f = new File(dir, id + ".yml");
        if (f.exists()) {
            YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
            for (SkillType s : SkillType.values()) d.xp.put(s, y.getDouble("skills." + s.name(), 0));
            d.mana = y.getDouble("mana", 100);
            d.showDamage = y.getBoolean("settings.damage", true);
            d.showActionBar = y.getBoolean("settings.actionbar", true);
            d.levelUpMessages = y.getBoolean("settings.levelup", true);
            for (String raw : y.getStringList("pets")) {
                try {
                    String[] a = raw.split("\\|");
                    d.pets.add(new PetData(a[0], Rarity.valueOf(a[1]), Double.parseDouble(a[2])));
                } catch (Exception ignored) { }
            }
            d.activePet = y.getInt("activePet", -1);
        }
        return d;
    }

    public void save(UUID id) {
        PlayerData d = map.get(id);
        if (d == null) return;
        YamlConfiguration y = new YamlConfiguration();
        for (SkillType s : SkillType.values()) y.set("skills." + s.name(), d.xp.getOrDefault(s, 0.0));
        y.set("mana", d.mana);
        y.set("settings.damage", d.showDamage);
        y.set("settings.actionbar", d.showActionBar);
        y.set("settings.levelup", d.levelUpMessages);
        java.util.List<String> pl = new java.util.ArrayList<>();
        for (PetData p : d.pets) pl.add(p.type + "|" + p.rarity.name() + "|" + p.xp);
        y.set("pets", pl);
        y.set("activePet", d.activePet);
        try {
            y.save(new File(dir, id + ".yml"));
        } catch (IOException e) {
            plugin.getLogger().warning("Konnte Spielerdaten nicht speichern: " + e.getMessage());
        }
    }

    public void remove(UUID id) {
        save(id);
        map.remove(id);
    }

    public void saveAll() {
        map.keySet().forEach(this::save);
    }
}
