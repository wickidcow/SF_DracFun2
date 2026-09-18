package io.github.wickidcow.sfdracfun2.modular;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Renders DracFun Reborn modular state without relying on legacy lore parsing. */
public final class ModularLore {

    private ModularLore() {}

    public static void refresh(ItemStack stack, ModularGearItem gear) {
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return;
        }

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Energy: " + ChatColor.AQUA + ModularData.getCharge(stack)
                + ChatColor.GRAY + " / " + ChatColor.AQUA + ModularData.getCapacity(stack));
        lore.add(ChatColor.GRAY + "Module Points: " + ChatColor.WHITE + ModularData.getUsedModulePoints(stack)
                + ChatColor.GRAY + " / " + ChatColor.WHITE + gear.getMaxModulePoints());

        addValue(lore, "AOE", ModuleEffects.aoe(stack));
        addValue(lore, "Arrow Damage", ModuleEffects.arrowDamage(stack));
        addFlag(lore, "Arrow Gravity", ModuleEffects.arrowGravity(stack));
        addFlag(lore, "Arrow Immunity", ModuleEffects.arrowImmunity(stack));
        addValue(lore, "Arrow Penetration", ModuleEffects.arrowPenetration(stack));
        addValue(lore, "Arrow Speed", ModuleEffects.arrowSpeed(stack));
        addValue(lore, "Damage", ModuleEffects.damage(stack));
        addValue(lore, "Flight", ModuleEffects.flight(stack));
        int autoFeedCapacity = ModuleEffects.autoFeed(stack);
        if (autoFeedCapacity > 0) {
            lore.add(ChatColor.DARK_GRAY + "Auto Feed: " + ChatColor.GREEN
                    + ModularData.getAutoFeedFood(stack)
                    + ChatColor.DARK_GRAY + " / " + ChatColor.GREEN + autoFeedCapacity);
        }
        addValue(lore, "Harvest", ModuleEffects.harvest(stack));
        addValue(lore, "Jump", ModuleEffects.jump(stack));
        addValue(lore, "Shield Capacity", ModuleEffects.shieldCapacity(stack));
        addValue(lore, "Shield Cooldown", ModuleEffects.shieldCooldown(stack));
        addValue(lore, "Shield Recovery", ModuleEffects.shieldRecovery(stack));
        addValue(lore, "Speed", ModuleEffects.speed(stack));
        addFlag(lore, "Undying", ModuleEffects.undying(stack));
        addFlag(lore, "Vision", ModuleEffects.vision(stack));

        meta.setLore(lore);
        stack.setItemMeta(meta);
    }

    private static void addValue(List<String> lore, String label, int value) {
        if (value != 0) {
            lore.add(ChatColor.DARK_GRAY + label + ": " + ChatColor.GREEN + value);
        }
    }

    private static void addFlag(List<String> lore, String label, boolean enabled) {
        if (enabled) {
            lore.add(ChatColor.DARK_GRAY + label + ": " + ChatColor.GREEN + "Enabled");
        }
    }
}
