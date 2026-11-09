package logisticspipes.logic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;

import lombok.Getter;

import logisticspipes.utils.item.SimpleStackInventory;

public class LogicController {

	/**
	 * Registry of task type names to factory functions.
	 * Concrete BaseLogicTask subclasses register themselves here so that
	 * readFromNBT() can reconstruct the right class from a saved type name.
	 */
	private static final Map<String, Function<CompoundTag, BaseLogicTask>> TASK_TYPES = new HashMap<>();

	public static void registerTaskType(String typeName, Function<CompoundTag, BaseLogicTask> factory) {
		TASK_TYPES.put(typeName, factory);
	}

	public SimpleStackInventory diskInv = new SimpleStackInventory(1, "Disk Inv", 1);

	public List<BaseLogicConnection> connections = new ArrayList<>();
	public List<BaseLogicTask> tasks = new ArrayList<>();

	private Thread oldThread = null;
	@Getter
	private boolean unresolvedTasks = false;

	public void calculate(BlockEntity tile) {
		if (oldThread != null && oldThread.isAlive()) {
			return;
		}
		for (BaseLogicTask task : tasks) {
			task.syncTick(tile);
		}
		//oldThread = new Thread() { @Override public void run() {
		for (BaseLogicConnection connection : connections) {
			if (!connection.isInvalidConnection()) {
				if (connection.getSource().getAmountOfOutput() <= connection.getSourceIndex()) {
					connection.setInvalidConnection(true);
					continue;
				}
				if (connection.getSource().getOutputParameterType(connection.getSourceIndex()) != connection.getType()) {
					connection.setInvalidConnection(true);
					continue;
				}
				if (connection.getTarget().getAmountOfInput() <= connection.getTargetIndex()) {
					connection.setInvalidConnection(true);
					continue;
				}
				if (connection.getTarget().getInputParameterType(connection.getTargetIndex()) != connection.getType()) {
					connection.setInvalidConnection(true);
					continue;
				}

			}
		}
		List<BaseLogicTask> toDos = new ArrayList<>(tasks);
		while (!toDos.isEmpty()) {
			boolean nothingDone = true;
			Iterator<BaseLogicTask> iter = toDos.iterator();
			while (iter.hasNext()) {
				BaseLogicTask task = iter.next();
				if (task.isCalculated()) {
					iter.remove();
					nothingDone = false;
				}
			}
			for (BaseLogicConnection connection : connections) {
				if (!connection.isInvalidConnection() && connection.getSource().isCalculated()) {
					connection.getTarget().setInputParameter(connection.getTargetIndex(), connection.getSource().getResult(connection.getSourceIndex()));
					nothingDone = false;
				}
			}
			if (nothingDone) {
				unresolvedTasks = true;
				return;
			}
		}
		unresolvedTasks = false;
		/*}};
		oldThread.setDaemon(true);
		oldThread.start();
		//*/
	}

	public void writeToNBT(CompoundTag nbt) {
		diskInv.writeToNBT(nbt, "LogicDiskInv");
		writeTasks(nbt);
		writeConnections(nbt);
	}

	public void readFromNBT(CompoundTag nbt) {
		diskInv.readFromNBT(nbt, "LogicDiskInv");
		readTasks(nbt);
		readConnections(nbt);
	}

	// ── Tasks ─────────────────────────────────────────────────────────────────

	private void writeTasks(CompoundTag nbt) {
		ListTag list = new ListTag();
		for (BaseLogicTask task : tasks) {
			CompoundTag entry = task.getCompoundTag();
			entry.putString("taskType", task.getTypeName());
			list.add(entry);
		}
		nbt.put("LogicTasks", list);
	}

	private void readTasks(CompoundTag nbt) {
		tasks.clear();
		ListTag list = nbt.getListOrEmpty("LogicTasks");
		for (int i = 0; i < list.size(); i++) {
			CompoundTag entry = list.getCompoundOrEmpty(i);
			String typeName = entry.getStringOr("taskType", "");
			Function<CompoundTag, BaseLogicTask> factory = TASK_TYPES.get(typeName);
			if (factory != null) {
				tasks.add(factory.apply(entry));
			}
			// Unknown task types are silently skipped; no data loss for known types.
		}
	}

	// ── Connections ───────────────────────────────────────────────────────────

	private void writeConnections(CompoundTag nbt) {
		ListTag list = new ListTag();
		for (BaseLogicConnection conn : connections) {
			CompoundTag entry = new CompoundTag();
			entry.putString("sourceUUID",  conn.getSource().getUuid().toString());
			entry.putInt   ("sourceIndex", conn.getSourceIndex());
			entry.putString("targetUUID",  conn.getTarget().getUuid().toString());
			entry.putInt   ("targetIndex", conn.getTargetIndex());
			entry.putString("type",        conn.getType().name());
			list.add(entry);
		}
		nbt.put("LogicConnections", list);
	}

	private void readConnections(CompoundTag nbt) {
		connections.clear();
		if (tasks.isEmpty()) return; // nothing to wire

		// Index tasks by UUID for fast lookup
		Map<UUID, BaseLogicTask> byUUID = new HashMap<>();
		for (BaseLogicTask task : tasks) {
			byUUID.put(task.getUuid(), task);
		}

		ListTag list = nbt.getListOrEmpty("LogicConnections");
		for (int i = 0; i < list.size(); i++) {
			CompoundTag entry = list.getCompoundOrEmpty(i);
			try {
				UUID sourceUUID = UUID.fromString(entry.getStringOr("sourceUUID", ""));
				UUID targetUUID = UUID.fromString(entry.getStringOr("targetUUID", ""));
				BaseLogicTask source = byUUID.get(sourceUUID);
				BaseLogicTask target = byUUID.get(targetUUID);
				if (source == null || target == null) continue; // dangling reference

				int sourceIndex = entry.getIntOr("sourceIndex", 0);
				int targetIndex = entry.getIntOr("targetIndex", 0);
				LogicParameterType type = LogicParameterType.valueOf(entry.getStringOr("type", ""));

				connections.add(new BaseLogicConnection(source, sourceIndex, target, targetIndex, type) {});
			} catch (IllegalArgumentException ignored) {
				// Malformed UUID or unknown LogicParameterType — skip this connection.
			}
		}
	}
}
