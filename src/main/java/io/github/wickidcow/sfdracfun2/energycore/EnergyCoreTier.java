package io.github.wickidcow.sfdracfun2.energycore;

/** Observable multiblock Energy Core tiers from DracFun 2.0.10. */
public enum EnergyCoreTier {
    TIER_1("DRACFUN_ENERGY_CORE_ACTIVATOR", 48_000_000, EnergyCoreStructure.tier1()),
    TIER_2("DRACFUN_ENERGY_CORE_ACTIVATOR_2", 256_000_000, EnergyCoreStructure.tier2()),
    TIER_3("DRACFUN_ENERGY_CORE_ACTIVATOR_3", 1_600_000_000, EnergyCoreStructure.tier3());

    private final String activatorId;
    private final int capacity;
    private final EnergyCoreStructure structure;

    EnergyCoreTier(String activatorId, int capacity, EnergyCoreStructure structure) {
        this.activatorId = activatorId;
        this.capacity = capacity;
        this.structure = structure;
    }

    public String activatorId() {
        return activatorId;
    }

    public int capacity() {
        return capacity;
    }

    public EnergyCoreStructure structure() {
        return structure;
    }

    public String displayName() {
        return switch (this) {
            case TIER_1 -> "Tier 1";
            case TIER_2 -> "Tier 2";
            case TIER_3 -> "Tier 3";
        };
    }
}
