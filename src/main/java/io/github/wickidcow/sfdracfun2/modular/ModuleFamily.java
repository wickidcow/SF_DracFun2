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
    AOE("DRACFUN_AOE", 1, 3, GearType.TOOL, tiers(ModuleTier.BASIC, ModuleTier.WYVERN, ModuleTier.DRACONIC, ModuleTier.CHAOTIC)),
    ARROW_DAMAGE("DRACFUN_ARROW_DAMAGE", -1, 1, GearType.BOW, tiers(ModuleTier.WYVERN, ModuleTier.DRACONIC, ModuleTier.CHAOTIC)),
    ARROW_GRAVITY("DRACFUN_ARROW_GRAVITY", -1, 2, GearType.BOW, tiers(ModuleTier.WYVERN, ModuleTier.DRACONIC, ModuleTier.CHAOTIC)),
    ARROW_IMMUNITY("DRACFUN_ARROW_IMMUNITY", 1, 2, GearType.ARMOR, tiers(ModuleTier.DRACONIC)),
    ARROW_PENETRATION("DRACFUN_ARROW_PENETRATION", -1, 2, GearType.BOW, tiers(ModuleTier.WYVERN, ModuleTier.DRACONIC, ModuleTier.CHAOTIC)),
    ARROW_SPEED("DRACFUN_ARROW_SPEED", 8, 1, GearType.BOW, tiers(ModuleTier.WYVERN, ModuleTier.DRACONIC, ModuleTier.CHAOTIC)),
    DAMAGE("DRACFUN_DAMAGE", -1, 1, GearType.SWORD, tiers(ModuleTier.BASIC, ModuleTier.WYVERN, ModuleTier.DRACONIC, ModuleTier.CHAOTIC)),
    POWER("DRACFUN_POWER", -1, 1, GearType.ALL, tiers(ModuleTier.BASIC, ModuleTier.WYVERN, ModuleTier.DRACONIC, ModuleTier.CHAOTIC)),
    FLIGHT("DRACFUN_FLIGHT", 1, 3, GearType.ARMOR, tiers(ModuleTier.WYVERN, ModuleTier.DRACONIC, ModuleTier.CHAOTIC)),
    AUTO_FEED("DRACFUN_AUTO_FEED", 1, 2, GearType.ARMOR, tiers(ModuleTier.BASIC, ModuleTier.WYVERN, ModuleTier.DRACONIC, ModuleTier.CHAOTIC)),
    HARVEST("DRACFUN_HARVEST", 1, 2, GearType.AXE, tiers(ModuleTier.WYVERN, ModuleTier.DRACONIC, ModuleTier.CHAOTIC)),
    JUMP("DRACFUN_JUMP", 3, 1, GearType.ARMOR, tiers(ModuleTier.BASIC, ModuleTier.WYVERN, ModuleTier.DRACONIC, ModuleTier.CHAOTIC)),
    SHIELD_CAPACITY("DRACFUN_SHIELD_CAPACITY", -1, 1, GearType.ARMOR, tiers(ModuleTier.WYVERN, ModuleTier.DRACONIC, ModuleTier.CHAOTIC)),
    SHIELD_CONTROL("DRACFUN_SHIELD_CONTROL", 1, 2, GearType.ARMOR, tiers(ModuleTier.WYVERN, ModuleTier.DRACONIC, ModuleTier.CHAOTIC)),
    SHIELD_RECOVERY("DRACFUN_SHIELD_RECOVERY", -1, 1, GearType.ARMOR, tiers(ModuleTier.WYVERN, ModuleTier.DRACONIC, ModuleTier.CHAOTIC)),
    SPEED("DRACFUN_SPEED", 8, 1, GearType.ARMOR, tiers(ModuleTier.BASIC, ModuleTier.WYVERN, ModuleTier.DRACONIC, ModuleTier.CHAOTIC)),
    UNDYING("DRACFUN_UNDYING", 1, 2, GearType.ARMOR, tiers(ModuleTier.WYVERN, ModuleTier.DRACONIC, ModuleTier.CHAOTIC)),
    VISION("DRACFUN_VISION", 1, 2, GearType.ARMOR, tiers(ModuleTier.WYVERN));

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
     * Original Slimefun item identity and installed-module PDC identity.
     *
     * <p>DracFun 2.0.10 builds both as family-first names, for example
     * DRACFUN_AOE_BASIC_MODULE and DRACFUN_POWER_WYVERN_MODULE.</p>
     */
    public String legacyItemId(ModuleTier tier) {
        requireSupported(tier);
        return legacyBaseId + '_' + tier.legacyName() + "_MODULE";
    }

    public String legacyDataKeyId(ModuleTier tier) {
        return legacyItemId(tier);
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
