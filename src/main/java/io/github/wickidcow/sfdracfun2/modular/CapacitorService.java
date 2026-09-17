package io.github.wickidcow.sfdracfun2.modular;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.wickidcow.sfdracfun2.SFDracFun2;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/**
 * Transfers stored charge from modular capacitors to other DracFun modular gear.
 *
 * <p>Player inventory access runs exclusively through each player's entity scheduler,
 * which follows the player across Folia regions and worlds. This replaces the old
 * global online-player scan and removes its slot-order dependency.</p>
 */
public final class CapacitorService implements Listener {

    private static final long INITIAL_DELAY_TICKS = 20L;
    private static final long PERIOD_TICKS = 20L;

    private final SFDracFun2 plugin;
    private final Set<UUID> scheduled = ConcurrentHashMap.newKeySet();

    public CapacitorService(SFDracFun2 plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            schedule(player);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        schedule(event.getPlayer());
    }

    private void schedule(Player player) {
        UUID uuid = player.getUniqueId();
        if (!scheduled.add(uuid)) {
            return;
        }

        player.getScheduler().runAtFixedRate(
                plugin,
                task -> transferFor(player),
                () -> scheduled.remove(uuid),
                INITIAL_DELAY_TICKS,
                PERIOD_TICKS);
    }

    private void transferFor(Player player) {
        if (!player.isValid() || player.isDead()) {
            return;
        }

        PlayerInventory inventory = player.getInventory();
        ItemStack[] contents = inventory.getContents();

        for (int capacitorSlot = 0; capacitorSlot < contents.length; capacitorSlot++) {
            ItemStack capacitor = contents[capacitorSlot];
            if (!isCapacitor(capacitor) || ModularData.getCharge(capacitor) <= 0) {
                continue;
            }

            for (int targetSlot = 0; targetSlot < contents.length; targetSlot++) {
                if (targetSlot == capacitorSlot || ModularData.getCharge(capacitor) <= 0) {
                    continue;
                }

                ItemStack target = contents[targetSlot];
                SlimefunItem targetItem = SlimefunItem.getByItem(target);
                if (!(targetItem instanceof ModularGearItem gear) || gear.getGearType() == GearType.CAPACITOR) {
                    continue;
                }

                int capacity = ModularData.getCapacity(target);
                int current = ModularData.getCharge(target);
                if (capacity <= 0 || current >= capacity) {
                    continue;
                }

                int available = ModularData.getCharge(capacitor);
                int transfer = Math.min(available, capacity - current);
                if (transfer <= 0) {
                    continue;
                }

                ModularData.removeCharge(capacitor, transfer);
                ModularData.addCharge(target, transfer);
                ModularLore.refresh(capacitor, (ModularGearItem) SlimefunItem.getByItem(capacitor));
                ModularLore.refresh(target, gear);

                contents[capacitorSlot] = capacitor;
                contents[targetSlot] = target;
                inventory.setItem(capacitorSlot, capacitor);
                inventory.setItem(targetSlot, target);
            }
        }
    }

    private static boolean isCapacitor(ItemStack stack) {
        if (stack == null || stack.getType().isAir()) {
            return false;
        }
        SlimefunItem sfItem = SlimefunItem.getByItem(stack);
        return sfItem instanceof ModularGearItem gear && gear.getGearType() == GearType.CAPACITOR;
    }
}
