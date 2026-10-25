package dev.skyblocktest;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/** SkyBlock-Werkbank: 3x3-Raster, Zutaten mit Mengen pro Slot (z.B. 64x Enchanted Coal). */
public final class CraftListener implements Listener {
    public static final int[] GRID = {10, 11, 12, 19, 20, 21, 28, 29, 30};
    public static final int RESULT = 24;
    public static final int SIZE = 45;
    private final SkyblockPlugin plugin;

    public CraftListener(SkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    static int gridIndex(int slot) {
        for (int i = 0; i < GRID.length; i++) if (GRID[i] == slot) return i;
        return -1;
    }

    public static void open(SkyblockPlugin plugin, Player p) {
        Menu h = new Menu(Menu.Kind.CRAFT);
        Inventory inv = Bukkit.createInventory(h, SIZE, Text.c("&8SkyBlock Werkbank"));
        h.set(inv);
        ItemStack pane = Menus.icon(Material.BLACK_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < SIZE; i++) if (gridIndex(i) < 0) inv.setItem(i, pane);
        inv.setItem(40, Menus.icon(Material.ARROW, "&aZurueck", java.util.List.of("&7Zum SkyBlock Menu")));
        inv.setItem(RESULT, noRecipe());
        p.openInventory(inv);
    }

    private static ItemStack noRecipe() {
        return Menus.icon(Material.BARRIER, "&cKein Rezept", java.util.List.of(
                "&7Lege Zutaten ins Raster.", "&7Mengen pro Slot zaehlen!", "", "&8Alle Rezepte: /recipes"));
    }

    private ItemStack[] grid(Inventory inv) {
        ItemStack[] g = new ItemStack[9];
        for (int i = 0; i < 9; i++) {
            ItemStack it = inv.getItem(GRID[i]);
            g[i] = (it == null || it.getType().isAir()) ? null : it.clone();
        }
        return g;
    }

    private void refresh(Inventory inv) {
        RecipeRegistry.Recipe r = plugin.recipes.match(grid(inv));
        inv.setItem(RESULT, r == null ? noRecipe() : plugin.recipes.result(r));
    }

    private void later(Inventory inv) {
        Bukkit.getScheduler().runTask(plugin, () -> refresh(inv));
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof Menu m) || m.kind != Menu.Kind.CRAFT) return;
        if (!(e.getWhoClicked() instanceof Player p)) return;
        Inventory top = e.getInventory();
        int raw = e.getRawSlot();

        if (e.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            e.setCancelled(true);
            return;
        }

        if (raw >= SIZE) { // Spieler-Inventar
            if (e.isShiftClick()) {
                e.setCancelled(true);
                ItemStack it = e.getCurrentItem();
                if (it == null || it.getType().isAir()) return;
                ItemStack rest = it.clone();
                for (int slot : GRID) {
                    ItemStack in = top.getItem(slot);
                    if (in != null && !in.getType().isAir() && in.isSimilar(rest) && in.getAmount() < in.getMaxStackSize()) {
                        int move = Math.min(rest.getAmount(), in.getMaxStackSize() - in.getAmount());
                        in.setAmount(in.getAmount() + move);
                        rest.setAmount(rest.getAmount() - move);
                        if (rest.getAmount() <= 0) break;
                    }
                }
                if (rest.getAmount() > 0) {
                    for (int slot : GRID) {
                        ItemStack in = top.getItem(slot);
                        if (in == null || in.getType().isAir()) {
                            top.setItem(slot, rest.clone());
                            rest.setAmount(0);
                            break;
                        }
                    }
                }
                e.setCurrentItem(rest.getAmount() <= 0 ? null : rest);
                later(top);
            }
            return;
        }

        if (raw == 40) {
            e.setCancelled(true);
            Bukkit.getScheduler().runTask(plugin, () -> Menus.main(plugin, p));
            return;
        }
        if (raw == RESULT) {
            e.setCancelled(true);
            ItemStack shown = top.getItem(RESULT);
            if (shown == null || shown.getType() == Material.BARRIER) return;
            int crafted = 0;
            do {
                ItemStack[] g = grid(top);
                RecipeRegistry.Recipe r = plugin.recipes.match(g);
                if (r == null) break;
                plugin.recipes.consume(r, g);
                for (int i = 0; i < 9; i++) top.setItem(GRID[i], g[i]);
                give(p, plugin.recipes.result(r));
                crafted++;
            } while (e.isShiftClick() && crafted < 64);
            if (crafted > 0) {
                p.playSound(p.getLocation(), Sound.BLOCK_ANVIL_USE, 0.6f, 1.4f);
                plugin.skills.addXp(p, SkillType.CARPENTRY, 5.0 * crafted);
            }
            refresh(top);
            return;
        }
        if (gridIndex(raw) < 0) {
            e.setCancelled(true);
            return;
        }
        later(top);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (!(e.getInventory().getHolder() instanceof Menu m) || m.kind != Menu.Kind.CRAFT) return;
        for (int raw : e.getRawSlots()) {
            if (raw < SIZE && gridIndex(raw) < 0) {
                e.setCancelled(true);
                return;
            }
        }
        later(e.getInventory());
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (!(e.getInventory().getHolder() instanceof Menu m) || m.kind != Menu.Kind.CRAFT) return;
        if (!(e.getPlayer() instanceof Player p)) return;
        for (int slot : GRID) {
            ItemStack it = e.getInventory().getItem(slot);
            if (it != null && !it.getType().isAir()) {
                give(p, it);
                e.getInventory().setItem(slot, null);
            }
        }
    }

    private void give(Player p, ItemStack it) {
        p.getInventory().addItem(it).values().forEach(x -> p.getWorld().dropItemNaturally(p.getLocation(), x));
    }
}
