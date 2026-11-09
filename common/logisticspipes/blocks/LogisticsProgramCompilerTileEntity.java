package logisticspipes.blocks;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import logisticspipes.utils.TagUtil;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import logisticspipes.utils.item.CustomDataUtil;
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
import net.minecraft.resources.Identifier;

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

		public static final Identifier BASIC = Identifier.fromNamespaceAndPath("logisticspipes", "compilercategory.basic");
		public static final Identifier TIER_2 = Identifier.fromNamespaceAndPath("logisticspipes", "compilercategory.tier_2");
		public static final Identifier FLUID = Identifier.fromNamespaceAndPath("logisticspipes", "compilercategory.fluid");
		public static final Identifier TIER_3 = Identifier.fromNamespaceAndPath("logisticspipes", "compilercategory.tier_3");
		public static final Identifier CHASSIS = Identifier.fromNamespaceAndPath("logisticspipes", "compilercategory.chassis");
		public static final Identifier CHASSIS_2 = Identifier.fromNamespaceAndPath("logisticspipes", "compilercategory.chassis_2");
		public static final Identifier CHASSIS_3 = Identifier.fromNamespaceAndPath("logisticspipes", "compilercategory.chassis_3");
		public static final Identifier MODDED = Identifier.fromNamespaceAndPath("logisticspipes", "compilercategory.modded");

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

	public static final Map<Identifier, Set<Identifier>> programByCategory = new LinkedHashMap<>();
	private final PlayerCollectionList playerList = new PlayerCollectionList();
	private String taskType = "";
	@Getter
	private Identifier currentTask = null;
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
		return CustomDataUtil.getOrCreateTag(this.getInventory().getItem(0)).getListOrEmpty(key);
	}

	private void addToListTagForKey(String key, String value) {
		CustomData.update(DataComponents.CUSTOM_DATA, this.getInventory().getItem(0), nbt -> {
			ListTag list = nbt.getListOrEmpty(key);
			list.add(StringTag.valueOf(value));
			nbt.put(key, list);
		});
	}

	public void triggerNewTask(Identifier category, String taskType) {
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
							addToListTagForKey("compilerCategories", currentTask.toString());
							break;
						}
						case "program": {
							addToListTagForKey("compilerPrograms", currentTask.toString());
							break;
						}
						case "flash":
							if (!getInventory().getItem(1).isEmpty()) {
								ItemStack programmer = getInventory().getItem(1);
								CustomData.update(DataComponents.CUSTOM_DATA, programmer,
										nbt -> nbt.putString(ItemLogisticsProgrammer.RECIPE_TARGET, currentTask.toString()));
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
	protected void loadAdditional(ValueInput input) {
		CompoundTag nbt = TagUtil.read(input);
		inventory.readFromNBT(nbt, "programcompilerinv");
		super.loadAdditional(input);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		CompoundTag nbt = new CompoundTag();
		inventory.writeToNBT(nbt, "programcompilerinv");
		super.saveAdditional(output);
		TagUtil.write(output, nbt);
	}
}
