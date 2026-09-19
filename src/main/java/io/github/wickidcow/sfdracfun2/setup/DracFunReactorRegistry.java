package io.github.wickidcow.sfdracfun2.setup;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.items.blocks.UnplaceableBlock;
import io.github.wickidcow.sfdracfun2.SFDracFun2;\nimport io.github.wickidcow.sfdracfun2.compat.LegacyTheme;
import io.github.wickidcow.sfdracfun2.fusion.FusionRecipeCatalog;
import io.github.wickidcow.sfdracfun2.fusion.FusionRecipeSpec;
import io.github.wickidcow.sfdracfun2.reactor.ReactorMachine;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Restores the observable DracFun 2.0.10 Draconic Reactor crafting progression.
 *
 * <p>Fusion-produced components retain their real Fusion Crafter recipe type and
 * audited 3x3 guide recipe while runtime crafting remains in FusionRecipeCatalog.</p>
 */
public final class DracFunReactorRegistry {

    private DracFunReactorRegistry() {}

    public static int register(SFDracFun2 addon, boolean hardMode) {
        ItemGroup machines = DracFunItemGroups.reactor(addon);

        ItemStack draconium = required(hardMode ? "DRACFUN_DRACONIUM_BLOCK" : "DRACFUN_DRACONIUM_INGOT");
        ItemStack awakened =
                required(hardMode ? "DRACFUN_AWAKENED_DRACONIUM_BLOCK" : "DRACFUN_AWAKENED_DRACONIUM_INGOT");
        ItemStack diamond = new ItemStack(hardMode ? Material.DIAMOND_BLOCK : Material.DIAMOND);
        ItemStack iron = new ItemStack(hardMode ? Material.IRON_BLOCK : Material.IRON_INGOT);
        ItemStack gold = hardMode ? SlimefunItems.GOLD_24K_BLOCK : SlimefunItems.GOLD_24K;

        ItemStack draconicCore = required("DRACFUN_DRACONIC_CORE");
        ItemStack wyvernCore = required("DRACFUN_WYVERN_CORE");

        SlimefunItemStack innerRotor = LegacyTheme.REACTOR.stack(
                "DRACFUN_REACTOR_STABILIZER_INNER_ROTOR",
                Material.COPPER_INGOT,
                "Reactor Stabilizer Inner Rotor");
        SlimefunItemStack outerRotor = LegacyTheme.REACTOR.stack(
                "DRACFUN_REACTOR_STABILIZER_OUTER_ROTOR",
                Material.IRON_INGOT,
                "Reactor Stabilizer Outer Rotor");
        SlimefunItemStack rotorAssembly = LegacyTheme.REACTOR.stack(
                "DRACFUN_REACTOR_STABILIZER_ROTOR_ASSEMBLY",
                Material.PISTON,
                "Reactor Stabilizer Rotor Assembly");
        SlimefunItemStack focusRing = LegacyTheme.REACTOR.stack(
                "DRACFUN_REACTOR_STABILIZER_FOCUS_RING",
                Material.ENDER_EYE,
                "Reactor Stabilizer Focus Ring");
        SlimefunItemStack stabilizerFrame = LegacyTheme.REACTOR.stack(
                "DRACFUN_REACTOR_STABILIZER_FRAME",
                Material.IRON_BLOCK,
                "Reactor Stabilizer Frame");

        SlimefunItemStack energyInjector = LegacyTheme.REACTOR.stack(
                "DRACFUN_REACTOR_ENERGY_INJECTOR",
                Material.LIGHTNING_ROD,
                "Reactor Energy Injector");
        SlimefunItemStack stabilizer = LegacyTheme.REACTOR.stack(
                "DRACFUN_REACTOR_STABILIZER",
                Material.BEACON,
                "Reactor Stabilizer");
        SlimefunItemStack reactorCore = LegacyTheme.REACTOR.stack(
                "DRACFUN_DRACONIC_REACTOR_CORE",
                Material.RESPAWN_ANCHOR,
                "Draconic Reactor Core");
        SlimefunItemStack reactor = LegacyTheme.REACTOR.stack(
                "DRACFUN_DRACONIC_REACTOR",
                Material.CRYING_OBSIDIAN,
                "Draconic Reactor",
                "&7Dangerous high-output reactor.",
                "&7Uses Awakened Draconium Blocks as fuel.");

        int registered = 0;

        registered += registerUnplaceable(
                addon,
                machines,
                innerRotor,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                recipe(
                        null, null, null,
                        awakened, awakened, awakened,
                        draconicCore, draconium, draconium));

        registered += registerUnplaceable(
                addon,
                machines,
                outerRotor,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                recipe(
                        null, null, null,
                        diamond, diamond, diamond,
                        draconicCore, draconium, draconium));

        registered += registerUnplaceable(
                addon,
                machines,
                rotorAssembly,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                recipe(
                        null, innerRotor, outerRotor,
                        wyvernCore, draconium, draconium,
                        null, innerRotor, outerRotor));

        registered += registerUnplaceable(
                addon,
                machines,
                focusRing,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                recipe(
                        gold, diamond, gold,
                        diamond, wyvernCore, diamond,
                        gold, diamond, gold));

        registered += registerUnplaceable(
                addon,
                machines,
                stabilizerFrame,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                recipe(
                        iron, iron, iron,
                        wyvernCore, awakened, null,
                        iron, iron, iron));

        // These three components are produced by Chaotic-tier Fusion Crafting.
        registered += registerFusionUnplaceable(addon, machines, energyInjector, hardMode);
        registered += registerFusionUnplaceable(addon, machines, stabilizer, hardMode);
        registered += registerFusionUnplaceable(addon, machines, reactorCore, hardMode);

        if (SlimefunItem.getById(reactor.getItemId()) == null) {
            ItemStack[] reactorRecipe = recipe(
                    null, stabilizer, null,
                    stabilizer, reactorCore, stabilizer,
                    null, stabilizer, energyInjector);
            new ReactorMachine(addon, machines, reactor, reactorRecipe).register(addon);
            registered++;
        }

        return registered;
    }

    private static int registerFusionUnplaceable(
            SFDracFun2 addon,
            ItemGroup group,
            SlimefunItemStack stack,
            boolean hardMode) {
        FusionRecipeSpec spec = FusionRecipeCatalog.requireByOutput(
                hardMode, true, stack.getItemId());
        return registerUnplaceable(
                addon,
                group,
                stack,
                DracFunRecipeTypes.fusion(spec.tier()),
                spec.toGuideRecipe());
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
            throw new IllegalStateException("Required Reactor prerequisite is not registered: " + id);
        }
        return item.getItem().clone();
    }

    private static SlimefunItemStack stack(String id, Material material, String name, String... lore) {
        return new SlimefunItemStack(id, material, name, lore);
    }

    private static ItemStack[] recipe(
            ItemStack a, ItemStack b, ItemStack c,
            ItemStack d, ItemStack e, ItemStack f,
            ItemStack g, ItemStack h, ItemStack i) {
        return new ItemStack[] {a, b, c, d, e, f, g, h, i};
    }
}
