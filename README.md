# SF_DracFun2

Clean-room Slimefun Legacy reimplementation of the gameplay concepts and world-compatibility surface of the discontinued **DracFun 2.0.10** addon.

> **Important:** This repository does not contain Phoenix's original source code, decompiled source, or original bundled assets. It is an independent implementation maintained for modern Slimefun Legacy servers. Phoenix is credited as the author of the original DracFun project; Phoenix has not authored or endorsed this repository.

## Current project version

**DracFun Reborn 2.0.2**

Final release artifact:

`SFL_DracFun-Reborn2.0.2.jar`

## Targets

- Slimefun Legacy 4.1.51+
- Minecraft 1.21.11 through the 26.x line
- Paper 1.21.11 / 26.2 / 26.3
- Purpur 1.21.11 / 26.2 / 26.3 when its API is available
- Folia 26.2+ where the implemented feature can be made region-safe
- Java 21 release bytecode

## Compatibility goals

The project preserves documented/observable legacy identifiers such as `DRACFUN_*` Slimefun IDs and persistent-data keys where needed so existing servers can migrate without silently losing registered items or machine state. New Java code lives under the `io.github.wickidcow.sfdracfun2` namespace.

The implementation does not copy original method bodies, decompiled Java, textures, models, custom-head assets, or other protected assets from the discontinued binary.

## Restored systems

DracFun Reborn 2.0.2 includes the completed clean-room restoration of the audited 134-ID legacy surface, Draconium progression and End resource generation, Energy Infuser, Item Converter, Fusion Crafting, modular equipment/modules, Energy Core multiblocks, Draconic Reactor, and Chaos Guardian encounter.

The 2.0.2 parity pass additionally restores or locks the original guide hierarchy, item presentation, machine GUIs, recipes and Fusion costs, Module Integrater behavior, modular gear attributes, Auto-Feed table/behavior, bow behavior, Reactor controls/status screen, Energy Core geometry/capacities, Guardian targeting/timing/attacks, legacy sounds, messages, and persisted modular keys.

Fresh 2.0.2 configurations enable the complete restoration by default. Every major system remains independently toggleable under `features:`, and existing explicit server configuration values are respected.

Remove the original **DracFun 2.0.10** JAR before installing Reborn. If both plugins are present, Reborn disables itself rather than risk duplicate `DRACFUN_*` registrations.

## Legacy-exact defaults and modern safety

Where practical, 2.0.2 defaults to the observable DracFun 2.0.10 behavior—even when the old addon had quirks. For example, the original Arrow Penetration integer-division bug is preserved by default. Server owners can opt into the corrected pierce behavior with:

`compatibility.fix-broken-arrow-penetration: true`

A few implementation defects remain intentionally corrected for server safety and compatibility, including per-Energy-Core completeness state, Paper/Folia-safe scheduling, restart-safe committed Fusion output, protected/custom-block handling for extra AOE mutations, and a configurable Reactor explosion ceiling.

Original custom-head textures/assets are not copied; clean-room vanilla substitutes are used where necessary.

## Verification

CI guards the audited identity, recipes, guide, modular runtime, bow, Auto-Feed, Energy Core, Reactor, Guardian, machine UI and migration behavior. The matrix compiles against Paper 1.21.11, Paper/Purpur 26.2, Paper/Purpur 26.3, and Folia 26.2.

Main/tag builds also perform a real Paper 26.2 boot smoke with Slimefun Legacy 4.1.51 and require the complete **134/134** legacy identity audit plus **3/3** migration aliases before accepting the raw JAR.

## Build output

Release artifact naming is fixed to:

`SFL_DracFun-Reborn2.0.2.jar`

## Credits

- **Phoenix** — original DracFun concept and discontinued DracFun 2.0.10 addon
- **wickidcow** — clean-room Slimefun Legacy reimplementation and maintenance
