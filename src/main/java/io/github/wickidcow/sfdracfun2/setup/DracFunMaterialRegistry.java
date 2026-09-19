package io.github.wickidcow.sfdracfun2.setup;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.items.blocks.WitherProofBlock;
import io.github.wickidcow.sfdracfun2.SFDracFun2;
import io.github.wickidcow.sfdracfun2.energycore.EnergyCorePieceItem;
import io.github.wickidcow.sfdracfun2.compat.LegacyTheme;
import io.github.wickidcow.sfdracfun2.items.EnderDraconiumOre;
import io.github.wickidcow.sfdracfun2.items.LegacyGuideItem;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

/** Restores the independently verifiable Draconium material progression. */
public final class DracFunMaterialRegistry {

    private DracFunMaterialRegistry() {}

    public static int register(SFDracFun2 addon, boolean enableEndResource) {
        ItemGroup group = DracFunItemGroups.materials(addon);
        int registered = 0;

        if (SlimefunItem.getById("DRACFUN_GUIDE") == null) {
            SlimefunItemStack guide = LegacyTheme.BASIC_CRAFTING.stack(
                    "DRACFUN_GUIDE",
                    Material.BOOK,
                    "DracFun Guide",
                    "DracFun Reborn 2.0.4 progression guide.",
                    "Open the DracFun category to browse Materials, Energy Core,",
                    "Wyvern, Draconic and Chaotic Gear, Modules, Electric machines",
                    "and the Draconic Reactor.",
                    "Progress through Fusion Crafting and prepare for the Chaos Guardian.");
            new LegacyGuideItem(group, guide).register(addon);
            registered++;
        }

        SlimefunItemStack ore = LegacyTheme.ORE.stack(
                "DRACFUN_DRACONIUM_ORE",
                Material.END_STONE,
                "Ender Draconium Ore");
        SlimefunItemStack dust = LegacyTheme.BASIC_CRAFTING.stack(
                "DRACFUN_DRACONIUM_DUST",
                Material.NETHERITE_SCRAP,
                "Draconium Dust");
        SlimefunItemStack ingot = LegacyTheme.BASIC_CRAFTING.stack(
                "DRACFUN_DRACONIUM_INGOT",
                Material.NETHERITE_INGOT,
                "Draconium Ingot");
        SlimefunItemStack block = LegacyTheme.BASIC_CRAFTING.stack(
                "DRACFUN_DRACONIUM_BLOCK",
                Material.NETHERITE_BLOCK,
                "Draconium Block");
        SlimefunItemStack infusedObsidian = LegacyTheme.BASIC_CRAFTING.stack(
                "DRACFUN_DRACONIUM_INFUSED_OBSIDIAN",
                Material.OBSIDIAN,
                "Draconium Infused Obsidian");

        if (enableEndResource && SlimefunItem.getById(ore.getItemId()) == null) {
            EnderDraconiumOre resource = new EnderDraconiumOre(
                    group,
                    ore,
                    new NamespacedKey(addon, "ender_draconium_ore"));
            resource.register(addon);
            resource.register();
            registered++;
        }

        if (SlimefunItem.getById(dust.getItemId()) == null) {
            new SlimefunItem(
                            group,
                            dust,
                            RecipeType.ORE_CRUSHER,
                            recipe(ore, null, null, null, null, null, null, null, null))
                    .register(addon);
            registered++;
        }

        if (SlimefunItem.getById(ingot.getItemId()) == null) {
            new SlimefunItem(
                            group,
                            ingot,
                            RecipeType.SMELTERY,
                            recipe(dust, null, null, null, null, null, null, null, null))
                    .register(addon);
            registered++;
        }

        if (SlimefunItem.getById(block.getItemId()) == null) {
            new EnergyCorePieceItem(group, block, RecipeType.ENHANCED_CRAFTING_TABLE, fill(ingot)).register(addon);
            registered++;
        }

        if (SlimefunItem.getById(infusedObsidian.getItemId()) == null) {
            ItemStack blazePowder = new ItemStack(Material.BLAZE_POWDER);
            ItemStack obsidian = new ItemStack(Material.OBSIDIAN);
            new WitherProofBlock(
                            group,
                            infusedObsidian,
                            RecipeType.MAGIC_WORKBENCH,
                            recipe(
                                    blazePowder, obsidian, blazePowder,
                                    obsidian, dust, obsidian,
                                    blazePowder, obsidian, blazePowder))
                    .register(addon);
            registered++;
        }

        return registered;
    }

    private static SlimefunItemStack stack(String id, Material material, String name, String... lore) {
        return new SlimefunItemStack(id, material, name, lore);
    }

    private static ItemStack[] fill(ItemStack item) {
        return recipe(item, item, item, item, item, item, item, item, item);
    }

    private static ItemStack[] recipe(
            ItemStack a, ItemStack b, ItemStack c,
            ItemStack d, ItemStack e, ItemStack f,
            ItemStack g, ItemStack h, ItemStack i) {
        return new ItemStack[] {a, b, c, d, e, f, g, h, i};
    }
}
