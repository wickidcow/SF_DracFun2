package io.github.wickidcow.sfdracfun2.guardian;

import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.NotPlaceable;
import io.github.thebusybiscuit.slimefun4.core.handlers.ItemUseHandler;
import org.bukkit.inventory.ItemStack;

/**
 * Clean-room Chaos Orb of Invocation.
 *
 * <p>Preserves DracFun 2.0.10's invocation ordering: once an eligible End
 * battle is found, one orb is consumed before the modular-armor check.</p>
 */
public final class ChaosOrbItem extends SlimefunItem implements NotPlaceable, ItemUseHandler {

    private final ChaosGuardianService guardianService;

    public ChaosOrbItem(
            ItemGroup itemGroup,
            SlimefunItemStack item,
            RecipeType recipeType,
            ItemStack[] recipe,
            ChaosGuardianService guardianService) {
        super(itemGroup, item, recipeType, recipe);
        this.guardianService = guardianService;
        addItemHandler(this);
    }

    @Override
    public void onRightClick(PlayerRightClickEvent event) {
        event.cancel();
        guardianService.invoke(event.getPlayer(), event.getItem());
    }
}
