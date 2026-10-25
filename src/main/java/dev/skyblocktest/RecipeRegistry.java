package dev.skyblocktest;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.util.*;

/**
 * Rezepte aus recipes.yml. Zutaten: lowercase = eigenes Item (items.yml-ID), UPPERCASE = Vanilla-Material
 * (nur normale Items ohne Plugin-ID). "TOKEN*N" = N Stueck pro Slot (formlos: insgesamt).
 */
public final class RecipeRegistry {
    public record Ing(String key, int amount) {}

    public static final class Recipe {
        public String id, result;
        public int amount = 1;
        public boolean shapeless;
        public Ing[][] grid;       // geformt: [Zeilen][Spalten], null = leer
        public List<Ing> list;     // formlos
    }

    private final SkyblockPlugin plugin;
    private final Map<String, Recipe> recipes = new LinkedHashMap<>();

    public RecipeRegistry(SkyblockPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public Collection<Recipe> all() {
        return recipes.values();
    }

    public List<Recipe> list() {
        return new ArrayList<>(recipes.values());
    }

    public void load() {
        recipes.clear();
        File f = new File(plugin.getDataFolder(), "recipes.yml");
        YamlConfiguration y = YamlConfiguration.loadConfiguration(f);
        ConfigurationSection sec = y.getConfigurationSection("recipes");
        if (sec == null) return;
        for (String id : sec.getKeys(false)) {
            ConfigurationSection s = sec.getConfigurationSection(id);
            if (s == null) continue;
            try {
                Recipe r = new Recipe();
                r.id = id;
                r.result = s.getString("result", id);
                r.amount = Math.max(1, s.getInt("amount", 1));
                if (!validKey(r.result)) throw new IllegalArgumentException("Ergebnis unbekannt: " + r.result);
                r.shapeless = s.getBoolean("shapeless", false);
                if (r.shapeless) {
                    r.list = new ArrayList<>();
                    for (String t : s.getStringList("ingredients")) r.list.add(parse(t));
                    if (r.list.isEmpty() || r.list.size() > 9) throw new IllegalArgumentException("1-9 Zutaten");
                } else {
                    List<String> shape = s.getStringList("shape");
                    ConfigurationSection ing = s.getConfigurationSection("ingredients");
                    if (shape.isEmpty() || shape.size() > 3 || ing == null) throw new IllegalArgumentException("shape/ingredients fehlt");
                    int cols = 0;
                    for (String row : shape) cols = Math.max(cols, row.length());
                    if (cols > 3 || cols == 0) throw new IllegalArgumentException("max 3 Spalten");
                    r.grid = new Ing[shape.size()][cols];
                    for (int i = 0; i < shape.size(); i++) {
                        for (int j = 0; j < shape.get(i).length(); j++) {
                            char c = shape.get(i).charAt(j);
                            if (c == ' ' || c == '.') continue;
                            String tok = ing.getString(String.valueOf(c));
                            if (tok == null) throw new IllegalArgumentException("Zeichen '" + c + "' nicht definiert");
                            r.grid[i][j] = parse(tok);
                        }
                    }
                    r.grid = trim(r.grid);
                }
                recipes.put(id.toLowerCase(), r);
            } catch (Exception ex) {
                plugin.getLogger().warning("Rezept '" + id + "' fehlerhaft: " + ex.getMessage());
            }
        }
        plugin.getLogger().info(recipes.size() + " Rezepte geladen.");
    }

    /** Entfernt leere Randzeilen/-spalten (wie bei Vanilla-Rezepten). */
    private static Ing[][] trim(Ing[][] g) {
        int r0 = g.length, r1 = -1, c0 = g[0].length, c1 = -1;
        for (int i = 0; i < g.length; i++) {
            for (int j = 0; j < g[0].length; j++) {
                if (g[i][j] == null) continue;
                r0 = Math.min(r0, i);
                r1 = Math.max(r1, i);
                c0 = Math.min(c0, j);
                c1 = Math.max(c1, j);
            }
        }
        if (r1 < 0) throw new IllegalArgumentException("Rezept ist leer");
        Ing[][] t = new Ing[r1 - r0 + 1][c1 - c0 + 1];
        for (int i = r0; i <= r1; i++) System.arraycopy(g[i], c0, t[i - r0], 0, c1 - c0 + 1);
        return t;
    }

    private Ing parse(String tok) {
        String t = tok.trim();
        int amount = 1;
        int star = t.indexOf('*');
        if (star > 0) {
            amount = Integer.parseInt(t.substring(star + 1).trim());
            t = t.substring(0, star).trim();
        }
        if (!validKey(t)) throw new IllegalArgumentException("Zutat unbekannt: " + t);
        return new Ing(t, Math.max(1, amount));
    }

    private boolean validKey(String k) {
        if (isVanilla(k)) return Material.matchMaterial(k) != null;
        return plugin.registry.get(k) != null;
    }

    private static boolean isVanilla(String k) {
        return k.equals(k.toUpperCase());
    }

    /** Schluessel eines Items: Plugin-ID (lowercase) oder Material-Name (UPPERCASE). */
    public String keyOf(ItemStack it) {
        if (it == null || it.getType().isAir()) return null;
        String id = plugin.items.id(it);
        return id != null ? id : it.getType().name();
    }

    // ------------------------------------------------------------ Matching
    public Recipe match(ItemStack[] g) {
        for (Recipe r : recipes.values()) if (matches(r, g)) return r;
        return null;
    }

    private boolean matches(Recipe r, ItemStack[] g) {
        if (r.shapeless) {
            Map<String, Integer> have = new HashMap<>();
            for (ItemStack it : g) {
                String k = keyOf(it);
                if (k != null) have.merge(k, it.getAmount(), Integer::sum);
            }
            if (have.size() != r.list.size()) return false;
            Set<String> seen = new HashSet<>();
            for (Ing ing : r.list) {
                if (!seen.add(ing.key())) return false;
                if (have.getOrDefault(ing.key(), 0) < ing.amount()) return false;
            }
            return true;
        }
        int minR = 3, maxR = -1, minC = 3, maxC = -1;
        for (int i = 0; i < 9; i++) {
            if (keyOf(g[i]) == null) continue;
            minR = Math.min(minR, i / 3);
            maxR = Math.max(maxR, i / 3);
            minC = Math.min(minC, i % 3);
            maxC = Math.max(maxC, i % 3);
        }
        if (maxR < 0) return false;
        int rows = maxR - minR + 1, cols = maxC - minC + 1;
        if (rows != r.grid.length || cols != r.grid[0].length) return false;
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                Ing need = r.grid[i][j];
                ItemStack it = g[(minR + i) * 3 + (minC + j)];
                String k = keyOf(it);
                if (need == null) {
                    if (k != null) return false;
                } else if (k == null || !k.equals(need.key()) || it.getAmount() < need.amount()) return false;
            }
        }
        return true;
    }

    /** Zieht die Zutaten ab (Aufrufer hat vorher match() geprueft). */
    public void consume(Recipe r, ItemStack[] g) {
        if (r.shapeless) {
            for (Ing ing : r.list) {
                int left = ing.amount();
                for (int i = 0; i < 9 && left > 0; i++) {
                    if (!ing.key().equals(keyOf(g[i]))) continue;
                    int take = Math.min(left, g[i].getAmount());
                    g[i].setAmount(g[i].getAmount() - take);
                    if (g[i].getAmount() <= 0) g[i] = null;
                    left -= take;
                }
            }
            return;
        }
        int minR = 3, minC = 3;
        for (int i = 0; i < 9; i++) {
            if (keyOf(g[i]) == null) continue;
            minR = Math.min(minR, i / 3);
            minC = Math.min(minC, i % 3);
        }
        for (int i = 0; i < r.grid.length; i++) {
            for (int j = 0; j < r.grid[0].length; j++) {
                Ing need = r.grid[i][j];
                if (need == null) continue;
                int idx = (minR + i) * 3 + (minC + j);
                g[idx].setAmount(g[idx].getAmount() - need.amount());
                if (g[idx].getAmount() <= 0) g[idx] = null;
            }
        }
    }

    public ItemStack result(Recipe r) {
        return stackFor(r.result, r.amount);
    }

    public ItemStack stackFor(String key, int amount) {
        if (isVanilla(key)) return new ItemStack(Material.matchMaterial(key), Math.max(1, Math.min(64, amount)));
        ItemDef d = plugin.registry.get(key);
        return plugin.items.create(d, Math.max(1, Math.min(64, amount)));
    }
}
