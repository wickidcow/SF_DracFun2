package io.github.wickidcow.sfdracfun2.setup;

import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.wickidcow.sfdracfun2.fusion.FusionTier;
import io.github.wickidcow.sfdracfun2.modular.LegacyDracFunKeys;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Guide RecipeTypes matching DracFun 2.0.10's four Fusion Crafter tiers. */
public final class DracFunRecipeTypes {

    private static RecipeType basic;
    private static RecipeType wyvern;
    private static RecipeType draconic;
    private static RecipeType chaotic;

    private DracFunRecipeTypes() {}

    public static RecipeType fusion(FusionTier tier) {
        return switch (tier) {
            case BASIC -> basic();
            case WYVERN -> wyvern();
            case DRACONIC -> draconic();
            case CHAOTIC -> chaotic();
        };
    }

    private static RecipeType basic() {
        if (basic == null) {
            basic = create(FusionTier.BASIC, Material.CRAFTING_TABLE);
        }
        return basic;
    }

    private static RecipeType wyvern() {
        if (wyvern == null) {
            wyvern = create(FusionTier.WYVERN, Material.SMITHING_TABLE);
        }
        return wyvern;
    }

    private static RecipeType draconic() {
        if (draconic == null) {
            draconic = create(FusionTier.DRACONIC, Material.RESPAWN_ANCHOR);
        }
        return draconic;
    }

    private static RecipeType chaotic() {
        if (chaotic == null) {
            chaotic = create(FusionTier.CHAOTIC, Material.CRYING_OBSIDIAN);
        }
        return chaotic;
    }

    private static RecipeType create(FusionTier tier, Material material) {
        ItemStack icon = new ItemStack(material);
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + tier.displayName() + " Fusion Crafter");
        icon.setItemMeta(meta);

        return new RecipeType(
                LegacyDracFunKeys.key(tier.machineId()),
                icon,
                null,
                "",
                ChatColor.GREEN + "Craft it using the " + tier.displayName() + " Fusion Crafter");
    }
}
