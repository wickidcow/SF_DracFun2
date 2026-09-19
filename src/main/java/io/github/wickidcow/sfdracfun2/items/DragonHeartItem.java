package io.github.wickidcow.sfdracfun2.items;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.items.settings.IntRangeSetting;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.attributes.RandomMobDrop;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Restores DracFun 2.0.10's Ender Dragon drop path for the Dragon Heart. */
public final class DragonHeartItem extends SlimefunItem implements NotPlaceable, RandomMobDrop {

    private final IntRangeSetting dropChance;

    public DragonHeartItem(ItemGroup group, SlimefunItemStack item) {
        super(group, item, RecipeType.MOB_DROP, mobDropRecipe());
        dropChance = new IntRangeSetting(this, "dragon-drop-chance", 1, 100, 100);
        addItemSetting(dropChance);
    }

    @Override
    public int getMobDropChance() {
        return dropChance.getValue();
    }

    private static ItemStack[] mobDropRecipe() {
        ItemStack[] recipe = new ItemStack[9];
        ItemStack dragon = new ItemStack(Material.DRAGON_HEAD);
        ItemMeta meta = dragon.getItemMeta();
        meta.setDisplayName(ChatColor.WHITE + "Ender Dragon");
        dragon.setItemMeta(meta);
        recipe[4] = dragon;
        return recipe;
    }
}
