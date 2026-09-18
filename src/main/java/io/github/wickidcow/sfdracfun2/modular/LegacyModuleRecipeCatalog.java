package io.github.wickidcow.sfdracfun2.modular;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

/**
 * Resolves the 56 observable Ancient Altar module recipes from DracFun 2.0.10.
 *
 * <p>The bundled text file is interoperability data extracted from the public
 * recipe behavior of the supplied binary. It contains identifiers/materials,
 * not original implementation source or assets.</p>
 */
public final class LegacyModuleRecipeCatalog {

    private static final String RESOURCE = "/legacy-module-recipes.txt";
    private static final Map<String, String[]> RECIPES = load();

    private LegacyModuleRecipeCatalog() {}

    public static ItemStack[] recipe(
            ModuleFamily family,
            ModuleTier tier,
            boolean hardMode) {
        String[] tokens = RECIPES.get(key(family, tier));
        if (tokens == null) {
            throw new IllegalStateException(
                    "No audited DracFun 2.0.10 module recipe for "
                            + family + " / " + tier);
        }

        ItemStack[] recipe = new ItemStack[9];
        for (int i = 0; i < recipe.length; i++) {
            recipe[i] = resolve(tokens[i], hardMode);
        }
        return recipe;
    }

    public static int recipeCount() {
        return RECIPES.size();
    }

    private static Map<String, String[]> load() {
        InputStream stream =
                LegacyModuleRecipeCatalog.class.getResourceAsStream(RESOURCE);
        if (stream == null) {
            throw new IllegalStateException(
                    "Missing clean-room module recipe resource " + RESOURCE);
        }

        Map<String, String[]> recipes = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }

                String[] parts = trimmed.split("\\|", -1);
                if (parts.length != 11) {
                    throw new IllegalStateException(
                            "Invalid module recipe line " + lineNumber
                                    + ": expected family, tier and nine ingredients");
                }

                String recipeKey = parts[0] + ':' + parts[1];
                String[] previous = recipes.put(
                        recipeKey,
                        Arrays.copyOfRange(parts, 2, 11));
                if (previous != null) {
                    throw new IllegalStateException(
                            "Duplicate module recipe " + recipeKey);
                }
            }
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to read clean-room module recipe resource",
                    exception);
        }

        if (recipes.size() != 56) {
            throw new IllegalStateException(
                    "Expected 56 audited module recipes but loaded "
                            + recipes.size());
        }
        return Map.copyOf(recipes);
    }

    private static ItemStack resolve(String token, boolean hardMode) {
        if ("_".equals(token)) {
            return null;
        }
        if (token.startsWith("v:")) {
            return new ItemStack(Material.valueOf(token.substring(2)));
        }
        if (token.startsWith("s:")) {
            return required(token.substring(2));
        }
        if (token.startsWith("d:")) {
            return dynamic(token.substring(2), hardMode);
        }
        if (token.startsWith("p:")) {
            return potion(token.substring(2));
        }
        throw new IllegalStateException(
                "Unknown module recipe ingredient token " + token);
    }

    private static ItemStack dynamic(String key, boolean hardMode) {
        return switch (key) {
            case "draconium" -> required(
                    hardMode
                            ? "DRACFUN_DRACONIUM_BLOCK"
                            : "DRACFUN_DRACONIUM_INGOT");
            case "awakenedDraconium" -> required(
                    hardMode
                            ? "DRACFUN_AWAKENED_DRACONIUM_BLOCK"
                            : "DRACFUN_AWAKENED_DRACONIUM_INGOT");
            case "smallChaos" -> required(
                    hardMode
                            ? "DRACFUN_LARGE_CHAOS_FRAGMENT"
                            : "DRACFUN_SMALL_CHAOS_FRAGMENT");
            case "bigChaos" -> required(
                    hardMode
                            ? "DRACFUN_CHAOS_SHARD"
                            : "DRACFUN_LARGE_CHAOS_FRAGMENT");
            case "gold" -> (hardMode
                    ? SlimefunItems.GOLD_24K_BLOCK
                    : SlimefunItems.GOLD_24K).clone();
            case "diamond" -> new ItemStack(
                    hardMode ? Material.DIAMOND_BLOCK : Material.DIAMOND);
            case "netherite" -> new ItemStack(
                    hardMode ? Material.NETHERITE_BLOCK : Material.NETHERITE_INGOT);
            case "redstone" -> new ItemStack(
                    hardMode ? Material.REDSTONE_BLOCK : Material.REDSTONE);
            case "iron" -> new ItemStack(
                    hardMode ? Material.IRON_BLOCK : Material.IRON_INGOT);
            case "emerald" -> new ItemStack(
                    hardMode ? Material.EMERALD_BLOCK : Material.EMERALD);
            default -> throw new IllegalStateException(
                    "Unknown dynamic module ingredient " + key);
        };
    }

    private static ItemStack required(String id) {
        SlimefunItem item = SlimefunItem.getById(id);
        if (item == null) {
            throw new IllegalStateException(
                    "Required module recipe item is not registered: " + id);
        }
        return item.getItem().clone();
    }

    private static ItemStack potion(String legacyName) {
        ItemStack stack = new ItemStack(Material.POTION);
        PotionMeta meta = (PotionMeta) stack.getItemMeta();
        PotionType type = resolvePotionType(legacyName);
        if (type == null) {
            throw new IllegalStateException(
                    "No compatible Bukkit PotionType for " + legacyName);
        }
        meta.setBasePotionType(type);
        stack.setItemMeta(meta);
        return stack;
    }

    private static PotionType resolvePotionType(String legacyName) {
        String[] candidates = switch (legacyName) {
            case "JUMP" -> new String[] {"JUMP", "LEAPING"};
            case "INSTANT_HEAL" -> new String[] {"INSTANT_HEAL", "HEALING"};
            default -> new String[] {legacyName};
        };

        for (String candidate : candidates) {
            try {
                return PotionType.valueOf(candidate);
            } catch (IllegalArgumentException ignored) {
                // Try the next cross-version spelling.
            }
        }
        return null;
    }

    private static String key(ModuleFamily family, ModuleTier tier) {
        return family.name() + ':' + tier.name();
    }
}
