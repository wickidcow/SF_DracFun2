package io.github.wickidcow.sfdracfun2.modular;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import io.github.wickidcow.sfdracfun2.compat.ProtectionCompat;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;

/**
 * Clean-room module installation station.
 *
 * <p>The UI is intentionally new; only the observable module compatibility/data
 * contracts from DracFun 2.0.10 are preserved.</p>
 */
public final class ModuleIntegratorMachine extends SlimefunItem {

    private static final int GEAR_INPUT = 10;
    private static final int GUIDE_SLOT = 11;
    private static final int MODULE_INPUT = 12;
    private static final int INSTALL_BUTTON = 14;
    private static final int OUTPUT = 16;
    private static final int REMOVE_BUTTON = 26;

    private static final int[] INPUT_BORDER = {
        0, 1, 2, 3, 4, 9, 11, 13, 18, 19, 20, 21, 22
    };
    private static final int[] OUTPUT_BORDER = {
        6, 7, 8, 15, 17, 24, 25, 26
    };
    private static final int[] MODULE_OUTPUT_BORDER = {
        27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 44, 45, 53
    };
    private static final int[] MODULE_OUTPUTS = {
        37, 38, 39, 40, 41, 42, 43, 46, 47, 48, 49, 50, 51, 52
    };

    public ModuleIntegratorMachine(
            ItemGroup group,
            SlimefunItemStack item,
            RecipeType recipeType,
            ItemStack[] recipe) {
        super(group, item, recipeType, recipe);

        new BlockMenuPreset(getId(), getItemName()) {
            @Override
            public void init() {
                setSize(54);
                constructMenu(this);
            }

            @Override
            public void newInstance(BlockMenu menu, Block block) {
                menu.addMenuClickHandler(INSTALL_BUTTON, (player, slot, clicked, action) -> {
                    install(menu, player);
                    return false;
                });
                menu.addMenuClickHandler(REMOVE_BUTTON, (player, slot, clicked, action) -> {
                    removeAll(menu, player);
                    return false;
                });
            }

            @Override
            public boolean canOpen(Block block, Player player) {
                return ProtectionCompat.canInteract(player, block.getLocation());
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                return new int[0];
            }
        };

        addItemHandler(new BlockBreakHandler(false, false) {
            @Override
            public void onPlayerBreak(BlockBreakEvent event, ItemStack item, java.util.List<ItemStack> drops) {
                var data = Slimefun.getDatabaseManager()
                        .getBlockDataController()
                        .getBlockData(event.getBlock().getLocation());
                if (data != null && data.getBlockMenu() != null) {
                    BlockMenu menu = data.getBlockMenu();
                    menu.dropItems(menu.getLocation(), new int[] {GEAR_INPUT, MODULE_INPUT, OUTPUT});
                    menu.dropItems(menu.getLocation(), MODULE_OUTPUTS);
                }
            }
        });
    }

    private void constructMenu(BlockMenuPreset preset) {
        preset.drawBackground(ChestMenuUtils.getInputSlotTexture(), INPUT_BORDER);
        preset.drawBackground(ChestMenuUtils.getOutputSlotTexture(), OUTPUT_BORDER);
        preset.drawBackground(new ItemStack(Material.LIME_STAINED_GLASS_PANE), MODULE_OUTPUT_BORDER);

        preset.addItem(5, new ItemStack(Material.BLACK_STAINED_GLASS_PANE), ChestMenuUtils.getEmptyClickHandler());
        preset.addItem(23, new ItemStack(Material.BLACK_STAINED_GLASS_PANE), ChestMenuUtils.getEmptyClickHandler());

        preset.addItem(
                GUIDE_SLOT,
                button(
                        Material.BOOK,
                        ChatColor.GREEN + "Place your item on the left and modifier on the right!"),
                ChestMenuUtils.getEmptyClickHandler());
        preset.addItem(
                INSTALL_BUTTON,
                button(Material.GREEN_STAINED_GLASS_PANE, ChatColor.GREEN + "Click to Integrate!"),
                ChestMenuUtils.getEmptyClickHandler());
        preset.addItem(
                REMOVE_BUTTON,
                button(Material.BARRIER, ChatColor.RED + "Removes all modules from your item!"),
                ChestMenuUtils.getEmptyClickHandler());
    }

    private void install(BlockMenu menu, Player player) {
        if (!isEmpty(menu.getItemInSlot(OUTPUT))) {
            error(player, "Unable to integrate module due to occupied output slot!");
            return;
        }

        ItemStack gearInput = menu.getItemInSlot(GEAR_INPUT);
        ItemStack moduleInput = menu.getItemInSlot(MODULE_INPUT);
        if (isEmpty(gearInput) || isEmpty(moduleInput)) {
            error(player, "Insert one modular gear item and one module.");
            return;
        }

        SlimefunItem gearSf = SlimefunItem.getByItem(gearInput);
        SlimefunItem moduleSf = SlimefunItem.getByItem(moduleInput);
        if (!(gearSf instanceof ModularGearItem gear)) {
            error(player, "Module integration is restricted to modular items exclusively!");
            return;
        }
        if (!(moduleSf instanceof ModuleItem module)) {
            error(player, "Invalid Module!");
            return;
        }

        ItemStack resultStack = gearInput.clone();
        resultStack.setAmount(1);
        ModuleInstallResult result = ModularData.installModule(
                resultStack,
                gear.getGearType(),
                gear.getGearTier(),
                module.getFamily(),
                module.getTier());
        if (result != ModuleInstallResult.VALID) {
            error(player, explain(result));
            return;
        }

        ModularLore.refresh(resultStack, gear);
        menu.replaceExistingItem(GEAR_INPUT, null);
        consumeOne(menu, MODULE_INPUT, moduleInput);
        menu.replaceExistingItem(OUTPUT, resultStack);
    }

    private void removeAll(BlockMenu menu, Player player) {
        if (!isEmpty(menu.getItemInSlot(OUTPUT))) {
            error(player, "Unable to remove modules due to occupied output slot!");
            return;
        }

        for (int slot : MODULE_OUTPUTS) {
            if (!isEmpty(menu.getItemInSlot(slot))) {
                error(player, "Unable to remove modules. Ensure all output slots are empty before proceeding!");
                return;
            }
        }

        ItemStack gearInput = menu.getItemInSlot(GEAR_INPUT);
        if (isEmpty(gearInput)) {
            return;
        }

        SlimefunItem gearSf = SlimefunItem.getByItem(gearInput);
        if (!(gearSf instanceof ModularGearItem gear)) {
            error(player, "Module removal is restricted to modular items exclusively!");
            return;
        }

        ItemStack resultStack = gearInput.clone();
        resultStack.setAmount(1);

        int outputIndex = 0;
        for (ModuleFamily family : ModuleFamily.values()) {
            for (ModuleTier tier : family.supportedTiers()) {
                int count = ModularData.getModuleCount(resultStack, family, tier);
                if (count <= 0) {
                    continue;
                }

                SlimefunItem registered = SlimefunItem.getById(family.legacyItemId(tier));
                if (!(registered instanceof ModuleItem) || outputIndex >= MODULE_OUTPUTS.length) {
                    continue;
                }

                ItemStack returned = registered.getItem().clone();
                returned.setAmount(count);
                menu.replaceExistingItem(MODULE_OUTPUTS[outputIndex++], returned);
            }
        }

        ModularData.removeAllModules(resultStack);
        ModularLore.refresh(resultStack, gear);

        menu.replaceExistingItem(GEAR_INPUT, null);
        menu.pushItem(resultStack, OUTPUT);
    }

    private void consumeOne(BlockMenu menu, int slot, ItemStack input) {
        if (input.getAmount() <= 1) {
            menu.replaceExistingItem(slot, null);
        } else {
            ItemStack reduced = input.clone();
            reduced.setAmount(input.getAmount() - 1);
            menu.replaceExistingItem(slot, reduced);
        }
    }

    private static ItemStack button(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    private static boolean isEmpty(ItemStack stack) {
        return stack == null || stack.getType().isAir();
    }

    private static String explain(ModuleInstallResult result) {
        return switch (result) {
            case UNSUPPORTED_TIER -> "That module tier did not exist for this module family in DracFun 2.0.10.";
            case MODULE_TIER_TOO_HIGH -> "The module surpasses the capabilities of the designated item!";
            case INCOMPATIBLE_GEAR -> "This module is incompatible with the specified item type!";
            case FAMILY_LIMIT_REACHED -> "The application of additional modules to this type is restricted due to the existing limit!";
            case MODULE_POINTS_EXCEEDED -> "The application of additional modules to this item is restricted due to the existing limit!";
            case VALID -> "Module can be installed.";
        };
    }

    private static void error(Player player, String message) {
        player.sendMessage(ChatColor.RED + message);
    }
}
