package io.github.wickidcow.sfdracfun2.energycore;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.ASlimefunDataContainer;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import java.util.Objects;
import javax.annotation.Nonnull;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
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
    private static final String DATA_VALIDATED_TICK = "reborn-energy-core-validated-tick";
    private static final String ENERGY_CHARGE = "energy-charge";

    private final EnergyCoreTier tier;

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
        if (parseLong(data.getData(DATA_VALIDATED_TICK)) == gameTime) {
            return "true".equals(data.getData(DATA_COMPLETE))
                    ? EnergyCoreStructure.Validation.COMPLETE
                    : EnergyCoreStructure.Validation.INCOMPLETE;
        }

        EnergyCoreStructure.Validation validation = tier.structure().validate(location);
        data.setData(DATA_VALIDATED_TICK, Long.toString(gameTime));

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

    private static long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return Long.MIN_VALUE;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return Long.MIN_VALUE;
        }
    }
}
