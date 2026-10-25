package dev.skyblocktest;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/** Pet-Eier, Boss-Beschwoerungen und Enchant-Buecher. */
public final class ItemActionListener implements Listener {
    private final SkyblockPlugin plugin;

    public ItemActionListener(SkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onUse(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND || !e.getAction().isRightClick()) return;
        ItemStack it = e.getItem();
        if (it == null || !it.hasItemMeta()) return;
        Player p = e.getPlayer();

        String pet = plugin.items.pet(it);
        if (pet != null) {
            e.setCancelled(true);
            PetType t = PetType.byId(pet);
            ItemDef d = plugin.registry.get(plugin.items.id(it));
            if (t == null || d == null) return;
            plugin.pets.add(p, t, d.rarity());
            it.setAmount(it.getAmount() - 1);
            return;
        }

        String summon = plugin.items.summon(it);
        if (summon != null) {
            e.setCancelled(true);
            Location loc = p.getLocation().add(p.getLocation().getDirection().setY(0).normalize().multiply(5));
            loc.setY(p.getWorld().getHighestBlockYAt(loc) + 1);
            if (loc.getY() < p.getLocation().getY() - 4 || loc.getY() > p.getLocation().getY() + 4) loc = p.getLocation().add(3, 0, 0);
            if (plugin.bosses.spawn(summon, loc, p)) it.setAmount(it.getAmount() - 1);
            else p.sendMessage(Text.c("&cDieser Boss existiert nicht."));
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBook(InventoryClickEvent e) {
        if (e.getInventory().getHolder() instanceof Menu) return;
        if (!(e.getWhoClicked() instanceof Player p)) return;
        ItemStack cursor = e.getCursor();
        ItemStack target = e.getCurrentItem();
        if (cursor == null || cursor.getType().isAir() || target == null || target.getType().isAir()) return;
        String book = plugin.items.book(cursor);
        if (book == null) return;
        e.setCancelled(true);

        String[] a = book.split(":");
        SkyEnchant en = SkyEnchant.byName(a[0]);
        int lvl;
        try {
            lvl = Integer.parseInt(a[1]);
        } catch (Exception ex) {
            return;
        }
        if (en == null) return;
        if (!compatible(en, target)) {
            p.sendMessage(Text.c("&c" + en.display + " passt nicht auf dieses Item."));
            return;
        }
        int have = plugin.items.enchant(target, en);
        int result;
        if (have < lvl) result = lvl;
        else if (have == lvl && lvl < en.maxLevel) result = lvl + 1;
        else {
            p.sendMessage(Text.c("&cDas Item hat " + en.display + " bereits auf diesem Level oder hoeher."));
            return;
        }
        plugin.items.setEnchant(target, en, result);
        e.setCurrentItem(target);
        ItemStack rest = cursor.clone();
        rest.setAmount(cursor.getAmount() - 1);
        p.setItemOnCursor(rest.getAmount() <= 0 ? null : rest);
        p.sendMessage(Text.c("&a" + en.display + " " + Text.roman(result) + " angewendet!"));
        plugin.stats.apply(p);
    }

    private boolean compatible(SkyEnchant en, ItemStack target) {
        String type = plugin.items.type(target);
        if (type == null) return false;
        return switch (en.cat) {
            case WEAPON -> type.equals("SWORD") || type.equals("BOW") || type.equals("WAND");
            case ARMOR -> type.equals("ARMOR");
            case BOOTS -> type.equals("ARMOR") && target.getType().name().endsWith("_BOOTS");
            case TOOL -> type.equals("TOOL");
        };
    }
}
