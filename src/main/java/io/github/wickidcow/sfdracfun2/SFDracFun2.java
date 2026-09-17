package io.github.wickidcow.sfdracfun2;

import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
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
