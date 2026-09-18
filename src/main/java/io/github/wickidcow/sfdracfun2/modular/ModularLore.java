package io.github.wickidcow.sfdracfun2.modular;

import io.github.wickidcow.sfdracfun2.compat.LegacyTheme;
import java.util.Arrays;
import java.util.List;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Refreshes live modular state while preserving DracFun 2.0.10's observable
 * lore format and tier theme footer.
 */
public final class ModularLore {

    private ModularLore() {}

    public static void refresh(ItemStack stack, ModularGearItem gear) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }

        List<String> live = LegacyModularLore.liveGearLore(stack, gear);
        String[] themed = themeForTier(gear.getGearTier()).lore(live.toArray(String[]::new));
        meta.setLore(Arrays.asList(themed));
        stack.setItemMeta(meta);
    }

    private static LegacyTheme themeForTier(int tier) {
        return switch (tier) {
            case 1 -> LegacyTheme.WYVERN_MODULAR;
            case 2 -> LegacyTheme.DRACONIC_MODULAR;
            case 3 -> LegacyTheme.CHAOTIC_MODULAR;
            default -> throw new IllegalArgumentException("Gear tier must be 1-3, got " + tier);
        };
    }
}
