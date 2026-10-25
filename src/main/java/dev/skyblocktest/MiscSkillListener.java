package dev.skyblocktest;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.Material;

public final class MiscSkillListener implements Listener {
    private final SkyblockPlugin plugin;

    public MiscSkillListener(SkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onFish(PlayerFishEvent e) {
        if (e.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            plugin.skills.addXp(e.getPlayer(), SkillType.FISHING, 20);
        }
    }

    @EventHandler
    public void onEnchant(EnchantItemEvent e) {
        plugin.skills.addXp(e.getEnchanter(), SkillType.ENCHANTING, e.getExpLevelCost() * 5 + 10);
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent e) {
        if (e.getItem().getType() == Material.POTION) plugin.skills.addXp(e.getPlayer(), SkillType.ALCHEMY, 8);
    }

    @EventHandler
    public void onCraft(CraftItemEvent e) {
        if (e.getWhoClicked() instanceof Player p) plugin.skills.addXp(p, SkillType.CARPENTRY, 2);
    }
}
