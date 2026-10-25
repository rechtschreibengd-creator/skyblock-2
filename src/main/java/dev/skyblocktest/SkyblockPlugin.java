package dev.skyblocktest;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class SkyblockPlugin extends JavaPlugin {
    public PlayerDataManager data;
    public AbilityRegistry abilities;
    public ItemUtil items;
    public ItemRegistry registry;
    public SkillManager skills;
    public StatManager stats;
    public MenuItem menuItem;
    public PetManager pets;
    public BossManager bosses;
    public RecipeRegistry recipes;

    @Override
    public void onEnable() {
        getDataFolder().mkdirs();
        if (!new File(getDataFolder(), "items.yml").exists()) saveResource("items.yml", false);
        if (!new File(getDataFolder(), "bosses.yml").exists()) saveResource("bosses.yml", false);
        if (!new File(getDataFolder(), "recipes.yml").exists()) saveResource("recipes.yml", false);

        data = new PlayerDataManager(this);
        abilities = new AbilityRegistry(this);   // vor ItemUtil/ItemRegistry (Lore braucht Abilities)
        items = new ItemUtil(this);
        registry = new ItemRegistry(this);
        recipes = new RecipeRegistry(this);
        skills = new SkillManager(this);
        pets = new PetManager(this);
        stats = new StatManager(this);
        menuItem = new MenuItem(this);

        var pm = getServer().getPluginManager();
        pm.registerEvents(new PlayerListener(this), this);
        pm.registerEvents(new CombatListener(this), this);
        pm.registerEvents(new BlockListener(this), this);
        pm.registerEvents(new MiscSkillListener(this), this);
        pm.registerEvents(new AbilityListener(this), this);
        pm.registerEvents(new Menus(this), this);
        pm.registerEvents(menuItem, this);
        bosses = new BossManager(this);
        pm.registerEvents(bosses, this);
        pm.registerEvents(new ItemActionListener(this), this);
        pm.registerEvents(new CraftListener(this), this);

        SkyCommand cmd = new SkyCommand(this);
        for (String name : new String[]{"sb", "skills", "stats", "menu", "pets", "craft", "recipes"}) {
            var c = getCommand(name);
            if (c != null) {
                c.setExecutor(cmd);
                c.setTabCompleter(cmd);
            }
        }

        stats.start();
        Bukkit.getScheduler().runTaskTimer(this, () -> data.saveAll(), 6000L, 6000L);
        for (Player p : Bukkit.getOnlinePlayers()) {
            stats.apply(p);
            menuItem.give(p);
        }
        getLogger().info("SkyblockTest aktiviert.");
    }

    @Override
    public void onDisable() {
        if (data != null) data.saveAll();
        if (bosses != null) bosses.shutdown();
    }
}
