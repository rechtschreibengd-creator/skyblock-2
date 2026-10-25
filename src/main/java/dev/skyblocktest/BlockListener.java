package dev.skyblocktest;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public final class BlockListener implements Listener {
    private static final Map<Material, Material> SMELT = Map.of(
            Material.RAW_IRON, Material.IRON_INGOT,
            Material.RAW_GOLD, Material.GOLD_INGOT,
            Material.RAW_COPPER, Material.COPPER_INGOT,
            Material.COBBLESTONE, Material.STONE,
            Material.COBBLED_DEEPSLATE, Material.DEEPSLATE,
            Material.SAND, Material.GLASS,
            Material.ANCIENT_DEBRIS, Material.NETHERITE_SCRAP);

    private static final Set<Material> CROPS = Set.of(Material.WHEAT, Material.CARROTS, Material.POTATOES,
            Material.BEETROOTS, Material.NETHER_WART, Material.COCOA);
    private static final Set<Material> BIG_CROPS = Set.of(Material.PUMPKIN, Material.MELON,
            Material.SUGAR_CANE, Material.CACTUS);

    private final SkyblockPlugin plugin;

    public BlockListener(SkyblockPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (p.getGameMode() != GameMode.SURVIVAL) return;
        Block b = e.getBlock();
        Material m = b.getType();

        SkillType skill;
        double xp;
        boolean crop = false;
        if (Tag.LOGS.isTagged(m)) {
            skill = SkillType.FORAGING;
            xp = 6;
        } else if (isGrownCrop(b)) {
            skill = SkillType.FARMING;
            xp = 4;
            crop = true;
        } else {
            xp = miningXp(m);
            if (xp <= 0) return;
            skill = SkillType.MINING;
        }

        ItemStack tool = p.getInventory().getItemInMainHand();
        ItemUtil iu = plugin.items;
        int smelt = iu.enchant(tool, SkyEnchant.SMELTING_TOUCH);
        int tele = iu.enchant(tool, SkyEnchant.TELEKINESIS);
        int fort = iu.enchant(tool, SkyEnchant.FORTUNE);
        int harv = iu.enchant(tool, SkyEnchant.HARVESTING);
        boolean mining = skill == SkillType.MINING;

        if (tele > 0 || (smelt > 0 && mining) || (fort > 0 && mining) || (harv > 0 && crop)) {
            double mult = 1 + (mining ? fort * 0.25 : 0) + (crop ? harv * 0.25 : 0);
            Collection<ItemStack> drops = b.getDrops(tool, p);
            e.setDropItems(false);
            for (ItemStack dr : drops) {
                ItemStack out = dr.clone();
                if (smelt > 0 && mining) {
                    Material s = SMELT.get(out.getType());
                    if (s != null) out.setType(s);
                }
                int n = prob(out.getAmount() * mult);
                while (n > 0) {
                    int a = Math.min(n, out.getMaxStackSize());
                    ItemStack part = out.clone();
                    part.setAmount(a);
                    n -= a;
                    if (tele > 0) {
                        p.getInventory().addItem(part).values()
                                .forEach(x -> b.getWorld().dropItemNaturally(p.getLocation(), x));
                    } else {
                        b.getWorld().dropItemNaturally(b.getLocation().add(0.5, 0.5, 0.5), part);
                    }
                }
            }
        }
        plugin.skills.addXp(p, skill, xp);
    }

    private static int prob(double x) {
        int n = (int) Math.floor(x);
        if (ThreadLocalRandom.current().nextDouble() < x - n) n++;
        return n;
    }

    private static boolean isGrownCrop(Block b) {
        Material m = b.getType();
        if (BIG_CROPS.contains(m)) return true;
        if (CROPS.contains(m) && b.getBlockData() instanceof Ageable a) return a.getAge() >= a.getMaximumAge();
        return false;
    }

    private static double miningXp(Material m) {
        String n = m.name();
        if (n.equals("ANCIENT_DEBRIS")) return 20;
        if (m == Material.OBSIDIAN) return 20;
        if (n.endsWith("_ORE")) {
            if (n.contains("DIAMOND")) return 10;
            if (n.contains("EMERALD")) return 12;
            if (n.contains("GOLD") || n.contains("LAPIS")) return 6;
            if (n.contains("COPPER")) return 3;
            if (n.contains("QUARTZ")) return 4;
            return 5;
        }
        return switch (m) {
            case STONE, COBBLESTONE, DEEPSLATE, COBBLED_DEEPSLATE, ANDESITE, DIORITE, GRANITE,
                 NETHERRACK, END_STONE, BLACKSTONE, BASALT, GLOWSTONE -> 1;
            default -> 0;
        };
    }
}
