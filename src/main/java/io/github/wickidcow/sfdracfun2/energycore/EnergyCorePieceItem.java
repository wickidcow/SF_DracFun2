package io.github.wickidcow.sfdracfun2.energycore;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Energy Core structure piece with the observable DracFun 2.0.10 break behavior.
 *
 * <p>Breaking a piece searches the same nearby volume used by the legacy addon
 * for an Energy Core activator. Reborn invalidates only that activator instead
 * of using the original global static completion flag.</p>
 */
public final class EnergyCorePieceItem extends SlimefunItem {

    public EnergyCorePieceItem(
            ItemGroup group,
            SlimefunItemStack item,
            RecipeType recipeType,
            ItemStack[] recipe) {
        super(group, item, recipeType, recipe);
        addBreakHandler();
    }

    public EnergyCorePieceItem(
            ItemGroup group,
            SlimefunItemStack item,
            RecipeType recipeType,
            ItemStack[] recipe,
            ItemStack output) {
        super(group, item, recipeType, recipe, output);
        addBreakHandler();
    }

    private void addBreakHandler() {
        addItemHandler(new BlockBreakHandler(false, false) {
            @Override
            public void onPlayerBreak(
                    BlockBreakEvent event,
                    ItemStack item,
                    java.util.List<ItemStack> drops) {
                Location piece = event.getBlock().getLocation();
                Location activator = findNearbyActivator(piece);
                if (activator != null && EnergyCoreMachine.invalidateImmediately(activator)) {
                    event.getPlayer().sendMessage(ChatColor.RED
                            + "You broke a part of the core! All stored energy has been expelled!");
                }
            }
        });
    }

    private static Location findNearbyActivator(Location piece) {
        World world = piece.getWorld();
        if (world == null) {
            return null;
        }

        for (int x = -3; x <= 3; x++) {
            for (int y = -7; y <= 0; y++) {
                for (int z = -3; z <= 3; z++) {
                    if (x == 0 && y == 0 && z == 0) {
                        continue;
                    }

                    int blockX = piece.getBlockX() + x;
                    int blockY = piece.getBlockY() + y;
                    int blockZ = piece.getBlockZ() + z;
                    if (!world.isChunkLoaded(blockX >> 4, blockZ >> 4)) {
                        continue;
                    }

                    Location candidate = new Location(world, blockX, blockY, blockZ);
                    var data = Slimefun.getDatabaseManager()
                            .getBlockDataController()
                            .getBlockData(candidate);
                    if (data == null || !data.isDataLoaded() || data.isPendingRemove()) {
                        continue;
                    }

                    String id = data.getSfId();
                    if (id != null && id.contains("DRACFUN_ENERGY_CORE_ACTIVATOR")) {
                        return candidate;
                    }
                }
            }
        }

        return null;
    }
}
