package io.github.wickidcow.sfdracfun2.setup;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.groups.NestedItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.groups.SubItemGroup;
import io.github.wickidcow.sfdracfun2.SFDracFun2;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

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

        ItemStack guideIcon = new ItemStack(Material.DRAGON_HEAD);
        ItemMeta guideMeta = guideIcon.getItemMeta();
        guideMeta.displayName(Component.text("DracFun", NamedTextColor.DARK_PURPLE));
        guideIcon.setItemMeta(guideMeta);

        dracFun = new NestedItemGroup(
                new NamespacedKey(addon, "dracfun_nested"),
                guideIcon);

        Component main = Component.text("DracFun", NamedTextColor.DARK_PURPLE);
        materials = subgroup(
                addon,
                "dracfun_material",
                Material.NETHERITE_INGOT,
                main.append(Component.text("Materials", NamedTextColor.GREEN));
        energyCore = subgroup(
                addon,
                "dracfun_energy_core",
                Material.BEACON,
                main.append(Component.text("EnergyCore", NamedTextColor.RED));
        wyvernGear = armorSubgroup(
                addon,
                "dracfun_wyvern_gear",
                Color.PURPLE,
                main.append(Component.text("WyvernGear", NamedTextColor.DARK_PURPLE));
        draconicGear = armorSubgroup(
                addon,
                "dracfun_draconic_gear",
                Color.ORANGE,
                main.append(Component.text("DraconicGear", NamedTextColor.GOLD));
        chaoticGear = armorSubgroup(
                addon,
                "dracfun_chaotic_gear",
                Color.BLACK,
                main.append(Component.text("ChaoticGear", NamedTextColor.DARK_GRAY));
        modules = subgroup(
                addon,
                "dracfun_module",
                Material.HEART_OF_THE_SEA,
                main.append(Component.text("Modules", NamedTextColor.AQUA));
        electric = subgroup(
                addon,
                "dracfun_electric",
                Material.RESPAWN_ANCHOR,
                main.append(Component.text("Electric", NamedTextColor.YELLOW));
        reactor = subgroup(
                addon,
                "dracfun_reactor",
                Material.CRYING_OBSIDIAN,
                main.append(Component.text("Reactor", NamedTextColor.GOLD));
    }

    private static SubItemGroup subgroup(
            SFDracFun2 addon,
            String key,
            Material material,
            Component displayName) {
        ItemStack icon = new ItemStack(material);
        ItemMeta meta = icon.getItemMeta();
        meta.displayName(displayName);
        icon.setItemMeta(meta);
        return new SubItemGroup(
                new NamespacedKey(addon, key),
                dracFun,
                icon);
    }
    private static SubItemGroup armorSubgroup(
            SFDracFun2 addon,
            String key,
            Color color,
            Component displayName) {
        ItemStack icon = new ItemStack(Material.LEATHER_HELMET);
        LeatherArmorMeta meta = (LeatherArmorMeta) icon.getItemMeta();
        meta.setColor(color);
        meta.displayName(displayName);
        icon.setItemMeta(meta);
        return new SubItemGroup(
                new NamespacedKey(addon, key),
                dracFun,
                icon);
    }

}
