package io.github.wickidcow.sfdracfun2;

import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.wickidcow.sfdracfun2.compat.LegacyCompatibilityRegistry;
import io.github.wickidcow.sfdracfun2.modular.CapacitorService;
import io.github.wickidcow.sfdracfun2.modular.ModularArmorEffectService;
import io.github.wickidcow.sfdracfun2.modular.ModularAutoFeedService;
import io.github.wickidcow.sfdracfun2.modular.ModularBowEffectService;
import io.github.wickidcow.sfdracfun2.modular.ModularToolEffectService;
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

        boolean modularGear = getConfig().getBoolean("features.modular-gear", true);
        if (modularGear) {
            if (!materialsEnabled) {
                modularGear = false;
                getLogger().warning(
                        "Modular gear requires features.materials=true; modular registration was skipped.");
            } else {
                try {
                    boolean modularHardMode =
                            getConfig().getBoolean("options.hard-mode", true);
                    int registered = DracFunModularRegistry.register(
                            this,
                            modularHardMode,
                            getConfig().getBoolean("options.use-dragon-egg", true));
                    new CapacitorService(this);
                    new ModularArmorEffectService(this);
                    new ModularAutoFeedService(this);
                    new ModularBowEffectService(this);
                    new ModularToolEffectService(this);
                    getLogger().info("Registered " + registered + " clean-room modular gear/module identities.");
                    getLogger().info("Started player-owned modular capacitor charging service.");
                    getLogger().info("Started region-safe modular armor effect service.");
                    getLogger().info("Started modular auto-feed service.");
                    getLogger().info("Started modular bow effect service.");
                    getLogger().info("Started region-safe modular AOE/HARVEST tool effect service.");
                } catch (IllegalStateException exception) {
                    modularGear = false;
                    getLogger().severe(
                            "Modular gear prerequisites were unavailable; modular registration was skipped: "
                                    + exception.getMessage());
                }
            }
        }

        if (getConfig().getBoolean("features.energy-infuser", true)) {
            if (!modularGear) {
                getLogger().warning("Energy Infuser is enabled while modular gear is disabled; unsupported items will pass through unchanged.");
            }
            int registered = DracFunMachineRegistry.registerEnergyInfuser(
                    this,
                    getConfig().getBoolean("options.hard-mode", true));
            getLogger().info("Registered " + registered + " clean-room Energy Infuser identity.");
        }

        if (getConfig().getBoolean("features.item-converter", true)) {
            int registered = DracFunMachineRegistry.registerItemConverter(this);
            getLogger().info("Registered " + registered + " clean-room Item Converter identity.");
        }

        boolean fusionCrafting = getConfig().getBoolean("features.fusion-crafting", true);
        boolean energyCore = getConfig().getBoolean("features.energy-core", true);
        boolean reactor = getConfig().getBoolean("features.reactor", true);
        boolean chaosGuardian = getConfig().getBoolean("features.chaos-guardian", true);
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

        boolean preserveLegacyIds =
                getConfig().getBoolean("compatibility.preserve-legacy-ids", true);
        if (preserveLegacyIds) {
            int registered = LegacyCompatibilityRegistry.registerMissingIdentities(this);
            getLogger().info("Registered " + registered + " hidden legacy compatibility identities.");
        }

        LegacyCompatibilityRegistry.IdentityAudit audit =
                LegacyCompatibilityRegistry.auditRegisteredIdentities();
        String auditSummary = "Legacy identity runtime audit: "
                + audit.accountedLegacyIds() + "/134 accounted for ("
                + audit.functionalItems() + " non-placeholder items, "
                + audit.placeholders() + " placeholders, "
                + (audit.guideCategoryPresent() ? "guide category present" : "guide category missing")
                + "); migration aliases "
                + audit.migrationAliasesRegistered() + "/3 registered.";

        boolean fullFeatureSet = java.util.List.of(
                        "features.materials",
                        "features.end-resource",
                        "features.energy-infuser",
                        "features.item-converter",
                        "features.fusion-crafting",
                        "features.modular-gear",
                        "features.energy-core",
                        "features.reactor",
                        "features.chaos-guardian")
                .stream()
                .allMatch(key -> getConfig().getBoolean(key, true));

        // With the complete feature set enabled, every 2.0.10 item identity should
        // be functional except DRACFUN_DRAGON_EGG when the vanilla Dragon Egg option
        // is selected. The three pre-2.0.10 armor aliases remain placeholders by
        // design and are tracked separately from the 134-ID main surface.
        int expectedMainPlaceholders =
                fullFeatureSet && useDragonEgg ? 1 : 0;
        boolean incompleteCompleteRestore = fullFeatureSet
                && audit.placeholders() != expectedMainPlaceholders;

        if (preserveLegacyIds
                && (audit.accountedLegacyIds() != 134
                        || audit.migrationAliasesRegistered() != 3
                        || incompleteCompleteRestore)) {
            getLogger().warning(auditSummary);
            if (incompleteCompleteRestore) {
                getLogger().warning(
                        "Complete-restoration audit expected "
                                + expectedMainPlaceholders
                                + " main placeholder(s), but found "
                                + audit.placeholders()
                                + ". A functional subsystem may have failed to register.");
            }
        } else {
            getLogger().info(auditSummary);
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
