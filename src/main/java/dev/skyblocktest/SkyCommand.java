package dev.skyblocktest;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public final class SkyCommand implements CommandExecutor, TabCompleter {
    private static final List<String> SUB = List.of("help", "give", "items", "enchant", "enchants", "enchantall",
            "setskill", "addxp", "maxskills", "heal", "reload", "boss", "pet");
    private final SkyblockPlugin plugin;

    public SkyCommand(SkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender s, Command c, String label, String[] a) {
        String name = c.getName().toLowerCase();
        if (name.equals("pets")) {
            if (s instanceof Player p) Menus.pets(plugin, p);
            return true;
        }
        if (name.equals("craft")) {
            if (s instanceof Player p) CraftListener.open(plugin, p);
            return true;
        }
        if (name.equals("recipes")) {
            if (s instanceof Player p) Menus.recipes(plugin, p, 0);
            return true;
        }
        if (name.equals("menu")) {
            if (s instanceof Player p) {
                if (a.length > 0 && a[0].equalsIgnoreCase("get")) plugin.menuItem.give(p);
                else Menus.main(plugin, p);
            }
            return true;
        }
        if (name.equals("skills")) {
            if (s instanceof Player p) Menus.skills(plugin, p);
            return true;
        }
        if (name.equals("stats")) {
            if (s instanceof Player p) showStats(p);
            return true;
        }
        if (a.length == 0 || a[0].equalsIgnoreCase("help")) {
            help(s);
            return true;
        }
        if (!s.hasPermission("skyblocktest.admin")) {
            s.sendMessage(Text.c("&cKeine Berechtigung."));
            return true;
        }
        if (!(s instanceof Player p)) {
            s.sendMessage("Nur im Spiel nutzbar.");
            return true;
        }
        switch (a[0].toLowerCase()) {
            case "give" -> {
                if (a.length < 2) { p.sendMessage(Text.c("&e/sb give <item> [anzahl]")); return true; }
                ItemDef d = plugin.registry.get(a[1]);
                if (d == null) { p.sendMessage(Text.c("&cUnbekanntes Item.")); return true; }
                int amt = a.length > 2 ? parse(a[2], 1) : 1;
                p.getInventory().addItem(plugin.items.create(d, Math.max(1, Math.min(64, amt))));
                p.sendMessage(Text.c("&aErhalten: " + d.rarity().color + d.name()));
            }
            case "items" -> Menus.items(plugin, p);
            case "enchants" -> {
                for (SkyEnchant e : SkyEnchant.values())
                    p.sendMessage(Text.c("&9" + e.display + " &7(max " + e.maxLevel + ") - " + e.description));
            }
            case "enchant" -> {
                ItemStack hand = p.getInventory().getItemInMainHand();
                if (hand.getType().isAir()) { p.sendMessage(Text.c("&cHalte ein Item in der Hand.")); return true; }
                if (a.length < 3) { p.sendMessage(Text.c("&e/sb enchant <name> <level>")); return true; }
                SkyEnchant e = SkyEnchant.byName(a[1]);
                if (e == null) { p.sendMessage(Text.c("&cUnbekanntes Enchantment (/sb enchants).")); return true; }
                int lvl = Math.max(0, Math.min(e.maxLevel, parse(a[2], 1)));
                plugin.items.setEnchant(hand, e, lvl);
                plugin.stats.apply(p);
                p.sendMessage(Text.c("&a" + e.display + " " + lvl + " gesetzt."));
            }
            case "enchantall" -> {
                ItemStack hand = p.getInventory().getItemInMainHand();
                if (hand.getType().isAir()) { p.sendMessage(Text.c("&cHalte ein Item in der Hand.")); return true; }
                for (SkyEnchant e : SkyEnchant.values()) plugin.items.setEnchant(hand, e, e.maxLevel);
                plugin.stats.apply(p);
                p.sendMessage(Text.c("&aAlle Enchantments auf Maximum."));
            }
            case "setskill" -> {
                if (a.length < 3) { p.sendMessage(Text.c("&e/sb setskill <skill> <level 0-50>")); return true; }
                SkillType sk = SkillType.byName(a[1]);
                if (sk == null) { p.sendMessage(Text.c("&cUnbekannter Skill.")); return true; }
                int lvl = Math.max(0, Math.min(50, parse(a[2], 0)));
                plugin.data.get(p).xp.put(sk, SkillType.xpForLevel(lvl));
                plugin.stats.apply(p);
                p.sendMessage(Text.c("&a" + sk.display + " auf Level " + lvl + " gesetzt."));
            }
            case "addxp" -> {
                if (a.length < 3) { p.sendMessage(Text.c("&e/sb addxp <skill> <xp>")); return true; }
                SkillType sk = SkillType.byName(a[1]);
                if (sk == null) { p.sendMessage(Text.c("&cUnbekannter Skill.")); return true; }
                plugin.skills.addXp(p, sk, parse(a[2], 0));
            }
            case "maxskills" -> {
                for (SkillType sk : SkillType.values()) plugin.data.get(p).xp.put(sk, SkillType.xpForLevel(50));
                plugin.stats.apply(p);
                p.sendMessage(Text.c("&aAlle Skills auf Level 50."));
            }
            case "heal" -> {
                plugin.stats.apply(p);
                p.setHealth(p.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH).getValue());
                plugin.data.get(p).mana = plugin.data.get(p).stat(Stat.INTELLIGENCE);
                p.sendMessage(Text.c("&aLeben und Mana aufgefuellt."));
            }
            case "reload" -> {
                plugin.registry.load();
                plugin.recipes.load();
                plugin.bosses.load();
                p.sendMessage(Text.c("&aitems.yml, recipes.yml und bosses.yml neu geladen."));
            }
            case "boss" -> {
                if (a.length < 2) {
                    p.sendMessage(Text.c("&e/sb boss <id>  &7Verfuegbar: &f" + String.join(", ",
                            plugin.bosses.all().stream().map(BossManager.BossDef::id).toList())));
                    return true;
                }
                if (!plugin.bosses.spawn(a[1], p.getLocation().add(p.getLocation().getDirection().setY(0).normalize().multiply(4)), p))
                    p.sendMessage(Text.c("&cUnbekannter Boss."));
            }
            case "pet" -> {
                if (a.length < 3) { p.sendMessage(Text.c("&e/sb pet <typ> <rarity>")); return true; }
                PetType t = PetType.byId(a[1]);
                if (t == null) { p.sendMessage(Text.c("&cUnbekanntes Pet.")); return true; }
                try {
                    plugin.pets.add(p, t, Rarity.valueOf(a[2].toUpperCase()));
                } catch (IllegalArgumentException ex) {
                    p.sendMessage(Text.c("&cUnbekannte Rarity."));
                }
            }
            default -> help(p);
        }
        return true;
    }

    private void showStats(Player p) {
        PlayerData d = plugin.data.get(p);
        plugin.stats.apply(p);
        p.sendMessage(Text.c("&6&lDeine Stats"));
        for (Stat st : Stat.values()) {
            if (st == Stat.DAMAGE) continue;
            p.sendMessage(Text.c(st.color + st.symbol + " " + st.display + ": &f" + Text.fmt(d.stat(st)) + (st.percent() ? "%" : "")));
        }
    }

    private void help(CommandSender s) {
        s.sendMessage(Text.c("&6&lSkyblockTest &7- Befehle"));
        s.sendMessage(Text.c("&e/stats &7- Deine Stats"));
        s.sendMessage(Text.c("&e/menu &7- SkyBlock-Menue (/menu get = Stern erneut holen)"));
        s.sendMessage(Text.c("&e/skills &7- Skill-Menue"));
        s.sendMessage(Text.c("&e/sb give <item> [anzahl] &7- Item erhalten"));
        s.sendMessage(Text.c("&e/sb items &7- Item-Browser"));
        s.sendMessage(Text.c("&e/sb enchant <name> <level> &7- Enchantment auf Item in der Hand"));
        s.sendMessage(Text.c("&e/sb enchantall &7- Alle Enchantments auf Max"));
        s.sendMessage(Text.c("&e/sb enchants &7- Enchantment-Liste"));
        s.sendMessage(Text.c("&e/sb setskill <skill> <lvl> | addxp <skill> <xp> | maxskills"));
        s.sendMessage(Text.c("&e/pets &7- Pets  &e/craft &7- Werkbank  &e/recipes &7- Rezeptbuch"));
        s.sendMessage(Text.c("&e/sb boss <id> | pet <typ> <rarity> &7- Admin-Test"));
        s.sendMessage(Text.c("&e/sb heal | reload"));
    }

    private static int parse(String s, int def) {
        try {
            return (int) Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String label, String[] a) {
        if (!c.getName().equalsIgnoreCase("sb")) return List.of();
        List<String> out = new ArrayList<>();
        if (a.length == 1) out.addAll(SUB);
        else if (a.length == 2) {
            switch (a[0].toLowerCase()) {
                case "give" -> plugin.registry.all().forEach(d -> out.add(d.id()));
                case "boss" -> plugin.bosses.all().forEach(b -> out.add(b.id()));
                case "pet" -> Arrays.stream(PetType.values()).forEach(t -> out.add(t.name().toLowerCase()));
                case "enchant" -> Arrays.stream(SkyEnchant.values()).forEach(e -> out.add(e.name().toLowerCase()));
                case "setskill", "addxp" -> Arrays.stream(SkillType.values()).forEach(k -> out.add(k.name().toLowerCase()));
                default -> { }
            }
        }
        String last = a[a.length - 1].toLowerCase();
        return out.stream().filter(x -> x.toLowerCase().startsWith(last)).toList();
    }
}
