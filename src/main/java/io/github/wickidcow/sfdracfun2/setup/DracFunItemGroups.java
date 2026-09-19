package io.github.wickidcow.sfdracfun2.setup;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.items.groups.NestedItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.groups.SubItemGroup;
import io.github.wickidcow.sfdracfun2.SFDracFun2;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Restores DracFun 2.0.10's nested guide layout.
 *
 * <p>The original addon exposed one DracFun parent with eight child sections:
 * Materials, Energy Core, Wyvern Gear, Draconic Gear, Chaotic Gear, Modules,
 * Electric and Reactor.</p>
 */
public final class DracFunItemGroups {

    private static NestedItemGroup dracFun;
    private static SubItemGroup materials;
    private static SubItemGroup energyCore;
    private static SubItemGroup wyvernGear;
    private static SubItemGroup draconicGear;
    private static SubItemGroup chaoticGear;
    private static SubItemGroup modules;
    private static SubItemGroup electric;
    private static SubItemGroup reactor;

    private DracFunItemGroups() {}

    public static ItemGroup materials(SFDracFun2 addon) {
        ensure(addon);
        return materials;
    }

    public static ItemGroup energyCore(SFDracFun2 addon) {
        ensure(addon);
        return energyCore;
    }

    public static ItemGroup gear(SFDracFun2 addon, int tier) {
        ensure(addon);
        return switch (tier) {
            case 1 -> wyvernGear;
            case 2 -> draconicGear;
            case 3 -> chaoticGear;
            default -> throw new IllegalArgumentException("Gear tier must be 1-3, got " + tier);
        };
    }

    public static ItemGroup modules(SFDracFun2 addon) {
        ensure(addon);
        return modules;
    }

    public static ItemGroup electric(SFDracFun2 addon) {
        ensure(addon);
        return electric;
    }

    public static ItemGroup reactor(SFDracFun2 addon) {
        ensure(addon);
        return reactor;
    }

    /** Compatibility alias for older Reborn registry code. */
    public static ItemGroup modular(SFDracFun2 addon) {
        return modules(addon);
    }

    /** Compatibility alias for older Reborn registry code. */
    public static ItemGroup machines(SFDracFun2 addon) {
        return electric(addon);
    }

    public static boolean hasLegacyGuideCategory() {
        return dracFun != null;
    }

    private static void ensure(SFDracFun2 addon) {
        if (dracFun != null) {
            return;
        }

        SlimefunItemStack guideIcon = new SlimefunItemStack(
                "DRACFUN_GUIDE",
                Material.DRAGON_HEAD,
                "&5DracFun",
                "&7DracFun 2.0.10 progression restored for Slimefun Legacy.");

        dracFun = new NestedItemGroup(
                new NamespacedKey(addon, "dracfun_nested"),
                guideIcon);

        materials = subgroup(
                addon,
                "dracfun_material",
                Material.NETHERITE_INGOT,
                ChatColor.LIGHT_PURPLE + "DracFun Materials");
        energyCore = subgroup(
                addon,
                "dracfun_energy_core",
                Material.BEACON,
                ChatColor.AQUA + "DracFun Energy Core");
        wyvernGear = subgroup(
                addon,
                "dracfun_wyvern_gear",
                Material.IRON_CHESTPLATE,
                ChatColor.LIGHT_PURPLE + "Wyvern Gear");
        draconicGear = subgroup(
                addon,
                "dracfun_draconic_gear",
                Material.NETHERITE_CHESTPLATE,
                ChatColor.GOLD + "Draconic Gear");
        chaoticGear = subgroup(
                addon,
                "dracfun_chaotic_gear",
                Material.NETHER_STAR,
                ChatColor.DARK_PURPLE + "Chaotic Gear");
        modules = subgroup(
                addon,
                "dracfun_module",
                Material.AMETHYST_SHARD,
                ChatColor.LIGHT_PURPLE + "DracFun Modules");
        electric = subgroup(
                addon,
                "dracfun_electric",
                Material.REDSTONE_TORCH,
                ChatColor.RED + "DracFun Electric");
        reactor = subgroup(
                addon,
                "dracfun_reactor",
                Material.RESPAWN_ANCHOR,
                ChatColor.DARK_PURPLE + "Draconic Reactor");
    }

    private static SubItemGroup subgroup(
            SFDracFun2 addon,
            String key,
            Material material,
            String displayName) {
        ItemStack icon = new ItemStack(material);
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(displayName);
        icon.setItemMeta(meta);
        return new SubItemGroup(
                new NamespacedKey(addon, key),
                dracFun,
                icon);
    }
}
