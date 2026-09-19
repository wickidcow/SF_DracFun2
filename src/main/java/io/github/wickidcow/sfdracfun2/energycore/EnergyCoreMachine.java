package io.github.wickidcow.sfdracfun2.energycore;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.ASlimefunDataContainer;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Restart-safe, per-block Energy Core implementation.
 *
 * <p>DracFun 2.0.10 used one static {@code notComplete} boolean shared by every
 * Energy Core instance. Breaking one core could therefore affect unrelated cores.
 * Reborn stores completeness on each activator's own block-data record and validates
 * that individual multiblock at the actual EnergyNet charge boundary.</p>
 */
public final class EnergyCoreMachine extends SlimefunItem implements EnergyNetComponent {

    private static final String DATA_COMPLETE = "reborn-energy-core-complete";
    private static final String ENERGY_CHARGE = "energy-charge";

    private final EnergyCoreTier tier;
    private final Map<BlockKey, ValidationStamp> validationCache = new ConcurrentHashMap<>();

    public EnergyCoreMachine(
            ItemGroup group,
            SlimefunItemStack item,
            EnergyCoreTier tier,
            ItemStack[] recipe) {
        super(group, item, RecipeType.ENHANCED_CRAFTING_TABLE, recipe);
        this.tier = Objects.requireNonNull(tier, "tier");

        addItemHandler(new BlockPlaceHandler(false) {
            @Override
            public void onPlayerPlace(@Nonnull BlockPlaceEvent event) {
                EnergyCoreStructure.Validation validation = tier.structure().validate(event.getBlock().getLocation());
                if (validation == EnergyCoreStructure.Validation.COMPLETE) {
                    event.getPlayer().sendMessage(ChatColor.GREEN
                            + tier.displayName()
                            + " Energy Core structure detected. Energy storage will activate on the next core tick.");
                } else if (validation == EnergyCoreStructure.Validation.INCOMPLETE) {
                    event.getPlayer().sendMessage(ChatColor.YELLOW
                            + tier.displayName()
                            + " Energy Core placed, but its multiblock structure is incomplete.");
                }
            }
        });

        addItemHandler(new BlockBreakHandler(false, false) {
            @Override
            public void onPlayerBreak(
                    @Nonnull BlockBreakEvent event,
                    @Nonnull ItemStack item,
                    @Nonnull java.util.List<ItemStack> drops) {
                validationCache.remove(BlockKey.of(event.getBlock().getLocation()));
            }
        });

        addItemHandler(new BlockTicker() {
            @Override
            public boolean isSynchronized() {
                return true;
            }

            @Override
            public void tick(Block block, SlimefunItem item, SlimefunBlockData data) {
                validateCore(block.getLocation(), data);
            }
        });
    }

    private EnergyCoreStructure.Validation validateCore(
            Location location,
            ASlimefunDataContainer data) {
        long gameTime = location.getWorld().getGameTime();
        BlockKey key = BlockKey.of(location);
        ValidationStamp cached = validationCache.get(key);
        if (cached != null && cached.gameTime() == gameTime) {
            return cached.validation();
        }

        EnergyCoreStructure.Validation validation = tier.structure().validate(location);
        validationCache.put(key, new ValidationStamp(gameTime, validation));

        if (validation == EnergyCoreStructure.Validation.UNKNOWN) {
            // Never destroy charge solely because a neighboring chunk/data record is not loaded.
            return validation;
        }

        boolean complete = validation == EnergyCoreStructure.Validation.COMPLETE;
        data.setData(DATA_COMPLETE, Boolean.toString(complete));
        if (!complete) {
            clearStoredCharge(location, data);
        }
        return validation;
    }

    static boolean invalidateImmediately(Location location) {
        SlimefunBlockData data = io.github.thebusybiscuit.slimefun4.implementation.Slimefun
                .getDatabaseManager()
                .getBlockDataController()
                .getBlockData(location);
        if (data == null || !data.isDataLoaded() || data.isPendingRemove()) {
            return false;
        }

        String id = data.getSfId();
        if (id == null || !id.contains("DRACFUN_ENERGY_CORE_ACTIVATOR")) {
            return false;
        }

        boolean wasComplete = "true".equals(data.getData(DATA_COMPLETE));
        data.setData(DATA_COMPLETE, "false");
        data.setData(ENERGY_CHARGE, "0");
        SlimefunUtils.updateCapacitorTexture(location, 0D);
        return wasComplete;
    }

    private void clearStoredCharge(Location location, ASlimefunDataContainer data) {
        String raw = data.getData(ENERGY_CHARGE);
        if (raw != null && !"0".equals(raw)) {
            data.setData(ENERGY_CHARGE, "0");
            SlimefunUtils.updateCapacitorTexture(location, 0D);
        }
    }

    private boolean canTransferEnergy(
            Location location,
            ASlimefunDataContainer data) {
        EnergyCoreStructure.Validation validation = validateCore(location, data);
        if (validation == EnergyCoreStructure.Validation.INCOMPLETE) {
            return false;
        }

        // UNKNOWN retains the last-known valid state while neighboring chunks load.
        return "true".equals(data.getData(DATA_COMPLETE));
    }

    @Override
    public boolean isEnergyNetActive(
            @Nonnull Location location,
            @Nonnull ASlimefunDataContainer data) {
        return canTransferEnergy(location, data);
    }

    @Override
    public long getChargeLong(
            @Nonnull Location location,
            @Nonnull ASlimefunDataContainer data) {
        if (!canTransferEnergy(location, data)) {
            return 0L;
        }
        return EnergyNetComponent.super.getChargeLong(location, data);
    }

    @Override
    public void setCharge(
            @Nonnull Location location,
            long charge,
            @Nonnull ASlimefunDataContainer data) {
        if (!canTransferEnergy(location, data)) {
            clearStoredCharge(location, data);
            return;
        }
        EnergyNetComponent.super.setCharge(location, charge, data);
    }

    @Override
    public void addCharge(
            @Nonnull Location location,
            long charge,
            @Nonnull ASlimefunDataContainer data) {
        if (!canTransferEnergy(location, data)) {
            clearStoredCharge(location, data);
            return;
        }
        EnergyNetComponent.super.addCharge(location, charge, data);
    }

    @Override
    public void removeCharge(
            @Nonnull Location location,
            long charge,
            @Nonnull ASlimefunDataContainer data) {
        if (!canTransferEnergy(location, data)) {
            clearStoredCharge(location, data);
            return;
        }
        EnergyNetComponent.super.removeCharge(location, charge, data);
    }

    @Override
    public @Nonnull EnergyNetComponentType getEnergyComponentType() {
        return EnergyNetComponentType.CAPACITOR;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getCapacity() {
        return tier.capacity();
    }

    public EnergyCoreTier getTier() {
        return tier;
    }

    private record BlockKey(UUID worldId, int x, int y, int z) {
        private static BlockKey of(Location location) {
            return new BlockKey(
                    location.getWorld().getUID(),
                    location.getBlockX(),
                    location.getBlockY(),
                    location.getBlockZ());
        }
    }

    private record ValidationStamp(
            long gameTime,
            EnergyCoreStructure.Validation validation) {}
}
