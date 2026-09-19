package io.github.wickidcow.sfdracfun2.reactor;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.ASlimefunDataContainer;
import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.ItemSetting;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.items.settings.DoubleRangeSetting;
import io.github.thebusybiscuit.slimefun4.api.items.settings.IntRangeSetting;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetProvider;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.utils.ChatUtils;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import io.github.wickidcow.sfdracfun2.SFDracFun2;
import io.github.wickidcow.sfdracfun2.compat.ProtectionCompat;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import me.mrCookieSlime.CSCoreLibPlugin.general.Inventory.ChestMenu;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Clean-room Draconic Reactor preserving DracFun 2.0.10's observable state,
 * fuel and reactor-physics contract.
 *
 * <p>The old implementation ran its reactor ticker asynchronously while touching
 * Bukkit inventories, blocks and explosions. Reborn intentionally performs all
 * reactor simulation from a synchronized Slimefun ticker and persists the old
 * DRACFUN_* state keys plus the previously in-memory production progress.</p>
 */
public final class ReactorMachine extends SlimefunItem implements EnergyNetProvider {

    public static final int ENERGY_CAPACITY = Integer.MAX_VALUE;
    public static final String FUEL_ID = "DRACFUN_AWAKENED_DRACONIUM_BLOCK";
    public static final String OUTPUT_ID = "DRACFUN_CHAOS_SHARD";

    private static final int SHIELD_SET = 10;
    private static final int MIN_SATURATION_SET = 12;
    private static final int FAILSAFE_SET = 14;
    private static final int STATUS = 16;
    private static final int INPUT = 37;
    private static final int CHARGE = 39;
    private static final int ACTIVATE = 40;
    private static final int SHUTDOWN = 41;
    private static final int OUTPUT = 43;

    private static final int[] BORDER = {
        0, 1, 2, 3, 4, 5, 6, 7, 8,
        9, 11, 13, 15, 17,
        18, 19, 20, 21, 22, 23, 24, 25, 26,
        30, 31, 32, 48, 49, 50
    };
    private static final int[] INPUT_BORDER = {27, 28, 29, 36, 38, 45, 46, 47};
    private static final int[] OUTPUT_BORDER = {33, 34, 35, 42, 44, 51, 52, 53};

    private static final int[] STATUS_BORDER = {
        2, 3, 4, 5, 6, 11, 15, 20, 24, 29, 33,
        36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 47, 48, 50, 51, 53
    };
    private static final int[] REACTOR_STATUS = {12, 13, 14, 21, 22, 23, 30, 31, 32};
    private static final int[] CORE_TEMPERATURE = {0, 9, 18, 27};
    private static final int[] CONTAINMENT_FIELD_STRENGTH = {1, 10, 19, 28};
    private static final int[] ENERGY_SATURATION = {7, 16, 25, 34};
    private static final int[] FUEL_CONVERSION_LEVEL = {8, 17, 26, 35};
    private static final int GENERATION_RATE_SLOT = 46;
    private static final int FIELD_INPUT_RATE_SLOT = 49;
    private static final int FUEL_CONVERSION_RATE_SLOT = 52;


    private final ItemSetting<Integer> breakExplosion;
    private final ItemSetting<Double> explosionMultiplier;
    private final double explosionPowerCap;
    private final boolean meltdownExplosionEnabled;
    private final ChestMenu statsMenu;

    public ReactorMachine(
            SFDracFun2 addon,
            ItemGroup group,
            SlimefunItemStack item,
            ItemStack[] recipe) {
        super(group, item, RecipeType.ENHANCED_CRAFTING_TABLE, recipe);

        // Exact DracFun 2.0.10 Slimefun item settings.
        breakExplosion = new IntRangeSetting(this, "explosion-on-break", 0, 4, 10);
        explosionMultiplier =
                new DoubleRangeSetting(this, "explosion-multiplier", 0D, 0.25D, 1D);
        addItemSetting(breakExplosion, explosionMultiplier);

        // Reborn-only safety controls remain outside the legacy gameplay surface.
        explosionPowerCap = Math.max(0D, addon.getConfig().getDouble("reactor.explosion-power-cap", 64D));
        meltdownExplosionEnabled = addon.getConfig().getBoolean("reactor.meltdown-explosion-enabled", true);

        statsMenu = new ChestMenu(getItemName(), 54);
        statsMenu.setEmptySlotsClickable(false);
        statsMenu.setPlayerInventoryClickable(false);
        initializeStatsMenu();

        new BlockMenuPreset(getId(), getItemName()) {
            @Override
            public void init() {
                setSize(54);
                drawBackground(new ItemStack(Material.GRAY_STAINED_GLASS_PANE), BORDER);
                drawBackground(new ItemStack(Material.BLUE_STAINED_GLASS_PANE), INPUT_BORDER);
                drawBackground(new ItemStack(Material.ORANGE_STAINED_GLASS_PANE), OUTPUT_BORDER);

                addItem(
                        SHIELD_SET,
                        controlItem(
                                Material.CYAN_STAINED_GLASS_PANE,
                                ChatColor.AQUA,
                                "Determine the energy allocation for the generation of a containment field."),
                        ChestMenuUtils.getEmptyClickHandler());
                addItem(
                        MIN_SATURATION_SET,
                        controlItem(
                                Material.GREEN_STAINED_GLASS_PANE,
                                ChatColor.GREEN,
                                "Determine the saturation level prior to initiating the generation of energy."),
                        ChestMenuUtils.getEmptyClickHandler());
                addItem(
                        FAILSAFE_SET,
                        controlItem(
                                Material.YELLOW_STAINED_GLASS_PANE,
                                ChatColor.RED,
                                "Toggle the fail-safe to ensure the automatic termination in the event of an emergency."),
                        ChestMenuUtils.getEmptyClickHandler());
                addItem(
                        CHARGE,
                        controlItem(Material.YELLOW_STAINED_GLASS_PANE, ChatColor.YELLOW, "Charge Reactor"),
                        ChestMenuUtils.getEmptyClickHandler());
                addItem(
                        ACTIVATE,
                        controlItem(Material.YELLOW_STAINED_GLASS_PANE, ChatColor.YELLOW, "Activate Reactor"),
                        ChestMenuUtils.getEmptyClickHandler());
                addItem(
                        SHUTDOWN,
                        controlItem(Material.YELLOW_STAINED_GLASS_PANE, ChatColor.YELLOW, "Shutdown Reactor"),
                        ChestMenuUtils.getEmptyClickHandler());
                addItem(
                        STATUS,
                        controlItem(
                                Material.BOOK,
                                ChatColor.RED,
                                "Click to view the current operational status of the reactor."),
                        ChestMenuUtils.getEmptyClickHandler());
                addMenuClickHandler(OUTPUT, (player, slot, clicked, action) -> !isEmpty(clicked));
            }

            @Override
            public void newInstance(BlockMenu menu, Block block) {
                Location location = block.getLocation();

                menu.addMenuClickHandler(SHIELD_SET, (player, slot, clicked, action) -> {
                    configureShieldInput(menu, location, player);
                    return false;
                });
                menu.addMenuClickHandler(MIN_SATURATION_SET, (player, slot, clicked, action) -> {
                    configureMinimumSaturation(menu, location, player);
                    return false;
                });
                menu.addMenuClickHandler(FAILSAFE_SET, (player, slot, clicked, action) -> {
                    toggleFailsafe(location, player);
                    return false;
                });
                menu.addMenuClickHandler(CHARGE, (player, slot, clicked, action) -> {
                    chargeReactor(menu, location, player);
                    return false;
                });
                menu.addMenuClickHandler(ACTIVATE, (player, slot, clicked, action) -> {
                    activateReactor(location, player);
                    return false;
                });
                menu.addMenuClickHandler(SHUTDOWN, (player, slot, clicked, action) -> {
                    shutdownReactor(location, player);
                    return false;
                });
                menu.addMenuClickHandler(STATUS, (player, slot, clicked, action) -> {
                    menu.close();
                    SlimefunBlockData data = blockData(location);
                    if (usable(data)) {
                        ReactorState.ensureDefaults(data);
                        refreshStatsMenu(location, data);
                        statsMenu.open(player);
                    }
                    return false;
                });
            }

            @Override
            public boolean canOpen(Block block, Player player) {
                return ProtectionCompat.canInteract(player, block.getLocation());
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                return flow == ItemTransportFlow.INSERT ? new int[] {INPUT} : new int[] {OUTPUT};
            }
        };

        addItemHandler(new BlockTicker() {
            @Override
            public boolean isSynchronized() {
                return true;
            }

            @Override
            public void tick(Block block, SlimefunItem item, SlimefunBlockData data) {
                tickReactor(block, data);
            }
        });

        addItemHandler(new BlockBreakHandler(false, false) {
            @Override
            public void onPlayerBreak(BlockBreakEvent event, ItemStack item, List<ItemStack> drops) {
                SlimefunBlockData data = blockData(event.getBlock().getLocation());
                if (data != null) {
                    ReactorState.ensureDefaults(data);
                    BlockMenu menu = data.getBlockMenu();
                    if (menu != null) {
                        menu.dropItems(menu.getLocation(), new int[] {INPUT, OUTPUT});
                    }

                    float breakPower = breakExplosion.getValue().floatValue();
                    if (ReactorState.phase(data) != ReactorPhase.COLD && breakPower > 0F) {
                        Location location = event.getBlock().getLocation();
                        location.getWorld().createExplosion(location, breakPower, true, true);
                    }
                }
            }
        });
    }

    private void tickReactor(Block block, SlimefunBlockData data) {
        ReactorState.ensureDefaults(data);
        recoverLegacyProgress(data);

        Location location = block.getLocation();
        injectStoredEnergy(location, data);

        // DracFun 2.0.10 only ran reactor physics while an explicit shard
        // production cycle existed in its progress map. A completed cycle
        // therefore stays idle until the player presses Charge again.
        boolean chargedCycle = ReactorState.progress(data) >= 0;
        if (!chargedCycle) {
            ReactorState.integer(data, ReactorState.GENERATION_RATE, 0);
            BlockMenu idleMenu = data.getBlockMenu();
            if (idleMenu != null && idleMenu.hasViewer()) {
                refreshMenu(location, data, idleMenu);
            }
            return;
        }

        runGeneration(location, data);

        int temperature = ReactorState.integer(data, ReactorState.TEMPERATURE);
        int shield = ReactorState.integer(data, ReactorState.SHIELD_CHARGE);
        int maxShield = ReactorState.integer(data, ReactorState.MAX_SHIELD_CHARGE);
        int saturation = ReactorState.integer(data, ReactorState.SATURATION);
        int maxSaturation = ReactorState.integer(data, ReactorState.MAX_SATURATION);
        int convertedFuel = ReactorState.integer(data, ReactorState.CONVERTED_FUEL);
        int reactableFuel = ReactorState.integer(data, ReactorState.REACTABLE_FUEL);

        updateCoreLogic(
                location,
                data,
                temperature,
                shield,
                maxShield,
                saturation,
                maxSaturation,
                convertedFuel,
                reactableFuel);

        BlockMenu menu = data.getBlockMenu();
        if (menu != null && menu.hasViewer()) {
            refreshMenu(location, data, menu);
        }
    }

    private void injectStoredEnergy(Location location, SlimefunBlockData data) {
        long stored = getChargeLong(location, data);
        int requested = ReactorState.integer(data, ReactorState.SHIELD_INPUT);
        int incoming = (int) Math.min(Integer.MAX_VALUE, Math.min(stored, Math.max(0, requested)));
        if (incoming <= 0) {
            return;
        }

        int consumed = injectEnergy(data, incoming);
        if (consumed > 0 && stored >= consumed) {
            removeCharge(location, consumed, data);
        }
    }

    private int injectEnergy(SlimefunBlockData data, int incoming) {
        ReactorPhase phase = ReactorState.phase(data);
        int temperature = ReactorState.integer(data, ReactorState.TEMPERATURE);
        int shield = ReactorState.integer(data, ReactorState.SHIELD_CHARGE);
        int maxShield = ReactorState.integer(data, ReactorState.MAX_SHIELD_CHARGE);
        int saturation = ReactorState.integer(data, ReactorState.SATURATION);
        int maxSaturation = ReactorState.integer(data, ReactorState.MAX_SATURATION);
        int reactableFuel = ReactorState.integer(data, ReactorState.REACTABLE_FUEL);

        if (phase == ReactorPhase.WARMING_UP && ReactorState.bool(data, ReactorState.INITIALIZED)) {
            int halfShield = maxShield / 2;
            int halfSaturation = maxSaturation / 2;

            if (shield < halfShield) {
                int accepted = Math.min(incoming, halfShield - shield);
                ReactorState.addNonNegative(data, ReactorState.SHIELD_CHARGE, accepted);
                return accepted;
            }

            if (saturation < halfSaturation) {
                int accepted = Math.min(incoming, halfSaturation - saturation);
                ReactorState.addNonNegative(data, ReactorState.SATURATION, accepted);
                return accepted;
            }

            if (temperature < 2000) {
                int divisor = Math.max(1, 1000 + safeMultiply(reactableFuel, 10));
                int increase = incoming / divisor;
                ReactorState.integer(data, ReactorState.TEMPERATURE, Math.min(2500, temperature + increase));
                return incoming;
            }

            return 0;
        }

        if (phase == ReactorPhase.RUNNING || phase == ReactorPhase.STOPPING) {
            double temperatureEfficiency = 1D;
            if (temperature > 15000) {
                temperatureEfficiency = 1D - Math.min(1D, (temperature - 15000D) / 10000D);
            }

            long denominator = (long) maxShield + 1L;
            long legacyIntegerRatio = denominator > 0 ? shield / denominator : 0L;
            long scaled = (long) incoming * (1L - legacyIntegerRatio);
            int room = Math.max(0, maxShield - shield);
            int accepted = (int) (Math.min(Math.max(0L, scaled), room) * temperatureEfficiency);
            if (accepted > 0) {
                ReactorState.integer(data, ReactorState.SHIELD_CHARGE, Math.min(maxShield, shield + accepted));
            }

            // DracFun 2.0.10 reported the full requested amount as consumed in online states.
            return incoming;
        }

        return 0;
    }

    private void runGeneration(Location location, SlimefunBlockData data) {
        int progress = ReactorState.progress(data);
        if (progress < 0) {
            ReactorState.integer(data, ReactorState.GENERATION_RATE, 0);
            return;
        }

        BlockMenu menu = data.getBlockMenu();
        if (progress == 0) {
            ItemStack output = itemById(OUTPUT_ID);
            if (menu != null && output != null && menu.fits(output, OUTPUT)) {
                ItemStack remainder = menu.pushItem(output, OUTPUT);
                if (remainder == null || remainder.getAmount() <= 0) {
                    ReactorState.progress(data, -1);
                    ReactorState.integer(data, ReactorState.GENERATION_RATE, 0);
                }
            }
            return;
        }

        if (ReactorState.phase(data) != ReactorPhase.RUNNING) {
            ReactorState.integer(data, ReactorState.GENERATION_RATE, 0);
            return;
        }

        int saturation = ReactorState.integer(data, ReactorState.SATURATION);
        int maxSaturation = ReactorState.integer(data, ReactorState.MAX_SATURATION);
        int minimumPercent = ReactorState.integer(data, ReactorState.MIN_SATURATION);
        int generation = (int) Math.min(
                Integer.MAX_VALUE,
                Math.max(0L, (long) minimumPercent * Math.max(0, maxSaturation) / 100L));

        if (saturation <= generation) {
            ReactorState.integer(data, ReactorState.GENERATION_RATE, 0);
            return;
        }

        long available = ENERGY_CAPACITY - getChargeLong(location, data);
        if (available < generation) {
            ReactorState.integer(data, ReactorState.GENERATION_RATE, 0);
            return;
        }

        ReactorState.progress(data, progress - 1);
        ReactorState.subtractNonNegative(data, ReactorState.SATURATION, generation);
        ReactorState.integer(data, ReactorState.GENERATION_RATE, generation);
        if (generation > 0) {
            addCharge(location, generation, data);
        }
    }

    private void updateCoreLogic(
            Location location,
            SlimefunBlockData data,
            int temperature,
            int shield,
            int maxShield,
            int saturation,
            int maxSaturation,
            int convertedFuel,
            int reactableFuel) {
        ReactorPhase phase = ReactorState.phase(data);

        switch (phase) {
            case COLD -> updateOfflineState(data, temperature, shield, maxShield, saturation, maxSaturation);
            case COOLING -> {
                updateOfflineState(data, temperature, shield, maxShield, saturation, maxSaturation);
                if (temperature <= 100) {
                    ReactorState.phase(data, ReactorPhase.COLD);
                }
            }
            case WARMING_UP -> initializeStartup(data, shield, saturation, convertedFuel, reactableFuel);
            case RUNNING -> {
                updateOnlineState(
                        data,
                        temperature,
                        shield,
                        maxShield,
                        saturation,
                        maxSaturation,
                        convertedFuel,
                        reactableFuel);

                int currentMaxSaturation = ReactorState.integer(data, ReactorState.MAX_SATURATION);
                int currentSaturation = ReactorState.integer(data, ReactorState.SATURATION);
                if (ReactorState.bool(data, ReactorState.FAILSAFE)
                        && ReactorState.integer(data, ReactorState.TEMPERATURE) < 2500
                        && currentMaxSaturation > 0
                        && currentSaturation / (double) currentMaxSaturation >= 0.95D) {
                    shutdown(data);
                }
            }
            case STOPPING -> {
                updateOnlineState(
                        data,
                        temperature,
                        shield,
                        maxShield,
                        saturation,
                        maxSaturation,
                        convertedFuel,
                        reactableFuel);
                if (ReactorState.integer(data, ReactorState.TEMPERATURE) <= 2000) {
                    ReactorState.phase(data, ReactorPhase.COOLING);
                    ReactorState.bool(data, ReactorState.INITIALIZED, false);
                }
            }
            case BEYOND_HOPE -> updateCriticalState(location, data, reactableFuel, convertedFuel);
        }
    }

    private void initializeStartup(
            SlimefunBlockData data,
            int shield,
            int saturation,
            int convertedFuel,
            int reactableFuel) {
        if (ReactorState.bool(data, ReactorState.INITIALIZED)) {
            return;
        }

        long totalFuel = Math.max(0L, (long) convertedFuel + reactableFuel);
        int maxSaturation = clampInt(totalFuel * 10_000L);
        int maxShield = clampInt(totalFuel * 1_000L);

        ReactorState.integer(data, ReactorState.MAX_SATURATION, maxSaturation);
        ReactorState.integer(data, ReactorState.MAX_SHIELD_CHARGE, maxShield);
        if (saturation > maxSaturation) {
            ReactorState.integer(data, ReactorState.SATURATION, maxSaturation);
        }
        if (shield > maxShield) {
            ReactorState.integer(data, ReactorState.SHIELD_CHARGE, maxShield);
        }
        ReactorState.bool(data, ReactorState.INITIALIZED, true);
    }

    private void updateOfflineState(
            SlimefunBlockData data,
            int temperature,
            int shield,
            int maxShield,
            int saturation,
            int maxSaturation) {
        ReactorState.integer(data, ReactorState.GENERATION_RATE, 0);
        ReactorState.integer(data, ReactorState.FIELD_INPUT_RATE, 0);
        ReactorState.integer(data, ReactorState.FUEL_USE_RATE, 0);

        if (temperature > 100) {
            ReactorState.subtractNonNegative(data, ReactorState.TEMPERATURE, 1);
        }

        if (shield > 0) {
            ReactorState.subtractNonNegative(
                    data,
                    ReactorState.SHIELD_CHARGE,
                    (int) (Math.max(0, maxShield) * 1.0E-5D));
        } else {
            ReactorState.integer(data, ReactorState.SHIELD_CHARGE, 0);
        }

        if (saturation > 0) {
            ReactorState.subtractNonNegative(
                    data,
                    ReactorState.SATURATION,
                    (int) (Math.max(0, maxSaturation) * 1.0E-7D));
        } else {
            ReactorState.integer(data, ReactorState.SATURATION, 0);
        }
    }

    private void updateOnlineState(
            SlimefunBlockData data,
            int temperature,
            int shield,
            int maxShield,
            int saturation,
            int maxSaturation,
            int convertedFuel,
            int reactableFuel) {
        ReactorPhase phase = ReactorState.phase(data);
        double coreSaturation = saturation / (maxSaturation + 1D);
        double saturationConversion = (1D - coreSaturation) * 99D;

        // Preserve 2.0.10's integer division before the x50 multiplier.
        double temp50 = Math.min((temperature / 10000) * 50, 99);

        double fuelTotal = (double) convertedFuel + reactableFuel;
        double conversionLevel = convertedFuel / (fuelTotal + 1D) * 1.3D - 0.3D;
        double saturationPenalty =
                Math.pow(saturationConversion, 3D) / (100D - saturationConversion) + 450D;
        double temperaturePenalty = Math.pow(temp50, 4D) / (100D - temp50);
        double temperatureRise =
                (saturationPenalty - temperaturePenalty * (1D - conversionLevel)
                        + conversionLevel * 1000D)
                        / 10000D;

        if (phase == ReactorPhase.STOPPING && conversionLevel < 1D) {
            if (temperature <= 2001) {
                ReactorState.phase(data, ReactorPhase.COOLING);
                ReactorState.bool(data, ReactorState.INITIALIZED, false);
                return;
            }

            if (saturation >= maxSaturation * 0.95D && reactableFuel > 0) {
                int cooling = (int) (1D - conversionLevel);
                ReactorState.subtractNonNegative(data, ReactorState.TEMPERATURE, cooling);
                temperature -= cooling;
            } else {
                int increase = (int) (temperatureRise * 10D);
                ReactorState.addNonNegative(data, ReactorState.TEMPERATURE, increase);
                temperature += increase;
            }
        } else {
            int increase = (int) (temperatureRise * 10D);
            ReactorState.addNonNegative(data, ReactorState.TEMPERATURE, increase);
            temperature += increase;
        }

        int baseMaxShield = clampInt(Math.max(0D, maxSaturation * 1.5D));
        int shieldHeatFactor = clampSignedInt(baseMaxShield * (1D + conversionLevel * 2D));
        int saturationGain = clampSignedInt((1D - coreSaturation) * shieldHeatFactor);
        ReactorState.addNonNegative(data, ReactorState.SATURATION, saturationGain);

        double temperatureDrain;
        if (temperature > 8000) {
            double delta = temperature - 8000D;
            temperatureDrain = 1D + delta * delta * 2.5E-6D;
        } else if (temperature > 2000) {
            temperatureDrain = 1D;
        } else if (temperature > 1000) {
            temperatureDrain = (temperature - 1000D) / 1000D;
        } else {
            temperatureDrain = 0D;
        }

        // 2.0.10 used Math.max(1, 1-coreSaturation), so this factor is normally 1.
        double fieldDrain = Math.min(
                temperatureDrain
                        * 0.01D
                        * Math.max(1D, 1D - coreSaturation)
                        * baseMaxShield
                        / 11D,
                Integer.MAX_VALUE);

        double fieldInputDenominator =
                (1D - shield / (maxShield + 1D)) + 1D;
        int fieldInputRate = fieldInputDenominator <= 0D
                ? 0
                : clampInt(fieldDrain / fieldInputDenominator);
        ReactorState.integer(data, ReactorState.FIELD_INPUT_RATE, fieldInputRate);

        ReactorState.subtractNonNegative(
                data,
                ReactorState.SHIELD_CHARGE,
                Math.min(clampInt(fieldDrain), Math.max(0, shield)));

        double fuelUse = temperatureDrain * (1D - coreSaturation) * 0.025D;
        int fuelUseRate = Math.max(0, clampSignedInt(fuelUse));

        // The legacy addon accumulated this display value forever. Reborn stores the
        // actual current rate, which keeps the status meaningful without changing fuel physics.
        ReactorState.integer(data, ReactorState.FUEL_USE_RATE, fuelUseRate);

        if (reactableFuel > 0 && fuelUseRate > 0) {
            int convertedNow = Math.min(reactableFuel, fuelUseRate);
            ReactorState.addNonNegative(data, ReactorState.CONVERTED_FUEL, convertedNow);
            ReactorState.subtractNonNegative(data, ReactorState.REACTABLE_FUEL, convertedNow);
        }

        if (ReactorState.integer(data, ReactorState.SHIELD_CHARGE) <= 0
                && temperature > 2000
                && phase != ReactorPhase.BEYOND_HOPE) {
            ReactorState.phase(data, ReactorPhase.BEYOND_HOPE);
        }
    }

    private void updateCriticalState(
            Location location,
            SlimefunBlockData data,
            int reactableFuel,
            int convertedFuel) {
        int countdown = ReactorState.integer(data, ReactorState.EXPLOSION_COUNTDOWN);
        if (countdown > 0) {
            ReactorState.integer(data, ReactorState.EXPLOSION_COUNTDOWN, countdown - 1);
            return;
        }

        if (!meltdownExplosionEnabled) {
            ReactorState.integer(data, ReactorState.EXPLOSION_COUNTDOWN, Integer.MAX_VALUE);
            return;
        }

        double fuel = (long) reactableFuel + convertedFuel;
        double multiplier = explosionMultiplier.getValue();
        double legacyPower =
                (50D + ((fuel - 162D) * multiplier / 100D)) * multiplier;
        float power = (float) Math.min(explosionPowerCap, Math.max(0D, legacyPower));
        explode(location, power);
    }

    private void explode(Location location, float power) {
        Slimefun.getDatabaseManager().getBlockDataController().removeBlock(location);
        location.getBlock().setType(Material.AIR);
        if (power > 0F) {
            location.getWorld().createExplosion(location, power, true, true);
        }
    }

    private void chargeReactor(BlockMenu menu, Location location, Player player) {
        SlimefunBlockData data = blockData(location);
        if (!usable(data)) {
            error(player, "Reactor data is still loading; try again.");
            return;
        }

        ReactorState.ensureDefaults(data);
        if (ReactorState.progress(data) >= 0) {
            error(player, "This reactor already has a charged production cycle.");
            return;
        }

        ItemStack fuel = menu.getItemInSlot(INPUT);
        if (!isEmpty(fuel) && isFuel(fuel)) {
            long fuelUnits = (long) fuel.getAmount() * 81L;
            ReactorState.addNonNegative(data, ReactorState.REACTABLE_FUEL, clampInt(fuelUnits));
            menu.consumeItem(INPUT, fuel.getAmount(), true);
        }

        if (!canCharge(data)) {
            error(player, "Conditions for energization remain unfulfilled. At least two fuel blocks are required.");
            return;
        }

        ReactorState.phase(data, ReactorPhase.WARMING_UP);
        ReactorState.progress(
                data,
                clampInt((long) ReactorState.integer(data, ReactorState.REACTABLE_FUEL) * 120L));
        player.sendMessage(ChatColor.GREEN + "The reactor has been charged.");
    }

    private void activateReactor(Location location, Player player) {
        SlimefunBlockData data = blockData(location);
        if (!usable(data)) {
            error(player, "Reactor data is still loading; try again.");
            return;
        }

        ReactorState.ensureDefaults(data);
        if (!canActivate(data)) {
            error(player, "Conditions for initialization remain unfulfilled.");
            return;
        }

        ReactorState.phase(data, ReactorPhase.RUNNING);
        player.sendMessage(ChatColor.GREEN + "The reactor has been activated.");
    }

    private void shutdownReactor(Location location, Player player) {
        SlimefunBlockData data = blockData(location);
        if (!usable(data)) {
            error(player, "Reactor data is still loading; try again.");
            return;
        }

        ReactorState.ensureDefaults(data);
        if (!shutdown(data)) {
            error(player, "Conditions for deactivation remain unfulfilled.");
            return;
        }

        player.sendMessage(ChatColor.GREEN + "The reactor has been deactivated.");
    }

    private static boolean shutdown(SlimefunBlockData data) {
        ReactorPhase phase = ReactorState.phase(data);
        if (phase == ReactorPhase.RUNNING || phase == ReactorPhase.WARMING_UP) {
            ReactorState.phase(data, ReactorPhase.STOPPING);
            return true;
        }
        return false;
    }

    private static boolean canCharge(SlimefunBlockData data) {
        ReactorPhase phase = ReactorState.phase(data);
        if (phase == ReactorPhase.BEYOND_HOPE) {
            return false;
        }

        long fuel = (long) ReactorState.integer(data, ReactorState.REACTABLE_FUEL)
                + ReactorState.integer(data, ReactorState.CONVERTED_FUEL);
        return (phase == ReactorPhase.COLD || phase == ReactorPhase.COOLING) && fuel >= 162L;
    }

    private static boolean canActivate(SlimefunBlockData data) {
        ReactorPhase phase = ReactorState.phase(data);
        if (phase != ReactorPhase.WARMING_UP && phase != ReactorPhase.STOPPING) {
            return false;
        }

        int temperature = ReactorState.integer(data, ReactorState.TEMPERATURE);
        int saturation = ReactorState.integer(data, ReactorState.SATURATION);
        int maxSaturation = ReactorState.integer(data, ReactorState.MAX_SATURATION);
        int shield = ReactorState.integer(data, ReactorState.SHIELD_CHARGE);
        int maxShield = ReactorState.integer(data, ReactorState.MAX_SHIELD_CHARGE);
        return temperature >= 2000
                && saturation >= maxSaturation / 2
                && shield >= maxShield / 2;
    }

    private void toggleFailsafe(Location location, Player player) {
        SlimefunBlockData data = blockData(location);
        if (!usable(data)) {
            error(player, "Reactor data is still loading; try again.");
            return;
        }

        ReactorState.ensureDefaults(data);
        boolean enabled = !ReactorState.bool(data, ReactorState.FAILSAFE);
        ReactorState.bool(data, ReactorState.FAILSAFE, enabled);
        player.sendMessage(ChatColor.GREEN + "Reactor fail-safe: " + (enabled ? "ON" : "OFF"));
    }

    private void configureShieldInput(BlockMenu menu, Location location, Player player) {
        menu.close();
        player.sendMessage(ChatColor.AQUA + "Enter containment-field energy allocation in J/tick (positive integer).");
        ChatUtils.awaitInput(player, input -> Slimefun.runSyncAt(location, () -> {
            SlimefunBlockData data = blockData(location);
            Integer value = positiveInteger(input);
            if (!usable(data) || value == null) {
                message(player, ChatColor.RED + "Invalid input. Enter a positive whole number.");
                return;
            }

            ReactorState.ensureDefaults(data);
            ReactorState.integer(data, ReactorState.SHIELD_INPUT, value);
            message(player, ChatColor.GREEN + "Containment-field input set to " + value + " J/tick.");
        }));
    }

    private void configureMinimumSaturation(BlockMenu menu, Location location, Player player) {
        menu.close();
        player.sendMessage(ChatColor.GREEN + "Enter minimum saturation percentage (1-99).");
        ChatUtils.awaitInput(player, input -> Slimefun.runSyncAt(location, () -> {
            SlimefunBlockData data = blockData(location);
            Integer value = positiveInteger(input);
            if (!usable(data) || value == null || value >= 100) {
                message(player, ChatColor.RED + "Invalid input. Enter a whole number from 1 to 99.");
                return;
            }

            ReactorState.ensureDefaults(data);
            ReactorState.integer(data, ReactorState.MIN_SATURATION, value);
            message(player, ChatColor.GREEN + "Minimum saturation set to " + value + "%.");
        }));
    }

    private void refreshMenu(Location location, SlimefunBlockData data, BlockMenu menu) {
        // DracFun 2.0.10 kept the main control panel static. The detailed
        // operational values are rendered only when the player opens Stats.
    }

    private void initializeStatsMenu() {
        ChestMenuUtils.drawBackground(statsMenu, STATUS_BORDER);
        fillStatsSlots(REACTOR_STATUS, new ItemStack(Material.BLACK_STAINED_GLASS_PANE));
        refreshStatsItems(
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                0);
    }

    private void refreshStatsMenu(Location location, SlimefunBlockData data) {
        int temperature = ReactorState.integer(data, ReactorState.TEMPERATURE);
        int shield = ReactorState.integer(data, ReactorState.SHIELD_CHARGE);
        int maxShield = ReactorState.integer(data, ReactorState.MAX_SHIELD_CHARGE);
        int saturation = ReactorState.integer(data, ReactorState.SATURATION);
        int maxSaturation = ReactorState.integer(data, ReactorState.MAX_SATURATION);
        int convertedFuel = ReactorState.integer(data, ReactorState.CONVERTED_FUEL);
        int reactableFuel = ReactorState.integer(data, ReactorState.REACTABLE_FUEL);
        int generationRate = ReactorState.integer(data, ReactorState.GENERATION_RATE);
        int fieldInputRate = ReactorState.integer(data, ReactorState.FIELD_INPUT_RATE);
        int fuelUseRate = ReactorState.integer(data, ReactorState.FUEL_USE_RATE);

        fillStatsSlots(
                REACTOR_STATUS,
                new ItemStack(
                        ReactorState.phase(data) == ReactorPhase.RUNNING
                                ? Material.ORANGE_STAINED_GLASS_PANE
                                : Material.BLACK_STAINED_GLASS_PANE));

        refreshStatsItems(
                temperature,
                shield,
                maxShield,
                saturation,
                maxSaturation,
                convertedFuel,
                reactableFuel,
                generationRate,
                fieldInputRate,
                fuelUseRate);
    }

    private void refreshStatsItems(
            int temperature,
            int shield,
            int maxShield,
            int saturation,
            int maxSaturation,
            int convertedFuel,
            int reactableFuel,
            int generationRate,
            int fieldInputRate,
            int fuelUseRate) {
        fillStatsSlots(
                CORE_TEMPERATURE,
                statsItem(
                        Material.RED_STAINED_GLASS_PANE,
                        ChatColor.RED,
                        "Core Temperature",
                        statLine(ChatColor.RED, temperature + " K"),
                        ""));
        fillStatsSlots(
                CONTAINMENT_FIELD_STRENGTH,
                statsItem(
                        Material.CYAN_STAINED_GLASS_PANE,
                        ChatColor.AQUA,
                        "Containment Field Strength",
                        statRatio(ChatColor.AQUA, shield, maxShield),
                        ""));
        fillStatsSlots(
                ENERGY_SATURATION,
                statsItem(
                        Material.LIME_STAINED_GLASS_PANE,
                        ChatColor.GREEN,
                        "Energy Saturation",
                        statRatio(ChatColor.GREEN, saturation, maxSaturation),
                        ""));
        fillStatsSlots(
                FUEL_CONVERSION_LEVEL,
                statsItem(
                        Material.YELLOW_STAINED_GLASS_PANE,
                        ChatColor.YELLOW,
                        "Fuel Conversion Level",
                        fuelRatio(convertedFuel, reactableFuel),
                        ""));

        statsMenu.replaceExistingItem(
                GENERATION_RATE_SLOT,
                statsItem(
                        Material.RED_STAINED_GLASS_PANE,
                        ChatColor.RED,
                        "Generation Rate",
                        ChatColor.WHITE + "This is the current RF/t being generated by the reactor.",
                        powerPerTick(generationRate)));
        statsMenu.replaceExistingItem(
                FIELD_INPUT_RATE_SLOT,
                statsItem(
                        Material.CYAN_STAINED_GLASS_PANE,
                        ChatColor.AQUA,
                        "Field Input Rate",
                        ChatColor.WHITE
                                + "This is the exact RF/t required to maintain the current field strength.",
                        ChatColor.WHITE
                                + "As field strength increases, this will increase exponentially.",
                        powerPerTick(fieldInputRate)));
        statsMenu.replaceExistingItem(
                FUEL_CONVERSION_RATE_SLOT,
                statsItem(
                        Material.YELLOW_STAINED_GLASS_PANE,
                        ChatColor.YELLOW,
                        "Fuel Conversion Rate",
                        ChatColor.WHITE + "This is how fast the reactor is currently using fuel.",
                        ChatColor.WHITE + "As the reactor saturation increases, this will go down.",
                        ChatColor.RED + "• " + ChatColor.WHITE + fuelUseRate + " nb/t"));
    }

    private void fillStatsSlots(int[] slots, ItemStack item) {
        for (int slot : slots) {
            statsMenu.replaceExistingItem(slot, item.clone());
        }
    }

    private static ItemStack controlItem(Material material, ChatColor color, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color + name);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack statsItem(Material material, ChatColor color, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(color + name);
        meta.setLore(java.util.Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    private static String statLine(ChatColor color, String value) {
        return color + "• " + ChatColor.WHITE + value;
    }

    private static String statRatio(ChatColor color, int value, int maximum) {
        int divisor = maximum + 1;
        int percent = divisor <= 0 ? 0 : (int) ((long) value * 100L / divisor);
        return color + "• " + ChatColor.WHITE + value + "/" + divisor + " (" + percent + "%)";
    }

    private static String fuelRatio(int convertedFuel, int reactableFuel) {
        int second = reactableFuel + 1;
        long total = Math.max(1L, (long) convertedFuel + second);
        int percent = (int) ((long) convertedFuel * 100L / total);
        return ChatColor.YELLOW + "• " + ChatColor.WHITE + convertedFuel + "/" + second
                + " (" + percent + "%)";
    }

    private static String powerPerTick(int amount) {
        return ChatColor.DARK_GRAY + "⚡" + ChatColor.YELLOW + " • " + ChatColor.GRAY + amount + " J/t";
    }

    private static ItemStack actionItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        List<String> lines = new ArrayList<>(lore.length);
        for (String line : lore) {
            lines.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        meta.setLore(lines);
        item.setItemMeta(meta);
        return item;
    }

    private static boolean isFuel(ItemStack stack) {
        SlimefunItem item = SlimefunItem.getByItem(stack);
        return item != null && FUEL_ID.equals(item.getId());
    }

    private static ItemStack itemById(String id) {
        SlimefunItem item = SlimefunItem.getById(id);
        return item == null ? null : item.getItem().clone();
    }

    private static SlimefunBlockData blockData(Location location) {
        return Slimefun.getDatabaseManager()
                .getBlockDataController()
                .getBlockData(location);
    }

    private static boolean usable(SlimefunBlockData data) {
        return data != null && data.isDataLoaded() && !data.isPendingRemove();
    }

    private static Integer positiveInteger(String value) {
        if (value == null) {
            return null;
        }

        try {
            int parsed = Integer.parseInt(value.trim());
            return parsed > 0 ? parsed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static void message(Player player, String message) {
        Slimefun.runSyncFor(player, () -> player.sendMessage(message));
    }

    private static void error(Player player, String message) {
        player.sendMessage(ChatColor.RED + message);
    }

    private static boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir();
    }

    private void recoverLegacyProgress(SlimefunBlockData data) {
        // Early Reborn builds could have persisted an active 2.0.10 reactor
        // without the original in-memory progress map. Recover that state once,
        // then permanently mark the block migrated so finishing a cycle cannot
        // auto-create another Chaos Shard cycle on the next tick.
        if (ReactorState.bool(data, ReactorState.REBORN_PROGRESS_MIGRATED)) {
            return;
        }
        ReactorState.bool(data, ReactorState.REBORN_PROGRESS_MIGRATED, true);

        if (ReactorState.progress(data) >= 0) {
            return;
        }

        ReactorPhase phase = ReactorState.phase(data);
        if (phase == ReactorPhase.COLD || phase == ReactorPhase.COOLING) {
            return;
        }

        long fuel = (long) ReactorState.integer(data, ReactorState.REACTABLE_FUEL)
                + ReactorState.integer(data, ReactorState.CONVERTED_FUEL);
        if (fuel < 162L) {
            return;
        }

        int reactable = ReactorState.integer(data, ReactorState.REACTABLE_FUEL);
        ReactorState.progress(data, clampInt(Math.max(1L, reactable) * 120L));
    }

    @Override
    public int getGeneratedOutput(@Nonnull Location location, @Nonnull ASlimefunDataContainer data) {
        // The legacy reactor generated into its own charge buffer. EnergyNet exposes
        // that stored charge as generator supply and writes any unused remainder back.
        return 0;
    }

    @Override
    public boolean willExplode(@Nonnull Location location, @Nonnull ASlimefunDataContainer data) {
        // Meltdowns are handled by the synchronized reactor ticker, never EnergyNet.
        return false;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getCapacity() {
        return ENERGY_CAPACITY;
    }

    private static int safeMultiply(int value, int multiplier) {
        return clampInt((long) value * multiplier);
    }

    private static int clampInt(long value) {
        return (int) Math.max(0L, Math.min(Integer.MAX_VALUE, value));
    }

    private static int clampInt(double value) {
        if (Double.isNaN(value) || value <= 0D) {
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, value);
    }

    private static int clampSignedInt(double value) {
        if (Double.isNaN(value)) {
            return 0;
        }
        return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, value));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
