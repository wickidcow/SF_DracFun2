# SF_DracFun2

Clean-room Slimefun Legacy reimplementation of the gameplay concepts and world-compatibility surface of the discontinued **DracFun 2.0.10** addon.

> **Important:** This repository does not contain Phoenix's original source code, decompiled source, or original bundled assets. It is an independent implementation maintained for modern Slimefun Legacy servers. Phoenix is credited as the author of the original DracFun project; Phoenix has not authored or endorsed this repository.

## Current project version

**DracFun Reborn 2.0.1**

Final release artifact:

`SFL_DracFun-Reborn2.0.1.jar`

## Targets

- Slimefun Legacy 4.1.51+
- Minecraft 1.21.11 through the 26.x line
- Paper 1.21.11 / 26.2 / 26.3
- Purpur 1.21.11 / 26.2 / 26.3 when its API is available
- Folia 26.2+ where the implemented feature can be made region-safe
- Java 21 release bytecode

## Compatibility goals

The project may preserve documented/observable legacy identifiers such as `DRACFUN_*` Slimefun IDs and persistent-data keys when needed so existing servers can migrate without silently losing registered items or machine state. New Java code lives under the `io.github.wickidcow.sfdracfun2` namespace.

The implementation will not copy original method bodies, decompiled Java, textures, models, or other protected assets from the discontinued binary.

## Restored systems

DracFun Reborn 2.0.1 includes the completed clean-room restoration of the legacy ID/data surface, Draconium progression and End resource generation, Energy Infuser, Item Converter, Fusion Crafting, modular equipment and modules, Energy Core multiblocks, Draconic Reactor, and Chaos Guardian encounter.

Fresh 2.0.1 configurations enable the complete restoration by default. Every major system remains independently toggleable under `features:` so server owners can stage or disable individual systems without preventing the rest of the addon from loading. Existing explicit configuration values are respected.

## Build output

Release artifact naming is fixed to:

`SFL_DracFun-Reborn2.0.1.jar`

## Credits

- **Phoenix** — original DracFun concept and discontinued DracFun 2.0.10 addon
- **wickidcow** — clean-room Slimefun Legacy reimplementation and maintenance
