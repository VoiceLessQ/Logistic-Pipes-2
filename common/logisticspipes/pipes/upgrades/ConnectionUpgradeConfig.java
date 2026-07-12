package logisticspipes.pipes.upgrades;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;

import lombok.AllArgsConstructor;
import lombok.Getter;

import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.abstractguis.UpgradeCoordinatesGuiProvider;
import logisticspipes.network.guis.upgrade.DisconnectionUpgradeConfigGuiProvider;
import logisticspipes.pipes.basic.CoreRoutedPipe;

public class ConnectionUpgradeConfig implements IConfigPipeUpgrade {

	public static String getName() {
		return "disconnection";
	}

	@AllArgsConstructor
	public enum Sides {
		UP(Direction.UP, "LPDIS-UP"),
		DOWN(Direction.DOWN, "LPDIS-DOWN"),
		NORTH(Direction.NORTH, "LPDIS-NORTH"),
		SOUTH(Direction.SOUTH, "LPDIS-SOUTH"),
		EAST(Direction.EAST, "LPDIS-EAST"),
		WEST(Direction.WEST, "LPDIS-WEST");
		@Getter
		private Direction dir;
		@Getter
		private String lpName;

		public static String getNameForDirection(Direction fd) {
			Optional<Sides> opt = Arrays.stream(values()).filter(side -> side.getDir() == fd).findFirst();
			if (opt.isPresent()) {
				return opt.get().getLpName();
			}
			return "LPDIS-UNKNWON";
		}
	}

	@Override
	public boolean needsUpdate() {
		return true;
	}

	@Override
	public boolean isAllowedForPipe(CoreRoutedPipe pipe) {
		return true;
	}

	@Override
	public boolean isAllowedForModule(LogisticsModule pipe) {
		return false;
	}

	@Override
	public String[] getAllowedPipes() {
		return new String[] { "all" };
	}

	@Override
	public String[] getAllowedModules() {
		return new String[] {};
	}

	@Override
	public UpgradeCoordinatesGuiProvider getGUI() {
		return NewGuiHandler.getGui(DisconnectionUpgradeConfigGuiProvider.class);
	}

	@Nonnull
	public Stream<Direction> getSides(@Nonnull ItemStack stack) {
		if (stack.isEmpty()) return Stream.empty();
		if (!logisticspipes.utils.item.StackTag.hasTag(stack)) {
			logisticspipes.utils.item.StackTag.setTag(stack, new CompoundTag());
		}
		final CompoundTag tag = Objects.requireNonNull(logisticspipes.utils.item.StackTag.getTag(stack));
		return Arrays.stream(Sides.values()).filter(side -> tag.getBoolean(side.getLpName())).map(Sides::getDir);
	}
}
