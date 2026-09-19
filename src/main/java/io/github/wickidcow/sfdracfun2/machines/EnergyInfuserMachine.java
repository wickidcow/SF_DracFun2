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
                int[] background = new int[] {
                    0, 1, 2, 3, 4, 5, 6, 7, 8,
                    9, 11, 12, 14, 15, 17,
                    18, 19, 20, 21, 22, 23, 24, 25, 26
                };
                drawBackground(background);
                addItem(STATUS, statusItem(0), ChestMenuUtils.getEmptyClickHandler());
                addMenuClickHandler(OUTPUT, (player, slot, clicked, action) -> !isEmpty(clicked));
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

        // DracFun 2.0.10 left vanilla/unregistered items in the input slot.
        if (input.getItemMeta() == null) {
            return;
        }

        SlimefunItem sfItem = SlimefunItem.getByItem(input);
        if (sfItem == null) {
            return;
        }
        if (!(sfItem instanceof ModularGearItem gear)) {
            moveToOutput(menu, input);
            return;
        }

        int capacity = ModularData.getCapacity(input);
        int charge = ModularData.getCharge(input);
        if (capacity == charge || capacity == 0) {
            moveToOutput(menu, input);
            return;
        }

        int needed = capacity - charge;
        long availableUnits = machineCharge / JOULES_PER_ITEM_CHARGE;
        int transfer = (int) Math.min(needed, Math.min(availableUnits, Integer.MAX_VALUE));

        // Preserve 2.0.10's exact integer-conversion behavior: when the machine
        // cannot finish the item, all stored J are consumed, including a <1000 J
        // remainder that cannot become one item-charge unit.
        if (needed > availableUnits) {
            ModularData.addCharge(input, transfer);
            if (machineCharge > 0) {
                removeCharge(block.getLocation(), machineCharge, data);
            }
        } else {
            ModularData.addCharge(input, needed);
            removeCharge(
                    block.getLocation(),
                    (long) needed * JOULES_PER_ITEM_CHARGE,
                    data);
        }

        ModularLore.refresh(input, gear);
        menu.replaceExistingItem(INPUT, input);
        // The original moved a now-full item on the following ticker pass.
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
        ItemStack item = new ItemStack(Material.LIGHTNING_ROD);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + "Energy Infuser");
        meta.setLore(java.util.List.of(
                ChatColor.GRAY + "Stored: " + ChatColor.GREEN + charge + ChatColor.GRAY + " / " + ENERGY_CAPACITY + " J",
                ChatColor.GRAY + "Rate: " + ChatColor.WHITE + JOULES_PER_ITEM_CHARGE + " J per item charge"));
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
