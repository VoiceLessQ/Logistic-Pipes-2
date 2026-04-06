# The Six Layers

### 1. Transport Layer (Unrouted Pipes)

**Role**: The dumb highways connecting the smart nodes.

- **Cache (Next-Hop)**: Only caches the valid exit directions.
- **Alt Destination**: If straight ahead is blocked, cache the available side routes.
- **Transfer Cache**: Visual/Timer payload only (no pathfinding logic here).

### 2. Routing Layer (The "Traffic Cops")

#### Basic Logistics Pipe

**Role**: The standard intersection node. Reads item UUIDs and tells them where to turn.

- **Cache (Routing Table)**: Map of `[Destination UUID → Exit Face]`.
- **Alt Destination**: `[Destination UUID → Fallback Exit Face]` (Used instantly if the primary route triggers a Block Update/Break event).
- **Block Update Cache**: Listens for chunk/block updates to flag the routing table as "dirty" for a background recalculation.

#### Firewall Logistics Pipe

**Role**: Blocks items or networks from seeing each other.

- **Cache (Filter Table)**: Caches the exact Item/Tag whitelist/blacklist in memory so it doesn't read the block NBT every tick.

### 3. Supply & Demand Layer (The "Triggers")

#### Provider Pipe / Provider Logistics Pipe Mk2

**Role**: Pulls items from chests to fulfill network requests.

- **Cache (Inventory Snapshot)**: Event-driven! Only updates when NeoForge/Fabric fires an inventory change event. (Zero polling lag).
- **Cache (Destination)**: Where the extracted item is going.
- **Cache (Pending Extractions)**: Keeps track of items promised to the network but not yet physically extracted.

#### Supplier Pipe

**Role**: Keeps an attached chest constantly stocked with a specific amount of items.

- **Cache (Target Level)**: What the chest should have.
- **Cache (In-Transit Deliveries)**: Crucial for lag! Caches what is currently en route so it doesn't accidentally request 500 extra iron while waiting for the first batch to arrive.

#### Request Pipe / Mk2 / Mk3

**Role**: The UI block where players manually order items.

- **Cache (Active Order Queue)**: Tracks UUIDs of items currently being shipped to this specific pipe.

#### QuickSort Logistics Pipe

**Role**: Rapidly sucks items out of a dump chest and routes them to their default storage.

- **Cache (Default Storage Map)**: A specialized routing table that instantly knows the closest storage for any given item tag.

### 4. Auto-Crafting Layer (The "Functions")

#### Crafting Logistics Pipe / Mk2 / Mk3

**Role**: Automatically provides ingredients to machines and extracts the result.

- **Cache (Recipe Map)**: Caches the exact inputs/outputs so it doesn't query the machine's recipe registry every tick.
- **Cache (Satellite Link)**: Stores the exact UUID of any connected Satellite Pipes.
- **Cache (Active Crafting Job)**: Tracks "I have inserted materials, I am waiting for X result."

#### Satellite Logistics Pipe

**Role**: Receives secondary ingredients for complex crafting (like IC2/GregTech machines with multiple input hatches).

- **Cache (Parent Crafting Pipe)**: Hard-linked to the parent pipe's cache to sync delivery times.

### 5. Modular Layer (The "Chassis System")

#### Chassis Pipes (Mk1, Mk2, Mk3, Mk4, Mk5)

**Role**: The customizable pipes that hold module cards (Extractor, ItemSink, Polymorphic, etc.). Mk tier dictates how many modules it can hold.

- **Cache (Module Logic State)**: Compiles all inserted modules into a single "Behavior Profile" on placement. If it has a Provider Module and an ItemSink Module, the background Engine treats it as one unified node rather than two separate checks.
- **Cache (Primary Destination)**: Where items go by default.
- **Cache (Alt Destination)**: Where items go if primary is blocked/broken.

**Note for modern port**: Treat the Mk 1-5 simply as slot-size limits. The backend node logic should be identical, just executing more or fewer module "payloads."

### 6. Fluid / Liquid Layer

#### Liquid Basic, Liquid Provider, Liquid Supplier, Fluid Crafting

**Cache Logic**: Identical to the item pipes, but hooked into NeoForge FluidHandler / Fabric FluidStorage capabilities.

**Payload Difference**: Instead of moving physical item entities, fluids can just be purely visual math. "Pipe A drains 1000mB, Pipe B fills 1000mB 20 ticks later."

---

## 🧠 The Global "Alt Cache Block Update" Flow

### When a player breaks a pipe in the middle of your base:

1. **Event Fires**: `BlockBreakEvent` triggers at X, Y, Z.
2. **Network Manager Reacts**: Identifies which cached Node died.
3. **Instant Fallback**: All remaining Basic/Chassis pipes instantly switch their active cache to **Alt Destination** for any items currently physically traveling.
4. **Background Rebuild**: The Network Manager spins up a background thread to re-run the Dijkstra/n8n graph math.
5. **Cache Swap**: Once the math is done, it overwrites the **Primary Destination Cache** on all nodes and clears the fallback state.
6. **Result**: Zero dropped ticks, zero server lag spikes, items re-route smoothly in real-time.

---

## Implementation Rules & Patterns

### Cache Hierarchy

```
Every Routing Node:
├── Primary Destination Cache  (computed path)
├── Alt Destination Cache      (fallback during rebuild)
└── Compiled Behavior Profile  (for chassis: all modules combined)
```

### Event-Driven Over Polling

- **DO**: Listen for `BlockUpdateEvent`, `InventoryChangeEvent`, `BlockBreakEvent`
- **DON'T**: Scan inventories every tick or recalculate paths every tick
- Inventory snapshots update on-demand via Fabric/NeoForge events

### Dual-Cache Pattern

- Every routing pipe maintains **Primary** and **Alt** destinations
- On block break, switch to Alt instantly
- Background thread rebuilds Primary in parallel
- Atomic swap when ready (no visual glitches)

### Compiled Behavior

- Chassis pipes read their modules once on placement
- Compile to: `List<ModuleBehavior> compiled`
- Each module payload executes sequentially
- If modules conflict (e.g., two ItemSinks), the later one wins

### In-Transit Tracking

- Supply/Provider pipes track what's been promised but not delivered
- Format: `Map<ItemIdentifier, Integer> pendingDeliveries`
- Prevents ordering 500 iron when 400 is already en route
- Updates when items physically arrive via `notifyOfItemArival()`

### No Per-Tick Pathfinding

- Routes computed in background thread
- Pathfinding doesn't happen in `routingUpdate()` tick
- All nodes cache the result
- Updates happen atomically via `swapCache()` call to all nodes

---

## Cache Keys & Entry Points

| Pipe Type | Cache Key Type                      | Entry Point                 |
| --------- | ----------------------------------- | --------------------------- |
| Basic     | `ItemIdentifier` → `Direction` | Item arrives at pipe        |
| Provider  | `@Nullable`, event-driven         | Inventory change event      |
| Supplier  | Target stack size                   | Timer tick (every 20 ticks) |
| Request   | `ItemIdentifier`                  | Player clicks GUI           |
| Crafting  | `Recipe` (input list)             | Module inserts items        |
| Satellite | Parent UUID                         | Linked to crafting pipe     |
| Chassis   | Module list                         | Placement or NBT load       |

---

## Future: Multi-Pipe Entities

For QuickSort, RemoteOrderer, and System Entrance/Destination pipes that need shared state:

- Consider a **PipeNetwork** manager that compiles all caches into one graph
- Background thread updates the graph asynchronously
- All pipes pull their cache from the shared manager
- Atomic cache swap affects entire network at once

This keeps individual pipes simple while enabling global optimization.
