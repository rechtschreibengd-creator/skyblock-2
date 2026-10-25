package dev.skyblocktest;

import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.Map;

public final class PetManager {
    private final SkyblockPlugin plugin;

    public PetManager(SkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    public static double xpForNext(int level) {
        return 100 + 60.0 * level;
    }

    public static int levelOf(double xp) {
        double rem = xp;
        int lvl = 1;
        while (lvl < 100 && rem >= xpForNext(lvl)) {
            rem -= xpForNext(lvl);
            lvl++;
        }
        return lvl;
    }

    public static double progress(double xp) {
        double rem = xp;
        int lvl = 1;
        while (lvl < 100 && rem >= xpForNext(lvl)) {
            rem -= xpForNext(lvl);
            lvl++;
        }
        return lvl >= 100 ? 100 : rem / xpForNext(lvl) * 100.0;
    }

    public static double rarityMult(Rarity r) {
        return switch (r) {
            case COMMON -> 0.6;
            case UNCOMMON -> 0.7;
            case RARE -> 0.8;
            case EPIC -> 0.9;
            case LEGENDARY -> 1.0;
            default -> 1.15;
        };
    }

    public static Map<Stat, Double> statsOf(PetData p) {
        Map<Stat, Double> out = new EnumMap<>(Stat.class);
        PetType t = PetType.byId(p.type);
        if (t == null) return out;
        double f = levelOf(p.xp) / 100.0 * rarityMult(p.rarity);
        for (int i = 0; i < t.stats.length; i++) out.put(t.stats[i], t.at100[i] * f);
        return out;
    }

    public PetData active(PlayerData d) {
        if (d.activePet < 0 || d.activePet >= d.pets.size()) return null;
        return d.pets.get(d.activePet);
    }

    public boolean hasPerk(PlayerData d, PetType type) {
        PetData a = active(d);
        return a != null && a.type.equalsIgnoreCase(type.name());
    }

    /** Gibt Pet-XP, wenn das aktive Pet zum Skill passt. Liefert den Skill-XP-Multiplikator zurueck. */
    public double xpMultiplier(PlayerData d, SkillType skill) {
        PetData a = active(d);
        PetType t = a == null ? null : PetType.byId(a.type);
        if (t == null || t.skill != skill) return 1.0;
        return 1.0 + t.xpBoost;
    }

    public void gainXp(Player p, PlayerData d, SkillType skill, double amount) {
        PetData a = active(d);
        PetType t = a == null ? null : PetType.byId(a.type);
        if (t == null || t.skill != skill) return;
        int before = levelOf(a.xp);
        a.xp += amount * 0.5;
        int after = levelOf(a.xp);
        if (after > before) {
            p.sendMessage(Text.c("&aDein " + a.rarity.color + t.display + " &aPet ist jetzt Level &e" + after + "&a!"));
            plugin.stats.apply(p);
        }
    }

    public void add(Player p, PetType t, Rarity r) {
        PlayerData d = plugin.data.get(p);
        d.pets.add(new PetData(t.name(), r, 0));
        p.sendMessage(Text.c("&aNeues Pet freigeschaltet: " + r.color + t.display + " &7(&e/pets&7)"));
    }

    public void toggle(Player p, int index) {
        PlayerData d = plugin.data.get(p);
        if (index < 0 || index >= d.pets.size()) return;
        if (d.activePet == index) {
            d.activePet = -1;
            p.sendMessage(Text.c("&7Pet eingepackt."));
        } else {
            d.activePet = index;
            PetData a = d.pets.get(index);
            PetType t = PetType.byId(a.type);
            p.sendMessage(Text.c("&aPet ausgewaehlt: " + a.rarity.color + (t == null ? a.type : t.display)));
        }
        plugin.stats.apply(p);
    }
}
