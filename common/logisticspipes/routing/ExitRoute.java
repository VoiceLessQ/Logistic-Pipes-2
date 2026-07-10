/*
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.routing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import logisticspipes.interfaces.routing.IFilter;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.routing.debug.ExitRouteDebug;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;
import network.rs485.logisticspipes.util.LPFinalSerializable;
import network.rs485.logisticspipes.world.DoubleCoordinates;

/**
 * Defines direction with a cost
 */
public class ExitRoute implements Comparable<ExitRoute>, LPFinalSerializable {

	public final double destinationDistanceToRoot;
	public final int blockDistance;
	public final EnumSet<PipeRoutingConnectionType> connectionDetails;
	public final @Nonnull IRouter destination;
	public Direction exitOrientation;
	public Direction insertOrientation;
	public double distanceToDestination;
	public IRouter root;
	public List<IFilter> filters = Collections.unmodifiableList(new ArrayList<>(0));
	/**
	 * Used to store debug information. No use in the actual Routing table
	 * calculation
	 */
	public ExitRouteDebug debug = new ExitRouteDebug();

	public ExitRoute(@Nullable IRouter source, @Nonnull IRouter destination, @Nullable Direction exitOrientation, @Nullable Direction insertOrientation, double metric,
			EnumSet<PipeRoutingConnectionType> connectionDetails, int blockDistance) {
		this.destination = destination;
		this.root = source;
		this.exitOrientation = exitOrientation;
		this.insertOrientation = insertOrientation;
		this.connectionDetails = connectionDetails;
		if (connectionDetails.contains(PipeRoutingConnectionType.canRouteTo)) {
			distanceToDestination = metric;
		} else {
			distanceToDestination = Integer.MAX_VALUE;
		}
		if (connectionDetails.contains(PipeRoutingConnectionType.canRequestFrom)) {
			destinationDistanceToRoot = metric;
		} else {
			destinationDistanceToRoot = Integer.MAX_VALUE;
		}
		this.blockDistance = blockDistance;
	}

	@OnlyIn(Dist.CLIENT)
	public ExitRoute(LPDataInput input) {
		if (!input.readBoolean()) {
			throw new RuntimeException("Cannot read an ExitRoute without destination");
		}
		final IRouter destinationRouter = readRouter(input);
		if (destinationRouter == null) {
			throw new RuntimeException("Destination of the ExitRoute could not be determined");
		}
		destination = destinationRouter;

		if (input.readBoolean()) {
			root = readRouter(input);
		} else {
			root = null;
		}

		exitOrientation = input.readFacing();
		insertOrientation = input.readFacing();

		connectionDetails = input.readEnumSet(PipeRoutingConnectionType.class);

		distanceToDestination = input.readDouble();

		double metric = input.readDouble();
		if (!connectionDetails.contains(PipeRoutingConnectionType.canRequestFrom)) {
			metric = Integer.MAX_VALUE;
		}
		destinationDistanceToRoot = metric;

		blockDistance = input.readInt();

		debug.filterPosition = input.readArrayList(DoubleCoordinates::new);
		debug.toStringNetwork = input.readUTF();
		debug.isNewlyAddedCanidate = input.readBoolean();
		debug.isTraced = input.readBoolean();
		debug.index = input.readInt();
	}

	public ExitRoute(IRouter source, IRouter destination, double distance, EnumSet<PipeRoutingConnectionType> enumSet, List<IFilter> filterA,
			List<IFilter> filterB, int blockDistance) {
		this(source, destination, null, null, distance, enumSet, blockDistance);
		List<IFilter> filter = new ArrayList<>(filterA.size() + filterB.size());
		filter.addAll(filterA);
		filter.addAll(filterB);
		filters = Collections.unmodifiableList(filter);
	}

	@OnlyIn(Dist.CLIENT)
	private IRouter readRouter(LPDataInput input) {
		DoubleCoordinates pos = new DoubleCoordinates(input);
		BlockEntity tile = pos.getTileEntity(MainProxy.getClientMainWorld());
		if (tile instanceof LogisticsTileGenericPipe && ((LogisticsTileGenericPipe) tile).pipe instanceof CoreRoutedPipe) {
			return ((CoreRoutedPipe) ((LogisticsTileGenericPipe) tile).pipe).getRouter();
		}
		return null;
	}

	@Override
	public void write(LPDataOutput output) {
		output.writeBoolean(true);
		destination.write(output);

		if (root == null) {
			output.writeBoolean(false);
		} else {
			output.writeBoolean(true);
			root.write(output);
		}

		output.writeFacing(exitOrientation);
		output.writeFacing(insertOrientation);

		output.writeEnumSet(connectionDetails, PipeRoutingConnectionType.class);

		output.writeDouble(distanceToDestination);

		output.writeDouble(destinationDistanceToRoot);

		output.writeInt(blockDistance);

		output.writeCollection(filters, (innerOutput, filter) -> innerOutput.writeSerializable(filter.getLPPosition()));
		output.writeUTF(toString());
		output.writeBoolean(debug.isNewlyAddedCanidate);
		output.writeBoolean(debug.isTraced);
		output.writeInt(debug.index);
	}

	@Override
	public boolean equals(Object aThat) {
		//check for self-comparison
		if (this == aThat) {
			return true;
		}

		if (!(aThat instanceof ExitRoute)) {
			return false;
		}
		ExitRoute that = (ExitRoute) aThat;
		return exitOrientation.equals(that.exitOrientation) && insertOrientation.equals(that.insertOrientation) && connectionDetails.equals(that.connectionDetails) && distanceToDestination == that.distanceToDestination && destinationDistanceToRoot == that.destinationDistanceToRoot && destination == that.destination
				&& filters.equals(that.filters);
	}

	public boolean isSameWay(ExitRoute that) {
		if (equals(that)) {
			return true;
		}
		return connectionDetails.equals(that.connectionDetails) && destination == that.destination && filters.equals(that.filters);
	}

	@Override
	public String toString() {
		return String.format("ExitRoute(exitdir=%s, insertdir=%s, distance=%s, reverse distance=%s, conn. details=%s, "
						+ "filters=%s)", exitOrientation, insertOrientation, distanceToDestination, destinationDistanceToRoot,
				connectionDetails, filters);
	}

	public void removeFlags(EnumSet<PipeRoutingConnectionType> flags) {
		connectionDetails.removeAll(flags);
	}

	public boolean containsFlag(PipeRoutingConnectionType flag) {
		return connectionDetails.contains(flag);
	}

	public boolean hasActivePipe() {
		return destination.getCachedPipe() != null;
	}

	//copies
	public EnumSet<PipeRoutingConnectionType> getFlags() {
		return EnumSet.copyOf(connectionDetails);
	}

	// Doesn't copy
	public Set<PipeRoutingConnectionType> getFlagsNoCopy() {
		return Collections.unmodifiableSet(connectionDetails);
	}

	@Override
	public int compareTo(ExitRoute o) {
		final int c = Double.compare(distanceToDestination, o.distanceToDestination);
		if (c == 0) {
			return Integer.compare(destination.getSimpleID(), o.destination.getSimpleID());
		}
		return c;
	}
}
