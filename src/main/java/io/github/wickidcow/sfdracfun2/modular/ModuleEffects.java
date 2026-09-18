package io.github.wickidcow.sfdracfun2.modular;

import org.bukkit.inventory.ItemStack;

/**
 * Calculates observable DracFun 2.0.10 module effects from per-item persistent data.
 */
public final class ModuleEffects {

    private ModuleEffects() {}

    public static int aoe(ItemStack stack) {
        ModuleTier tier = ModularData.highestTier(stack, ModuleFamily.AOE);
        return tier == null ? 0 : tier.level() + 1;
    }

    public static int arrowDamage(ItemStack stack) {
        return weighted(stack, ModuleFamily.ARROW_DAMAGE, 0, 25, 50, 75);
    }

    public static boolean arrowGravity(ItemStack stack) {
        return enabled(stack, ModuleFamily.ARROW_GRAVITY);
    }

    public static boolean arrowImmunity(ItemStack stack) {
        return enabled(stack, ModuleFamily.ARROW_IMMUNITY);
    }

    public static int arrowPenetration(ItemStack stack) {
        return weighted(stack, ModuleFamily.ARROW_PENETRATION, 0, 25, 50, 75);
    }

    public static int arrowSpeed(ItemStack stack) {
        return weighted(stack, ModuleFamily.ARROW_SPEED, 0, 15, 35, 75);
    }

    public static int damage(ItemStack stack) {
        return weighted(stack, ModuleFamily.DAMAGE, 2, 4, 8, 16);
    }

    public static int flight(ItemStack stack) {
        return weighted(stack, ModuleFamily.FLIGHT, 0, 100, 200, 300);
    }

    public static int autoFeed(ItemStack stack) {
        return weighted(stack, ModuleFamily.AUTO_FEED, 40, 150, 400, 1_000);
    }

    public static int harvest(ItemStack stack) {
        return weighted(stack, ModuleFamily.HARVEST, 0, 16, 64, 128);
    }

    public static int jump(ItemStack stack) {
        return weighted(stack, ModuleFamily.JUMP, 25, 75, 125, 400);
    }

    public static int shieldCapacity(ItemStack stack) {
        return weighted(stack, ModuleFamily.SHIELD_CAPACITY, 0, 25, 50, 100);
    }

    public static int shieldCooldown(ItemStack stack) {
        return weighted(stack, ModuleFamily.SHIELD_CONTROL, 0, 15, 10, 5);
    }

    public static int shieldRecovery(ItemStack stack) {
        return weighted(stack, ModuleFamily.SHIELD_RECOVERY, 0, 1, 2, 5);
    }

    public static int speed(ItemStack stack) {
        return weighted(stack, ModuleFamily.SPEED, 10, 25, 50, 150);
    }

    public static boolean undying(ItemStack stack) {
        return enabled(stack, ModuleFamily.UNDYING);
    }

    public static boolean vision(ItemStack stack) {
        return enabled(stack, ModuleFamily.VISION);
    }

    public static boolean enabled(ItemStack stack, ModuleFamily family) {
        return ModularData.getFamilyCount(stack, family) > 0;
    }

    public static int weighted(
            ItemStack stack,
            ModuleFamily family,
            int basic,
            int wyvern,
            int draconic,
            int chaotic) {
        long value = 0;
        value += (long) ModularData.getModuleCount(stack, family, ModuleTier.BASIC) * basic;
        value += (long) ModularData.getModuleCount(stack, family, ModuleTier.WYVERN) * wyvern;
        value += (long) ModularData.getModuleCount(stack, family, ModuleTier.DRACONIC) * draconic;
        value += (long) ModularData.getModuleCount(stack, family, ModuleTier.CHAOTIC) * chaotic;
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) value;
    }
}
