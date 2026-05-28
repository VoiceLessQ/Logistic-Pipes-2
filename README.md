# Logistic Pipes 2

A revival of LogisticsPipes, ported from Forge 1.12.2 to **NeoForge 1.21.1** — bringing the classic request-based item routing, automated crafting, and modular chassis pipes back to modern Minecraft. Also runs on **Fabric 1.21.1** via Architectury.

See the original [CurseForge page](https://www.curseforge.com/minecraft/mc-mods/logistics-pipes) for background on what the mod has been since its 1.4.x days.

> **Beta / work in progress.** Expect bugs, crashes, missing polish, and the occasional broken feature. Back up your worlds before testing, and please open an issue if something goes wrong — noisy reports are more useful than silent frustration.

## What is Logistic Pipes

A logistics mod that lets you build pipe networks capable of requesting items on demand, automatically sorting inventory, and triggering crafting chains. Pipes connect to any inventory and route items intelligently based on your configured rules — no constant item flow, no lost items, just point-to-point delivery driven by what you ask for.

## Status

Work in progress. The migration is an ongoing port of the original 1.12.2 codebase to the modern toolchain. Core gameplay (placing pipes, routing, requesting, chassis modules, crafting) is functional. Power Junction + RF intake works on NeoForge. Polish, missing GUIs, and third-party integrations are the main outstanding work. See [PROJECT_STATE.md](PROJECT_STATE.md) for the directory layout and what's been stripped from earlier attempts.

## Supported loaders

| Loader | Support |
|---|---|
| **NeoForge 21.1.x** (MC 1.21.1) | ✅ primary target — Power Junction RF intake wired via `IEnergyStorage` |
| **Fabric** (MC 1.21.1, via Architectury) | ✅ core supported (external Fabric energy import via Team Reborn Energy is still stubbed) |
| **Forge** (any version) | ❌ not supported on 1.21.1 — may return for an eventual 1.20.1 backport (see below) |

### Why no Forge on 1.21.1

On the 1.21.x line NeoForge and Forge have diverged enough in API, mappings, and capability handling that supporting both means maintaining two platform-helper layers, two energy-cap bridges, and two registry styles for the same engine. The 1.21+ modding ecosystem has consolidated heavily on NeoForge, and Fabric (already supported via Architectury) covers the rest, so paying the dual-platform tax here isn't worth it. A future 1.20.1 backport is a different story — on that MC version Forge 47.x is what's actually alive, so that build (if it happens) will target Forge, not NeoForge.

### MC 1.20.1 — delayed, not abandoned

The old `0.0.1` jar on the `1.20.1` branch of this repo was built against NeoForge `20.1.88`, which NeoForge has since unpublished from their Maven. With the field-name mappings on that runtime gone, the jar throws `NoSuchFieldError: BLOCK_ENTITY_TYPE` on any current Forge 47.x or NeoForge 47.x loader (see [issue #1](https://github.com/VoiceLessQ/Logistic-Pipes-2/issues/1)).

We haven't given up on 1.20.1 — players on that version deserve a working build. The current focus is finishing the 1.21.1 line first, then revisiting 1.20.1 with whichever loader is realistic at that point (likely Forge 47.x, since the NeoForge 1.20.1 line is dead). No timeline yet. In the meantime the 1.21.1 build is the working one.

## Known limitations

- **Mod integrations disabled** — BuildCraft, IndustrialCraft 2, Thermal Dynamics, JEI, TheOneProbe, AE2 and similar integrations are stubbed out; most are waiting on upstream ports or haven't been wired yet.
- **Missing GUIs** — Power Junction has one (procedural energy bar). Crafter, Provider, Item Sink, Satellite, Firewall, Orderer, Security Station GUIs are not yet ported.
- **Per-pipe power gates not wired** — Power Junction stores and serves energy, but no pipe currently calls `useEnergy()` yet. Power flow is end-to-end ready; per-action gates land per pipe type as they migrate.
- **Items-in-transit rendering** — visible but still being polished.
- **No world upgrade path from 1.12.2 saves** — legacy data fixers are not ported; start on a 1.21.1 world.

## Planned

- Per-pipe power gates (Power Junction → routed pipe operations)
- Remaining pipe GUIs (Crafter, Provider, Item Sink, Satellite, Firewall, Orderer)
- Team Reborn Energy bridge for Fabric (external energy import)
- JEI and TheOneProbe integration
- Published API jar so addons can be built against a stable surface
- Performance improvements (render caching, dirty-flag system)

## Versions

- Minecraft **1.21.1**
- NeoForge **21.11.38-beta**
- Fabric Loader **0.18.4** + Fabric API **0.139.5+1.21.11**
- Architectury **19.0.1**
- Java **21** (provisioned automatically via Gradle toolchains)
- Gradle **8.14** + architectury-loom 1.13-SNAPSHOT

## Building

```
./gradlew build         # full build; output in fabric/build/libs/ and neoforge/build/libs/
./gradlew runClient     # launch a dev client (per platform module)
./gradlew runServer     # launch a dev server
./gradlew check         # unit tests
```

## Contributing

Issues and PRs welcome. Skim [PROJECT_STATE.md](PROJECT_STATE.md) and the open issues for current focus areas before starting work. The `mc-1.21.1` local branch (pushed as `1.21.1` on GitHub) is the active development line.

## Credits

- **Krapht** — original concept and early codebase
- **RS485 and LogisticsPipes contributors** — the 1.12.2 codebase this project is built on
- **NoZeroG (VoiceLessQ)** — modern port and ongoing maintenance

## License

Distributed under the Minecraft Mod Public License 1.0.1 — see LICENSE.md.

— The Logistic Pipes 2 Team
