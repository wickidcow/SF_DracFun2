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
import javax.annotation.Nonnull;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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

    private static final int[] INPUTS = {0, 1, 2, 9, 10, 11, 18, 19, 20};
    private static final int STATUS = 13;
    private static final int OUTPUT = 16;

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
                setSize(27);
                int[] background = {3, 4, 5, 6, 7, 8, 12, 14, 15, 17, 21, 22, 23, 24, 25, 26};
                drawBackground(background);
                addItem(STATUS, statusItem("Ready", 0L), ChestMenuUtils.getEmptyClickHandler());
                addMenuClickHandler(OUTPUT, (player, slot, clicked, action) -> !isEmpty(clicked));
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
            error(player, "Fusion Crafter data is not ready yet.");
            return;
        }

        if (isActive(data)) {
            error(player, "A fusion is already in progress.");
            return;
        }
        if (!isEmpty(menu.getItemInSlot(OUTPUT))) {
            error(player, "Take the existing output before starting another fusion.");
            return;
        }

        FusionRecipeSpec recipe = findRecipe(readInputs(menu));
        if (recipe == null) {
            error(player, "No valid " + tier.displayName() + " Fusion recipe matches these inputs.");
            return;
        }

        ItemStack output = createOutput(recipe);
        if (output == null) {
            error(player, "The output " + recipe.outputId() + " is not enabled or available on this server.");
            return;
        }
        if (!menu.fits(output, OUTPUT)) {
            error(player, "The output slot cannot accept this fusion result.");
            return;
        }

        long charge = getChargeLong(block.getLocation(), data);
        if (charge < recipe.energyCost()) {
            error(player, "This fusion requires " + recipe.energyCost() + " J; stored energy is " + charge + " J.");
            return;
        }

        long completeAt = System.currentTimeMillis() + FUSION_DELAY_MILLIS;
        data.setData(DATA_OUTPUT, recipe.outputId());
        data.setData(DATA_COMPLETE_AT, Long.toString(completeAt));
        menu.replaceExistingItem(STATUS, statusItem("Fusion in progress", FUSION_DELAY_MILLIS));
        player.sendMessage(ChatColor.LIGHT_PURPLE + "Fusion started. Inputs and energy will be committed on completion.");
    }

    private void tickFusion(Block block, SlimefunBlockData data) {
        BlockMenu menu = data.getBlockMenu();
        if (menu == null) {
            return;
        }

        String outputId = data.getData(DATA_OUTPUT);
        if (outputId == null || outputId.isBlank()) {
            if (menu.hasViewer()) {
                menu.replaceExistingItem(STATUS, statusItem("Ready", 0L));
            }
            return;
        }

        long completeAt = parseLong(data.getData(DATA_COMPLETE_AT));
        long remaining = Math.max(0L, completeAt - System.currentTimeMillis());
        if (remaining > 0L) {
            if (menu.hasViewer()) {
                menu.replaceExistingItem(STATUS, statusItem("Fusion in progress", remaining));
            }
            return;
        }

        finishFusion(block, data, menu, outputId);
    }

    private void finishFusion(Block block, SlimefunBlockData data, BlockMenu menu, String outputId) {
        FusionRecipeSpec recipe = findRecipeByOutput(outputId, readInputs(menu));
        if (recipe == null) {
            clearPending(data);
            menu.replaceExistingItem(STATUS, statusItem("Cancelled: inputs changed", 0L));
            return;
        }

        ItemStack output = createOutput(recipe);
        if (output == null || !menu.fits(output, OUTPUT)) {
            clearPending(data);
            menu.replaceExistingItem(STATUS, statusItem("Cancelled: output unavailable", 0L));
            return;
        }

        long charge = getChargeLong(block.getLocation(), data);
        if (charge < recipe.energyCost()) {
            clearPending(data);
            menu.replaceExistingItem(STATUS, statusItem("Cancelled: insufficient energy", 0L));
            return;
        }

        for (int input : INPUTS) {
            if (!isEmpty(menu.getItemInSlot(input))) {
                menu.consumeItem(input, 1, true);
            }
        }
        removeCharge(block.getLocation(), recipe.energyCost(), data);
        menu.pushItem(output, OUTPUT);
        clearPending(data);
        menu.replaceExistingItem(STATUS, statusItem("Fusion complete", 0L));
    }

    private FusionRecipeSpec findRecipe(ItemStack[] input) {
        for (FusionRecipeSpec recipe : recipes) {
            if (tier.accepts(recipe.tier()) && recipe.matches(input)) {
                return recipe;
            }
        }
        return null;
    }

    private FusionRecipeSpec findRecipeByOutput(String outputId, ItemStack[] input) {
        for (FusionRecipeSpec recipe : recipes) {
            if (tier.accepts(recipe.tier()) && recipe.outputId().equals(outputId) && recipe.matches(input)) {
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

    private ItemStack statusItem(String state, long remainingMillis) {
        ItemStack item = new ItemStack(Material.END_CRYSTAL);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE + tier.displayName() + " Fusion Crafter");
        java.util.List<String> lore = new java.util.ArrayList<>();
        lore.add(ChatColor.GRAY + "State: " + ChatColor.WHITE + state);
        lore.add(ChatColor.GRAY + "Capacity: " + ChatColor.WHITE + tier.capacity() + " J");
        if (remainingMillis > 0L) {
            lore.add(ChatColor.GRAY + "Completes in: " + ChatColor.WHITE + Math.max(1L, (remainingMillis + 999L) / 1000L) + "s");
        } else {
            lore.add(ChatColor.YELLOW + "Click to start a matching fusion recipe.");
        }
        meta.setLore(lore);
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

    private static void error(Player player, String message) {
        player.sendMessage(ChatColor.RED + message);
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
