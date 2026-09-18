package io.github.wickidcow.sfdracfun2.items;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotConfigurable;
import org.bukkit.inventory.ItemStack;

/**
 * Restores the observable behavior of DracFun 2.0.10's DRACFUN_GUIDE item.
 *
 * <p>The original guide item was a visible, uncraftable Book used as a future
 * help placeholder and implemented NotConfigurable so it did not appear in
 * Slimefun's Items.yml.</p>
 */
public final class LegacyGuideItem extends SlimefunItem implements NotConfigurable {

    public LegacyGuideItem(ItemGroup group, SlimefunItemStack item) {
        super(group, item, RecipeType.NULL, new ItemStack[9]);
    }
}
