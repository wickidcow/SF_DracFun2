package io.github.wickidcow.sfdracfun2.reactor;

import java.util.Locale;

/**
 * Observable DracFun 2.0.10 reactor states.
 *
 * <p>The persisted values intentionally keep the legacy STATE_* spelling so
 * existing block data can be read without a destructive migration.</p>
 */
public enum ReactorPhase {
    COLD("STATE_COLD"),
    WARMING_UP("STATE_WARMING_UP"),
    RUNNING("STATE_RUNNING"),
    STOPPING("STATE_STOPPING"),
    COOLING("STATE_COOLING"),
    BEYOND_HOPE("STATE_BEYOND_HOPE");

    private final String legacyValue;

    ReactorPhase(String legacyValue) {
        this.legacyValue = legacyValue;
    }

    public String legacyValue() {
        return legacyValue;
    }

    public static ReactorPhase fromLegacy(String value) {
        if (value == null || value.isBlank()) {
            return COLD;
        }

        for (ReactorPhase phase : values()) {
            if (phase.legacyValue.equalsIgnoreCase(value)) {
                return phase;
            }
        }

        try {
            return valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return COLD;
        }
    }
}
