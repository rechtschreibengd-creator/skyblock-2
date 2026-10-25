package dev.skyblocktest;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class Menus implements Listener {
    private final SkyblockPlugin plugin;

    public Menus(SkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------ Helfer
    static ItemStack icon(Material mat, String name, List<String> lore) {
        ItemStack it = new ItemStack(mat);
        it.editMeta(m -> {
            m.displayName(Text.c(name));
            if (lore != null) m.lore(lore.stream().map(Text::c).toList());
            m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        });
        return it;
    }

    private static ItemStack pane() {
        return icon(Material.BLACK_STAINED_GLASS_PANE, " ", null);
    }

    private static ItemStack back(String text) {
        return icon(Material.ARROW, "&aZurueck", List.of("&7" + text));
    }

    private static void fill(Inventory inv, int from, int to) {
        for (int i = from; i < to; i++) if (inv.getItem(i) == null) inv.setItem(i, pane());
    }

    private static Inventory create(Menu.Kind kind, int size, String title) {
        Menu h = new Menu(kind);
        Inventory inv = Bukkit.createInventory(h, size, Text.c(title));
        h.set(inv);
        return inv;
    }

    private static String avg(PlayerData d) {
        double sum = 0;
        for (SkillType s : SkillType.values()) sum += d.level(s);
        return Text.fmt(Math.round(sum / SkillType.values().length * 10) / 10.0);
    }

    private static int grid(int i) {
        return 10 + (i / 7) * 9 + (i % 7);
    }

    private void later(Runnable r) {
        Bukkit.getScheduler().runTask(plugin, r);
    }

    // ------------------------------------------------------------ Hauptmenue
    public static void main(SkyblockPlugin plugin, Player p) {
        Inventory inv = create(Menu.Kind.MAIN, 54, "&8SkyBlock Menu");
        PlayerData d = plugin.data.get(p);
        boolean admin = p.hasPermission("skyblocktest.admin");

        inv.setItem(4, profile(p, d));
        inv.setItem(20, icon(Material.IRON_SWORD, "&aSkills", List.of(
                "&7Sieh dir deine Skill-Level,", "&7XP und Boni an.", "",
                "&7Skill-Durchschnitt: &6" + avg(d), "", "&eKlicken zum Ansehen!")));
        inv.setItem(22, icon(Material.ENCHANTING_TABLE, "&aEnchantments", List.of(
                "&7Alle Enchantments mit", "&7Beschreibung und Max-Level.", "", "&eKlicken zum Ansehen!")));
        inv.setItem(24, icon(Material.ENDER_PEARL, "&aAbilities", List.of(
                "&7Alle Item-Abilities mit", "&7Mana-Kosten und Cooldown.", "", "&eKlicken zum Ansehen!")));
        inv.setItem(29, icon(Material.CRAFTING_TABLE, "&aSkyBlock Werkbank", List.of(
                "&7Stelle eigene Items her.", "&7Zutaten haben Mengen pro Slot!", "", "&eKlicken zum Oeffnen!")));
        inv.setItem(31, icon(Material.BONE, "&aPets", List.of(
                "&7Deine Pets: &e" + d.pets.size(), "&7Rüste ein Pet aus fuer Stats.", "", "&eKlicken zum Oeffnen!")));
        inv.setItem(33, icon(Material.BOOK, "&aRezeptbuch", List.of(
                "&7Alle Rezepte im Ueberblick.", "&7Aktuell: &e" + plugin.recipes.all().size() + " Rezepte", "", "&eKlicken zum Oeffnen!")));
        inv.setItem(38, icon(Material.WITHER_SKELETON_SKULL, "&aMinibosse", List.of(
                "&7Spawnchancen und seltene Drops", "&7aller Minibosse.", "", "&eKlicken zum Ansehen!")));
        inv.setItem(40, icon(Material.CHEST, "&aItem-Browser", List.of(
                "&7Alle Plugin-Items zum Nehmen.", "",
                admin ? "&eKlicken zum Oeffnen!" : "&cNur fuer Admins.")));
        inv.setItem(42, icon(Material.COMPARATOR, "&aEinstellungen", List.of(
                "&7Schadensanzeige, Action-Bar", "&7und Nachrichten umschalten.", "", "&eKlicken zum Oeffnen!")));
        inv.setItem(49, icon(Material.BARRIER, "&cSchliessen", null));
        fill(inv, 0, 54);
        p.openInventory(inv);
    }

    private static ItemStack profile(Player p, PlayerData d) {
        List<String> L = new ArrayList<>();
        L.add("&7Deine Stats:");
        for (Stat s : Stat.values()) {
            if (s == Stat.DAMAGE) continue;
            L.add(s.color + s.symbol + " " + s.display + " &f" + Text.fmt(d.stat(s)) + (s.percent() ? "%" : ""));
        }
        L.add("");
        L.add("&7Skill-Durchschnitt: &6" + avg(d));
        ItemStack it = new ItemStack(Material.PLAYER_HEAD);
        it.editMeta(SkullMeta.class, m -> {
            m.setOwningPlayer(p);
            m.displayName(Text.c("&a" + p.getName() + "s Profil"));
            m.lore(L.stream().map(Text::c).toList());
        });
        return it;
    }

    // ------------------------------------------------------------ Skills
    public static void skills(SkyblockPlugin plugin, Player p) {
        Inventory inv = create(Menu.Kind.SKILLS, 27, "&8Deine Skills");
        PlayerData d = plugin.data.get(p);
        int i = 0;
        for (SkillType s : SkillType.values()) {
            double xp = d.xp.getOrDefault(s, 0.0);
            int lvl = SkillType.levelOf(xp);
            List<String> L = new ArrayList<>();
            L.add("&7Level: &a" + lvl + "&7/50");
            L.add("&7Fortschritt: &e" + Text.fmt(SkillType.progress(xp)) + "%");
            L.add("&7Gesamt-XP: &b" + Text.fmt(xp));
            L.add("");
            if (s.stat != null) {
                L.add("&7Boni bisher:");
                L.add(s.stat.color + "+" + Text.fmt(s.bonus(lvl)) + (s.stat.percent() ? "%" : "") + " " + s.stat.symbol + " " + s.stat.display);
            }
            inv.setItem(9 + i++, icon(s.icon, "&a" + s.display, L));
        }
        inv.setItem(22, back("Zum SkyBlock Menu"));
        fill(inv, 0, 27);
        p.openInventory(inv);
    }

    // ------------------------------------------------------------ Enchantments
    public static void enchants(SkyblockPlugin plugin, Player p) {
        Inventory inv = create(Menu.Kind.ENCHANTS, 54, "&8Enchantments");
        int i = 0;
        for (SkyEnchant e : SkyEnchant.values()) {
            List<String> L = new ArrayList<>();
            for (String ln : Text.wrap(e.description, 36)) L.add("&7" + ln);
            L.add("");
            L.add("&7Max Level: &a" + Text.roman(e.maxLevel));
            L.add("&7Typ: &e" + catName(e.cat));
            L.add("&8/sb enchant " + e.name().toLowerCase() + " <level>");
            inv.setItem(grid(i++), icon(Material.ENCHANTED_BOOK, "&9" + e.display, L));
        }
        inv.setItem(49, back("Zum SkyBlock Menu"));
        fill(inv, 0, 54);
        p.openInventory(inv);
    }

    private static String catName(SkyEnchant.Cat c) {
        return switch (c) {
            case WEAPON -> "Waffe";
            case ARMOR -> "Ruestung";
            case BOOTS -> "Stiefel";
            case TOOL -> "Werkzeug";
        };
    }

    // ------------------------------------------------------------ Abilities
    public static void abilities(SkyblockPlugin plugin, Player p) {
        Inventory inv = create(Menu.Kind.ABILITIES, 54, "&8Abilities");
        Map<String, Material> mats = new java.util.HashMap<>();
        mats.put("instant_transmission", Material.ENDER_PEARL);
        mats.put("wither_impact", Material.WITHER_SKELETON_SKULL);
        mats.put("healing_circle", Material.GOLDEN_APPLE);
        mats.put("ice_spray", Material.SNOWBALL);
        mats.put("giant_slam", Material.ANVIL);
        mats.put("lightning_strike", Material.LIGHTNING_ROD);
        mats.put("meteor_strike", Material.FIRE_CHARGE);
        mats.put("grappling_hook", Material.FISHING_ROD);
        mats.put("leap", Material.FEATHER);
        mats.put("war_cry", Material.GOAT_HORN);
        mats.put("life_surge", Material.GLISTERING_MELON_SLICE);
        mats.put("wither_shield", Material.SHIELD);
        mats.put("gravity_well", Material.OBSIDIAN);
        mats.put("mana_flux", Material.LAPIS_LAZULI);
        mats.put("shadow_step", Material.ECHO_SHARD);
        mats.put("venom_burst", Material.FERMENTED_SPIDER_EYE);
        mats.put("soul_drain", Material.SOUL_LANTERN);
        int i = 0;
        for (Ability a : plugin.abilities.all().values()) {
            List<String> L = new ArrayList<>();
            for (String ln : Text.wrap(a.description, 36)) L.add("&7" + ln);
            L.add("");
            L.add("&8Mana Cost: &3" + a.mana);
            if (a.cooldownTicks > 0) L.add("&8Cooldown: &a" + (a.cooldownTicks / 20) + "s");
            L.add("");
            L.add("&7Auf diesen Items:");
            int shown = 0, total = 0;
            for (ItemDef d : plugin.registry.all()) {
                if (!a.id.equals(d.ability())) continue;
                total++;
                if (shown < 6) {
                    L.add("&8- " + d.rarity().color + d.name());
                    shown++;
                }
            }
            if (total > shown) L.add("&8+" + (total - shown) + " weitere");
            inv.setItem(grid(i++), icon(mats.getOrDefault(a.id, Material.NETHER_STAR), "&6" + a.name, L));
        }
        inv.setItem(49, back("Zum SkyBlock Menu"));
        fill(inv, 0, 54);
        p.openInventory(inv);
    }

    // ------------------------------------------------------------ Einstellungen
    public static void settings(SkyblockPlugin plugin, Player p) {
        Inventory inv = create(Menu.Kind.SETTINGS, 27, "&8Einstellungen");
        renderSettings(inv, plugin.data.get(p));
        p.openInventory(inv);
    }

    private static void renderSettings(Inventory inv, PlayerData d) {
        inv.setItem(11, toggle("Schadensanzeige", "Zeigt Schadenszahlen ueber Gegnern an.", d.showDamage));
        inv.setItem(13, toggle("Action-Bar", "Zeigt Leben, Defense und Mana an.", d.showActionBar));
        inv.setItem(15, toggle("Level-Up-Nachrichten", "Chat-Nachricht bei Skill-Level-Ups.", d.levelUpMessages));
        inv.setItem(22, back("Zum SkyBlock Menu"));
        fill(inv, 0, 27);
    }

    private static ItemStack toggle(String name, String desc, boolean on) {
        return icon(on ? Material.LIME_DYE : Material.GRAY_DYE, "&a" + name, List.of(
                "&7" + desc, "", "&7Status: " + (on ? "&aAn" : "&cAus"), "", "&eKlicken zum Umschalten!"));
    }

    // ------------------------------------------------------------ Item-Browser
    public static void items(SkyblockPlugin plugin, Player p) {
        items(plugin, p, 0);
    }

    public static void items(SkyblockPlugin plugin, Player p, int page) {
        List<ItemDef> all = new ArrayList<>(plugin.registry.all());
        int pages = Math.max(1, (all.size() + 44) / 45);
        int pg = Math.max(0, Math.min(page, pages - 1));
        Menu h = new Menu(Menu.Kind.ITEMS);
        h.page = pg;
        Inventory inv = Bukkit.createInventory(h, 54, Text.c("&8Items " + (pg + 1) + "/" + pages + " (Klick = nehmen)"));
        h.set(inv);
        for (int i = 0; i < 45; i++) {
            int idx = pg * 45 + i;
            if (idx >= all.size()) break;
            inv.setItem(i, plugin.items.create(all.get(idx), 1));
        }
        if (pg > 0) inv.setItem(45, icon(Material.ARROW, "&aSeite zurueck", null));
        if (pg < pages - 1) inv.setItem(53, icon(Material.ARROW, "&aNaechste Seite", null));
        inv.setItem(49, back("Zum SkyBlock Menu"));
        fill(inv, 45, 54);
        p.openInventory(inv);
    }

    // ------------------------------------------------------------ Hilfen fuer neue Menues
    static ItemStack withLore(ItemStack it, String... extra) {
        ItemStack c = it.clone();
        c.editMeta(m -> {
            List<net.kyori.adventure.text.Component> l = m.lore() == null ? new ArrayList<>() : new ArrayList<>(m.lore());
            for (String x : extra) l.add(Text.c(x));
            m.lore(l);
        });
        return c;
    }

    static int fromGrid(int slot) {
        if (slot < 10) return -1;
        int row = (slot - 10) / 9, col = (slot - 10) % 9;
        return col < 7 ? row * 7 + col : -1;
    }

    // ------------------------------------------------------------ Rezeptbuch
    public static void recipes(SkyblockPlugin plugin, Player p, int page) {
        List<RecipeRegistry.Recipe> all = plugin.recipes.list();
        int pages = Math.max(1, (all.size() + 44) / 45);
        int pg = Math.max(0, Math.min(page, pages - 1));
        Menu h = new Menu(Menu.Kind.RECIPES);
        h.page = pg;
        Inventory inv = Bukkit.createInventory(h, 54, Text.c("&8Rezeptbuch " + (pg + 1) + "/" + pages));
        h.set(inv);
        for (int i = 0; i < 45; i++) {
            int idx = pg * 45 + i;
            if (idx >= all.size()) break;
            inv.setItem(i, withLore(plugin.recipes.result(all.get(idx)), "", "&eKlicken, um das Rezept zu sehen!"));
        }
        if (pg > 0) inv.setItem(45, icon(Material.ARROW, "&aSeite zurueck", null));
        if (pg < pages - 1) inv.setItem(53, icon(Material.ARROW, "&aNaechste Seite", null));
        inv.setItem(49, back("Zum SkyBlock Menu"));
        fill(inv, 45, 54);
        p.openInventory(inv);
    }

    public static void recipeView(SkyblockPlugin plugin, Player p, int idx, int backPage) {
        List<RecipeRegistry.Recipe> all = plugin.recipes.list();
        if (idx < 0 || idx >= all.size()) return;
        RecipeRegistry.Recipe r = all.get(idx);
        Menu h = new Menu(Menu.Kind.RECIPE_VIEW);
        h.ref = backPage;
        Inventory inv = Bukkit.createInventory(h, CraftListener.SIZE, Text.c("&8Rezept" + (r.shapeless ? " (formlos)" : "")));
        h.set(inv);
        for (int i = 0; i < CraftListener.SIZE; i++) inv.setItem(i, pane());
        for (int slot : CraftListener.GRID) inv.setItem(slot, null);
        if (r.shapeless) {
            for (int i = 0; i < r.list.size(); i++) inv.setItem(CraftListener.GRID[i], ingredient(plugin, r.list.get(i), true));
        } else {
            for (int i = 0; i < r.grid.length; i++)
                for (int j = 0; j < r.grid[0].length; j++)
                    if (r.grid[i][j] != null) inv.setItem(CraftListener.GRID[i * 3 + j], ingredient(plugin, r.grid[i][j], false));
        }
        inv.setItem(CraftListener.RESULT, plugin.recipes.result(r));
        inv.setItem(40, back("Zum Rezeptbuch"));
        p.openInventory(inv);
    }

    private static ItemStack ingredient(SkyblockPlugin plugin, RecipeRegistry.Ing ing, boolean total) {
        ItemStack it = plugin.recipes.stackFor(ing.key(), 1);
        it.setAmount(Math.max(1, Math.min(64, ing.amount())));
        return withLore(it, "", total ? "&7Benoetigt insgesamt: &e" + ing.amount() : "&7Benoetigt in diesem Slot: &e" + ing.amount());
    }

    // ------------------------------------------------------------ Pets
    public static void pets(SkyblockPlugin plugin, Player p) {
        Inventory inv = create(Menu.Kind.PETS, 54, "&8Deine Pets");
        PlayerData d = plugin.data.get(p);
        for (int i = 0; i < d.pets.size() && i < 28; i++) {
            PetData pd = d.pets.get(i);
            PetType t = PetType.byId(pd.type);
            if (t == null) continue;
            boolean active = d.activePet == i;
            int lvl = PetManager.levelOf(pd.xp);
            List<String> L = new ArrayList<>();
            L.add("&7Skill: &e" + t.skill.display);
            L.add("&7Level: &a" + lvl + "&7/100  &8(" + Text.fmt(PetManager.progress(pd.xp)) + "%)");
            L.add("");
            for (Map.Entry<Stat, Double> en : PetManager.statsOf(pd).entrySet()) {
                Stat s = en.getKey();
                L.add("&7" + s.display + ": " + s.color + "+" + Text.fmt(Math.round(en.getValue() * 10) / 10.0) + (s.percent() ? "%" : ""));
            }
            L.add("");
            for (String ln : Text.wrap(t.perk, 36)) L.add("&6" + ln);
            L.add("");
            L.add(active ? "&aAusgewaehlt! &eKlicken zum Einpacken." : "&eKlicken zum Ausruesten!");
            L.add(pd.rarity.color + "&l" + pd.rarity.name() + " PET");
            ItemStack it = icon(t.icon, pd.rarity.color + t.display + " &7[Lv" + lvl + "]", L);
            if (active) it.editMeta(m -> m.setEnchantmentGlintOverride(true));
            inv.setItem(10 + (i / 7) * 9 + (i % 7), it);
        }
        if (d.pets.isEmpty()) inv.setItem(22, icon(Material.BARRIER, "&cKeine Pets", List.of(
                "&7Pets droppen von Minibossen", "&7oder koennen gecraftet werden.")));
        inv.setItem(49, back("Zum SkyBlock Menu"));
        fill(inv, 0, 54);
        p.openInventory(inv);
    }

    // ------------------------------------------------------------ Minibosse
    public static void bosses(SkyblockPlugin plugin, Player p) {
        Inventory inv = create(Menu.Kind.BOSSES, 54, "&8Minibosse");
        int i = 0;
        for (BossManager.BossDef b : plugin.bosses.all()) {
            Material mat = switch (b.type()) {
                case ZOMBIE -> Material.ZOMBIE_HEAD;
                case SKELETON -> Material.SKELETON_SKULL;
                case SPIDER -> Material.SPIDER_EYE;
                case ENDERMAN -> Material.ENDER_EYE;
                default -> Material.WITHER_SKELETON_SKULL;
            };
            List<String> L = new ArrayList<>();
            L.add("&7Leben: &c" + Text.fmt(b.health() * 5) + " &8(Skyblock-HP)");
            L.add("&7Spawnchance: &e" + Text.pct(b.spawnChance()) + " &7pro " + b.type().name().toLowerCase() + "-Spawn");
            L.add("&7Kampf-XP: &3" + Text.fmt(b.combatXp()));
            L.add("");
            L.add("&6Drops &8(Magic Find erhoeht die Chancen):");
            for (BossManager.Drop dr : b.drops()) {
                ItemDef def = plugin.registry.get(dr.item());
                String nm = def == null ? dr.item() : def.rarity().color + def.name();
                L.add("&8- " + nm + " &7" + Text.pct(dr.chance()));
            }
            inv.setItem(grid(i++), icon(mat, b.color() + "&l" + b.name(), L));
        }
        inv.setItem(49, back("Zum SkyBlock Menu"));
        fill(inv, 0, 54);
        p.openInventory(inv);
    }

    // ------------------------------------------------------------ Klicks
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof Menu m)) return;
        if (m.kind == Menu.Kind.CRAFT) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p) || e.getClickedInventory() != e.getInventory()) return;
        ItemStack cur = e.getCurrentItem();
        if (cur == null || cur.getType().isAir() || cur.getType() == Material.BLACK_STAINED_GLASS_PANE) return;
        int slot = e.getSlot();
        p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.4f, 1f);

        switch (m.kind) {
            case MAIN -> {
                switch (slot) {
                    case 20 -> later(() -> skills(plugin, p));
                    case 22 -> later(() -> enchants(plugin, p));
                    case 24 -> later(() -> abilities(plugin, p));
                    case 29 -> later(() -> CraftListener.open(plugin, p));
                    case 31 -> later(() -> pets(plugin, p));
                    case 33 -> later(() -> recipes(plugin, p, 0));
                    case 38 -> later(() -> bosses(plugin, p));
                    case 40 -> {
                        if (p.hasPermission("skyblocktest.admin")) later(() -> items(plugin, p));
                        else p.sendMessage(Text.c("&cNur fuer Admins."));
                    }
                    case 42 -> later(() -> settings(plugin, p));
                    case 49 -> later(p::closeInventory);
                    default -> { }
                }
            }
            case SKILLS -> {
                if (slot == 22) later(() -> main(plugin, p));
            }
            case ENCHANTS, ABILITIES -> {
                if (slot == 49) later(() -> main(plugin, p));
            }
            case SETTINGS -> {
                PlayerData d = plugin.data.get(p);
                switch (slot) {
                    case 11 -> { d.showDamage = !d.showDamage; renderSettings(e.getInventory(), d); }
                    case 13 -> { d.showActionBar = !d.showActionBar; renderSettings(e.getInventory(), d); }
                    case 15 -> { d.levelUpMessages = !d.levelUpMessages; renderSettings(e.getInventory(), d); }
                    case 22 -> later(() -> main(plugin, p));
                    default -> { }
                }
            }
            case RECIPES -> {
                if (slot == 49) later(() -> main(plugin, p));
                else if (slot == 45) later(() -> recipes(plugin, p, m.page - 1));
                else if (slot == 53) later(() -> recipes(plugin, p, m.page + 1));
                else if (slot < 45) later(() -> recipeView(plugin, p, m.page * 45 + slot, m.page));
            }
            case RECIPE_VIEW -> {
                if (slot == 40) later(() -> recipes(plugin, p, m.ref));
            }
            case PETS -> {
                if (slot == 49) later(() -> main(plugin, p));
                else {
                    int idx = fromGrid(slot);
                    if (idx >= 0) {
                        plugin.pets.toggle(p, idx);
                        later(() -> pets(plugin, p));
                    }
                }
            }
            case BOSSES -> {
                if (slot == 49) later(() -> main(plugin, p));
            }
            case CRAFT -> { }
            case ITEMS -> {
                if (slot == 49) later(() -> main(plugin, p));
                else if (slot == 45) later(() -> items(plugin, p, m.page - 1));
                else if (slot == 53) later(() -> items(plugin, p, m.page + 1));
                else if (slot < 45 && p.hasPermission("skyblocktest.admin")) p.getInventory().addItem(cur.clone());
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getInventory().getHolder() instanceof Menu m && m.kind != Menu.Kind.CRAFT) e.setCancelled(true);
    }
}
