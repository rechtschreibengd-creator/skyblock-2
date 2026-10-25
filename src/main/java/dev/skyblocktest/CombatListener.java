package dev.skyblocktest;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Schadensmodell (1 Skyblock-HP = 0.2 Vanilla-HP):
 * Damage = (5 + Waffe + Strength/5) * (1 + Strength/100) * (1 + Enchants) * (1 + CombatLvl*1%) * (1 + CritDmg/100 bei Crit)
 * Eingehender Schaden wird mit 100/(100+Defense) multipliziert.
 */
public final class CombatListener implements Listener {
    public static boolean bypass = false;
    private boolean thorning = false;
    private final SkyblockPlugin plugin;

    public CombatListener(SkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onOutgoing(EntityDamageByEntityEvent e) {
        if (bypass || thorning) return;
        if (e.getCause() == DamageCause.ENTITY_SWEEP_ATTACK || e.getCause() == DamageCause.THORNS) return;

        Player atk = null;
        boolean ranged = false;
        if (e.getDamager() instanceof Player direct) {
            atk = direct;
        } else if (e.getDamager() instanceof Projectile pr && pr.getShooter() instanceof Player shooter) {
            atk = shooter;
            ranged = true;
        }
        if (atk == null || !(e.getEntity() instanceof LivingEntity target) || target instanceof ArmorStand) return;

        PlayerData d = plugin.data.get(atk);
        EnumMap<Stat, Double> st = plugin.stats.compute(atk, d);
        ItemStack hand = atk.getInventory().getItemInMainHand();
        ItemUtil iu = plugin.items;

        double str = st.get(Stat.STRENGTH);
        double base = 5 + st.get(Stat.DAMAGE) + Math.floor(str / 5.0);
        double add = 0;

        AttributeInstance ta = target.getAttribute(Attribute.MAX_HEALTH);
        double tmax = ta == null ? Math.max(1, target.getHealth()) : ta.getValue();
        double missingPct = Math.max(0, (1 - target.getHealth() / tmax) * 100.0);

        if (!ranged) add += 0.05 * iu.enchant(hand, SkyEnchant.SHARPNESS);
        else add += 0.08 * iu.enchant(hand, SkyEnchant.POWER);

        add += 0.002 * iu.enchant(hand, SkyEnchant.EXECUTE) * missingPct;

        int fs = iu.enchant(hand, SkyEnchant.FIRST_STRIKE);
        if (fs > 0 && d.firstStrike.add(target.getUniqueId())) add += 0.25 * fs;
        if (d.firstStrike.size() > 300) d.firstStrike.clear();

        int gk = iu.enchant(hand, SkyEnchant.GIANT_KILLER);
        if (gk > 0) {
            AttributeInstance ma = atk.getAttribute(Attribute.MAX_HEALTH);
            double myMax = ma == null ? 20 : ma.getValue();
            double diff = (tmax - myMax) / myMax * 100.0;
            if (diff > 0) add += Math.min(0.05 * gk, 0.001 * gk * diff);
        }

        int es = iu.enchant(hand, SkyEnchant.ENDER_SLAYER);
        if (es > 0 && (target instanceof Enderman || target instanceof EnderDragon || target instanceof Endermite)) {
            add += 0.12 * es;
        }

        if (plugin.pets.hasPerk(d, PetType.ENDERMAN)
                && (target instanceof Enderman || target instanceof EnderDragon || target instanceof Endermite)) add += 0.25;

        double dmg = base * (1 + str / 100.0) * (1 + add) * (1 + d.level(SkillType.COMBAT) * 0.01);
        boolean crit = ThreadLocalRandom.current().nextDouble() * 100.0 < st.get(Stat.CRIT_CHANCE);
        if (crit) dmg *= 1 + st.get(Stat.CRIT_DAMAGE) / 100.0;

        int vamp = iu.enchant(hand, SkyEnchant.VAMPIRISM);
        if (vamp > 0) {
            AttributeInstance ma = atk.getAttribute(Attribute.MAX_HEALTH);
            if (ma != null) heal(atk, (ma.getValue() - atk.getHealth()) * 0.02 * vamp);
        }
        int ls = iu.enchant(hand, SkyEnchant.LIFE_STEAL);
        if (ls > 0) {
            AttributeInstance ma = atk.getAttribute(Attribute.MAX_HEALTH);
            if (ma != null) heal(atk, ma.getValue() * 0.005 * ls);
        }

        if (d.showDamage) indicator(target.getLocation(), dmg, crit);
        e.setDamage(dmg / 5.0);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onIncoming(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player p)) return;
        DamageCause c = e.getCause();
        if (c == DamageCause.VOID || c == DamageCause.KILL || c == DamageCause.SUICIDE
                || c == DamageCause.WORLD_BORDER || c == DamageCause.STARVATION) return;
        PlayerData d = plugin.data.get(p);
        double def = d.stat(Stat.DEFENSE);
        e.setDamage(e.getDamage() * 100.0 / (100.0 + def));

        long nowMs = System.currentTimeMillis();
        if (plugin.pets.hasPerk(d, PetType.PHOENIX) && nowMs > d.phoenixUntil && p.getHealth() - e.getFinalDamage() <= 0) {
            e.setCancelled(true);
            AttributeInstance pm = p.getAttribute(Attribute.MAX_HEALTH);
            p.setHealth(pm == null ? 10 : Math.max(1, pm.getValue() * 0.5));
            d.phoenixUntil = nowMs + 300000L;
            p.getWorld().spawnParticle(org.bukkit.Particle.FLAME, p.getLocation().add(0, 1, 0), 80, 0.6, 1, 0.6, 0.1);
            p.getWorld().playSound(p.getLocation(), org.bukkit.Sound.ITEM_TOTEM_USE, 1f, 1f);
            p.sendMessage(Text.c("&6Dein Phoenix hat dich wiederbelebt!"));
            return;
        }

        if (e instanceof EntityDamageByEntityEvent ev && !thorning && ev.getDamager() instanceof LivingEntity le) {
            int th = 0;
            for (ItemStack a : p.getInventory().getArmorContents()) th += plugin.items.enchant(a, SkyEnchant.THORNS);
            if (th > 0) {
                thorning = true;
                try {
                    le.damage(Math.max(1.0, e.getDamage() * 0.2 * th), p);
                } finally {
                    thorning = false;
                }
            }
        }
    }

    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        LivingEntity dead = e.getEntity();
        Player killer = dead.getKiller();
        if (killer != null && !(dead instanceof Player)) {
            AttributeInstance a = dead.getAttribute(Attribute.MAX_HEALTH);
            double hp = a == null ? 10 : a.getValue();
            plugin.skills.addXp(killer, SkillType.COMBAT, Math.max(1, hp * 0.5));
        }
        for (Player p : Bukkit.getOnlinePlayers()) plugin.data.get(p).firstStrike.remove(dead.getUniqueId());
    }

    private void heal(Player p, double amount) {
        AttributeInstance a = p.getAttribute(Attribute.MAX_HEALTH);
        if (a == null || amount <= 0) return;
        p.setHealth(Math.min(a.getValue(), p.getHealth() + amount));
    }

    private void indicator(Location loc, double dmg, boolean crit) {
        ThreadLocalRandom r = ThreadLocalRandom.current();
        Location l = loc.clone().add(r.nextDouble(-0.5, 0.5), 1.4 + r.nextDouble(0.4), r.nextDouble(-0.5, 0.5));
        String txt = crit ? "&f✧&c" + Math.round(dmg) + "&f✧" : "&7" + Math.round(dmg);
        TextDisplay td = loc.getWorld().spawn(l, TextDisplay.class, t -> {
            t.text(Text.c(txt));
            t.setBillboard(Display.Billboard.CENTER);
            t.setPersistent(false);
        });
        Bukkit.getScheduler().runTaskLater(plugin, td::remove, 25L);
    }
}
