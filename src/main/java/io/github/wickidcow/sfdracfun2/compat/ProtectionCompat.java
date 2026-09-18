package io.github.wickidcow.sfdracfun2.compat;

import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.lang.reflect.Method;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * Accesses Slimefun's Dough-backed protection manager without forcing addons to compile against Dough.
 */
public final class ProtectionCompat {

    private static final Method PROTECTION_GETTER = findProtectionGetter();

    private static volatile PermissionBridge interactBridge;
    private static volatile PermissionBridge breakBridge;
    private static volatile ActionBridge breakLogBridge;

    private ProtectionCompat() {}

    public static boolean canInteract(Player player, Location location) {
        if (player.hasPermission("slimefun.inventory.bypass")) {
            return true;
        }
        return hasPermission(player, location, "INTERACT_BLOCK", false);
    }

    public static boolean canBreak(Player player, Block block) {
        if (player.hasPermission("slimefun.inventory.bypass")) {
            return true;
        }
        return hasPermission(player, block, "BREAK_BLOCK", true);
    }

    public static void logBreak(Player player, Block block) {
        Object manager = protectionManager();
        if (manager == null) {
            return;
        }

        try {
            ActionBridge bridge = breakLogBridge;
            if (bridge == null || !bridge.managerType().isInstance(manager)) {
                bridge = resolveActionBridge(manager, block, "BREAK_BLOCK", "logAction");
                breakLogBridge = bridge;
            }

            if (bridge != null) {
                bridge.method().invoke(manager, player, block, bridge.interaction());
            }
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            // Logging is best-effort; permission checks remain fail-closed.
        }
    }

    private static boolean hasPermission(
            Player player,
            Object target,
            String interactionName,
            boolean breakPermission) {
        Object manager = protectionManager();
        if (manager == null) {
            return false;
        }

        try {
            PermissionBridge bridge = breakPermission ? breakBridge : interactBridge;
            if (bridge == null || !bridge.managerType().isInstance(manager)) {
                bridge = resolvePermissionBridge(manager, target, interactionName);
                if (breakPermission) {
                    breakBridge = bridge;
                } else {
                    interactBridge = bridge;
                }
            }

            if (bridge == null) {
                return false;
            }

            return Boolean.TRUE.equals(
                    bridge.method().invoke(manager, player, target, bridge.interaction()));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    private static Object protectionManager() {
        if (PROTECTION_GETTER == null) {
            return null;
        }

        try {
            return PROTECTION_GETTER.invoke(null);
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ignored) {
            return null;
        }
    }

    private static Method findProtectionGetter() {
        try {
            return Slimefun.class.getMethod("getProtectionManager");
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return null;
        }
    }

    private static PermissionBridge resolvePermissionBridge(
            Object manager,
            Object target,
            String interactionName) {
        for (Method method : manager.getClass().getMethods()) {
            if (!method.getName().equals("hasPermission") || method.getParameterCount() != 3) {
                continue;
            }

            Class<?>[] parameters = method.getParameterTypes();
            if (!parameters[0].isAssignableFrom(Player.class)
                    || !parameters[1].isInstance(target)
                    || !parameters[2].isEnum()) {
                continue;
            }

            Object interaction = enumConstant(parameters[2], interactionName);
            if (interaction != null) {
                return new PermissionBridge(
                        manager.getClass(),
                        method,
                        interaction);
            }
        }
        return null;
    }

    private static ActionBridge resolveActionBridge(
            Object manager,
            Object target,
            String interactionName,
            String methodName) {
        for (Method method : manager.getClass().getMethods()) {
            if (!method.getName().equals(methodName) || method.getParameterCount() != 3) {
                continue;
            }

            Class<?>[] parameters = method.getParameterTypes();
            if (!parameters[0].isAssignableFrom(Player.class)
                    || !parameters[1].isInstance(target)
                    || !parameters[2].isEnum()) {
                continue;
            }

            Object interaction = enumConstant(parameters[2], interactionName);
            if (interaction != null) {
                return new ActionBridge(
                        manager.getClass(),
                        method,
                        interaction);
            }
        }
        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object enumConstant(Class<?> enumType, String name) {
        try {
            return Enum.valueOf((Class<? extends Enum>) enumType.asSubclass(Enum.class), name);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private record PermissionBridge(
            Class<?> managerType,
            Method method,
            Object interaction) {}

    private record ActionBridge(
            Class<?> managerType,
            Method method,
            Object interaction) {}
}
