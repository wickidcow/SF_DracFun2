package io.github.wickidcow.sfdracfun2.items;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.ItemSetting;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.items.settings.IntRangeSetting;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.attributes.RandomMobDrop;
import org.bukkit.inventory.ItemStack;

/**
 * Clean-room restoration of DracFun 2.0.10's Dragon Heart mob-drop item.
 */
public final class DragonHeartMobDrop extends SlimefunItem implements NotPlaceable, RandomMobDrop {

    private final ItemSetting<Integer> chance;

    public DragonHeartMobDrop(
            ItemGroup group,
            SlimefunItemStack item,
            ItemStack[] recipe) {
        super(group, item, RecipeType.MOB_DROP, recipe);

        // Exact observable 2.0.10 setting: dragon-drop-chance, min 1,
        // default 100, max 100.
        this.chance = new IntRangeSetting(this, "dragon-drop-chance", 1, 100, 100);
        addItemSetting(chance);
    }

    @Override
    public int getMobDropChance() {
        return chance.getValue();
    }
}
