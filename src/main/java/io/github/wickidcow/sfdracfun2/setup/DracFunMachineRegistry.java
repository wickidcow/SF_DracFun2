package io.github.wickidcow.sfdracfun2.setup;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.wickidcow.sfdracfun2.SFDracFun2;
import io.github.wickidcow.sfdracfun2.fusion.FusionCrafterMachine;
import io.github.wickidcow.sfdracfun2.fusion.FusionRecipeCatalog;
import io.github.wickidcow.sfdracfun2.fusion.FusionRecipeSpec;
import io.github.wickidcow.sfdracfun2.fusion.FusionTier;
import io.github.wickidcow.sfdracfun2.machines.EnergyInfuserMachine;
import io.github.wickidcow.sfdracfun2.machines.ItemConverterMachine;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/** Registers completed clean-room DracFun machine implementations. */
public final class DracFunMachineRegistry {

    private DracFunMachineRegistry() {}

    public static int registerEnergyInfuser(SFDracFun2 addon, boolean hardMode) {
        int registered = DracFunFusionComponentRegistry.registerParticleGenerator(addon, hardMode);

        String id = "DRACFUN_ENERGY_INFUSER";
        if (SlimefunItem.getById(id) != null) {
            return registered;
        }

        ItemGroup group = DracFunItemGroups.machines(addon);
        ItemStack draconium = requiredItem(
                hardMode ? "DRACFUN_DRACONIUM_BLOCK" : "DRACFUN_DRACONIUM_INGOT");
        ItemStack particleGenerator = requiredItem("DRACFUN_PARTICLE_GENERATOR");
        ItemStack draconicCore = requiredItem("DRACFUN_DRACONIC_CORE");

        SlimefunItemStack stack = new SlimefunItemStack(
                id,
                Material.RESPAWN_ANCHOR,
                "&dEnergy Infuser",
                "&7Charges powered DracFun modular equipment.",
                "&71000 J = 1 item charge unit.");
        new EnergyInfuserMachine(
                        group,
                        stack,
                        io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType.ENHANCED_CRAFTING_TABLE,
                        new ItemStack[] {
                            draconium, particleGenerator, draconium,
                            draconicCore, new ItemStack(Material.ENCHANTING_TABLE), draconicCore,
                            draconium, draconicCore, draconium
                        })
                .register(addon);
        return registered + 1;
    }

    public static int registerItemConverter(SFDracFun2 addon) {
        String id = "DRACFUN_ITEM_CONVERTER";
        if (SlimefunItem.getById(id) != null) {
            return 0;
        }

        ItemGroup group = DracFunItemGroups.machines(addon);
        SlimefunItemStack stack = new SlimefunItemStack(
                id,
                Material.SMITHING_TABLE,
                "&dItem Converter",
                "&7Rebuilds supported legacy DracFun items",
                "&7using the current clean-room item templates.");
        new ItemConverterMachine(
                        group,
                        stack,
                        io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType.MAGIC_WORKBENCH,
                        new ItemStack[] {
                            null, null, null,
                            null, new ItemStack(Material.CRAFTING_TABLE), null,
                            null, null, null
                        })
                .register(addon);
        return 1;
    }

    public static int registerFusionCrafters(SFDracFun2 addon, boolean hardMode, boolean useDragonEgg) {
        ItemGroup group = DracFunItemGroups.machines(addon);
        List<FusionRecipeSpec> recipes = FusionRecipeCatalog.create(hardMode, useDragonEgg);
        ItemStack fusionCore = requiredItem("DRACFUN_FUSION_CRAFTING_CORE");
        int registered = 0;

        registered += registerFusionCrafter(
                addon,
                group,
                recipes,
                FusionTier.BASIC,
                Material.CRAFTING_TABLE,
                altarRecipe(requiredItem("DRACFUN_BASIC_FUSION_CRAFTING_INJECTOR"), fusionCore));
        registered += registerFusionCrafter(
                addon,
                group,
                recipes,
                FusionTier.WYVERN,
                Material.SMITHING_TABLE,
                altarRecipe(requiredItem("DRACFUN_WYVERN_FUSION_CRAFTING_INJECTOR"), fusionCore));
        registered += registerFusionCrafter(
                addon,
                group,
                recipes,
                FusionTier.DRACONIC,
                Material.RESPAWN_ANCHOR,
                altarRecipe(requiredItem("DRACFUN_DRACONIC_FUSION_CRAFTING_INJECTOR"), fusionCore));
        registered += registerFusionCrafter(
                addon,
                group,
                recipes,
                FusionTier.CHAOTIC,
                Material.CRYING_OBSIDIAN,
                altarRecipe(requiredItem("DRACFUN_CHAOTIC_FUSION_CRAFTING_INJECTOR"), fusionCore));
        return registered;
    }

    private static int registerFusionCrafter(
            SFDracFun2 addon,
            ItemGroup group,
            List<FusionRecipeSpec> recipes,
            FusionTier tier,
            Material material,
            ItemStack[] craftingRecipe) {
        if (SlimefunItem.getById(tier.machineId()) != null) {
            return 0;
        }

        SlimefunItemStack stack = new SlimefunItemStack(
                tier.machineId(),
                material,
                "&d" + tier.displayName() + " Fusion Crafter",
                "&7Clean-room Fusion Crafting machine.",
                "&7Capacity: &f" + tier.capacity() + " J",
                "&7Fusion duration: &f5 seconds");
        new FusionCrafterMachine(group, stack, tier, recipes, craftingRecipe).register(addon);
        return 1;
    }

    private static ItemStack requiredItem(String id) {
        SlimefunItem item = SlimefunItem.getById(id);
        if (item == null) {
            throw new IllegalStateException("Required Fusion component is not registered: " + id);
        }
        return item.getItem().clone();
    }

    private static ItemStack[] altarRecipe(ItemStack injector, ItemStack core) {
        return new ItemStack[] {
            injector, injector, injector,
            injector, core, injector,
            injector, injector, injector
        };
    }
}
