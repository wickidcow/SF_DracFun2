package io.github.wickidcow.sfdracfun2.modular;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.wickidcow.sfdracfun2.SFDracFun2;
import java.util.EnumMap;
import java.util.Map;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Restores DracFun 2.0.10's modular armor AUTO_FEED behavior.
 *
 * <p>The legacy addon stores buffered hunger points directly on the armor under
 * {@code dracfun:dracfun_auto_feed}. Sneak-right-clicking the armor with food
 * in the offhand loads that buffer. Hunger loss at 12 or below and starvation
 * damage draw from the stored buffer automatically.</p>
 */
public final class ModularAutoFeedService implements Listener {

    private static final Map<Material, Integer> FOOD_POINTS = createFoodMap();

    private final SFDracFun2 plugin;

    public ModularAutoFeedService(SFDracFun2 plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
    public void onArmorUse(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack armor = event.getItem();
        SlimefunItem slimefunItem = SlimefunItem.getByItem(armor);
        if (!(slimefunItem instanceof ModularArmorItem gear)) {
            return;
        }

        // DracFun 2.0.10 cancelled the armor's ordinary right-click behavior.
        event.setCancelled(true);
        if (action == Action.RIGHT_CLICK_BLOCK
                || event.getHand() != EquipmentSlot.HAND
                || !event.getPlayer().isSneaking()) {
            return;
        }

        int capacity = ModuleEffects.autoFeed(armor);
        if (capacity <= 0) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack food = player.getInventory().getItemInOffHand();
        Integer foodPoints = FOOD_POINTS.get(food.getType());
        if (foodPoints == null || foodPoints <= 0 || food.getAmount() <= 0) {
            return;
        }

        int stored = Math.min(capacity, ModularData.getAutoFeedFood(armor));
        int space = capacity - stored;
        if (space <= 0) {
            return;
        }

        int consume = Math.min(
                food.getAmount(),
                (space + foodPoints - 1) / foodPoints);
        if (consume <= 0) {
            return;
        }

        int added = Math.min(space, consume * foodPoints);
        ModularData.setAutoFeedFood(armor, stored + added);
        ModularLore.refresh(armor, gear);
        player.getInventory().setItemInMainHand(armor);

        ItemStack remaining = food.clone();
        remaining.setAmount(food.getAmount() - consume);
        if (remaining.getAmount() <= 0) {
            player.getInventory().setItemInOffHand(new ItemStack(Material.AIR));
        } else {
            player.getInventory().setItemInOffHand(remaining);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onHungerLoss(FoodLevelChangeEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || event.getFoodLevel() > 12) {
            return;
        }

        int foodLevel = feed(player);
        if (foodLevel >= 0) {
            event.setFoodLevel(foodLevel);
            burp(player);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onStarvation(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || event.getCause() != EntityDamageEvent.DamageCause.STARVATION) {
            return;
        }

        int foodLevel = feed(player);
        if (foodLevel >= 0) {
            // The old listener replenished food but did not cancel the current
            // starvation hit, so preserve that observable behavior.
            player.setFoodLevel(foodLevel);
            burp(player);
        }
    }

    private static int feed(Player player) {
        ItemStack armor = player.getInventory().getChestplate();
        if (!(SlimefunItem.getByItem(armor) instanceof ModularArmorItem gear)) {
            return -1;
        }

        int capacity = ModuleEffects.autoFeed(armor);
        if (capacity <= 0) {
            return -1;
        }

        int stored = Math.min(capacity, ModularData.getAutoFeedFood(armor));
        if (stored <= 0) {
            return -1;
        }

        int current = player.getFoodLevel();
        int missing = Math.max(0, 20 - current);
        int consumed = Math.min(missing, stored);
        ModularData.setAutoFeedFood(armor, stored - consumed);
        ModularLore.refresh(armor, gear);
        player.getInventory().setChestplate(armor);
        return Math.min(20, current + consumed);
    }

    private static void burp(Player player) {
        player.playSound(
                player.getLocation(),
                Sound.ENTITY_PLAYER_BURP,
                1.0F,
                1.0F);
    }

    private static Map<Material, Integer> createFoodMap() {
        EnumMap<Material, Integer> foods = new EnumMap<>(Material.class);
        foods.put(Material.APPLE, 4);
        foods.put(Material.BAKED_POTATO, 5);
        foods.put(Material.BEETROOT, 1);
        foods.put(Material.BEETROOT_SOUP, 6);
        foods.put(Material.BREAD, 5);
        foods.put(Material.CARROT, 3);
        foods.put(Material.CHORUS_FRUIT, 4);
        foods.put(Material.COOKED_CHICKEN, 6);
        foods.put(Material.COOKED_COD, 5);
        foods.put(Material.COOKED_MUTTON, 6);
        foods.put(Material.COOKED_PORKCHOP, 8);
        foods.put(Material.COOKED_RABBIT, 5);
        foods.put(Material.COOKED_SALMON, 6);
        foods.put(Material.COOKIE, 2);
        foods.put(Material.DRIED_KELP, 1);
        foods.put(Material.GLOW_BERRIES, 2);
        foods.put(Material.GOLDEN_CARROT, 6);
        foods.put(Material.HONEY_BOTTLE, 6);
        foods.put(Material.MELON_SLICE, 2);
        foods.put(Material.POTATO, 1);
        foods.put(Material.PUMPKIN_PIE, 8);
        foods.put(Material.RABBIT_STEW, 10);
        foods.put(Material.BEEF, 3);
        foods.put(Material.CHICKEN, 2);
        foods.put(Material.COD, 2);
        foods.put(Material.MUTTON, 2);
        foods.put(Material.PORKCHOP, 3);
        foods.put(Material.RABBIT, 3);
        foods.put(Material.SALMON, 2);
        foods.put(Material.COOKED_BEEF, 8);
        foods.put(Material.SWEET_BERRIES, 2);
        foods.put(Material.TROPICAL_FISH, 1);
        return Map.copyOf(foods);
    }
}
