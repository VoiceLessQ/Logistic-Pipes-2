package com.Morph.logisticspipes.routing;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.Morph.logisticspipes.api.ILogisticsPowerProvider;
import com.Morph.logisticspipes.pipes.basic.CoreRoutedPipe;
import com.Morph.logisticspipes.pipes.basic.LogisticsPipeBlockEntity;
import com.Morph.logisticspipes.routing.pathfinder.PathFinder;
import com.Morph.logisticspipes.utils.item.ItemIdentifier;

/**
 * Per-pipe router. Discovers adjacent routers via PathFinder, then runs
 * Dijkstra across the global LSA database to build a route table.
 *
 * Ported from LP 1.12.2 ServerRouter — power, CC/OC, filters, async update
 * removed for Phase 3. Route table rebuilt synchronously on demand.
 */
public class ServerRouter implements IRouter, Comparable<ServerRouter> {

    // -------------------------------------------------------------------------
    // Static ID management (mirrors LP 1.12.2 simpleID system)
    // -------------------------------------------------------------------------

    private static int firstFreeId = 1;
    private static final BitSet usedIds = new BitSet();

    private static synchronized int claimSimpleID() {
        int idx = usedIds.nextClearBit(firstFreeId);
        firstFreeId = idx + 1;
        usedIds.set(idx);
        return idx;
    }

    private static synchronized void releaseSimpleID(int idx) {
        usedIds.clear(idx);
        if (idx < firstFreeId) firstFreeId = idx;
    }

    public static synchronized int getBiggestSimpleID() {
        return usedIds.length();
    }

    public static synchronized void cleanup() {
        usedIds.clear();
        firstFreeId = 1;
    }

    // -------------------------------------------------------------------------
    // Instance state
    // -------------------------------------------------------------------------

    public final UUID id;
    private final int simpleID;
    private final BlockPos pos;
    private final ServerLevel level;

    private WeakReference<CoreRoutedPipe> pipeCache = new WeakReference<>(null);
    private boolean destroyed = false;

    /** Direct adjacent routed pipes → exit route from this router's perspective. */
    public Map<CoreRoutedPipe, ExitRoute> adjacent = new HashMap<>();

    /** Power providers (e.g. Power Junction) sitting on this router's 6 neighbours. */
    private List<ILogisticsPowerProvider> localPowerProviders = Collections.emptyList();

    /**
     * Route table: index = destination simpleID → sorted list of ExitRoutes.
     * Rebuilt by Dijkstra. AtomicReference for lock-free reads while rebuild runs.
     */
    private final AtomicReference<List<List<ExitRoute>>> routeTable =
            new AtomicReference<>(Collections.emptyList());

    /** All reachable routers sorted by cost, for provider scanning. */
    private final AtomicReference<List<ExitRoute>> routeCosts =
            new AtomicReference<>(Collections.emptyList());

    private boolean connectionDirty = true;

    public ServerRouter(UUID id, ServerLevel level, BlockPos pos) {
        this.id = id;
        this.level = level;
        this.pos = pos;
        this.simpleID = claimSimpleID();
    }

    // -------------------------------------------------------------------------
    // IRouter
    // -------------------------------------------------------------------------

    @Override
    public void destroy() {
        destroyed = true;
        releaseSimpleID(simpleID);
        RouterManager.removeRouter(id);
    }

    @Override
    public void update(boolean doFullRefresh, CoreRoutedPipe pipe) {
        if (destroyed) return;
        if (doFullRefresh || connectionDirty) {
            refreshAdjacent(pipe);
            refreshLocalPowerProviders();
            connectionDirty = false;
        }
        rebuildRoutingTable();
    }

    /**
     * Returns every {@link ILogisticsPowerProvider} reachable through the LP network,
     * ordered by routing cost (closest first). Includes this router's own local
     * adjacent providers at the front, followed by reachable routers' providers in
     * the order they appear in {@link #routeCosts}.
     */
    public List<ILogisticsPowerProvider> getPowerProvidersInNetwork() {
        List<ILogisticsPowerProvider> all = new ArrayList<>(localPowerProviders);
        for (ExitRoute route : routeCosts.get()) {
            if (route.destination instanceof ServerRouter sr && sr != this) {
                all.addAll(sr.localPowerProviders);
            }
        }
        return all;
    }

    @Override
    public boolean isRoutedExit(Direction dir) {
        for (ExitRoute route : adjacent.values()) {
            if (route.exitOrientation == dir) return true;
        }
        return false;
    }

    @Override
    public boolean hasRoute(int id, boolean active, ItemIdentifier type) {
        List<List<ExitRoute>> table = routeTable.get();
        if (id >= table.size()) return false;
        List<ExitRoute> routes = table.get(id);
        if (routes == null || routes.isEmpty()) return false;
        if (!active) return true;
        return routes.stream().anyMatch(ExitRoute::hasActivePipe);
    }

    @Override
    public ExitRoute getExitFor(int id, boolean active, ItemIdentifier type) {
        List<List<ExitRoute>> table = routeTable.get();
        if (id >= table.size()) return null;
        List<ExitRoute> routes = table.get(id);
        if (routes == null || routes.isEmpty()) return null;
        for (ExitRoute r : routes) {
            if (!active || r.hasActivePipe()) return r;
        }
        return null;
    }

    @Override
    public List<List<ExitRoute>> getRouteTable() {
        return routeTable.get();
    }

    @Override
    public List<ExitRoute> getIRoutersByCost() {
        return routeCosts.get();
    }

    @Override
    @Nullable
    public CoreRoutedPipe getPipe() {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof LogisticsPipeBlockEntity lpBE && lpBE.getPipe() instanceof CoreRoutedPipe crp) {
            pipeCache = new WeakReference<>(crp);
            return crp;
        }
        return null;
    }

    @Override
    @Nullable
    public CoreRoutedPipe getCachedPipe() {
        return pipeCache.get();
    }

    @Override
    public boolean isAt(BlockPos p) {
        return pos.equals(p);
    }

    @Override
    public UUID getId() { return id; }

    @Override
    public void clearPipeCache() { pipeCache = new WeakReference<>(null); }

    @Override
    public int getSimpleID() { return simpleID; }

    @Override
    public BlockPos getPos() { return pos; }

    @Override
    public boolean isSideDisconnected(Direction dir) { return false; }

    @Override
    public List<ExitRoute> getDistanceTo(IRouter r) {
        List<List<ExitRoute>> table = routeTable.get();
        int id = r.getSimpleID();
        if (id >= table.size()) return Collections.emptyList();
        List<ExitRoute> routes = table.get(id);
        return routes != null ? routes : Collections.emptyList();
    }

    // -------------------------------------------------------------------------
    // Routing logic
    // -------------------------------------------------------------------------

    /** Uses PathFinder to discover directly reachable routed pipes. */
    private void refreshAdjacent(CoreRoutedPipe pipe) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof LogisticsPipeBlockEntity lpBE)) {
            adjacent = new HashMap<>();
            return;
        }
        PathFinder finder = new PathFinder(lpBE);
        adjacent = finder.result;
        // Stamp root router on each exit route
        for (ExitRoute route : adjacent.values()) {
            route.root = this;
        }
    }

    /** Scans the 6 cardinal neighbours of this router's pipe for power providers. */
    private void refreshLocalPowerProviders() {
        List<ILogisticsPowerProvider> found = null;
        for (Direction dir : Direction.values()) {
            BlockEntity be = level.getBlockEntity(pos.relative(dir));
            if (be instanceof ILogisticsPowerProvider provider) {
                if (found == null) found = new ArrayList<>(2);
                found.add(provider);
            }
        }
        localPowerProviders = found != null ? found : Collections.emptyList();
    }

    /**
     * Dijkstra across the router graph to build the full route table.
     * Each router knows its immediate neighbours via adjacent.
     * We flood-fill from here using those adjacency lists.
     */
    private void rebuildRoutingTable() {
        int maxId = RouterManager.getBiggestSimpleID() + 1;

        // dist[simpleID] → best known cost
        double[] dist = new double[maxId];
        java.util.Arrays.fill(dist, Double.MAX_VALUE);
        dist[simpleID] = 0;

        // bestExit[simpleID] → ExitRoute from THIS router toward that destination
        ExitRoute[] bestExit = new ExitRoute[maxId];

        PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> Double.compare(dist[a[0]], dist[b[0]]));
        pq.add(new int[]{simpleID});

        // Seed with direct neighbours
        for (ExitRoute route : adjacent.values()) {
            int destId = route.destination.getSimpleID();
            if (destId >= maxId) continue;
            if (route.distanceToDestination < dist[destId]) {
                dist[destId] = route.distanceToDestination;
                bestExit[destId] = route;
                pq.add(new int[]{destId});
            }
        }

        while (!pq.isEmpty()) {
            int[] cur = pq.poll();
            int curId = cur[0];
            ServerRouter curRouter = RouterManager.getRouterBySimpleID(curId);
            if (curRouter == null || curRouter == this) continue;

            // Relax edges from curRouter's adjacency
            for (ExitRoute neighbourRoute : curRouter.adjacent.values()) {
                int neighbourId = neighbourRoute.destination.getSimpleID();
                if (neighbourId >= maxId) continue;
                double newDist = dist[curId] + neighbourRoute.distanceToDestination;
                if (newDist < dist[neighbourId]) {
                    dist[neighbourId] = newDist;
                    // The exit from THIS router toward neighbourId is still via bestExit[curId]
                    bestExit[neighbourId] = bestExit[curId];
                    pq.add(new int[]{neighbourId});
                }
            }
        }

        // Build route table and sorted cost list
        List<List<ExitRoute>> newTable = new ArrayList<>(maxId);
        for (int i = 0; i < maxId; i++) newTable.add(null);

        List<ExitRoute> costs = new ArrayList<>();
        for (int i = 0; i < maxId; i++) {
            if (bestExit[i] != null) {
                // Create a new ExitRoute reflecting the full distance
                ExitRoute original = bestExit[i];
                ExitRoute entry = new ExitRoute(this, original.destination,
                        original.exitOrientation, original.insertOrientation,
                        dist[i], original.connectionDetails, (int) dist[i]);
                newTable.set(i, List.of(entry));
                costs.add(entry);
            }
        }

        costs.sort(null);
        routeTable.set(Collections.unmodifiableList(newTable));
        routeCosts.set(Collections.unmodifiableList(costs));
    }

    public void flagConnectionDirty() {
        connectionDirty = true;
    }

    @Override
    public int compareTo(ServerRouter o) {
        return Integer.compare(simpleID, o.simpleID);
    }

    @Override
    public String toString() {
        return "ServerRouter[id=" + simpleID + ", pos=" + pos + "]";
    }
}
