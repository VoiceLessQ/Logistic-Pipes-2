package com.Morph.logisticspipes.routing.pathfinder;

import java.util.ArrayDeque;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.Morph.logisticspipes.pipes.basic.CoreRoutedPipe;
import com.Morph.logisticspipes.pipes.basic.LogisticsPipeBlockEntity;
import com.Morph.logisticspipes.routing.ExitRoute;
import com.Morph.logisticspipes.routing.PipeRoutingConnectionType;

/**
 * BFS through the pipe network from a starting routed pipe to find all
 * reachable routed pipes, recording exit direction and distance.
 *
 * Ported from LP 1.12.2 PathFinder — simplified for Phase 3 (no filters, no power,
 * no special connections, no change listeners).
 */
public class PathFinder {

    private static final int MAX_VISITED = 4096;
    private static final int MAX_LENGTH = 256;

    /** result: adjacent routed pipes → their ExitRoute from startPipe's perspective */
    public final Map<CoreRoutedPipe, ExitRoute> result;

    private final Set<BlockPos> visited = new HashSet<>();
    private int visitCount = 0;

    public PathFinder(LogisticsPipeBlockEntity startBE) {
        result = findConnectedRoutingPipes(startBE);
    }

    /**
     * BFS from startBE. For each direction, walk through unrouted pipes until we
     * hit a routed pipe (an endpoint) or a dead end. Record each routed pipe found
     * with its exit direction and block distance.
     */
    private Map<CoreRoutedPipe, ExitRoute> findConnectedRoutingPipes(LogisticsPipeBlockEntity startBE) {
        Map<CoreRoutedPipe, ExitRoute> found = new HashMap<>();
        Level level = startBE.getLevel();
        if (level == null) return found;

        BlockPos startPos = startBE.getBlockPos();
        visited.add(startPos);

        // For each of the 6 exit directions from the start pipe
        for (Direction exitDir : Direction.values()) {
            if (!startBE.isConnected(exitDir)) continue;

            // BFS along this branch
            Queue<BlockPos> queue = new ArrayDeque<>();
            Map<BlockPos, Integer> distanceMap = new HashMap<>();
            Set<BlockPos> branchVisited = new HashSet<>();

            BlockPos firstStep = startPos.relative(exitDir);
            queue.add(firstStep);
            distanceMap.put(firstStep, 1);
            branchVisited.add(startPos);

            while (!queue.isEmpty() && visitCount < MAX_VISITED) {
                BlockPos cur = queue.poll();
                if (branchVisited.contains(cur)) continue;
                branchVisited.add(cur);
                visitCount++;

                int dist = distanceMap.getOrDefault(cur, MAX_LENGTH);
                if (dist > MAX_LENGTH) continue;

                BlockEntity be = level.getBlockEntity(cur);
                if (!(be instanceof LogisticsPipeBlockEntity curPipe)) continue;

                CoreRoutedPipe routedPipe = (curPipe.getPipe() instanceof CoreRoutedPipe rp) ? rp : null;

                if (routedPipe != null) {
                    // Found a routed pipe — this is an endpoint
                    Direction insertDir = exitDir.getOpposite();
                    EnumSet<PipeRoutingConnectionType> connType = EnumSet.of(
                            PipeRoutingConnectionType.canRouteTo,
                            PipeRoutingConnectionType.canRequestFrom
                    );
                    ExitRoute route = new ExitRoute(null, routedPipe.getRouter(),
                            exitDir, insertDir, dist, connType, dist);
                    // Keep shortest path if multiple routes to same pipe
                    found.merge(routedPipe, route,
                            (a, b) -> a.distanceToDestination <= b.distanceToDestination ? a : b);
                } else {
                    // Unrouted pipe — relay, continue BFS
                    for (Direction nextDir : Direction.values()) {
                        if (!curPipe.isConnected(nextDir)) continue;
                        BlockPos next = cur.relative(nextDir);
                        if (!branchVisited.contains(next)) {
                            distanceMap.put(next, dist + 1);
                            queue.add(next);
                        }
                    }
                }
            }
        }

        return found;
    }
}
