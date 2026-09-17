package io.github.wickidcow.sfdracfun2.setup;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.wickidcow.sfdracfun2.SFDracFun2;
import io.github.wickidcow.sfdracfun2.guardian.ChaosGuardianService;
import io.github.wickidcow.sfdracfun2.guardian.ChaosOrbItem;
import org.bukkit.Material;

/** Registers the clean-room Chaos Guardian invocation and battle listeners. */
public final class DracFunChaosGuardianRegistry {

    private DracFunChaosGuardianRegistry() {}

    public static int register(SFDracFun2 addon) {
        ChaosGuardianService service = new ChaosGuardianService(addon);
        service.start();

        String id = "DRACFUN_CHAOS_ORB_OF_INVOCATION";
        if (SlimefunItem.getById(id) != null) {
            return 0;
        }

        ItemGroup group = DracFunItemGroups.materials(addon);
        SlimefunItemStack orb = new SlimefunItemStack(
                id,
                Material.ENDER_EYE,
                "&5Chaos Orb of Invocation",
                "&7Invokes the Chaos Guardian in The End.",
                "&7Requires a DracFun modular armor chestplate.",
                "&cThe battle is intentionally dangerous.");

        new ChaosOrbItem(group, orb, service).register(addon);
        return 1;
    }
}
