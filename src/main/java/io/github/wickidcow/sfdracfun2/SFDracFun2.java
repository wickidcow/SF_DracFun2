package io.github.wickidcow.sfdracfun2;

import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.wickidcow.sfdracfun2.compat.LegacyCompatibilityRegistry;
import io.github.wickidcow.sfdracfun2.modular.CapacitorService;
import io.github.wickidcow.sfdracfun2.setup.DracFunMachineRegistry;
import io.github.wickidcow.sfdracfun2.setup.DracFunMaterialRegistry;
import io.github.wickidcow.sfdracfun2.setup.DracFunModularRegistry;
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

        if (getConfig().getBoolean("features.materials", true)) {
            boolean endResource = getConfig().getBoolean("features.end-resource", true);
            int registered = DracFunMaterialRegistry.register(this, endResource);
            getLogger().info("Registered " + registered + " functional Draconium material identities.");
        }

        boolean modularGear = getConfig().getBoolean("features.modular-gear", false);
        if (modularGear) {
            int registered = DracFunModularRegistry.register(this);
            new CapacitorService(this);
            getLogger().info("Registered " + registered + " clean-room modular gear/module identities.");
            getLogger().info("Started player-owned modular capacitor charging service.");
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

        if (getConfig().getBoolean("features.fusion-crafting", false)) {
            boolean hardMode = getConfig().getBoolean("options.hard-mode", true);
            boolean useDragonEgg = getConfig().getBoolean("options.use-dragon-egg", true);
            int registered = DracFunMachineRegistry.registerFusionCrafters(this, hardMode, useDragonEgg);
            getLogger().info("Registered " + registered + " clean-room Fusion Crafter identities.");
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
