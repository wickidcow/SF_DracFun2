package io.github.wickidcow.sfdracfun2.setup;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.items.blocks.UnplaceableBlock;
import io.github.wickidcow.sfdracfun2.SFDracFun2;
import io.github.wickidcow.sfdracfun2.energycore.EnergyCoreMachine;
import io.github.wickidcow.sfdracfun2.energycore.EnergyCoreTier;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Registers the observable Energy Core progression from DracFun 2.0.10 using
 * new vanilla-material representations.
 */
public final class DracFunEnergyCoreRegistry {

    private DracFunEnergyCoreRegistry() {}

    /**
     * Registers the three portable Energy Core material identities.
     *
     * <p>These are shared prerequisites for Fusion Crafting and the Energy Core
     * multiblocks, so this method is intentionally independent from the
     * {@code features.energy-core} toggle.</p>
     */
    public static int registerEnergyMaterials(SFDracFun2 addon, boolean hardMode) {
        ItemGroup materials = DracFunItemGroups.materials(addon);
        ItemStack draconium = required(hardMode ? "DRACFUN_DRACONIUM_BLOCK" : "DRACFUN_DRACONIUM_INGOT");
        ItemStack awakened = required(hardMode ? "DRACFUN_AWAKENED_DRACONIUM_BLOCK" : "DRACFUN_AWAKENED_DRACONIUM_INGOT");
        ItemStack draconicCore = required("DRACFUN_DRACONIC_CORE");
        ItemStack wyvernCore = required("DRACFUN_WYVERN_CORE");

        SlimefunItemStack wyvernEnergyCore = stack(
                "DRACFUN_WYVERN_ENERGY_CORE", Material.HEART_OF_THE_SEA, "&dWyvern Energy Core");
        SlimefunItemStack draconicEnergyCore = stack(
                "DRACFUN_DRACONIC_ENERGY_CORE", Material.ECHO_SHARD, "&5Draconic Energy Core");
        SlimefunItemStack chaoticEnergyCore = stack(
                "DRACFUN_CHAOTIC_ENERGY_CORE", Material.NETHER_STAR, "&5Chaotic Energy Core");

        int registered = 0;
        registered += registerUnplaceable(
                addon,
                materials,
                wyvernEnergyCore,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                recipe(
                        draconium, new ItemStack(Material.REDSTONE_BLOCK), draconium,
                        new ItemStack(Material.REDSTONE_BLOCK), draconicCore, new ItemStack(Material.REDSTONE_BLOCK),
                        draconium, new ItemStack(Material.REDSTONE_BLOCK), draconium));

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
        registered += registerUnplaceable(
                addon,
                materials,
                chaoticEnergyCore,
                RecipeType.NULL,
                new ItemStack[9]);

        return registered;
    }

    /** Registers the placeable multiblock pieces and all three Energy Core activators. */
    public static int registerMultiblocks(SFDracFun2 addon, boolean hardMode) {
        ItemGroup machines = DracFunItemGroups.machines(addon);
        ItemStack draconium = required(hardMode ? "DRACFUN_DRACONIUM_BLOCK" : "DRACFUN_DRACONIUM_INGOT");
        ItemStack diamond = new ItemStack(hardMode ? Material.DIAMOND_BLOCK : Material.DIAMOND);
        ItemStack draconicCore = required("DRACFUN_DRACONIC_CORE");
        ItemStack wyvernCore = required("DRACFUN_WYVERN_CORE");
        ItemStack wyvernEnergyCore = required("DRACFUN_WYVERN_ENERGY_CORE");

        SlimefunItemStack particleGenerator = stack(
                "DRACFUN_PARTICLE_GENERATOR", Material.END_ROD, "&dParticle Generator");
        SlimefunItemStack stabilizer = stack(
                "DRACFUN_ENERGY_CORE_STABILIZER", Material.AMETHYST_BLOCK, "&dEnergy Core Stabilizer");
        SlimefunItemStack coreBlock1 = stack(
                "DRACFUN_ENERGY_CORE_BLOCK", Material.PURPUR_BLOCK, "&dEnergy Core Block");
        SlimefunItemStack coreBlock2 = stack(
                "DRACFUN_ENERGY_CORE_BLOCK_2", Material.OBSIDIAN, "&5Energy Core Block II");
        SlimefunItemStack coreBlock3 = stack(
                "DRACFUN_ENERGY_CORE_BLOCK_3", Material.CRYING_OBSIDIAN, "&5Energy Core Block III");

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

        SlimefunItemStack activator1 = stack(
                EnergyCoreTier.TIER_1.activatorId(), Material.REDSTONE_LAMP, "&dEnergy Core Activator I");
        SlimefunItemStack activator2 = stack(
                EnergyCoreTier.TIER_2.activatorId(), Material.RESPAWN_ANCHOR, "&5Energy Core Activator II");
        SlimefunItemStack activator3 = stack(
                EnergyCoreTier.TIER_3.activatorId(), Material.BEACON, "&5Energy Core Activator III");

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
        new SlimefunItem(group, stack, RecipeType.ENHANCED_CRAFTING_TABLE, recipe).register(addon);
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

    private static SlimefunItemStack stack(String id, Material material, String name) {
        return new SlimefunItemStack(id, material, name, "&8DracFun Reborn clean-room item");
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
