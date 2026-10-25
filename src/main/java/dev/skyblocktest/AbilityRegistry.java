package dev.skyblocktest;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.LinkedHashMap;
import java.util.Map;

public final class AbilityRegistry {
    private final Map<String, Ability> map = new LinkedHashMap<>();
    private final SkyblockPlugin plugin;

    public AbilityRegistry(SkyblockPlugin plugin) {
        this.plugin = plugin;
        reg("instant_transmission", "Instant Transmission",
                "Teleportiere dich 8 Bloecke nach vorne und erhalte 3 Sekunden Speed.", 50, 0, p -> {
                    Location t = ahead(p, 8);
                    if (t == null) return false;
                    p.teleport(t);
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 60, 1));
                    p.getWorld().playSound(t, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1f);
                    return true;
                });

        reg("wither_impact", "Wither Impact",
                "Teleportiere dich 10 Bloecke nach vorne, loese eine Explosion aus und erhalte Absorption.", 150, 20, p -> {
                    Location t = ahead(p, 10);
                    if (t == null) return false;
                    p.teleport(t);
                    World w = t.getWorld();
                    w.spawnParticle(Particle.EXPLOSION_EMITTER, t, 1);
                    w.playSound(t, Sound.ENTITY_GENERIC_EXPLODE, 1f, 1f);
                    for (Entity en : w.getNearbyEntities(t, 6, 6, 6)) {
                        if (en instanceof LivingEntity le && en != p && !(en instanceof ArmorStand)) {
                            damage(p, le, 40);
                        }
                    }
                    p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 100, 2));
                    return true;
                });

        reg("healing_circle", "Healing Circle",
                "Heilt dich und Spieler in 6 Bloecken Umkreis um 25% der maximalen Gesundheit.", 70, 40, p -> {
                    for (Player o : p.getWorld().getPlayers()) {
                        if (o.getLocation().distance(p.getLocation()) > 6) continue;
                        AttributeInstance a = o.getAttribute(Attribute.MAX_HEALTH);
                        if (a == null) continue;
                        o.setHealth(Math.min(a.getValue(), o.getHealth() + a.getValue() * 0.25));
                        o.getWorld().spawnParticle(Particle.HEART, o.getLocation().add(0, 2, 0), 5);
                    }
                    p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1.5f);
                    return true;
                });

        reg("ice_spray", "Ice Spray",
                "Verlangsamt und verletzt alle Gegner vor dir in 6 Bloecken.", 80, 60, p -> {
                    Vector look = p.getEyeLocation().getDirection();
                    for (Entity en : p.getNearbyEntities(6, 6, 6)) {
                        if (!(en instanceof LivingEntity le) || en instanceof ArmorStand) continue;
                        Vector to = en.getLocation().toVector().subtract(p.getLocation().toVector()).normalize();
                        if (to.dot(look) < 0.4) continue;
                        le.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 3));
                        damage(p, le, 8);
                        le.getWorld().spawnParticle(Particle.SNOWFLAKE, le.getLocation().add(0, 1, 0), 20, 0.4, 0.5, 0.4, 0.02);
                    }
                    p.playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK, 1f, 0.8f);
                    return true;
                });

        reg("giant_slam", "Giant's Slam",
                "Schlaegt auf den Boden, schleudert Gegner in 5 Bloecken hoch und verletzt sie.", 100, 80, p -> {
                    for (Entity en : p.getNearbyEntities(5, 3, 5)) {
                        if (!(en instanceof LivingEntity le) || en instanceof ArmorStand) continue;
                        damage(p, le, 15);
                        le.setVelocity(new Vector(0, 0.8, 0));
                    }
                    p.getWorld().spawnParticle(Particle.CLOUD, p.getLocation(), 40, 2, 0.2, 2, 0.05);
                    p.playSound(p.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.7f);
                    return true;
                });

        reg("lightning_strike", "Thunder Strike",
                "Beschwoert einen Blitz am anvisierten Punkt (bis 30 Bloecke) und verletzt Gegner im Umkreis.", 120, 40, p -> {
                    Location t = target(p, 30);
                    World w = t.getWorld();
                    w.strikeLightningEffect(t);
                    for (Entity en : w.getNearbyEntities(t, 4, 4, 4)) {
                        if (en instanceof LivingEntity le && en != p && !(en instanceof ArmorStand)) damage(p, le, 25);
                    }
                    return true;
                });

        reg("meteor_strike", "Meteor Strike",
                "Ein Meteor schlaegt am anvisierten Punkt (bis 25 Bloecke) ein und verursacht grossen Flaechenschaden.", 200, 100, p -> {
                    Location t = target(p, 25);
                    World w = t.getWorld();
                    w.spawnParticle(Particle.EXPLOSION_EMITTER, t, 2);
                    w.spawnParticle(Particle.FLAME, t, 80, 2.5, 0.5, 2.5, 0.05);
                    w.playSound(t, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.6f);
                    for (Entity en : w.getNearbyEntities(t, 6, 5, 6)) {
                        if (en instanceof LivingEntity le && en != p && !(en instanceof ArmorStand)) damage(p, le, 60);
                    }
                    return true;
                });

        reg("grappling_hook", "Grapple",
                "Schleudert dich in Blickrichtung nach vorne.", 0, 40, p -> {
                    p.setVelocity(p.getEyeLocation().getDirection().multiply(1.7).setY(Math.max(0.5, p.getEyeLocation().getDirection().getY() * 1.7)));
                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1f, 0.8f);
                    return true;
                });

        reg("leap", "Leap",
                "Springt weit nach vorne. Du erhaeltst kurz Federfall.", 40, 60, p -> {
                    Vector dir = p.getEyeLocation().getDirection().setY(0).normalize().multiply(1.3);
                    p.setVelocity(dir.setY(0.9));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 80, 0));
                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1f, 1f);
                    return true;
                });

        reg("war_cry", "War Cry",
                "Erhalte fuer 10 Sekunden +60 Strength und +25% Crit Damage.", 100, 300, p -> {
                    PlayerData d = plugin.data.get(p);
                    long until = System.currentTimeMillis() + 10000L;
                    d.buffs.add(new PlayerData.Buff(Stat.STRENGTH, 60, until));
                    d.buffs.add(new PlayerData.Buff(Stat.CRIT_DAMAGE, 25, until));
                    plugin.stats.apply(p);
                    p.getWorld().playSound(p.getLocation(), Sound.EVENT_RAID_HORN, 1f, 1.2f);
                    p.getWorld().spawnParticle(Particle.ANGRY_VILLAGER, p.getLocation().add(0, 2, 0), 8, 0.4, 0.3, 0.4);
                    return true;
                });

        reg("life_surge", "Life Surge",
                "Heilt dich sofort um 40% deiner maximalen Gesundheit und gibt kurz Regeneration.", 90, 200, p -> {
                    AttributeInstance a = p.getAttribute(Attribute.MAX_HEALTH);
                    if (a == null) return false;
                    p.setHealth(Math.min(a.getValue(), p.getHealth() + a.getValue() * 0.4));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 100, 1));
                    p.getWorld().spawnParticle(Particle.HEART, p.getLocation().add(0, 2, 0), 8, 0.4, 0.3, 0.4);
                    return true;
                });

        reg("wither_shield", "Wither Shield",
                "Gewaehrt 6 Sekunden Absorption und Widerstand.", 150, 100, p -> {
                    p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 120, 3));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.RESISTANCE, 120, 1));
                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.5f, 1.8f);
                    return true;
                });

        reg("gravity_well", "Gravity Well",
                "Zieht alle Gegner um den anvisierten Punkt 3 Sekunden lang zusammen und verletzt sie.", 150, 160, p -> {
                    final Location t = target(p, 20);
                    final int[] n = {0};
                    Bukkit.getScheduler().runTaskTimer(plugin, task -> {
                        World w = t.getWorld();
                        w.spawnParticle(Particle.PORTAL, t, 40, 1.5, 1.5, 1.5, 0.2);
                        for (Entity en : w.getNearbyEntities(t, 8, 6, 8)) {
                            if (!(en instanceof LivingEntity le) || en == p || en instanceof ArmorStand) continue;
                            Vector pull = t.toVector().subtract(le.getLocation().toVector()).multiply(0.25);
                            le.setVelocity(pull.setY(Math.max(0.1, pull.getY())));
                            if (n[0] % 2 == 0) damage(p, le, 3);
                        }
                        if (++n[0] >= 6) task.cancel();
                    }, 0L, 10L);
                    return true;
                });

        reg("mana_flux", "Mana Flux",
                "Stellt sofort 30% deines maximalen Manas wieder her.", 0, 400, p -> {
                    PlayerData d = plugin.data.get(p);
                    double max = d.stat(Stat.INTELLIGENCE);
                    d.mana = Math.min(max, d.mana + max * 0.3);
                    p.getWorld().spawnParticle(Particle.ENCHANT, p.getLocation().add(0, 1.5, 0), 40, 0.5, 0.5, 0.5, 0.5);
                    return true;
                });

        reg("shadow_step", "Shadow Step",
                "Teleportiere dich 12 Bloecke nach vorne.", 60, 40, p -> {
                    Location t = ahead(p, 12);
                    if (t == null) return false;
                    p.getWorld().spawnParticle(Particle.LARGE_SMOKE, p.getLocation().add(0, 1, 0), 25, 0.3, 0.6, 0.3, 0.02);
                    p.teleport(t);
                    p.getWorld().playSound(t, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 1.5f);
                    return true;
                });

        reg("venom_burst", "Venom Burst",
                "Vergiftet und verletzt alle Gegner in 7 Bloecken.", 90, 100, p -> {
                    for (Entity en : p.getNearbyEntities(7, 4, 7)) {
                        if (!(en instanceof LivingEntity le) || en instanceof ArmorStand) continue;
                        le.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 100, 1));
                        damage(p, le, 10);
                    }
                    p.getWorld().spawnParticle(Particle.ITEM_SLIME, p.getLocation().add(0, 1, 0), 80, 3, 0.5, 3);
                    p.playSound(p.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 1f, 0.6f);
                    return true;
                });

        reg("soul_drain", "Soul Drain",
                "Entzieht Gegnern in 6 Bloecken Leben und heilt dich dabei.", 110, 120, p -> {
                    AttributeInstance a = p.getAttribute(Attribute.MAX_HEALTH);
                    double heal = 0;
                    for (Entity en : p.getNearbyEntities(6, 4, 6)) {
                        if (!(en instanceof LivingEntity le) || en instanceof ArmorStand) continue;
                        damage(p, le, 12);
                        heal += a == null ? 0 : a.getValue() * 0.1;
                        le.getWorld().spawnParticle(Particle.SOUL, le.getLocation().add(0, 1, 0), 10, 0.3, 0.5, 0.3, 0.02);
                    }
                    if (a != null) p.setHealth(Math.min(a.getValue(), p.getHealth() + Math.min(heal, a.getValue() * 0.4)));
                    return true;
                });
    }

    private void reg(String id, String name, String desc, int mana, int cd, Ability.Action a) {
        map.put(id, new Ability(id, name, desc, mana, cd, a));
    }

    public Ability get(String id) {
        return id == null ? null : map.get(id);
    }

    public Map<String, Ability> all() {
        return map;
    }

    /** Schaden, der NICHT durch die Skyblock-Schadensformel laeuft. */
    private static void damage(Player src, LivingEntity target, double vanillaDamage) {
        CombatListener.bypass = true;
        try {
            target.damage(vanillaDamage, src);
        } finally {
            CombatListener.bypass = false;
        }
    }

    private static Location target(Player p, double range) {
        Location eye = p.getEyeLocation();
        var r = p.getWorld().rayTraceBlocks(eye, eye.getDirection(), range, FluidCollisionMode.NEVER, true);
        if (r != null && r.getHitPosition() != null) return r.getHitPosition().toLocation(p.getWorld());
        return eye.clone().add(eye.getDirection().multiply(range));
    }

    private static Location ahead(Player p, double range) {
        Location base = p.getLocation();
        Vector dir = p.getEyeLocation().getDirection().normalize();
        Location last = null;
        for (double d = 0.5; d <= range; d += 0.5) {
            Location l = base.clone().add(dir.clone().multiply(d));
            if (l.getBlock().isPassable() && l.clone().add(0, 1, 0).getBlock().isPassable()) last = l;
            else break;
        }
        return last;
    }
}
