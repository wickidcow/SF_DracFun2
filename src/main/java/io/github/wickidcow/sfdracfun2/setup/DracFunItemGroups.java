package io.github.wickidcow.sfdracfun2.setup;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.wickidcow.sfdracfun2.SFDracFun2;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

/**
 * Legacy-compatible DracFun guide category.
 *
 * <p>DracFun 2.0.10 used {@code DRACFUN_GUIDE} as its guide/category icon
 * identity rather than as a normal craftable Slimefun item. Reborn keeps one
 * shared DracFun category so restored materials, modular gear and machines
 * appear together in the Slimefun guide.</p>
 */
public final class DracFunItemGroups {

    private static ItemGroup dracFun;

    private DracFunItemGroups() {}

    public static ItemGroup materials(SFDracFun2 addon) {
        return dracFun(addon);
    }

    public static ItemGroup modular(SFDracFun2 addon) {
        return dracFun(addon);
    }

    public static ItemGroup machines(SFDracFun2 addon) {
        return dracFun(addon);
    }

    private static ItemGroup dracFun(SFDracFun2 addon) {
        if (dracFun == null) {
            SlimefunItemStack guideIcon = new SlimefunItemStack(
                    "DRACFUN_GUIDE",
                    Material.DRAGON_HEAD,
                    "&5DracFun",
                    "&7DracFun Reborn progression, modular gear and machines.");
            dracFun = new ItemGroup(
                    new NamespacedKey(addon, "dracfun"),
                    guideIcon);
        }
        return dracFun;
    }
}
