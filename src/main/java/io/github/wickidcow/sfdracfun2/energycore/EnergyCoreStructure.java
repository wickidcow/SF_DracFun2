package io.github.wickidcow.sfdracfun2.energycore;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import java.util.LinkedHashMap;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.World;

/**
 * Immutable multiblock layouts observed from DracFun 2.0.10.
 *
 * <p>Only the relative block identity contract is reproduced here. Validation uses
 * Slimefun Legacy's modern block-data controller instead of the deprecated BlockStorage
 * facade used by the old addon.</p>
 */
public final class EnergyCoreStructure {

    public enum Validation {
        COMPLETE,
        INCOMPLETE,
        UNKNOWN
    }

    private static final String STABILIZER = "DRACFUN_ENERGY_CORE_STABILIZER";
    private static final String DRACONIUM_BLOCK = "DRACFUN_DRACONIUM_BLOCK";
    private static final String CORE_BLOCK_1 = "DRACFUN_ENERGY_CORE_BLOCK";
    private static final String CORE_BLOCK_2 = "DRACFUN_ENERGY_CORE_BLOCK_2";
    private static final String CORE_BLOCK_3 = "DRACFUN_ENERGY_CORE_BLOCK_3";

    private final Map<Offset, String> required;

    private EnergyCoreStructure(Map<Offset, String> required) {
        this.required = Map.copyOf(required);
    }

    public static EnergyCoreStructure tier1() {
        Map<Offset, String> blocks = new LinkedHashMap<>();
        layer3x3(blocks, STABILIZER, CORE_BLOCK_1, 1);
        return new EnergyCoreStructure(blocks);
    }

    public static EnergyCoreStructure tier2() {
        Map<Offset, String> blocks = new LinkedHashMap<>();
        layer3x3(blocks, DRACONIUM_BLOCK, DRACONIUM_BLOCK, 1);
        layer3x3(blocks, STABILIZER, CORE_BLOCK_2, 2);
        layer3x3(blocks, DRACONIUM_BLOCK, DRACONIUM_BLOCK, 3);
        return new EnergyCoreStructure(blocks);
    }

    public static EnergyCoreStructure tier3() {
        Map<Offset, String> blocks = new LinkedHashMap<>();

        layer3x3(blocks, DRACONIUM_BLOCK, DRACONIUM_BLOCK, 1);

        layer3x3(blocks, STABILIZER, STABILIZER, 2);
        sides(blocks, DRACONIUM_BLOCK, 2);

        corners(blocks, DRACONIUM_BLOCK, 3);
        layer3x3(blocks, STABILIZER, STABILIZER, 3);
        sides(blocks, STABILIZER, 3);
        edges(blocks, DRACONIUM_BLOCK, 3);

        corners(blocks, DRACONIUM_BLOCK, 4);
        layer3x3(blocks, STABILIZER, CORE_BLOCK_3, 4);
        sides(blocks, STABILIZER, 4);
        edges(blocks, DRACONIUM_BLOCK, 4);

        corners(blocks, DRACONIUM_BLOCK, 5);
        layer3x3(blocks, STABILIZER, STABILIZER, 5);
        sides(blocks, STABILIZER, 5);
        edges(blocks, DRACONIUM_BLOCK, 5);

        layer3x3(blocks, STABILIZER, STABILIZER, 6);
        sides(blocks, DRACONIUM_BLOCK, 6);

        layer3x3(blocks, DRACONIUM_BLOCK, DRACONIUM_BLOCK, 7);
        return new EnergyCoreStructure(blocks);
    }

    /**
     * Returns UNKNOWN instead of INCOMPLETE when a required chunk/block record has not
     * finished loading. This prevents energy from being destroyed merely because part
     * of a large core crosses a temporarily unloaded chunk boundary.
     */
    public Validation validate(Location origin) {
        World world = origin.getWorld();
        if (world == null) {
            return Validation.UNKNOWN;
        }

        for (Map.Entry<Offset, String> entry : required.entrySet()) {
            Offset offset = entry.getKey();
            int blockX = origin.getBlockX() + offset.x();
            int blockY = origin.getBlockY() + offset.y();
            int blockZ = origin.getBlockZ() + offset.z();

            if (!world.isChunkLoaded(blockX >> 4, blockZ >> 4)) {
                return Validation.UNKNOWN;
            }

            Location location = new Location(world, blockX, blockY, blockZ);
            SlimefunBlockData data = Slimefun.getDatabaseManager()
                    .getBlockDataController()
                    .getBlockData(location);

            if (data == null) {
                return Validation.INCOMPLETE;
            }
            if (!data.isDataLoaded() || data.isPendingRemove()) {
                return Validation.UNKNOWN;
            }
            if (!entry.getValue().equals(data.getSfId())) {
                return Validation.INCOMPLETE;
            }
        }

        return Validation.COMPLETE;
    }

    public int blockCount() {
        return required.size();
    }

    private static void layer3x3(Map<Offset, String> target, String outside, String center, int y) {
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                target.put(new Offset(x, y, z), x == 0 && z == 0 ? center : outside);
            }
        }
    }

    /** Matches the twelve radius-two side positions from the old CoreUtils helper. */
    private static void sides(Map<Offset, String> target, String item, int y) {
        target.put(new Offset(2, y, 0), item);
        target.put(new Offset(0, y, -2), item);
        target.put(new Offset(0, y, 2), item);
        target.put(new Offset(-2, y, 0), item);
        target.put(new Offset(2, y, 1), item);
        target.put(new Offset(1, y, -2), item);
        target.put(new Offset(1, y, 2), item);
        target.put(new Offset(-2, y, 1), item);
        target.put(new Offset(2, y, -1), item);
        target.put(new Offset(-1, y, -2), item);
        target.put(new Offset(-1, y, 2), item);
        target.put(new Offset(-2, y, -1), item);
    }

    /** Matches the twelve radius-three edge positions from the old CoreUtils helper. */
    private static void edges(Map<Offset, String> target, String item, int y) {
        target.put(new Offset(3, y, 0), item);
        target.put(new Offset(0, y, -3), item);
        target.put(new Offset(0, y, 3), item);
        target.put(new Offset(-3, y, 0), item);
        target.put(new Offset(3, y, 1), item);
        target.put(new Offset(1, y, -3), item);
        target.put(new Offset(1, y, 3), item);
        target.put(new Offset(-3, y, 1), item);
        target.put(new Offset(3, y, -1), item);
        target.put(new Offset(-1, y, -3), item);
        target.put(new Offset(-1, y, 3), item);
        target.put(new Offset(-3, y, -1), item);
    }

    /** Matches the four radius-two corner positions from the old CoreUtils helper. */
    private static void corners(Map<Offset, String> target, String item, int y) {
        target.put(new Offset(2, y, -2), item);
        target.put(new Offset(2, y, 2), item);
        target.put(new Offset(-2, y, -2), item);
        target.put(new Offset(-2, y, 2), item);
    }

    private record Offset(int x, int y, int z) {}
}
