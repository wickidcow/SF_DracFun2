package io.github.wickidcow.sfdracfun2.fusion;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/** One positional ingredient in a clean-room Fusion recipe. */
public record FusionIngredient(String slimefunId, Material material) {

    public FusionIngredient {
        if ((slimefunId == null) == (material == null)) {
            throw new IllegalArgumentException("Fusion ingredient must use exactly one identity type");
        }
    }

    public static FusionIngredient slimefun(String id) {
        return new FusionIngredient(id, null);
    }

    public static FusionIngredient vanilla(Material material) {
        return new FusionIngredient(null, material);
    }

    /**
     * DracFun 2.0.10 compared Fusion inputs with checkAmount=false and then consumed
     * one item from each occupied slot. Reborn intentionally preserves that runtime
     * contract rather than trusting display-stack amounts from the old recipe guide.
     */
    public boolean matches(ItemStack stack) {
        if (stack == null || stack.getType().isAir() || stack.getAmount() < 1) {
            return false;
        }

        if (slimefunId != null) {
            SlimefunItem item = SlimefunItem.getByItem(stack);
            return item != null && slimefunId.equals(item.getId());
        }

        return SlimefunItem.getByItem(stack) == null && stack.getType() == material;
    }

    public ItemStack toGuideItemStack() {
        if (slimefunId != null) {
            SlimefunItem item = SlimefunItem.getById(slimefunId);
            if (item == null) {
                throw new IllegalStateException("Fusion guide ingredient is not registered: " + slimefunId);
            }
            return item.getItem().clone();
        }
        return new ItemStack(material);
    }

    public String description() {
        return slimefunId != null ? slimefunId : material.getKey().toString();
    }
}
