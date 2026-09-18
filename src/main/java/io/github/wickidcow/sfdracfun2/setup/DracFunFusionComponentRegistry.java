package io.github.wickidcow.sfdracfun2.setup;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.items.blocks.UnplaceableBlock;
import io.github.wickidcow.sfdracfun2.SFDracFun2;
import io.github.wickidcow.sfdracfun2.compat.LegacyTheme;
import io.github.wickidcow.sfdracfun2.items.DragonHeartMobDrop;
import io.github.wickidcow.sfdracfun2.modular.LegacyDracFunKeys;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/**
 * Restores the material/component identities that feed DracFun's Fusion progression.
 * Original observable display names, theme lore and vanilla material representations
 * are retained where applicable.
 */
public final class DracFunFusionComponentRegistry {

    private DracFunFusionComponentRegistry() {}

    public static int register(SFDracFun2 addon, boolean hardMode, boolean useDragonEgg) {
        ItemGroup materials = DracFunItemGroups.materials(addon);
        ItemGroup machines = DracFunItemGroups.machines(addon);

        int registered = registerParticleGenerator(addon, hardMode);

        ItemStack draconiumIngot = required("DRACFUN_DRACONIUM_INGOT");
        ItemStack draconiumBlock = required("DRACFUN_DRACONIUM_BLOCK");
        ItemStack draconium = hardMode ? draconiumBlock : draconiumIngot;
        ItemStack gold = hardMode ? SlimefunItems.GOLD_24K_BLOCK : SlimefunItems.GOLD_24K;
        ItemStack diamond = new ItemStack(hardMode ? Material.DIAMOND_BLOCK : Material.DIAMOND);
        ItemStack draconicCore = required("DRACFUN_DRACONIC_CORE");

        SlimefunItemStack wyvernCore = LegacyTheme.BASIC_CRAFTING.stack(
                "DRACFUN_WYVERN_CORE", Material.AMETHYST_SHARD, "Wyvern Core");
        SlimefunItemStack fusionCore = LegacyTheme.FUSION_CRAFTING.stack(
                "DRACFUN_FUSION_CRAFTING_CORE", Material.LODESTONE, "Fusion Crafting Core");
        SlimefunItemStack basicInjector = LegacyTheme.FUSION_CRAFTING.stack(
                "DRACFUN_BASIC_FUSION_CRAFTING_INJECTOR", Material.COPPER_BLOCK, "Basic Fusion Crafting Injector");
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

        SlimefunItemStack awakenedBlock = fusionOutput(
                LegacyTheme.ADVANCED_CRAFTING,
                "DRACFUN_AWAKENED_DRACONIUM_BLOCK",
                Material.WAXED_COPPER_BLOCK,
                "Awakened Draconium Block",
                50_000_000);
        SlimefunItemStack awakenedIngot = LegacyTheme.ADVANCED_CRAFTING.stack(
                "DRACFUN_AWAKENED_DRACONIUM_INGOT",
                Material.COPPER_INGOT,
                "Awakened Draconium Ingot");
        SlimefunItemStack awakenedNugget = LegacyTheme.ADVANCED_CRAFTING.stack(
                "DRACFUN_AWAKENED_DRACONIUM_NUGGET",
                Material.GOLD_NUGGET,
                "Awakened Draconium Nugget");
        SlimefunItemStack awakenedCore = fusionOutput(
                LegacyTheme.ADVANCED_CRAFTING,
                "DRACFUN_AWAKENED_CORE",
                Material.HEART_OF_THE_SEA,
                "Awakened Core",
                1_000_000);
        SlimefunItemStack chaoticCore = fusionOutput(
                LegacyTheme.END_GAME_CRAFTING,
                "DRACFUN_CHAOTIC_CORE",
                Material.NETHER_STAR,
                "Chaotic Core",
                100_000_000);

        SlimefunItemStack dragonHeart = LegacyTheme.MOB.stack(
                "DRACFUN_DRAGON_HEART",
                Material.DRAGON_BREATH,
                "Dragon Heart");
        SlimefunItemStack smallChaos = LegacyTheme.MOB.stack(
                "DRACFUN_SMALL_CHAOS_FRAGMENT",
                Material.BLACK_DYE,
                "Small Chaos Fragment");
        SlimefunItemStack largeChaos = LegacyTheme.MOB.stack(
                "DRACFUN_LARGE_CHAOS_FRAGMENT",
                Material.FLINT,
                "Large Chaos Fragment");
        SlimefunItemStack chaosShard = LegacyTheme.MOB.stack(
                "DRACFUN_CHAOS_SHARD",
                Material.NETHER_STAR,
                "Chaos Shard");

        registered += registerUnplaceable(
                addon,
                materials,
                wyvernCore,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                recipe(
                        draconium, draconicCore, draconium,
                        draconicCore, new ItemStack(Material.NETHER_STAR), draconicCore,
                        draconium, draconicCore, draconium));

        registered += registerUnplaceable(
                addon,
                machines,
                fusionCore,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                recipe(
                        new ItemStack(Material.LAPIS_BLOCK), diamond, new ItemStack(Material.LAPIS_BLOCK),
                        diamond, draconicCore, diamond,
                        new ItemStack(Material.LAPIS_BLOCK), diamond, new ItemStack(Material.LAPIS_BLOCK)));

        registered += registerUnplaceable(
                addon,
                machines,
                basicInjector,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                recipe(
                        diamond, draconicCore, diamond,
                        new ItemStack(Material.IRON_BLOCK), new ItemStack(Material.IRON_BLOCK), new ItemStack(Material.IRON_BLOCK),
                        new ItemStack(Material.IRON_BLOCK), new ItemStack(Material.IRON_BLOCK), new ItemStack(Material.IRON_BLOCK)));

        registered += registerUnplaceable(addon, machines, wyvernInjector, RecipeType.NULL, emptyRecipe());
        registered += registerUnplaceable(addon, machines, draconicInjector, RecipeType.NULL, emptyRecipe());
        registered += registerUnplaceable(addon, machines, chaoticInjector, RecipeType.NULL, emptyRecipe());

        registered += registerSimple(addon, materials, awakenedBlock, RecipeType.NULL, emptyRecipe());
        registered += registerUnplaceable(
                addon,
                materials,
                awakenedIngot,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                center(awakenedBlock),
                awakenedIngot.asQuantity(9));
        registered += registerUnplaceable(
                addon,
                materials,
                awakenedNugget,
                RecipeType.ENHANCED_CRAFTING_TABLE,
                center(awakenedIngot),
                awakenedNugget.asQuantity(9));
        registered += registerUnplaceable(addon, materials, awakenedCore, RecipeType.NULL, emptyRecipe());
        registered += registerUnplaceable(addon, materials, chaoticCore, RecipeType.NULL, emptyRecipe());

        if (SlimefunItem.getById(dragonHeart.getItemId()) == null) {
            new DragonHeartMobDrop(
                            materials,
                            dragonHeart,
                            center(enderDragonGuideIcon()))
                    .register(addon);
            registered++;
        }
        registered += registerUnplaceable(
                addon,
                materials,
                smallChaos,
                RecipeType.MAGIC_WORKBENCH,
                center(chaosShard),
                smallChaos.asQuantity(81));
        registered += registerUnplaceable(addon, materials, largeChaos, RecipeType.MAGIC_WORKBENCH, fill(smallChaos));
        registered += registerUnplaceable(addon, materials, chaosShard, RecipeType.MAGIC_WORKBENCH, fill(largeChaos));

        if (!useDragonEgg) {
            SlimefunItemStack customEgg = LegacyTheme.ADVANCED_CRAFTING.stack(
                    "DRACFUN_DRAGON_EGG",
                    Material.DRAGON_EGG,
                    "DracFun's Dragon Egg");
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
                Material.RED_STAINED_GLASS,
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

    private static ItemStack enderDragonGuideIcon() {
        ItemStack icon = new ItemStack(Material.DRAGON_HEAD);
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName("Ender Dragon");
        icon.setItemMeta(meta);
        return icon;
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

    private static SlimefunItemStack fusionOutput(
            LegacyTheme theme,
            String id,
            Material material,
            String name,
            int fusionPower) {
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
