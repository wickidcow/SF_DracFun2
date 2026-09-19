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
    private static final int MODULE_INPUT = 12;
    private static final int INSTALL_BUTTON = 14;
    private static final int OUTPUT = 16;
    private static final int REMOVE_BUTTON = 26;
    private static final int[] MODULE_OUTPUTS = {
        37, 38, 39, 40, 41, 42, 43,
        46, 47, 48, 49, 50, 51, 52
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
                    menu.dropItems(menu.getLocation(), breakDropSlots());
                }
            }
        });
    }

    private void constructMenu(BlockMenuPreset preset) {
        int[] inputBorder = {
            0, 1, 2, 3, 4, 9, 11, 13, 18, 19, 20, 21, 22
        };
        int[] outputBorder = {6, 7, 8, 15, 17, 24, 25};
        int[] moduleOutputBorder = {
            27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 44, 45, 53
        };

        preset.drawBackground(ChestMenuUtils.getInputSlotTexture(), inputBorder);
        preset.drawBackground(ChestMenuUtils.getOutputSlotTexture(), outputBorder);
        preset.drawBackground(new ItemStack(Material.LIME_STAINED_GLASS_PANE), moduleOutputBorder);
        preset.addItem(5, new ItemStack(Material.BLACK_STAINED_GLASS_PANE), ChestMenuUtils.getEmptyClickHandler());
        preset.addItem(23, new ItemStack(Material.BLACK_STAINED_GLASS_PANE), ChestMenuUtils.getEmptyClickHandler());
        preset.addItem(11, button(Material.BOOK, "&aModule Integrator Guide",
                "&7Place modular gear in slot 10.",
                "&7Place a module in slot 12.",
                "&7Removed modules appear below."), ChestMenuUtils.getEmptyClickHandler());
        preset.addItem(INSTALL_BUTTON, button(Material.GREEN_STAINED_GLASS_PANE, "&aInstall Module"),
                ChestMenuUtils.getEmptyClickHandler());
        preset.addItem(REMOVE_BUTTON, button(Material.BARRIER, "&cRemove All Modules",
                "&7Returns installed modules to the",
                "&7dedicated output slots below."), ChestMenuUtils.getEmptyClickHandler());

        preset.addMenuClickHandler(OUTPUT, (player, slot, clicked, action) -> !isEmpty(clicked));
        for (int slot : MODULE_OUTPUTS) {
            preset.addMenuClickHandler(slot, (player, clickedSlot, clicked, action) -> !isEmpty(clicked));
        }
    }

    private void install(BlockMenu menu, Player player) {
        if (!isEmpty(menu.getItemInSlot(OUTPUT))) {
            error(player, "Take the existing output before installing another module.");
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
            error(player, "The gear slot only accepts DracFun modular equipment.");
            return;
        }
        if (!(moduleSf instanceof ModuleItem module)) {
            error(player, "The module slot only accepts DracFun modules.");
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
        player.sendMessage(ChatColor.GREEN + "Module installed.");
    }

    private void removeAll(BlockMenu menu, Player player) {
        if (!isEmpty(menu.getItemInSlot(OUTPUT))) {
            error(player, "Take the existing output before removing modules.");
            return;
        }
        for (int slot : MODULE_OUTPUTS) {
            if (!isEmpty(menu.getItemInSlot(slot))) {
                error(player, "Empty all module-return output slots before removing modules.");
                return;
            }
        }

        ItemStack gearInput = menu.getItemInSlot(GEAR_INPUT);
        if (isEmpty(gearInput)) {
            error(player, "Insert modular gear in the gear slot first.");
            return;
        }

        SlimefunItem gearSf = SlimefunItem.getByItem(gearInput);
        if (!(gearSf instanceof ModularGearItem gear)) {
            error(player, "The gear slot only accepts DracFun modular equipment.");
            return;
        }

        ItemStack resultStack = gearInput.clone();
        resultStack.setAmount(1);
        java.util.List<ItemStack> returnedModules = collectModules(resultStack);
        if (returnedModules.size() > MODULE_OUTPUTS.length) {
            error(player, "Too many distinct module stacks to return safely.");
            return;
        }

        int returned = 0;
        for (int i = 0; i < returnedModules.size(); i++) {
            ItemStack stack = returnedModules.get(i);
            returned += stack.getAmount();
            menu.replaceExistingItem(MODULE_OUTPUTS[i], stack);
        }

        ModularData.removeAllModules(resultStack);
        ModularLore.refresh(resultStack, gear);

        menu.replaceExistingItem(GEAR_INPUT, null);
        menu.replaceExistingItem(OUTPUT, resultStack);
        player.sendMessage(ChatColor.GREEN + "Removed and returned " + returned + " module(s).");
    }

    private java.util.List<ItemStack> collectModules(ItemStack gear) {
        java.util.List<ItemStack> returned = new java.util.ArrayList<>();
        for (ModuleFamily family : ModuleFamily.values()) {
            for (ModuleTier tier : family.supportedTiers()) {
                int count = ModularData.getModuleCount(gear, family, tier);
                if (count <= 0) {
                    continue;
                }

                SlimefunItem registered = SlimefunItem.getById(family.legacyItemId(tier));
                if (!(registered instanceof ModuleItem)) {
                    continue;
                }

                ItemStack stack = registered.getItem().clone();
                stack.setAmount(count);
                returned.add(stack);
            }
        }
        return returned;
    }

    private static int[] breakDropSlots() {
        int[] slots = new int[3 + MODULE_OUTPUTS.length];
        slots[0] = GEAR_INPUT;
        slots[1] = MODULE_INPUT;
        slots[2] = OUTPUT;
        System.arraycopy(MODULE_OUTPUTS, 0, slots, 3, MODULE_OUTPUTS.length);
        return slots;
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

    private static ItemStack button(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
        java.util.List<String> lines = new java.util.ArrayList<>();
        for (String line : lore) {
            lines.add(ChatColor.translateAlternateColorCodes('&', line));
        }
        meta.setLore(lines);
        item.setItemMeta(meta);
        return item;
    }

    private static boolean isEmpty(ItemStack stack) {
        return stack == null || stack.getType().isAir();
    }

    private static String explain(ModuleInstallResult result) {
        return switch (result) {
            case UNSUPPORTED_TIER -> "That module tier did not exist for this module family in DracFun 2.0.10.";
            case MODULE_TIER_TOO_HIGH -> "The module tier is higher than the gear tier.";
            case INCOMPATIBLE_GEAR -> "That module type cannot be installed in this kind of gear.";
            case FAMILY_LIMIT_REACHED -> "This gear has reached the installation limit for that module type.";
            case MODULE_POINTS_EXCEEDED -> "Installing that module would exceed the gear's module-point capacity.";
            case VALID -> "Module can be installed.";
        };
    }

    private static void error(Player player, String message) {
        player.sendMessage(ChatColor.RED + message);
    }
}
