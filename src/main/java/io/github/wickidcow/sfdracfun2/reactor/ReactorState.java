package io.github.wickidcow.sfdracfun2.reactor;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Typed access to the observable DracFun 2.0.10 reactor block-data contract.
 *
 * <p>The DRACFUN_* keys are intentionally unchanged so an existing reactor can
 * be adopted by the clean-room implementation. Reborn-only runtime state uses
 * a separate key and does not overwrite unrelated legacy metadata.</p>
 */
public final class ReactorState {

    public static final String STATE = "DRACFUN_STATE";
    public static final String INITIALIZED = "DRACFUN_INITIALIZED";
    public static final String FAILSAFE = "DRACFUN_FAILSAFE";
    public static final String SHIELD_INPUT = "DRACFUN_SHIELD_INPUT";
    public static final String TEMPERATURE = "DRACFUN_TEMPERATURE";
    public static final String MIN_SATURATION = "DRACFUN_MIN_SATURATION";
    public static final String SATURATION = "DRACFUN_SATURATION";
    public static final String MAX_SATURATION = "DRACFUN_MAX_SATURATION";
    public static final String REACTABLE_FUEL = "DRACFUN_REACTABLE_FUEL";
    public static final String CONVERTED_FUEL = "DRACFUN_CONVERTED_FUEL";
    public static final String SHIELD_CHARGE = "DRACFUN_SHIELD_CHARGE";
    public static final String MAX_SHIELD_CHARGE = "DRACFUN_MAX_SHIELD_CHARGE";
    public static final String GENERATION_RATE = "DRACFUN_GENERATION_RATE";
    public static final String FIELD_INPUT_RATE = "DRACFUN_FIELD_INPUT_RATE";
    public static final String FUEL_USE_RATE = "DRACFUN_FUEL_USE_RATE";
    public static final String EXPLOSION_COUNTDOWN = "DRACFUN_EXPLOSION_COUNTDOWN";

    public static final String REBORN_PROGRESS = "reborn-reactor-progress";

    private ReactorState() {}

    public static void ensureDefaults(SlimefunBlockData data) {
        putIfMissing(data, STATE, ReactorPhase.COLD.legacyValue());
        putIfMissing(data, INITIALIZED, "false");
        putIfMissing(data, FAILSAFE, "false");
        putIfMissing(data, SHIELD_INPUT, "0");
        putIfMissing(data, TEMPERATURE, "0");
        putIfMissing(data, MIN_SATURATION, "0");
        putIfMissing(data, SATURATION, "0");
        putIfMissing(data, MAX_SATURATION, "0");
        putIfMissing(data, REACTABLE_FUEL, "0");
        putIfMissing(data, CONVERTED_FUEL, "0");
        putIfMissing(data, SHIELD_CHARGE, "0");
        putIfMissing(data, MAX_SHIELD_CHARGE, "0");
        putIfMissing(data, GENERATION_RATE, "0");
        putIfMissing(data, FIELD_INPUT_RATE, "0");
        putIfMissing(data, FUEL_USE_RATE, "0");
        putIfMissing(
                data,
                EXPLOSION_COUNTDOWN,
                Integer.toString(120 + ThreadLocalRandom.current().nextInt(120)));
        putIfMissing(data, REBORN_PROGRESS, "-1");
    }

    public static ReactorPhase phase(SlimefunBlockData data) {
        return ReactorPhase.fromLegacy(data.getData(STATE));
    }

    public static void phase(SlimefunBlockData data, ReactorPhase phase) {
        data.setData(STATE, phase.legacyValue());
    }

    public static boolean bool(SlimefunBlockData data, String key) {
        return Boolean.parseBoolean(data.getData(key));
    }

    public static void bool(SlimefunBlockData data, String key, boolean value) {
        data.setData(key, Boolean.toString(value));
    }

    public static int integer(SlimefunBlockData data, String key) {
        String value = data.getData(key);
        if (value == null || value.isBlank()) {
            return 0;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    public static void integer(SlimefunBlockData data, String key, int value) {
        data.setData(key, Integer.toString(value));
    }

    public static int addNonNegative(SlimefunBlockData data, String key, int amount) {
        long next = (long) integer(data, key) + amount;
        int clamped = (int) Math.max(0L, Math.min(Integer.MAX_VALUE, next));
        integer(data, key, clamped);
        return clamped;
    }

    public static int subtractNonNegative(SlimefunBlockData data, String key, int amount) {
        return addNonNegative(data, key, -Math.max(0, amount));
    }

    public static int progress(SlimefunBlockData data) {
        return integer(data, REBORN_PROGRESS);
    }

    public static void progress(SlimefunBlockData data, int progress) {
        integer(data, REBORN_PROGRESS, progress);
    }

    private static void putIfMissing(SlimefunBlockData data, String key, String value) {
        if (data.getData(key) == null) {
            data.setData(key, value);
        }
    }
}
