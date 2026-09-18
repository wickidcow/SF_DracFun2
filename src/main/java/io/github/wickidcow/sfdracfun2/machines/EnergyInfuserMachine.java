package io.github.wickidcow.sfdracfun2.machines;

import com.xzavier0722.mc.plugin.slimefun4.storage.controller.SlimefunBlockData;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import io.github.wickidcow.sfdracfun2.compat.ProtectionCompat;
import io.github.wickidcow.sfdracfun2.modular.ModularData;
import io.github.wickidcow.sfdracfun2.modular.ModularGearItem;
import io.github.wickidcow.sfdracfun2.modular.ModularLore;
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
 * Clean-room Energy Infuser preserving the observable 2.0.10 energy conversion contract.
 */
public final class EnergyInfuserMachine extends SlimefunItem implements EnergyNetComponent {

    public static final int ENERGY_CAPACITY = 100_000_000;
    public static final int JOULES_PER_ITEM_CHARGE = 1_000;

    private static final int INPUT = 10;
    private static final int STATUS = 13;
    private static final int OUTPUT = 16;

    private static final int[] INPUT_BORDER = {0, 1, 2, 9, 11, 18, 19, 20};
    private static final int[] OUTPUT_BORDER = {6, 7, 8, 15, 17, 24, 25, 26};
    private static final int[] BACKGROUND = {3, 4, 5, 12, 14, 21, 22, 23};

    public EnergyInfuserMachine(
            ItemGroup group,
            SlimefunItemStack item,
            RecipeType recipeType,
            ItemStack[] recipe) {
        super(group, item, recipeType, recipe);

        new BlockMenuPreset(getId(), getItemName()) {
            @Override
            public void init() {
                setSize(27);
                drawBackground(BACKGROUND);
                drawBackground(new ItemStack(Material.BLUE_STAINED_GLASS_PANE), INPUT_BORDER);
                drawBackground(new ItemStack(Material.ORANGE_STAINED_GLASS_PANE), OUTPUT_BORDER);
                addItem(STATUS, statusItem(0), ChestMenuUtils.getEmptyClickHandler());
            }

            @Override
            public void newInstance(BlockMenu menu, Block block) {}

            @Override
            public boolean canOpen(Block block, Player player) {
                return ProtectionCompat.canInteract(player, block.getLocation());
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                return flow == ItemTransportFlow.INSERT ? new int[] {INPUT} : new int[] {OUTPUT};
            }
        };

        addItemHandler(new BlockTicker() {
            @Override
            public boolean isSynchronized() {
                return true;
            }

            @Override
            public void tick(Block block, SlimefunItem item, SlimefunBlockData data) {
                tickMachine(block, data);
            }
        });

        addItemHandler(new BlockBreakHandler(false, false) {
            @Override
            public void onPlayerBreak(BlockBreakEvent event, ItemStack item, java.util.List<ItemStack> drops) {
                var data = io.github.thebusybiscuit.slimefun4.implementation.Slimefun.getDatabaseManager()
                        .getBlockDataController()
                        .getBlockData(event.getBlock().getLocation());
                if (data != null && data.getBlockMenu() != null) {
                    BlockMenu menu = data.getBlockMenu();
                    menu.dropItems(menu.getLocation(), new int[] {INPUT, OUTPUT});
                }
            }
        });
    }

    private void tickMachine(Block block, SlimefunBlockData data) {
        BlockMenu menu = data.getBlockMenu();
        if (menu == null) {
            return;
        }

        long machineCharge = getChargeLong(block.getLocation(), data);
        if (menu.hasViewer()) {
            menu.replaceExistingItem(STATUS, statusItem(machineCharge));
        }

        ItemStack input = menu.getItemInSlot(INPUT);
        if (isEmpty(input) || input.getAmount() != 1) {
            return;
        }

        SlimefunItem sfItem = SlimefunItem.getByItem(input);
        if (!(sfItem instanceof ModularGearItem gear)) {
            moveToOutput(menu, input);
            return;
        }

        int capacity = ModularData.getCapacity(input);
        int charge = ModularData.getCharge(input);
        if (capacity <= 0 || charge >= capacity) {
            moveToOutput(menu, input);
            return;
        }

        long availableUnits = machineCharge / JOULES_PER_ITEM_CHARGE;
        if (availableUnits <= 0) {
            return;
        }

        int needed = capacity - charge;
        int transfer = (int) Math.min(needed, Math.min(availableUnits, Integer.MAX_VALUE));
        if (transfer <= 0) {
            return;
        }

        ModularData.addCharge(input, transfer);
        ModularLore.refresh(input, gear);
        menu.replaceExistingItem(INPUT, input);
        removeCharge(block.getLocation(), (long) transfer * JOULES_PER_ITEM_CHARGE, data);

        if (ModularData.getCharge(input) >= capacity) {
            moveToOutput(menu, input);
        }
    }

    private static void moveToOutput(BlockMenu menu, ItemStack input) {
        if (!menu.fits(input, OUTPUT)) {
            return;
        }
        ItemStack remainder = menu.pushItem(input.clone(), OUTPUT);
        if (remainder == null || remainder.getAmount() <= 0) {
            menu.replaceExistingItem(INPUT, null);
        }
    }

    private static ItemStack statusItem(long charge) {
        // The original used its Electric category custom head here. Reborn
        // keeps a vanilla icon while preserving the exact observable title/lore.
        ItemStack item = new ItemStack(Material.LIGHTNING_ROD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Current Power");
        meta.setLore(java.util.List.of(ChatColor.GREEN + Long.toString(charge)));
        item.setItemMeta(meta);
        return item;
    }

    private static boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir();
    }

    @Override
    public @Nonnull EnergyNetComponentType getEnergyComponentType() {
        return EnergyNetComponentType.CONSUMER;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getCapacity() {
        return ENERGY_CAPACITY;
    }
}
