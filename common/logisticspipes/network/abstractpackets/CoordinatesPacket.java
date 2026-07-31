package logisticspipes.network.abstractpackets;

import java.util.function.Function;
import javax.annotation.Nonnull;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import logisticspipes.network.exception.TargetNotFoundException;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;
import network.rs485.logisticspipes.world.DoubleCoordinates;

@ToString
public abstract class CoordinatesPacket extends ModernPacket {

	@Getter
	@Setter
	private int posX;
	@Getter
	@Setter
	private int posY;
	@Getter
	@Setter
	private int posZ;

	public CoordinatesPacket(int id) {
		super(id);
	}

	@Nonnull
	@SuppressWarnings("unchecked")
	public static <T> T getTileAs(Object whosAsking, Level world, BlockPos blockPos, Class<T> clazz) {
		final BlockEntity tile = getWorldTile(whosAsking, world, blockPos);
		if (tile != null) {
			if (clazz.isAssignableFrom(tile.getClass())) {
				return (T) tile;
			}
			throw new TargetNotFoundException("Couldn't find " + clazz.getName() + ", found " + tile.getClass() + " at: " + blockPos, whosAsking);
		} else {
			throw new TargetNotFoundException("Couldn't find " + clazz.getName() + " at: " + blockPos, whosAsking);
		}
	}

	private static BlockEntity getWorldTile(Object whosAsking, Level world, BlockPos blockPos) {
		if (world == null) {
			throw new TargetNotFoundException("World was null", whosAsking);
		}
		if (world.isEmptyBlock(blockPos)) {
			throw new TargetNotFoundException("Only found air at: " + blockPos, whosAsking);
		}

		return world.getBlockEntity(blockPos);
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeInt(posX);
		output.writeInt(posY);
		output.writeInt(posZ);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		posX = input.readInt();
		posY = input.readInt();
		posZ = input.readInt();

	}

	public CoordinatesPacket setTilePos(BlockEntity tile) {
		setDimension(tile.getLevel());
		setPosX(tile.getBlockPos().getX());
		setPosY(tile.getBlockPos().getY());
		setPosZ(tile.getBlockPos().getZ());
		return this;
	}

	public CoordinatesPacket setLPPos(DoubleCoordinates pos) {
		setPosX(pos.getXInt());
		setPosY(pos.getYInt());
		setPosZ(pos.getZInt());
		return this;
	}

	public CoordinatesPacket setPacketPos(CoordinatesPacket packet) {
		posX = packet.getPosX();
		posY = packet.getPosY();
		posZ = packet.getPosZ();
		return this;
	}

	public CoordinatesPacket setBlockPos(BlockPos pos) {
		posX = pos.getX();
		posY = pos.getY();
		posZ = pos.getZ();
		return this;
	}

	public BlockEntity getTileAs(Level world, Function<BlockEntity, Boolean> validateResult) {
		BlockEntity tile = getTileAs(world, BlockEntity.class);
		if (!validateResult.apply(tile)) {
			throw new TargetNotFoundException("BlockEntity condition not met", this);
		}
		return tile;
	}

	/**
	 * Retrieves tileEntity at packet coordinates if any.
	 */
	public <T> T getTileAs(Level world, Class<T> clazz) {
		return getTileAs(this, world, new BlockPos(getPosX(), getPosY(), getPosZ()), clazz);
	}

	/**
	 * Retrieves tileEntity or CoreUnroutedPipe at packet coordinates if any.
	 */
	@SuppressWarnings("unchecked")
	public <T> T getTileOrPipe(Level world, Class<T> clazz) {
		final BlockEntity tile = getWorldTile(this, world, new BlockPos(getPosX(), getPosY(), getPosZ()));
		if (tile != null) {
			if (clazz.isAssignableFrom(tile.getClass())) {
				return (T) tile;
			}
			if (tile instanceof LogisticsTileGenericPipe) {
				if (((LogisticsTileGenericPipe) tile).pipe != null && clazz.isAssignableFrom(((LogisticsTileGenericPipe) tile).pipe.getClass())) {
					return (T) ((LogisticsTileGenericPipe) tile).pipe;
				}
				throw new TargetNotFoundException("Couldn't find " + clazz.getName() + ", found pipe with " + tile.getClass() + " at: " + new BlockPos(getPosX(), getPosY(), getPosZ()), this);
			}
		} else {
			throw new TargetNotFoundException("Couldn't find " + clazz.getName() + " at: " + new BlockPos(getPosX(), getPosY(), getPosZ()), this);
		}
		throw new TargetNotFoundException("Couldn't find " + clazz.getName() + ", found " + tile.getClass() + " at: " + new BlockPos(getPosX(), getPosY(), getPosZ()), this);
	}

	/**
	 * Retrieves pipe at packet coordinates if any.
	 */
	@Deprecated
	public LogisticsTileGenericPipe getPipe(Level world) {
		return getPipe(world, LTGPCompletionCheck.NONE);
	}

	/**
	 * Retrieves pipe at packet coordinates in the given world, or {@code null} if there is none.
	 */
	public LogisticsTileGenericPipe getPipeOrNull(Level world) {
		if (world == null) {
			return null;
		}
		BlockEntity tile = world.getBlockEntity(new BlockPos(getPosX(), getPosY(), getPosZ()));
		return tile instanceof LogisticsTileGenericPipe ? (LogisticsTileGenericPipe) tile : null;
	}

	@Nonnull
	public LogisticsTileGenericPipe getPipe(Level world, LTGPCompletionCheck check) {
		LogisticsTileGenericPipe pipe = getTileAs(world, LogisticsTileGenericPipe.class);
		if (check == LTGPCompletionCheck.PIPE || check == LTGPCompletionCheck.TRANSPORT) {
			if (pipe.pipe == null) {
				throw new TargetNotFoundException("The found pipe didn't have a loaded pipe field", this);
			}
		}
		if (check == LTGPCompletionCheck.TRANSPORT) {
			if (pipe.pipe.transport == null) {
				throw new TargetNotFoundException("The found pipe didn't have a loaded transport field", this);
			}
		}
		return pipe;
	}

	public enum LTGPCompletionCheck {
		NONE,
		PIPE,
		TRANSPORT
	}
}
