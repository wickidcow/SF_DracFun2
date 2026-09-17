package io.github.wickidcow.sfdracfun2.modular;

import java.util.Locale;
import org.bukkit.NamespacedKey;

/**
 * Namespaced keys used by legacy DracFun item data.
 *
 * <p>The original addon created these keys from its plugin namespace, so compatibility
 * data must remain under {@code dracfun:*} even though this implementation uses a
 * different Java package and plugin name.</p>
 */
public final class LegacyDracFunKeys {

    public static final String NAMESPACE = "dracfun";

    public static final NamespacedKey ENERGY = key("DRACFUN_ENERGY");
    public static final NamespacedKey CAPACITY = key("DRACFUN_CAPACITY");

    private LegacyDracFunKeys() {}

    public static NamespacedKey key(String legacyId) {
        return new NamespacedKey(NAMESPACE, legacyId.toLowerCase(Locale.ROOT));
    }
}
