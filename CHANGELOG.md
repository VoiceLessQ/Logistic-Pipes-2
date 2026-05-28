# Changelog

All notable changes to this project are documented here. The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project follows [Semantic Versioning](https://semver.org/) where practical.

## Unreleased — MC 1.21.1 line

The 1.21.1 line is a retarget from the abandoned NeoForge 1.20.1 line (see 0.0.1 below). New mod id `morph`, new group `com.Morph`, dual-platform via Architectury. No saves carry over from the 1.20.1 attempt.

### Added
- **Platform**: Architectury split — `common/`, `fabric/`, `neoforge/`. Fabric loader 0.18.4 + Fabric API 0.139.5+1.21.11 on the Fabric side; NeoForge 21.11.38-beta on the NeoForge side.
- **Routing**: ServerRouter / ClientRouter / RouterManager, Dijkstra route tables, BFS PathFinder, atomic route-table swap, alt-destination fallback, block-break invalidation.
- **Pipes**: all core types — Basic, Provider, Request (Mk1-3), Supplier, Satellite, Crafter (Mk1-3), Chassis Mk1-5, Firewall, fluid pipes, system entrance/destination, quicksort, remote orderer.
- **Modules**: ItemSink, Provider, ActiveSupplier, PassiveSupplier, Extractor, Terminus, Crafter, Satellite, Firewall, OreDictItemSink, PolymorphicItemSink, EnchantmentSink, CreativeTabBasedItemSink, ModBasedItemSink.
- **Request system**: RequestTree, RequestHandler, RequestLog, ItemResource, DictResource, in-transit tracking.
- **Transport**: PipeTransportLogistics, LPTravelingItem with client-side position sync.
- **Networking**: 25+ S2C/C2S packets via Architectury NetworkManager.
- **Rendering**: OBJ-based block entity renderer with LP1-accurate geometry, mount brackets, indicators, supports for straight pipes, per-type indicator textures.
- **GUIs**: Request pipe, Chassis pipe, Supplier pipe, Power Junction.
- **Power system (new this line)**:
  - `ILogisticsPowerProvider` / `IRoutedPowerProvider` interfaces
  - `LogisticsPowerJunctionBlock` + `BlockEntity` — 2,000,000 LP buffer with 2 RF = 1 LP conversion
  - NeoForge `IEnergyStorage` capability — cables can push RF in
  - Fabric Team Reborn Energy bridge stubbed (LP-internal use works on Fabric; external RF import deferred)
  - ServerRouter scans 6 neighbours for providers; CoreRoutedPipe `useEnergy()` / `canUseEnergy()` walk providers in cost order
  - Power Junction GUI with live RF fill bar
- **Config**: JSON config at `config/morph.json` — `power.usageMultiplier`, `power.usageDisabled`, `power.maxStorage`, `routing.refreshTicks`, `routing.maxNetworkSize`.
- **Docs**: `PROJECT_STATE.md` (directory map + stripped-work index), `LP1_ANALYSIS.md` (upstream codebase breakdown), `reference/power-1.20.1/` (151 files of the abandoned port preserved for reference).

### Known issues
- **Per-pipe power gates not wired** — Power Junction stores and serves energy, but no pipe currently calls `useEnergy()` yet. The plumbing is ready; gates land per pipe type during ongoing migration.
- **Missing GUIs** — Crafter, Provider, Item Sink, Satellite, Firewall, Orderer, Security Station screens not yet ported.
- **Fabric external energy import** — Team Reborn Energy bridge is stubbed; cables on Fabric can't push RF into a Power Junction yet.
- **Third-party mod integrations** — JEI, TheOneProbe, AE2, CC:Tweaked are stubbed/absent.
- **Items-in-transit rendering** — visible but still being tuned.
- **No world upgrade path from 1.12.2 or 1.20.1 saves** — legacy data fixers are not ported; start on a fresh 1.21.1 world.

## 0.0.1 — 2026-04-16 — **deprecated**

> ⚠️ This build was compiled against NeoForge 20.1.88. NeoForge has since unpublished the 20.1.x line from their Maven, and the field-name mappings on that runtime are incompatible with current Forge 47.x / NeoForge 47.x loaders — so the jar throws `NoSuchFieldError: BLOCK_ENTITY_TYPE` at load on any modern 1.20.1 environment (see [issue #1](https://github.com/VoiceLessQ/Logistic-Pipes-2/issues/1)). Use the 1.21.1 build instead.

First public beta of Logistic Pipes 2 — a NeoForge 1.20.1 revival of the classic Logistics Pipes. Distributed under the MMPL-1.0.1.

### Added
- Initial NeoForge 1.20.1 port of the full Forge 1.12.2 codebase
- Core pipe placement, persistence, and rendering
- Routing network (ServerRouter / ClientRouter), pathfinding, promises
- Chassis pipes (Mk1–Mk5) with hot-swappable modules
- Request pipes, provider pipes, crafter pipes, satellite pipes
- GUI rewrite on the 1.20.1 Menu/Screen API (all LP screens)
- Packet system on SimpleChannel + LPPacketPayload
- `RegisterCapabilitiesEvent` wiring for the main block entities
- Item models and crafting recipes for modules and upgrades
- Brigadier-based commands; OreDictionary → Tags migration

### Known issues (at time of release)
- JEI integration — LP's JEI plugin is ported but JEI itself cannot be loaded on NeoForge 1.20.1 due to an SRG-name remap gap; see MIGRATION.md for details
- Third-party mod integrations — BuildCraft, IC2, Thermal Dynamics, EnderStorage, IronChest, OpenComputers, The One Probe are stubbed; waiting on upstream 1.20.1 ports
- Rendering polish — traveling items render but are still being tuned
- SideConfigDisplay — not yet ported
- No world upgrade path from 1.12.2 saves — start on a fresh 1.20.1 world

## Credits

- **Krapht** — original Logistics Pipes concept
- **RS485 and the LogisticsPipes contributors** — the 1.12.2 codebase this project is built on
- **NoZeroG (VoiceLessQ)** — NeoForge 1.20.1 migration, 1.21.1 retarget, and ongoing maintenance
