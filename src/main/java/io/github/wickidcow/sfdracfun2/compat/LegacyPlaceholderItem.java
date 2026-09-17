package io.github.wickidcow.sfdracfun2.compat;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import org.bukkit.inventory.ItemStack;

/**
 * Inert registration used only while a legacy identity has not yet received its
 * clean-room functional implementation.
 */
final class LegacyPlaceholderItem extends SlimefunItem implements NotPlaceable {

    LegacyPlaceholderItem(ItemGroup group, SlimefunItemStack item) {
        super(group, item, RecipeType.NULL, new ItemStack[9]);
    }
}
