package io.github.wickidcow.sfdracfun2.compat;

import io.github.wickidcow.sfdracfun2.SFDracFun2;
import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Migrates configuration files created by the early staged Reborn builds.
 *
 * <p>Those builds intentionally shipped every subsystem after Materials disabled while
 * implementation work was still in progress. Bukkit's saveDefaultConfig() correctly keeps
 * an existing file, so servers that tested an early build could continue carrying those
 * old development defaults even after installing the completed release.</p>
 */
public final class LegacyConfigMigration {

    public static final int CURRENT_CONFIG_VERSION = 1;

    private static final List<String> STAGED_DISABLED_FEATURES = List.of(
            "features.energy-infuser",
            "features.item-converter",
            "features.fusion-crafting",
            "features.modular-gear",
            "features.energy-core",
            "features.reactor",
            "features.chaos-guardian");

    private LegacyConfigMigration() {}

    /**
     * Upgrades only the recognizable pre-completion development-default configuration.
     * Deliberate feature choices in newer configurations are left untouched.
     *
     * @return true when the old staged defaults were migrated
     */
    public static boolean migrateStagedDefaults(SFDracFun2 addon) {
        FileConfiguration config = addon.getConfig();

        // JavaPlugin loads the packaged config as YAML defaults. isSet() is intentional
        // here: contains()/getInt() would see the new packaged config-version even when
        // an upgraded server's on-disk config never had that key.
        if (config.isSet("config-version")
                && config.getInt("config-version", 0) >= CURRENT_CONFIG_VERSION) {
            return false;
        }

        boolean oldStagedFeatureSet = config.getBoolean("features.materials", true)
                && config.getBoolean("features.end-resource", true)
                && STAGED_DISABLED_FEATURES.stream()
                        .allMatch(key -> config.contains(key) && !config.getBoolean(key));

        // These keys/defaults identify the development-era config that existed before
        // the complete restoration was enabled by default. Requiring them prevents a
        // modern server owner's intentional all-off feature selection from being changed.
        boolean oldStagedSafetyDefaults = config.isSet("reactor.explosion-multiplier")
                && config.isSet("reactor.break-explosion-power")
                && !config.isSet("compatibility.fix-broken-arrow-penetration")
                && !config.isSet("guardian.cleanup-crystal-cages")
                && config.getInt("guardian.wither-minion-lifetime-ticks", 200) == 200;

        if (!oldStagedFeatureSet || !oldStagedSafetyDefaults) {
            return false;
        }

        for (String key : STAGED_DISABLED_FEATURES) {
            config.set(key, true);
        }
        config.set("config-version", CURRENT_CONFIG_VERSION);
        addon.saveConfig();

        addon.getLogger().warning(
                "Migrated pre-completion DracFun Reborn config defaults: "
                        + "Energy Infuser, Item Converter, Fusion Crafting, modular gear, "
                        + "Energy Core, Reactor and Chaos Guardian are now enabled. "
                        + "Future manual feature choices will be preserved.");
        return true;
    }
}
