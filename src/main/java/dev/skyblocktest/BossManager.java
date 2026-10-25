package dev.skyblocktest;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/** Minibosse (bosses.yml): spawnen selten aus normalen Zombies/Endermen/Skeletons/Spinnen oder per Beschwoerungs-Item. */
public final class BossManager implements Listener {
    public record Drop(String item, double chance, int min, int max, boolean announce) {}

    public record BossDef(String id, EntityType type, String name, String color, double health, double damage,
                          double spawnChance, double combatXp, List<Drop> drops) {}

    private static final class Live {
        final BossDef def;
        final BossBar bar;
        final Map<UUID, Double> dmg = new HashMap<>();
        long nextAbility = 0;
        boolean summoned = false;

        Live(BossDef def, BossBar bar) {
            this.def = def;
            this.bar = bar;
        }
    }

    private final SkyblockPlugin plugin;
    private final Map<String, BossDef> defs = new LinkedHashMap<>();
    private final Map<UUID, Live> live = new HashMap<>();

    public BossManager(SkyblockPlugin plugin) {
        this.plugin = plugin;
        load();
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 20L, 20L);
    }

    public void load() {
        defs.clear();
        File f = new File(plugin.getDataFolder(), "bosses.yml");
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        ConfigurationSection sec = y.getConfigurationSection("bosses");
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(id);
            if (s == null) continue;
            try {
                List<Drop> drops = new ArrayList<>();
                for (Map<?, ?> m : s.getMapList("drops")) {
                    String item = String.valueOf(m.get("item"));
                    double chance = Double.parseDouble(String.valueOf(m.get("chance")));
                    int min = m.get("min") == null ? 1 : Integer.parseInt(String.valueOf(m.get("min")));
                    int max = m.get("max") == null ? min : Integer.parseInt(String.valueOf(m.get("max")));
                    boolean ann = m.get("announce") != null && Boolean.parseBoolean(String.valueOf(m.get("announce")));
                    drops.add(new Drop(item.toLowerCase(), chance, min, max, ann));
                }
                defs.put(id.toLowerCase(), new BossDef(id.toLowerCase(),
                        EntityType.valueOf(s.getString("entity", "ZOMBIE").toUpperCase()),
                        s.getString("name", id), s.getString("color", "&c"),
                        s.getDouble("health", 300), s.getDouble("damage", 8),
                        s.getDouble("spawn_chance", 0.01), s.getDouble("combat_xp", 500), drops));
            } catch (Exception ex) {
                plugin.getLogger().warning("Boss '" + id + "' fehlerhaft: " + ex.getMessage());
            }
        }
        plugin.getLogger().info(defs.size() + " Bosse geladen.");
    }

    public Collection<BossDef> all() {
        return defs.values();
    }

    public BossDef get(String id) {
        return id == null ? null : defs.get(id.toLowerCase());
    }

    public boolean isBoss(Entity e) {
        return live.containsKey(e.getUniqueId());
    }

    // ------------------------------------------------------------ Spawnen
    public boolean spawn(String id, Location loc, Player summoner) {
        BossDef d = get(id);
        if (d == null) return false;
        LivingEntity e = (LivingEntity) loc.getWorld().spawnEntity(loc, d.type(), CreatureSpawnEvent.SpawnReason.CUSTOM);
        setup(e, d);
        if (summoner != null) {
            summoner.sendMessage(Text.c(d.color() + "&l" + d.name() + " &7ist erschienen!"));
        }
        return true;
    }

    private void setup(LivingEntity e, BossDef d) {
        AttributeInstance hp = e.getAttribute(Attribute.MAX_HEALTH);
        if (hp != null) hp.setBaseValue(Math.min(1024, d.health()));
        e.setHealth(Math.min(1024, d.health()));
        AttributeInstance ad = e.getAttribute(Attribute.ATTACK_DAMAGE);
        if (ad != null) ad.setBaseValue(d.damage());
        e.customName(Text.c(d.color() + "&l" + d.name()));
        e.setCustomNameVisible(true);
        e.setGlowing(true);
        e.setRemoveWhenFarAway(true);
        if (e instanceof Ageable a) a.setAdult();
        BossBar bar = Bukkit.createBossBar(org.bukkit.ChatColor.translateAlternateColorCodes('&', d.color() + d.name()),
                BarColor.RED, BarStyle.SEGMENTED_10);
        live.put(e.getUniqueId(), new Live(d, bar));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onNaturalSpawn(CreatureSpawnEvent e) {
        if (e.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;
        for (BossDef d : defs.values()) {
            if (d.type() != e.getEntityType()) continue;
            if (e.getEntity() instanceof Ageable a && !a.isAdult()) return;
            if (ThreadLocalRandom.current().nextDouble() < d.spawnChance()) {
                setup(e.getEntity(), d);
                final Location loc = e.getLocation();
                Bukkit.getScheduler().runTask(plugin, () -> {
                    for (Player p : loc.getWorld().getPlayers()) {
                        if (p.getLocation().distanceSquared(loc) < 60 * 60)
                            p.sendMessage(Text.c(d.color() + "&l" + d.name() + " &7ist in deiner Naehe erschienen!"));
                    }
                });
            }
            return;
        }
    }

    // ------------------------------------------------------------ Schaden tracken
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent e) {
        Live l = live.get(e.getEntity().getUniqueId());
        if (l == null) return;
        Player p = null;
        if (e.getDamager() instanceof Player direct) p = direct;
        else if (e.getDamager() instanceof Projectile pr && pr.getShooter() instanceof Player sh) p = sh;
        if (p != null) l.dmg.merge(p.getUniqueId(), e.getFinalDamage(), Double::sum);
    }

    // ------------------------------------------------------------ Tod + Drops
    @EventHandler
    public void onDeath(EntityDeathEvent e) {
        Live l = live.remove(e.getEntity().getUniqueId());
        if (l == null) return;
        l.bar.removeAll();
        e.getDrops().clear();
        e.setDroppedExp(300);

        UUID top = null;
        double best = 0, total = 0;
        for (Map.Entry<UUID, Double> en : l.dmg.entrySet()) {
            total += en.getValue();
            if (en.getValue() > best) {
                best = en.getValue();
                top = en.getKey();
            }
        }
        Player killer = e.getEntity().getKiller();
        Player winner = top == null ? killer : Bukkit.getPlayer(top);
        Location loc = e.getEntity().getLocation();

        for (Map.Entry<UUID, Double> en : l.dmg.entrySet()) {
            Player p = Bukkit.getPlayer(en.getKey());
            if (p == null || total <= 0) continue;
            double share = en.getValue() / total;
            if (share >= 0.1 || p.equals(winner)) {
                plugin.skills.addXp(p, SkillType.COMBAT, l.def.combatXp());
                p.sendMessage(Text.c("&7" + l.def.color() + l.def.name() + " &7besiegt! Dein Schadensanteil: &e" + Math.round(share * 100) + "%"));
            }
        }
        if (winner == null) return;

        double mf = plugin.data.get(winner).stat(Stat.MAGIC_FIND);
        double mult = 1.0 + mf / 100.0;
        for (Drop d : l.def.drops()) {
            double chance = Math.min(1.0, d.chance() * (d.chance() >= 1.0 ? 1.0 : mult));
            if (ThreadLocalRandom.current().nextDouble() >= chance) continue;
            ItemDef def = plugin.registry.get(d.item());
            if (def == null) continue;
            int amt = d.min() >= d.max() ? d.min() : ThreadLocalRandom.current().nextInt(d.min(), d.max() + 1);
            ItemStack stack = plugin.items.create(def, Math.max(1, Math.min(64, amt)));
            Item drop = loc.getWorld().dropItemNaturally(loc.clone().add(0, 0.5, 0), stack);
            drop.setOwner(winner.getUniqueId());
            drop.setGlowing(true);
            if (d.announce()) {
                winner.sendMessage(Text.c("&6&lSELTENER DROP! " + def.rarity().color + def.name()
                        + " &7(" + Text.pct(d.chance()) + ")" + (mf > 0 ? " &b(+" + Text.fmt(mf) + "% Magic Find)" : "")));
                winner.playSound(winner.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 0.6f);
                winner.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, drop.getLocation(), 30, 0.4, 0.6, 0.4, 0.2);
            }
        }
    }

    // ------------------------------------------------------------ Tick: Bossbar + Faehigkeiten
    private void tick() {
        long now = System.currentTimeMillis();
        for (Iterator<Map.Entry<UUID, Live>> it = live.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Live> en = it.next();
            Entity ent = Bukkit.getEntity(en.getKey());
            Live l = en.getValue();
            if (!(ent instanceof LivingEntity le) || !le.isValid() || le.isDead()) {
                if (ent == null || !ent.isValid()) {
                    l.bar.removeAll();
                    it.remove();
                }
                continue;
            }
            AttributeInstance a = le.getAttribute(Attribute.MAX_HEALTH);
            double max = a == null ? 100 : a.getValue();
            l.bar.setProgress(Math.max(0, Math.min(1, le.getHealth() / max)));
            for (Player p : le.getWorld().getPlayers()) {
                boolean near = p.getLocation().distanceSquared(le.getLocation()) < 40 * 40;
                if (near) l.bar.addPlayer(p);
                else l.bar.removePlayer(p);
            }
            if (now < l.nextAbility) continue;
            Player target = nearest(le, 16);
            if (target == null) continue;
            l.nextAbility = now + 7000 + ThreadLocalRandom.current().nextInt(3000);
            ability(le, l, target);
        }
    }

    private Player nearest(LivingEntity e, double r) {
        Player best = null;
        double bd = r * r;
        for (Player p : e.getWorld().getPlayers()) {
            if (p.getGameMode() == GameMode.SPECTATOR || p.isDead()) continue;
            double d = p.getLocation().distanceSquared(e.getLocation());
            if (d < bd) {
                bd = d;
                best = p;
            }
        }
        return best;
    }

    private void hit(LivingEntity src, Player p, double vanilla) {
        CombatListener.bypass = true;
        try {
            p.damage(vanilla, src);
        } finally {
            CombatListener.bypass = false;
        }
    }

    private void ability(LivingEntity e, Live l, Player target) {
        World w = e.getWorld();
        switch (l.def.type()) {
            case ZOMBIE -> { // Ground Pound + Verstaerkung bei 50% HP
                w.spawnParticle(Particle.EXPLOSION, e.getLocation(), 3, 1.5, 0.2, 1.5);
                w.playSound(e.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.6f);
                for (Player p : w.getPlayers()) {
                    if (p.getLocation().distanceSquared(e.getLocation()) > 25) continue;
                    hit(e, p, l.def.damage() * 1.2);
                    p.setVelocity(new Vector(0, 0.9, 0));
                }
                AttributeInstance a = e.getAttribute(Attribute.MAX_HEALTH);
                if (!l.summoned && a != null && e.getHealth() < a.getValue() * 0.5) {
                    l.summoned = true;
                    for (int i = 0; i < 2; i++) {
                        Zombie z = (Zombie) w.spawnEntity(e.getLocation().add(i * 2 - 1, 0, 0), EntityType.ZOMBIE);
                        z.setAdult();
                        z.customName(Text.c("&7Gerufener Untoter"));
                    }
                }
            }
            case ENDERMAN -> { // Teleport hinter das Ziel
                Location t = target.getLocation().clone();
                t.add(t.getDirection().setY(0).normalize().multiply(-2));
                w.spawnParticle(Particle.PORTAL, e.getLocation(), 40, 0.5, 1, 0.5);
                e.teleport(t);
                w.playSound(t, Sound.ENTITY_ENDERMAN_TELEPORT, 1f, 0.8f);
                hit(e, target, l.def.damage());
                target.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 60, 0));
            }
            case SKELETON -> { // Pfeil-Salve
                Location eye = e.getEyeLocation();
                Vector base = target.getEyeLocation().toVector().subtract(eye.toVector()).normalize();
                for (int i = -2; i <= 2; i++) {
                    Vector v = base.clone().rotateAroundY(Math.toRadians(i * 9)).multiply(1.8);
                    Arrow ar = w.spawnArrow(eye, v, 1.8f, 0f);
                    ar.setShooter(e);
                    ar.setDamage(l.def.damage() * 0.35);
                    ar.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
                    Bukkit.getScheduler().runTaskLater(plugin, ar::remove, 100L);
                }
                w.playSound(eye, Sound.ENTITY_SKELETON_SHOOT, 1f, 0.6f);
            }
            case SPIDER -> { // Giftstoss
                w.spawnParticle(Particle.ITEM_SLIME, e.getLocation(), 60, 2, 0.3, 2);
                w.playSound(e.getLocation(), Sound.ENTITY_SPIDER_AMBIENT, 1.5f, 0.5f);
                for (Player p : w.getPlayers()) {
                    if (p.getLocation().distanceSquared(e.getLocation()) > 36) continue;
                    p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 80, 1));
                    p.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 80, 2));
                    hit(e, p, l.def.damage() * 0.6);
                }
            }
            default -> { }
        }
    }

    public void shutdown() {
        for (Live l : live.values()) l.bar.removeAll();
        live.clear();
    }
}
