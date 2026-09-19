package io.github.wickidcow.sfdracfun2.setup;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.wickidcow.sfdracfun2.SFDracFun2;\nimport io.github.wickidcow.sfdracfun2.compat.LegacyTheme;
import io.github.wickidcow.sfdracfun2.guardian.ChaosGuardianService;
import io.github.wickidcow.sfdracfun2.guardian.ChaosOrbItem;
import io.github.wickidcow.sfdracfun2.fusion.FusionRecipeCatalog;
import io.github.wickidcow.sfdracfun2.fusion.FusionRecipeSpec;
import org.bukkit.Material;

/** Registers the clean-room Chaos Guardian invocation and battle listeners. */
public final class DracFunChaosGuardianRegistry {

    private DracFunChaosGuardianRegistry() {}

    public static int register(SFDracFun2 addon, boolean hardMode) {
        ChaosGuardianService service = new ChaosGuardianService(addon);
        service.start();

        String id = "DRACFUN_CHAOS_ORB_OF_INVOCATION";
        if (SlimefunItem.getById(id) != null) {
            return 0;
        }

        ItemGroup group = DracFunItemGroups.materials(addon);
        SlimefunItemStack orb = LegacyTheme.END_GAME_CRAFTING.stack(
                id,
                Material.ENDER_EYE,
                "Chaos Orb of Invocation",
                "&7Invokes the Chaos Guardian in The End.",
                "&7Requires a DracFun modular armor chestplate.",
                "&cThe battle is intentionally dangerous.");

        FusionRecipeSpec recipe = FusionRecipeCatalog.requireByOutput(
                hardMode, true, id);
        new ChaosOrbItem(
                        group,
                        orb,
                        DracFunRecipeTypes.fusion(recipe.tier()),
                        recipe.toGuideRecipe(),
                        service)
                .register(addon);
        return 1;
    }
}
