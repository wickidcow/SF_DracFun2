package io.github.wickidcow.sfdracfun2.setup;

import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.wickidcow.sfdracfun2.SFDracFun2;
import io.github.wickidcow.sfdracfun2.fusion.FusionTier;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Guide-only Fusion RecipeTypes matching DracFun 2.0.10's four Fusion tiers.
 *
 * <p>Runtime Fusion recipes remain owned by FusionRecipeCatalog. These recipe
 * types expose the same audited 3x3 inputs in the Slimefun guide without
 * registering a second crafting implementation.</p>
 */
public final class DracFunRecipeTypes {

    private static RecipeType basic;
    private static RecipeType wyvern;
    private static RecipeType draconic;
    private static RecipeType chaotic;

    private DracFunRecipeTypes() {}

    public static RecipeType fusion(SFDracFun2 addon, FusionTier tier) {
        return switch (tier) {
            case BASIC -> basic(addon);
            case WYVERN -> wyvern(addon);
            case DRACONIC -> draconic(addon);
            case CHAOTIC -> chaotic(addon);
        };
    }

    private static RecipeType basic(SFDracFun2 addon) {
        if (basic == null) {
            basic = create(addon, "basic_fusion", Material.CRAFTING_TABLE, "Basic Fusion Crafter");
        }
        return basic;
    }

    private static RecipeType wyvern(SFDracFun2 addon) {
        if (wyvern == null) {
            wyvern = create(addon, "wyvern_fusion", Material.SMITHING_TABLE, "Wyvern Fusion Crafter");
        }
        return wyvern;
    }

    private static RecipeType draconic(SFDracFun2 addon) {
        if (draconic == null) {
            draconic = create(addon, "draconic_fusion", Material.RESPAWN_ANCHOR, "Draconic Fusion Crafter");
        }
        return draconic;
    }

    private static RecipeType chaotic(SFDracFun2 addon) {
        if (chaotic == null) {
            chaotic = create(addon, "chaotic_fusion", Material.CRYING_OBSIDIAN, "Chaotic Fusion Crafter");
        }
        return chaotic;
    }

    private static RecipeType create(
            SFDracFun2 addon,
            String key,
            Material material,
            String machineName) {
        ItemStack icon = new ItemStack(material);
        ItemMeta meta = icon.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + machineName);
        icon.setItemMeta(meta);

        return new RecipeType(
                new NamespacedKey(addon, key),
                icon,
                null,
                "",
                ChatColor.GREEN + "Craft it using the " + machineName);
    }
}
