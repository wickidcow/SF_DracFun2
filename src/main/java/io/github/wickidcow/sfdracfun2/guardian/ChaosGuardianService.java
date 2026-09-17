package io.github.wickidcow.sfdracfun2.guardian;

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
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.DragonBattle;
import org.bukkit.entity.DragonFireball;
import org.bukkit.entity.EndCrystal;
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
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
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

    private static final long ATTACK_PERIOD_TICKS = 600L;
    private static final double PARTICIPANT_RANGE_SQUARED = 256D * 256D;

    private final SFDracFun2 plugin;
    private final boolean crystalCages;
    private final boolean punishUnarmored;
    private final int witherLifetimeTicks;
    private final double laserDamageCap;
    private final Set<UUID> pendingWorlds = ConcurrentHashMap.newKeySet();

    public ChaosGuardianService(SFDracFun2 plugin) {
        this.plugin = plugin;
        crystalCages = plugin.getConfig().getBoolean("guardian.crystal-cages-enabled", true);
        punishUnarmored = plugin.getConfig().getBoolean("guardian.punish-unarmored", true);
        witherLifetimeTicks = Math.max(
                20,
                plugin.getConfig().getInt("guardian.wither-minion-lifetime-ticks", 200));
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
                        dragon.getPersistentDataContainer().set(
                                CRYSTAL_COUNT,
                                PersistentDataType.INTEGER,
                                battle.getHealingCrystals().size());
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
                    + "The Chaos Guardian can only be invoked in The End.");
            return false;
        }

        DragonBattle battle = world.getEnderDragonBattle();
        if (battle == null) {
            player.sendMessage(ChatColor.RED
                    + "This End world does not expose a dragon battle.");
            return false;
        }

        if (battle.getEnderDragon() != null) {
            player.sendMessage(ChatColor.RED
                    + "A dragon battle is already active in this End world.");
            return false;
        }

        if (pendingWorlds.contains(world.getUID())) {
            player.sendMessage(ChatColor.YELLOW
                    + "A Chaos Guardian invocation is already in progress.");
            return false;
        }

        if (!isWearingModularArmor(player)) {
            player.sendMessage(ChatColor.RED
                    + "The Chaos Guardian rejects challengers without DracFun modular armor.");
            return false;
        }

        if (invocationItem == null || invocationItem.getAmount() <= 0) {
            return false;
        }

        invocationItem.setAmount(invocationItem.getAmount() - 1);
        pendingWorlds.add(world.getUID());

        player.playSound(
                player.getLocation(),
                Sound.ENTITY_ENDER_DRAGON_GROWL,
                2F,
                0.65F);
        player.sendMessage(ChatColor.DARK_PURPLE
                + "The End begins to answer the Chaos Orb...");

        Location owner = world.getSpawnLocation();
        Slimefun.runSyncAt(owner, () -> {
            if (!plugin.isEnabled()) {
                pendingWorlds.remove(world.getUID());
                return;
            }
            initiateRespawn(battle);
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
        dragon.setCustomNameVisible(true);
        tag(dragon, GUARDIAN);

        initializeBossBar(dragon.getBossBar());

        var crystals = battle.getHealingCrystals();
        dragon.getPersistentDataContainer().set(
                CRYSTAL_COUNT,
                PersistentDataType.INTEGER,
                crystals.size());

        for (EndCrystal crystal : crystals) {
            Slimefun.runSyncFor(crystal, () -> {
                tag(crystal, CRYSTAL);
                if (crystalCages) {
                    buildCage(crystal.getLocation());
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

        double health = dragon.getHealth();
        World world = dragon.getWorld();
        Location origin = new Location(world, 0D, 0D, 0D);

        for (Player player : world.getPlayers()) {
            Slimefun.runSyncFor(player, () -> {
                if (!plugin.isEnabled()
                        || !player.isOnline()
                        || player.isDead()
                        || player.getWorld() != world
                        || player.getLocation().distanceSquared(origin) > PARTICIPANT_RANGE_SQUARED) {
                    return;
                }

                if (health > 1800D) {
                    basicAttack(dragon, player);
                } else {
                    advancedAttack(dragon, player);
                }
            });
        }

        schedulePulse(dragon);
    }

    private void basicAttack(EnderDragon dragon, Player player) {
        int shots = 12 + ThreadLocalRandom.current().nextInt(6);
        for (int i = 0; i < shots; i++) {
            long delay = (long) i * 4L;
            Slimefun.runSyncFor(player, () -> {
                if (!validCombatant(player, dragon)) {
                    return;
                }

                Location target = player.getEyeLocation().clone();
                Slimefun.runSyncFor(dragon, () -> {
                    if (!dragon.isValid() || dragon.isDead()) {
                        return;
                    }

                    Vector direction = target.toVector()
                            .subtract(dragon.getEyeLocation().toVector())
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
            witherAttack(player);
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

                double damage = Math.min(laserDamageCap, pulse * 250D);
                player.damage(damage);
                player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 24);
                player.playSound(
                        player.getLocation(),
                        Sound.ENTITY_GENERIC_EXPLODE,
                        1.25F,
                        1.6F);
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

    private void witherAttack(Player player) {
        int count = ThreadLocalRandom.current().nextInt(3, 6);
        for (int i = 0; i < count; i++) {
            Location spawn = player.getLocation().clone().add(
                    ThreadLocalRandom.current().nextInt(-4, 4),
                    ThreadLocalRandom.current().nextInt(0, 4),
                    ThreadLocalRandom.current().nextInt(-4, 4));

            Wither wither = player.getWorld().spawn(spawn, Wither.class);
            tag(wither, MINION);
            wither.setCustomName(ChatColor.DARK_RED + "Guardian Wither");
            wither.setTarget(player);
            applyMultiplier(wither, new String[] {"GENERIC_FOLLOW_RANGE", "FOLLOW_RANGE"}, 200D);
            applyMultiplier(wither, new String[] {"GENERIC_MOVEMENT_SPEED", "MOVEMENT_SPEED"}, 2D);
            applyMultiplier(wither, new String[] {"GENERIC_FLYING_SPEED", "FLYING_SPEED"}, 2D);
            applyMultiplier(wither, new String[] {"GENERIC_ARMOR", "ARMOR"}, 8D);

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
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void onCrystalDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof EndCrystal crystal) || !hasTag(crystal, CRYSTAL)) {
            return;
        }

        if (!(event.getDamager() instanceof Player player) || !canBreakCrystal(player)) {
            event.setCancelled(true);
            return;
        }

        Location cage = crystal.getLocation().clone();
        DragonBattle battle = crystal.getWorld().getEnderDragonBattle();
        EnderDragon guardian = battle == null ? null : battle.getEnderDragon();

        Slimefun.runSyncFor(
                crystal,
                () -> {
                    // A surviving crystal keeps its shield contribution.
                },
                () -> onChaosCrystalRetired(cage, guardian),
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
    public void onFireballHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof DragonFireball fireball)) {
            return;
        }

        ProjectileSource shooter = fireball.getShooter();
        if (!(shooter instanceof EnderDragon dragon) || !isGuardian(dragon)) {
            return;
        }

        Set<Player> targets = new HashSet<>();
        if (event.getHitEntity() instanceof Player player) {
            targets.add(player);
        }

        for (Entity entity : fireball.getNearbyEntities(4D, 4D, 4D)) {
            if (entity instanceof Player player) {
                targets.add(player);
            }
        }

        for (Player player : targets) {
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
        player.getWorld().spawnParticle(Particle.FLAME, player.getLocation(), 32);
        player.playSound(
                player.getLocation(),
                Sound.ENTITY_GENERIC_EXPLODE,
                1.25F,
                0.75F);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onGuardianDeath(EntityDeathEvent event) {
        if (!(event.getEntity() instanceof EnderDragon dragon) || !isGuardian(dragon)) {
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

        pendingWorlds.remove(dragon.getWorld().getUID());
        cleanupTaggedCrystals(dragon.getWorld());

        if (!punishUnarmored) {
            return;
        }

        World world = dragon.getWorld();
        Location origin = new Location(world, 0D, 0D, 0D);
        for (Player player : world.getPlayers()) {
            Slimefun.runSyncFor(player, () -> {
                if (!player.isOnline()
                        || player.isDead()
                        || player.getWorld() != world
                        || player.getLocation().distanceSquared(origin) > PARTICIPANT_RANGE_SQUARED
                        || isWearingModularArmor(player)) {
                    return;
                }

                player.sendMessage(ChatColor.RED
                        + "The collapsing Chaos battle overwhelms your unprotected armor.");
                player.setHealth(0D);
            });
        }
    }

    private void cleanupTaggedCrystals(World world) {
        DragonBattle battle = world.getEnderDragonBattle();
        if (battle == null) {
            return;
        }

        for (EndCrystal crystal : battle.getHealingCrystals()) {
            Slimefun.runSyncFor(crystal, () -> {
                if (hasTag(crystal, CRYSTAL)) {
                    cleanupCage(crystal.getLocation());
                }
            });
        }
    }

    private void buildCage(Location crystalLocation) {
        Location base = crystalLocation.getBlock().getLocation();

        for (int x = -2; x <= 2; x++) {
            for (int y = -1; y <= 3; y++) {
                for (int z = -2; z <= 2; z++) {
                    boolean top = y == 3;
                    boolean shell = top || y == -1 || Math.abs(x) == 2 || Math.abs(z) == 2;
                    if (!shell) {
                        continue;
                    }

                    Location target = base.clone().add(x, y, z);
                    Slimefun.runSyncAt(target, () -> {
                        if (!plugin.isEnabled()) {
                            return;
                        }

                        var block = target.getBlock();
                        if (!block.getType().isAir()) {
                            return;
                        }

                        if (top) {
                            block.setType(ThreadLocalRandom.current().nextBoolean()
                                    ? Material.CRYING_OBSIDIAN
                                    : Material.OBSIDIAN);
                        } else {
                            block.setType(Material.IRON_BARS);
                        }
                    });
                }
            }
        }
    }

    private void cleanupCage(Location crystalLocation) {
        if (!crystalCages) {
            return;
        }

        Location base = crystalLocation.getBlock().getLocation();
        for (int x = -2; x <= 2; x++) {
            for (int y = -1; y <= 3; y++) {
                for (int z = -2; z <= 2; z++) {
                    boolean top = y == 3;
                    boolean shell = top || y == -1 || Math.abs(x) == 2 || Math.abs(z) == 2;
                    if (!shell) {
                        continue;
                    }

                    Location target = base.clone().add(x, y, z);
                    Slimefun.runSyncAt(target, () -> {
                        var block = target.getBlock();
                        Material type = block.getType();
                        if (type == Material.IRON_BARS
                                || type == Material.OBSIDIAN
                                || type == Material.CRYING_OBSIDIAN) {
                            block.setType(Material.AIR);
                        }
                    });
                }
            }
        }
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

    private void onChaosCrystalRetired(Location cage, EnderDragon guardian) {
        Slimefun.runSyncAt(cage, () -> cleanupCage(cage));

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
