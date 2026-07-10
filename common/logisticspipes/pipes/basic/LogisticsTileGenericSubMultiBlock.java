package logisticspipes.pipes.basic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;

import net.minecraft.world.level.block.Block;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;

import net.minecraft.core.BlockPos;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import logisticspipes.interfaces.ITickable;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.packets.multiblock.MultiBlockCoordinatesPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.renderer.state.PipeSubRenderState;
import logisticspipes.routing.pathfinder.IPipeInformationProvider;
import logisticspipes.routing.pathfinder.ISubMultiBlockPipeInformationProvider;
import logisticspipes.utils.TileBuffer;
import network.rs485.logisticspipes.world.DoubleCoordinates;

public class LogisticsTileGenericSubMultiBlock extends BlockEntity implements ISubMultiBlockPipeInformationProvider, ITickable {

	private Set<DoubleCoordinates> mainPipePos = new HashSet<>();
	private List<LogisticsTileGenericPipe> mainPipe;
	private List<CoreMultiBlockPipe.SubBlockTypeForShare> subTypes = new ArrayList<>();
	private TileBuffer[] tileBuffer;
	public final PipeSubRenderState renderState;

	@Deprecated
	public LogisticsTileGenericSubMultiBlock(BlockPos blockPos, BlockState blockState) {
		super(logisticspipes.LPRegistries.BE_SUB_PIPE.get(), blockPos, blockState);
		renderState = new PipeSubRenderState();
	}

	public LogisticsTileGenericSubMultiBlock(BlockPos blockPos, BlockState blockState, DoubleCoordinates pos) {
		super(logisticspipes.LPRegistries.BE_SUB_PIPE.get(), blockPos, blockState);
		if (pos != null) {
			mainPipePos.add(pos);
		}
		mainPipe = null;
		renderState = new PipeSubRenderState();
	}

	public List<LogisticsTileGenericPipe> getMainPipe() {
		if (mainPipe == null) {
			mainPipe = new ArrayList<>();
			for (DoubleCoordinates pos : mainPipePos) {
				BlockEntity tile = pos.getTileEntity(getLevel());
				if (tile instanceof LogisticsTileGenericPipe) {
					mainPipe.add((LogisticsTileGenericPipe) tile);
				}
			}
			mainPipe = Collections.unmodifiableList(mainPipe);
		}
		if (MainProxy.isServer(getLevel())) {
			boolean allInvalid = true;
			for (LogisticsTileGenericPipe pipe : mainPipe) {
				if (!pipe.isRemoved()) {
					allInvalid = false;
					break;
				}
			}
			if (mainPipe.isEmpty() || allInvalid) {
				getLevel().removeBlock(getBlockPos(), false);
			}
		}
		if (mainPipe != null) {
			return mainPipe;
		}
		return Collections.emptyList();
	}

	/**
	 * Side-effect-free variant of {@link #getMainPipe()} for shape/pick queries: no caching,
	 * and never mutates the world (getMainPipe may remove this block when all mains are gone,
	 * which must not happen mid collision query).
	 */
	public List<LogisticsTileGenericPipe> getConnectedMainPipes() {
		if (getLevel() == null) {
			return Collections.emptyList();
		}
		List<LogisticsTileGenericPipe> result = new ArrayList<>(mainPipePos.size());
		for (DoubleCoordinates pos : mainPipePos) {
			BlockEntity tile = pos.getTileEntity(getLevel());
			if (tile instanceof LogisticsTileGenericPipe) {
				result.add((LogisticsTileGenericPipe) tile);
			}
		}
		return result;
	}

	public List<CoreMultiBlockPipe.SubBlockTypeForShare> getSubTypes() {
		return Collections.unmodifiableList(subTypes);
	}

	@Override
	public void update() {
		if (MainProxy.isClient(getLevel())) {
			return;
		}
		List<LogisticsTileGenericPipe> pipes = getMainPipe();
		for (LogisticsTileGenericPipe pipe : pipes) {
			pipe.subMultiBlock.add(new DoubleCoordinates(this));
		}
	}

	@Override
	public void load(CompoundTag nbt) {
		super.load(nbt);
		if (nbt.contains("MainPipePos_xPos")) {
			mainPipePos.clear();
			DoubleCoordinates pos = DoubleCoordinates.readFromNBT("MainPipePos_", nbt);
			if (pos != null) {
				mainPipePos.add(pos);
			}
		}
		if (nbt.contains("MainPipePosList")) {
			ListTag list = nbt.getList("MainPipePosList", net.minecraft.nbt.Tag.TAG_COMPOUND);
			for (int i = 0; i < list.size(); i++) {
				DoubleCoordinates pos = DoubleCoordinates.readFromNBT("MainPipePos_", list.getCompound(i));
				if (pos != null) {
					mainPipePos.add(pos);
				}
			}
		}
		if (nbt.contains("SubTypeList")) {
			ListTag list = nbt.getList("SubTypeList", net.minecraft.nbt.Tag.TAG_STRING);
			subTypes.clear();
			for (int i = 0; i < list.size(); i++) {
				String name = list.getString(i);
				CoreMultiBlockPipe.SubBlockTypeForShare type = CoreMultiBlockPipe.SubBlockTypeForShare.valueOf(name);
				if (type != null) {
					subTypes.add(type);
				}
			}
		}
		mainPipe = null;
	}

	@Override
	public void saveAdditional(CompoundTag nbt) {
		super.saveAdditional(nbt);
		ListTag nbtList = new ListTag();
		for (DoubleCoordinates pos : mainPipePos) {
			CompoundTag compound = new CompoundTag();
			pos.writeToNBT("MainPipePos_", compound);
			nbtList.add(compound);
		}
		nbt.put("MainPipePosList", nbtList);
		ListTag nbtTypeList = new ListTag();
		for (CoreMultiBlockPipe.SubBlockTypeForShare type : subTypes) {
			if (type == null) continue;
			nbtTypeList.add(StringTag.valueOf(type.name()));
		}
		nbt.put("SubTypeList", nbtTypeList);
	}

	@Nonnull
	@Override
	public CompoundTag getUpdateTag() {
		CompoundTag nbt = super.getUpdateTag();
		try {
			PacketHandler.addPacketToNBT(getLPDescriptionPacket(), nbt);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return nbt;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void handleUpdateTag(@Nonnull CompoundTag tag) {
		PacketHandler.queueAndRemovePacketFromNBT(tag);
		super.handleUpdateTag(tag);
	}

	@Override
	public ClientboundBlockEntityDataPacket getUpdatePacket() {
		return ClientboundBlockEntityDataPacket.create(this);
	}

	@Override
	public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet) {
		if (packet.getTag() != null) PacketHandler.queueAndRemovePacketFromNBT(packet.getTag());
	}

	public ModernPacket getLPDescriptionPacket() {
		MultiBlockCoordinatesPacket packet = PacketHandler.getPacket(MultiBlockCoordinatesPacket.class);
		packet.setTilePos(this);
		packet.setTargetPos(mainPipePos);
		packet.setSubTypes(subTypes);
		return packet;
	}

	public void setPosition(Set<DoubleCoordinates> lpPosition, List<CoreMultiBlockPipe.SubBlockTypeForShare> subTypes) {
		mainPipePos = lpPosition;
		this.subTypes = subTypes;
		mainPipe = null;
	}

	public BlockEntity getTile() {
		return this;
	}

	public BlockEntity getTile(Direction to) {
		return getTile(to, false);
	}

	public BlockEntity getTile(Direction to, boolean force) {
		TileBuffer[] cache = getTileCache();
		if (cache != null) {
			if (force) {
				cache[to.ordinal()].refresh();
			}
			return cache[to.ordinal()].getTile();
		} else {
			return null;
		}
	}

	public Block getBlock(Direction to) {
		TileBuffer[] cache = getTileCache();
		if (cache != null) {
			return cache[to.ordinal()].getBlock();
		} else {
			return null;
		}
	}

	public TileBuffer[] getTileCache() {
		if (tileBuffer == null) {
			tileBuffer = TileBuffer.makeBuffer(getLevel(), getBlockPos(), true);
		}
		return tileBuffer;
	}

	// invalidate() removed from BlockEntity in 1.20.1 — use setRemoved()
	@Override
	public void setRemoved() {
		super.setRemoved();
		tileBuffer = null;
	}

	// validate() removed from BlockEntity in 1.20.1 — use onLoad()
	@Override
	public void onLoad() {
		super.onLoad();
		tileBuffer = null;
	}

	public void scheduleNeighborChange() {
		tileBuffer = null;
	}

	public void addSubTypeTo(CoreMultiBlockPipe.SubBlockTypeForShare type) {
		if (type == null) throw new NullPointerException();
		subTypes.add(type);
	}

	public void addMultiBlockMainPos(DoubleCoordinates placeAt) {
		if (mainPipePos.add(placeAt)) {
			mainPipe = null;
		}
	}

	public boolean removeMainPipe(DoubleCoordinates doubleCoordinates) {
		mainPipePos.remove(doubleCoordinates);
		return mainPipePos.isEmpty();
	}

	public void removeSubType(CoreMultiBlockPipe.SubBlockTypeForShare type) {
		subTypes.remove(type);
	}

	@Override
	public IPipeInformationProvider getMainTile() {
		List<LogisticsTileGenericPipe> mainTiles = this.getMainPipe();
		if (mainTiles.size() != 1) {
			return null;
		}
		return mainTiles.get(0);
	}
}
