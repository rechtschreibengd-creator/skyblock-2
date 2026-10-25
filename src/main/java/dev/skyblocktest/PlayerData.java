package dev.skyblocktest;

import java.util.*;

public final class PlayerData {
    public final UUID uuid;
    public final EnumMap<SkillType, Double> xp = new EnumMap<>(SkillType.class);
    public final EnumMap<Stat, Double> stats = new EnumMap<>(Stat.class);
    public final Map<String, Long> cooldowns = new HashMap<>();
    public final Set<UUID> firstStrike = new HashSet<>();
    public double mana = 100;
    public record Buff(Stat stat, double amount, long until) {}
    public final List<Buff> buffs = new ArrayList<>();
    public final List<PetData> pets = new ArrayList<>();
    public int activePet = -1;
    public long phoenixUntil = 0;
    public boolean showDamage = true, showActionBar = true, levelUpMessages = true;
    public String xpMsg = "";
    public long xpMsgUntil = 0;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        for (Stat s : Stat.values()) stats.put(s, s.base);
    }

    public int level(SkillType s) {
        return SkillType.levelOf(xp.getOrDefault(s, 0.0));
    }

    public double stat(Stat s) {
        return stats.getOrDefault(s, s.base);
    }
}
