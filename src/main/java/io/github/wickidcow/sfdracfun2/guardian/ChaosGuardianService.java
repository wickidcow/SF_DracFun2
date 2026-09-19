package io.github.wickidcow.sfdracfun2.guardian;

import com.destroystokyo.paper.event.entity.EnderDragonFireballHitEvent;
import com.destroystokyo.paper.event.entity.EnderDragonShootFireballEvent;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.wickidcow.sfdracfun2.SFDracFun2;
import io.github.wickidcow.sfdracfun2.modular.GearType;
import io.github.wickidcow.sfdracfun2.modular.LegacyDracFunKeys;
import io.github.wickidcow.sfdracfun2.modular.ModularArmorItem;
import io.github.wickidcow.sfdracfun2.modular.ModularGearItem;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.ChatColor;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.DragonBattle;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.EnderCrystal;
import org.bukkit.entity.EnderDragon;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EnderDragonChangePhaseEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

/**
 * Clean-room Chaos Guardian battle controller.
 *
 * <p>This preserves the observable 2.0.10 boss contract while replacing its
 * static global fight state and Paper-only synthetic dragon-fireball events.
 * All delayed mutations are routed through Slimefun's location/entity scheduler
 * facade so the same code can run on Paper, Purpur and Folia.</p>
 */
public final class ChaosGuardianService implements Listener {

    public static final NamespacedKey GUARDIAN =
            LegacyDracFunKeys.key("DRACFUN_CHAOS_GUARDIAN");
    public static final NamespacedKey CRYSTAL =
            LegacyDracFunKeys.key("DRACFUN_CHAOS_CRYSTAL");

    private static final NamespacedKey INVULNERABLE_UNTIL =
            LegacyDracFunKeys.key("REBORN_CHAOS_INVULNERABLE_UNTIL");
    private static final NamespacedKey MINION =
            LegacyDracFunKeys.key("REBORN_CHAOS_MINION");
    private static final NamespacedKey CRYSTAL_COUNT =
            LegacyDracFunKeys.key("REBORN_CHAOS_CRYSTAL_COUNT");
    private static final NamespacedKey CAGE_MASK =
            LegacyDracFunKeys.key("REBORN_CHAOS_CAGE_MASK");

    private static final long ATTACK_PERIOD_TICKS = 600L;
    private static final double PARTICIPANT_RANGE_SQUARED = 256D * 256D;

    private final SFDracFun2 plugin;
    private final boolean crystalCages;
    private final boolean cleanupCrystalCages;
    private final boolean punishUnarmored;
    private final int witherLifetimeTicks;
    private final double laserDamageCap;
    private final Set<UUID> pendingWorlds = ConcurrentHashMap.newKeySet();
    private final Set<UUID> guardiansWithNaturalFireball = ConcurrentHashMap.newKeySet();
    private final Map<UUID, List<UUID>> participantSnapshots = new ConcurrentHashMap<>();
    private final Map<UUID, Map<CageBlock, Material>> cageBlocks = new ConcurrentHashMap<>();

    public ChaosGuardianService(SFDracFun2 plugin) {
        this.plugin = plugin;
        crystalCages = plugin.getConfig().getBoolean("guardian.crystal-cages-enabled", true);
        cleanupCrystalCages = plugin.getConfig().getBoolean("guardian.cleanup-crystal-cages", false);
        punishUnarmored = plugin.getConfig().getBoolean("guardian.punish-unarmored", true);
        witherLifetimeTicks = Math.max(
                0,
                plugin.getConfig().getInt("guardian.wither-minion-lifetime-ticks", 0));
        laserDamageCap = Math.max(
                1D,
                plugin.getConfig().getDouble("guardian.laser-damage-cap", 5000D));
    }

    public void start() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        for (World world : plugin.getServer().getWorlds()) {
            if (world.getEnvironment() != World.Environment.THE_END) {
                continue;
            }

            Slimefun.runSyncAt(world.getSpawnLocation(), () -> {
                if (!plugin.isEnabled()) {
                    return;
                }

                DragonBattle battle = world.getEnderDragonBattle();
                EnderDragon dragon = battle == null ? null : battle.getEnderDragon();
                if (dragon != null && isGuardian(dragon)) {
                    Slimefun.runSyncFor(dragon, () -> {
                        initializeBossBar(dragon.getBossBar());
                        snapshotParticipants(
                        dragon,
                        new Location(dragon.getWorld(), 0D, 0D, 0D));
                        dragon.getPersistentDataContainer().set(
                                CRYSTAL_COUNT,
                                PersistentDataType.INTEGER,
                                battle.getHealingCrystals().size());
                        for (EnderCrystal crystal : battle.getHealingCrystals()) {
                            Slimefun.runSyncFor(crystal, () -> {
                                if (hasTag(crystal, CRYSTAL)) {
                                    restoreCageTracking(crystal);
                                }
                            });
                        }
                        schedulePulse(dragon);
                    });
                }
            });
        }
    }

    public boolean invoke(Player player, ItemStack invocationItem) {
        World world = player.getWorld();
        if (world.getEnvironment() != World.Environment.THE_END) {
            player.sendMessage(ChatColor.RED
                    + "In this realm, the invocation of the Chaos Guardian is strictly forbidden.");
            return false;
        }

        DragonBattle battle = world.getEnderDragonBattle();
        if (battle == null || battle.getEnderDragon() != null) {
            // DracFun 2.0.10 silently ignored invocation while no battle existed
            // or while an Ender Dragon was already active.
            return false;
        }

        // Retain Reborn's duplicate-invocation guard, but keep the legacy path silent.
        if (pendingWorlds.contains(world.getUID())) {
            return false;
        }

        if (invocationItem == null || invocationItem.getAmount() <= 0) {
            return false;
        }

        // Exact 2.0.10 ordering: consume the orb before checking modular armor.
        invocationItem.setAmount(invocationItem.getAmount() - 1);

        if (!isWearingModularArmor(player)) {
            player.sendMessage(ChatColor.RED
                    + "Did you, a mere mortal, truly believe that you could contend with the Guardian in such armor?");
            player.setHealth(0D);
            return false;
        }

        player.playSound(
                player.getLocation(),
                "dracfun:dracfun.chaos_chamber_ambient",
                SoundCategory.AMBIENT,
                3F,
                3F);

        pendingWorlds.add(world.getUID());
        Location owner = world.getSpawnLocation();

        Slimefun.runSyncAt(owner, () -> {
            if (!plugin.isEnabled()) {
                pendingWorlds.remove(world.getUID());
                return;
            }
            initiateRespawn(battle);
            player.sendMessage(ChatColor.GREEN
                    + "May luck guide you to triumph in the midst of chaos!");
        }, 600L);

        Slimefun.runSyncAt(owner, () -> initializeWhenReady(world, battle, 0), 1260L);
        return true;
    }

    private void initializeWhenReady(World world, DragonBattle battle, int attempt) {
        if (!plugin.isEnabled()) {
            pendingWorlds.remove(world.getUID());
            return;
        }

        EnderDragon dragon = battle.getEnderDragon();
        if (dragon == null) {
            if (attempt >= 60) {
                pendingWorlds.remove(world.getUID());
                plugin.getLogger().warning(
                        "Chaos Guardian invocation timed out waiting for the Ender Dragon respawn.");
                return;
            }

            Slimefun.runSyncAt(
                    world.getSpawnLocation(),
                    () -> initializeWhenReady(world, battle, attempt + 1),
                    100L);
            return;
        }

        Slimefun.runSyncFor(dragon, () -> initializeFight(dragon, battle));
        pendingWorlds.remove(world.getUID());
    }

    private void initializeFight(EnderDragon dragon, DragonBattle battle) {
        if (!plugin.isEnabled() || !dragon.isValid()) {
            return;
        }

        applyMultiplier(dragon, new String[] {"GENERIC_MAX_HEALTH", "MAX_HEALTH"}, 10D);
        applyMultiplier(dragon, new String[] {"GENERIC_FOLLOW_RANGE", "FOLLOW_RANGE"}, 4D);
        applyMultiplier(dragon, new String[] {"GENERIC_ARMOR_TOUGHNESS", "ARMOR_TOUGHNESS"}, 4D);
        applyMultiplier(dragon, new String[] {"GENERIC_ARMOR", "ARMOR"}, 4D);
        applyMultiplier(dragon, new String[] {"GENERIC_ATTACK_DAMAGE", "ATTACK_DAMAGE"}, 2D);
        applyMultiplier(dragon, new String[] {"GENERIC_ATTACK_SPEED", "ATTACK_SPEED"}, 2D);
        applyMultiplier(dragon, new String[] {"GENERIC_ATTACK_KNOCKBACK", "ATTACK_KNOCKBACK"}, 2D);
        applyMultiplier(dragon, new String[] {"GENERIC_FLYING_SPEED", "FLYING_SPEED"}, 1.5D);
        applyMultiplier(dragon, new String[] {"GENERIC_MOVEMENT_SPEED", "MOVEMENT_SPEED"}, 1.5D);

        AttributeInstance maxHealth = findAttribute(
                dragon,
                "GENERIC_MAX_HEALTH",
                "MAX_HEALTH");
        double targetHealth = maxHealth == null ? dragon.getHealth() : Math.min(2000D, maxHealth.getValue());
        dragon.setHealth(Math.max(1D, targetHealth));
        dragon.setCustomName(ChatColor.DARK_RED + "Chaos Guardian");
        tag(dragon, GUARDIAN);

        initializeBossBar(dragon.getBossBar());

        var crystals = battle.getHealingCrystals();
        dragon.getPersistentDataContainer().set(
                CRYSTAL_COUNT,
                PersistentDataType.INTEGER,
                crystals.size());

        snapshotParticipants(dragon, dragon.getLocation());

        for (EnderCrystal crystal : crystals) {
            Slimefun.runSyncFor(crystal, () -> {
                tag(crystal, CRYSTAL);
                if (crystalCages) {
                    buildCage(crystal);
                }
            });
        }

        schedulePulse(dragon);
    }

    private void schedulePulse(EnderDragon dragon) {
        Slimefun.runSyncFor(
                dragon,
                () -> pulse(dragon),
                () -> {},
                ATTACK_PERIOD_TICKS);
    }

    private void pulse(EnderDragon dragon) {
        if (!plugin.isEnabled() || !dragon.isValid() || dragon.isDead() || !isGuardian(dragon)) {
            return;
        }

        World world = dragon.getWorld();
        Location origin = new Location(world, 0D, 0D, 0D);
        Collection<Player> participants = world.getNearbyPlayers(origin, 256D);
        participantSnapshots.put(
                dragon.getUniqueId(),
                participants.stream().map(Player::getUniqueId).toList());

        // GuardianBattle.run() only replayed the attack event after the Guardian
        // had naturally produced at least one DragonFireball.
        if (guardiansWithNaturalFireball.contains(dragon.getUniqueId())) {
            attackSnapshot(dragon);
        }

        schedulePulse(dragon);
    }

    private void attackSnapshot(EnderDragon dragon) {
        List<UUID> snapshot = participantSnapshots.get(dragon.getUniqueId());
        if (snapshot == null || snapshot.isEmpty()) {
            return;
        }

        Set<UUID> ids = Set.copyOf(snapshot);
        double health = dragon.getHealth();
        World world = dragon.getWorld();
        for (Player player : world.getPlayers()) {
            if (!ids.contains(player.getUniqueId())) {
                continue;
            }

            Slimefun.runSyncFor(player, () -> {
                if (!plugin.isEnabled()
                        || !player.isOnline()
                        || player.isDead()
                        || player.getWorld() != world) {
                    return;
                }

                if (health > 1800D) {
                    basicAttack(dragon, player);
                } else {
                    advancedAttack(dragon, player);
                }
            });
        }
    }

    private void basicAttack(EnderDragon dragon, Player player) {
        int shots = ThreadLocalRandom.current().nextInt(6, 12) + 6;
        for (int i = 0; i < shots; i++) {
            long delay = (long) i * 4L;
            Slimefun.runSyncFor(player, () -> {
                if (!validCombatant(player, dragon)) {
                    return;
                }

                Location target = player.getLocation().clone();
                Slimefun.runSyncFor(dragon, () -> {
                    if (!dragon.isValid() || dragon.isDead()) {
                        return;
                    }

                    Vector direction = target.toVector()
                            .subtract(dragon.getLocation().toVector())
                            .normalize();
                    dragon.launchProjectile(DragonFireball.class, direction);
                });
            }, delay);
        }
    }

    private void advancedAttack(EnderDragon dragon, Player player) {
        int roll = ThreadLocalRandom.current().nextInt(1, 10);
        if (roll != 5) {
            laserAttack(dragon, player);
            return;
        }

        if (ThreadLocalRandom.current().nextBoolean()) {
            gravityAttack(player);
        } else {
            witherAttack(dragon, player);
        }
    }

    private void laserAttack(EnderDragon dragon, Player player) {
        Slimefun.runSyncFor(dragon, () -> {
            if (!dragon.isValid() || dragon.isDead()) {
                return;
            }

            dragon.getPersistentDataContainer().set(
                    INVULNERABLE_UNTIL,
                    PersistentDataType.LONG,
                    System.currentTimeMillis() + 14_000L);

            BossBar bossBar = dragon.getBossBar();
            if (bossBar != null) {
                bossBar.setColor(BarColor.BLUE);
            }

            Location hover = dragon.getWorld().getSpawnLocation().clone().add(0D, 32D, 0D);
            dragon.teleportAsync(hover);
            dragon.setPhase(EnderDragon.Phase.HOVER);
        });

        for (int i = 1; i <= 20; i++) {
            final int pulse = i;
            Slimefun.runSyncFor(player, () -> {
                if (!validCombatant(player, dragon)) {
                    return;
                }

                Location dragonLocation = dragon.getLocation();
                Vector direction = player.getLocation()
                        .toVector()
                        .subtract(dragonLocation.toVector())
                        .normalize();
                var trace = player.getWorld().rayTraceBlocks(
                        dragonLocation,
                        direction,
                        256D,
                        FluidCollisionMode.NEVER,
                        true);
                if (trace == null
                        || (trace.getHitBlock() != null
                                && trace.getHitBlock().getType() == Material.OBSIDIAN)) {
                    return;
                }

                spawnLegacyLaser(player.getWorld(), dragonLocation, player.getLocation());

                double damage = Math.min(laserDamageCap, pulse * 250D);
                player.damage(damage);
                player.getLocation().createExplosion(5.0F, false, false);
                spawnLegacyHugeExplosion(player, 3);
                player.playSound(
                        player.getLocation(),
                        "dracfun:dracfun.beam",
                        SoundCategory.BLOCKS,
                        3F,
                        3F);
            }, 60L + (long) i * 10L);
        }

        Slimefun.runSyncFor(dragon, () -> {
            if (!dragon.isValid() || dragon.isDead()) {
                return;
            }

            dragon.setPhase(EnderDragon.Phase.CIRCLING);
            BossBar bossBar = dragon.getBossBar();
            if (bossBar != null) {
                bossBar.setColor(BarColor.RED);
            }
        }, 300L);
    }

    private void gravityAttack(Player player) {
        player.clearActivePotionEffects();
        applyEffect(player, "BLINDNESS", 100, 2);
        applyEffect(player, "CONFUSION", 100, 2);
        applyEffect(player, "NAUSEA", 100, 2);
        applyEffect(player, "DARKNESS", 100, 2);
        applyEffect(player, "HARM", 100, 2);
        applyEffect(player, "INSTANT_DAMAGE", 100, 2);
        applyEffect(player, "HUNGER", 100, 2);
        applyEffect(player, "LEVITATION", 100, 2);
        applyEffect(player, "POISON", 100, 2);
        applyEffect(player, "SLOW", 100, 2);
        applyEffect(player, "SLOWNESS", 100, 2);
        applyEffect(player, "SLOW_DIGGING", 100, 2);
        applyEffect(player, "MINING_FATIGUE", 100, 2);
        applyEffect(player, "WEAKNESS", 100, 2);
        applyEffect(player, "WITHER", 100, 2);
        player.setAllowFlight(false);
        player.setFlying(false);
        player.teleportAsync(player.getWorld().getSpawnLocation());
    }

    private void witherAttack(EnderDragon dragon, Player player) {
        Set<UUID> spawned = new HashSet<>();
        for (int i = 0; i <= ThreadLocalRandom.current().nextInt(3, 6); i++) {
            Location spawn = player.getLocation().clone().add(
                    ThreadLocalRandom.current().nextInt(-4, 4),
                    ThreadLocalRandom.current().nextInt(0, 4),
                    ThreadLocalRandom.current().nextInt(-4, 4));

            Wither wither = player.getWorld().spawn(spawn, Wither.class);
            tag(wither, MINION);
            spawned.add(wither.getUniqueId());
        }

        // DracFun 2.0.10 then scanned every Wither within 64 blocks of the
        // target player and reconfigured any it encountered, including Withers
        // not spawned by DracFun. Preserve that legacy encounter quirk.
        for (Wither wither : player.getLocation().getNearbyEntitiesByType(Wither.class, 64D)) {
            Slimefun.runSyncFor(wither, () -> {
                if (!wither.isValid() || wither.isDead()) {
                    return;
                }

                applyMultiplier(wither, new String[] {"GENERIC_FOLLOW_RANGE", "FOLLOW_RANGE"}, 200D);
                applyMultiplier(wither, new String[] {"GENERIC_MOVEMENT_SPEED", "MOVEMENT_SPEED"}, 2D);
                applyMultiplier(wither, new String[] {"GENERIC_FLYING_SPEED", "FLYING_SPEED"}, 2D);
                applyMultiplier(wither, new String[] {"GENERIC_ARMOR", "ARMOR"}, 8D);
                wither.setCustomName(ChatColor.DARK_RED + "Guardian Wither");
                initializeBossBar(wither.getBossBar());
                wither.lookAt(player);
                wither.setTarget(player);

                if (spawned.contains(wither.getUniqueId()) && witherLifetimeTicks > 0) {
                    Slimefun.runSyncFor(
                            wither,
                            () -> {
                                if (wither.isValid()) {
                                    wither.remove();
                                }
                            },
                            () -> {},
                            witherLifetimeTicks);
                }
            });
        }

        // The legacy Wither phase followed with another 12-17-shot basic
        // Guardian volley after 100 ticks.
        Slimefun.runSyncFor(player, () -> {
            if (validCombatant(player, dragon)) {
                basicAttack(dragon, player);
            }
        }, 100L);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onCrystalDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof EnderCrystal crystal) || !hasTag(crystal, CRYSTAL)) {
            return;
        }

        if (!(event.getDamager() instanceof Player player) || !canBreakCrystal(player)) {
            event.setCancelled(true);
            return;
        }

        UUID crystalId = crystal.getUniqueId();
        World crystalWorld = crystal.getWorld();
        DragonBattle battle = crystalWorld.getEnderDragonBattle();
        EnderDragon guardian = battle == null ? null : battle.getEnderDragon();

        Slimefun.runSyncFor(
                crystal,
                () -> {
                    // A surviving crystal keeps its shield contribution.
                },
                () -> onChaosCrystalRetired(crystalId, crystalWorld, guardian),
                1L);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onGuardianDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof EnderDragon dragon) || !isGuardian(dragon)) {
            return;
        }

        if (isTemporarilyInvulnerable(dragon)) {
            event.setCancelled(true);
            event.setDamage(0D);
            return;
        }

        switch (event.getCause()) {
            case THORNS, WITHER, ENTITY_EXPLOSION, BLOCK_EXPLOSION -> {
                event.setCancelled(true);
                event.setDamage(0D);
                return;
            }
            default -> {
                // Continue below.
            }
        }

        if (crystalCount(dragon) > 0) {
            event.setCancelled(true);
            event.setDamage(0D);
            return;
        }

        double damage = event.getFinalDamage();
        event.setDamage(damage > 200D ? 20D : Math.max(0D, damage / 10D));
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPhaseChange(EnderDragonChangePhaseEvent event) {
        if (!isGuardian(event.getEntity())) {
            return;
        }

        EnderDragon.Phase phase = event.getNewPhase();
        if (phase == EnderDragon.Phase.BREATH_ATTACK || phase == EnderDragon.Phase.LAND_ON_PORTAL) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFireballShoot(EnderDragonShootFireballEvent event) {
        EnderDragon dragon = event.getEntity();
        if (!isGuardian(dragon)) {
            return;
        }

        guardiansWithNaturalFireball.add(dragon.getUniqueId());
        attackSnapshot(dragon);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onFireballHit(EnderDragonFireballHitEvent event) {
        DragonBattle battle = event.getEntity().getWorld().getEnderDragonBattle();
        EnderDragon guardian = battle == null ? null : battle.getEnderDragon();
        if (guardian == null || !isGuardian(guardian)) {
            return;
        }

        AreaEffectCloud cloud = event.getAreaEffectCloud();
        cloud.setDuration(cloud.getDuration() * 3);

        PotionEffectType harm = PotionEffectType.getByName("HARM");
        if (harm == null) {
            harm = PotionEffectType.getByName("INSTANT_DAMAGE");
        }
        if (harm != null) {
            cloud.addCustomEffect(new PotionEffect(harm, 600, 2), true);
        }

        event.getTargets().removeIf(target -> !(target instanceof Player));
        for (var target : event.getTargets()) {
            Player player = (Player) target;
            Slimefun.runSyncFor(player, () -> punishFireballHit(player));
        }
    }

    private void punishFireballHit(Player player) {
        if (player.isDead()) {
            return;
        }

        if (!isWearingModularArmor(player)) {
            player.setHealth(0D);
            return;
        }

        player.damage(500D);
        player.getLocation().createExplosion(5.0F, false, false);
        spawnLegacyHugeExplosion(player, 3);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onGuardianDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof EnderDragon dragon) || !isGuardian(dragon)) {
            return;
        }

        List<UUID> snapshot = participantSnapshots.remove(dragon.getUniqueId());
        guardiansWithNaturalFireball.remove(dragon.getUniqueId());
        pendingWorlds.remove(dragon.getWorld().getUID());

        // DracFun 2.0.10 returned immediately when its PLAYERS snapshot was empty,
        // before replacing the vanilla dragon rewards.
        if (snapshot == null || snapshot.isEmpty()) {
            if (cleanupCrystalCages) {
                cleanupTaggedCrystals(dragon.getWorld());
            }
            return;
        }

        event.setDroppedExp(20_000);
        event.getDrops().clear();

        ItemStack hearts = itemById("DRACFUN_DRAGON_HEART", 2);
        ItemStack shard = itemById("DRACFUN_CHAOS_SHARD", 1);
        if (hearts != null) {
            event.getDrops().add(hearts);
        }
        if (shard != null) {
            event.getDrops().add(shard);
        }

        if (cleanupCrystalCages) {
            cleanupTaggedCrystals(dragon.getWorld());
        }

        if (!punishUnarmored) {
            return;
        }

        Set<UUID> participantIds = Set.copyOf(snapshot);
        World world = dragon.getWorld();
        for (Player player : world.getPlayers()) {
            if (!participantIds.contains(player.getUniqueId())) {
                continue;
            }

            Slimefun.runSyncFor(player, () -> {
                if (!player.isOnline()
                        || player.isDead()
                        || player.getWorld() != world
                        || isWearingModularArmor(player)) {
                    return;
                }

                player.sendMessage(ChatColor.RED
                        + "As the Chaos Island crumbled, it enveloped you, burying you alive! "
                        + "A sturdier armor might have shielded you. Here's to better luck on your next endeavor!");
                player.setHealth(0D);
            });
        }
    }

    private void cleanupTaggedCrystals(World world) {
        DragonBattle battle = world.getEnderDragonBattle();
        if (battle == null) {
            return;
        }

        for (EnderCrystal crystal : battle.getHealingCrystals()) {
            Slimefun.runSyncFor(crystal, () -> {
                if (hasTag(crystal, CRYSTAL)) {
                    cleanupCage(crystal.getUniqueId(), crystal.getWorld());
                }
            });
        }
    }

    private void buildCage(EnderCrystal crystal) {
        if (!crystalCages) {
            return;
        }

        UUID crystalId = crystal.getUniqueId();
        Location base = crystal.getLocation().getBlock().getLocation();
        Map<CageBlock, Material> tracked = new ConcurrentHashMap<>();
        cageBlocks.put(crystalId, tracked);

        // DracFun 2.0.10 filled the entire 5x5x5 cage with iron bars,
        // then replaced the top y=3 layer with random obsidian/crying obsidian.
        // Track only blocks that were originally air for Reborn's safe cleanup;
        // overwritten blocks remain persistent like the legacy behavior.
        for (int x = -2; x <= 2; x++) {
            for (int y = -1; y <= 3; y++) {
                for (int z = -2; z <= 2; z++) {
                    boolean top = y == 3;
                    Location target = base.clone().add(x, y, z);
                    CageBlock key = new CageBlock(
                            target.getBlockX(),
                            target.getBlockY(),
                            target.getBlockZ());

                    Slimefun.runSyncAt(target, () -> {
                        if (!plugin.isEnabled()) {
                            return;
                        }

                        var block = target.getBlock();
                        boolean wasAir = block.getType().isAir();
                        Material placed = top
                                ? (ThreadLocalRandom.current().nextBoolean()
                                        ? Material.CRYING_OBSIDIAN
                                        : Material.OBSIDIAN)
                                : Material.IRON_BARS;
                        block.setType(placed);
                        if (wasAir) {
                            tracked.put(key, placed);
                        }
                    });
                }
            }
        }

        // Persist the exact blocks we created. A restart can then clean up the cage
        // without guessing which nearby obsidian/iron bars belonged to the player.
        Slimefun.runSyncFor(
                crystal,
                () -> persistCageTracking(crystal, base, tracked),
                () -> {},
                40L);
    }

    private void persistCageTracking(
            EnderCrystal crystal,
            Location base,
            Map<CageBlock, Material> tracked) {
        if (!crystal.isValid()) {
            return;
        }

        byte[] mask = new byte[125];
        for (Map.Entry<CageBlock, Material> entry : tracked.entrySet()) {
            CageBlock block = entry.getKey();
            int dx = block.x() - base.getBlockX();
            int dy = block.y() - base.getBlockY();
            int dz = block.z() - base.getBlockZ();
            int index = cageIndex(dx, dy, dz);
            if (index >= 0) {
                mask[index] = cageMaterialCode(entry.getValue());
            }
        }

        crystal.getPersistentDataContainer().set(
                CAGE_MASK,
                PersistentDataType.BYTE_ARRAY,
                mask);
    }

    private void restoreCageTracking(EnderCrystal crystal) {
        byte[] mask = crystal.getPersistentDataContainer().get(
                CAGE_MASK,
                PersistentDataType.BYTE_ARRAY);
        if (mask == null || mask.length != 125) {
            return;
        }

        Location base = crystal.getLocation().getBlock().getLocation();
        Map<CageBlock, Material> tracked = new ConcurrentHashMap<>();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy <= 3; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    int index = cageIndex(dx, dy, dz);
                    Material material = cageMaterial(mask[index]);
                    if (material != null) {
                        tracked.put(
                                new CageBlock(
                                        base.getBlockX() + dx,
                                        base.getBlockY() + dy,
                                        base.getBlockZ() + dz),
                                material);
                    }
                }
            }
        }

        if (!tracked.isEmpty()) {
            cageBlocks.put(crystal.getUniqueId(), tracked);
        }
    }

    private void cleanupCage(UUID crystalId, World world) {
        if (!crystalCages) {
            return;
        }

        Map<CageBlock, Material> tracked = cageBlocks.remove(crystalId);
        if (tracked == null || tracked.isEmpty()) {
            return;
        }

        for (Map.Entry<CageBlock, Material> entry : tracked.entrySet()) {
            CageBlock key = entry.getKey();
            Material expected = entry.getValue();
            Location target = new Location(world, key.x(), key.y(), key.z());

            Slimefun.runSyncAt(target, () -> {
                var block = target.getBlock();
                // If somebody changed a cage block during the fight, leave the new
                // block alone instead of deleting player work during cleanup.
                if (block.getType() == expected) {
                    block.setType(Material.AIR);
                }
            });
        }
    }

    private static int cageIndex(int dx, int dy, int dz) {
        if (dx < -2 || dx > 2 || dy < -1 || dy > 3 || dz < -2 || dz > 2) {
            return -1;
        }
        return ((dx + 2) * 25) + ((dy + 1) * 5) + (dz + 2);
    }

    private static byte cageMaterialCode(Material material) {
        if (material == Material.IRON_BARS) {
            return 1;
        }
        if (material == Material.OBSIDIAN) {
            return 2;
        }
        if (material == Material.CRYING_OBSIDIAN) {
            return 3;
        }
        return 0;
    }

    private static Material cageMaterial(byte code) {
        return switch (code) {
            case 1 -> Material.IRON_BARS;
            case 2 -> Material.OBSIDIAN;
            case 3 -> Material.CRYING_OBSIDIAN;
            default -> null;
        };
    }

    private record CageBlock(int x, int y, int z) {}

    private static void spawnLegacyLaser(World world, Location start, Location end) {
        double step = start.distance(end) / 64D;
        Vector direction = end.toVector().subtract(start.toVector()).normalize();
        for (int i = 0; i < 64; i++) {
            Location point = start.clone().add(direction.clone().multiply(i * step));
            world.spawnParticle(Particle.FLAME, point, 3);
        }
    }

    private static void spawnLegacyHugeExplosion(Player player, int count) {
        Particle particle = particleByName("EXPLOSION_HUGE", "EXPLOSION_EMITTER", "EXPLOSION");
        if (particle != null) {
            player.spawnParticle(particle, player.getLocation(), count);
        }
    }

    private static Particle particleByName(String... names) {
        for (String name : names) {
            try {
                return Particle.valueOf(name);
            } catch (IllegalArgumentException ignored) {
                // Try the next cross-version particle name.
            }
        }
        return null;
    }

    private static boolean canBreakCrystal(Player player) {
        SlimefunItem item = SlimefunItem.getByItem(player.getInventory().getItemInMainHand());
        if (!(item instanceof ModularGearItem gear)) {
            return false;
        }

        return gear.getGearType() == GearType.STAFF || gear.getGearTier() == 3;
    }

    public static boolean isWearingModularArmor(Player player) {
        SlimefunItem chestplate =
                SlimefunItem.getByItem(player.getInventory().getChestplate());
        return chestplate instanceof ModularArmorItem;
    }

    private void snapshotParticipants(EnderDragon dragon, Location center) {
        Collection<Player> nearby = dragon.getWorld().getNearbyPlayers(center, 256D);
        participantSnapshots.put(
                dragon.getUniqueId(),
                nearby.stream().map(Player::getUniqueId).toList());
    }

    private static boolean validCombatant(Player player, EnderDragon dragon) {
        return player.isOnline()
                && !player.isDead()
                && dragon.isValid()
                && !dragon.isDead()
                && player.getWorld() == dragon.getWorld();
    }

    private static int crystalCount(EnderDragon dragon) {
        Integer count = dragon.getPersistentDataContainer().get(
                CRYSTAL_COUNT,
                PersistentDataType.INTEGER);
        return count == null ? 0 : Math.max(0, count);
    }

    private void onChaosCrystalRetired(UUID crystalId, World world, EnderDragon guardian) {
        if (cleanupCrystalCages) {
            cleanupCage(crystalId, world);
        } else {
            cageBlocks.remove(crystalId);
        }

        if (guardian == null) {
            return;
        }

        Slimefun.runSyncFor(guardian, () -> {
            if (isGuardian(guardian)) {
                guardian.getPersistentDataContainer().set(
                        CRYSTAL_COUNT,
                        PersistentDataType.INTEGER,
                        Math.max(0, crystalCount(guardian) - 1));
            }
        });
    }

    private static boolean isGuardian(Entity entity) {
        return entity instanceof EnderDragon && hasTag(entity, GUARDIAN);
    }

    private static boolean isTemporarilyInvulnerable(EnderDragon dragon) {
        Long until = dragon.getPersistentDataContainer().get(
                INVULNERABLE_UNTIL,
                PersistentDataType.LONG);
        if (until == null) {
            return false;
        }

        if (until > System.currentTimeMillis()) {
            return true;
        }

        dragon.getPersistentDataContainer().remove(INVULNERABLE_UNTIL);
        return false;
    }

    private static void tag(Entity entity, NamespacedKey key) {
        entity.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);
    }

    private static boolean hasTag(Entity entity, NamespacedKey key) {
        Byte value = entity.getPersistentDataContainer().get(key, PersistentDataType.BYTE);
        return value != null && value != 0;
    }

    private static void initializeBossBar(BossBar bossBar) {
        if (bossBar == null) {
            return;
        }

        bossBar.setColor(BarColor.RED);
        bossBar.addFlag(BarFlag.CREATE_FOG);
        bossBar.addFlag(BarFlag.DARKEN_SKY);
        bossBar.addFlag(BarFlag.PLAY_BOSS_MUSIC);
    }

    private static void initiateRespawn(DragonBattle battle) {
        try {
            Method noArg = battle.getClass().getMethod("initiateRespawn");
            noArg.invoke(battle);
            return;
        } catch (ReflectiveOperationException ignored) {
            // Try the Paper compatibility overload below.
        }

        try {
            Method collectionArg = battle.getClass().getMethod("initiateRespawn", Collection.class);
            collectionArg.invoke(battle, new Object[] {null});
        } catch (ReflectiveOperationException ignored) {
            // The caller's initialization retry will eventually time out cleanly.
        }
    }

    private static void applyMultiplier(
            org.bukkit.entity.LivingEntity entity,
            String[] attributeNames,
            double multiplier) {
        AttributeInstance instance = findAttribute(entity, attributeNames);
        if (instance == null) {
            return;
        }

        double base = instance.getBaseValue();
        double target = base * multiplier;
        if (Double.isFinite(target) && target > 0D) {
            instance.setBaseValue(target);
        }
    }

    private static AttributeInstance findAttribute(
            org.bukkit.entity.LivingEntity entity,
            String... attributeNames) {
        for (String name : attributeNames) {
            try {
                Field field = Attribute.class.getField(name);
                Object value = field.get(null);
                if (value instanceof Attribute attribute) {
                    AttributeInstance instance = entity.getAttribute(attribute);
                    if (instance != null) {
                        return instance;
                    }
                }
            } catch (ReflectiveOperationException | LinkageError ignored) {
                // Try the next cross-version field name.
            }
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    private static void applyEffect(Player player, String effectName, int duration, int amplifier) {
        PotionEffectType type = PotionEffectType.getByName(effectName);
        if (type != null) {
            player.addPotionEffect(new PotionEffect(type, duration, amplifier, true, false, false));
        }
    }

    private static ItemStack itemById(String id, int amount) {
        SlimefunItem item = SlimefunItem.getById(id);
        if (item == null) {
            return null;
        }

        ItemStack stack = item.getItem().clone();
        stack.setAmount(amount);
        return stack;
    }
}
