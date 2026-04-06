# LP2 Deferred Features — Implementation Plan

Generated: 2026-04-06
Status: Ready to implement — build passes, all CRITICAL/HIGH/MEDIUM done

---

## Implementation Rules — Cache Hierarchy (apply to EVERY feature)

These rules from PIPE_ARCHITECTURE.md govern all new code:

1. **Atomic Route Table** — ServerRouter uses AtomicReference<List<List<ExitRoute>>>. Reads are lock-free. Any feature touching routing tables must atomic-swap writes, never lock during reads.

2. **Event-Driven Over Polling** — `interestsDirty` flag gates interest map rebuild. Every new state cache must follow: set dirty flag on change, compute lazily. Never poll in tick().

3. **Compiled Behavior** — Chassis modules compute-once on attach/change. New module-level state should similarly cache, not re-derive each tick.

4. **In-Transit Tracking** — `PriorityBlockingQueue<ItemRoutingInformation>` with 640-tick timeout pruning. New delivery tracking must use this mechanism.

5. **No Per-Tick Pathfinding** — `getRouteTable()` is a lock-free atomic read. Call it once, cache result. Never re-run Dijkstra per-tick.

6. **Fallback Table** — During rebuild, old routes stay valid. Features must tolerate fallback table (may have stale routes).

7. **Background Thread Safety** — `createRouteTable()` runs off main thread. Data read during route computation must be thread-safe (AtomicReference or read-only snapshot).

---

## Feature Order (recommended)

| # | Feature | New Files | Modified Files | Est. Lines New | Est. Lines Modified | Risk |
|---|---|---|---|---|---|---|
| 1 | Security securityTick | 0 | 2 | 0 | ~30 | Low |
| 2 | Pipe Renderer (traveling items) | 1 | 3 | ~120 | ~30 | Medium |
| 3 | Statistics tracking | 0 | 2 | 0 | ~65 | Low |
| 4 | Upgrade Insertion GUI | 2 | 4 | ~155 | ~75 | Low-Medium |

AsyncExtractorModule and PipeBlockRequestTable are deferred (no plan needed yet).

---

## Feature 1 — Security `securityTick()` (LOW COMPLEXITY)

### Context

`UpgradeManager.securityTick()` is a confirmed no-op stub. `_securityID` UUID field is persisted. `SecurityStationManager` uses `ConcurrentHashMap<UUID, ISecurityProvider>` so reads are already thread-safe. `SafeTimeTracker` is available and used elsewhere for throttled checks.

### File: `common/.../pipes/upgrades/UpgradeManager.java`

Add field:
```java
private final SafeTimeTracker _securityTickTimer = new SafeTimeTracker(20);
```

Replace `securityTick()` no-op with:
```java
public void securityTick() {
    Level level = pipe.container != null ? pipe.container.getLevel() : null;
    if (level == null || level.isClientSide() || !_securityTickTimer.markTimeIfDelay(level)) return;
    if (_securityID != null
            && SecurityStationManager.INSTANCE.getStation(_securityID) == null) {
        _securityID = null;
        pipe.setSecurityId(null);
        if (pipe.container != null) pipe.container.setChanged();
    }
}
```

Add imports: `com.Morph.logisticspipes.security.SecurityStationManager`

Cache hierarchy: `SafeTimeTracker` uses `level.getGameTime()` — main-thread safe. `SecurityStationManager.getStation()` reads a ConcurrentHashMap — thread-safe. Runs every 20 ticks, not every tick.

### File: `common/.../pipes/basic/CoreRoutedPipe.java`

In `routingUpdate()`, after the module tick call, add:
```java
upgradeManager.securityTick();
```

`routingUpdate()` is only called with `_router != null` on the server side, so this is server-only. Correct.

---

## Feature 2 — Pipe Renderer: Traveling Item Animation (MEDIUM COMPLEXITY)

### Context

Static pipe geometry is done via blockstates JSON + multipart models (pipe_core_N.json, pipe_arm_*.json). What is missing is the `BlockEntityRenderer<LogisticsPipeBlockEntity>` for traveling items. Server side is complete: `PacketPipePositionS2C` is registered and broadcast from `moveSolids()`. `LPTravelingItemClient` objects are created in `PipeTransportLogistics.handleItemPositionPacket()` and stored in both `transport.items` (per-pipe list) and `LPTravelingItem.clientList` (global weak map).

### Step 2.1 — Complete `LPNetworking.handlePipePosition` body

File: `common/.../network/LPNetworking.java`

Find the existing `handlePipePosition` method stub and add:
```java
private static void handlePipePosition(PacketPipePositionS2C payload,
                                       NetworkManager.PacketContext ctx) {
    ctx.queue(() -> {
        Level level = Minecraft.getInstance().level;
        if (level == null) return;
        BlockEntity be = level.getBlockEntity(payload.pos());
        if (!(be instanceof LogisticsPipeBlockEntity lbe)) return;
        if (lbe.pipe == null || lbe.pipe.transport == null) return;
        lbe.pipe.transport.handleItemPositionPacket(
                payload.travelId(),
                payload.inputOrdinal() >= 0 ? Direction.values()[payload.inputOrdinal()] : null,
                payload.outputOrdinal() >= 0 ? Direction.values()[payload.outputOrdinal()] : null,
                payload.speed(),
                payload.position(),
                payload.yaw());
    });
}
```

Note: check `PacketPipePositionS2C` fields — if `inputOrdinal`/`outputOrdinal` can be -1 for null direction, guard accordingly.

Cache hierarchy: runs on the client render thread via `ctx.queue()`. Reads/writes `transport.items` which is a per-pipe list — the only thread accessing this on the client is the render thread, so no synchronization needed client-side.

### Step 2.2 — Create `LPPipeRenderer.java`

File (new): `common/src/main/java/com/Morph/logisticspipes/client/LPPipeRenderer.java`

```java
package com.Morph.logisticspipes.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import dev.architectury.injectables.annotations.ExpectPlatform;

import com.Morph.logisticspipes.pipes.basic.LogisticsPipeBlockEntity;
import com.Morph.logisticspipes.transport.LPTravelingItem;
import com.Morph.logisticspipes.transport.LPTravelingItem.LPTravelingItemClient;

public class LPPipeRenderer implements BlockEntityRenderer<LogisticsPipeBlockEntity> {

    public LPPipeRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(LogisticsPipeBlockEntity be, float partialTick, PoseStack ps,
                       MultiBufferSource buf, int light, int overlay) {
        if (be.pipe == null || be.pipe.transport == null) return;

        // Snapshot — do NOT hold a live reference across frames (server thread writes this list)
        List<LPTravelingItem> snapshot;
        synchronized (be.pipe.transport.items) {
            snapshot = new ArrayList<>(be.pipe.transport.items);
        }

        Minecraft mc = Minecraft.getInstance();
        for (LPTravelingItem raw : snapshot) {
            if (!(raw instanceof LPTravelingItemClient item)) continue;
            if (item.getItemIdentifierStack() == null) continue;

            float pos = Math.clamp(item.getPosition() + item.getSpeed() * partialTick, 0f, 1f);

            // Compute offset from block origin (block center = 0.5, 0.5, 0.5)
            double ox = 0.5, oy = 0.375, oz = 0.5;
            if (item.input != null && item.output != null) {
                if (pos < 0.5f) {
                    // Approaching center from input side
                    float t = pos / 0.5f; // 0=at input face, 1=at center
                    ox = 0.5 + item.input.getStepX() * (1 - t) * 0.5;
                    oy = 0.375 + item.input.getStepY() * (1 - t) * 0.5;
                    oz = 0.5 + item.input.getStepZ() * (1 - t) * 0.5;
                } else {
                    // Leaving center toward output side
                    float t = (pos - 0.5f) / 0.5f; // 0=at center, 1=at output face
                    ox = 0.5 + item.output.getStepX() * t * 0.5;
                    oy = 0.375 + item.output.getStepY() * t * 0.5;
                    oz = 0.5 + item.output.getStepZ() * t * 0.5;
                }
            }

            ps.pushPose();
            ps.translate(ox - 0.5, oy - 0.5, oz - 0.5); // renderer origin is block origin
            ps.scale(0.25f, 0.25f, 0.25f);

            ItemStack stack = item.getItemIdentifierStack().makeNormalStack();
            mc.getItemRenderer().renderStatic(
                    stack, ItemDisplayContext.GROUND, light, overlay, ps, buf,
                    be.getLevel(), 0);

            ps.popPose();
        }
    }

    @Override
    public boolean shouldRenderOffScreen(LogisticsPipeBlockEntity be) {
        return true; // render even when BE is off-screen so items crossing chunk borders appear
    }
}
```

Cache hierarchy:
- Snapshot copy of `transport.items` inside `synchronized` block — prevents ConcurrentModificationException from server tick thread.
- Reads `item.input/output/position/speed` — these fields are written only by the client render thread (from `ctx.queue()` in Step 2.1), so no contention.
- Does NOT call any routing logic. Does NOT read `pipeConnections[]` (used only for model selection, already handled by the JSON model).

### Step 2.3 — Register `LPPipeRenderer`

File: `common/.../LPClientSetup.java`

Add method:
```java
public static void registerBlockEntityRenderers() {
    BlockEntityRendererRegistry.register(LPBlocks.PIPE_BLOCK_ENTITY.get(), LPPipeRenderer::new);
}
```

Add import: `dev.architectury.registry.client.rendering.BlockEntityRendererRegistry`

File: `fabric/src/main/java/com/Morph/fabric/client/ExampleModFabricClient.java`
Add call to `LPClientSetup.registerBlockEntityRenderers();` in the client init method.

File: `neoforge/src/main/java/com/Morph/neoforge/client/ExampleModNeoForgeClient.java`
Add call to `LPClientSetup.registerBlockEntityRenderers();` inside the client setup event handler.

### Gotchas

- Verify `LPItemList` (the type of `transport.items`) — if it is already a `CopyOnWriteArrayList` or thread-safe, the `synchronized` block is redundant but harmless. If it's a plain ArrayList, the synchronized block is required.
- Verify `makeNormalStack()` exists on `ItemIdentifierStack`. If not, use `new ItemStack(item.getItemIdentifierStack().getItem().getItem())`.
- `Math.clamp` requires Java 21 — confirmed (project uses Java 21).
- `BlockEntityRendererRegistry` is Architectury's abstraction — both Fabric and NeoForge platforms support it. Import: `dev.architectury.registry.client.rendering.BlockEntityRendererRegistry`.
- The `com.mojang.blaze3d` package import may need adjustment — use the remapped MC path.

---

## Feature 3 — Statistics Tracking (`relayedItem`) (LOW COMPLEXITY)

### Context

`CoreRoutedPipe.relayedItem(int stackSize)` is a no-op stub at line ~585. `LPOverlayRenderer` renders pipe name + connection info on shift-hover. Adding a per-second relay count (rolling 20-bucket window) completes the HUD.

Cache hierarchy: `AtomicInteger[]` circular buffer — lock-free, thread-safe for the integrated server case. In SMP, client-side pipe object is a different instance than server-side, so count reads 0 on the client. This is acceptable; a future `PacketStatS2C` can fix SMP.

### File: `common/.../pipes/basic/CoreRoutedPipe.java`

Add fields (after existing fields):
```java
private final java.util.concurrent.atomic.AtomicInteger[] _relayedPerTick;
private volatile int _relayBucket = 0;
private volatile long _lastRelayTick = -1L;
```

Initialize in constructor (add to existing constructor body):
```java
_relayedPerTick = new java.util.concurrent.atomic.AtomicInteger[20];
for (int i = 0; i < 20; i++) _relayedPerTick[i] = new java.util.concurrent.atomic.AtomicInteger(0);
```

Replace `relayedItem()` no-op:
```java
public void relayedItem(int stackSize) {
    long now = container != null && container.getLevel() != null
               ? container.getLevel().getGameTime() : -1L;
    if (now >= 0 && now != _lastRelayTick) {
        _relayBucket = (int)(now % 20);
        _relayedPerTick[_relayBucket].set(0);
        _lastRelayTick = now;
    }
    if (now >= 0) _relayedPerTick[_relayBucket].addAndGet(stackSize);
}

public int getRelayedLastSecond() {
    int sum = 0;
    for (var a : _relayedPerTick) sum += a.get();
    return sum;
}
```

Cache hierarchy: `AtomicInteger.addAndGet()` and `AtomicInteger.get()` are lock-free. `volatile` on `_relayBucket` and `_lastRelayTick` ensures the render thread sees the latest bucket index. This follows the event-driven pattern: counter updated only when `relayedItem()` is called, not polled.

### File: `common/.../client/LPOverlayRenderer.java`

In `renderHud`, after the connections line, add:
```java
if (tile.pipe instanceof CoreRoutedPipe routedPipe) {
    int relayed = routedPipe.getRelayedLastSecond();
    // expand background height by one line
    graphics.fill(x - 2, y - 2, x + 140, y + font.lineHeight * 3 + 4, 0xAA000000);
    graphics.drawString(font, "Relayed/s: " + relayed, x, y + font.lineHeight * 2, 0xFFDD44, false);
}
```

Adjust existing `graphics.fill` call to not hard-code 2 lines when the stat is shown.

---

## Feature 4 — Upgrade Insertion GUI (LOW-MEDIUM COMPLEXITY)

### Context

`UpgradeManager.inv` (9 slots), `sneakyInv` (9 slots), `secInv` (1 slot) are all `SimpleStackInventory` implementing `Container`. `tryInserting()` is implemented. No `MenuUpgrade`, `ScreenUpgrade`, or `UPGRADE` menu type exists yet.

### Step 4.1 — Create `MenuUpgrade.java`

File (new): `common/src/main/java/com/Morph/logisticspipes/gui/menu/MenuUpgrade.java`

Follow `MenuChassis` pattern. Server constructor takes live `Container` refs from `UpgradeManager`; client constructor reads `BlockPos` from buf and creates `SimpleContainer` snapshots (vanilla sync fills them).

Slot layout (176×198 background):
- Main upgrade row labels at y=6
- Main upgrade grid (9 slots, 3×3): x=8, y=18
- Sneaky upgrade label at y=57
- Sneaky upgrade grid (9 slots, 3×3): x=8, y=67
- Security card label at y=107
- Security slot (1): x=8, y=117
- Player inventory (27): x=8, y=136
- Hotbar (9): x=8, y=186 — wait, this exceeds 198px. Recalculate:
  - total slots height: 3*18 + 4 (spacing) + 3*18 + 4 + 18 + 8 = 54+4+54+4+18+8 = 142 for content
  - Player inv: 3*18=54, hotbar: 18, gap: 4
  - Total: 142 + 4 + 54 + 4 + 18 = 222 → use imageHeight=222

```java
public class MenuUpgrade extends AbstractContainerMenu {
    public final BlockPos pos;

    // Client constructor
    public MenuUpgrade(int syncId, Inventory playerInv, FriendlyByteBuf buf) {
        this(syncId, playerInv, buf.readBlockPos(),
             new SimpleContainer(9), new SimpleContainer(9), new SimpleContainer(1));
    }

    // Server constructor — pass live UpgradeManager containers
    public MenuUpgrade(int syncId, Inventory playerInv, BlockPos pos,
                       Container mainInv, Container sneakyInv, Container secInv) {
        super(LPMenuTypes.UPGRADE.get(), syncId);
        this.pos = pos;

        // Main upgrades (9 slots, 3x3 at y=18)
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 3; col++)
                addSlot(new Slot(mainInv, row * 3 + col, 8 + col * 18, 18 + row * 18));

        // Sneaky upgrades (9 slots, 3x3 at y=67)
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 3; col++)
                addSlot(new Slot(sneakyInv, row * 3 + col, 8 + col * 18, 67 + row * 18));

        // Security card (1 slot at y=117)
        addSlot(new Slot(secInv, 0, 8, 117));

        // Player inventory (3 rows at y=140)
        for (int row = 0; row < 3; row++)
            for (int col = 0; col < 9; col++)
                addSlot(new Slot(playerInv, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));

        // Hotbar (y=194)
        for (int col = 0; col < 9; col++)
            addSlot(new Slot(playerInv, col, 8 + col * 18, 194));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }

    @Override
    public boolean stillValid(Player player) { return true; }
}
```

### Step 4.2 — Create `ScreenUpgrade.java`

File (new): `common/src/main/java/com/Morph/logisticspipes/gui/screen/ScreenUpgrade.java`

```java
@Environment(EnvType.CLIENT)
public class ScreenUpgrade extends AbstractContainerScreen<MenuUpgrade> {
    public ScreenUpgrade(MenuUpgrade menu, Inventory inv, Component title) {
        super(menu, inv, title);
        imageWidth = 176;
        imageHeight = 222;
    }

    @Override
    protected void renderBg(GuiGraphics g, float pt, int mx, int my) {
        g.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFC6C6C6);
        // Slot backgrounds
        g.fill(leftPos + 6, topPos + 16, leftPos + 60, topPos + 70, 0xFF8B8B8B);
        g.fill(leftPos + 6, topPos + 65, leftPos + 60, topPos + 119, 0xFF8B8B8B);
        g.fill(leftPos + 6, topPos + 115, leftPos + 26, topPos + 135, 0xFF8B8B8B);
        // Labels
        g.drawString(font, "Upgrades",        leftPos + 8, topPos + 7,   0x404040, false);
        g.drawString(font, "Sneaky Upgrades", leftPos + 8, topPos + 56,  0x404040, false);
        g.drawString(font, "Security Card",   leftPos + 8, topPos + 106, 0x404040, false);
    }
}
```

No special click logic needed — vanilla `AbstractContainerScreen` handles slot rendering and item pickup from `addSlot()` calls.

### Step 4.3 — Add `UPGRADE` to `LPMenuTypes.java`

File: `common/.../LPMenuTypes.java`

Add before `SECURITY_STATION`:
```java
public static final RegistrySupplier<MenuType<MenuUpgrade>> UPGRADE =
        MENUS.register("upgrade",
                () -> MenuRegistry.ofExtended(MenuUpgrade::new));
```

Add import: `MenuUpgrade`

### Step 4.4 — Register `ScreenUpgrade` in `LPClientSetup.java`

File: `common/.../LPClientSetup.java`

In `registerScreens()`:
```java
MenuScreenRegistry.registerScreenFactory(LPMenuTypes.UPGRADE.get(), ScreenUpgrade::new);
```

### Step 4.5 — Wire upgrade GUI open in `CoreRoutedPipe.blockActivated` (common)

File: `common/.../pipes/basic/CoreRoutedPipe.java`

Override or extend `blockActivated(Player player)`. Add at the TOP of the method (before other checks):

```java
// Shift + empty hand: open upgrade management GUI
if (player.isShiftKeyDown() && player.getMainHandItem().isEmpty()
        && player instanceof ServerPlayer sp && container != null) {
    UpgradeManager um = upgradeManager;
    MenuRegistry.openExtendedMenu(sp,
        new MenuProvider() {
            @Override public Component getDisplayName() { return Component.literal("Pipe Upgrades"); }
            @Override public AbstractContainerMenu createMenu(int syncId, Inventory inv, Player p) {
                return new MenuUpgrade(syncId, inv, container.getBlockPos(),
                        um.inv, um.sneakyInv, um.secInv);
            }
        },
        buf -> buf.writeBlockPos(container.getBlockPos()));
    return;
}

// Holding an upgrade: insert it directly
if (player.getMainHandItem().getItem() instanceof ItemUpgrade
        && !player.getMainHandItem().isEmpty()) {
    Level lvl = container != null ? container.getLevel() : null;
    if (lvl != null && !lvl.isClientSide()) {
        upgradeManager.tryInserting(lvl, player);
    }
    return;
}
```

Add imports: `MenuUpgrade`, `net.minecraft.network.chat.Component`, `net.minecraft.world.MenuProvider`, `net.minecraft.server.level.ServerPlayer`

Cache hierarchy: no caching needed. `UpgradeManager.inv` is a live `SimpleStackInventory` — passing it directly to `MenuUpgrade` means vanilla Container sync handles propagating changes to the client. Upgrade slots change infrequently (manual insertion only).

### Step 4.6 — Add `handleOpenGui` case for UPGRADE in `LPNetworking.java`

The `blockActivated` path above opens the menu directly server-side when called on `!level.isClientSide()`, following the same pattern as `SecurityStationBlock.useWithoutItem`. No `PacketOpenGuiC2S` change needed for this flow.

However, if `blockActivated` is called from `LogisticsPipeBlock.useWithoutItem` with the `!level.isClientSide()` guard already in place, the `MenuRegistry.openExtendedMenu` call will happen server-side correctly. Verify `LogisticsPipeBlock.useWithoutItem` structure matches this expectation.

---

## Cross-Cutting Concerns

**LPItemList thread safety (Feature 2)**: Before using `synchronized(be.pipe.transport.items)` in the renderer, check `LPItemList`'s implementation. If it's a `java.util.ArrayList` subclass, the synchronized block is required. If it wraps `CopyOnWriteArrayList`, the block is redundant.

**`makeNormalStack()` in renderer (Feature 2)**: Verify this method exists on `ItemIdentifierStack`. If missing, use `new ItemStack(item.getItemIdentifierStack().getItem().getItem())` as fallback.

**Menu slot math (Feature 4)**: The pixel heights above are estimates. Verify against `AbstractContainerScreen`'s slot rendering to ensure player inventory rows don't overlap content. Adjust `imageHeight` accordingly.

**SMP statistics (Feature 3)**: In multiplayer, `CoreRoutedPipe` on the client is NOT the same object as on the server. `getRelayedLastSecond()` will always return 0 on the client. A future `PacketStatS2C` can add SMP support when needed.

**`securityTick()` import (Feature 1)**: Verify `SecurityStationManager.INSTANCE` is a static field or singleton accessor. If `getStation(UUID)` is on `ISecurityStationManager` interface, cast accordingly.

---

## Deferred (No Plan Yet)

- **AsyncExtractorModule**: Async item extraction from inventories. Depends on threading design decision.
- **PipeBlockRequestTable**: Crafting monitoring block. Depends on logisticsManager port.
- **logisticsManager port**: Full `getAvailableItems()` API for history display in Request pipe.
- **SMP relay stats**: `PacketStatS2C` for broadcasting relay counts to clients.
- **Upgrade GUI `quickMoveStack`**: Shift-click from player inventory into upgrade slots (currently returns EMPTY).
