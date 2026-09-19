package io.github.wickidcow.sfdracfun2.setup;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.items.blocks.UnplaceableBlock;
import io.github.wickidcow.sfdracfun2.SFDracFun2;
import io.github.wickidcow.sfdracfun2.energycore.EnergyCorePieceItem;
import io.github.wickidcow.sfdracfun2.compat.LegacyTheme;
import io.github.wickidcow.sfdracfun2.fusion.FusionRecipeCatalog;
import io.github.wickidcow.sfdracfun2.fusion.FusionRecipeSpec;
import io.github.wickidcow.sfdracfun2.modular.LegacyDracFunKeys;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Restores the material/component identities that feed DracFun's Fusion progression.
 * All visuals are new vanilla-material representations; only observable IDs, recipes and
 * persistence values are preserved.
 */
public final class DracFunFusionComponentRegistry {

    private DracFunFusionComponentRegistry() {}

    public static int register(SFDracFun2 addon, boolean hardMode, boolean useDragonEgg) {
        ItemGroup materials = DracFunItemGroups.materials(addon);
        ItemGroup machines = DracFunItemGroups.electric(addon);

        int registered = registerParticleGenerator(addon, hardMode);

        ItemStack draconiumIngot = required("DRACFUN_DRACONIUM_INGOT");
        ItemStack draconiumBlock = required("DRACFUN_DRACONIUM_BLOCK");
        ItemStack draconium = hardMode ? draconiumBlock : draconiumIngot;
        ItemStack gold = hardMode ? SlimefunItems.GOLD_24K_BLOCK : SlimefunItems.GOLD_24K;
        ItemStack diamond = new ItemStack(hardMode ? Material.DIAMOND_BLOCK : Material.DIAMOND);
        ItemStack draconicCore = required("DRACFUN_DRACONIC_CORE");

        SlimefunItemStack wyvernCore = LegacyTheme.BASIC_CRAFTING.stack(
                "DRACFUN_WYVERN_CORE", Material.AMETHYST_SHARD, "Wyvern Core");
        registered += registerUnplaceable(
                addon,
                materials,
                wyvernCore,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                recipe(
                        draconium, draconicCore, draconium,
                        draconicCore, new ItemStack(Material.NETHER_STAR), draconicCore,
                        draconium, draconicCore, draconium));

        SlimefunItemStack fusionCore = LegacyTheme.FUSION_CRAFTING.stack(
                "DRACFUN_FUSION_CRAFTING_CORE", Material.LODESTONE, "Fusion Crafting Core");
        registered += registerUnplaceable(
                addon,
                machines,
                fusionCore,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                recipe(
                        new ItemStack(Material.LAPIS_BLOCK), diamond, new ItemStack(Material.LAPIS_BLOCK),
                        diamond, draconicCore, diamond,
                        new ItemStack(Material.LAPIS_BLOCK), diamond, new ItemStack(Material.LAPIS_BLOCK)));

        SlimefunItemStack basicInjector = LegacyTheme.FUSION_CRAFTING.stack(
                "DRACFUN_BASIC_FUSION_CRAFTING_INJECTOR",
                Material.COPPER_BLOCK,
                "Basic Fusion Crafting Injector");
        registered += registerUnplaceable(
                addon,
                machines,
                basicInjector,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                recipe(
                        diamond, draconicCore, diamond,
                        new ItemStack(Material.IRON_BLOCK), new ItemStack(Material.IRON_BLOCK), new ItemStack(Material.IRON_BLOCK),
                        new ItemStack(Material.IRON_BLOCK), new ItemStack(Material.IRON_BLOCK), new ItemStack(Material.IRON_BLOCK)));

        // Register guide prerequisites before Fusion outputs reference them.
        registered += DracFunSharedProgressionRegistry.registerDragonHeart(addon);

        SlimefunItemStack smallChaos = LegacyTheme.MOB.stack(
                "DRACFUN_SMALL_CHAOS_FRAGMENT", Material.BLACK_DYE, "Small Chaos Fragment");
        SlimefunItemStack largeChaos = LegacyTheme.MOB.stack(
                "DRACFUN_LARGE_CHAOS_FRAGMENT", Material.FLINT, "Large Chaos Fragment");
        SlimefunItemStack chaosShard = LegacyTheme.MOB.stack(
                "DRACFUN_CHAOS_SHARD", Material.NETHER_STAR, "Chaos Shard");
        registered += registerUnplaceable(
                addon,
                materials,
                smallChaos,
                RecipeType.MAGIC_WORKBENCH,
                center(chaosShard),
                smallChaos.asQuantity(81));
        registered += registerUnplaceable(
                addon,
                materials,
                largeChaos,
                RecipeType.MAGIC_WORKBENCH,
                fill(smallChaos));
        registered += registerUnplaceable(
                addon,
                materials,
                chaosShard,
                RecipeType.MAGIC_WORKBENCH,
                fill(largeChaos));

        if (!useDragonEgg) {
            SlimefunItemStack customEgg = LegacyTheme.ADVANCED_CRAFTING.stack(
                    "DRACFUN_DRAGON_EGG", Material.DRAGON_EGG, "DracFun's Dragon Egg");
            registered += registerUnplaceable(
                    addon,
                    materials,
                    customEgg,
                    RecipeType.ANCIENT_ALTAR,
                    recipe(
                            diamond, gold, diamond,
                            gold, new ItemStack(Material.NETHER_STAR), gold,
                            diamond, gold, diamond));
        }

        SlimefunItemStack awakenedBlock = fusionOutput(
                LegacyTheme.ADVANCED_CRAFTING,
                "DRACFUN_AWAKENED_DRACONIUM_BLOCK",
                Material.WAXED_COPPER_BLOCK,
                "Awakened Draconium Block",
                50_000_000);
        registered += registerFusionSimple(
                addon, materials, awakenedBlock, hardMode, useDragonEgg);

        SlimefunItemStack awakenedIngot = LegacyTheme.ADVANCED_CRAFTING.stack(
                "DRACFUN_AWAKENED_DRACONIUM_INGOT", Material.COPPER_INGOT, "Awakened Draconium Ingot");
        registered += registerUnplaceable(
                addon,
                materials,
                awakenedIngot,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                center(awakenedBlock),
                awakenedIngot.asQuantity(9));

        SlimefunItemStack awakenedNugget = LegacyTheme.ADVANCED_CRAFTING.stack(
                "DRACFUN_AWAKENED_DRACONIUM_NUGGET", Material.GOLD_NUGGET, "Awakened Draconium Nugget");
        registered += registerUnplaceable(
                addon,
                materials,
                awakenedNugget,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                center(awakenedIngot),
                awakenedNugget.asQuantity(9));

        SlimefunItemStack awakenedCore = fusionOutput(
                LegacyTheme.ADVANCED_CRAFTING,
                "DRACFUN_AWAKENED_CORE", Material.HEART_OF_THE_SEA, "Awakened Core", 1_000_000);
        registered += registerFusionUnplaceable(
                addon, materials, awakenedCore, hardMode, useDragonEgg);

        SlimefunItemStack chaoticCore = fusionOutput(
                LegacyTheme.END_GAME_CRAFTING,
                "DRACFUN_CHAOTIC_CORE", Material.NETHER_STAR, "Chaotic Core", 100_000_000);
        registered += registerFusionUnplaceable(
                addon, materials, chaoticCore, hardMode, useDragonEgg);

        SlimefunItemStack wyvernInjector = fusionOutput(
                LegacyTheme.FUSION_CRAFTING,
                "DRACFUN_WYVERN_FUSION_CRAFTING_INJECTOR",
                Material.AMETHYST_BLOCK,
                "Wyvern Fusion Crafting Injector",
                32_000);
        SlimefunItemStack draconicInjector = fusionOutput(
                LegacyTheme.FUSION_CRAFTING,
                "DRACFUN_DRACONIC_FUSION_CRAFTING_INJECTOR",
                Material.RESPAWN_ANCHOR,
                "Draconic Fusion Crafting Injector",
                256_000);
        SlimefunItemStack chaoticInjector = fusionOutput(
                LegacyTheme.FUSION_CRAFTING,
                "DRACFUN_CHAOTIC_FUSION_CRAFTING_INJECTOR",
                Material.CRYING_OBSIDIAN,
                "Chaotic Fusion Crafting Injector",
                8_000_000);

        registered += registerFusionUnplaceable(
                addon, machines, wyvernInjector, hardMode, useDragonEgg);
        registered += registerFusionUnplaceable(
                addon, machines, draconicInjector, hardMode, useDragonEgg);
        registered += registerFusionUnplaceable(
                addon, machines, chaoticInjector, hardMode, useDragonEgg);

        return registered;
    }

    /**
     * Registers the Particle Generator and its Draconic Core prerequisite.
     *
     * <p>The Particle Generator is referenced by both the Energy Infuser and
     * Shield Control module progression, so it must remain available even when
     * Fusion Crafting itself is disabled.</p>
     */
    public static int registerParticleGenerator(SFDracFun2 addon, boolean hardMode) {
        int registered = registerDraconicCore(addon, hardMode);

        String id = "DRACFUN_PARTICLE_GENERATOR";
        if (SlimefunItem.getById(id) != null) {
            return registered;
        }

        ItemGroup materials = DracFunItemGroups.materials(addon);
        ItemStack draconicCore = required("DRACFUN_DRACONIC_CORE");
        ItemStack redstoneBlock = new ItemStack(Material.REDSTONE_BLOCK);
        ItemStack blazeRod = new ItemStack(Material.BLAZE_ROD);

        SlimefunItemStack particleGenerator = LegacyTheme.BASIC_CRAFTING.stack(
                id,
                Material.END_CRYSTAL,
                "Particle Generator");

        registered += registerUnplaceable(
                addon,
                materials,
                particleGenerator,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                recipe(
                        redstoneBlock, blazeRod, redstoneBlock,
                        blazeRod, draconicCore, blazeRod,
                        redstoneBlock, blazeRod, redstoneBlock));
        return registered;
    }

    /**
     * Registers the Draconic Core as a shared progression component.
     *
     * <p>Modular gear, Fusion, Reactor and Energy Core progression all reference
     * this identity, so it must not be owned exclusively by the Fusion feature.</p>
     */
    public static int registerDraconicCore(SFDracFun2 addon, boolean hardMode) {
        ItemGroup materials = DracFunItemGroups.materials(addon);

        ItemStack draconium = required(
                hardMode ? "DRACFUN_DRACONIUM_BLOCK" : "DRACFUN_DRACONIUM_INGOT");
        ItemStack gold = hardMode ? SlimefunItems.GOLD_24K_BLOCK : SlimefunItems.GOLD_24K;
        ItemStack diamond = new ItemStack(hardMode ? Material.DIAMOND_BLOCK : Material.DIAMOND);

        SlimefunItemStack draconicCore = LegacyTheme.BASIC_CRAFTING.stack(
                "DRACFUN_DRACONIC_CORE",
                Material.ECHO_SHARD,
                "Draconic Core");

        return registerUnplaceable(
                addon,
                materials,
                draconicCore,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                recipe(
                        gold, draconium, gold,
                        draconium, diamond, draconium,
                        gold, draconium, gold));
    }

    private static int registerFusionUnplaceable(
            SFDracFun2 addon,
            ItemGroup group,
            SlimefunItemStack item,
            boolean hardMode,
            boolean useDragonEgg) {
        FusionRecipeSpec spec = FusionRecipeCatalog.requireByOutput(
                hardMode, useDragonEgg, item.getItemId());
        return registerUnplaceable(
                addon,
                group,
                item,
                DracFunRecipeTypes.fusion(spec.tier()),
                spec.toGuideRecipe());
    }

    private static int registerFusionSimple(
            SFDracFun2 addon,
            ItemGroup group,
            SlimefunItemStack item,
            boolean hardMode,
            boolean useDragonEgg) {
        if (SlimefunItem.getById(item.getItemId()) != null) {
            return 0;
        }
        FusionRecipeSpec spec = FusionRecipeCatalog.requireByOutput(
                hardMode, useDragonEgg, item.getItemId());
        new EnergyCorePieceItem(
                        group,
                        item,
                        DracFunRecipeTypes.fusion(spec.tier()),
                        spec.toGuideRecipe())
                .register(addon);
        return 1;
    }

    private static ItemStack required(String id) {
        SlimefunItem item = SlimefunItem.getById(id);
        if (item == null) {
            throw new IllegalStateException("Required DracFun progression item is not registered: " + id);
        }
        return item.getItem().clone();
    }

    private static int registerUnplaceable(
            SFDracFun2 addon,
            ItemGroup group,
            SlimefunItemStack item,
            RecipeType type,
            ItemStack[] recipe) {
        if (SlimefunItem.getById(item.getItemId()) != null) {
            return 0;
        }
        new UnplaceableBlock(group, item, type, recipe).register(addon);
        return 1;
    }

    private static int registerUnplaceable(
            SFDracFun2 addon,
            ItemGroup group,
            SlimefunItemStack item,
            RecipeType type,
            ItemStack[] recipe,
            ItemStack output) {
        if (SlimefunItem.getById(item.getItemId()) != null) {
            return 0;
        }
        new UnplaceableBlock(group, item, type, recipe, output).register(addon);
        return 1;
    }

    private static int registerSimple(
            SFDracFun2 addon,
            ItemGroup group,
            SlimefunItemStack item,
            RecipeType type,
            ItemStack[] recipe) {
        if (SlimefunItem.getById(item.getItemId()) != null) {
            return 0;
        }
        new SlimefunItem(group, item, type, recipe).register(addon);
        return 1;
    }

    private static SlimefunItemStack stack(String id, Material material, String name) {
        return new SlimefunItemStack(id, material, name, "&8DracFun Reborn clean-room item");
    }

    private static SlimefunItemStack fusionOutput(
            LegacyTheme theme, String id, Material material, String name, int fusionPower) {
        SlimefunItemStack stack = theme.stack(id, material, name);
        ItemMeta meta = stack.getItemMeta();
        meta.getPersistentDataContainer().set(
                LegacyDracFunKeys.FUSION_POWER,
                PersistentDataType.INTEGER,
                fusionPower);
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack[] center(ItemStack item) {
        return recipe(null, null, null, null, item, null, null, null, null);
    }

    private static ItemStack[] fill(ItemStack item) {
        return recipe(item, item, item, item, item, item, item, item, item);
    }

    private static ItemStack[] emptyRecipe() {
        return new ItemStack[9];
    }

    private static ItemStack[] recipe(
            ItemStack a, ItemStack b, ItemStack c,
            ItemStack d, ItemStack e, ItemStack f,
            ItemStack g, ItemStack h, ItemStack i) {
        return new ItemStack[] {a, b, c, d, e, f, g, h, i};
    }
}
