# Changelog

All notable changes to this project are documented here. The format is based on
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
follows [Semantic Versioning](https://semver.org/) where practical.

## [Unreleased]

### Added
- Coloured remote orderers are back: craft an orderer with any dye. The
  orderer keeps its link, and resetting it keeps the colour.

### Fixed
- The slot finder works again: the hovered slot shows red, and clicking it
  picks that slot.
- Hitting a pipe shows particles in the pipe's own texture instead of a
  jumble of pipe parts.
- Item and power sparkles on pipes are glowing stars again instead of flat
  coloured squares.
- Filled fluid containers show the fluid's own texture in the window again.
- ItemSink and Provider module screens show item tooltips again. Shift-clicking
  an item fills the next free filter slot, and with a fuzzy upgrade, hovering a
  filter slot opens the fuzzy flag list.
- Routing channels no longer go missing when the first one was made outside
  the overworld. Channels saved in another dimension's data folder before this
  fix need to be made again.

## [0.0.3] - 2026-10-08

First release for Minecraft 1.21.1 (NeoForge 21.1). Requires Kotlin for Forge 5
or newer. Worlds from the 1.20.1 version are not migrated. There is no
MinecraftForge build and none is planned.

### Added
- Creative Power Source block: endless Forge Energy for testing power setups
  without a generator mod.
- Recipes for the RF Power Provider and RF Power Supplier Upgrade (1.12.2 only
  had them through Thermal Expansion).

### Fixed: crashes and lost items
- Client crash when joining a world saved with items inside pipes.
- Items lost enchantments, custom names and potion effects while in the
  network, and enchanted items were treated as plain ones.
- Breaking a pipe dropped nothing, and the items inside it were lost.
- Machine blocks (Power Junction, providers, crafting tables, security station
  and others) dropped nothing when mined.
- The Logistics Crafting Table refunded every ingredient on each craft.
- Remote orderer links, security card IDs, sneaky upgrade sides and disk names
  were not saved.
- Changing `chassisSlots` on an existing world crashed the server or wiped
  chassis contents.
- A dedicated server hung on `stop`.

### Fixed: recipes
- No LP recipe loaded on 1.21, and every recipe using a programmed Logistics
  Programmer was missing.
- Fluid Basic and Fluid Terminus pipes are craftable again.
- The Logistics Programmer is no longer used up in crafting, and stacks to 64.
- The reset recipe accepts several items of the same kind at once.

### Fixed: GUIs and controls
- Keyboard shortcuts work again in fourteen screens (Escape, arrows, Home, End,
  Delete, Page Up/Down, Ctrl+V), and the mouse wheel works in five popups.
- Popups receive mouse and keyboard input; Escape closes only the popup.
- Text fields take focus on click; Backspace and arrows work in the amount field.
- Check boxes toggle on the first click.
- The request table and crafting table show their output on dedicated servers.
- Request monitor "Save as Image" works again (writes `screenshots/*_tree.png`).
- Quick-sort markers show only for chests and clear when the chest closes.
- Guide book: hover marker, click sound and hover colours are back.
- The extractor module HUD shows its side again.

### Fixed: rendering
- HUD glasses panels show up in the world, with their content and the target
  crosshair. HUD glasses use their own texture and give no armour.
- High-speed tubes render without garbled textures and have their real shape
  for collision, selection and pick-block.
- Ghost preview when holding a pipe.
- Machine blocks keep their placed rotation and hide cover plates where a pipe
  connects.
- Powered and unpowered pipe textures, end caps against solid blocks after a
  world load, and the break particles from 1.12.2.
- Fluid containers show the colour of their fluid.
- Laser power particles no longer corrupt other translucent particles.

### Fixed: server, config and commands
- Config changes apply without a restart (for example `powerUsageDisabled`).
- `chassisSlots` config restored (default 1, 2, 3, 4, 8; needs a restart).
- `pipeDurability` controls break time again (default 0.25); `threadCount`
  accepts 0 for synchronous routing.
- `/logisticspipes` and `/lp` work from the server console and RCON.
- NBT sent by clients is capped at 2 MiB, like vanilla packets.
- A pipe saved without its type is removed with a warning instead of staying
  as an invisible block.
- Pipes can be placed into grass and snow, but not inside entities.
- Item names use the 1.21 translation keys; AE2 integration looks for `ae2`.

### Performance
- Routing table rebuilds are about 4x cheaper. On a 1728-router test grid,
  breaking one pipe dropped from 21 s to under 5 s of routing thread time and
  from about 300 ms to 15-25 ms on the server thread.
- A router queues one rebuild per change instead of one per tick.

### Known issues
- HUD glasses work but still have layering and readability glitches.
- No integration yet for AE2, Storage Drawers or TheOneProbe.

## [0.0.2] - 2026-06-10

Second public beta. Fixes the 0.0.1 production crash, restores the guide
book and several inert features to working order, and removes ~1,900 lines
of dead 1.12-era compatibility code. Please report issues at the project
issue tracker — this is a testing release.

### Fixed
- **Request Table rendered completely invisible.** Its block body was drawn
  by a 1.12 renderer that was never ported, and the modern pipe renderer
  skipped block-shaped pipes entirely. The table now renders with its proper
  texture, honors its placed rotation, and visually merges with connected
  pipes. Its sprite is also registered in the real blocks-atlas config
  (`assets/minecraft/atlases/blocks.json`) — the mod-namespaced duplicate of
  that file was silently ignored by the game and has been removed.
- **Middle-click pick-block on pipes returned nothing** (and logged
  `Picking on: logisticspipes:pipe gave null item`); pipes now hand back
  their actual pipe item via the Forge `getCloneItemStack` hook.
- **Client crashed rendering pipes when rejoining a world that already
  contained them** (`NullPointerException: Cannot read field "isClientSide"`
  in the block-entity renderer). The client tile entity can run `load()` more
  than once on a cold world load; each run replaced the pipe object without
  rebinding it to its container. `load()` now rebinds, and
  `MainProxy.isClient(Level)` null-guards like its `isServer` twin.
  Latent since the original port — earlier testing only ever placed pipes
  live, which initializes through a different path.
- **Guide Book was non-functional**: page content never scrolled (no `tick()`
  animation), every button click went to an empty handler (home, add/remove
  bookmark, tab switching, tab color cycling all dead), the active tab and
  tab tooltips were drawn under the opaque frame, the bookmark button's
  hitbox drifted from its rendered position, and the slider rail showed a
  tiling artifact. All ported faithfully from LP1's `drawScreen`/
  `updateScreen`/`mousePressed` flow onto the 1.20.1 `GuiGraphics`/widget
  model.
- **"Creative Tab Based Item Sink" module sank nothing on dedicated servers**
  (and showed `null` for survival players): vanilla only builds creative tab
  contents from the client creative-inventory screen. The item→tab mapping is
  now built mod-side from CATEGORY tabs only — previously the SEARCH tab
  (which aggregates every item) swallowed most lookups.
- **Five fluid pipes were uncraftable** (request, provider, satellite,
  insertion, extractor): chipped-crafting recipes and the program-compiler
  FLUID category are reinstated, based off the fluid supplier pipe.
- **Fluid-type picker GUI never opened** from fluid module slots; rewired to
  LP1's `SelectItemOutOfList` popup flow.
- Three packets (`ComponentList`, `MissingItems`,
  `RoutingUpdateAskForTarget`) referenced client classes unguarded on
  dedicated servers; orderer popup toggle (`displayPopup`) now persists; a
  `%d`-on-ResourceLocation crash in `ServerRouter.toString()` debug output.

### Changed
- **Registry access migrated to vanilla** `BuiltInRegistries`/`Registries`
  keys throughout (including `DeferredRegister.create`); zero
  `ForgeRegistries` references remain, shrinking the loader-coupled surface
  for future version ports.
- The inert `@ClientSideOnlyMethodContent` annotation (honored only by the
  deleted 1.12 coremod, i.e. fake protection) is gone; every former use now
  has a real `FMLEnvironment.dist` guard + `@OnlyIn(Dist.CLIENT)` helper.
- Guide book main-menu links point at this repository instead of upstream
  RS485 (bug reports, builds, contribution); original-creator credits remain.
- `MIGRATION.md` gained a measured "Forward-port surface (1.21+)" section
  (namespace rename, capabilities rework, `CustomPacketPayload`, events,
  fluids, toolchain).

### Removed
- **The entire 1.12-era dead-mod compat layer** (−1,866 lines): BuildCraft,
  IC2, ComputerCraft, CoFH/Thermal Expansion, Thermal Dynamics, NEI,
  IronChest, EnderStorage, EnderCore, OpenComputers and MCMultiPart stubs,
  whose dummy behavior is now constant-folded at the former call sites (no
  runtime behavior change; also removed two latent NPE paths). `PowerProxy`
  stays — it is LP's own live Forge Energy implementation.

### Fixed (0.0.1 production)
- **0.0.1 crashed on load on every production NeoForge/Forge 1.20.1 install**
  (`NoSuchFieldError: BLOCK_ENTITY_TYPE` at `LPRegistries.<clinit>`, issue #1).
  NeoGradle 7 does not reobfuscate for 1.20.1, so the published jar shipped
  Mojmap names while the production runtime uses SRG. The build now
  reobfuscates Mojmap → SRG with TinyRemapper; AutoRenamingTool was unusable
  here because it propagated MC method renames (e.g. `Container.isEmpty →
  m_7983_`) into `java.util.Deque`, crashing the mod a second way.
- **Mod would not load on a dedicated server** once past that crash —
  client-only classes leaked onto the common load path. Gated the render
  proxy (`CCLProxy` / `LPRenderStateImpl`), the client tick/chat/login event
  handlers, and render-model preloading behind `Dist.CLIENT`, and moved two
  GUI packets' client bodies into `@OnlyIn(Dist.CLIENT)` helpers. Packet IDs
  are now index-based so a packet skipped on one side can no longer desync the
  protocol. A dedicated server now reaches *Done* with zero LogisticsPipes
  errors.
- **Branch HEAD compiles again.** Commit `7e2591c38` ("gate DEBUG on
  production env") made `LogisticsPipes.DEBUG` `final` + initialiser-
  gated on `!FMLEnvironment.production`, but left the older manifest-
  based `LogisticsPipes.DEBUG = false;` assignment in place — final-
  reassignment compile error. Removed the dead line; the field-
  initialiser already handles the production check. (`ce8371362`)

### Status
- **Feature parity with LP 1.12.2 (upstream `dev`):** essentially
  complete. Only 18 Java files from LP1 `dev` are intentionally not
  ported — all 1.12.2-only legacy: ASM coremod transformers
  (`asm/*` — superseded by Mixins on 1.20.1+), MCMP / Thermal
  Dynamics integration (those mods don't exist on 1.20.1), old
  `IGuiHandler` API (`network/GuiHandler.java`), OreDictionary recipe
  conditions (replaced by Tags), and 1.12.2 API-name utilities
  (`EnumFacingUtil`, `FinalNBTTagCompound`, etc.).
- **Build verified:** `./gradlew runClient` boots cleanly, mod
  identifies as `"Logistic Pipes 2 1.0.0 (logisticspipes)"`, all 121
  pipe textures register, integrated server tick fires.

## [0.0.1] - 2026-04-16

First public beta of Logistic Pipes 2 — a NeoForge 1.20.1 revival of the
classic Logistics Pipes. Distributed under the MMPL-1.0.1.

### Added
- Initial NeoForge 1.20.1 port of the full Forge 1.12.2 codebase
- Core pipe placement, persistence, and rendering
- Routing network (`ServerRouter` / `ClientRouter`), pathfinding, promises
- Chassis pipes (Mk1–Mk5) with hot-swappable modules
- Request pipes, provider pipes, crafter pipes, satellite pipes
- GUI rewrite on the 1.20.1 Menu/Screen API (all LP screens)
- Packet system on `SimpleChannel` + `LPPacketPayload`
- `RegisterCapabilitiesEvent` wiring for the main block entities
- Item models and crafting recipes for modules and upgrades
- Brigadier-based commands; OreDictionary → Tags migration

### Known issues
- **JEI integration** — LP's JEI plugin is ported but JEI itself cannot be
  loaded on NeoForge 1.20.1 due to an SRG-name remap gap; see
  [MIGRATION.md](MIGRATION.md) for details
- **Third-party mod integrations** — BuildCraft, IC2, Thermal Dynamics,
  EnderStorage, IronChest, OpenComputers, The One Probe are stubbed;
  waiting on upstream 1.20.1 ports
- **Rendering polish** — traveling items render but are still being tuned
- **SideConfigDisplay** — not yet ported
- **No world upgrade path from 1.12.2 saves** — start on a fresh 1.20.1 world

### Credits
- **Krapht** — original Logistics Pipes concept
- **RS485 and the LogisticsPipes contributors** — the 1.12.2 codebase this
  port is built on; see
  [upstream contributors](https://github.com/RS485/LogisticsPipes/contributors)
- **NoZeroG** — NeoForge 1.20.1 migration and ongoing maintenance

[0.0.1]: https://github.com/VoiceLessQ/Logistic-Pipes-2/releases/tag/v0.0.1
