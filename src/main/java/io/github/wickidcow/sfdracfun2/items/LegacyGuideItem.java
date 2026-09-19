package io.github.wickidcow.sfdracfun2.items;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotConfigurable;
import org.bukkit.inventory.ItemStack;

/**
 * Restores DracFun 2.0.10's visible DRACFUN_GUIDE placeholder book.
 */
public final class LegacyGuideItem extends SlimefunItem implements NotConfigurable {

    public LegacyGuideItem(ItemGroup group, SlimefunItemStack item) {
        super(group, item, RecipeType.NULL, new ItemStack[9]);
    }
}
