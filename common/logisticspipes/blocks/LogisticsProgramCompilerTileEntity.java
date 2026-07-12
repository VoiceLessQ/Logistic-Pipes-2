package logisticspipes.blocks;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

import lombok.Getter;

import logisticspipes.config.Configs;
import logisticspipes.interfaces.IGuiOpenControler;
import logisticspipes.interfaces.IGuiTileEntity;
import logisticspipes.items.ItemLogisticsProgrammer;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractguis.CoordinatesGuiProvider;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.guis.block.ProgramCompilerGui;
import logisticspipes.network.packets.block.CompilerStatusPacket;
import logisticspipes.pipes.PipeItemsBasicLogistics;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.item.SimpleStackInventory;
import network.rs485.logisticspipes.world.CoordinateUtils;
import network.rs485.logisticspipes.world.DoubleCoordinates;

public class LogisticsProgramCompilerTileEntity extends LogisticsSolidTileEntity implements IGuiTileEntity, IGuiOpenControler {

	public static class ProgrammCategories {

		public static final ResourceLocation BASIC = ResourceLocation.fromNamespaceAndPath("logisticspipes", "compilercategory.basic");
		public static final ResourceLocation TIER_2 = ResourceLocation.fromNamespaceAndPath("logisticspipes", "compilercategory.tier_2");
		public static final ResourceLocation FLUID = ResourceLocation.fromNamespaceAndPath("logisticspipes", "compilercategory.fluid");
		public static final ResourceLocation TIER_3 = ResourceLocation.fromNamespaceAndPath("logisticspipes", "compilercategory.tier_3");
		public static final ResourceLocation CHASSIS = ResourceLocation.fromNamespaceAndPath("logisticspipes", "compilercategory.chassis");
		public static final ResourceLocation CHASSIS_2 = ResourceLocation.fromNamespaceAndPath("logisticspipes", "compilercategory.chassis_2");
		public static final ResourceLocation CHASSIS_3 = ResourceLocation.fromNamespaceAndPath("logisticspipes", "compilercategory.chassis_3");
		public static final ResourceLocation MODDED = ResourceLocation.fromNamespaceAndPath("logisticspipes", "compilercategory.modded");

		static {
			//Force the order of keys
			programByCategory.put(BASIC, new HashSet<>());
			programByCategory.put(TIER_2, new HashSet<>());
			programByCategory.put(FLUID, new HashSet<>());
			programByCategory.put(TIER_3, new HashSet<>());
			programByCategory.put(CHASSIS, new HashSet<>());
			programByCategory.put(CHASSIS_2, new HashSet<>());
			programByCategory.put(CHASSIS_3, new HashSet<>());
			programByCategory.put(MODDED, new HashSet<>());
		}
	}

	public LogisticsProgramCompilerTileEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
		super(logisticspipes.LPRegistries.BE_PROGRAM_COMPILER.get(), pos, state);
	}

	public static final Map<ResourceLocation, Set<ResourceLocation>> programByCategory = new LinkedHashMap<>();
	private final PlayerCollectionList playerList = new PlayerCollectionList();
	private String taskType = "";
	@Getter
	private ResourceLocation currentTask = null;
	@Getter
	private double taskProgress = 0;
	@Getter
	private boolean wasAbleToConsumePower = false;

	@Getter
	private SimpleStackInventory inventory = new SimpleStackInventory(2, "programcompilerinv", 64);

	@Override
	public CoordinatesGuiProvider getGuiProvider() {
		return NewGuiHandler.getGui(ProgramCompilerGui.class);
	}

	public ListTag getListTagForKey(String key) {
		CompoundTag nbt = logisticspipes.utils.item.StackTag.getTag(this.getInventory().getItem(0));
		if (nbt == null) {
			logisticspipes.utils.item.StackTag.setTag(this.getInventory().getItem(0), new CompoundTag());
			nbt = logisticspipes.utils.item.StackTag.getTag(this.getInventory().getItem(0));
		}

		if (!nbt.contains(key)) {
			ListTag list = new ListTag();
			nbt.put(key, list);
		}
		return nbt.getList(key, 8 /* String */);
	}

	public void triggerNewTask(ResourceLocation category, String taskType) {
		if (currentTask != null) return;
		this.taskType = taskType;
		currentTask = category;
		taskProgress = 0;
		wasAbleToConsumePower = true;
		updateClient();
	}

	@Override
	public void guiOpenedByPlayer(Player player) {
		playerList.add(player);
		MainProxy.sendPacketToPlayer(getClientUpdatePacket(), player);
	}

	private CoordinatesPacket getClientUpdatePacket() {
		return PacketHandler.getPacket(CompilerStatusPacket.class)
				.setCategory(currentTask)
				.setProgress(taskProgress)
				.setWasAbleToConsumePower(wasAbleToConsumePower)
				.setDisk(getInventory().getItem(0))
				.setProgrammer(getInventory().getItem(1))
				.setTilePos(this);
	}

	@Override
	public void guiClosedByPlayer(Player player) {
		playerList.remove(player);
	}

	@Override
	public void update() {
		super.update();
		if (MainProxy.isServer(getWorld())) {
			if (currentTask != null) {
				wasAbleToConsumePower = false;
				for (Direction dir : Direction.values()) {
					if (dir == Direction.UP) continue;
					DoubleCoordinates pos = CoordinateUtils.add(new DoubleCoordinates(this), dir);
					BlockEntity tile = pos.getTileEntity(getWorld());
					if (!(tile instanceof LogisticsTileGenericPipe)) {
						continue;
					}
					LogisticsTileGenericPipe tPipe = (LogisticsTileGenericPipe) tile;
					if (!(tPipe.pipe.getClass() == PipeItemsBasicLogistics.class)) {
						continue;
					}
					CoreRoutedPipe pipe = (CoreRoutedPipe) tPipe.pipe;
					if (pipe.useEnergy(10)) {
						switch (taskType) {
							case "category":
								taskProgress += 0.0005 * Configs.COMPILER_SPEED;
								break;
							case "program":
								taskProgress += 0.0025 * Configs.COMPILER_SPEED;
								break;
							case "flash":
								taskProgress += 0.01 * Configs.COMPILER_SPEED;
								break;
							default:
								taskProgress = 1;
								break;
						}
						wasAbleToConsumePower = true;
					}
				}
				if (taskProgress >= 1) {
					switch (taskType) {
						case "category": {
							ListTag list = getListTagForKey("compilerCategories");
							list.add(StringTag.valueOf(currentTask.toString()));
							break;
						}
						case "program": {
							ListTag list = getListTagForKey("compilerPrograms");
							list.add(StringTag.valueOf(currentTask.toString()));
							break;
						}
						case "flash":
							if (!getInventory().getItem(1).isEmpty()) {
								ItemStack programmer = getInventory().getItem(1);
								if (!logisticspipes.utils.item.StackTag.hasTag(programmer)) {
									logisticspipes.utils.item.StackTag.setTag(programmer, new CompoundTag());
								}
								assert logisticspipes.utils.item.StackTag.getTag(programmer) != null;
								logisticspipes.utils.item.StackTag.getTag(programmer)
										.putString(ItemLogisticsProgrammer.RECIPE_TARGET, currentTask.toString());
							}
							break;
						default:
							throw new UnsupportedOperationException(taskType);
					}

					taskType = "";
					currentTask = null;
					taskProgress = 0;
					wasAbleToConsumePower = false;
				}
				updateClient();
			}
		}
	}

	public void updateClient() {
		MainProxy.sendToPlayerList(getClientUpdatePacket(), playerList);
	}

	@Override
	public void onBlockBreak() {
		inventory.dropContents(level, getBlockPos());
	}

	public void setStateOnClient(CompilerStatusPacket compilerStatusPacket) {
		getInventory().setItem(0, compilerStatusPacket.getDisk());
		getInventory().setItem(1, compilerStatusPacket.getProgrammer());
		currentTask = compilerStatusPacket.getCategory();
		taskProgress = compilerStatusPacket.getProgress();
		wasAbleToConsumePower = compilerStatusPacket.isWasAbleToConsumePower();
	}

	@Override
	protected void loadAdditional(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
		inventory.readFromNBT(nbt, "programcompilerinv");
		super.loadAdditional(nbt, registries);
	}

	@Override
	protected void saveAdditional(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
		inventory.writeToNBT(nbt, "programcompilerinv");
		super.saveAdditional(nbt, registries);
	}
}
