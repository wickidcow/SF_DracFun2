package io.github.wickidcow.sfdracfun2.fusion;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;

/**
 * Observable positional Fusion recipes from DracFun 2.0.10.
 *
 * <p>The old crafter ignored ItemStack amounts while matching and consumed one item
 * from each occupied input slot. The Awakened Draconium Block guide entry displayed
 * four Draconium Blocks in one slot, but the runtime still accepted/consumed one;
 * this catalog preserves the actual machine behavior.</p>
 */
public final class FusionRecipeCatalog {

    private FusionRecipeCatalog() {}

    public static List<FusionRecipeSpec> create(boolean hardMode, boolean useDragonEgg) {
        FusionIngredient diamond = vanilla(hardMode ? Material.DIAMOND_BLOCK : Material.DIAMOND);
        FusionIngredient draconium = sf(hardMode ? "DRACFUN_DRACONIUM_BLOCK" : "DRACFUN_DRACONIUM_INGOT");
        FusionIngredient awakened = sf(hardMode ? "DRACFUN_AWAKENED_DRACONIUM_BLOCK" : "DRACFUN_AWAKENED_DRACONIUM_INGOT");
        FusionIngredient dragonEgg = useDragonEgg ? vanilla(Material.DRAGON_EGG) : sf("DRACFUN_DRAGON_EGG");

        List<FusionRecipeSpec> recipes = new ArrayList<>(List.of(
                recipe(
                        FusionTier.BASIC,
                        "DRACFUN_WYVERN_FUSION_CRAFTING_INJECTOR",
                        1,
                        32_000,
                        sf("DRACFUN_WYVERN_CORE"),
                        sf("DRACFUN_BASIC_FUSION_CRAFTING_INJECTOR"),
                        diamond,
                        sf("DRACFUN_DRACONIC_CORE"),
                        diamond,
                        sf("DRACFUN_DRACONIC_CORE"),
                        diamond,
                        sf("DRACFUN_DRACONIUM_BLOCK"),
                        diamond),

                recipe(
                        FusionTier.WYVERN,
                        "DRACFUN_AWAKENED_CORE",
                        1,
                        1_000_000,
                        sf("DRACFUN_WYVERN_CORE"),
                        vanilla(Material.NETHER_STAR),
                        sf("DRACFUN_WYVERN_CORE"),
                        awakened,
                        awakened,
                        awakened,
                        awakened,
                        sf("DRACFUN_WYVERN_CORE"),
                        sf("DRACFUN_WYVERN_CORE")),

                recipe(
                        FusionTier.WYVERN,
                        "DRACFUN_CHAOTIC_CORE",
                        1,
                        100_000_000,
                        awakened,
                        sf("DRACFUN_CHAOS_SHARD"),
                        awakened,
                        sf("DRACFUN_AWAKENED_CORE"),
                        sf("DRACFUN_AWAKENED_CORE"),
                        sf("DRACFUN_AWAKENED_CORE"),
                        sf("DRACFUN_AWAKENED_CORE"),
                        awakened,
                        awakened),

                recipe(
                        FusionTier.WYVERN,
                        "DRACFUN_AWAKENED_DRACONIUM_BLOCK",
                        4,
                        50_000_000,
                        sf("DRACFUN_DRACONIC_CORE"),
                        sf("DRACFUN_DRACONIUM_BLOCK"),
                        sf("DRACFUN_DRACONIC_CORE"),
                        sf("DRACFUN_DRACONIC_CORE"),
                        sf("DRACFUN_DRAGON_HEART"),
                        sf("DRACFUN_DRACONIC_CORE"),
                        sf("DRACFUN_DRAGON_HEART"),
                        sf("DRACFUN_DRACONIC_CORE"),
                        sf("DRACFUN_DRACONIC_CORE")),

                recipe(
                        FusionTier.WYVERN,
                        "DRACFUN_DRACONIC_FUSION_CRAFTING_INJECTOR",
                        1,
                        256_000,
                        diamond,
                        sf("DRACFUN_WYVERN_FUSION_CRAFTING_INJECTOR"),
                        diamond,
                        sf("DRACFUN_WYVERN_CORE"),
                        sf("DRACFUN_AWAKENED_DRACONIUM_BLOCK"),
                        sf("DRACFUN_WYVERN_CORE"),
                        sf("DRACFUN_AWAKENED_DRACONIUM_BLOCK"),
                        diamond,
                        diamond),

                recipe(
                        FusionTier.DRACONIC,
                        "DRACFUN_CHAOTIC_FUSION_CRAFTING_INJECTOR",
                        1,
                        8_000_000,
                        diamond,
                        sf("DRACFUN_DRACONIC_FUSION_CRAFTING_INJECTOR"),
                        diamond,
                        sf("DRACFUN_CHAOTIC_CORE"),
                        diamond,
                        dragonEgg,
                        diamond,
                        diamond,
                        diamond),

                recipe(
                        FusionTier.DRACONIC,
                        "DRACFUN_CHAOTIC_ENERGY_CORE",
                        1,
                        2_147_483_646,
                        sf("DRACFUN_LARGE_CHAOS_FRAGMENT"),
                        sf("DRACFUN_DRACONIC_ENERGY_CORE"),
                        sf("DRACFUN_LARGE_CHAOS_FRAGMENT"),
                        sf("DRACFUN_LARGE_CHAOS_FRAGMENT"),
                        sf("DRACFUN_LARGE_CHAOS_FRAGMENT"),
                        sf("DRACFUN_LARGE_CHAOS_FRAGMENT"),
                        sf("DRACFUN_LARGE_CHAOS_FRAGMENT"),
                        sf("DRACFUN_LARGE_CHAOS_FRAGMENT"),
                        sf("DRACFUN_LARGE_CHAOS_FRAGMENT")),

                recipe(
                        FusionTier.DRACONIC,
                        "DRACFUN_CHAOS_ORB_OF_INVOCATION",
                        1,
                        2_147_483_646,
                        draconium,
                        sf("DRACFUN_DRAGON_HEART"),
                        draconium,
                        awakened,
                        awakened,
                        awakened,
                        awakened,
                        vanilla(Material.DRAGON_BREATH),
                        vanilla(Material.DRAGON_BREATH)),

                recipe(
                        FusionTier.CHAOTIC,
                        "DRACFUN_REACTOR_ENERGY_INJECTOR",
                        1,
                        16_000_000,
                        draconium,
                        sf("DRACFUN_WYVERN_CORE"),
                        sf("DRACFUN_REACTOR_STABILIZER_INNER_ROTOR"),
                        draconium,
                        sf("DRACFUN_REACTOR_STABILIZER_INNER_ROTOR"),
                        draconium,
                        sf("DRACFUN_REACTOR_STABILIZER_INNER_ROTOR"),
                        draconium,
                        sf("DRACFUN_REACTOR_STABILIZER_INNER_ROTOR")),

                recipe(
                        FusionTier.CHAOTIC,
                        "DRACFUN_REACTOR_STABILIZER",
                        1,
                        16_000_000,
                        awakened,
                        sf("DRACFUN_REACTOR_STABILIZER_FRAME"),
                        sf("DRACFUN_DRACONIC_ENERGY_CORE"),
                        sf("DRACFUN_REACTOR_STABILIZER_ROTOR_ASSEMBLY"),
                        sf("DRACFUN_REACTOR_STABILIZER_FOCUS_RING"),
                        awakened,
                        awakened,
                        sf("DRACFUN_CHAOTIC_CORE"),
                        awakened),

                recipe(
                        FusionTier.CHAOTIC,
                        "DRACFUN_DRACONIC_REACTOR_CORE",
                        1,
                        64_000_000,
                        awakened,
                        sf("DRACFUN_CHAOS_SHARD"),
                        draconium,
                        awakened,
                        draconium,
                        awakened,
                        draconium,
                        awakened,
                        draconium)));

        recipes.addAll(LegacyModularFusionRecipeCatalog.create(hardMode));
        return List.copyOf(recipes);
    }

    public static FusionRecipeSpec requireByOutput(
            boolean hardMode,
            boolean useDragonEgg,
            String outputId) {
        return create(hardMode, useDragonEgg).stream()
                .filter(recipe -> recipe.outputId().equals(outputId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No audited DracFun 2.0.10 Fusion recipe for " + outputId));
    }

    private static FusionRecipeSpec recipe(
            FusionTier tier,
            String outputId,
            int outputAmount,
            int energyCost,
            FusionIngredient... ingredients) {
        return new FusionRecipeSpec(tier, List.of(ingredients), outputId, outputAmount, energyCost);
    }

    private static FusionIngredient sf(String id) {
        return FusionIngredient.slimefun(id);
    }

    private static FusionIngredient vanilla(Material material) {
        return FusionIngredient.vanilla(material);
    }
}
