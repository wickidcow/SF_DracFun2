package io.github.wickidcow.sfdracfun2.setup;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.wickidcow.sfdracfun2.SFDracFun2;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Item-group definitions owned by the clean-room implementation. */
public final class DracFunItemGroups {

    private DracFunItemGroups() {}

    public static ItemGroup materials(SFDracFun2 addon) {
        return group(addon, "materials", Material.NETHERITE_INGOT, "DracFun Materials");
    }

    public static ItemGroup modular(SFDracFun2 addon) {
        return group(addon, "modular", Material.SMITHING_TABLE, "DracFun Modular Gear");
    }

    private static ItemGroup group(SFDracFun2 addon, String key, Material material, String name) {
        ItemStack icon = new ItemStack(material);
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + name);
        icon.setItemMeta(meta);
        return new ItemGroup(new NamespacedKey(addon, key), icon);
    }
}
