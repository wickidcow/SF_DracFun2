package io.github.wickidcow.sfdracfun2.modular;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.attributes.Soulbound;
import org.bukkit.inventory.ItemStack;

/** Base item type for clean-room DracFun modular equipment. */
public class ModularGearItem extends SlimefunItem implements NotPlaceable, Soulbound {

    private final GearType gearType;
    private final int gearTier;

    public ModularGearItem(ItemGroup group, SlimefunItemStack item, GearType gearType, int gearTier) {
        super(group, item, RecipeType.NULL, new ItemStack[9]);
        this.gearType = gearType;
        this.gearTier = gearTier;
    }

    public GearType getGearType() {
        return gearType;
    }

    public int getGearTier() {
        return gearTier;
    }

    public int getMaxModulePoints() {
        return gearType.maxModulePoints(gearTier);
    }

    public int getFusionPowerCost() {
        return switch (gearTier) {
            case 1 -> 8_000_000;
            case 2 -> 32_000_000;
            case 3 -> 128_000_000;
            default -> throw new IllegalStateException("Unsupported modular gear tier " + gearTier);
        };
    }
}
