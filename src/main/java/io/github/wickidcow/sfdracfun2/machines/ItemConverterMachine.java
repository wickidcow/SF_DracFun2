package io.github.wickidcow.sfdracfun2.machines;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.utils.ChestMenuUtils;
import io.github.wickidcow.sfdracfun2.compat.LegacyPlaceholderItem;
import io.github.wickidcow.sfdracfun2.compat.ProtectionCompat;
import java.util.Map;
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
 * Rebuilds the specific old DracFun items accepted by the 2.0.10 Item Converter.
 *
 * <p>Conversion intentionally uses a fresh registered template and preserves only stack
 * amount. This mirrors the old converter's requirement for plain/unmodified inputs and
 * prevents stale legacy metadata from leaking into the rebuilt item.</p>
 */
public final class ItemConverterMachine extends SlimefunItem {

    private static final int INPUT = 10;
    private static final int CONVERT = 13;
    private static final int OUTPUT = 16;

    private static final Map<String, String> TARGETS = Map.ofEntries(
            Map.entry("DRACFUN_AWAKENED_DRACONIUM_BLOCK", "DRACFUN_AWAKENED_DRACONIUM_BLOCK"),
            Map.entry("DRACFUN_WYVERN_FLUX_CAPACITOR", "DRACFUN_WYVERN_FLUX_CAPACITOR"),
            Map.entry("DRACFUN_DRACONIC_FLUX_CAPACITOR", "DRACFUN_DRACONIC_FLUX_CAPACITOR"),
            Map.entry("DRACFUN_WYVERN_CHESTPLATE", "DRACFUN_WYVERN_ARMOR"),
            Map.entry("DRACFUN_DRACONIC_CHESTPLATE", "DRACFUN_DRACONIC_ARMOR"),
            Map.entry("DRACFUN_CHAOTIC_CHESTPLATE", "DRACFUN_CHAOTIC_ARMOR"),
            Map.entry("DRACFUN_WYVERN_AXE", "DRACFUN_WYVERN_AXE"),
            Map.entry("DRACFUN_DRACONIC_AXE", "DRACFUN_DRACONIC_AXE"),
            Map.entry("DRACFUN_WYVERN_BOW", "DRACFUN_WYVERN_BOW"),
            Map.entry("DRACFUN_DRACONIC_BOW", "DRACFUN_DRACONIC_BOW"),
            Map.entry("DRACFUN_WYVERN_SWORD", "DRACFUN_WYVERN_SWORD"),
            Map.entry("DRACFUN_DRACONIC_SWORD", "DRACFUN_DRACONIC_SWORD"),
            Map.entry("DRACFUN_WYVERN_PICKAXE", "DRACFUN_WYVERN_PICKAXE"),
            Map.entry("DRACFUN_DRACONIC_PICKAXE", "DRACFUN_DRACONIC_PICKAXE"),
            Map.entry("DRACFUN_WYVERN_SHOVEL", "DRACFUN_WYVERN_SHOVEL"),
            Map.entry("DRACFUN_DRACONIC_SHOVEL", "DRACFUN_DRACONIC_SHOVEL"),
            Map.entry("DRACFUN_WYVERN_HOE", "DRACFUN_WYVERN_HOE"),
            Map.entry("DRACFUN_DRACONIC_HOE", "DRACFUN_DRACONIC_HOE"));

    public ItemConverterMachine(ItemGroup group, SlimefunItemStack item) {
        super(group, item, RecipeType.NULL, new ItemStack[9]);
        setHidden(true);

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
                addItem(CONVERT, button(), ChestMenuUtils.getEmptyClickHandler());
                addMenuClickHandler(OUTPUT, (player, slot, clicked, action) -> !isEmpty(clicked));
            }

            @Override
            public void newInstance(BlockMenu menu, Block block) {
                menu.addMenuClickHandler(CONVERT, (player, slot, clicked, action) -> {
                    convert(menu, player);
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
                    menu.dropItems(menu.getLocation(), new int[] {INPUT, OUTPUT});
                }
            }
        });
    }

    private void convert(BlockMenu menu, Player player) {
        if (!isEmpty(menu.getItemInSlot(OUTPUT))) {
            player.sendMessage(ChatColor.RED + "Take the existing output first.");
            return;
        }

        ItemStack input = menu.getItemInSlot(INPUT);
        if (isEmpty(input)) {
            player.sendMessage(ChatColor.RED + "Insert an old DracFun item first.");
            return;
        }

        SlimefunItem source = SlimefunItem.getByItem(input);
        if (source == null) {
            player.sendMessage(ChatColor.RED + "That item does not contain a recognized DracFun identity.");
            return;
        }

        String targetId = TARGETS.get(source.getId());
        if (targetId == null) {
            player.sendMessage(ChatColor.RED + "That item was not supported by DracFun 2.0.10's converter.");
            return;
        }

        SlimefunItem target = SlimefunItem.getById(targetId);
        if (target == null || target instanceof LegacyPlaceholderItem) {
            player.sendMessage(ChatColor.RED + "The clean-room replacement for " + targetId + " is not implemented yet.");
            return;
        }

        ItemStack converted = target.getItem().clone();
        converted.setAmount(input.getAmount());
        menu.replaceExistingItem(INPUT, null);
        menu.replaceExistingItem(OUTPUT, converted);
        player.sendMessage(ChatColor.GREEN + "Converted legacy DracFun item to " + targetId + '.');
    }

    private static ItemStack button() {
        ItemStack item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "Convert Legacy Item");
        meta.setLore(java.util.List.of(
                ChatColor.YELLOW + "Use plain legacy items only.",
                ChatColor.GRAY + "Extra enchantments/metadata are intentionally removed."));
        item.setItemMeta(meta);
        return item;
    }

    private static boolean isEmpty(ItemStack item) {
        return item == null || item.getType().isAir();
    }
}
