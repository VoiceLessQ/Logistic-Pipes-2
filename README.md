# Logistic Pipes 2 (Morph)

Port of [LogisticsPipes](https://github.com/RS485/LogisticsPipes) (originally 1.12.2) to **Minecraft 1.21.1**.

Mod ID: `morph` &nbsp;·&nbsp; Group: `com.Morph` &nbsp;·&nbsp; License: see upstream LP1

## Supported loaders

| Loader | Support |
|---|---|
| **NeoForge 21.1.x** (1.21.1) | ✅ primary target |
| **Fabric 0.18.x** (1.21.1, via Architectury) | ✅ supported (external Fabric energy import via Team Reborn Energy still stubbed) |
| **Forge** (any version) | ❌ **not supported** |

### Why no Forge?

NeoForge forked from Forge during the MC 1.20.1 cycle and the two have diverged in API, mappings, and capability handling. Supporting both would mean maintaining two platform-helper layers, two energy-cap bridges, and two registry styles for what amounts to the same engine — a cost we're not paying. The MC modding community on current versions has consolidated heavily on NeoForge, and Fabric (which we already support via Architectury) covers the rest.

### Why not MC 1.20.1?

NeoForge dropped 1.20.1 — the `20.1.x` line was unpublished from their Maven and is no longer fetchable for fresh builds. The earliest MC where NeoForge is alive and supported is the 1.20.4+ range; 1.21.1 is the version this project targets and is the lowest version we intend to backport to.

A legacy `0.0.1` release jar exists on the `1.20.1` branch of this repo — it was built against the now-unpublished NeoForge 20.1.88 and **does not load on current Forge or NeoForge runtimes** (GitHub issue #1). That release is deprecated; use the 1.21.1 build instead.

## Status

Active development. See [PROJECT_STATE.md](PROJECT_STATE.md) for the directory layout, what's been stripped from earlier attempts and where it lives, and the canonical branch (`mc-1.21.1` locally, `1.21.1` on GitHub).

Implemented: routing engine (Dijkstra/BFS/LSA), request/crafting system, all core pipe types, modules, chassis Mk1-5, BESR pipe rendering, Power Junction (block + GUI + NeoForge `IEnergyStorage` cap), JSON config.

In progress / TODO: per-pipe power gates, remaining GUIs, Fabric Team Reborn Energy bridge, JEI/TheOneProbe integration. See REMAINING_WORK locally for the full checklist.

## Building

```
./gradlew build
```

Artifacts land in `fabric/build/libs/` and `neoforge/build/libs/`.

## Acknowledgements

Original LogisticsPipes by the [RS485 team](https://github.com/RS485/LogisticsPipes) — all the design and most of the algorithms are theirs. This is a re-port to the modern MC platform.
