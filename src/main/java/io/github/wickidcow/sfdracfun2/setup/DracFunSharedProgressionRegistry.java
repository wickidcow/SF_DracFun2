package io.github.wickidcow.sfdracfun2.setup;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.items.blocks.UnplaceableBlock;
import io.github.wickidcow.sfdracfun2.SFDracFun2;
import io.github.wickidcow.sfdracfun2.energycore.EnergyCorePieceItem;
import io.github.wickidcow.sfdracfun2.items.DragonHeartItem;
import io.github.wickidcow.sfdracfun2.modular.LegacyDracFunKeys;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Shared material/core progression used by Fusion, modular gear, Reactor and Guardian systems.
 *
 * <p>These identities are not machines. Keeping them in a shared registry allows
 * feature groups to remain independently toggleable without silently substituting
 * compatibility placeholders for ingredients they actually need.</p>
 */
public final class DracFunSharedProgressionRegistry {

    private DracFunSharedProgressionRegistry() {}

    public static int register(
            SFDracFun2 addon,
            boolean hardMode,
            boolean useDragonEgg) {
        ItemGroup materials = DracFunItemGroups.materials(addon);
        int registered = DracFunFusionComponentRegistry.registerParticleGenerator(addon, hardMode);

        ItemStack draconium = required(
                hardMode ? "DRACFUN_DRACONIUM_BLOCK" : "DRACFUN_DRACONIUM_INGOT");
        ItemStack gold = hardMode ? SlimefunItems.GOLD_24K_BLOCK : SlimefunItems.GOLD_24K;
        ItemStack diamond = new ItemStack(hardMode ? Material.DIAMOND_BLOCK : Material.DIAMOND);
        ItemStack draconicCore = required("DRACFUN_DRACONIC_CORE");

        SlimefunItemStack wyvernCore = stack(
                "DRACFUN_WYVERN_CORE",
                Material.AMETHYST_SHARD,
                "&dWyvern Core");
        registered += registerUnplaceable(
                addon,
                materials,
                wyvernCore,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                recipe(
                        draconium, draconicCore, draconium,
                        draconicCore, new ItemStack(Material.NETHER_STAR), draconicCore,
                        draconium, draconicCore, draconium));

        SlimefunItemStack awakenedBlock = fusionOutput(
                "DRACFUN_AWAKENED_DRACONIUM_BLOCK",
                Material.NETHERITE_BLOCK,
                "&6Awakened Draconium Block",
                50_000_000);
        registered += registerSimple(
                addon,
                materials,
                awakenedBlock,
                RecipeType.NULL,
                emptyRecipe());

        SlimefunItemStack awakenedIngot = stack(
                "DRACFUN_AWAKENED_DRACONIUM_INGOT",
                Material.NETHERITE_INGOT,
                "&6Awakened Draconium Ingot");
        registered += registerUnplaceable(
                addon,
                materials,
                awakenedIngot,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                center(awakenedBlock),
                awakenedIngot.asQuantity(9));

        SlimefunItemStack awakenedNugget = stack(
                "DRACFUN_AWAKENED_DRACONIUM_NUGGET",
                Material.GOLD_NUGGET,
                "&6Awakened Draconium Nugget");
        registered += registerUnplaceable(
                addon,
                materials,
                awakenedNugget,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                center(awakenedIngot),
                awakenedNugget.asQuantity(9));

        SlimefunItemStack awakenedCore = fusionOutput(
                "DRACFUN_AWAKENED_CORE",
                Material.HEART_OF_THE_SEA,
                "&6Awakened Core",
                1_000_000);
        registered += registerUnplaceable(
                addon,
                materials,
                awakenedCore,
                RecipeType.NULL,
                emptyRecipe());

        SlimefunItemStack chaoticCore = fusionOutput(
                "DRACFUN_CHAOTIC_CORE",
                Material.NETHER_STAR,
                "&5Chaotic Core",
                100_000_000);
        registered += registerUnplaceable(
                addon,
                materials,
                chaoticCore,
                RecipeType.NULL,
                emptyRecipe());

        registered += registerDragonHeart(addon);

        SlimefunItemStack smallChaos = stack(
                "DRACFUN_SMALL_CHAOS_FRAGMENT",
                Material.PRISMARINE_CRYSTALS,
                "&5Small Chaos Fragment");
        SlimefunItemStack largeChaos = stack(
                "DRACFUN_LARGE_CHAOS_FRAGMENT",
                Material.ECHO_SHARD,
                "&5Large Chaos Fragment");
        SlimefunItemStack chaosShard = stack(
                "DRACFUN_CHAOS_SHARD",
                Material.NETHER_STAR,
                "&5Chaos Shard");

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
            SlimefunItemStack customEgg = stack(
                    "DRACFUN_DRAGON_EGG",
                    Material.DRAGON_EGG,
                    "&5Draconic Dragon Egg");
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

        return registered;
    }

    public static int registerDragonHeart(SFDracFun2 addon) {
        String id = "DRACFUN_DRAGON_HEART";
        if (SlimefunItem.getById(id) != null) {
            return 0;
        }

        SlimefunItemStack dragonHeart = stack(
                id,
                Material.DRAGON_BREATH,
                "&5Dragon Heart");
        new DragonHeartItem(DracFunItemGroups.materials(addon), dragonHeart).register(addon);
        return 1;
    }

    private static ItemStack required(String id) {
        SlimefunItem item = SlimefunItem.getById(id);
        if (item == null) {
            throw new IllegalStateException(
                    "Required shared DracFun progression item is not registered: " + id);
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
        new EnergyCorePieceItem(group, item, type, recipe).register(addon);
        return 1;
    }

    private static SlimefunItemStack stack(
            String id,
            Material material,
            String name) {
        return new SlimefunItemStack(
                id,
                material,
                name,
                "&8DracFun Reborn clean-room item");
    }

    private static SlimefunItemStack fusionOutput(
            String id,
            Material material,
            String name,
            int fusionPower) {
        SlimefunItemStack stack = stack(id, material, name);
        ItemMeta meta = stack.getItemMeta();
        meta.getPersistentDataContainer().set(
                LegacyDracFunKeys.FUSION_POWER,
                PersistentDataType.INTEGER,
                fusionPower);
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack[] center(ItemStack item) {
        return recipe(
                null, null, null,
                null, item, null,
                null, null, null);
    }

    private static ItemStack[] fill(ItemStack item) {
        return recipe(
                item, item, item,
                item, item, item,
                item, item, item);
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
