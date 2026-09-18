package io.github.wickidcow.sfdracfun2.compat;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.wickidcow.sfdracfun2.SFDracFun2;
import io.github.wickidcow.sfdracfun2.setup.DracFunItemGroups;
import java.util.LinkedHashSet;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

/**
 * Registers inert hidden identities for legacy DracFun data that has not yet
 * received a functional clean-room implementation.
 */
public final class LegacyCompatibilityRegistry {

    private LegacyCompatibilityRegistry() {}

    public static int registerMissingIdentities(SFDracFun2 addon) {
        ItemGroup group = new ItemGroup(
                new NamespacedKey(addon, "legacy_compatibility"),
                new ItemStack(Material.RESPAWN_ANCHOR));

        Set<String> identities = new LinkedHashSet<>(LegacyIdentityCatalog.DRACFUN_2_0_10_IDS);
        identities.addAll(LegacyIdentityCatalog.PRE_2_0_10_MIGRATION_ALIASES);

        int registered = 0;
        for (String id : identities) {
            if (SlimefunItem.getById(id) != null) {
                continue;
            }

            SlimefunItemStack stack = new SlimefunItemStack(
                    id,
                    Material.BARRIER,
                    "&8Legacy DracFun Compatibility",
                    "&7Identity: &f" + id,
                    "&7Compatibility identity for a disabled or migration-only feature.",
                    "&7This placeholder is intentionally uncraftable and hidden.");

            LegacyPlaceholderItem item = new LegacyPlaceholderItem(group, stack);
            item.register(addon);
            item.setHidden(true);
            registered++;
        }

        return registered;
    }

    /**
     * Audits the runtime registration surface after functional systems and optional
     * compatibility placeholders have been registered.
     */
    public static IdentityAudit auditRegisteredIdentities() {
        int registeredItems = 0;
        int placeholders = 0;
        int missingItems = 0;

        for (String id : LegacyIdentityCatalog.DRACFUN_2_0_10_IDS) {
            SlimefunItem item = SlimefunItem.getById(id);
            if (item == null) {
                missingItems++;
            } else {
                registeredItems++;
                if (isPlaceholder(item)) {
                    placeholders++;
                }
            }
        }

        int aliasesRegistered = 0;
        int aliasPlaceholders = 0;
        for (String id : LegacyIdentityCatalog.PRE_2_0_10_MIGRATION_ALIASES) {
            SlimefunItem item = SlimefunItem.getById(id);
            if (item != null) {
                aliasesRegistered++;
                if (isPlaceholder(item)) {
                    aliasPlaceholders++;
                }
            }
        }

        return new IdentityAudit(
                DracFunItemGroups.hasLegacyGuideCategory(),
                registeredItems,
                placeholders,
                missingItems,
                aliasesRegistered,
                aliasPlaceholders);
    }

    /** Returns whether an identity is still backed only by our inert compatibility placeholder. */
    public static boolean isPlaceholder(SlimefunItem item) {
        return item instanceof LegacyPlaceholderItem;
    }

    public record IdentityAudit(
            boolean guideCategoryPresent,
            int registeredItems,
            int placeholders,
            int missingItems,
            int migrationAliasesRegistered,
            int migrationAliasPlaceholders) {

        public int accountedLegacyIds() {
            return registeredItems;
        }

        public int functionalItems() {
            return registeredItems - placeholders;
        }
    }
}
