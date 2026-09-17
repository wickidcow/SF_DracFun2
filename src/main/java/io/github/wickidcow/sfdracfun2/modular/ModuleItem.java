package io.github.wickidcow.sfdracfun2.modular;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import org.bukkit.inventory.ItemStack;

/** One installable modular upgrade identity. */
public final class ModuleItem extends SlimefunItem implements NotPlaceable {

    private final ModuleFamily family;
    private final ModuleTier tier;

    public ModuleItem(ItemGroup group, SlimefunItemStack item, ModuleFamily family, ModuleTier tier) {
        super(group, item, RecipeType.NULL, new ItemStack[9]);
        this.family = family;
        this.tier = tier;
        setHidden(true);
    }

    public ModuleFamily getFamily() {
        return family;
    }

    public ModuleTier getTier() {
        return tier;
    }

    public int getPointCost() {
        return family.pointCost();
    }
}
