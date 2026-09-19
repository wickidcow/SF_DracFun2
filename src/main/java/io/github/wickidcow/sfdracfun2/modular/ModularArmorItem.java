package io.github.wickidcow.sfdracfun2.modular;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.ProtectiveArmor;
import io.github.thebusybiscuit.slimefun4.core.attributes.ProtectionType;
import javax.annotation.Nonnull;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

/** Modular armor with the protection contract exposed by DracFun 2.0.10. */
public final class ModularArmorItem extends ModularGearItem implements ProtectiveArmor {

    private static final NamespacedKey ARMOR_SET_ID = LegacyDracFunKeys.key("DRACFUN_ARMOR");

    public ModularArmorItem(
            ItemGroup group,
            SlimefunItemStack item,
            int gearTier,
            RecipeType recipeType,
            ItemStack[] recipe) {
        super(group, item, GearType.ARMOR, gearTier, recipeType, recipe);
    }

    @Override
    public @Nonnull ProtectionType[] getProtectionTypes() {
        return ProtectionType.values();
    }

    @Override
    public boolean isFullSetRequired() {
        return false;
    }

    @Override
    public NamespacedKey getArmorSetId() {
        return ARMOR_SET_ID;
    }
}
