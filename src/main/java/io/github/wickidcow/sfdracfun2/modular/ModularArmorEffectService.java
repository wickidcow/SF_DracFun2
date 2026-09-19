package io.github.wickidcow.sfdracfun2.modular;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.wickidcow.sfdracfun2.SFDracFun2;
import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

/**
 * Region-safe runtime for DracFun modular chestplate effects.
 *
 * <p>DracFun 2.0.10 drove passive armor effects from global/async tasks.
 * Reborn gives each online player an entity-owned task so all inventory and
 * player mutation remains on the owning Paper/Folia region thread.</p>
 */
public final class ModularArmorEffectService implements Listener {

    // DracFun 2.0.10 scheduled shield work every 100 ticks and passive
    // armor effects every 600 ticks. Keep one region-safe 100-tick task and
    // run the passive portion every sixth pass.
    private static final long INITIAL_DELAY_TICKS = 100L;
    private static final long PERIOD_TICKS = 100L;
    private static final int PASSIVE_EVERY_PASSES = 6;

    private final SFDracFun2 plugin;
    private final Set<UUID> scheduled = ConcurrentHashMap.newKeySet();
    private final Map<UUID, FlightGrant> flightGrants = new ConcurrentHashMap<>();
    private final Map<UUID, Integer> passivePasses = new ConcurrentHashMap<>();
    private final Map<UUID, Long> undyingCooldownUntil = new ConcurrentHashMap<>();
    private final Map<UUID, Long> invincibleUntil = new ConcurrentHashMap<>();

    public ModularArmorEffectService(SFDracFun2 plugin) {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            schedule(player);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        schedule(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true)
    public void onArmorChange(PlayerArmorChangeEvent event) {
        Player player = event.getPlayer();
        ItemStack newItem = event.getNewItem();

        if (newItem != null
                && !newItem.getType().isAir()
                && SlimefunItem.getByItem(newItem) instanceof ModularArmorItem) {
            updateFlight(player, newItem);
        } else if (event.getOldItem() != null
                && SlimefunItem.getByItem(event.getOldItem()) instanceof ModularArmorItem) {
            restoreFlight(player);
        }
    }

    private void schedule(Player player) {
        UUID uuid = player.getUniqueId();
        if (!scheduled.add(uuid)) {
            return;
        }

        player.getScheduler().runAtFixedRate(
                plugin,
                task -> tick(player),
                () -> {
                    scheduled.remove(uuid);
                    flightGrants.remove(uuid);
                    passivePasses.remove(uuid);
                    undyingCooldownUntil.remove(uuid);
                    invincibleUntil.remove(uuid);
                },
                INITIAL_DELAY_TICKS,
                PERIOD_TICKS);
    }

    private void tick(Player player) {
        if (!player.isValid() || player.isDead()) {
            return;
        }

        ItemStack armor = modularArmor(player);
        if (armor == null) {
            restoreFlight(player);
            return;
        }

        int pass = passivePasses.merge(player.getUniqueId(), 1, Integer::sum);
        if (pass >= PASSIVE_EVERY_PASSES) {
            passivePasses.put(player.getUniqueId(), 0);
            applyPassiveEffects(player, armor);
        }

        updateFlight(player, armor);
        regenerateShield(armor);

        SlimefunItem sfItem = SlimefunItem.getByItem(armor);
        if (sfItem instanceof ModularGearItem gear) {
            ModularLore.refresh(armor, gear);
            player.getInventory().setChestplate(armor);
        }
    }

    private static void applyPassiveEffects(Player player, ItemStack armor) {
        int jump = ModuleEffects.jump(armor);
        if (jump > 0) {
            addEffect(player, new String[] {"JUMP_BOOST", "JUMP"}, jump / 100);
        }

        int speed = ModuleEffects.speed(armor);
        if (speed > 0) {
            addEffect(player, new String[] {"SPEED"}, speed / 100);
        }

        if (ModuleEffects.vision(armor)) {
            addEffect(player, new String[] {"NIGHT_VISION"}, 0);
        }
    }

    private void updateFlight(Player player, ItemStack armor) {
        int flight = ModuleEffects.flight(armor);
        UUID uuid = player.getUniqueId();

        if (flight <= 0) {
            restoreFlight(player);
            return;
        }

        boolean newlyGranted = !flightGrants.containsKey(uuid);
        flightGrants.computeIfAbsent(
                uuid,
                ignored -> new FlightGrant(player.getAllowFlight(), player.getFlySpeed()));

        player.setAllowFlight(true);
        player.setFlySpeed(clampFlightSpeed(flight / 1000.0F));
        if (newlyGranted) {
            player.setFlying(true);
        }
    }

    private void restoreFlight(Player player) {
        FlightGrant grant = flightGrants.remove(player.getUniqueId());
        if (grant == null) {
            return;
        }

        player.setFlySpeed(grant.previousFlySpeed());
        player.setAllowFlight(grant.previousAllowFlight());
        if (!grant.previousAllowFlight()
                && player.getGameMode() != GameMode.CREATIVE
                && player.getGameMode() != GameMode.SPECTATOR) {
            player.setFlying(false);
        }
    }

    private static void regenerateShield(ItemStack armor) {
        ModuleTier controlTier =
                ModularData.highestTier(armor, ModuleFamily.SHIELD_CONTROL);
        int capacity = ModuleEffects.shieldCapacity(armor);
        int recovery = ModuleEffects.shieldRecovery(armor) * 5;

        if (controlTier == null || capacity <= 0 || recovery <= 0) {
            return;
        }

        int cooldown = ModularData.getShieldCooldown(armor);
        if (cooldown > 0) {
            ModularData.setShieldCooldown(armor, Math.max(0, cooldown - 5));
            return;
        }

        int shield = Math.min(capacity, ModularData.getShield(armor));
        if (shield >= capacity) {
            if (ModularData.getShield(armor) != shield) {
                ModularData.setShield(armor, shield);
            }
            return;
        }

        int required = Math.min(recovery, capacity - shield);
        if (ModularData.getCharge(armor) < required) {
            return;
        }

        ModularData.removeCharge(armor, required);
        ModularData.setShield(armor, shield + required);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onArrowDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || !(event.getDamager() instanceof AbstractArrow)) {
            return;
        }

        ItemStack armor = modularArmor(player);
        if (armor != null && ModuleEffects.arrowImmunity(armor)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)
                || event.getFinalDamage() <= 0D) {
            return;
        }

        long now = System.currentTimeMillis();
        Long invincible = invincibleUntil.get(player.getUniqueId());
        if (invincible != null) {
            if (invincible > now) {
                event.setCancelled(true);
                event.setDamage(0D);
                return;
            }
            invincibleUntil.remove(player.getUniqueId());
        }

        ItemStack armor = modularArmor(player);
        if (armor == null) {
            return;
        }

        if (absorbWithShield(event, armor)) {
            refreshChestplate(player, armor);
            return;
        }

        applyUndying(event, player, armor, now);
    }

    private static boolean absorbWithShield(
            EntityDamageEvent event,
            ItemStack armor) {
        ModuleTier controlTier =
                ModularData.highestTier(armor, ModuleFamily.SHIELD_CONTROL);
        int capacity = ModuleEffects.shieldCapacity(armor);
        int recovery = ModuleEffects.shieldRecovery(armor);

        if (controlTier == null || capacity <= 0 || recovery <= 0) {
            return false;
        }

        int shield = ModularData.getShield(armor);
        double finalDamage = event.getFinalDamage();
        if (shield < finalDamage) {
            return false;
        }

        int cooldown = switch (controlTier) {
            case WYVERN -> 15;
            case DRACONIC -> 10;
            case CHAOTIC -> 5;
            case BASIC -> 0;
        };

        ModularData.setShieldCooldown(armor, cooldown);
        ModularData.setShield(
                armor,
                Math.max(0, shield - (int) finalDamage));
        event.setDamage(0D);
        return true;
    }

    private void applyUndying(
            EntityDamageEvent event,
            Player player,
            ItemStack armor,
            long now) {
        if (event.getFinalDamage() < player.getHealth()) {
            return;
        }

        ModuleTier tier = ModularData.highestTier(armor, ModuleFamily.UNDYING);
        if (tier == null || tier == ModuleTier.BASIC) {
            return;
        }

        UUID uuid = player.getUniqueId();
        long cooldownUntil =
                undyingCooldownUntil.getOrDefault(uuid, 0L);
        if (cooldownUntil > now) {
            return;
        }

        UndyingSpec spec = switch (tier) {
            case WYVERN -> new UndyingSpec(12, 6, 120, 4);
            case DRACONIC -> new UndyingSpec(15, 12, 60, 6);
            case CHAOTIC -> new UndyingSpec(24, 20, 45, 8);
            case BASIC -> null;
        };
        if (spec == null || ModularData.getCharge(armor) < spec.chargeCost()) {
            return;
        }

        ModularData.removeCharge(armor, spec.chargeCost());
        undyingCooldownUntil.put(
                uuid,
                now + spec.cooldownSeconds() * 1000L);
        invincibleUntil.put(
                uuid,
                now + spec.invincibleSeconds() * 1000L);

        event.setCancelled(true);
        event.setDamage(0D);
        var maxHealth =
                player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
        player.setHealth(Math.min(
                maxHealth == null ? spec.reviveHealth() : maxHealth.getValue(),
                spec.reviveHealth()));
        player.playSound(
                player.getLocation(),
                Sound.ITEM_TOTEM_USE,
                1F,
                1F);
        refreshChestplate(player, armor);
    }

    private static ItemStack modularArmor(Player player) {
        ItemStack chestplate = player.getInventory().getChestplate();
        if (chestplate == null || chestplate.getType().isAir()) {
            return null;
        }
        return SlimefunItem.getByItem(chestplate) instanceof ModularArmorItem
                ? chestplate
                : null;
    }

    private static void refreshChestplate(Player player, ItemStack armor) {
        SlimefunItem item = SlimefunItem.getByItem(armor);
        if (item instanceof ModularGearItem gear) {
            ModularLore.refresh(armor, gear);
        }
        player.getInventory().setChestplate(armor);
    }

    @SuppressWarnings("deprecation")
    private static void addEffect(
            Player player,
            String[] names,
            int amplifier) {
        PotionEffectType type = null;
        for (String name : names) {
            type = PotionEffectType.getByName(name);
            if (type != null) {
                break;
            }
        }

        if (type == null) {
            return;
        }

        player.removePotionEffect(type);
        player.addPotionEffect(new PotionEffect(
                type,
                600,
                Math.max(0, amplifier),
                true,
                false,
                false));
    }

    private static float clampFlightSpeed(float speed) {
        return Math.max(-1F, Math.min(1F, speed));
    }

    private record FlightGrant(
            boolean previousAllowFlight,
            float previousFlySpeed) {}

    private record UndyingSpec(
            int chargeCost,
            int reviveHealth,
            int cooldownSeconds,
            int invincibleSeconds) {}
}
