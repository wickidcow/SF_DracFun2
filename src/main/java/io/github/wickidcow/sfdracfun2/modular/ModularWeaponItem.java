package io.github.wickidcow.sfdracfun2.modular;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.WeaponUseHandler;

/** Sword/staff modular item with the legacy powered DAMAGE-module behavior. */
import org.bukkit.inventory.ItemStack;

public final class ModularWeaponItem extends ModularGearItem {

    public ModularWeaponItem(
            ItemGroup group,
            SlimefunItemStack item,
            GearType gearType,
            int gearTier,
            RecipeType recipeType,
            ItemStack[] recipe) {
        super(group, item, gearType, gearTier, recipeType, recipe);
        if (gearType != GearType.SWORD && gearType != GearType.STAFF) {
            throw new IllegalArgumentException("ModularWeaponItem only supports sword/staff gear");
        }

        addItemHandler((WeaponUseHandler) (event, player, stack) -> {
            if (!ModularGearState.canUsePoweredEffect(player, stack)) {
                return;
            }

            int bonusDamage = ModuleEffects.damage(stack);
            if (bonusDamage > 0) {
                event.setDamage(event.getDamage() + bonusDamage);
                ModularData.removeCharge(stack, 1);
                ModularLore.refresh(stack, this);
            }
        });
    }
}
