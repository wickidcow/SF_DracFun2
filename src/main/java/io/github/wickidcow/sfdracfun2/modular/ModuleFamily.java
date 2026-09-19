package io.github.wickidcow.sfdracfun2.modular;

import java.util.EnumSet;
import java.util.Set;

/**
 * Observable DracFun 2.0.10 module families.
 *
 * <p>The point cost used by the old integrator is the square of the declared
 * module size: size 1 = 1 point, size 2 = 4 points, size 3 = 9 points.</p>
 */
public enum ModuleFamily {
    AOE("AOE", 1, 3, GearType.TOOL, tiers(ModuleTier.BASIC, ModuleTier.WYVERN, ModuleTier.DRACONIC, ModuleTier.CHAOTIC)),
    ARROW_DAMAGE("ARROW_DAMAGE", -1, 1, GearType.BOW, tiers(ModuleTier.WYVERN, ModuleTier.DRACONIC, ModuleTier.CHAOTIC)),
    ARROW_GRAVITY("ARROW_GRAVITY", -1, 2, GearType.BOW, tiers(ModuleTier.WYVERN, ModuleTier.DRACONIC, ModuleTier.CHAOTIC)),
    ARROW_IMMUNITY("ARROW_IMMUNITY", 1, 2, GearType.ARMOR, tiers(ModuleTier.DRACONIC)),
    ARROW_PENETRATION("ARROW_PENETRATION", -1, 2, GearType.BOW, tiers(ModuleTier.WYVERN, ModuleTier.DRACONIC, ModuleTier.CHAOTIC)),
    ARROW_SPEED("ARROW_SPEED", 8, 1, GearType.BOW, tiers(ModuleTier.WYVERN, ModuleTier.DRACONIC, ModuleTier.CHAOTIC)),
    DAMAGE("DAMAGE", -1, 1, GearType.SWORD, tiers(ModuleTier.BASIC, ModuleTier.WYVERN, ModuleTier.DRACONIC, ModuleTier.CHAOTIC)),
    POWER("ENERGY", -1, 1, GearType.ALL, tiers(ModuleTier.BASIC, ModuleTier.WYVERN, ModuleTier.DRACONIC, ModuleTier.CHAOTIC)),
    FLIGHT("FLIGHT", 1, 3, GearType.ARMOR, tiers(ModuleTier.WYVERN, ModuleTier.DRACONIC, ModuleTier.CHAOTIC)),
    AUTO_FEED("AUTO_FEED", 1, 2, GearType.ARMOR, tiers(ModuleTier.BASIC, ModuleTier.WYVERN, ModuleTier.DRACONIC, ModuleTier.CHAOTIC)),
    HARVEST("HARVEST", 1, 2, GearType.AXE, tiers(ModuleTier.WYVERN, ModuleTier.DRACONIC, ModuleTier.CHAOTIC)),
    JUMP("JUMP", 3, 1, GearType.ARMOR, tiers(ModuleTier.BASIC, ModuleTier.WYVERN, ModuleTier.DRACONIC, ModuleTier.CHAOTIC)),
    SHIELD_CAPACITY("SHIELD_CAPACITY", -1, 1, GearType.ARMOR, tiers(ModuleTier.WYVERN, ModuleTier.DRACONIC, ModuleTier.CHAOTIC)),
    SHIELD_CONTROL("SHIELD_CONTROL", 1, 2, GearType.ARMOR, tiers(ModuleTier.WYVERN, ModuleTier.DRACONIC, ModuleTier.CHAOTIC)),
    SHIELD_RECOVERY("SHIELD_RECOVERY", -1, 1, GearType.ARMOR, tiers(ModuleTier.WYVERN, ModuleTier.DRACONIC, ModuleTier.CHAOTIC)),
    SPEED("SPEED", 8, 1, GearType.ARMOR, tiers(ModuleTier.BASIC, ModuleTier.WYVERN, ModuleTier.DRACONIC, ModuleTier.CHAOTIC)),
    UNDYING("UNDYING", 1, 2, GearType.ARMOR, tiers(ModuleTier.WYVERN, ModuleTier.DRACONIC, ModuleTier.CHAOTIC)),
    VISION("VISION", 1, 2, GearType.ARMOR, tiers(ModuleTier.WYVERN));

    private final String legacyBaseId;
    private final int installLimit;
    private final int size;
    private final GearType targetType;
    private final Set<ModuleTier> supportedTiers;

    ModuleFamily(String legacyBaseId, int installLimit, int size, GearType targetType, Set<ModuleTier> supportedTiers) {
        this.legacyBaseId = legacyBaseId;
        this.installLimit = installLimit;
        this.size = size;
        this.targetType = targetType;
        this.supportedTiers = supportedTiers;
    }

    public String legacyBaseId() {
        return legacyBaseId;
    }

    public int installLimit() {
        return installLimit;
    }

    public int size() {
        return size;
    }

    public int pointCost() {
        return size * size;
    }

    public GearType targetType() {
        return targetType;
    }

    public boolean supports(ModuleTier tier) {
        return supportedTiers.contains(tier);
    }

    public Set<ModuleTier> supportedTiers() {
        return Set.copyOf(supportedTiers);
    }

    /**
     * Original Slimefun item identity. DracFun 2.0.10 used tier-first item IDs,
     * e.g. DRACFUN_BASIC_AOE_MODULE and DRACFUN_BASIC_ENERGY_MODULE.
     */
    public String legacyItemId(ModuleTier tier) {
        requireSupported(tier);
        return "DRACFUN_" + tier.legacyName() + '_' + legacyBaseId + "_MODULE";
    }

    /**
     * Original per-gear persistent-data identity.
     *
     * <p>These deliberately do not match the item IDs: the old addon stored
     * family-first keys such as DRACFUN_AOE_BASIC_MODULE. The POWER family also
     * stored DRACFUN_POWER_* even though its visible module item was named ENERGY.</p>
     */
    public String legacyDataKeyId(ModuleTier tier) {
        requireSupported(tier);
        return "DRACFUN_" + name() + '_' + tier.legacyName() + "_MODULE";
    }

    private void requireSupported(ModuleTier tier) {
        if (!supports(tier)) {
            throw new IllegalArgumentException(name() + " does not exist at tier " + tier);
        }
    }

    private static Set<ModuleTier> tiers(ModuleTier first, ModuleTier... remaining) {
        EnumSet<ModuleTier> set = EnumSet.of(first, remaining);
        return Set.copyOf(set);
    }
}
