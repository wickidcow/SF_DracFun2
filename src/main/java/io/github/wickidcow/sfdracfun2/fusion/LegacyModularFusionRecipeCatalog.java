package io.github.wickidcow.sfdracfun2.fusion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;

/**
 * Resolves the 26 observable modular-gear Fusion recipes from DracFun 2.0.10.
 *
 * <p>The bundled resource contains interoperability recipe data only: tier,
 * output ID, energy cost and positional ingredient identities.</p>
 */
public final class LegacyModularFusionRecipeCatalog {

    private static final String RESOURCE = "/legacy-modular-fusion-recipes.txt";

    private LegacyModularFusionRecipeCatalog() {}

    public static List<FusionRecipeSpec> create(boolean hardMode) {
        InputStream stream =
                LegacyModularFusionRecipeCatalog.class.getResourceAsStream(RESOURCE);
        if (stream == null) {
            throw new IllegalStateException(
                    "Missing clean-room modular Fusion recipe resource " + RESOURCE);
        }

        List<FusionRecipeSpec> recipes = new ArrayList<>();
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
                if (parts.length != 12) {
                    throw new IllegalStateException(
                            "Invalid modular Fusion recipe line " + lineNumber
                                    + ": expected tier, output, energy and nine ingredients");
                }

                FusionTier tier;
                int energyCost;
                try {
                    tier = FusionTier.valueOf(parts[0]);
                    energyCost = Integer.parseInt(parts[2]);
                } catch (IllegalArgumentException exception) {
                    throw new IllegalStateException(
                            "Invalid modular Fusion metadata at line " + lineNumber,
                            exception);
                }

                List<FusionIngredient> ingredients = new ArrayList<>(9);
                for (int i = 3; i < 12; i++) {
                    ingredients.add(resolve(parts[i], hardMode));
                }

                recipes.add(new FusionRecipeSpec(
                        tier,
                        ingredients,
                        parts[1],
                        1,
                        energyCost));
            }
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to read clean-room modular Fusion recipe resource",
                    exception);
        }

        if (recipes.size() != 26) {
            throw new IllegalStateException(
                    "Expected 26 audited modular Fusion recipes but loaded "
                            + recipes.size());
        }

        return List.copyOf(recipes);
    }

    private static FusionIngredient resolve(String token, boolean hardMode) {
        if (token.startsWith("v:")) {
            return FusionIngredient.vanilla(
                    Material.valueOf(token.substring(2)));
        }

        if (token.startsWith("s:")) {
            return FusionIngredient.slimefun(token.substring(2));
        }

        if (token.startsWith("d:")) {
            return dynamic(token.substring(2), hardMode);
        }

        throw new IllegalStateException(
                "Unknown modular Fusion ingredient token " + token);
    }

    private static FusionIngredient dynamic(String key, boolean hardMode) {
        return switch (key) {
            case "draconium" -> FusionIngredient.slimefun(
                    hardMode
                            ? "DRACFUN_DRACONIUM_BLOCK"
                            : "DRACFUN_DRACONIUM_INGOT");
            case "awakenedDraconium" -> FusionIngredient.slimefun(
                    hardMode
                            ? "DRACFUN_AWAKENED_DRACONIUM_BLOCK"
                            : "DRACFUN_AWAKENED_DRACONIUM_INGOT");
            case "bigChaos" -> FusionIngredient.slimefun(
                    hardMode
                            ? "DRACFUN_CHAOS_SHARD"
                            : "DRACFUN_LARGE_CHAOS_FRAGMENT");
            case "netherite" -> FusionIngredient.vanilla(
                    hardMode
                            ? Material.NETHERITE_BLOCK
                            : Material.NETHERITE_INGOT);
            default -> throw new IllegalStateException(
                    "Unknown dynamic modular Fusion ingredient " + key);
        };
    }
}
