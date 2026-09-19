package io.github.wickidcow.sfdracfun2.modular;

import java.util.ArrayList;
import java.util.List;
import io.github.thebusybiscuit.slimefun4.utils.ChatUtils;
import org.bukkit.ChatColor;
import org.bukkit.inventory.ItemStack;

/**
 * Recreates the observable modular-item lore format from DracFun 2.0.10.
 */
public final class LegacyModularLore {

    private static final String GOLD = ChatColor.GOLD.toString();
    private static final String AQUA = ChatColor.AQUA.toString();
    private static final String GREEN = ChatColor.GREEN.toString();
    private static final String GRAY = ChatColor.GRAY.toString();

    private LegacyModularLore() {}

    public static List<String> defaultGearLore(GearType type, int tier) {
        List<String> lore = new ArrayList<>();

        switch (type) {
            case ARMOR -> {
                lore.add(flag("🚫", "Arrow Immunity", false));
                lore.add(value("🪽", "Flight Boost", "None"));
                lore.add(value("🍴", "Food Storage", "None"));
                lore.add(value("🦘", "Jump Boost", "None"));
                lore.add(value("⛨", "Shield Capacity", "None"));
                lore.add(value("⏳", "Cooldown", "N/A"));
                lore.add(value("⇌", "Shield Recharge Rate", "N/A"));
                lore.add(value("💨", "Speed Boost", "None"));
                lore.add(flag("🪦", "Undying", false));
                lore.add(flag("🥽", "Night Vision", false));
            }
            case AXE -> {
                lore.add(value("†", "Damage", "Default"));
                lore.add(value("🪓", "Harvest Range", "None"));
            }
            case BOW -> {
                lore.add(value("➵", "Arrow Damage", "Default"));
                lore.add(flag("☁", "Arrow Gravity", false));
                lore.add(value("⤖", "Arrow Penetration", "Default"));
                lore.add(value("⥵", "Arrow Speed", "Default"));
            }
            case HOE, PICKAXE, SHOVEL, TOOL -> {
                // DracFun 2.0.10 intentionally inserted a newline entry before AOE.
                lore.add("\n");
                lore.add(value("✾", "AOE", "None"));
            }
            case STAFF -> {
                lore.add(value("†", "Damage", "Default"));
                lore.add(value("✾", "AOE", "None"));
            }
            case SWORD -> lore.add(value("†", "Damage", "Default"));
            case CAPACITOR, ALL -> {
                // Capacitors had no capability lines before the generic modular metadata.
            }
        }

        lore.add(moduleLimit(type.maxModulePoints(tier)));
        lore.add(gearType(type.legacyName()));
        lore.add(energy(0, 0));
        return lore;
    }

    public static List<String> moduleLore(ModuleFamily family, ModuleTier tier) {
        List<String> lore = new ArrayList<>(effectLore(family, tier));
        lore.add(moduleSize(family.size()));
        lore.add(moduleLimit(family.installLimit()));
        lore.add(gearType(family.targetType().legacyName()));
        return lore;
    }

    public static List<String> liveGearLore(ItemStack stack, ModularGearItem gear) {
        List<String> lore = new ArrayList<>();

        switch (gear.getGearType()) {
            case ARMOR -> {
                lore.add(flag("🚫", "Arrow Immunity", ModuleEffects.arrowImmunity(stack)));
                lore.add(value("🪽", "Flight Boost", noneOr(ModuleEffects.flight(stack))));
                int foodCapacity = ModuleEffects.autoFeed(stack);
                lore.add(value(
                        "🍴",
                        "Food Storage",
                        foodCapacity <= 0
                                ? "None"
                                : ModularData.getAutoFeedFood(stack) + " / " + foodCapacity));
                lore.add(value("🦘", "Jump Boost", noneOr(ModuleEffects.jump(stack))));
                int shieldCapacity = ModuleEffects.shieldCapacity(stack);
                lore.add(value(
                        "⛨",
                        "Shield Capacity",
                        shieldCapacity <= 0
                                ? "None"
                                : ModularData.getShield(stack) + " / " + shieldCapacity));
                int cooldown = ModuleEffects.shieldCooldown(stack);
                lore.add(value("⏳", "Cooldown", cooldown <= 0 ? "N/A" : cooldown + " seconds"));
                int recovery = ModuleEffects.shieldRecovery(stack);
                lore.add(value(
                        "⇌",
                        "Shield Recharge Rate",
                        recovery <= 0 ? "N/A" : "+" + recovery + " Point/t"));
                lore.add(value("💨", "Speed Boost", noneOr(ModuleEffects.speed(stack))));
                lore.add(flag("🪦", "Undying", ModuleEffects.undying(stack)));
                lore.add(flag("🥽", "Night Vision", ModuleEffects.vision(stack)));
            }
            case AXE -> {
                lore.add(value("†", "Damage", defaultOrPlus(ModuleEffects.damage(stack))));
                lore.add(value("🪓", "Harvest Range", noneOr(ModuleEffects.harvest(stack))));
            }
            case BOW -> {
                lore.add(value("➵", "Arrow Damage", defaultOrPercent(ModuleEffects.arrowDamage(stack))));
                lore.add(flag("☁", "Arrow Gravity", ModuleEffects.arrowGravity(stack)));
                lore.add(value(
                        "⤖",
                        "Arrow Penetration",
                        defaultOr(ModuleEffects.arrowPenetration(stack))));
                lore.add(value("⥵", "Arrow Speed", defaultOr(ModuleEffects.arrowSpeed(stack))));
            }
            case HOE, PICKAXE, SHOVEL, TOOL -> {
                lore.add("\n");
                int aoe = ModuleEffects.aoe(stack);
                lore.add(value("✾", "AOE", aoe <= 0 ? "None" : areaDisplay(aoe)));
            }
            case STAFF -> {
                lore.add(value("†", "Damage", defaultOrPlus(ModuleEffects.damage(stack))));
                int aoe = ModuleEffects.aoe(stack);
                lore.add(value("✾", "AOE", aoe <= 0 ? "None" : areaDisplay(aoe)));
            }
            case SWORD -> lore.add(value("†", "Damage", defaultOrPlus(ModuleEffects.damage(stack))));
            case CAPACITOR, ALL -> {}
        }

        lore.add(moduleLimit(gear.getMaxModulePoints()));
        lore.add(gearType(gear.getGearType().legacyName()));
        lore.add(energy(ModularData.getCharge(stack), ModularData.getCapacity(stack)));
        return lore;
    }

    private static List<String> effectLore(ModuleFamily family, ModuleTier tier) {
        int level = tier.level();
        return switch (family) {
            case AOE -> List.of(value("✾", "AOE", switch (level) {
                case 0 -> "3x3";
                case 1 -> "5x5";
                case 2 -> "7x7";
                case 3 -> "9x9";
                default -> throw badTier(tier);
            }));
            case ARROW_DAMAGE -> List.of(value("➵", "Arrow Damage", "+" + switch (level) {
                case 1 -> 25;
                case 2 -> 50;
                case 3 -> 75;
                default -> throw badTier(tier);
            } + "%"));
            case ARROW_GRAVITY, ARROW_IMMUNITY, VISION -> List.of();
            case ARROW_PENETRATION -> List.of(value("⤖", "Arrow Penetration", Integer.toString(switch (level) {
                case 1 -> 25;
                case 2 -> 50;
                case 3 -> 75;
                default -> throw badTier(tier);
            })));
            case ARROW_SPEED -> List.of(value("⥵", "Arrow Speed", Integer.toString(switch (level) {
                case 1 -> 15;
                case 2 -> 35;
                case 3 -> 75;
                default -> throw badTier(tier);
            })));
            case DAMAGE -> List.of(value("†", "Damage", "+" + switch (level) {
                case 0 -> 2;
                case 1 -> 4;
                case 2 -> 8;
                case 3 -> 16;
                default -> throw badTier(tier);
            }));
            case POWER -> List.of(value("⚡", "QS Storage", "+" + tier.powerCapacity()));
            case FLIGHT -> List.of(value("🪽", "Flight Boost", Integer.toString(switch (level) {
                case 1 -> 100;
                case 2 -> 200;
                case 3 -> 300;
                default -> throw badTier(tier);
            })));
            case AUTO_FEED -> List.of(value("🍴", "Food Storage", "0 / " + switch (level) {
                case 0 -> 40;
                case 1 -> 150;
                case 2 -> 400;
                case 3 -> 1000;
                default -> throw badTier(tier);
            }));
            case HARVEST -> List.of(value("🪓", "Harvest Range", Integer.toString(switch (level) {
                case 1 -> 16;
                case 2 -> 64;
                case 3 -> 128;
                default -> throw badTier(tier);
            })));
            case JUMP -> List.of(value("🦘", "Jump Boost", Integer.toString(switch (level) {
                case 0 -> 25;
                case 1 -> 75;
                case 2 -> 125;
                case 3 -> 400;
                default -> throw badTier(tier);
            })));
            case SHIELD_CAPACITY -> List.of(value("⛨", "Shield Capacity", "0 / " + switch (level) {
                case 1 -> 25;
                case 2 -> 50;
                case 3 -> 100;
                default -> throw badTier(tier);
            }));
            case SHIELD_CONTROL -> List.of(value("⏳", "Cooldown", switch (level) {
                case 1 -> "15 seconds";
                case 2 -> "10 seconds";
                case 3 -> "5 seconds";
                default -> throw badTier(tier);
            }));
            case SHIELD_RECOVERY -> List.of(value("⇌", "Shield Recharge Rate", "+" + switch (level) {
                case 1 -> 1;
                case 2 -> 2;
                case 3 -> 5;
                default -> throw badTier(tier);
            } + " Point/t"));
            case SPEED -> List.of(value("💨", "Speed Boost", Integer.toString(switch (level) {
                case 0 -> 10;
                case 1 -> 25;
                case 2 -> 50;
                case 3 -> 150;
                default -> throw badTier(tier);
            })));
            case UNDYING -> List.of(
                    value("❤", "Health", "+" + switch (level) {
                        case 1 -> 3;
                        case 2 -> 6;
                        case 3 -> 10;
                        default -> throw badTier(tier);
                    } + " hearts"),
                    value("⏳", "Cooldown", switch (level) {
                        case 1 -> "120 seconds";
                        case 2 -> "60 seconds";
                        case 3 -> "45 seconds";
                        default -> throw badTier(tier);
                    }),
                    value("⚡", "Operation Cost", "+" + switch (level) {
                        case 1 -> "0.1";
                        case 2 -> "0.25";
                        case 3 -> "0.5";
                        default -> throw badTier(tier);
                    } + " QS/t"),
                    value("☠", "Invulnerability", "+" + switch (level) {
                        case 1 -> 4;
                        case 2 -> 6;
                        case 3 -> 8;
                        default -> throw badTier(tier);
                    } + " seconds"));
        };
    }

    public static String moduleSize(int size) {
        return value("⭿", "Module Size", size + "x" + size);
    }

    public static String moduleLimit(int limit) {
        return value("∎", "Limit", limit == -1 ? "No Limit" : Integer.toString(limit));
    }

    public static String gearType(String type) {
        return value("⚔", "Gear Type", ChatUtils.humanize(type));
    }

    public static String energy(int charge, int capacity) {
        return GREEN + "⚡ " + GRAY + charge + " / " + capacity + " QS";
    }

    private static String flag(String symbol, String label, boolean enabled) {
        return value(symbol, label, enabled ? "Yes" : "No");
    }

    private static String value(String symbol, String label, String value) {
        return GOLD + symbol + ' ' + AQUA + label + GOLD + " - " + AQUA + value;
    }

    private static String noneOr(int value) {
        return value <= 0 ? "None" : Integer.toString(value);
    }

    private static String defaultOr(int value) {
        return value <= 0 ? "Default" : Integer.toString(value);
    }

    private static String defaultOrPlus(int value) {
        return value <= 0 ? "Default" : "+" + value;
    }

    private static String defaultOrPercent(int value) {
        return value <= 0 ? "Default" : "+" + value + "%";
    }

    private static String areaDisplay(int radiusTierValue) {
        int side = radiusTierValue * 2 + 1;
        return side + "x" + side;
    }

    private static IllegalArgumentException badTier(ModuleTier tier) {
        return new IllegalArgumentException("Unexpected module tier " + tier);
    }
}
