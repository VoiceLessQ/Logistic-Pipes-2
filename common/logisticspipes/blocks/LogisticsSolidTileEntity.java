package logisticspipes.blocks;

import javax.annotation.Nonnull;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

// OpenComputers imports removed — OC not on classpath for 1.20.1; interfaces added at runtime via @ModDependentInterface ASM

import logisticspipes.LPConstants;
import logisticspipes.asm.ModDependentField;
import logisticspipes.asm.ModDependentInterface;
import logisticspipes.asm.ModDependentMethod;
import logisticspipes.interfaces.IRotationProvider;
import logisticspipes.interfaces.ITickable;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.block.RequestRotationPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.computers.interfaces.CCCommand;
import logisticspipes.proxy.computers.interfaces.CCType;
import logisticspipes.proxy.computers.interfaces.ILPCCTypeHolder;
import network.rs485.logisticspipes.connection.NeighborTileEntity;
import network.rs485.logisticspipes.world.DoubleCoordinates;
import network.rs485.logisticspipes.world.WorldCoordinatesWrapper;

@ModDependentInterface(modId = { LPConstants.openComputersModID, LPConstants.openComputersModID, LPConstants.openComputersModID }, interfacePath = { "li.cil.oc.api.network.ManagedPeripheral", "li.cil.oc.api.network.Environment", "li.cil.oc.api.network.SidedEnvironment" })
@CCType(name = "LogisticsSolidBlock")
public class LogisticsSolidTileEntity extends BlockEntity implements ITickable, ILPCCTypeHolder, IRotationProvider {
		// ManagedPeripheral, Environment, SidedEnvironment — added at runtime by @ModDependentInterface ASM when OC is present

	private final Object[] ccTypeHolder = new Object[1];
	private boolean init = false;
	public int rotation = 0;

	@ModDependentField(modId = LPConstants.openComputersModID)
	public Object node; // was: Node (OC removed from classpath)

	public LogisticsSolidTileEntity(net.minecraft.world.level.block.entity.BlockEntityType<?> type, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
		super(type, pos, state);
	}

	/** Returns the level this block entity is in. Replaces removed getWorld() from 1.12.2. */
	protected Level getWorld() {
		return this.level;
	}

	@Override
	public void load(CompoundTag nbt) {
		super.load(nbt);
		rotation = nbt.getInt("rotation");
	}

	@Override
	public void saveAdditional(CompoundTag nbt) {
		super.saveAdditional(nbt);
		nbt.putInt("rotation", rotation);
	}

	// onChunkUnload() removed in 1.20.1 — handled by level unload events if needed

	@Override
	public void update() {
		if (MainProxy.isClient(getWorld())) {
			if (!init) {
				MainProxy.sendPacketToServer(PacketHandler.getPacket(RequestRotationPacket.class).setBlockPos(getBlockPos()));
				init = true;
			}
			return;
		}
	}

	// shouldRefresh() removed in 1.20.1 — block entities are replaced on block change by default

	@Override
	public void setRemoved() {
		super.setRemoved();
	}

	public void onBlockBreak() {
	}

	@Override
	@CCCommand(description = "Returns the LP rotation value for this block")
	public int getRotation() {
		return rotation;
	}

	public boolean isActive() {
		return false;
	}

	@Override
	public void setRotation(int rotation) {
		this.rotation = rotation;
	}

	public void notifyOfBlockChange() {
	}

	// OC methods — @Override removed, types replaced with Object (OC not on classpath)
	@ModDependentMethod(modId = LPConstants.openComputersModID)
	public Object node() {
		return node;
	}

	@ModDependentMethod(modId = LPConstants.openComputersModID)
	public void onConnect(Object node1) {
	}

	@ModDependentMethod(modId = LPConstants.openComputersModID)
	public void onDisconnect(Object node1) {
	}

	@ModDependentMethod(modId = LPConstants.openComputersModID)
	public void onMessage(Object message) {
	}

	@ModDependentMethod(modId = LPConstants.openComputersModID)
	public Object[] invoke(String s, Object context, Object arguments) {
		// TODO: OC BaseWrapperClass.WRAPPER/isDirectCall not available — OC integration deferred
		return new Object[0];
	}

	@ModDependentMethod(modId = LPConstants.openComputersModID)
	public String[] methods() {
		return new String[] { "getBlock" };
	}

	@ModDependentMethod(modId = LPConstants.openComputersModID)
	public Object sidedNode(Direction side) {
		final NeighborTileEntity<BlockEntity> neighbor = new WorldCoordinatesWrapper(this).getNeighbor(side);
		if (neighbor == null || neighbor.isLogisticsPipe() || neighbor.getTileEntity() instanceof LogisticsSolidTileEntity) {
			return null;
		} else {
			return node();
		}
	}

	@OnlyIn(Dist.CLIENT)
	@ModDependentMethod(modId = LPConstants.openComputersModID)
	public boolean canConnect(Direction side) {
		final NeighborTileEntity<BlockEntity> neighbor = new WorldCoordinatesWrapper(this).getNeighbor(side);
		return neighbor != null && !neighbor.isLogisticsPipe() && !(neighbor.getTileEntity() instanceof LogisticsSolidTileEntity);
	}

	public DoubleCoordinates getLPPosition() {
		return new DoubleCoordinates(this);
	}

	public Level getLevelForHUD() {
		return getWorld();
	}

	@Override
	public Object[] getTypeHolder() {
		return ccTypeHolder;
	}

}
