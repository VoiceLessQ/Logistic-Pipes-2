LogisticsPipes 1.12.2 — Complete Technical Analysis
Generated: 2026-04-06
Source: F:\Minecraft modding\Mod Github\LogisticsPipes
Target: F:\Minecraft modding\Projects\Fabric-Neoforge\Logistic Pipes 2
======================================================================


SCALE
------
1,014 Java files + 113 Kotlin files (two parallel codebases inside LP1)
26+ pipe types, 16+ module types, 50+ packet types, 12+ GUIs
Kotlin layer (src/main/kotlin/network/rs485/) is a partial in-progress refactor of the Java layer


======================================================================
1. TOP-LEVEL DIRECTORY STRUCTURE
======================================================================

LogisticsPipes/
  common/logisticspipes/          Main source (Java, 1014 files)
    LogisticsPipes.java           @Mod entry point
    LPItems.java                  Item registration
    LPBlocks.java                 Block registration
    api/                          Public API interfaces
    asm/                          ASM class transformers + coreloader
    blocks/                       TileEntities for non-pipe blocks
    commands/                     /lp server commands
    config/                       Configs.java (all tunable values)
    datafixer/                    NBT migration utilities
    entity/                       Custom entities
    gui/                          Legacy 1.7-style GUIs
    hud/                          HUD rendering
    interfaces/                   Core capability interfaces
    items/                        Item classes (pipes, modules, tools)
    logic/                        Logic gates/controllers
    logistics/                    LogisticsManager (item-sink/crafting core)
    logisticspipes/               Internal routing structures
    modules/                      16+ module implementations
    modplugins/                   JEI, NEI, MCMP integrations
    network/                      Packet system + GUI networking
    pipes/                        26+ pipe implementations
    pipefxhandlers/               Particle effects
    proxy/                        Mod compatibility proxies
    recipes/                      Crafting recipes, chip recipes
    renderer/                     Pipe rendering
    request/                      RequestTree + promise system
    routing/                      Router, PathFinder, route caching
    security/                     Security station permissions
    ticks/                        Tick handlers + async threading
    transport/                    LPTravelingItem + PipeTransportLogistics
    utils/                        Utility classes

  src/main/kotlin/network/rs485/logisticspipes/
    config/                       ServerConfiguration.kt, ClientConfiguration.kt
    connection/                   Adjacent.kt (adjacency detection)
    gui/                          Modern widget-based GUI framework
    logistics/                    LogisticsManager (Kotlin refactor)
    module/                       Async modules, module utilities
    property/                     Property system (declarative NBT sync)
    routing/                      AsyncRouting
    world/                        CoordinateUtils, DoubleCoordinates

  src/api/java/                   Public API
  resources/                      Assets (textures, lang files, mcmod.info)


======================================================================
2. ENTRY POINTS & INITIALIZATION
======================================================================

LogisticsPipes.java  (@Mod)
  preInit  — registers packets, initializes routing system
  init     — registers GUIs, event listeners, tick handlers, creates thread pool
  postInit — post-init tasks

SimpleServiceLocator.java — service registry pattern
  Holds: RouterManager, LogisticsManager, InventoryUtilFactory,
         ChannelConnectionManager, SecurityStationManager
  Accessed globally (static singleton pattern)

LogisticsPipesCoreLoader.java — FMLLoadingPlugin / ASM coremod
  Injects bytecode into TileEntity at load time
  Adds change-listener hooks
  DROP for 1.21.11 — use Mixins only if actually needed

Thread pool (created in init):
  RoutingTableUpdateThread  x4 (async routing calculations)
  ServerPacketBufferHandlerThread  (batches S2C packets, 50ms window)
  ClientPacketBufferHandlerThread  (batches C2S packets)
  ServerTickDispatcher.kt  (Kotlin coroutine task scheduler)


======================================================================
3. BLOCK & ITEM REGISTRATION
======================================================================

LPBlocks.java
  Registers LogisticsBlockGenericPipe — ONE block class for all pipe types
  Block delegates all logic to TileEntity's CoreRoutedPipe instance

LPItems.java
  ItemLogisticsPipe   — 26+ pipe item variants
  ItemModule          — 16+ module types
  ItemUpgrade         — pipe upgrades (Sneaky, Disconnection)
  RemoteOrderer       — remote ordering item
  LogisticsFluidContainer — portable 16,000 mB fluid container
  ItemDisk            — data/recipe storage
  ItemLogisticsProgrammer — module programming tool

StaticResolverUtil.java — scans @PipeComponent annotations, auto-registers


======================================================================
4. PIPE CLASS HIERARCHY
======================================================================

TileEntity
  LogisticsTileGenericPipe  (ITickable, IOCTile, ILPPipeTile, IPipeInformationProvider)
    CoreUnroutedPipe
      CoreRoutedPipe  ← abstract, all routed logic lives here
        PipeItemsBasicLogistics
        PipeItemsProviderLogistics          implements IProvideItems
        PipeItemsRequestLogistics           implements IRequestItems
          PipeItemsRequestLogisticsMk2
            PipeBlockRequestTable           block form with GUI
        PipeItemsCraftingLogistics          implements ICraftItems
        PipeItemsSupplierLogistics          active puller
        PipeItemsInvSysConnector
        PipeItemsRemoteOrdererLogistics
        PipeItemsSatelliteLogistics         channel-based remote ordering
        PipeItemsFluidSupplier
        PipeItemsSystemEntranceLogistics
        PipeItemsSystemDestinationLogistics
        PipeItemsFirewall                   filter/block routing
        FluidRoutedPipe                     extends CoreRoutedPipe
          PipeFluidBasic
          PipeFluidProvider
          PipeFluidRequestLogistics
          PipeFluidSatellite
          PipeFluidSupplierMk2
          PipeFluidInsertion
          PipeFluidExtractor
        PipeLogisticsChassis                abstract chassis base
          PipeLogisticsChassisMk1  (1 slot)
          PipeLogisticsChassisMk2  (2 slots)
          PipeLogisticsChassisMk3  (3 slots)
          PipeLogisticsChassisMk4  (4 slots)
          PipeLogisticsChassisMk5  (8 slots)

CoreMultiBlockPipe  (extends CoreUnroutedPipe)
  HSTubeLine, HSTubeCurve, HSTubeGain, HSTubeSpeedup, HSTubeSCurve


======================================================================
5. TILE ENTITY / BLOCK ENTITY SYSTEM
======================================================================

LogisticsTileGenericPipe fields:
  CoreRoutedPipe pipe            — the pipe logic object
  PipeRenderState renderState    — connection state for rendering
  LPTravelingItem[] items        — items currently in transit
  Map<UUID, ItemRoutingInformation> routingInfo  — destination info
  ServerRouter router            — routing node

NBT structure:
  pipe            ItemStack (pipe item type)
  pipeType        String (registry name)
  routerId        UUID
  upgradeManager  NBT list
  moduleInventory ItemIdentifierInventory
  items           NBT list of LPTravelingItem
  [custom pipe data]
    throttleTime
    signs[6]
    ordersInfo[]
    [module-specific data]

Tick logic (every tick, server-side):
  1. initializePipe() if first tick
  2. pipe.doWork()
       a. Process incoming items (tryInsertItem)
       b. Process routing requests
       c. Execute module ticks
       d. Process outgoing queue (_sendQueue)
  3. recheckConnections() every 30 ticks
  4. Sync dirty state to clients

Connection management:
  Adjacent.kt — DynamicAdjacent, SingleAdjacent, NoAdjacent
  Rechecked every 600 ticks (LOGISTICS_DETECTION_FREQUENCY)
  Connection types: INVENTORY, PIPE, SPECIAL_PIPE, SPECIAL_INVENTORY


======================================================================
6. ROUTING SYSTEM
======================================================================

Architecture: OSPF-style Link-State Advertising + Dijkstra

ServerRouter.java — one node per pipe in the network
  UUID id
  int simpleID          (faster int-based ID for arrays)
  CoreRoutedPipe pipe
  LSA _myLsa            (Link-State Advertisement)
  TreeSet<ItemIdentifier> interests
  Map<ItemIdentifier, List<ExitRoute>> routingTable  (cached)
  ReentrantReadWriteLock routingTableUpdateLock

  [global static data]
  SharedLSADatabase[]                 all routers' LSAs
  globalSpecificInterests<ItemIdentifier, TreeSet<ServerRouter>>
  genericInterests                    routers that accept any item

ClientRouter.java — client-side mirror (simplified, for rendering/HUD)
  Synced via SetValuePacket

IRouter.java — interface
  UUID getId()
  int getSimpleID()
  CoreRoutedPipe getPipe()
  List<ExitRoute> getRoutes(ItemIdentifier)
  List<ExitRoute> getRoutes(ItemIdentifier, IPipeServiceProvider)

ExitRoute.java
  IRouter destination
  int priority
  EnumFacing exitOrientation
  List<IFilter> filters
  Set<PipeRoutingConnectionType> connectionType
  int distanceMetric

PathFinder.java — BFS traversal
  paintAndgetConnectedRoutingPipes(
    startPipe, startOrientation, maxVisited=100, maxLength=50, ...
  ) -> Map<CoreRoutedPipe, ExitRoute>

  Algorithm:
    BFS from starting pipe
    Track visited via coordinate blacklist
    When routed pipe found: record as ExitRoute
    maxVisited + maxLength guards prevent infinite recursion

Routing table update process:
  1. RoutingTableUpdateThread calls updateRouting() periodically
  2. Dijkstra computes shortest paths to all destinations
  3. Routes cached by ItemIdentifier (different items can route differently)
  4. Lock-free reads, write-locked on change
  5. 4 threads, each handles subset of routers

Performance hotspot — LogisticsManager.determineItemDestination():
  1. Get source router
  2. Get valid destinations for item type (from globalSpecificInterests)
  3. canSink()? for each candidate
  4. Pick highest priority destination
  5. Send item
  Called for every routed item — must be fast (uses route cache)


======================================================================
7. REQUEST SYSTEM
======================================================================

RequestTree.java — recursive tree
  IResource requestType             (ItemResource / DictResource / FluidResource)
  RequestTree parent
  EnumSet<ActiveRequestType> flags
    Provide       — search existing items
    Craft         — trigger crafting pipes
    AcceptPartial — accept less than requested
    SimulateOnly  — dry run
    LogMissing    — log unfulfilled parts
    LogUsed       — log fulfilled parts
  List<IPromise> promises
  List<RequestTree> children        (sub-requests, recursive)

RequestHandler.java — entry point
  handleRequest(IResource, IRouter, IAdditionalTargetInformation)
    -> LinkedLogisticsOrderList

IPromise.java
  ItemIdentifier getItemType()
  int getAmount()
  IProvide getProvider()
  void fulfill()

Resource types:
  ItemResource    — exact item match
  DictResource    — OreDict / Tag-based fuzzy match
  FluidResource   — fluid type

Request flow:
  1. Requester calls RequestHandler.handleRequest()
  2. Tree searches IProvide + ICraft interfaces
  3. Builds promise graph (recursive for crafting dependencies)
  4. RequestTree.fullFill() executes promises in dependency order
  5. Items routed via transport system
  6. RequestLog tracks results

Recursive crafting:
  Crafter needs ingredient → creates sub-RequestTree
  Sub-tree resolved first, memoized
  Depth tracking prevents infinite recursion
  One recipe can satisfy multiple requests via extras


======================================================================
8. TRANSPORT SYSTEM
======================================================================

LPTravelingItemServer fields:
  int id                          unique per-world ID
  ItemIdentifierStack item
  EnumFacing input, output        entry/exit sides of pipe
  float position                  0.0 = start, 1.0 = end
  float speed                     blocks/tick
  TileEntity container            current pipe
  IRouter destination
  ItemRoutingInformation routingInfo

LPTravelingItemClient — client side
  Simplified, interpolated for rendering
  Receives position/speed updates via packet

PipeTransportLogistics.java
  Map<Integer, LPTravelingItemServer> items  (in-pipe items)
  Queue<Triplet<IRoutedItem, EnumFacing, ItemSendMode>> outgoing

  updateEntity() each tick:
    1. move existing items (position += speed)
    2. if position >= 1.0: hand off to next pipe
    3. if blocked: back up, attempt reroute
    4. if no reroute: wait in place
    5. process outgoing queue

Speeds:
  Base: 0.01 b/tick
  LOGISTICS_ROUTED_SPEED_MULTIPLIER = 20 -> 0.2 b/s
  LOGISTICS_DEFAULTROUTED_SPEED_MULTIPLIER = 10 -> 0.1 b/s

Client interpolation:
  Server sends start pos + end pos + speed
  Client interpolates smoothly each frame
  No per-frame server packets

Reliability:
  IRequireReliableTransport — items wait, never drop
  UUID-tracked per item
  Stored in serverList for persistence across ticks

Fluid transport (PipeFluidTransportLogistics):
  FluidStack-based (not ItemStack)
  mB/tick flow rate
  Pressure-based priority (higher priority = faster drain)


======================================================================
9. MODULE SYSTEM
======================================================================

LogisticsModule (abstract base)
  IWorldProvider _world
  IPipeServiceProvider _service     (pipe interface: power, routing, inventory)
  ModulePositionType slot           IN_PIPE, IN_HAND, IN_ITEM
  int positionInt                   slot number in chassis

  abstract: getLPName(), tick(), hasGenericInterests(),
            sinksItem(), collectSpecificInterests()

16 module implementations:

  ModuleProvider            IProvideItems     advertise + extract items
  ModuleItemSink            ISinkItems        filter-based acceptance
  ModuleCrafter             ICraftItems       triggers crafting
  ModuleActiveSupplier      IRequestItems     actively pulls on timer
  ModulePassiveSupplier     IRequestItems     pulls when requested
  ModuleSatellite           channel-based     remote endpoint
  ModuleTerminus            direct delivery   final destination
  ModuleFirewall            filter/block      routing control
  ModuleOreDictItemSink     ISinkItems        tag/oredict matching
  ModulePolymorphicItemSink ISinkItems        NBT-variation matching
  ModuleEnchantmentSink     ISinkItems        enchanted items only
  ModuleEnchantmentSinkMk2  ISinkItems        enhanced enchantment sink
  ModuleCreativeTabBasedItemSink  ISinkItems  creative tab filter
  ModuleModBasedItemSink    ISinkItems        filter by mod source
  ModuleFluidSupplier       IRequestFluid     fluid puller
  ChassisModule             container         holds other modules (Mk1-5)

Module slot positions:
  IN_PIPE   slots 0-8  (attached to pipe)
  IN_HAND   slot 9     (held by player with configurator)
  IN_ITEM   slot 10    (stored on disk/card)

Module init sequence:
  1. Module created when pipe loads
  2. registerHandler(IWorldProvider, IPipeServiceProvider)
  3. registerPosition(ModulePositionType, int)
  4. readFromNBT()
  5. tick() called every pipe tick

Properties system (Kotlin PropertyHolder.kt):
  Declarative NBT syncing
  Reactive bindings to GUI
  Handles all module config serialization


======================================================================
10. CHASSIS SYSTEM
======================================================================

PipeLogisticsChassis (abstract)
  ChassisModule _module             container for all slots
  ItemIdentifierInventory _moduleInventory   filter items
  getChassisSize()                  1/2/3/4/8 for Mk1-5
  getPointedOrientation()           which face to look at
  getAvailableAdjacent()            connection to adjacent inventory

Mk1 (1 slot), Mk2 (2), Mk3 (3), Mk4 (4), Mk5 (8)

Each slot:
  Holds one LogisticsModule instance
  Has its own ItemIdentifierInventory for filters
  Receives tick() from chassis each tick
  Can act as provider OR sink depending on installed module

Chassis implements ICraftItems + IBufferItems
Request handling: iterates all module slots, delegates to each module

Item buffering:
  Internal inventory for queued items
  FIFO queue when pipe is blocked
  Respects module priorities


======================================================================
11. SATELLITE SYSTEM
======================================================================

Concept:
  [Request Pipe] -> [Satellite Receiver] <-- channel ID --> [Satellite Sender] <- [Provider]

PipeItemsSatelliteLogistics — implements IRequestItems
  channel ID (long)
  On request: looks up IChannelManager, finds connected satellite
  Routes through network to satellite sender

PipeFluidSatellite — same for fluids

ModuleSatellite — module form (installed in chassis)

IChannelManager — global registry: channel ID -> satellite pipe
IChannelRoutingConnection — routing between satellites

Operation:
  1. Receiver queries channel manager
  2. Gets list of all satellites on same channel
  3. Treats as normal router nodes
  4. Routes items through standard algorithm
  5. Virtual routing link created per-channel


======================================================================
12. NETWORK / PACKET SYSTEM
======================================================================

PacketHandler.java — central registration
  NetworkRegistry.newSimpleChannel("LOGISTICSPIPES")

Base class: ModernPacket (abstract)
  CoordinatesPacket   (includes BlockPos)
  GuiPacket           (GUI interactions)
  ListSyncPacket      (item list sync)
  RequestPacket       (request messages)
  50+ specific implementations

Key packets:
  PipeTileStatePacket         pipe render/connection state
  ChassisPipeModuleContent    module inventory sync
  SetValuePacket              generic key-value property sync
  SendQueueContent            item queue sync
  StatUpdate                  pipe statistics
  ParticleFX                  visual effects
  SecurityUpdate              security station sync

Serialization:
  LPDataInput / LPDataOutput — binary protocol
  Compact (more efficient than JSON/NBT for network)
  Custom handlers per type: writeItemIdentifier, readItemIdentifier, etc.

Packet batching:
  ServerPacketBufferHandlerThread — waits 50ms, sends in bulk
  Reduces TCP overhead significantly on busy networks

Client->Server:
  GUI button clicks, configuration changes, request for status

Server->Client:
  Pipe connection state, item positions, module inventory, statistics


======================================================================
13. GUI SYSTEM
======================================================================

Two systems coexist in LP1:

Legacy Java GUIs (logisticspipes/gui/)
  1.7-style GuiContainer extensions
  Direct slot/button handling

Modern Kotlin GUIs (src/main/kotlin/.../gui/)
  Widget-based framework
  Declarative DSL layout
  Reactive property bindings
  LPFontRenderer.kt — custom BDF font rendering

Widget types:
  LPGuiButton, TextButton, Label, VerticalLabel
  SlotGroup, GhostSlots (JEI-style drag)
  FuzzySelectionWidget, LockedSlot, WidgetContainer

Pipe GUIs:
  ChassisGuiProvider        chassis module management
  InvSysConGuiProvider      inventory system connector
  PipeController            main pipe interface

Module GUIs (12+ total):
  ItemSinkGui               filter configuration
  ProviderGui               advertised items
  CrafterGui                recipe setup
  ActiveSupplierGui         pull config
  PassiveSupplierGui        request config
  SatelliteGui              channel config
  FirewallGui               filter rules

Block GUIs:
  SecurityStationGui        player access levels
  AutoCraftingGui           crafting table setup
  PowerJunctionGui          power monitoring
  StatisticsGui             network statistics
  ProgramCompilerGui        chip programmer

GuideBook.kt — in-game handbook, Markdown content, custom font renderer


======================================================================
14. POWER SYSTEM
======================================================================

Power providers:
  LogisticsIC2PowerProviderTileEntity   EU from IC2  [DROP — no 1.21 IC2]
  LogisticsRFPowerProviderTileEntity    RF/FE        [KEEP — still relevant]
  LogisticsPowerJunctionTileEntity      balancer/router block

PowerSupplierHandler (per-pipe):
  double internalPower
  addEnergy(double), getEnergy(), useEnergy(double)

Cost per operation:
  Item routing: scales by hop count
  Crafting operation: 250 EU (LOGISTICS_CRAFTING_TABLE_POWER_USAGE)
  Router maintenance: minimal per tick

Config:
  LOGISTICS_POWER_USAGE_DISABLED = false
  POWER_USAGE_MULTIPLIER = 1.0

For LP2: FE (Forge Energy) on NeoForge, Fabric Energy API on Fabric
  Use IPlatformHelper.findEnergyProvider() abstraction


======================================================================
15. FLUID SYSTEM
======================================================================

FluidRoutedPipe (extends CoreRoutedPipe)
  Base for all 6 fluid pipe types

Pipe types:
  PipeFluidBasic            unfiltered fluid routing
  PipeFluidProvider         advertises adjacent fluids
  PipeFluidRequestLogistics requests specific fluids
  PipeFluidSatellite        channel-based fluid routing
  PipeFluidSupplierMk2      active fluid puller
  PipeFluidInsertion        accepts fluid from adjacent
  PipeFluidExtractor        extracts + routes fluid

PipeFluidTransportLogistics:
  FluidStack-based transport (not ItemStack)
  mB/tick flow rate
  Pressure-based priority
  Merge/split at junctions

LogisticsFluidContainer:
  Portable item holding one fluid type
  16,000 mB capacity
  Used with PipeItemsFluidSupplier

Routing:
  Destination found by fluid type (like items)
  Pressure-based priority
  Backpressure handling
  Same router infrastructure as items


======================================================================
16. ORDERER & REMOTE ORDERER
======================================================================

ItemPipeManager.java — player orderer item
  Open GUI to view/request items from network
  Shows full network inventory

RemoteOrderer.java — item
  Extended range (configurable)
  Position-targeted
  Opens order GUI

Order GUI:
  Item display + search
  Amount selector
  Destination picker
  Priority selector

Order tracking:
  LogisticsOrder — item, amount, destination, status
  LogisticsItemOrderManager — per-pipe order tracking
  LogisticsOrderManager — global order tracking


======================================================================
17. SECURITY SYSTEM
======================================================================

LogisticsSecurityTileEntity
  Stores: Player UUID -> SecurityLevel
  SecurityLevel enum: ADMIN, USER, GUEST, BLOCKED

Pipes check security on:
  Extract operations
  Send operations
  Configuration changes

CCSecurityCheck.java — CC:Tweaked integration
  Verifies player UUID against security station
  Throws PermissionException if denied


======================================================================
18. INTEGRATIONS — KEEP vs DROP
======================================================================

DROP (no 1.21 version or replaced):
  BuildCraft pipe API       — LP2 has own system
  IC2 (EU power)            — no 1.21 port
  NEI                       — replaced by JEI
  MCMultiPart               — no 1.21 port
  ASM CoreMod               — use Mixins if needed
  EnderCore                 — no 1.21 port
  CoFH Core legacy          — replaced by modern FE

KEEP / PORT:
  JEI                       — has 1.21 port, widely used
  TheOneProbe               — has 1.21 port
  RF/FE power               — still relevant on NeoForge

OPTIONAL:
  AE2                       — has 1.21 port, useful integration
  CC:Tweaked                — has 1.21 port
  Storage Drawers           — has 1.21 port
  Thermal mods              — check for Fabric ports


======================================================================
19. UTILITY CLASSES WORTH PORTING
======================================================================

ItemIdentifier.java         — immutable item type with NBT hash
ItemIdentifierStack.java    — item + count
ItemIdentifierInventory.java — ItemIdentifier-based inventory
FluidIdentifier.java        — fluid type identifier
FluidIdentifierStack.java   — fluid + amount
DoubleCoordinates.kt        — double-precision position type
TileBuffer.java             — cached TileEntity lookups
OrientationsUtil.java       — EnumFacing/Direction utilities

Data structures:
  ExitRoute.java              route destination info
  ItemRoutingInformation.java routing metadata
  SinkReply.java              module response to item query
  LSA.java                    link-state advertisement
  Pair.java, Triplet.java     immutable tuples
  LPItemList.java             item tracking list

Serialization:
  LPDataInput/LPDataOutput    binary packet protocol


======================================================================
20. CONFIG VALUES
======================================================================

PathFinder limits:
  LOGISTICS_DETECTION_LENGTH = 50       max BFS depth
  LOGISTICS_DETECTION_COUNT = 100       max nodes visited
  LOGISTICS_DETECTION_FREQUENCY = 600   ticks between connection rechecks

Speed:
  LOGISTICS_ROUTED_SPEED_MULTIPLIER = 20
  LOGISTICS_DEFAULTROUTED_SPEED_MULTIPLIER = 10

Crafting:
  LOGISTICS_CRAFTING_TABLE_POWER_USAGE = 250

Performance:
  MULTI_THREAD_NUMBER = 4               routing thread count
  MINIMUM_INVENTORY_SLOT_ACCESS_PER_TICK = 10
  MAXIMUM_INVENTORY_SLOT_ACCESS_PER_TICK = 0
  DISABLE_ASYNC_WORK = false

Display:
  LOGISTICS_HUD_RENDER_DISTANCE = 15
  ENABLE_PARTICLE_FX = true
  OPAQUE = false

Chassis:
  CHASSIS_SLOTS_ARRAY = {1, 2, 3, 4, 8}  Mk1-5 slot counts

Power:
  POWER_USAGE_MULTIPLIER = 1.0
  LOGISTICS_POWER_USAGE_DISABLED = false

For LP2: use Architectury config or a common YAML-backed system


======================================================================
LP2 CURRENT STATUS vs LP1 FEATURE COMPLETENESS
======================================================================

DONE:
  Pipe block + BlockEntity system
  All 26+ pipe types (class hierarchy)
  Routing — Dijkstra, LSA, BFS PathFinder, ServerRouter, RouterManager
  Transport — LPTravelingItem, PipeTransportLogistics
  All 16+ module types
  Chassis Mk1-5
  Request + crafting system (RequestTree, RequestHandler, IResource)
  Networking — all 50+ packets via Architectury NetworkManager
  GUIs — request pipe, chassis, supplier, creative tab (Phase 6)
  Registry — LPBlocks, LPItems, LPMenuTypes, LPCreativeTab

NOT STARTED / INCOMPLETE:
  Power system (FE on NeoForge, Fabric Energy on Fabric)
  Fluid pipes (6 types + PipeFluidTransportLogistics)
  Config system (expose tunable values)
  Remaining GUIs (crafting, provider, item-sink, satellite, firewall, orderer, security)
  JEI plugin (recipe viewer + ghost ingredients)
  TheOneProbe provider
  Client-side item interpolation (smooth travel rendering)
  Statistics tracking (items sent/received per pipe)
  Security station full implementation
  Remote orderer full implementation
