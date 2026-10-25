package dev.skyblocktest;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class AbilityListener implements Listener {
    private final SkyblockPlugin plugin;

    public AbilityListener(SkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || !e.getAction().isRightClick()) return;
        ItemStack it = e.getItem();
        if (it == null) return;
        Ability a = plugin.abilities.get(plugin.items.ability(it));
        if (a == null) return;

        Player p = e.getPlayer();
        PlayerData d = plugin.data.get(p);
        long now = System.currentTimeMillis();
        Long until = d.cooldowns.get(a.id);
        if (until != null && until > now) {
            p.sendActionBar(Text.c("&cAbility im Cooldown (" + Text.fmt((until - now) / 1000.0) + "s)"));
            return;
        }
        if (d.mana < a.mana) {
            p.sendActionBar(Text.c("&cNicht genug Mana!"));
            return;
        }
        if (!a.action.run(p)) return;
        d.mana -= a.mana;
        d.cooldowns.put(a.id, now + a.cooldownTicks * 50L);
        p.sendActionBar(Text.c("&b" + a.name + " &3(-" + a.mana + " Mana)"));
        e.setCancelled(true);
    }
}
