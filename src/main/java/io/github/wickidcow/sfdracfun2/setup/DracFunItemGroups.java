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
        ItemStack icon = new ItemStack(Material.NETHERITE_INGOT);
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + "DracFun Materials");
        icon.setItemMeta(meta);

        return new ItemGroup(new NamespacedKey(addon, "materials"), icon);
    }
}
