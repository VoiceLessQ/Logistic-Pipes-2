package logisticspipes.blocks.powertile;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import logisticspipes.utils.TagUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;

// IC2 imports removed — no 1.20.1 port; IEnergySink added at runtime via @ModDependentInterface

import logisticspipes.LPConstants;
import logisticspipes.asm.ModDependentInterface;
import logisticspipes.asm.ModDependentMethod;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.IC2RebornEnergy;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.TREnergyStorage;
import logisticspipes.proxy.interfaces.IEnergyStorage;
import logisticspipes.renderer.LogisticsHUDRenderer;

@ModDependentInterface(modId = { LPConstants.ic2ModID }, interfacePath = { "ic2.api.energy.tile.IEnergySink" })
public class LogisticsIC2PowerProviderTileEntity extends LogisticsPowerProviderTileEntity
		// IEnergySink — added at runtime by @ModDependentInterface when IC2 is present
{

	public static final int MAX_STORAGE = 40000000;
	public static final int MAX_MAXMODE = 8;
	public static final int MAX_PROVIDE_PER_TICK = 2048 * 6; //TODO

	private boolean addedToEnergyNet = false;

	// accepts EU (1 E = 1 EU) from Tech Reborn cables, generators and storage via Team Reborn Energy
	private final TREnergyStorage trEnergy = new TREnergyStorage(new IEnergyStorage() {

		@Override
		public int receiveEnergy(int maxReceive, boolean simulate) {
			int received = (int) Math.max(0, Math.min(maxReceive, freeSpace()));
			if (!simulate) addEnergy(received);
			return received;
		}

		@Override
		public int extractEnergy(int maxExtract, boolean simulate) {
			return 0;
		}

		@Override
		public int getEnergyStored() {
			return (int) internalStorage;
		}

		@Override
		public int getMaxEnergyStored() {
			return getMaxStorage();
		}

		@Override
		public boolean canExtract() {
			return false;
		}

		@Override
		public boolean canReceive() {
			return true;
		}
	});
	private boolean init = false;

	public LogisticsIC2PowerProviderTileEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
		super(logisticspipes.LPRegistries.BE_POWER_PROVIDER_EU.get(), pos, state);
	}

	@Override
	public void update() {
		super.update();
		if (MainProxy.isServer(getWorld())) {
			int eu = IC2RebornEnergy.pull(this, (int) freeSpace());
			if (eu > 0) {
				addEnergy(eu);
			}
		}
		if (!init && !addedToEnergyNet) {
			// IC2 energy net registration removed — IC2 has no 1.20.1 port (former dummy was a no-op).
			addedToEnergyNet = true;
		}
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		if (MainProxy.isClient(getWorld())) {
			LogisticsHUDRenderer.instance().remove(this);
		}
		if (addedToEnergyNet) {
			addedToEnergyNet = false;
		}
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
	protected void loadAdditional(ValueInput input) {
		CompoundTag nbt = TagUtil.read(input);
		super.loadAdditional(input);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		CompoundTag nbt = new CompoundTag();
		super.saveAdditional(output);
		TagUtil.write(output, nbt);
	}

	public TREnergyStorage getTREnergy() {
		return trEnergy;
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
