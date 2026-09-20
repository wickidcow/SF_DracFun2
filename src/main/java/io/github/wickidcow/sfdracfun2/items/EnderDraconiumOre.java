package io.github.wickidcow.sfdracfun2.items;

import io.github.thebusybiscuit.slimefun4.api.geo.GEOResource;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.ItemSetting;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.items.settings.IntRangeSetting;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.inventory.ItemStack;

/**
 * Modern GEO resource for the legacy Ender Draconium Ore identity.
 *
 * <p>The supply range mirrors the observable 2.0.10 defaults while using a
 * new vanilla-material representation instead of Phoenix's old custom head.</p>
 */
public final class EnderDraconiumOre extends SlimefunItem implements GEOResource, NotPlaceable {

    private final SlimefunItemStack stack;
    private final NamespacedKey key;
    private final ItemSetting<Integer> minOres;
    private final ItemSetting<Integer> maxOres;
    private final ItemSetting<Integer> bonusOres;
    private final ItemSetting<Integer> rarityDenominator;

    public EnderDraconiumOre(ItemGroup group, SlimefunItemStack stack, NamespacedKey key) {
        super(group, stack, RecipeType.GEO_MINER, new ItemStack[9]);
        this.stack = stack;
        this.key = key;
        this.minOres = new IntRangeSetting(this, "min-ores", 1, 8, 64);
        this.maxOres = new IntRangeSetting(this, "max-ores", 1, 12, 64);
        this.bonusOres = new IntRangeSetting(this, "bonus-ores", 1, 12, 64);
        this.rarityDenominator = new IntRangeSetting(this, "rarity-denominator", 1, 400, 1_000_000);
        addItemSetting(minOres, maxOres, bonusOres, rarityDenominator);
    }

    @Override
    public int getDefaultSupply(World.Environment environment, Biome biome) {
        if (environment != World.Environment.THE_END) {
            return 0;
        }

        int denominator = rarityDenominator.getValue();
        if (ThreadLocalRandom.current().nextInt(denominator) != 0) {
            return 0;
        }

        int min = minOres.getValue();
        int max = maxOres.getValue();
        if (max <= min) {
            return min;
        }
        return ThreadLocalRandom.current().nextInt(min, max);
    }

    @Override
    public int getMaxDeviation() {
        return bonusOres.getValue();
    }

    @Override
    public String getName() {
        return "Ender Draconium Ore";
    }

    @Override
    public ItemStack getItem() {
        return stack.clone();
    }

    @Override
    public boolean isObtainableFromGEOMiner() {
        return true;
    }

    @Override
    public NamespacedKey getKey() {
        return key;
    }
}
