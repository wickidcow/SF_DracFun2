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
 * Reborn stores completeness on each activator's own block-data record and pauses
 * EnergyNet participation whenever that individual multiblock is incomplete.</p>
 */
public final class EnergyCoreMachine extends SlimefunItem implements EnergyNetComponent {

    private static final String DATA_COMPLETE = "reborn-energy-core-complete";

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

    private void validateCore(Location location, SlimefunBlockData data) {
        EnergyCoreStructure.Validation validation = tier.structure().validate(location);
        if (validation == EnergyCoreStructure.Validation.UNKNOWN) {
            // Never destroy charge solely because a neighboring chunk/data record is not loaded.
            return;
        }

        boolean complete = validation == EnergyCoreStructure.Validation.COMPLETE;
        boolean previous = Boolean.parseBoolean(data.getData(DATA_COMPLETE));

        if (complete) {
            if (!previous) {
                data.setData(DATA_COMPLETE, "true");
            }
            return;
        }

        if (previous || getChargeLong(location, data) > 0L) {
            setCharge(location, 0L, data);
        }
        if (previous || data.getData(DATA_COMPLETE) == null) {
            data.setData(DATA_COMPLETE, "false");
        }
    }

    @Override
    public boolean isEnergyNetActive(@Nonnull Location location, @Nonnull ASlimefunDataContainer data) {
        return "true".equals(data.getData(DATA_COMPLETE));
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
}
