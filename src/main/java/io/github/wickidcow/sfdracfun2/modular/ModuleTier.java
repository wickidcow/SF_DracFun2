package io.github.wickidcow.sfdracfun2.modular;

/** Legacy DracFun module tiers. */
public enum ModuleTier {
    BASIC(0, "BASIC", 1_000),
    WYVERN(1, "WYVERN", 4_000),
    DRACONIC(2, "DRACONIC", 16_000),
    CHAOTIC(3, "CHAOTIC", 64_000);

    private final int level;
    private final String legacyName;
    private final int powerCapacity;

    ModuleTier(int level, String legacyName, int powerCapacity) {
        this.level = level;
        this.legacyName = legacyName;
        this.powerCapacity = powerCapacity;
    }

    public int level() {
        return level;
    }

    public String legacyName() {
        return legacyName;
    }

    public int powerCapacity() {
        return powerCapacity;
    }
}
