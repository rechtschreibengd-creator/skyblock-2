package dev.skyblocktest;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PlayerListener implements Listener {
    private final SkyblockPlugin plugin;

    public PlayerListener(SkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        plugin.data.get(p);
        plugin.stats.apply(p);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        plugin.data.remove(e.getPlayer().getUniqueId());
    }
}
