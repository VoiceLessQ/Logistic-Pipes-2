# Logistic Pipes 2

A revival of [Logistics Pipes](https://github.com/RS485/LogisticsPipes), ported from Forge 1.12.2 to **NeoForge 1.20.1** — bringing the classic request-based item routing, automated crafting, and modular chassis pipes back to modern Minecraft.

See the original [CurseForge page](https://www.curseforge.com/minecraft/mc-mods/logistics-pipes) for background on what the mod has been since its 1.4.x days.

> **Beta / work in progress.** Expect bugs, crashes, missing polish, and the occasional broken feature. Back up your worlds before testing, and please open an issue if something goes wrong — noisy reports are more useful than silent frustration.

## Fabric 26.2 port (this branch)

This branch is a port of Logistic Pipes 2 from NeoForge 1.20.1 to **Fabric, Minecraft 26.2** (Fabric Loader 0.20.1, Fabric API 0.161.0+26.2, Java 25).

**Required:** Fabric API, Fabric Language Kotlin 1.14.2+. Team Reborn Energy is bundled.

**Integrations:** Tech Reborn and BuildCraft Refabricated (power through Team Reborn Energy, EU power supplier upgrades LV–EV, EU power provider), IC2 Reborn (pulls EU from adjacent generators), CC:Tweaked peripherals.

**Status:** beta. Playable in singleplayer and on a dedicated server: routing, requesting, chassis modules, crafting, GUIs, HUD glasses, persistence. Remaining gaps are listed in `FABRIC-STUBS.md`; AE2 and EnderStorage have no Fabric 26.x builds and stay disabled.

Build: `./gradlew build` (JDK 25+), jar in `build/libs/`.

## Forge or NeoForge?

**Both — on this Minecraft version they are the same platform.** NeoForge's 1.20.1 release was its first after forking from Forge and is fully Forge-compatible (it even ships under the `forge` id). The released jar runs unchanged on:

- **Forge 1.20.1** — 47.2.x / 47.3.x from [files.minecraftforge.net](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.20.1.html)
- **NeoForge 1.20.1** — 47.1.x, e.g. the [1.20.1-47.1.106 installer](https://maven.neoforged.net/#/releases/net/neoforged/forge/1.20.1-47.1.106)

The split only matters from Minecraft **1.20.2 onward**, where NeoForge changed its internals — this jar will not run there (you get a clean "requires Minecraft 1.20.1" screen instead). The planned 1.21.x version of this mod targets NeoForge proper.

**Required dependency:** [Kotlin for Forge](https://www.curseforge.com/minecraft/mc-mods/kotlin-for-forge) **4.x** (the 1.20.1 file) — the mod will not load without it.

## What is Logistic Pipes

A logistics mod that lets you build pipe networks capable of requesting items on demand, automatically sorting inventory, and triggering crafting chains. Pipes connect to any inventory and route items intelligently based on your configured rules — no constant item flow, no lost items, just point-to-point delivery driven by what you ask for.

<!-- TODO: add a screenshot or short GIF of a network in action -->

## Status

Work in progress, but **feature-complete vs LP 1.12.2 upstream**. The migration ported the full 1014-file LP1 `dev` source to NeoForge 1.20.1; only 18 1.12.2-only legacy files (ASM coremods, OreDict recipe conditions, old `IGuiHandler` API, naming-collision utility classes) are intentionally not ported — each of those is either superseded by a modern equivalent (Mixins instead of coremod ASM, Tags instead of OreDict) or removed because the integrated mod doesn't exist on 1.20.1.

Core gameplay (placing pipes, routing, requesting, chassis modules, crafting) is fully functional. Polish and third-party integrations are the main outstanding work. See [MIGRATION.md](MIGRATION.md) for a detailed checkpoint and [CHANGELOG.md](CHANGELOG.md) for recent changes.

## Known Limitations

- **Some mod integrations missing**: JEI, Applied Energistics 2, CC: Tweaked, Storage Drawers and The One Probe work. BuildCraft, IndustrialCraft 2, Thermal and NEI support is not ported; most of those mods have no modern release.
- **Items-in-transit rendering** — visible but still being polished.
- **No world upgrade path from 1.12.2 saves** — legacy data fixers are not ported; start on a 1.20.1 world.

## Planned

- Fabric support via [Architectury](https://docs.architectury.dev/) (after the NeoForge 1.20.1 port reaches feature parity)
- Published API jar so addons can be built against a stable surface
- Performance improvements (render caching, dirty-flag system)

## Versions

- Minecraft **1.20.1** (only — see "Forge or NeoForge?" above)
- Forge **47.2+** / NeoForge **47.1.x** (built against 47.1.88)
- Kotlin for Forge **4.x** (runtime dependency for players)
- Java **17** (provisioned automatically via Gradle toolchains)
- Kotlin **1.9.10**

## Building

```bash
./gradlew build         # full build; output in build/libs/
./gradlew runClient     # launch a dev client
./gradlew runServer     # launch a dev server
./gradlew check         # unit tests
```

Requires `git-lfs` for a small number of LFS-tracked JARs. If you cloned without it:

```bash
git lfs install && git lfs fetch && git lfs checkout
```

## Contributing

Issues and PRs welcome. Skim [MIGRATION.md](MIGRATION.md) and the open issues for current focus areas before starting work.

## Credits

- **Krapht** — original concept and early codebase
- **[RS485 and LogisticsPipes contributors](https://github.com/RS485/LogisticsPipes/contributors)** — the 1.12.2 codebase this project is built on
- **NoZeroG** — NeoForge 1.20.1 port and ongoing maintenance

## Changelog

See [CHANGELOG.md](CHANGELOG.md).

## License

Distributed under the Minecraft Mod Public License 1.0.1 — see [LICENSE.md](LICENSE.md).
