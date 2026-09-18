package io.github.wickidcow.sfdracfun2;

import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.wickidcow.sfdracfun2.compat.LegacyCompatibilityRegistry;
import io.github.wickidcow.sfdracfun2.modular.CapacitorService;
import io.github.wickidcow.sfdracfun2.setup.DracFunChaosGuardianRegistry;
import io.github.wickidcow.sfdracfun2.setup.DracFunEnergyCoreRegistry;
import io.github.wickidcow.sfdracfun2.setup.DracFunFusionComponentRegistry;
import io.github.wickidcow.sfdracfun2.setup.DracFunMachineRegistry;
import io.github.wickidcow.sfdracfun2.setup.DracFunMaterialRegistry;
import io.github.wickidcow.sfdracfun2.setup.DracFunModularRegistry;
import io.github.wickidcow.sfdracfun2.setup.DracFunReactorRegistry;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Clean-room compatibility implementation for the discontinued DracFun addon.
 *
 * <p>This project intentionally does not reuse Phoenix's original Java source,
 * decompiled source, or bundled assets.</p>
 */
public final class SFDracFun2 extends JavaPlugin implements SlimefunAddon {

    public static final String LEGACY_DRACFUN_VERSION = "2.0.10";

    private static SFDracFun2 instance;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        getLogger().info("SF_DracFun2 clean-room compatibility layer starting.");
        getLogger().info("Legacy compatibility target: DracFun " + LEGACY_DRACFUN_VERSION);
        getLogger().info("No original DracFun source code or assets are bundled in this plugin.");

        boolean materialsEnabled = getConfig().getBoolean("features.materials", true);
        if (materialsEnabled) {
            boolean endResource = getConfig().getBoolean("features.end-resource", true);
            int registered = DracFunMaterialRegistry.register(this, endResource);
            getLogger().info("Registered " + registered + " functional Draconium material identities.");
        }

        boolean modularGear = getConfig().getBoolean("features.modular-gear", false);
        if (modularGear) {
            if (!materialsEnabled) {
                modularGear = false;
                getLogger().warning(
                        "Modular gear requires features.materials=true; modular registration was skipped.");
            } else {
                try {
                    boolean modularHardMode =
                            getConfig().getBoolean("options.hard-mode", true);
                    int registered = DracFunModularRegistry.register(this, modularHardMode);
                    new CapacitorService(this);
                    getLogger().info("Registered " + registered + " clean-room modular gear/module identities.");
                    getLogger().info("Started player-owned modular capacitor charging service.");
                } catch (IllegalStateException exception) {
                    modularGear = false;
                    getLogger().severe(
                            "Modular gear prerequisites were unavailable; modular registration was skipped: "
                                    + exception.getMessage());
                }
            }
        }

        if (getConfig().getBoolean("features.energy-infuser", false)) {
            if (!modularGear) {
                getLogger().warning("Energy Infuser is enabled while modular gear is disabled; unsupported items will pass through unchanged.");
            }
            int registered = DracFunMachineRegistry.registerEnergyInfuser(this);
            getLogger().info("Registered " + registered + " clean-room Energy Infuser identity.");
        }

        if (getConfig().getBoolean("features.item-converter", false)) {
            int registered = DracFunMachineRegistry.registerItemConverter(this);
            getLogger().info("Registered " + registered + " clean-room Item Converter identity.");
        }

        boolean fusionCrafting = getConfig().getBoolean("features.fusion-crafting", false);
        boolean energyCore = getConfig().getBoolean("features.energy-core", false);
        boolean reactor = getConfig().getBoolean("features.reactor", false);
        boolean chaosGuardian = getConfig().getBoolean("features.chaos-guardian", false);
        boolean hardMode = getConfig().getBoolean("options.hard-mode", true);
        boolean useDragonEgg = getConfig().getBoolean("options.use-dragon-egg", true);

        if (fusionCrafting) {
            if (!materialsEnabled) {
                getLogger().warning("Fusion Crafting requires features.materials=true; Fusion registration was skipped.");
            } else {
                try {
                    int components = DracFunFusionComponentRegistry.register(this, hardMode, useDragonEgg);
                    int energyMaterials = DracFunEnergyCoreRegistry.registerEnergyMaterials(this, hardMode);
                    int crafters = DracFunMachineRegistry.registerFusionCrafters(this, hardMode, useDragonEgg);
                    getLogger().info("Registered " + components + " Fusion progression component identities.");
                    getLogger().info("Registered " + energyMaterials + " shared Energy Core material identities.");
                    getLogger().info("Registered " + crafters + " clean-room Fusion Crafter identities.");
                } catch (IllegalStateException exception) {
                    getLogger().severe("Fusion Crafting prerequisites were unavailable; Fusion registration was skipped: "
                            + exception.getMessage());
                }
            }
        }

        if (energyCore) {
            if (!materialsEnabled) {
                getLogger().warning("Energy Core requires features.materials=true; Energy Core registration was skipped.");
            } else {
                try {
                    int energyMaterials = DracFunEnergyCoreRegistry.registerEnergyMaterials(this, hardMode);
                    int multiblocks = DracFunEnergyCoreRegistry.registerMultiblocks(this, hardMode);
                    getLogger().info("Registered " + energyMaterials + " Energy Core material identities.");
                    getLogger().info("Registered " + multiblocks + " Energy Core multiblock identities.");
                } catch (IllegalStateException exception) {
                    getLogger().severe("Energy Core prerequisites were unavailable; Energy Core registration was skipped: "
                            + exception.getMessage());
                }
            }
        }

        if (reactor) {
            if (!materialsEnabled) {
                getLogger().warning("Draconic Reactor requires features.materials=true; Reactor registration was skipped.");
            } else {
                try {
                    int sharedFusionComponents =
                            DracFunFusionComponentRegistry.register(this, hardMode, useDragonEgg);
                    int energyMaterials =
                            DracFunEnergyCoreRegistry.registerEnergyMaterials(this, hardMode);
                    int reactorItems = DracFunReactorRegistry.register(this, hardMode);

                    getLogger().info("Registered " + sharedFusionComponents
                            + " shared Fusion/material identities required by the Reactor.");
                    getLogger().info("Registered " + energyMaterials
                            + " shared Energy Core identities required by the Reactor.");
                    getLogger().info("Registered " + reactorItems
                            + " clean-room Draconic Reactor identities.");

                    if (!fusionCrafting) {
                        getLogger().warning(
                                "Draconic Reactor is enabled while Fusion Crafting is disabled. "
                                        + "Reactor identities are available, but the final Stabilizer, "
                                        + "Energy Injector and Reactor Core are not survival-craftable.");
                    }
                } catch (IllegalStateException exception) {
                    getLogger().severe("Draconic Reactor prerequisites were unavailable; Reactor registration was skipped: "
                            + exception.getMessage());
                }
            }
        }

        if (chaosGuardian) {
            if (!materialsEnabled) {
                getLogger().warning(
                        "Chaos Guardian requires features.materials=true; Guardian registration was skipped.");
            } else if (!modularGear) {
                getLogger().warning(
                        "Chaos Guardian requires features.modular-gear=true because the legacy battle "
                                + "requires a DracFun modular armor chestplate; Guardian registration was skipped.");
            } else {
                try {
                    int sharedComponents =
                            DracFunFusionComponentRegistry.register(this, hardMode, useDragonEgg);
                    int guardianItems = DracFunChaosGuardianRegistry.register(this);
                    getLogger().info("Registered " + sharedComponents
                            + " shared material identities required by the Chaos Guardian.");
                    getLogger().info("Registered " + guardianItems
                            + " clean-room Chaos Guardian invocation identity.");

                    if (!fusionCrafting) {
                        getLogger().warning(
                                "Chaos Guardian is enabled while Fusion Crafting is disabled. "
                                        + "The Chaos Orb identity is available, but it is not survival-craftable.");
                    }
                } catch (IllegalStateException exception) {
                    getLogger().severe(
                            "Chaos Guardian prerequisites were unavailable; Guardian registration was skipped: "
                                    + exception.getMessage());
                }
            }
        }

        if (getConfig().getBoolean("compatibility.preserve-legacy-ids", true)) {
            int registered = LegacyCompatibilityRegistry.registerMissingIdentities(this);
            getLogger().info("Registered " + registered + " hidden legacy compatibility identities.");
        }
    }

    @Override
    public void onDisable() {
        instance = null;
    }

    public static SFDracFun2 getInstance() {
        return instance;
    }

    @Override
    public String getBugTrackerURL() {
        return "https://github.com/wickidcow/SF_DracFun2/issues";
    }

    @Override
    public JavaPlugin getJavaPlugin() {
        return this;
    }
}
