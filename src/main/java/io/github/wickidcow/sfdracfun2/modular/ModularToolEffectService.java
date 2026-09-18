package io.github.wickidcow.sfdracfun2.modular;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.utils.tags.SlimefunTag;
import io.github.wickidcow.sfdracfun2.SFDracFun2;
import io.github.wickidcow.sfdracfun2.compat.ProtectionCompat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Region-safe runtime for the legacy AOE and HARVEST modular tool effects.
 *
 * <p>DracFun 2.0.10 used the highest installed AOE tier as a cube radius of
 * 1/2/3/4 blocks and used HARVEST limits of 16/64/128 connected logs. Reborn
 * preserves those observable limits while scheduling block mutation on each
 * block-owning region and refusing to touch protected, custom or Slimefun
 * blocks.</p>
 */
public final class ModularToolEffectService implements Listener {

    private static final int[][] HARVEST_OFFSETS = {
        {0, 1, 0},
        {0, -1, 0},
        {0, 0, -1},
        {1, 0, 0},
        {0, 0, 1},
        {-1, 0, 0},
        {1, 0, -1},
        {-1, 0, -1},
        {1, 0, 1},
        {-1, 0, 1}
    };

    private final SFDracFun2 plugin;

    public ModularToolEffectService(SFDracFun2 plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onToolBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        if (!player.isSneaking()) {
            return;
        }

        ItemStack tool = player.getInventory().getItemInMainHand();
        SlimefunItem item = SlimefunItem.getByItem(tool);
        if (!(item instanceof ModularGearItem gear)
                || !ModularGearState.canUsePoweredEffect(player, tool)) {
            return;
        }

        if (gear.getGearType() == GearType.AXE && Tag.LOGS.isTagged(event.getBlock().getType())) {
            applyHarvest(player, event.getBlock(), tool, gear);
            return;
        }

        if (gear.getGearType() == GearType.PICKAXE || gear.getGearType() == GearType.SHOVEL) {
            int radius = ModuleEffects.aoe(tool);
            if (radius <= 0) {
                return;
            }

            scheduleBreakCube(player, event.getBlock(), tool.clone(), radius, true);
            consumeCharge(player, EquipmentSlot.HAND, tool, gear);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }

        ItemStack held = event.getItem();
        if (held == null || held.getType().isAir()) {
            return;
        }

        SlimefunItem item = SlimefunItem.getByItem(held);
        if (!(item instanceof ModularGearItem gear)) {
            return;
        }

        if (gear.getGearType() == GearType.HOE) {
            handleHoe(event, held, gear);
        } else if (gear.getGearType() == GearType.STAFF) {
            handleStaff(event, held, gear);
        }
    }

    private void handleHoe(PlayerInteractEvent event, ItemStack hoe, ModularGearItem gear) {
        event.setCancelled(true);

        Player player = event.getPlayer();
        Block clicked = event.getClickedBlock();
        if (clicked == null || !isValidFarmland(player, clicked)) {
            return;
        }

        clicked.setType(Material.FARMLAND);

        if (!player.isSneaking() || !ModularGearState.canUsePoweredEffect(player, hoe)) {
            return;
        }

        int radius = ModuleEffects.aoe(hoe);
        if (radius <= 0) {
            return;
        }

        scheduleTillCube(player, clicked, radius, true);
        consumeCharge(player, event.getHand(), hoe, gear);
    }

    private void handleStaff(PlayerInteractEvent event, ItemStack staff, ModularGearItem gear) {
        event.setCancelled(true);

        Player player = event.getPlayer();
        Block clicked = event.getClickedBlock();
        if (clicked == null
                || !ModularGearState.canUsePoweredEffect(player, staff)
                || !isValidBreakBlock(player, clicked)) {
            return;
        }

        // Legacy Staff of Power mined blocks as a Netherite Pickaxe.
        ItemStack pickaxe = new ItemStack(Material.NETHERITE_PICKAXE);
        clicked.breakNaturally(pickaxe);

        if (!player.isSneaking()) {
            return;
        }

        int radius = ModuleEffects.aoe(staff);
        if (radius <= 0) {
            return;
        }

        scheduleBreakCube(player, clicked, pickaxe, radius, true);
        consumeCharge(player, event.getHand(), staff, gear);
    }

    private void applyHarvest(
            Player player,
            Block origin,
            ItemStack axe,
            ModularGearItem gear) {
        int limit = ModuleEffects.harvest(axe);
        if (limit <= 0) {
            return;
        }

        HarvestContext context = new HarvestContext(
                player,
                axe.clone(),
                limit,
                ConcurrentHashMap.newKeySet(),
                new AtomicInteger(1));

        context.visited().add(BlockKey.of(origin));
        for (int[] offset : HARVEST_OFFSETS) {
            scheduleHarvestCandidate(
                    origin.getWorld(),
                    origin.getX() + offset[0],
                    origin.getY() + offset[1],
                    origin.getZ() + offset[2],
                    context);
        }

        consumeCharge(player, EquipmentSlot.HAND, axe, gear);
    }

    private void scheduleHarvestCandidate(
            World world,
            int x,
            int y,
            int z,
            HarvestContext context) {
        if (y < world.getMinHeight() || y >= world.getMaxHeight()) {
            return;
        }

        BlockKey key = new BlockKey(world.getUID(), x, y, z);
        if (!context.visited().add(key) || context.accepted().get() >= context.limit()) {
            return;
        }

        plugin.getServer().getRegionScheduler().execute(
                plugin,
                world,
                x >> 4,
                z >> 4,
                () -> {
                    if (context.accepted().get() >= context.limit()) {
                        return;
                    }

                    Block block = world.getBlockAt(x, y, z);
                    if (!Tag.LOGS.isTagged(block.getType())) {
                        return;
                    }

                    if (!reserveHarvestSlot(context.accepted(), context.limit())) {
                        return;
                    }

                    // Dough's legacy Vein traversal expanded through connected logs
                    // before applying protection checks. Preserve that traversal.
                    for (int[] offset : HARVEST_OFFSETS) {
                        scheduleHarvestCandidate(
                                world,
                                x + offset[0],
                                y + offset[1],
                                z + offset[2],
                                context);
                    }

                    if (isCustomOrSlimefunBlock(block)
                            || !ProtectionCompat.canBreak(context.player(), block)) {
                        return;
                    }

                    ProtectionCompat.logBreak(context.player(), block);
                    block.breakNaturally(context.tool());
                });
    }

    private static boolean reserveHarvestSlot(AtomicInteger accepted, int limit) {
        while (true) {
            int current = accepted.get();
            if (current >= limit) {
                return false;
            }
            if (accepted.compareAndSet(current, current + 1)) {
                return true;
            }
        }
    }

    private void scheduleBreakCube(
            Player player,
            Block origin,
            ItemStack dropTool,
            int radius,
            boolean skipOrigin) {
        Map<Long, List<BlockPos>> chunks = new HashMap<>();
        World world = origin.getWorld();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                int y = origin.getY() + dy;
                if (y < world.getMinHeight() || y >= world.getMaxHeight()) {
                    continue;
                }

                for (int dz = -radius; dz <= radius; dz++) {
                    if (skipOrigin && dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }

                    int x = origin.getX() + dx;
                    int z = origin.getZ() + dz;
                    long chunkKey = chunkKey(x >> 4, z >> 4);
                    chunks.computeIfAbsent(chunkKey, ignored -> new ArrayList<>())
                            .add(new BlockPos(x, y, z));
                }
            }
        }

        for (Map.Entry<Long, List<BlockPos>> entry : chunks.entrySet()) {
            int chunkX = (int) (entry.getKey() >> 32);
            int chunkZ = (int) (long) entry.getKey();
            List<BlockPos> positions = List.copyOf(entry.getValue());

            plugin.getServer().getRegionScheduler().execute(
                    plugin,
                    world,
                    chunkX,
                    chunkZ,
                    () -> {
                        for (BlockPos pos : positions) {
                            Block block = world.getBlockAt(pos.x(), pos.y(), pos.z());
                            if (!isValidBreakBlock(player, block)) {
                                continue;
                            }

                            ProtectionCompat.logBreak(player, block);
                            block.breakNaturally(dropTool);
                        }
                    });
        }
    }

    private void scheduleTillCube(
            Player player,
            Block origin,
            int radius,
            boolean skipOrigin) {
        Map<Long, List<BlockPos>> chunks = new HashMap<>();
        World world = origin.getWorld();

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                int y = origin.getY() + dy;
                if (y < world.getMinHeight() || y >= world.getMaxHeight()) {
                    continue;
                }

                for (int dz = -radius; dz <= radius; dz++) {
                    if (skipOrigin && dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }

                    int x = origin.getX() + dx;
                    int z = origin.getZ() + dz;
                    long chunkKey = chunkKey(x >> 4, z >> 4);
                    chunks.computeIfAbsent(chunkKey, ignored -> new ArrayList<>())
                            .add(new BlockPos(x, y, z));
                }
            }
        }

        for (Map.Entry<Long, List<BlockPos>> entry : chunks.entrySet()) {
            int chunkX = (int) (entry.getKey() >> 32);
            int chunkZ = (int) (long) entry.getKey();
            List<BlockPos> positions = List.copyOf(entry.getValue());

            plugin.getServer().getRegionScheduler().execute(
                    plugin,
                    world,
                    chunkX,
                    chunkZ,
                    () -> {
                        for (BlockPos pos : positions) {
                            Block block = world.getBlockAt(pos.x(), pos.y(), pos.z());
                            if (isValidFarmland(player, block)) {
                                block.setType(Material.FARMLAND);
                            }
                        }
                    });
        }
    }

    private static boolean isValidBreakBlock(Player player, Block block) {
        if (block.isEmpty()
                || block.isLiquid()
                || SlimefunTag.UNBREAKABLE_MATERIALS.isTagged(block.getType())
                || !block.getWorld().getWorldBorder().isInside(block.getLocation())
                || isCustomOrSlimefunBlock(block)) {
            return false;
        }

        return ProtectionCompat.canBreak(player, block);
    }

    private static boolean isValidFarmland(Player player, Block block) {
        if (block.isEmpty()
                || block.isLiquid()
                || !SlimefunTag.DIRT_VARIANTS.isTagged(block.getType())
                || !block.getWorld().getWorldBorder().isInside(block.getLocation())
                || isCustomOrSlimefunBlock(block)) {
            return false;
        }

        return ProtectionCompat.canBreak(player, block);
    }

    private static boolean isCustomOrSlimefunBlock(Block block) {
        try {
            if (Slimefun.getDatabaseManager()
                    .getBlockDataController()
                    .getBlockData(block.getLocation()) != null) {
                return true;
            }
        } catch (RuntimeException ignored) {
            return true;
        }

        try {
            return Slimefun.getIntegrations().isCustomBlock(block);
        } catch (RuntimeException | LinkageError ignored) {
            return false;
        }
    }

    private static void consumeCharge(
            Player player,
            EquipmentSlot hand,
            ItemStack item,
            ModularGearItem gear) {
        ModularData.removeCharge(item, 1);
        ModularLore.refresh(item, gear);

        if (hand == EquipmentSlot.OFF_HAND) {
            player.getInventory().setItemInOffHand(item);
        } else {
            player.getInventory().setItemInMainHand(item);
        }
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return ((long) chunkX << 32) ^ (chunkZ & 0xffffffffL);
    }

    private record BlockPos(int x, int y, int z) {}

    private record BlockKey(java.util.UUID worldId, int x, int y, int z) {
        private static BlockKey of(Block block) {
            return new BlockKey(
                    block.getWorld().getUID(),
                    block.getX(),
                    block.getY(),
                    block.getZ());
        }
    }

    private record HarvestContext(
            Player player,
            ItemStack tool,
            int limit,
            Set<BlockKey> visited,
            AtomicInteger accepted) {}
}
