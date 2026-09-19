package io.github.wickidcow.sfdracfun2.fusion;

import java.util.List;
import org.bukkit.inventory.ItemStack;

/** Immutable clean-room representation of one positional 3x3 Fusion recipe. */
public record FusionRecipeSpec(
        FusionTier tier,
        List<FusionIngredient> ingredients,
        String outputId,
        int outputAmount,
        int energyCost) {

    public FusionRecipeSpec {
        ingredients = List.copyOf(ingredients);
        if (ingredients.size() != 9) {
            throw new IllegalArgumentException("Fusion recipes must contain exactly nine positional ingredients");
        }
        if (outputAmount < 1) {
            throw new IllegalArgumentException("Fusion output amount must be positive");
        }
        if (energyCost < 0) {
            throw new IllegalArgumentException("Fusion energy cost cannot be negative");
        }
    }

    public ItemStack[] toGuideRecipe() {
        ItemStack[] recipe = new ItemStack[9];
        for (int i = 0; i < ingredients.size(); i++) {
            FusionIngredient ingredient = ingredients.get(i);
            recipe[i] = ingredient == null ? null : ingredient.toGuideItemStack();
        }
        return recipe;
    }

    public boolean matches(ItemStack[] input) {
        if (input == null || input.length != 9) {
            return false;
        }

        for (int i = 0; i < ingredients.size(); i++) {
            FusionIngredient expected = ingredients.get(i);
            ItemStack actual = input[i];
            if (expected == null) {
                if (actual != null && !actual.getType().isAir()) {
                    return false;
                }
            } else if (!expected.matches(actual)) {
                return false;
            }
        }
        return true;
    }
}
