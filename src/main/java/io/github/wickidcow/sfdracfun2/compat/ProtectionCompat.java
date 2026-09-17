package io.github.wickidcow.sfdracfun2.compat;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.lang.reflect.Method;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Accesses Slimefun's Dough-backed protection manager without forcing addons to compile against Dough.
 */
public final class ProtectionCompat {

    private ProtectionCompat() {}

    public static boolean canInteract(Player player, Location location) {
        if (player.hasPermission("slimefun.inventory.bypass")) {
            return true;
        }

        try {
            Method getter = Slimefun.class.getMethod("getProtectionManager");
            Object manager = getter.invoke(null);
            if (manager == null) {
                return false;
            }

            ClassLoader loader = manager.getClass().getClassLoader();
            Class<?> interactionClass = Class.forName(
                    "io.github.bakedlibs.dough.protection.Interaction", false, loader);
            @SuppressWarnings({"rawtypes", "unchecked"})
            Object interaction = Enum.valueOf(
                    (Class<? extends Enum>) interactionClass.asSubclass(Enum.class), "INTERACT_BLOCK");
            Method hasPermission = manager.getClass().getMethod(
                    "hasPermission", Player.class, Location.class, interactionClass);
            return Boolean.TRUE.equals(hasPermission.invoke(manager, player, location, interaction));
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }
}
