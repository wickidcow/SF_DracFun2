package io.github.wickidcow.sfdracfun2.modular;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.wickidcow.sfdracfun2.SFDracFun2;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

/**
 * Runtime for powered modular bow effects.
 *
 * <p>DracFun 2.0.10 applied speed/damage/gravity modifiers when a powered
 * modular bow was fired while the shooter wore modular armor, then consumed
 * one charge. Reborn preserves that gate and charge cost. The original
 * Arrow Penetration code stored 25/50/75 percent using integer division by
 * 100, producing zero; Reborn maps that intended percentage to an actual
 * Bukkit pierce level instead of preserving the dead value.</p>
 */
public final class ModularBowEffectService implements Listener {

    private static final NamespacedKey MODULAR_ARROW =
            LegacyDracFunKeys.key("REBORN_MODULAR_ARROW");
    private static final NamespacedKey PENETRATION =
            LegacyDracFunKeys.key("DRACFUN_ARROW_PENETRATION");

    public ModularBowEffectService(SFDracFun2 plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || !(event.getProjectile() instanceof AbstractArrow arrow)) {
            return;
        }

        ItemStack bow = event.getBow();
        SlimefunItem sfItem = SlimefunItem.getByItem(bow);
        if (!(sfItem instanceof ModularGearItem gear)
                || gear.getGearType() != GearType.BOW
                || !ModularGearState.canUsePoweredEffect(player, bow)) {
            return;
        }

        arrow.setGlowing(true);
        arrow.setCritical(true);

        if (ModuleEffects.arrowGravity(bow)) {
            arrow.setGravity(false);
        }

        int speedPercent = ModuleEffects.arrowSpeed(bow);
        if (speedPercent > 0) {
            arrow.setVelocity(
                    arrow.getVelocity().multiply(1D + speedPercent / 100D));
        }

        int damagePercent = ModuleEffects.arrowDamage(bow);
        if (damagePercent > 0) {
            arrow.setDamage(
                    arrow.getDamage() * (1D + damagePercent / 100D));
        }

        int penetrationPercent = ModuleEffects.arrowPenetration(bow);
        if (penetrationPercent > 0) {
            int pierce = Math.max(
                    1,
                    (int) Math.ceil(penetrationPercent / 25D));
            arrow.setPierceLevel((byte) Math.min(127, pierce));
            arrow.getPersistentDataContainer().set(
                    PENETRATION,
                    PersistentDataType.INTEGER,
                    penetrationPercent);
        }

        arrow.getPersistentDataContainer().set(
                MODULAR_ARROW,
                PersistentDataType.BYTE,
                (byte) 1);

        ModularData.removeCharge(bow, 1);
        ModularLore.refresh(bow, gear);
        restoreBow(player, event.getHand(), bow);
    }

    /**
     * DracFun's ModularBow handler added the projectile's configured damage a
     * second time to the Bukkit damage event. Preserve that observable combat
     * contract for arrows fired by Reborn modular bows.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onArrowDamage(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof AbstractArrow arrow)) {
            return;
        }

        Byte modular = arrow.getPersistentDataContainer().get(
                MODULAR_ARROW,
                PersistentDataType.BYTE);
        if (modular == null || modular == 0) {
            return;
        }

        event.setDamage(event.getDamage() + arrow.getDamage());
    }

    private static void restoreBow(
            Player player,
            EquipmentSlot hand,
            ItemStack bow) {
        if (hand == EquipmentSlot.OFF_HAND) {
            player.getInventory().setItemInOffHand(bow);
        } else {
            player.getInventory().setItemInMainHand(bow);
        }
    }
}
