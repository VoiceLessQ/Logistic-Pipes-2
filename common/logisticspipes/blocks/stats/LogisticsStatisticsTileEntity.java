package logisticspipes.blocks.stats;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import logisticspipes.utils.TagUtil;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.CompoundTag;

import logisticspipes.blocks.LogisticsSolidTileEntity;
import logisticspipes.interfaces.IGuiTileEntity;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.abstractguis.CoordinatesGuiProvider;
import logisticspipes.network.guis.block.StatisticsGui;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import network.rs485.logisticspipes.connection.NeighborTileEntity;
import network.rs485.logisticspipes.world.WorldCoordinatesWrapper;

public class LogisticsStatisticsTileEntity extends LogisticsSolidTileEntity implements IGuiTileEntity {

	public LogisticsStatisticsTileEntity(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
		super(logisticspipes.LPRegistries.BE_STATISTICS_TABLE.get(), pos, state);
	}

	public List<TrackingTask> tasks = new ArrayList<>();
	private int tickCount;
	private CoreRoutedPipe cachedConnectedPipe;

	@Override
	public void notifyOfBlockChange() {
		super.notifyOfBlockChange();
		cachedConnectedPipe = null;
	}

	@Override
	public void update() {
		if (MainProxy.isClient(getWorld())) {
			return;
		}
		tickCount++;
		if (getConnectedPipe() == null) {
			return;
		}
		for (TrackingTask task : tasks) {
			task.tick(tickCount, getConnectedPipe());
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		CompoundTag nbt = TagUtil.read(input);
		super.loadAdditional(input);
		int size = nbt.getIntOr("taskSize", 0);
		for (int i = 0; i < size; i++) {
			CompoundTag tag = (CompoundTag) nbt.get("Task_" + i);
			TrackingTask task = new TrackingTask();
			task.readFromNBT(tag);
			tasks.add(task);
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		CompoundTag nbt = new CompoundTag();
		super.saveAdditional(output);
		nbt.putInt("taskSize", tasks.size());
		int count = 0;
		for (TrackingTask task : tasks) {
			CompoundTag tag = new CompoundTag();
			task.writeToNBT(tag);
			nbt.put("Task_" + count, tag);
			count++;
		}
		TagUtil.write(output, nbt);
	}

	@Override
	public CoordinatesGuiProvider getGuiProvider() {
		return NewGuiHandler.getGui(StatisticsGui.class).setTrackingList(tasks);
	}

	public CoreRoutedPipe getConnectedPipe() {
		if (cachedConnectedPipe == null) {
			new WorldCoordinatesWrapper(this).allNeighborTileEntities().stream()
					.filter(NeighborTileEntity::isLogisticsPipe)
					.filter(adjacent -> ((LogisticsTileGenericPipe) adjacent.getTileEntity()).pipe instanceof CoreRoutedPipe)
					.map(adjacent -> (CoreRoutedPipe) (((LogisticsTileGenericPipe) adjacent.getTileEntity()).pipe))
					.findFirst()
					.ifPresent(coreRoutedPipe -> cachedConnectedPipe = coreRoutedPipe);
		}
		return cachedConnectedPipe;
	}
}
