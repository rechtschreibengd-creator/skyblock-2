package dev.skyblocktest;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.ThreadLocalRandom;

public final class SkillManager {
    private final SkyblockPlugin plugin;

    public SkillManager(SkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    public void addXp(Player p, SkillType skill, double amount) {
        PlayerData d = plugin.data.get(p);
        ItemStack hand = p.getInventory().getItemInMainHand();
        amount *= plugin.pets.xpMultiplier(d, skill);
        int exp = plugin.items.enchant(hand, SkyEnchant.EXPERIENCE);
        if (exp > 0 && ThreadLocalRandom.current().nextDouble() < 0.125 * exp) amount *= 2;

        plugin.pets.gainXp(p, d, skill, amount);
        int before = d.level(skill);
        double total = d.xp.getOrDefault(skill, 0.0) + amount;
        d.xp.put(skill, total);
        int after = SkillType.levelOf(total);

        d.xpMsg = "&3+" + Text.fmt(amount) + " " + skill.display + " &7(" + Text.fmt(SkillType.progress(total)) + "%)";
        d.xpMsgUntil = System.currentTimeMillis() + 2500;

        if (after > before) {
            StringBuilder reward = new StringBuilder();
            if (skill.stat != null) {
                double gain = skill.bonus(after) - skill.bonus(before);
                reward.append("&8+ ").append(skill.stat.color).append(Text.fmt(gain)).append(skill.stat.percent() ? "%" : "")
                        .append(" ").append(skill.stat.symbol).append(" ").append(skill.stat.display);
            }
            if (d.levelUpMessages) {
            p.sendMessage(Text.c("&3&m                                        "));
            p.sendMessage(Text.c("&b&lSKILL LEVEL UP &3" + skill.display + " &8" + before + "➜&3" + after));
            if (reward.length() > 0) p.sendMessage(Text.c(reward.toString()));
            p.sendMessage(Text.c("&3&m                                        "));
            }
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            plugin.stats.apply(p);
        }
    }
}
