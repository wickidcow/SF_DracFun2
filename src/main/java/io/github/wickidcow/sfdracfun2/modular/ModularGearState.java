package io.github.wickidcow.sfdracfun2.modular;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Shared runtime checks used by powered modular gear effects. */
public final class ModularGearState {

    private ModularGearState() {}

    public static boolean hasPower(ItemStack stack) {
        return ModularData.getCapacity(stack) > 0 && ModularData.getCharge(stack) > 0;
    }

    public static boolean isWearingModularArmor(Player player) {
        ItemStack chestplate = player.getInventory().getChestplate();
        return chestplate != null && SlimefunItem.getByItem(chestplate) instanceof ModularArmorItem;
    }

    /** Mirrors the observable DracFun 2.0.10 gate for active modular effects. */
    public static boolean canUsePoweredEffect(Player player, ItemStack stack) {
        return hasPower(stack) && isWearingModularArmor(player);
    }
}
