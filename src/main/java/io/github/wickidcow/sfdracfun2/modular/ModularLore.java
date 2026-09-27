package io.github.wickidcow.sfdracfun2.modular;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Renders the observable DracFun 2.0.10 modular lore from PDC-backed state. */
public final class ModularLore {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    private ModularLore() {}

    public static void refresh(ItemStack stack, ModularGearItem gear) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }

        meta.lore(LegacyModularLore.liveGearLore(stack, gear)
                .stream()
                .map(LEGACY::deserialize)
                .toList());
        stack.setItemMeta(meta);
    }
}
