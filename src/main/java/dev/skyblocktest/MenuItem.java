package dev.skyblocktest;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/** Der SkyBlock-Menue-Stern in der letzten Hotbar-Slot (wie bei Hypixel). */
public final class MenuItem implements Listener {
    public static final int SLOT = 8;
    private final SkyblockPlugin plugin;
    private final NamespacedKey key;

    public MenuItem(SkyblockPlugin plugin) {
        this.plugin = plugin;
        this.key = new NamespacedKey(plugin, "menu_item");
    }

    public ItemStack create() {
        ItemStack it = new ItemStack(Material.NETHER_STAR);
        it.editMeta(m -> {
            m.displayName(Text.c("&aSkyBlock Menu &7(Rechtsklick)"));
            m.lore(List.of(
                    Text.c("&7Dein Zugang zu Skills, Stats,"),
                    Text.c("&7Enchantments und Einstellungen."),
                    Text.c(""),
                    Text.c("&eRechtsklick zum Oeffnen!")));
            m.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
        });
        return it;
    }

    public boolean is(ItemStack it) {
        if (it == null || it.getType() != Material.NETHER_STAR || !it.hasItemMeta()) return false;
        return it.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    public void give(Player p) {
        PlayerInventory inv = p.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            if (is(inv.getItem(i))) inv.setItem(i, null);
        }
        ItemStack old = inv.getItem(SLOT);
        inv.setItem(SLOT, create());
        if (old != null && !old.getType().isAir()) {
            inv.addItem(old).values().forEach(x -> p.getWorld().dropItemNaturally(p.getLocation(), x));
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        give(e.getPlayer());
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        Player p = e.getPlayer();
        Bukkit.getScheduler().runTask(plugin, () -> give(p));
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        e.getDrops().removeIf(this::is);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || !is(e.getItem())) return;
        e.setCancelled(true);
        if (e.getAction().isRightClick()) Menus.main(plugin, e.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        if (is(e.getItemDrop().getItemStack())) e.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        if (is(e.getMainHandItem()) || is(e.getOffHandItem())) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onClick(InventoryClickEvent e) {
        boolean hit = is(e.getCurrentItem()) || is(e.getCursor());
        if (!hit && e.getWhoClicked() instanceof Player p && e.getHotbarButton() >= 0) {
            hit = is(p.getInventory().getItem(e.getHotbarButton()));
        }
        if (hit) e.setCancelled(true);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (is(e.getOldCursor())) e.setCancelled(true);
    }
}
