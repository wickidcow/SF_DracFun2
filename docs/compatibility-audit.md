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
