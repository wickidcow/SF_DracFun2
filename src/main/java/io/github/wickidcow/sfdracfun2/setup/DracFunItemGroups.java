package io.github.wickidcow.sfdracfun2.setup;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.groups.NestedItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.groups.SubItemGroup;
import io.github.wickidcow.sfdracfun2.SFDracFun2;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

/**
 * Clean-room reconstruction of the observable DracFun 2.0.10 guide hierarchy.
 *
 * <p>The original addon exposed one nested DracFun category with eight child
 * groups: Materials, EnergyCore, WyvernGear, DraconicGear, ChaoticGear,
 * Modules, Electric and Reactor. The original custom head assets are not copied;
 * Reborn uses vanilla icons while preserving the guide structure and names.</p>
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
        ensureInitialized(addon);
        return materials;
    }

    public static ItemGroup energyCore(SFDracFun2 addon) {
        ensureInitialized(addon);
        return energyCore;
    }

    public static ItemGroup gear(SFDracFun2 addon, int tier) {
        ensureInitialized(addon);
        return switch (tier) {
            case 1 -> wyvernGear;
            case 2 -> draconicGear;
            case 3 -> chaoticGear;
            default -> throw new IllegalArgumentException("Gear tier must be 1-3, got " + tier);
        };
    }

    public static ItemGroup modules(SFDracFun2 addon) {
        ensureInitialized(addon);
        return modules;
    }

    /** Compatibility alias used by older Reborn registry code. */
    public static ItemGroup modular(SFDracFun2 addon) {
        return modules(addon);
    }

    public static ItemGroup electric(SFDracFun2 addon) {
        ensureInitialized(addon);
        return electric;
    }

    /** Compatibility alias used by Fusion and machine registry code. */
    public static ItemGroup machines(SFDracFun2 addon) {
        return electric(addon);
    }

    public static ItemGroup reactor(SFDracFun2 addon) {
        ensureInitialized(addon);
        return reactor;
    }

    public static boolean hasLegacyGuideCategory() {
        return dracFun != null
                && materials != null
                && energyCore != null
                && wyvernGear != null
                && draconicGear != null
                && chaoticGear != null
                && modules != null
                && electric != null
                && reactor != null;
    }

    private static void ensureInitialized(SFDracFun2 addon) {
        if (dracFun != null) {
            return;
        }

        dracFun = new NestedItemGroup(
                new NamespacedKey(addon, "dracfun_nested"),
                icon(Material.DRAGON_HEAD, ChatColor.DARK_PURPLE + "DracFun"));

        materials = new SubItemGroup(
                new NamespacedKey(addon, "dracfun_material"),
                dracFun,
                icon(Material.NETHERITE_INGOT, ChatColor.GREEN + "Materials"));
        energyCore = new SubItemGroup(
                new NamespacedKey(addon, "dracfun_energy_core"),
                dracFun,
                icon(Material.BEACON, ChatColor.RED + "EnergyCore"));
        wyvernGear = new SubItemGroup(
                new NamespacedKey(addon, "dracfun_wyvern_gear"),
                dracFun,
                armorIcon(Color.PURPLE, ChatColor.DARK_PURPLE + "WyvernGear"));
        draconicGear = new SubItemGroup(
                new NamespacedKey(addon, "dracfun_draconic_gear"),
                dracFun,
                armorIcon(Color.ORANGE, ChatColor.GOLD + "DraconicGear"));
        chaoticGear = new SubItemGroup(
                new NamespacedKey(addon, "dracfun_chaotic_gear"),
                dracFun,
                armorIcon(Color.BLACK, ChatColor.DARK_GRAY + "ChaoticGear"));
        modules = new SubItemGroup(
                new NamespacedKey(addon, "dracfun_module"),
                dracFun,
                icon(Material.HEART_OF_THE_SEA, ChatColor.AQUA + "Modules"));
        electric = new SubItemGroup(
                new NamespacedKey(addon, "dracfun_electric"),
                dracFun,
                icon(Material.RESPAWN_ANCHOR, ChatColor.YELLOW + "Electric"));
        reactor = new SubItemGroup(
                new NamespacedKey(addon, "dracfun_reactor"),
                dracFun,
                icon(Material.CRYING_OBSIDIAN, ChatColor.RED + "Reactor"));
    }

    private static ItemStack icon(Material material, String name) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(name);
        stack.setItemMeta(meta);
        return stack;
    }

    private static ItemStack armorIcon(Color color, String name) {
        ItemStack stack = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta meta = (LeatherArmorMeta) stack.getItemMeta();
        meta.setColor(color);
        meta.setDisplayName(name);
        stack.setItemMeta(meta);
        return stack;
    }
}
