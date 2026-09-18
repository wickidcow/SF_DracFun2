package io.github.wickidcow.sfdracfun2.setup;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.items.blocks.UnplaceableBlock;
import io.github.wickidcow.sfdracfun2.SFDracFun2;
import io.github.wickidcow.sfdracfun2.compat.LegacyTheme;
import io.github.wickidcow.sfdracfun2.modular.GearType;
import io.github.wickidcow.sfdracfun2.modular.LegacyDracFunKeys;
import io.github.wickidcow.sfdracfun2.modular.LegacyModuleRecipeCatalog;
import io.github.wickidcow.sfdracfun2.modular.LegacyModularLore;
import io.github.wickidcow.sfdracfun2.modular.ModularArmorItem;
import io.github.wickidcow.sfdracfun2.modular.ModularGearItem;
import io.github.wickidcow.sfdracfun2.modular.ModularWeaponItem;
import io.github.wickidcow.sfdracfun2.modular.ModuleFamily;
import io.github.wickidcow.sfdracfun2.modular.ModuleIntegratorMachine;
import io.github.wickidcow.sfdracfun2.modular.ModuleItem;
import io.github.wickidcow.sfdracfun2.modular.ModuleTier;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;

/** Registers clean-room modular items while preserving Fusion as their acquisition path. */
public final class DracFunModularRegistry {

    private DracFunModularRegistry() {}

    public static int register(
            SFDracFun2 addon,
            boolean hardMode,
            boolean useDragonEgg) {
        ItemGroup moduleGroup = DracFunItemGroups.modules(addon);
        int registered = 0;

        registered += DracFunSharedProgressionRegistry.register(
                addon,
                hardMode,
                useDragonEgg);
        registered += DracFunEnergyCoreRegistry.registerEnergyMaterials(
                addon,
                hardMode);
        registered += registerModuleCore(addon, moduleGroup, hardMode);

        for (GearType type : List.of(
                GearType.ARMOR,
                GearType.AXE,
                GearType.BOW,
                GearType.CAPACITOR,
                GearType.HOE,
                GearType.PICKAXE,
                GearType.SHOVEL,
                GearType.SWORD)) {
            for (int tier = 1; tier <= 3; tier++) {
                registered += registerGear(addon, DracFunItemGroups.gear(addon, tier), type, tier);
            }
        }

        registered += registerGear(addon, DracFunItemGroups.gear(addon, 2), GearType.STAFF, 2);
        registered += registerGear(addon, DracFunItemGroups.gear(addon, 3), GearType.STAFF, 3);

        for (ModuleFamily family : ModuleFamily.values()) {
            for (ModuleTier tier : ModuleTier.values()) {
                if (family.supports(tier)) {
                    registered += registerModule(
                            addon,
                            moduleGroup,
                            family,
                            tier,
                            hardMode);
                }
            }
        }

        registered += registerIntegrator(addon, moduleGroup, hardMode);
        return registered;
    }

    private static int registerModuleCore(
            SFDracFun2 addon,
            ItemGroup group,
            boolean hardMode) {
        String id = "DRACFUN_MODULE_CORE";
        if (SlimefunItem.getById(id) != null) {
            return 0;
        }

        ItemStack iron = new ItemStack(hardMode ? Material.IRON_BLOCK : Material.IRON_INGOT);
        ItemStack redstone = new ItemStack(hardMode ? Material.REDSTONE_BLOCK : Material.REDSTONE);
        ItemStack gold = hardMode ? SlimefunItems.GOLD_24K_BLOCK : SlimefunItems.GOLD_24K;
        ItemStack draconium = requiredItem(
                hardMode ? "DRACFUN_DRACONIUM_BLOCK" : "DRACFUN_DRACONIUM_INGOT");

        SlimefunItemStack stack = LegacyTheme.BASIC_MODULAR.stack(
                id,
                Material.HEART_OF_THE_SEA,
                "Module Core");

        new UnplaceableBlock(
                        group,
                        stack,
                        RecipeType.ANCIENT_ALTAR,
                        recipe(
                                iron, redstone, iron,
                                gold, draconium, gold,
                                iron, redstone, iron))
                .register(addon);
        return 1;
    }

    private static int registerGear(SFDracFun2 addon, ItemGroup group, GearType type, int tier) {
        String id = type.legacyItemId(tier);
        if (SlimefunItem.getById(id) != null) {
            return 0;
        }

        ItemStack base = new ItemStack(materialFor(type, tier));
        if (base.getItemMeta() instanceof LeatherArmorMeta leather) {
            leather.setColor(switch (tier) {
                case 1 -> Color.PURPLE;
                case 2 -> Color.ORANGE;
                case 3 -> Color.BLACK;
                default -> Color.WHITE;
            });
            base.setItemMeta(leather);
        }

        LegacyTheme theme = modularTheme(tier);
        List<String> originalLore = LegacyModularLore.defaultGearLore(type, tier);
        SlimefunItemStack stack = theme.stack(
                id,
                base,
                tierName(tier) + " Modular " + displayName(type),
                originalLore.toArray(String[]::new));

        ItemMeta meta = stack.getItemMeta();
        meta.setUnbreakable(true);
        meta.getPersistentDataContainer().set(LegacyDracFunKeys.ENERGY, PersistentDataType.INTEGER, 0);
        meta.getPersistentDataContainer().set(LegacyDracFunKeys.CAPACITY, PersistentDataType.INTEGER, 0);
        meta.getPersistentDataContainer().set(
                LegacyDracFunKeys.FUSION_POWER,
                PersistentDataType.INTEGER,
                fusionPowerForTier(tier));

        if (meta instanceof LeatherArmorMeta) {
            addArmorAttributes(addon, meta, tier);
        } else {
            addToolAttributes(addon, meta, type, tier);
        }

        stack.setItemMeta(meta);

        if (type == GearType.ARMOR) {
            new ModularArmorItem(group, stack, tier).register(addon);
        } else if (type == GearType.SWORD || type == GearType.STAFF) {
            new ModularWeaponItem(group, stack, type, tier).register(addon);
        } else {
            new ModularGearItem(group, stack, type, tier).register(addon);
        }
        return 1;
    }

    private static double tierModifier(int tier) {
        return switch (tier) {
            case 1 -> 1.25D;
            case 2 -> 1.75D;
            case 3 -> 2.50D;
            default -> throw new IllegalArgumentException("Gear tier must be 1-3, got " + tier);
        };
    }

    private static void addArmorAttributes(SFDracFun2 addon, ItemMeta meta, int tier) {
        double modifier = tierModifier(tier);
        AttributeModifier armor = new AttributeModifier(
                new NamespacedKey(addon, "modular_armor_" + tier),
                20.0D * modifier,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.CHEST);
        AttributeModifier toughness = new AttributeModifier(
                new NamespacedKey(addon, "modular_armor_toughness_" + tier),
                8.0D * modifier,
                AttributeModifier.Operation.ADD_NUMBER,
                EquipmentSlotGroup.CHEST);

        meta.addAttributeModifier(Attribute.ARMOR, armor);
        meta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, toughness);
    }

    private static void addToolAttributes(SFDracFun2 addon, ItemMeta meta, GearType type, int tier) {
        double baseSpeed;
        double baseDamage;
        switch (type) {
            case AXE -> {
                baseSpeed = 1.0D;
                baseDamage = 9.0D;
            }
            case HOE -> {
                baseSpeed = 4.0D;
                baseDamage = 1.0D;
            }
            case PICKAXE, SHOVEL, TOOL -> {
                baseSpeed = 1.1D;
                baseDamage = 5.25D;
            }
            case SWORD -> {
                baseSpeed = 1.6D;
                baseDamage = 7.0D;
            }
            case STAFF -> {
                baseSpeed = 0.5D;
                baseDamage = 9.0D;
            }
            default -> {
                return;
            }
        }

        double modifier = tierModifier(tier);
        String suffix = type.name().toLowerCase() + '_' + tier;
        meta.addAttributeModifier(
                Attribute.ATTACK_SPEED,
                new AttributeModifier(
                        new NamespacedKey(addon, "modular_attack_speed_" + suffix),
                        baseSpeed * modifier,
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.MAINHAND));
        meta.addAttributeModifier(
                Attribute.ATTACK_DAMAGE,
                new AttributeModifier(
                        new NamespacedKey(addon, "modular_attack_damage_" + suffix),
                        baseDamage * modifier,
                        AttributeModifier.Operation.ADD_NUMBER,
                        EquipmentSlotGroup.MAINHAND));
    }

    private static int registerModule(
            SFDracFun2 addon,
            ItemGroup group,
            ModuleFamily family,
            ModuleTier tier,
            boolean hardMode) {
        String id = family.legacyItemId(tier);
        if (SlimefunItem.getById(id) != null) {
            return 0;
        }

        ItemStack moduleIcon = new ItemStack(materialFor(tier));
        moduleIcon.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS);
        List<String> originalLore = LegacyModularLore.moduleLore(family, tier);
        SlimefunItemStack stack = moduleTheme(tier).stack(
                id,
                moduleIcon,
                moduleTierName(tier) + ' ' + moduleName(family) + " Module",
                originalLore.toArray(String[]::new));
        new ModuleItem(
                        group,
                        stack,
                        family,
                        tier,
                        RecipeType.ANCIENT_ALTAR,
                        LegacyModuleRecipeCatalog.recipe(
                                family,
                                tier,
                                hardMode))
                .register(addon);
        return 1;
    }

    private static int registerIntegrator(SFDracFun2 addon, ItemGroup group, boolean hardMode) {
        String id = "DRACFUN_MODULE_INTEGRATER";
        if (SlimefunItem.getById(id) != null) {
            return 0;
        }

        ItemStack diamond = new ItemStack(hardMode ? Material.DIAMOND_BLOCK : Material.DIAMOND);
        ItemStack moduleCore = requiredItem("DRACFUN_MODULE_CORE");
        ItemStack draconicCore = requiredItem("DRACFUN_DRACONIC_CORE");

        SlimefunItemStack stack = LegacyTheme.MACHINE.stack(
                id,
                Material.LODESTONE,
                "Module Integrater");
        new ModuleIntegratorMachine(
                        group,
                        stack,
                        RecipeType.ANCIENT_ALTAR,
                        recipe(
                                diamond, moduleCore, diamond,
                                moduleCore, draconicCore, moduleCore,
                                diamond, moduleCore, diamond))
                .register(addon);
        return 1;
    }

    private static ItemStack requiredItem(String id) {
        SlimefunItem item = SlimefunItem.getById(id);
        if (item == null) {
            throw new IllegalStateException("Required modular progression item is not registered: " + id);
        }
        return item.getItem().clone();
    }

    private static ItemStack[] recipe(
            ItemStack a, ItemStack b, ItemStack c,
            ItemStack d, ItemStack e, ItemStack f,
            ItemStack g, ItemStack h, ItemStack i) {
        return new ItemStack[] {a, b, c, d, e, f, g, h, i};
    }

    private static int fusionPowerForTier(int tier) {
        return switch (tier) {
            case 1 -> 8_000_000;
            case 2 -> 32_000_000;
            case 3 -> 128_000_000;
            default -> throw new IllegalArgumentException("Gear tier must be 1-3, got " + tier);
        };
    }

    private static Material materialFor(GearType type, int tier) {
        return switch (type) {
            case ARMOR -> Material.LEATHER_CHESTPLATE;
            case AXE -> tier == 3 ? Material.NETHERITE_AXE : Material.DIAMOND_AXE;
            case BOW -> Material.BOW;
            // The original capacitor icons were custom heads; Reborn keeps a
            // clean-room vanilla representation while preserving name/lore/behavior.
            case CAPACITOR -> Material.REDSTONE_TORCH;
            case HOE -> tier == 3 ? Material.NETHERITE_HOE : Material.DIAMOND_HOE;
            case PICKAXE, TOOL -> tier == 3 ? Material.NETHERITE_PICKAXE : Material.DIAMOND_PICKAXE;
            case SHOVEL -> tier == 3 ? Material.NETHERITE_SHOVEL : Material.DIAMOND_SHOVEL;
            case STAFF -> Material.TRIDENT;
            case SWORD -> tier == 3 ? Material.NETHERITE_SWORD : Material.DIAMOND_SWORD;
            case ALL -> Material.NETHER_STAR;
        };
    }

    private static Material materialFor(ModuleTier tier) {
        return switch (tier) {
            case BASIC -> Material.MUSIC_DISC_WAIT;
            case WYVERN -> Material.MUSIC_DISC_MALL;
            case DRACONIC -> Material.MUSIC_DISC_13;
            case CHAOTIC -> Material.MUSIC_DISC_STAL;
        };
    }

    private static LegacyTheme modularTheme(int tier) {
        return switch (tier) {
            case 1 -> LegacyTheme.WYVERN_MODULAR;
            case 2 -> LegacyTheme.DRACONIC_MODULAR;
            case 3 -> LegacyTheme.CHAOTIC_MODULAR;
            default -> throw new IllegalArgumentException("Gear tier must be 1-3, got " + tier);
        };
    }

    private static LegacyTheme moduleTheme(ModuleTier tier) {
        return switch (tier) {
            case BASIC -> LegacyTheme.BASIC_MODULAR;
            case WYVERN -> LegacyTheme.WYVERN_MODULAR;
            case DRACONIC -> LegacyTheme.DRACONIC_MODULAR;
            case CHAOTIC -> LegacyTheme.CHAOTIC_MODULAR;
        };
    }

    private static String tierName(int tier) {
        return switch (tier) {
            case 1 -> "Wyvern";
            case 2 -> "Draconic";
            case 3 -> "Chaotic";
            default -> "Unknown";
        };
    }

    private static String moduleTierName(ModuleTier tier) {
        return switch (tier) {
            case BASIC -> "Basic";
            case WYVERN -> "Wyvern";
            case DRACONIC -> "Draconic";
            case CHAOTIC -> "Chaotic";
        };
    }

    private static String displayName(GearType type) {
        return switch (type) {
            case ARMOR -> "Armor";
            case AXE -> "Axe";
            case BOW -> "Bow";
            case CAPACITOR -> "Flux Capacitor";
            case HOE -> "Hoe";
            case PICKAXE -> "Pickaxe";
            case SHOVEL -> "Shovel";
            case STAFF -> "Staff of Power";
            case SWORD -> "Sword";
            case TOOL -> "Tool";
            case ALL -> "Gear";
        };
    }

    private static String moduleName(ModuleFamily family) {
        String[] words = family.name().toLowerCase().split("_");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }
}
