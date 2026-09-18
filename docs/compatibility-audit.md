# DracFun 2.0.10 compatibility audit

This document records observable compatibility facts from the discontinued DracFun 2.0.10 binary supplied for migration testing. It is **not** reconstructed source code.

## Binary metadata

- Plugin name: `DracFun`
- Version: `2.0.10`
- Main class: `me.phoenix.dracfun.DracFun`
- Declared Bukkit API: `1.20`
- Hard dependency: `Slimefun`
- Legacy website field: `https://github.com/PhoenixCodingStuff/DracFun`
- Binary contains 84 class files total
- 70 class files belong to the DracFun implementation namespace
- 14 class files are a shaded metrics implementation
- `DracFunItems` exposes 133 static Slimefun item definitions
- `ItemConverter` contributes one additional registered identity, `DRACFUN_ITEM_CONVERTER`
- Total confirmed DracFun 2.0.10 registered identity surface: **134 IDs**

## Legacy configuration surface

The old default configuration contains two options:

```yaml
options:
  hard-mode: true
  use-dragon-egg: true
```

SF_DracFun2 retains those keys so an existing server can carry its configuration forward.

## Observable feature groups

The old binary exposes systems for:

- Draconium materials and an End resource
- Dragon Heart / Chaos material progression
- tiered Draconic cores and energy cores
- Energy Infuser
- item conversion
- Fusion Crafting core/injectors/crafters
- modular armor, tools, bows, staff, flux capacitors and modules
- Energy Core multiblock
- Draconic Reactor
- Chaos Guardian encounter/invocation

These systems will be restored independently so a failure in one subsystem does not prevent the rest of the addon from enabling.

## Identity audit details

- `DRACFUN_GUIDE` is the legacy Slimefun guide/category icon identity, not a normal craftable item. SF_DracFun2 uses it as the icon for one shared `DracFun` ItemGroup and excludes it from inert placeholder registration.

Not every Java field name in the old binary is the actual persisted Slimefun ID. Two generated families matter for migration:

- Module field names such as `DRACFUN_BASIC_AOE_MODULE` generate the runtime ID `DRACFUN_AOE_BASIC_MODULE`.
- Staff fields such as `DRACFUN_DRACONIC_STAFF` generate `DRACFUN_DRACONIC_STAFF_OF_POWER`.
- The old `ENERGY` module field names are generated with the module key `POWER`, e.g. `DRACFUN_POWER_WYVERN_MODULE`.

The verified runtime forms are recorded in `LegacyIdentityCatalog`, rather than blindly reusing Java field names.

DracFun 2.0.10's Item Converter also recognizes three older armor IDs from pre-2.0.10 data:

- `DRACFUN_WYVERN_CHESTPLATE`
- `DRACFUN_DRACONIC_CHESTPLATE`
- `DRACFUN_CHAOTIC_CHESTPLATE`

Those are tracked separately as migration aliases. The 2.0.10 registered armor identities are `DRACFUN_WYVERN_ARMOR`, `DRACFUN_DRACONIC_ARMOR`, and `DRACFUN_CHAOTIC_ARMOR`.

### 134-ID implementation coverage

A source-level coverage pass now accounts for every identity in `LegacyIdentityCatalog.DRACFUN_2_0_10_IDS`:

- 44 fixed IDs are claimed directly by the material, shared-progression, machine, reactor, guardian, energy-core and modular registries.
- 26 modular gear IDs are generated from the registered `GearType`/tier combinations (24 three-tier gear IDs plus Draconic and Chaotic Staff of Power).
- 56 module IDs are generated from every supported `ModuleFamily` / `ModuleTier` combination.
- 3 Energy Core activator IDs are supplied by `EnergyCoreTier`.
- 4 Fusion Crafter IDs are supplied by `FusionTier`.
- `DRACFUN_GUIDE` is intentionally the shared guide-category icon identity rather than a normal Slimefun item.

Total accounted legacy 2.0.10 identity surface: **134 / 134**. Feature toggles can still intentionally leave disabled subsystems represented by hidden compatibility placeholders at runtime.

### Item Converter migration fallback

DracFun 2.0.10 accepted 18 legacy converter inputs, including the three pre-2.0.10 armor aliases `DRACFUN_WYVERN_CHESTPLATE`, `DRACFUN_DRACONIC_CHESTPLATE`, and `DRACFUN_CHAOTIC_CHESTPLATE`. The original converter first attempted normal Slimefun lookup and, if that failed, read the stored Slimefun item-data identity directly from the ItemStack.

Reborn preserves the same fallback through Slimefun Legacy's maintained `CustomItemDataService`. This means an old convertible item can still be recognized when `compatibility.preserve-legacy-ids` is disabled and no placeholder has registered its legacy ID. The converted output is rebuilt from the current registered template and preserves only stack amount, preventing stale legacy metadata from carrying forward.

### Runtime identity self-audit

After feature registration and optional placeholder registration, Reborn performs a startup audit of the complete legacy identity surface. The audit reports the number of registered non-placeholder items, hidden placeholders, whether the `DRACFUN_GUIDE` category identity is present, and how many of the three pre-2.0.10 armor migration aliases are registered. With `compatibility.preserve-legacy-ids: true`, anything short of 134/134 legacy identities or 3/3 migration aliases produces a warning.

The Module Integrator and Item Converter both use the shared fail-closed `ProtectionCompat` bridge rather than maintaining separate reflective protection implementations.

## Modular persistence contract

The old modular system stores item state under the original Bukkit namespace `dracfun`. SF_DracFun2 therefore creates compatibility keys with an explicit `dracfun` namespace instead of using this plugin's own namespace.

Confirmed modular keys include:

- `DRACFUN_ENERGY`
- `DRACFUN_CAPACITY`
- `DRACFUN_FUSION_POWER`
- `DRACFUN_SHIELD`
- `DRACFUN_COOLDOWN`
- one integer key for each installed runtime module ID such as `DRACFUN_POWER_WYVERN_MODULE`

Gear fusion-power requirements are 8,000,000 for Wyvern, 32,000,000 for Draconic, and 128,000,000 for Chaotic gear.

Module storage is additive by tier. A module family can have counts at Basic, Wyvern, Draconic, and Chaotic tiers and an effect is calculated by multiplying each installed count by that tier's value and summing the result.

Module-point cost is the square of the module's declared size: size 1 costs 1 point, size 2 costs 4, and size 3 costs 9. Module limits and gear compatibility are preserved in `ModuleFamily` and `GearType`.

Two implementation problems in 2.0.10 are intentionally corrected without changing the persisted format:

- module-point usage is derived from each ItemStack's PDC instead of a mutable counter on the singleton Slimefun item object;
- charge is clamped to the valid range `0..capacity`, including when capacity is reduced.

Removing all modules also removes legacy shield/cooldown state and resets charge/capacity, matching the observable reset behavior while avoiding shared-state leakage.

### Modular AOE and HARVEST behavior

The supplied 2.0.10 binary shows that powered modular tool effects require positive capacity, positive charge, and a DracFun modular armor chestplate.

- AOE activates while sneaking. The highest installed AOE tier selects a cube radius of 1 (Basic), 2 (Wyvern), 3 (Draconic), or 4 (Chaotic), and the activation consumes one charge.
- Pickaxe/shovel AOE breaks valid blocks in that cube using the modular tool.
- Hoe AOE converts valid dirt variants in the same cube to farmland.
- Staff of Power right-click mining uses Netherite Pickaxe drops; sneaking with AOE extends that mining through the AOE cube.
- HARVEST activates on a sneaking modular-axe log break. Its Wyvern/Draconic/Chaotic limits are 16/64/128 connected logs, using the ten legacy vein-adjacency directions (six cardinal/up/down plus four horizontal diagonals), and consumes one charge.

Reborn keeps those limits while applying two safety corrections: the already-breaking AOE origin is skipped to prevent duplicate drops, and extra block mutations exclude Slimefun/custom/protected blocks. Block mutation is dispatched through the Paper/Folia region scheduler.

### Modular AUTO_FEED behavior

The legacy module count keys use the same generated runtime form as the module item IDs: `DRACFUN_<FAMILY>_<TIER>_MODULE`. The JVM concat recipe in 2.0.10 explicitly appends `_<TIER>_MODULE`, so Reborn's existing per-tier PDC keys are migration-compatible.

AUTO_FEED also uses the separate family key `DRACFUN_AUTO_FEED` as a buffered-food counter on modular armor. Its highest installed tier sets a capacity of 40 (Basic), 150 (Wyvern), 400 (Draconic), or 1000 (Chaotic) hunger points.

- Sneak-right-clicking modular armor in the main hand loads supported food from the offhand into that buffer.
- When a food-level change would reach 12 or lower, the chestplate feeds the player from the buffer and plays the legacy burp sound.
- On starvation damage, the chestplate feeds the player from the buffer but does not cancel the current starvation hit.
- Reborn preserves offhand item metadata when consuming only part of a food stack; the old implementation rebuilt leftovers from material/amount and could discard metadata.

### Energy Core and Guardian safety corrections

The Energy Core keeps 2.0.10's multiblock layouts and capacities, but replaces the old global `notComplete` flag with per-activator state. EnergyNet activity now also re-validates the local structure before exposing stored charge, so a known broken structure stops supplying power immediately rather than waiting for the next core ticker. An `UNKNOWN` validation caused only by unloaded neighboring data retains the last-known state and never destroys charge.

Chaos Guardian crystal cages are terrain-safe in both directions. Reborn only places cage blocks into air, records the exact coordinates and materials it created, persists that record on the crystal for restart recovery, and removes only those exact unchanged blocks during cleanup. Pre-existing/player blocks in the cage shell are never deleted by the cleanup pass.

### 2.0.1 default feature set

The restoration-era configuration originally left Energy Infuser, Item Converter, Fusion Crafting, modular gear, Energy Core, Reactor and Chaos Guardian disabled while those systems were being implemented. DracFun Reborn 2.0.1 now enables the complete restored feature set on fresh configurations. All feature flags remain independent and existing explicit server configuration values are never overwritten.

The release workflow verifies that the packaged `config.yml` has all nine restored feature flags enabled, in addition to the existing cross-platform compilation, clean-room boundary and raw-JAR checks.

## Confirmed modern compatibility breakpoints

### Bukkit attribute names

The old binary directly references legacy attribute constants such as:

- `GENERIC_ARMOR`
- `GENERIC_ARMOR_TOUGHNESS`
- `GENERIC_ATTACK_SPEED`
- `GENERIC_ATTACK_DAMAGE`

Modern Paper exposes the corresponding attribute identities without the old `GENERIC_` constant names. A direct binary load can therefore fail during item initialization. The clean implementation will use the current API rather than attempting to patch the old class files.

### Scheduler / Folia safety

The old plugin schedules global Bukkit repeating tasks. Two modular-item tasks and the invincibility maintenance task are scheduled asynchronously, while interacting with player/entity state. That design is not suitable for modern Paper safety expectations and is not Folia-region-safe.

SF_DracFun2 will route entity/player work through platform-safe scheduling and will not perform Bukkit inventory/entity mutation from asynchronous worker threads.

### Legacy Slimefun storage API

The old binary references `me.mrCookieSlime.Slimefun.api.BlockStorage`. Slimefun Legacy still provides a compatibility facade for old addons, but new SF_DracFun2 code should use the maintained storage/data-controller paths wherever practical instead of adding new dependencies on the deprecated facade.

### Telemetry

The old JAR shades its own metrics classes. SF_DracFun2 intentionally omits that copied telemetry implementation. This avoids another runtime dependency/conflict surface and keeps the clean-room artifact limited to new project code.

## Compatibility behavior during restoration

Until a legacy ID has a functional clean-room implementation, SF_DracFun2 may register that ID as a hidden, recipe-less, non-placeable placeholder. This has three goals:

1. allow Slimefun Legacy to recognize existing item metadata instead of treating the ID as unknown;
2. prevent players from crafting or placing incomplete implementations;
3. allow the placeholder to disappear automatically once a functional item has already claimed that ID.

The placeholder layer is controlled by `compatibility.preserve-legacy-ids` and does not contain original DracFun assets.

## Compatibility identities

Existing-world compatibility may require preservation of observable identifiers such as:

- verified `DRACFUN_*` Slimefun item IDs
- `DRACFUN_*` persistent-data keys
- machine/block identities stored by Slimefun
- the two legacy config options above

Identifier preservation does **not** require using Phoenix's Java package namespace. New source code remains under `io.github.wickidcow.sfdracfun2`.

## Clean-room boundary

Allowed inputs for the reimplementation are observable behavior and compatibility facts such as item IDs, configuration keys, recipes/data formats needed for interoperability, user-visible behavior, and API interaction requirements.

The repository must not contain Phoenix's decompiled method bodies, reconstructed original source files, original textures/models, or shaded original classes.
