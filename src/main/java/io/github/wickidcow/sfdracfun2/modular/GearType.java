package io.github.wickidcow.sfdracfun2.modular;

/** Observable DracFun gear families and their legacy module-point budgets. */
public enum GearType {
    ARMOR("ARMOR", 30, 48, 80),
    AXE("AXE", 16, 30, 48),
    BOW("BOW", 16, 30, 48),
    CAPACITOR("FLUX_CAPACITOR", 16, 25, 48),
    HOE("HOE", 16, 30, 48),
    PICKAXE("PICKAXE", 16, 30, 48),
    SHOVEL("SHOVEL", 16, 30, 48),
    STAFF("STAFF_OF_POWER", 0, 48, 80),
    SWORD("SWORD", 16, 30, 48),
    TOOL("TOOL", 16, 30, 48),
    ALL("ALL_GEAR", 0, 0, 0);

    private final String legacyName;
    private final int[] modulePoints;

    GearType(String legacyName, int tier1Points, int tier2Points, int tier3Points) {
        this.legacyName = legacyName;
        this.modulePoints = new int[] {0, tier1Points, tier2Points, tier3Points};
    }

    public String legacyName() {
        return legacyName;
    }

    public int maxModulePoints(int gearTier) {
        if (gearTier < 1 || gearTier > 3) {
            throw new IllegalArgumentException("Gear tier must be 1-3, got " + gearTier);
        }
        return modulePoints[gearTier];
    }

    /** Matches the compatibility table used by DracFun 2.0.10's Module Integrater. */
    public boolean accepts(ModuleFamily family) {
        GearType target = family.targetType();
        if (target == ALL) {
            return true;
        }

        return switch (this) {
            case ARMOR -> target == ARMOR;
            case AXE -> target == AXE || target == SWORD;
            case BOW -> target == BOW;
            case CAPACITOR -> false;
            case HOE, PICKAXE, SHOVEL, TOOL -> target == TOOL;
            case SWORD -> target == SWORD;
            case STAFF -> target == SWORD || target == TOOL;
            case ALL -> target == ALL;
        };
    }

    public String legacyItemId(int gearTier) {
        String prefix = switch (gearTier) {
            case 1 -> "WYVERN";
            case 2 -> "DRACONIC";
            case 3 -> "CHAOTIC";
            default -> throw new IllegalArgumentException("Gear tier must be 1-3, got " + gearTier);
        };

        // The type is named STAFF_OF_POWER internally, but DracFun 2.0.10's
        // registered Slimefun IDs were DRACFUN_DRACONIC_STAFF and
        // DRACFUN_CHAOTIC_STAFF.
        String itemSuffix = this == STAFF ? "STAFF" : legacyName;
        return "DRACFUN_" + prefix + '_' + itemSuffix;
    }
}
