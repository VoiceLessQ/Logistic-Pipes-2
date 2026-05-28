package logisticspipes.blocks.powertile;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;

// IC2 imports removed — no 1.20.1 port; IEnergySink added at runtime via @ModDependentInterface

import logisticspipes.LPConstants;
import logisticspipes.asm.ModDependentInterface;
import logisticspipes.asm.ModDependentMethod;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.renderer.LogisticsHUDRenderer;

@ModDependentInterface(modId = { LPConstants.ic2ModID }, interfacePath = { "ic2.api.energy.tile.IEnergySink" })
public class LogisticsIC2PowerProviderTileEntity extends LogisticsPowerProviderTileEntity
		// IEnergySink — added at runtime by @ModDependentInterface when IC2 is present
{

	public static final int MAX_STORAGE = 40000000;
	public static final int MAX_MAXMODE = 8;
	public static final int MAX_PROVIDE_PER_TICK = 2048 * 6; //TODO

	private boolean addedToEnergyNet = false;
	private boolean init = false;

	public LogisticsIC2PowerProviderTileEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
		super(logisticspipes.LPRegistries.BE_POWER_PROVIDER_EU.get(), pos, state);
	}

	@Override
	public void update() {
		super.update();
		// TODO(1.20.1): IC2 not ported — energy net registration disabled
		// if (!init) {
		// 	if (!addedToEnergyNet) {
		// 		SimpleServiceLocator.IC2Proxy.registerToEneryNet(this);
		// 		addedToEnergyNet = true;
		// 	}
		// }
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		if (MainProxy.isClient(getWorld())) {
			LogisticsHUDRenderer.instance().remove(this);
		}
		// TODO(1.20.1): IC2 not ported — energy net unregistration disabled
		// if (addedToEnergyNet) {
		// 	SimpleServiceLocator.IC2Proxy.unregisterToEneryNet(this);
		// 	addedToEnergyNet = false;
		// }
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if (MainProxy.isClient(getWorld())) {
			init = false;
		}
		if (!addedToEnergyNet) {
			init = false;
		}
	}

	// onChunkUnload removed in 1.20.1 — setRemoved() covers this case

	public void addEnergy(double amount) {
		if (MainProxy.isClient(getWorld())) {
			return;
		}
		internalStorage += amount;
		if (internalStorage > LogisticsIC2PowerProviderTileEntity.MAX_STORAGE) {
			internalStorage = LogisticsIC2PowerProviderTileEntity.MAX_STORAGE;
		}
		if (internalStorage >= getMaxStorage()) {
			needMorePowerTriggerCheck = false;
		}
	}

	public double freeSpace() {
		return getMaxStorage() - internalStorage;
	}

	@Override
	public int getMaxStorage() {
		maxMode = Math.min(LogisticsIC2PowerProviderTileEntity.MAX_MAXMODE, Math.max(1, maxMode));
		return (LogisticsIC2PowerProviderTileEntity.MAX_STORAGE / maxMode);
	}

	@Override
	public void load(CompoundTag nbt) {
		super.load(nbt);
	}

	@Override
	public void saveAdditional(CompoundTag nbt) {
		super.saveAdditional(nbt);
	}

	@Override
	public String getBrand() {
		return "EU";
	}

	@Override
	protected double getMaxProvidePerTick() {
		return LogisticsIC2PowerProviderTileEntity.MAX_PROVIDE_PER_TICK;
	}

	@Override
	protected void handlePower(CoreRoutedPipe pipe, double toSend) {
		pipe.handleIC2PowerArival(toSend);
	}

	@Override
	protected int getLaserColor() {
		return LogisticsPowerProviderTileEntity.IC2_COLOR;
	}

	// @Override removed — IEnergySink not in implements
	@ModDependentMethod(modId = LPConstants.ic2ModID)
	public boolean acceptsEnergyFrom(Object tile, Direction dir) { // was: IEnergyEmitter tile
		return true;
	}

	// @Override removed — IEnergySink not in implements
	@ModDependentMethod(modId = LPConstants.ic2ModID)
	public double getDemandedEnergy() {
		return freeSpace();
	}

	// @Override removed — IEnergySink not in implements
	@ModDependentMethod(modId = LPConstants.ic2ModID)
	public int getSinkTier() {
		return Integer.MAX_VALUE;
	}

	// @Override removed — IEnergySink not in implements
	@ModDependentMethod(modId = LPConstants.ic2ModID)
	public double injectEnergy(Direction directionFrom, double amount, double voltage) {
		addEnergy(amount);
		return 0;
	}
}
