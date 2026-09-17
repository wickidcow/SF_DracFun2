package io.github.wickidcow.sfdracfun2.modular;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * Reads and writes the observable modular-item data format used by DracFun 2.0.10.
 *
 * <p>Unlike the old implementation, used module points are derived from each ItemStack's
 * persistent data instead of a mutable field on the singleton SlimefunItem instance.</p>
 */
public final class ModularData {

    private ModularData() {}

    public static int getCharge(ItemStack stack) {
        return getInt(stack, LegacyDracFunKeys.ENERGY);
    }

    public static int getCapacity(ItemStack stack) {
        return Math.max(0, getInt(stack, LegacyDracFunKeys.CAPACITY));
    }

    public static int getFusionPower(ItemStack stack) {
        return Math.max(0, getInt(stack, LegacyDracFunKeys.FUSION_POWER));
    }

    public static void setCharge(ItemStack stack, int charge) {
        int capacity = getCapacity(stack);
        setInt(stack, LegacyDracFunKeys.ENERGY, clamp(charge, 0, capacity));
    }

    public static void setCapacity(ItemStack stack, int capacity) {
        int safeCapacity = Math.max(0, capacity);
        setInt(stack, LegacyDracFunKeys.CAPACITY, safeCapacity);
        if (getCharge(stack) > safeCapacity) {
            setCharge(stack, safeCapacity);
        }
    }

    public static void setFusionPower(ItemStack stack, int fusionPower) {
        setInt(stack, LegacyDracFunKeys.FUSION_POWER, Math.max(0, fusionPower));
    }

    public static int addCharge(ItemStack stack, int amount) {
        if (amount <= 0) {
            return getCharge(stack);
        }
        setCharge(stack, getCharge(stack) + amount);
        return getCharge(stack);
    }

    public static int removeCharge(ItemStack stack, int amount) {
        if (amount <= 0) {
            return getCharge(stack);
        }
        setCharge(stack, getCharge(stack) - amount);
        return getCharge(stack);
    }

    public static int getModuleCount(ItemStack stack, ModuleFamily family, ModuleTier tier) {
        if (!family.supports(tier)) {
            return 0;
        }
        return Math.max(0, getInt(stack, LegacyDracFunKeys.key(family.legacyItemId(tier))));
    }

    public static int getFamilyCount(ItemStack stack, ModuleFamily family) {
        int total = 0;
        for (ModuleTier tier : family.supportedTiers()) {
            total += getModuleCount(stack, family, tier);
        }
        return total;
    }

    public static int getUsedModulePoints(ItemStack stack) {
        int total = 0;
        for (ModuleFamily family : ModuleFamily.values()) {
            total += getFamilyCount(stack, family) * family.pointCost();
        }
        return total;
    }

    public static ModuleInstallResult validateInstall(
            ItemStack stack,
            GearType gearType,
            int gearTier,
            ModuleFamily family,
            ModuleTier moduleTier) {
        if (!family.supports(moduleTier)) {
            return ModuleInstallResult.UNSUPPORTED_TIER;
        }
        if (moduleTier.level() > gearTier) {
            return ModuleInstallResult.MODULE_TIER_TOO_HIGH;
        }
        if (!gearType.accepts(family)) {
            return ModuleInstallResult.INCOMPATIBLE_GEAR;
        }
        if (family.installLimit() >= 0 && getFamilyCount(stack, family) >= family.installLimit()) {
            return ModuleInstallResult.FAMILY_LIMIT_REACHED;
        }
        int maxPoints = gearType.maxModulePoints(gearTier);
        if (getUsedModulePoints(stack) + family.pointCost() > maxPoints) {
            return ModuleInstallResult.MODULE_POINTS_EXCEEDED;
        }
        return ModuleInstallResult.VALID;
    }

    public static ModuleInstallResult installModule(
            ItemStack stack,
            GearType gearType,
            int gearTier,
            ModuleFamily family,
            ModuleTier moduleTier) {
        ModuleInstallResult result = validateInstall(stack, gearType, gearTier, family, moduleTier);
        if (result != ModuleInstallResult.VALID) {
            return result;
        }

        NamespacedKey key = LegacyDracFunKeys.key(family.legacyItemId(moduleTier));
        setInt(stack, key, getModuleCount(stack, family, moduleTier) + 1);
        if (family == ModuleFamily.POWER) {
            recalculatePowerCapacity(stack);
        }
        return ModuleInstallResult.VALID;
    }

    public static int recalculatePowerCapacity(ItemStack stack) {
        long capacity = 0;
        for (ModuleTier tier : ModuleFamily.POWER.supportedTiers()) {
            capacity += (long) getModuleCount(stack, ModuleFamily.POWER, tier) * tier.powerCapacity();
        }
        int safeCapacity = capacity > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) capacity;
        setCapacity(stack, safeCapacity);
        return safeCapacity;
    }

    public static void removeAllModules(ItemStack stack) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }

        PersistentDataContainer data = meta.getPersistentDataContainer();
        for (ModuleFamily family : ModuleFamily.values()) {
            for (ModuleTier tier : family.supportedTiers()) {
                data.remove(LegacyDracFunKeys.key(family.legacyItemId(tier)));
            }
        }
        data.remove(LegacyDracFunKeys.SHIELD);
        data.remove(LegacyDracFunKeys.COOLDOWN);
        data.set(LegacyDracFunKeys.ENERGY, PersistentDataType.INTEGER, 0);
        data.set(LegacyDracFunKeys.CAPACITY, PersistentDataType.INTEGER, 0);
        stack.setItemMeta(meta);
    }

    private static int getInt(ItemStack stack, NamespacedKey key) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return 0;
        }
        Integer value = meta.getPersistentDataContainer().get(key, PersistentDataType.INTEGER);
        return value == null ? 0 : value;
    }

    private static void setInt(ItemStack stack, NamespacedKey key, int value) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.getPersistentDataContainer().set(key, PersistentDataType.INTEGER, value);
        stack.setItemMeta(meta);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
