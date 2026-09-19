package io.github.wickidcow.sfdracfun2.compat;

import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

/**
 * Reproduces the observable item-name/lore theme surface from DracFun 2.0.10.
 *
 * <p>This is interoperability presentation data only. It intentionally uses
 * vanilla item materials supplied by the clean-room implementation and does
 * not bundle Phoenix's original custom-head assets.</p>
 */
public enum LegacyTheme {

    ORE("#C0C0C0", "Ore Resource"),
    BASIC_CRAFTING("#800080", "Basic Crafting Component"),
    ADVANCED_CRAFTING("#FFA500", "Advanced Crafting Component"),
    END_GAME_CRAFTING("#333333", "End-Game Crafting Component"),
    ENERGY_CORE("#FF0000", "Energy Core Component"),
    FUSION_CRAFTING("#40E0D0", "Fusion Crafting Component"),
    MACHINE("#FFFF00", "Electric Machine"),
    MOB("#3A3B3C", "Mob Drop"),
    REACTOR("#FFA500", "Draconic Reactor Part"),
    BASIC_MODULAR("#00FFFF", "Basic Modular Item"),
    WYVERN_MODULAR("#800080", "Wyvern Modular Item"),
    DRACONIC_MODULAR("#FFA500", "Draconic Modular Item"),
    CHAOTIC_MODULAR("#333333", "Chaotic Modular Item");

    private static final String GRAY = "\u00A77";

    private final String color;
    private final String loreLine;

    LegacyTheme(String hexColor, String loreLine) {
        this.color = legacyHex(hexColor);
        this.loreLine = loreLine;
    }

    public String color() {
        return color;
    }

    public String loreLine() {
        return loreLine;
    }

    public SlimefunItemStack stack(
            String id,
            Material material,
            String displayName,
            String... customLore) {
        return new SlimefunItemStack(
                id,
                material,
                color + displayName,
                lore(customLore));
    }

    public SlimefunItemStack stack(
            String id,
            ItemStack item,
            String displayName,
            String... customLore) {
        return new SlimefunItemStack(
                id,
                item,
                color + displayName,
                lore(customLore));
    }

    public String[] lore(String... customLore) {
        List<String> lines = new ArrayList<>();
        // DracFun 2.0.10's Theme helper always inserted this opening blank line.
        lines.add("");

        if (customLore != null) {
            for (String line : customLore) {
                lines.add(GRAY + line);
            }
        }

        // It also inserted another blank line before the theme/type marker.
        lines.add("");
        lines.add(color + loreLine);
        return lines.toArray(String[]::new);
    }

    private static String legacyHex(String hex) {
        String normalized = hex.startsWith("#") ? hex.substring(1) : hex;
        if (normalized.length() != 6) {
            throw new IllegalArgumentException("Expected six-digit RGB hex color, got " + hex);
        }

        StringBuilder out = new StringBuilder("\u00A7x");
        for (int i = 0; i < normalized.length(); i++) {
            out.append("\u00A7").append(normalized.charAt(i));
        }
        return out.toString();
    }
}
