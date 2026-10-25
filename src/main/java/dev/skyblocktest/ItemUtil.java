package dev.skyblocktest;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

/** Alle Item-Daten (Stats, Enchants, Ability) liegen im PersistentDataContainer. */
public final class ItemUtil {
    private final SkyblockPlugin plugin;
    private final NamespacedKey ID, RARITY, TYPE, ABILITY, DESC, PET, SUMMON, BOOK;

    public ItemUtil(SkyblockPlugin plugin) {
        this.plugin = plugin;
        ID = new NamespacedKey(plugin, "id");
        RARITY = new NamespacedKey(plugin, "rarity");
        TYPE = new NamespacedKey(plugin, "type");
        ABILITY = new NamespacedKey(plugin, "ability");
        DESC = new NamespacedKey(plugin, "desc");
        PET = new NamespacedKey(plugin, "pet");
        SUMMON = new NamespacedKey(plugin, "summon");
        BOOK = new NamespacedKey(plugin, "book");
    }

    private NamespacedKey sk(Stat s) {
        return new NamespacedKey(plugin, "s_" + s.name().toLowerCase());
    }

    private NamespacedKey ek(SkyEnchant e) {
        return new NamespacedKey(plugin, "e_" + e.name().toLowerCase());
    }

    private PersistentDataContainer pdc(ItemStack it) {
        if (it == null || it.getType().isAir() || !it.hasItemMeta()) return null;
        return it.getItemMeta().getPersistentDataContainer();
    }

    public ItemStack create(ItemDef d, int amount) {
        ItemStack it = new ItemStack(d.material(), amount);
        ItemMeta m = it.getItemMeta();
        PersistentDataContainer c = m.getPersistentDataContainer();
        c.set(ID, PersistentDataType.STRING, d.id());
        c.set(RARITY, PersistentDataType.STRING, d.rarity().name());
        c.set(TYPE, PersistentDataType.STRING, d.type());
        if (d.ability() != null) c.set(ABILITY, PersistentDataType.STRING, d.ability());
        if (!d.description().isEmpty()) c.set(DESC, PersistentDataType.STRING, String.join("|", d.description()));
        if (d.pet() != null) c.set(PET, PersistentDataType.STRING, d.pet());
        if (d.summon() != null) c.set(SUMMON, PersistentDataType.STRING, d.summon());
        if (d.book() != null) c.set(BOOK, PersistentDataType.STRING, d.book());
        if (d.glow()) m.setEnchantmentGlintOverride(true);
        d.stats().forEach((s, v) -> c.set(sk(s), PersistentDataType.DOUBLE, v));
        d.enchants().forEach((e, l) -> c.set(ek(e), PersistentDataType.INTEGER, l));
        m.displayName(Text.c(d.rarity().color + d.name()));
        m.setUnbreakable(true);
        m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE);
        it.setItemMeta(m);
        rebuildLore(it);
        return it;
    }

    public String id(ItemStack it) {
        PersistentDataContainer c = pdc(it);
        return c == null ? null : c.get(ID, PersistentDataType.STRING);
    }

    public String type(ItemStack it) {
        PersistentDataContainer c = pdc(it);
        return c == null ? null : c.get(TYPE, PersistentDataType.STRING);
    }

    private String str(ItemStack it, NamespacedKey k) {
        PersistentDataContainer c = pdc(it);
        return c == null ? null : c.get(k, PersistentDataType.STRING);
    }

    public String pet(ItemStack it) { return str(it, PET); }

    public String summon(ItemStack it) { return str(it, SUMMON); }

    public String book(ItemStack it) { return str(it, BOOK); }

    public String ability(ItemStack it) {
        PersistentDataContainer c = pdc(it);
        return c == null ? null : c.get(ABILITY, PersistentDataType.STRING);
    }

    public double stat(ItemStack it, Stat s) {
        PersistentDataContainer c = pdc(it);
        if (c == null) return 0;
        Double v = c.get(sk(s), PersistentDataType.DOUBLE);
        return v == null ? 0 : v;
    }

    public int enchant(ItemStack it, SkyEnchant e) {
        PersistentDataContainer c = pdc(it);
        if (c == null) return 0;
        Integer v = c.get(ek(e), PersistentDataType.INTEGER);
        return v == null ? 0 : v;
    }

    public Map<SkyEnchant, Integer> enchants(ItemStack it) {
        Map<SkyEnchant, Integer> out = new EnumMap<>(SkyEnchant.class);
        PersistentDataContainer c = pdc(it);
        if (c == null) return out;
        for (SkyEnchant e : SkyEnchant.values()) {
            Integer v = c.get(ek(e), PersistentDataType.INTEGER);
            if (v != null && v > 0) out.put(e, v);
        }
        return out;
    }

    public void setEnchant(ItemStack it, SkyEnchant e, int level) {
        it.editMeta(m -> {
            PersistentDataContainer c = m.getPersistentDataContainer();
            if (level <= 0) c.remove(ek(e));
            else c.set(ek(e), PersistentDataType.INTEGER, level);
        });
        rebuildLore(it);
    }

    public void rebuildLore(ItemStack it) {
        it.editMeta(m -> {
            PersistentDataContainer c = m.getPersistentDataContainer();
            List<String> L = new ArrayList<>();

            boolean any = false;
            for (Stat s : Stat.values()) {
                Double v = c.get(sk(s), PersistentDataType.DOUBLE);
                if (v != null && v != 0) {
                    L.add("&7" + s.display + ": " + s.color + (v > 0 ? "+" : "") + Text.fmt(v) + (s.percent() ? "%" : ""));
                    any = true;
                }
            }
            if (any) L.add("");

            List<String> ens = new ArrayList<>();
            for (SkyEnchant e : SkyEnchant.values()) {
                Integer l = c.get(ek(e), PersistentDataType.INTEGER);
                if (l != null && l > 0) ens.add("&9" + e.display + " " + Text.roman(l));
            }
            if (!ens.isEmpty()) {
                for (int i = 0; i < ens.size(); i += 3)
                    L.add(String.join("&7, ", ens.subList(i, Math.min(i + 3, ens.size()))));
                L.add("");
            }

            Ability a = plugin.abilities.get(c.get(ABILITY, PersistentDataType.STRING));
            if (a != null) {
                L.add("&6Ability: " + a.name + " &e&lRECHTSKLICK");
                for (String ln : Text.wrap(a.description, 38)) L.add("&7" + ln);
                L.add("&8Mana Cost: &3" + a.mana);
                if (a.cooldownTicks > 0) L.add("&8Cooldown: &a" + (a.cooldownTicks / 20) + "s");
                L.add("");
            }

            if (c.has(PET, PersistentDataType.STRING)) L.add("&eRechtsklick, um dieses Pet freizuschalten!");
            if (c.has(SUMMON, PersistentDataType.STRING)) L.add("&eRechtsklick, um den Boss zu beschwoeren!");
            if (c.has(BOOK, PersistentDataType.STRING)) {
                L.add("&7Klicke dieses Buch im Inventar auf ein");
                L.add("&7passendes Item, um es anzuwenden.");
            }
            if (c.has(PET, PersistentDataType.STRING) || c.has(SUMMON, PersistentDataType.STRING)
                    || c.has(BOOK, PersistentDataType.STRING)) L.add("");

            String desc = c.get(DESC, PersistentDataType.STRING);
            if (desc != null) {
                for (String ln : desc.split("\\|")) L.add("&7" + ln);
                L.add("");
            }

            String rs = c.get(RARITY, PersistentDataType.STRING);
            Rarity r = rs == null ? Rarity.COMMON : Rarity.valueOf(rs);
            String type = c.get(TYPE, PersistentDataType.STRING);
            L.add(r.color + "&l" + r.name() + (type == null || type.equals("OTHER") ? "" : " " + type));

            m.lore(L.stream().map(Text::c).toList());
        });
    }
}
