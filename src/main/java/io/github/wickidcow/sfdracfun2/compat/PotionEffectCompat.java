package io.github.wickidcow.sfdracfun2.compat;

import java.util.Locale;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.potion.PotionEffectType;

/**
 * Resolves current potion effect registry entries while preserving legacy Bukkit names
 * used by DracFun 2.0.10.
 */
public final class PotionEffectCompat {

    private PotionEffectCompat() {}

    public static PotionEffectType find(String... names) {
        for (String name : names) {
            if (name == null || name.isBlank()) {
                continue;
            }

            PotionEffectType type = Registry.MOB_EFFECT.get(
                    NamespacedKey.minecraft(modernKey(name)));
            if (type != null) {
                return type;
            }
        }

        return null;
    }

    private static String modernKey(String name) {
        return switch (name.toUpperCase(Locale.ROOT)) {
            case "HARM", "INSTANT_DAMAGE" -> "instant_damage";
            case "CONFUSION", "NAUSEA" -> "nausea";
            case "SLOW", "SLOWNESS" -> "slowness";
            case "SLOW_DIGGING", "MINING_FATIGUE" -> "mining_fatigue";
            default -> name.toLowerCase(Locale.ROOT);
        };
    }
}
