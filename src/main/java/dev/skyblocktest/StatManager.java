package dev.skyblocktest;

import org.bukkit.Bukkit;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.EnumMap;
import java.util.Map;

public final class StatManager {
    private final SkyblockPlugin plugin;

    public StatManager(SkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 10L);
    }

    public EnumMap<Stat, Double> compute(Player p, PlayerData d) {
        EnumMap<Stat, Double> s = new EnumMap<>(Stat.class);
        for (Stat st : Stat.values()) s.put(st, st.base);
        for (SkillType sk : SkillType.values()) {
            if (sk.stat != null) s.merge(sk.stat, sk.bonus(d.level(sk)), Double::sum);
        }
        PlayerInventory inv = p.getInventory();
        add(s, inv.getItemInMainHand(), true, false);
        ItemStack[] armor = inv.getArmorContents(); // 0 = Stiefel
        for (int i = 0; i < armor.length; i++) add(s, armor[i], false, i == 0);

        java.util.Set<String> seen = new java.util.HashSet<>();
        for (ItemStack it : inv.getStorageContents()) {
            if (it == null || !it.hasItemMeta() || !"TALISMAN".equals(plugin.items.type(it))) continue;
            if (seen.add(plugin.items.id(it))) add(s, it, false, false);
        }

        PetData pet = plugin.pets.active(d);
        if (pet != null) {
            for (Map.Entry<Stat, Double> en : PetManager.statsOf(pet).entrySet()) s.merge(en.getKey(), en.getValue(), Double::sum);
        }

        long now = System.currentTimeMillis();
        d.buffs.removeIf(b -> b.until() <= now);
        for (PlayerData.Buff b : d.buffs) s.merge(b.stat(), b.amount(), Double::sum);
        return s;
    }

    private void add(EnumMap<Stat, Double> s, ItemStack it, boolean hand, boolean boots) {
        if (it == null || it.getType().isAir() || !it.hasItemMeta()) return;
        String ty = plugin.items.type(it);
        if (hand && ("ARMOR".equals(ty) || "TALISMAN".equals(ty))) return;
        for (Stat st : Stat.values()) {
            double v = plugin.items.stat(it, st);
            if (v != 0) s.merge(st, v, Double::sum);
        }
        for (Map.Entry<SkyEnchant, Integer> en : plugin.items.enchants(it).entrySet()) {
            SkyEnchant e = en.getKey();
            if (e.stat == null) continue;
            switch (e.cat) {
                case ARMOR -> { if (hand) continue; }
                case BOOTS -> { if (!boots) continue; }
                case WEAPON, TOOL -> { if (!hand) continue; }
            }
            s.merge(e.stat, e.perLevel * en.getValue(), Double::sum);
        }
    }

    /** Uebertraegt die berechneten Stats auf Max-Health und Laufgeschwindigkeit. */
    public void apply(Player p) {
        PlayerData d = plugin.data.get(p);
        EnumMap<Stat, Double> st = compute(p, d);
        d.stats.clear();
        d.stats.putAll(st);

        AttributeInstance hp = p.getAttribute(Attribute.MAX_HEALTH);
        if (hp != null) {
            double target = Math.max(1.0, Math.min(1024.0, st.get(Stat.HEALTH) / 5.0));
            if (Math.abs(hp.getBaseValue() - target) > 0.01) hp.setBaseValue(target);
            if (p.getHealth() > target) p.setHealth(target);
            p.setHealthScale(20.0);
            p.setHealthScaled(true);
        }
        float walk = (float) Math.max(0.0, Math.min(1.0, 0.2 * st.get(Stat.SPEED) / 100.0));
        p.setWalkSpeed(walk);
    }

    private void tick() {
        long now = System.currentTimeMillis();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.isDead()) continue;
            PlayerData d = plugin.data.get(p);
            apply(p);

            double maxMana = d.stat(Stat.INTELLIGENCE);
            d.mana = Math.min(maxMana, d.mana + maxMana * 0.01);

            AttributeInstance hp = p.getAttribute(Attribute.MAX_HEALTH);
            double max = hp == null ? 20 : hp.getValue();
            if (p.getHealth() < max) p.setHealth(Math.min(max, p.getHealth() + max * 0.0075));

            ItemStack hand = p.getInventory().getItemInMainHand();
            int eff = plugin.items.enchant(hand, SkyEnchant.EFFICIENCY);
            if (eff > 0) {
                p.addPotionEffect(new PotionEffect(PotionEffectType.HASTE, 30, Math.min(eff, 5) - 1, true, false, false));
            }

            PetData ap = plugin.pets.active(d);
            StringBuilder sb = new StringBuilder();
            sb.append("&c❤ ").append((int) Math.ceil(p.getHealth() * 5)).append("/").append((int) d.stat(Stat.HEALTH));
            sb.append("   &a❈ ").append((int) d.stat(Stat.DEFENSE));
            sb.append("   &b✎ ").append((int) d.mana).append("/").append((int) maxMana);
            if (ap != null && PetType.byId(ap.type) != null)
                sb.append("   &7[").append(ap.rarity.color).append(PetType.byId(ap.type).display).append(" Lv").append(PetManager.levelOf(ap.xp)).append("&7]");
            if (d.xpMsgUntil > now) sb.append("   ").append(d.xpMsg);
            if (d.showActionBar) p.sendActionBar(Text.c(sb.toString()));
        }
    }
}
