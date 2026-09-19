package io.github.wickidcow.sfdracfun2.fusion;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import io.github.wickidcow.sfdracfun2.compat.LegacyCompatibilityRegistry;
import io.github.wickidcow.sfdracfun2.compat.ProtectionCompat;
import java.util.List;
import java.util.UUID;
import javax.annotation.Nonnull;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Clean-room Fusion Crafter preserving the observable 2.0.10 recipe/tier contract.
 *
 * <p>The original addon consumed energy and inputs immediately and then completed from
 * an asynchronous Bukkit task 100 ticks later. Reborn keeps the five-second fusion time
 * but persists the pending output and completion timestamp in Slimefun block data. The
 * synchronized block ticker performs the final validation and mutation, making fusion
 * restart/chunk-unload safe and avoiding asynchronous inventory access.</p>
 */
public final class FusionCrafterMachine extends SlimefunItem implements EnergyNetComponent {

    public static final long FUSION_DELAY_MILLIS = 5_000L;

    private static final String DATA_OUTPUT = "reborn-fusion-output";
    private static final String DATA_COMPLETE_AT = "reborn-fusion-complete-at";
    private static final String DATA_PLAYER = "reborn-fusion-player";

    private static final int[] INPUTS = {10, 13, 16, 19, 25, 28, 34, 37, 43};
    private static final int STATUS = 31;
    private static final int OUTPUT = 40;

    private static final int[] OUTPUT_BORDER = {30, 31, 32, 39, 41, 48, 49, 50};
    private static final int[] INPUT_BORDER = {
        0, 1, 2, 6, 7, 8, 9, 11, 15, 17, 18, 20, 24, 26,
        27, 29, 33, 35, 36, 38, 42, 44, 45, 46, 47, 51, 52, 53
    };
    private static final int[] BACKGROUND = {3, 4, 5, 12, 14, 21, 22, 23};

    private final FusionTier tier;
    private final List<FusionRecipeSpec> recipes;

    public FusionCrafterMachine(
            ItemGroup group,
            SlimefunItemStack item,
            FusionTier tier,
            List<FusionRecipeSpec> recipes,
            ItemStack[] craftingRecipe) {
        super(group, item, RecipeType.ANCIENT_ALTAR, craftingRecipe);
        this.tier = tier;
        this.recipes = List.copyOf(recipes);

        new BlockMenuPreset(getId(), getItemName()) {
            @Override
            public void init() {
                setSize(54);
                drawBackground(new ItemStack(Material.ORANGE_STAINED_GLASS_PANE), OUTPUT_BORDER);
                drawBackground(new ItemStack(Material.BLUE_STAINED_GLASS_PANE), INPUT_BORDER);
                drawBackground(BACKGROUND);
                addItem(STATUS, statusItem(), ChestMenuUtils.getEmptyClickHandler());
            }

            @Override
            public void newInstance(BlockMenu menu, Block block) {
                menu.addMenuClickHandler(STATUS, (player, slot, clicked, action) -> {
                    startFusion(block, menu, player);
                    return false;
                });
            }

            @Override
            public boolean canOpen(Block block, Player player) {
                return ProtectionCompat.canInteract(player, block.getLocation());
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                return flow == ItemTransportFlow.INSERT ? INPUTS.clone() : new int[] {OUTPUT};
            }
        };

        addItemHandler(new BlockTicker() {
            @Override
            public boolean isSynchronized() {
                return true;
            }

            @Override
            public void tick(Block block, SlimefunItem item, SlimefunBlockData data) {
                tickFusion(block, data);
            }
        });

        addItemHandler(new BlockBreakHandler(false, false) {
            @Override
            public void onPlayerBreak(BlockBreakEvent event, ItemStack item, java.util.List<ItemStack> drops) {
                SlimefunBlockData data = Slimefun.getDatabaseManager()
                        .getBlockDataController()
                        .getBlockData(event.getBlock().getLocation());
                if (data != null && data.getBlockMenu() != null) {
                    BlockMenu menu = data.getBlockMenu();
                    menu.dropItems(menu.getLocation(), inputAndOutputSlots());
                }
            }
        });
    }

    private void startFusion(Block block, BlockMenu menu, Player player) {
        SlimefunBlockData data = Slimefun.getDatabaseManager()
                .getBlockDataController()
                .getBlockData(block.getLocation());
        if (data == null || !data.isDataLoaded() || data.isPendingRemove()) {
            return;
        }

        if (isActive(data)) {
            return;
        }
        if (!isEmpty(menu.getItemInSlot(OUTPUT))) {
            warning(player, "You may not start another fusion as output slot is occupied!");
            return;
        }

        FusionRecipeSpec recipe = findRecipe(readInputs(menu));
        if (recipe == null) {
            warning(player, "Invalid recipe!");
            return;
        }

        ItemStack output = createOutput(recipe);
        if (output == null) {
            return;
        }
        if (!menu.fits(output, OUTPUT)) {
            return;
        }

        long charge = getChargeLong(block.getLocation(), data);
        if (charge < recipe.energyCost()) {
            warning(player, "This Fusion Craft requires a minimum of " + recipe.energyCost() + "J of power!");
            return;
        }

        player.playSound(
                block.getLocation(),
                "dracfun:dracfun.fusion_rotation",
                SoundCategory.BLOCKS,
                1F,
                1F);

        // DracFun 2.0.10 committed the transaction at button-click time:
        // energy was removed and one item from every occupied input slot was
        // consumed before the 100-tick completion task was scheduled.
        removeCharge(block.getLocation(), recipe.energyCost(), data);
        for (int input : INPUTS) {
            if (!isEmpty(menu.getItemInSlot(input))) {
                menu.consumeItem(input, 1, true);
            }
        }

        long completeAt = System.currentTimeMillis() + FUSION_DELAY_MILLIS;
        data.setData(DATA_OUTPUT, recipe.outputId());
        data.setData(DATA_COMPLETE_AT, Long.toString(completeAt));
        data.setData(DATA_PLAYER, player.getUniqueId().toString());
        menu.replaceExistingItem(STATUS, statusItem());
    }

    private void tickFusion(Block block, SlimefunBlockData data) {
        BlockMenu menu = data.getBlockMenu();
        if (menu == null) {
            return;
        }

        String outputId = data.getData(DATA_OUTPUT);
        if (outputId == null || outputId.isBlank()) {
            if (menu.hasViewer()) {
                menu.replaceExistingItem(STATUS, statusItem());
            }
            return;
        }

        long completeAt = parseLong(data.getData(DATA_COMPLETE_AT));
        long remaining = Math.max(0L, completeAt - System.currentTimeMillis());
        if (remaining > 0L) {
            if (menu.hasViewer()) {
                menu.replaceExistingItem(STATUS, statusItem());
            }
            return;
        }

        finishFusion(block, data, menu, outputId);
    }

    private void finishFusion(Block block, SlimefunBlockData data, BlockMenu menu, String outputId) {
        FusionRecipeSpec recipe = findRecipeByOutput(outputId);
        if (recipe == null) {
            // A committed legacy-style fusion must never refund or silently
            // discard its transaction. Keep it pending for an administrator to
            // diagnose rather than cancelling spent inputs/energy.
            menu.replaceExistingItem(STATUS, statusItem());
            return;
        }

        ItemStack output = createOutput(recipe);
        if (output == null || !menu.fits(output, OUTPUT)) {
            // The original delayed task simply attempted to push its result.
            // Reborn waits instead of deleting a committed result if the output
            // cannot currently be delivered.
            menu.replaceExistingItem(STATUS, statusItem());
            return;
        }

        String playerId = data.getData(DATA_PLAYER);
        if (playerId != null && !playerId.isBlank()) {
            try {
                Player player = Bukkit.getPlayer(UUID.fromString(playerId));
                if (player != null && player.isOnline()) {
                    player.playSound(
                            block.getLocation(),
                            "dracfun:dracfun.fusion_complete",
                            SoundCategory.BLOCKS,
                            1F,
                            1F);
                }
            } catch (IllegalArgumentException ignored) {
                // Corrupt/missing legacy initiator metadata must not block the committed output.
            }
        }

        menu.pushItem(output, OUTPUT);
        clearPending(data);
        menu.replaceExistingItem(STATUS, statusItem());
    }

    private FusionRecipeSpec findRecipe(ItemStack[] input) {
        for (FusionRecipeSpec recipe : recipes) {
            if (tier.accepts(recipe.tier()) && recipe.matches(input)) {
                return recipe;
            }
        }
        return null;
    }

    private FusionRecipeSpec findRecipeByOutput(String outputId) {
        for (FusionRecipeSpec recipe : recipes) {
            if (tier.accepts(recipe.tier()) && recipe.outputId().equals(outputId)) {
                return recipe;
            }
        }
        return null;
    }

    private static ItemStack[] readInputs(BlockMenu menu) {
        ItemStack[] stacks = new ItemStack[INPUTS.length];
        for (int i = 0; i < INPUTS.length; i++) {
            stacks[i] = menu.getItemInSlot(INPUTS[i]);
        }
        return stacks;
    }

    private static ItemStack createOutput(FusionRecipeSpec recipe) {
        SlimefunItem target = SlimefunItem.getById(recipe.outputId());
        if (target == null || LegacyCompatibilityRegistry.isPlaceholder(target)) {
            return null;
        }
        ItemStack output = target.getItem().clone();
        output.setAmount(recipe.outputAmount());
        return output;
    }

    private static boolean isActive(SlimefunBlockData data) {
        String output = data.getData(DATA_OUTPUT);
        return output != null && !output.isBlank();
    }

    private static void clearPending(SlimefunBlockData data) {
        data.removeData(DATA_OUTPUT);
        data.removeData(DATA_COMPLETE_AT);
        data.removeData(DATA_PLAYER);
    }

    private static long parseLong(String value) {
        if (value == null) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    private ItemStack statusItem() {
        // Original used a custom arrow-down head; use a vanilla arrow without
        // copying the original bundled asset.
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Click To Start Fusion Crafting!");
        meta.setLore(java.util.List.of(""));
        item.setItemMeta(meta);
        return item;
    }

    private static int[] inputAndOutputSlots() {
        int[] slots = new int[INPUTS.length + 1];
        System.arraycopy(INPUTS, 0, slots, 0, INPUTS.length);
        slots[slots.length - 1] = OUTPUT;
        return slots;
    }

    private static boolean isEmpty(ItemStack stack) {
        return stack == null || stack.getType().isAir();
    }

    private static void warning(Player player, String message) {
        player.sendMessage(ChatColor.YELLOW + message);
    }

    @Override
    public @Nonnull EnergyNetComponentType getEnergyComponentType() {
        return EnergyNetComponentType.CONSUMER;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getCapacity() {
        return tier.capacity();
    }
}
