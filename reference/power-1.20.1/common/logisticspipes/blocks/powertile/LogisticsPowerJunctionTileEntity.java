package logisticspipes.blocks.powertile;

import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.CrashReportCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;



// CapabilityEnergy removed in NeoForge 1.20.1 — use ForgeCapabilities.EnergyStorage.BLOCK
import net.minecraftforge.energy.IEnergyStorage;

// import buildcraft.api.mj.IMjConnector;
// import buildcraft.api.mj.IMjReceiver;
// IC2 imports removed — IC2 has no 1.20.1 port; IEnergySink interface added at runtime via @ModDependentInterface ASM

import logisticspipes.LPConstants;
import logisticspipes.LogisticsPipes;
import logisticspipes.api.ILogisticsPowerProvider;
import logisticspipes.asm.ModDependentInterface;
import logisticspipes.asm.ModDependentMethod;
import logisticspipes.blocks.LogisticsSolidTileEntity;
import logisticspipes.config.Configs;
import logisticspipes.gui.hud.HUDPowerLevel;
import logisticspipes.interfaces.IBlockWatchingHandler;
import logisticspipes.interfaces.IGuiOpenControler;
import logisticspipes.interfaces.IGuiTileEntity;
import logisticspipes.interfaces.IHeadUpDisplayBlockRendererProvider;
import logisticspipes.interfaces.IHeadUpDisplayRenderer;
import logisticspipes.interfaces.IPowerLevelDisplay;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractguis.CoordinatesGuiProvider;
import logisticspipes.network.guis.block.PowerJunctionGui;
import logisticspipes.network.packets.block.PowerJunctionLevel;
import logisticspipes.network.packets.hud.HUDStartBlockWatchingPacket;
import logisticspipes.network.packets.hud.HUDStopBlockWatchingPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.computers.interfaces.CCCommand;
import logisticspipes.proxy.computers.interfaces.CCType;
import logisticspipes.renderer.LogisticsHUDRenderer;
import logisticspipes.utils.PlayerCollectionList;

@ModDependentInterface(modId = { LPConstants.ic2ModID }, interfacePath = { "ic2.api.energy.tile.IEnergySink" })
@CCType(name = "LogisticsPowerJunction")
public class LogisticsPowerJunctionTileEntity extends LogisticsSolidTileEntity implements IGuiTileEntity, ILogisticsPowerProvider, IPowerLevelDisplay, IGuiOpenControler, IHeadUpDisplayBlockRendererProvider, IBlockWatchingHandler
		// IEnergySink — added at runtime by @ModDependentInterface ASM when IC2 is present
{

	public Object OPENPERIPHERAL_IGNORE; //Tell OpenPeripheral to ignore this class

	// TODO: BuildCraft MJ capabilities (IMjConnector, IMjReceiver) — deferred until BuildCraft 1.20.1 is available.
	// Capability<T> API removed in NeoForge 1.20.1; use BlockCapability when migrating.

	// true if it needs more power, turns off at full, turns on at 50%.
	public boolean needMorePowerTriggerCheck = true;

	public final static int IC2Multiplier = 2;
	public final static int RFDivisor = 2;
	public final static int MJMultiplier = 5;
	public final static int MAX_STORAGE = 2000000;

	private int internalStorage = 0;
	private int lastUpdateStorage = 0;
	private double internalBuffer = 0;

	//small buffer to hold a fractional LP worth of RF
	private int internalRFbuffer = 0;

	private boolean addedToEnergyNet = false;

	private boolean init = false;
	private PlayerCollectionList guiListener = new PlayerCollectionList();
	private PlayerCollectionList watcherList = new PlayerCollectionList();
	private IHeadUpDisplayRenderer HUD;

	private IEnergyStorage energyInterface = new IEnergyStorage() {

		@Override
		public int receiveEnergy(int maxReceive, boolean simulate) {
			if (freeSpace() < 1) {
				return 0;
			}
			int RFspace = freeSpace() * LogisticsPowerJunctionTileEntity.RFDivisor - internalRFbuffer;
			int RFtotake = Math.min(maxReceive, RFspace);
			if (!simulate) {
				addEnergy(RFtotake / LogisticsPowerJunctionTileEntity.RFDivisor);
				internalRFbuffer += RFtotake % LogisticsPowerJunctionTileEntity.RFDivisor;
				if (internalRFbuffer >= LogisticsPowerJunctionTileEntity.RFDivisor) {
					addEnergy(1);
					internalRFbuffer -= LogisticsPowerJunctionTileEntity.RFDivisor;
				}
			}
			return RFtotake;
		}

		@Override
		public int extractEnergy(int maxExtract, boolean simulate) {
			return 0;
		}

		@Override
		public int getEnergyStored() {
			return internalStorage * LogisticsPowerJunctionTileEntity.RFDivisor + internalRFbuffer;
		}

		@Override
		public int getMaxEnergyStored() {
			return LogisticsPowerJunctionTileEntity.MAX_STORAGE * LogisticsPowerJunctionTileEntity.RFDivisor;
		}

		@Override
		public boolean canExtract() {
			return false;
		}

		@Override
		public boolean canReceive() {
			return true;
		}
	};

	private Object mjReceiver;

	public LogisticsPowerJunctionTileEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
		super(logisticspipes.LPRegistries.BE_POWER_JUNCTION.get(), pos, state);
		HUD = new HUDPowerLevel(this);
		// TODO(1.20.1): BuildCraft MJ API not ported — receiver disabled
		// mjReceiver = SimpleServiceLocator.buildCraftProxy.createMjReceiver(this);
		mjReceiver = null;
	}

	@Override
	public boolean useEnergy(int amount, List<Object> providersToIgnore) {
		if (providersToIgnore != null && providersToIgnore.contains(this)) {
			return false;
		}
		if (canUseEnergy(amount, null)) {
			this.setChanged();
			internalStorage -= (int) ((amount * Configs.POWER_USAGE_MULTIPLIER) + 0.5D);
			if (internalStorage < LogisticsPowerJunctionTileEntity.MAX_STORAGE / 2) {
				needMorePowerTriggerCheck = true;
			}
			return true;
		}
		return false;
	}

	@Override
	public boolean canUseEnergy(int amount, List<Object> providersToIgnore) {
		if (providersToIgnore != null && providersToIgnore.contains(this)) {
			return false;
		}
		return internalStorage >= (int) ((amount * Configs.POWER_USAGE_MULTIPLIER) + 0.5D);
	}

	@Override
	public boolean useEnergy(int amount) {
		return useEnergy(amount, null);
	}

	public int freeSpace() {
		return LogisticsPowerJunctionTileEntity.MAX_STORAGE - internalStorage;
	}

	public void updateClients() {
		MainProxy.sendToPlayerList(PacketHandler.getPacket(PowerJunctionLevel.class).putInt(internalStorage).setBlockPos(getBlockPos()), guiListener);
		MainProxy.sendToPlayerList(PacketHandler.getPacket(PowerJunctionLevel.class).putInt(internalStorage).setBlockPos(getBlockPos()), watcherList);
		lastUpdateStorage = internalStorage;
	}

	@Override
	public boolean canUseEnergy(int amount) {
		return canUseEnergy(amount, null);
	}

	public void addEnergy(float amount) {
		if (MainProxy.isClient(getWorld())) {
			return;
		}
		internalStorage += amount;
		if (internalStorage > LogisticsPowerJunctionTileEntity.MAX_STORAGE) {
			internalStorage = LogisticsPowerJunctionTileEntity.MAX_STORAGE;
		}
		if (internalStorage == LogisticsPowerJunctionTileEntity.MAX_STORAGE) {
			needMorePowerTriggerCheck = false;
		}
		this.setChanged();
	}

	@Override
	public void load(CompoundTag par1nbtTagCompound) {
		super.load(par1nbtTagCompound);
		internalStorage = par1nbtTagCompound.getInt("powerLevel");
		if (par1nbtTagCompound.contains("needMorePowerTriggerCheck")) {
			needMorePowerTriggerCheck = par1nbtTagCompound.getBoolean("needMorePowerTriggerCheck");
		}
	}

	@Override
	public void saveAdditional(CompoundTag par1nbtTagCompound) {
		super.saveAdditional(par1nbtTagCompound);
		par1nbtTagCompound.putInt("powerLevel", internalStorage);
		par1nbtTagCompound.putBoolean("needMorePowerTriggerCheck", needMorePowerTriggerCheck);
	}

	@Override
	public void update() {
		super.update();
		if (MainProxy.isServer(getWorld())) {
			if (internalStorage != lastUpdateStorage) {
				updateClients();
			}
		}
		if (!init) {
			if (MainProxy.isClient(getWorld())) {
				LogisticsHUDRenderer.instance().add(this);
			}
			if (!addedToEnergyNet) {
				SimpleServiceLocator.IC2Proxy.registerToEneryNet(this);
				addedToEnergyNet = true;
			}
			init = true;
		}
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		if (MainProxy.isClient(getWorld())) {
			LogisticsHUDRenderer.instance().remove(this);
		}
		if (addedToEnergyNet) {
			SimpleServiceLocator.IC2Proxy.unregisterToEneryNet(this);
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

	@Override
	@CCCommand(description = "Returns the currently stored power")
	public int getPowerLevel() {
		return internalStorage;
	}

	@Override
	public int getDisplayPowerLevel() {
		return getPowerLevel();
	}

	@Override
	public String getBrand() {
		return "LP";
	}

	@Override
	@CCCommand(description = "Returns the max. storable power")
	public int getMaxStorage() {
		return LogisticsPowerJunctionTileEntity.MAX_STORAGE;
	}

	@Override
	public int getChargeState() {
		return internalStorage * 100 / LogisticsPowerJunctionTileEntity.MAX_STORAGE;
	}

	@Override
	public void guiOpenedByPlayer(Player player) {
		guiListener.add(player);
		updateClients();
	}

	@Override
	public void guiClosedByPlayer(Player player) {
		guiListener.remove(player);
	}

	public void handlePowerPacket(int integer) {
		if (MainProxy.isClient(getWorld())) {
			internalStorage = integer;
		}
	}

	@Override
	public IHeadUpDisplayRenderer getRenderer() {
		return HUD;
	}

	@Override
	public net.minecraft.world.level.Level getLevelForHUD() {
		return getWorld();
	}

	@Override
	public int getX() {
		return getBlockPos().getX();
	}

	@Override
	public int getY() {
		return getBlockPos().getY();
	}

	@Override
	public int getZ() {
		return getBlockPos().getZ();
	}

	@Override
	public void startWatching() {
		MainProxy.sendPacketToServer(PacketHandler.getPacket(HUDStartBlockWatchingPacket.class).setPosX(getX()).setPosY(getY()).setPosZ(getZ()));
	}

	@Override
	public void stopWatching() {
		MainProxy.sendPacketToServer(PacketHandler.getPacket(HUDStopBlockWatchingPacket.class).setPosX(getX()).setPosY(getY()).setPosZ(getZ()));
	}

	@Override
	public void playerStartWatching(Player player) {
		watcherList.add(player);
		updateClients();
	}

	@Override
	public void playerStopWatching(Player player) {
		watcherList.remove(player);
	}

	@Override
	public boolean isHUDExistent() {
		return getWorld().getBlockEntity(getBlockPos()) == this;
	}

	@Override
	public void fillCrashReportCategory(CrashReportCategory par1CrashReportCategory) {
		super.fillCrashReportCategory(par1CrashReportCategory);
		par1CrashReportCategory.setDetail("LP-Version", LogisticsPipes.getVersionString());
	}

	// @Override removed — IEnergySink not in implements
	@ModDependentMethod(modId = LPConstants.ic2ModID)
	public boolean acceptsEnergyFrom(Object tile, Direction dir) { // was: IEnergyEmitter tile
		return true;
	}

	private void transferFromIC2Buffer() {
		if (freeSpace() > 0 && internalBuffer >= 1) {
			int addAmount = Math.min((int) Math.floor(internalBuffer), freeSpace());
			addEnergy(addAmount);
			internalBuffer -= addAmount;
		}
	}

	// @Override removed — IEnergySink not in implements
	@ModDependentMethod(modId = LPConstants.ic2ModID)
	public double getDemandedEnergy() {
		if (!addedToEnergyNet) {
			return 0;
		}
		transferFromIC2Buffer();
		//round up so we demand enough to completely fill visible storage
		return (freeSpace() + LogisticsPowerJunctionTileEntity.IC2Multiplier - 1) / LogisticsPowerJunctionTileEntity.IC2Multiplier;
	}

	// @Override removed — IEnergySink not in implements
	@ModDependentMethod(modId = LPConstants.ic2ModID)
	public double injectEnergy(Direction directionFrom, double amount, double voltage) {
		internalBuffer += amount * LogisticsPowerJunctionTileEntity.IC2Multiplier;
		transferFromIC2Buffer();
		return 0;
	}

	// @Override removed — IEnergySink not in implements
	@ModDependentMethod(modId = LPConstants.ic2ModID)
	public int getSinkTier() {
		return Integer.MAX_VALUE;
	}

	@Override
	public boolean isHUDInvalid() {
		return isRemoved();
	}

	/** Used by RegisterCapabilitiesEvent wiring in LPRegistries. */
	public net.minecraftforge.energy.IEnergyStorage getEnergyInterface() {
		return energyInterface;
	}

	@Override
	public CoordinatesGuiProvider getGuiProvider() {
		return NewGuiHandler.getGui(PowerJunctionGui.class);
	}
}
