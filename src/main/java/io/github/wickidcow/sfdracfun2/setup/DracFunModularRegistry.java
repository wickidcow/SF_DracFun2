package io.github.wickidcow.sfdracfun2.setup;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.wickidcow.sfdracfun2.SFDracFun2;
import io.github.wickidcow.sfdracfun2.modular.GearType;
import io.github.wickidcow.sfdracfun2.modular.LegacyDracFunKeys;
import io.github.wickidcow.sfdracfun2.modular.ModularGearItem;
import io.github.wickidcow.sfdracfun2.modular.ModuleFamily;
import io.github.wickidcow.sfdracfun2.modular.ModuleItem;
import io.github.wickidcow.sfdracfun2.modular.ModuleTier;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataType;

/** Registers clean-room modular item identities without exposing unfinished recipes. */
public final class DracFunModularRegistry {

    private DracFunModularRegistry() {}

    public static int register(SFDracFun2 addon) {
        ItemGroup group = DracFunItemGroups.modular(addon);
        int registered = 0;

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
                registered += registerGear(addon, group, type, tier);
            }
        }

        registered += registerGear(addon, group, GearType.STAFF, 2);
        registered += registerGear(addon, group, GearType.STAFF, 3);

        for (ModuleFamily family : ModuleFamily.values()) {
            for (ModuleTier tier : family.supportedTiers()) {
                registered += registerModule(addon, group, family, tier);
            }
        }

        return registered;
    }

    private static int registerGear(SFDracFun2 addon, ItemGroup group, GearType type, int tier) {
        String id = type.legacyItemId(tier);
        if (SlimefunItem.getById(id) != null) {
            return 0;
        }

        ItemStack base = new ItemStack(materialFor(type));
        ItemMeta meta = base.getItemMeta();
        meta.setDisplayName(colorForTier(tier) + tierName(tier) + ' ' + displayName(type));
        meta.setUnbreakable(true);
        meta.getPersistentDataContainer().set(LegacyDracFunKeys.ENERGY, PersistentDataType.INTEGER, 0);
        meta.getPersistentDataContainer().set(LegacyDracFunKeys.CAPACITY, PersistentDataType.INTEGER, 0);
        meta.getPersistentDataContainer().set(
                LegacyDracFunKeys.FUSION_POWER,
                PersistentDataType.INTEGER,
                fusionPowerForTier(tier));

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Energy: 0 / 0");
        lore.add(ChatColor.GRAY + "Module Points: 0 / " + type.maxModulePoints(tier));
        lore.add(ChatColor.DARK_GRAY + "DracFun Reborn compatibility item");
        meta.setLore(lore);

        if (meta instanceof LeatherArmorMeta leather) {
            leather.setColor(switch (tier) {
                case 1 -> Color.PURPLE;
                case 2 -> Color.ORANGE;
                case 3 -> Color.BLACK;
                default -> Color.WHITE;
            });
        }

        base.setItemMeta(meta);
        SlimefunItemStack stack = new SlimefunItemStack(id, base);
        new ModularGearItem(group, stack, type, tier).register(addon);
        return 1;
    }

    private static int registerModule(
            SFDracFun2 addon,
            ItemGroup group,
            ModuleFamily family,
            ModuleTier tier) {
        String id = family.legacyItemId(tier);
        if (SlimefunItem.getById(id) != null) {
            return 0;
        }

        SlimefunItemStack stack = new SlimefunItemStack(
                id,
                materialFor(tier),
                colorForModule(tier) + moduleTierName(tier) + ' ' + moduleName(family) + " Module",
                "&7Module cost: &f" + family.pointCost(),
                "&7Target: &f" + family.targetType().legacyName(),
                "&8DracFun Reborn compatibility module");
        new ModuleItem(group, stack, family, tier).register(addon);
        return 1;
    }

    private static int fusionPowerForTier(int tier) {
        return switch (tier) {
            case 1 -> 8_000_000;
            case 2 -> 32_000_000;
            case 3 -> 128_000_000;
            default -> throw new IllegalArgumentException("Gear tier must be 1-3, got " + tier);
        };
    }

    private static Material materialFor(GearType type) {
        return switch (type) {
            case ARMOR -> Material.LEATHER_CHESTPLATE;
            case AXE -> Material.NETHERITE_AXE;
            case BOW -> Material.BOW;
            case CAPACITOR -> Material.REDSTONE_TORCH;
            case HOE -> Material.NETHERITE_HOE;
            case PICKAXE, TOOL -> Material.NETHERITE_PICKAXE;
            case SHOVEL -> Material.NETHERITE_SHOVEL;
            case STAFF -> Material.BLAZE_ROD;
            case SWORD -> Material.NETHERITE_SWORD;
            case ALL -> Material.NETHER_STAR;
        };
    }

    private static Material materialFor(ModuleTier tier) {
        return switch (tier) {
            case BASIC -> Material.REDSTONE;
            case WYVERN -> Material.AMETHYST_SHARD;
            case DRACONIC -> Material.ECHO_SHARD;
            case CHAOTIC -> Material.NETHER_STAR;
        };
    }

    private static ChatColor colorForTier(int tier) {
        return switch (tier) {
            case 1 -> ChatColor.LIGHT_PURPLE;
            case 2 -> ChatColor.GOLD;
            case 3 -> ChatColor.DARK_PURPLE;
            default -> ChatColor.WHITE;
        };
    }

    private static ChatColor colorForModule(ModuleTier tier) {
        return switch (tier) {
            case BASIC -> ChatColor.WHITE;
            case WYVERN -> ChatColor.LIGHT_PURPLE;
            case DRACONIC -> ChatColor.GOLD;
            case CHAOTIC -> ChatColor.DARK_PURPLE;
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
