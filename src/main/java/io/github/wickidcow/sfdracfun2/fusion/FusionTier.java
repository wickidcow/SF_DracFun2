package io.github.wickidcow.sfdracfun2.fusion;

/** Observable Fusion Crafter tiers and machine capacities from DracFun 2.0.10. */
public enum FusionTier {
    BASIC("DRACFUN_BASIC_FUSION_CRAFTER", 32_000),
    WYVERN("DRACFUN_WYVERN_FUSION_CRAFTER", 50_000_000),
    DRACONIC("DRACFUN_DRACONIC_FUSION_CRAFTER", Integer.MAX_VALUE),
    CHAOTIC("DRACFUN_CHAOTIC_FUSION_CRAFTER", Integer.MAX_VALUE);

    private final String machineId;
    private final int capacity;

    FusionTier(String machineId, int capacity) {
        this.machineId = machineId;
        this.capacity = capacity;
    }

    public String machineId() {
        return machineId;
    }

    public int capacity() {
        return capacity;
    }

    /**
     * DracFun 2.0.10 did not make Fusion tiers a simple cumulative ladder.
     * Basic only saw Basic recipes; Wyvern only saw Wyvern; Draconic saw
     * Wyvern + Draconic; Chaotic saw Wyvern + Draconic + Chaotic.
     */
    public boolean accepts(FusionTier recipeTier) {
        return switch (this) {
            case BASIC -> recipeTier == BASIC;
            case WYVERN -> recipeTier == WYVERN;
            case DRACONIC -> recipeTier == WYVERN || recipeTier == DRACONIC;
            case CHAOTIC -> recipeTier == WYVERN || recipeTier == DRACONIC || recipeTier == CHAOTIC;
        };
    }

    public String displayName() {
        return switch (this) {
            case BASIC -> "Basic";
            case WYVERN -> "Wyvern";
            case DRACONIC -> "Draconic";
            case CHAOTIC -> "Chaotic";
        };
    }
}
