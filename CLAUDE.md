This is a port of LogisticsPipes (originally 1.12.2) to Minecraft 1.21.11, targeting both Fabric and NeoForge via Architectury.

--- Current state ---
build: PASSING (common + fabric + neoforge all compile clean)
completed: all CRITICAL, all HIGH, all MEDIUM items done — see REMAINING_WORK.md
remaining: LOW cosmetic items (texture indices, IGuiTileEntity generics, IRouteLayer stubs)

--- Source references ---
porting source: F:\Minecraft modding\Mod Github\LogisticsPipes
  purpose: reference only — original LP 1.12.2 source being ported, do not modify
  read from it only when porting or comparing implementations
  key path inside: common\logisticspipes\ and common\logisticspipes\request\resources\

local APIs: F:\Minecraft modding\Modding API's
  architectury-api\
    path: F:\Minecraft modding\Modding API's\architectury-api
    version: 6.6 (1.19.2) — pattern reference only, version mismatch
    if-version-mismatch: clone https://github.com/architectury/architectury-api tag 19.0.1+1.21.11
  fabric-api 1.21.11\
    path: F:\Minecraft modding\Modding API's\fabric-api 1.21.11
    version: 0.142.0 for MC 26.1 (1.21.x) — valid reference for Transfer API, lifecycle events
  NeoForge\
    path: F:\Minecraft modding\Modding API's\NeoForge
    use for: Transfer API / capability patterns
  NOTE: build dependencies are fetched from Maven automatically; local copies are for reading only

--- Build ---
mod loader: Fabric 0.18.4 + NeoForge 21.11.38-beta (dual-platform via Architectury)
architectury API: 19.0.1
fabric API: 0.139.5+1.21.11
minecraft: 1.21.11
mappings: official Mojang mappings (loom.officialMojangMappings()) — not Yarn
java: 21
gradle: 8.14 (architectury-loom 1.13-SNAPSHOT)
mod ID: morph
group: com.Morph
version: 1.0.0

--- Project structure ---
common/   — all LP2 logic, shared between platforms; no net.fabricmc or net.neoforged imports allowed here
fabric/   — Fabric entry points and FabricPlatformHelper
neoforge/ — NeoForge entry points and NeoForgePlatformHelper

common/src/main/java/com/Morph/ExampleMod.java              — common init entry (called by both platforms)
common/src/main/java/com/Morph/logisticspipes/              — all LP2 logic
common/src/main/resources/morph.mixins.json                 — mixin config (currently no active mixins)
common/src/main/resources/logisticspipes2.accesswidener     — access widener (currently empty)

fabric/src/main/java/com/Morph/fabric/ExampleModFabric.java        — Fabric ModInitializer, sets FabricPlatformHelper
fabric/src/main/java/com/Morph/fabric/ExampleModFabricClient.java  — Fabric client init
fabric/src/main/java/com/Morph/fabric/FabricPlatformHelper.java    — Fabric impl of IPlatformHelper
fabric/src/main/resources/fabric.mod.json

neoforge/src/main/java/com/Morph/neoforge/ExampleModNeoForge.java       — NeoForge @Mod, sets NeoForgePlatformHelper
neoforge/src/main/java/com/Morph/neoforge/ExampleModNeoForgeClient.java — NeoForge client init
neoforge/src/main/java/com/Morph/neoforge/NeoForgePlatformHelper.java   — NeoForge impl of IPlatformHelper
neoforge/src/main/resources/META-INF/neoforge.mods.toml

--- Common init sequence (ExampleMod.init()) ---
LPBlocks.register()
LPItems.register()
LPMenuTypes.register()
LPCreativeTab.register()
LPNetworking.register()
LPTickHandler.register()

--- Platform abstraction ---
common/src/main/java/com/Morph/logisticspipes/platform/IPlatformHelper.java
  insertItem(level, pos, side, stack, simulate)          — default overload omits simulate (false)
  extractItem(level, pos, side, template, maxCount, simulate) — default overload omits simulate (false)
  findEnergyProvider, getFluidContents, extractFluid, insertFluid

FabricPlatformHelper  — Fabric Transfer API: ItemStorage.SIDED, Transaction (abort = simulate)
NeoForgePlatformHelper — NeoForge Transfer API: Capabilities.Item.BLOCK, Transaction (abort = simulate)

--- Key common packages ---
pipes/basic/       — CoreUnroutedPipe, CoreRoutedPipe, LogisticsPipeBlockEntity
pipes/             — all pipe types (request Mk1-3, provider, supplier, crafter Mk1-3, chassis Mk1-5, satellite, firewall, fluid, remote orderer, quicksort, system entrance/destination)
modules/           — ModuleProvider, ModuleCrafter, ModuleItemSink, ModuleActiveSupplier, ModulePassiveSupplier, ModuleSatellite, ModuleExtractor, ModuleTerminus, ModuleFirewall, ModuleOreDictItemSink, ModulePolymorphicItemSink
routing/           — ServerRouter, ClientRouter, IRouter, PathFinder, ExitRoute, RouterManager
routing/order/     — LogisticsOrderManager, LogisticsItemOrderManager (watcher system implemented)
request/           — RequestTree, RequestHandler, RequestLog
request/resources/ — IResource, ItemResource, DictResource (fuzzy matching — HIGH priority TODO)
network/           — LPNetworking + all S2C/C2S packet records
transport/         — PipeTransportLogistics, LPTravelingItem

--- Networking (all via Architectury NetworkManager) ---
S2C: PacketPipeSyncS2C, PacketModuleInvS2C, PacketNetworkItemsS2C, PacketOrdererContentS2C,
     PacketMissingItemsS2C, PacketComponentListS2C, PacketPipeStateS2C, PacketChassisSlotsS2C,
     PacketSatelliteNamesS2C, PacketCraftingPipeUpdateS2C, PacketPipePositionS2C,
     PacketPipeManagerContentS2C, PacketSecurityStationS2C
C2S: PacketModuleInvRequestC2S, PacketRequestItemC2S, PacketSubmitRequestC2S, PacketOpenGuiC2S,
     PacketModulePropertiesC2S, PacketItemSinkImportC2S, PacketSupplierModeC2S,
     PacketSatelliteSetNameC2S, PacketFirewallFlagsC2S, PacketSecurityLevelC2S, PacketSecurityFriendC2S

--- Routing architecture ---
ServerRouter uses AtomicReference<List<List<ExitRoute>>> for lock-free route table reads.
Fallback table held during rebuild so in-transit items survive topology changes.
Routes computed once via Dijkstra, cached, atomically swapped — no per-tick pathfinding.
globalSpecificInterests populated via ServerRouter.updateInterests() on each router update.
getNetworkItems() walks reachable providers via getIRoutersByCost() and calls getAllItems().

--- Constraints ---
No markdown headers in any output or generated files.
Do not run gradle builds to verify changes.
Do not ask for confirmation before proceeding — execute directly.
Common code must not import net.fabricmc or net.neoforged classes.
Use Architectury NetworkManager for all networking in common code.
Mojang mappings are used — not Yarn.
No per-tick pathfinding; no per-tick routing table recalculation.
Keep changes minimal — do not refactor surrounding code, add docstrings, or add comments to unchanged lines.
