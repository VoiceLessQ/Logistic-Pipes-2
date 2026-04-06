package com.Morph.logisticspipes.routing;

import java.util.EnumSet;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.Direction;

/**
 * Describes one route from this router toward a destination router.
 * Ported from LP 1.12.2 ExitRoute — Direction replaces EnumFacing, power removed.
 */
public class ExitRoute implements Comparable<ExitRoute> {

    /** The router this route leads to. */
    @Nonnull
    public final IRouter destination;

    /** The router this route originates from (null = this router). */
    @Nullable
    public IRouter root;

    /** Which face of THIS pipe to exit through. */
    public Direction exitOrientation;

    /** Which face of the DESTINATION pipe this arrives at. */
    public Direction insertOrientation;

    /** Routing cost to destination (lower = preferred). */
    public double distanceToDestination;

    /** Reverse cost — used when destination requests FROM this pipe. */
    public final double destinationDistanceToRoot;

    /** Block count between source and destination. */
    public final int blockDistance;

    /** Connection capability flags. */
    public final EnumSet<PipeRoutingConnectionType> connectionDetails;

    /** Active filters on this route (firewall, etc.). Empty for Phase 3. */
    public List<Object> filters = List.of();

    public ExitRoute(@Nullable IRouter source, @Nonnull IRouter destination,
                     @Nullable Direction exitOrientation, @Nullable Direction insertOrientation,
                     double metric, EnumSet<PipeRoutingConnectionType> connectionDetails, int blockDistance) {
        this.destination = destination;
        this.root = source;
        this.exitOrientation = exitOrientation;
        this.insertOrientation = insertOrientation;
        this.connectionDetails = connectionDetails;
        this.blockDistance = blockDistance;

        this.distanceToDestination = connectionDetails.contains(PipeRoutingConnectionType.canRouteTo)
                ? metric : Double.MAX_VALUE;
        this.destinationDistanceToRoot = connectionDetails.contains(PipeRoutingConnectionType.canRequestFrom)
                ? metric : Double.MAX_VALUE;
    }

    public boolean containsFlag(PipeRoutingConnectionType flag) {
        return connectionDetails.contains(flag);
    }

    public boolean hasActivePipe() {
        return destination.getCachedPipe() != null;
    }

    @Override
    public int compareTo(ExitRoute o) {
        int c = Double.compare(distanceToDestination, o.distanceToDestination);
        if (c == 0) return Integer.compare(destination.getSimpleID(), o.destination.getSimpleID());
        return c;
    }

    @Override
    public String toString() {
        return "ExitRoute(exit=" + exitOrientation + ", insert=" + insertOrientation
                + ", dist=" + distanceToDestination + ", conn=" + connectionDetails + ")";
    }
}
