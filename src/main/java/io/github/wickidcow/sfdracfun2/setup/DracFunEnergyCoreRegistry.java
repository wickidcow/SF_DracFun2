package io.github.wickidcow.sfdracfun2.setup;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.items.blocks.UnplaceableBlock;
import io.github.wickidcow.sfdracfun2.SFDracFun2;
import io.github.wickidcow.sfdracfun2.energycore.EnergyCoreMachine;
import io.github.wickidcow.sfdracfun2.compat.LegacyTheme;
import io.github.wickidcow.sfdracfun2.energycore.EnergyCorePieceItem;
import io.github.wickidcow.sfdracfun2.energycore.EnergyCoreTier;
import io.github.wickidcow.sfdracfun2.fusion.FusionRecipeCatalog;
import io.github.wickidcow.sfdracfun2.fusion.FusionRecipeSpec;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Registers the observable Energy Core progression from DracFun 2.0.10 using
 * new vanilla-material representations.
 */
public final class DracFunEnergyCoreRegistry {

    private DracFunEnergyCoreRegistry() {}

    /**
     * Registers the portable Energy Core material identities needed by Fusion and
     * multiblock progression. Draconic/Wyvern cores are ensured here as shared
     * prerequisites so Energy Core can be enabled without requiring Fusion startup.
     */
    public static int registerEnergyMaterials(SFDracFun2 addon, boolean hardMode) {
        ItemGroup materials = DracFunItemGroups.materials(addon);
        int registered = registerBaseCores(addon, materials, hardMode);

        ItemStack draconium = required(hardMode ? "DRACFUN_DRACONIUM_BLOCK" : "DRACFUN_DRACONIUM_INGOT");
        ItemStack draconicCore = required("DRACFUN_DRACONIC_CORE");
        ItemStack wyvernCore = required("DRACFUN_WYVERN_CORE");

        SlimefunItemStack wyvernEnergyCore = LegacyTheme.BASIC_CRAFTING.stack(
                "DRACFUN_WYVERN_ENERGY_CORE", Material.HEART_OF_THE_SEA, "Wyvern Energy Core");
        registered += registerUnplaceable(
                addon,
                materials,
                wyvernEnergyCore,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                recipe(
                        draconium, new ItemStack(Material.REDSTONE_BLOCK), draconium,
                        new ItemStack(Material.REDSTONE_BLOCK), draconicCore, new ItemStack(Material.REDSTONE_BLOCK),
                        draconium, new ItemStack(Material.REDSTONE_BLOCK), draconium));

        String awakenedId = hardMode ? "DRACFUN_AWAKENED_DRACONIUM_BLOCK" : "DRACFUN_AWAKENED_DRACONIUM_INGOT";
        SlimefunItem awakenedItem = SlimefunItem.getById(awakenedId);
        if (awakenedItem != null) {
            ItemStack awakened = awakenedItem.getItem().clone();
            SlimefunItemStack draconicEnergyCore = LegacyTheme.ADVANCED_CRAFTING.stack(
                    "DRACFUN_DRACONIC_ENERGY_CORE", Material.ECHO_SHARD, "Draconic Energy Core");
            SlimefunItemStack chaoticEnergyCore = LegacyTheme.END_GAME_CRAFTING.stack(
                    "DRACFUN_CHAOTIC_ENERGY_CORE", Material.NETHER_STAR, "Chaotic Energy Core");

            registered += registerUnplaceable(
                    addon,
                    materials,
                    draconicEnergyCore,
                    RecipeType.ENHANCED_CRAFTING_TABLE,
                    recipe(
                            awakened, wyvernEnergyCore, awakened,
                            wyvernEnergyCore, wyvernCore, wyvernEnergyCore,
                            awakened, wyvernEnergyCore, awakened));

            // The Chaotic Energy Core is produced by Draconic-tier Fusion Crafting.
            FusionRecipeSpec chaoticEnergyRecipe = FusionRecipeCatalog.requireByOutput(
                    hardMode, true, chaoticEnergyCore.getItemId());
            registered += registerUnplaceable(
                    addon,
                    materials,
                    chaoticEnergyCore,
                    DracFunRecipeTypes.fusion(chaoticEnergyRecipe.tier()),
                    chaoticEnergyRecipe.toGuideRecipe());
        }

        return registered;
    }

    /** Registers the placeable multiblock pieces and all three Energy Core activators. */
    public static int registerMultiblocks(SFDracFun2 addon, boolean hardMode) {
        ItemGroup machines = DracFunItemGroups.energyCore(addon);
        ItemStack draconium = required(hardMode ? "DRACFUN_DRACONIUM_BLOCK" : "DRACFUN_DRACONIUM_INGOT");
        ItemStack diamond = new ItemStack(hardMode ? Material.DIAMOND_BLOCK : Material.DIAMOND);
        ItemStack draconicCore = required("DRACFUN_DRACONIC_CORE");
        ItemStack wyvernCore = required("DRACFUN_WYVERN_CORE");
        ItemStack wyvernEnergyCore = required("DRACFUN_WYVERN_ENERGY_CORE");

        SlimefunItemStack particleGenerator = LegacyTheme.BASIC_CRAFTING.stack(
                "DRACFUN_PARTICLE_GENERATOR", Material.RED_STAINED_GLASS, "Particle Generator");
        SlimefunItemStack stabilizer = LegacyTheme.ENERGY_CORE.stack(
                "DRACFUN_ENERGY_CORE_STABILIZER", Material.BLUE_STAINED_GLASS, "Energy Core Stabilizer");
        SlimefunItemStack coreBlock1 = LegacyTheme.ENERGY_CORE.stack(
                "DRACFUN_ENERGY_CORE_BLOCK", Material.RED_GLAZED_TERRACOTTA, "Energy Core");
        SlimefunItemStack coreBlock2 = LegacyTheme.ENERGY_CORE.stack(
                "DRACFUN_ENERGY_CORE_BLOCK_2", Material.RED_GLAZED_TERRACOTTA, "Energy Core (II)");
        SlimefunItemStack coreBlock3 = LegacyTheme.ENERGY_CORE.stack(
                "DRACFUN_ENERGY_CORE_BLOCK_3", Material.RED_GLAZED_TERRACOTTA, "Energy Core (III)");

        int registered = 0;
        registered += registerUnplaceable(
                addon,
                machines,
                particleGenerator,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                recipe(
                        new ItemStack(Material.REDSTONE_BLOCK), new ItemStack(Material.BLAZE_ROD), new ItemStack(Material.REDSTONE_BLOCK),
                        new ItemStack(Material.BLAZE_ROD), draconicCore, new ItemStack(Material.BLAZE_ROD),
                        new ItemStack(Material.REDSTONE_BLOCK), new ItemStack(Material.BLAZE_ROD), new ItemStack(Material.REDSTONE_BLOCK)));

        registered += registerPlaceable(
                addon,
                machines,
                stabilizer,
                recipe(
                        diamond, null, diamond,
                        null, particleGenerator, null,
                        diamond, null, diamond));

        registered += registerPlaceable(
                addon,
                machines,
                coreBlock1,
                recipe(
                        draconium, draconium, draconium,
                        wyvernEnergyCore, wyvernCore, wyvernEnergyCore,
                        draconium, draconium, draconium));

        registered += registerPlaceable(
                addon,
                machines,
                coreBlock2,
                surround(new ItemStack(Material.REDSTONE_BLOCK), coreBlock1));

        registered += registerPlaceable(
                addon,
                machines,
                coreBlock3,
                surround(new ItemStack(Material.REDSTONE_BLOCK), coreBlock2));

        SlimefunItemStack activator1 = LegacyTheme.ENERGY_CORE.stack(
                EnergyCoreTier.TIER_1.activatorId(), Material.BLACK_STAINED_GLASS, "Energy Core Activator");
        SlimefunItemStack activator2 = LegacyTheme.ENERGY_CORE.stack(
                EnergyCoreTier.TIER_2.activatorId(), Material.BLACK_STAINED_GLASS, "Energy Core Activator (II)");
        SlimefunItemStack activator3 = LegacyTheme.ENERGY_CORE.stack(
                EnergyCoreTier.TIER_3.activatorId(), Material.BLACK_STAINED_GLASS, "Energy Core Activator (III)");

        registered += registerActivator(
                addon,
                machines,
                activator1,
                EnergyCoreTier.TIER_1,
                surround(new ItemStack(Material.REDSTONE_BLOCK), new ItemStack(Material.LEVER)));
        registered += registerActivator(
                addon,
                machines,
                activator2,
                EnergyCoreTier.TIER_2,
                surround(new ItemStack(Material.REDSTONE_BLOCK), activator1));
        registered += registerActivator(
                addon,
                machines,
                activator3,
                EnergyCoreTier.TIER_3,
                surround(new ItemStack(Material.REDSTONE_BLOCK), activator2));

        return registered;
    }

    private static int registerBaseCores(SFDracFun2 addon, ItemGroup materials, boolean hardMode) {
        ItemStack draconium = required(hardMode ? "DRACFUN_DRACONIUM_BLOCK" : "DRACFUN_DRACONIUM_INGOT");
        ItemStack gold = hardMode ? SlimefunItems.GOLD_24K_BLOCK : SlimefunItems.GOLD_24K;
        ItemStack diamond = new ItemStack(hardMode ? Material.DIAMOND_BLOCK : Material.DIAMOND);

        SlimefunItemStack draconicCore = LegacyTheme.BASIC_CRAFTING.stack(
                "DRACFUN_DRACONIC_CORE", Material.ECHO_SHARD, "Draconic Core");
        SlimefunItemStack wyvernCore = LegacyTheme.BASIC_CRAFTING.stack(
                "DRACFUN_WYVERN_CORE", Material.AMETHYST_SHARD, "Wyvern Core");

        int registered = 0;
        registered += registerUnplaceable(
                addon,
                materials,
                draconicCore,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                recipe(
                        gold, draconium, gold,
                        draconium, diamond, draconium,
                        gold, draconium, gold));
        registered += registerUnplaceable(
                addon,
                materials,
                wyvernCore,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                recipe(
                        draconium, draconicCore, draconium,
                        draconicCore, new ItemStack(Material.NETHER_STAR), draconicCore,
                        draconium, draconicCore, draconium));
        return registered;
    }

    private static int registerActivator(
            SFDracFun2 addon,
            ItemGroup group,
            SlimefunItemStack stack,
            EnergyCoreTier tier,
            ItemStack[] recipe) {
        if (SlimefunItem.getById(stack.getItemId()) != null) {
            return 0;
        }
        new EnergyCoreMachine(group, stack, tier, recipe).register(addon);
        return 1;
    }

    private static int registerPlaceable(
            SFDracFun2 addon,
            ItemGroup group,
            SlimefunItemStack stack,
            ItemStack[] recipe) {
        if (SlimefunItem.getById(stack.getItemId()) != null) {
            return 0;
        }
        new EnergyCorePieceItem(group, stack, RecipeType.ENHANCED_CRAFTING_TABLE, recipe).register(addon);
        return 1;
    }

    private static int registerUnplaceable(
            SFDracFun2 addon,
            ItemGroup group,
            SlimefunItemStack stack,
            RecipeType recipeType,
            ItemStack[] recipe) {
        if (SlimefunItem.getById(stack.getItemId()) != null) {
            return 0;
        }
        new UnplaceableBlock(group, stack, recipeType, recipe).register(addon);
        return 1;
    }

    private static ItemStack required(String id) {
        SlimefunItem item = SlimefunItem.getById(id);
        if (item == null) {
            throw new IllegalStateException("Required Energy Core prerequisite is not registered: " + id);
        }
        return item.getItem().clone();
    }


    private static ItemStack[] surround(ItemStack outside, ItemStack center) {
        return recipe(
                outside, outside, outside,
                outside, center, outside,
                outside, outside, outside);
    }

    private static ItemStack[] recipe(
            ItemStack a, ItemStack b, ItemStack c,
            ItemStack d, ItemStack e, ItemStack f,
            ItemStack g, ItemStack h, ItemStack i) {
        return new ItemStack[] {a, b, c, d, e, f, g, h, i};
    }
}
