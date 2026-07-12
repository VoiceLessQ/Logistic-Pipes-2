package logisticspipes.pipes.upgrades;

import java.util.Arrays;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;

import lombok.AllArgsConstructor;
import lombok.Getter;

import logisticspipes.modules.LogisticsModule;
import logisticspipes.modules.ModuleCreativeTabBasedItemSink;
import logisticspipes.modules.ModuleItemSink;
import logisticspipes.modules.ModuleModBasedItemSink;
import logisticspipes.modules.ModuleOreDictItemSink;
import logisticspipes.modules.ModulePolymorphicItemSink;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.abstractguis.UpgradeCoordinatesGuiProvider;
import logisticspipes.network.guis.upgrade.SneakyUpgradeConfigGuiProvider;
import logisticspipes.pipes.basic.CoreRoutedPipe;

public class SneakyUpgradeConfig implements IConfigPipeUpgrade {

	public static final String SIDE_KEY = "LPSNEAKY-SIDE";

	public static String getName() {
		return "sneaky";
	}

	@AllArgsConstructor
	public enum Sides {
		UP(Direction.UP, "LPSNEAKY-UP"),
		DOWN(Direction.DOWN, "LPSNEAKY-DOWN"),
		NORTH(Direction.NORTH, "LPSNEAKY-NORTH"),
		SOUTH(Direction.SOUTH, "LPSNEAKY-SOUTH"),
		EAST(Direction.EAST, "LPSNEAKY-EAST"),
		WEST(Direction.WEST, "LPSNEAKY-WEST");

		@Getter
		private Direction dir;
		@Getter
		private String lpName;

		public static String getNameForDirection(Direction fd) {
			return Arrays.stream(values())
					.filter(side -> side.getDir() == fd)
					.map(Sides::getLpName)
					.findFirst()
					.orElse("LPSNEAKY-UNKNWON");
		}
	}

	@Override
	public boolean needsUpdate() {
		return false;
	}

	@Override
	public boolean isAllowedForPipe(CoreRoutedPipe pipe) {
		return true;
	}

	@Override
	public boolean isAllowedForModule(LogisticsModule module) {
		return module instanceof ModuleItemSink || module instanceof ModulePolymorphicItemSink || module instanceof ModuleModBasedItemSink
				|| module instanceof ModuleOreDictItemSink || module instanceof ModuleCreativeTabBasedItemSink;
	}

	@Override
	public String[] getAllowedPipes() {
		return new String[] { "all" };
	}

	@Override
	public String[] getAllowedModules() {
		return new String[] { "itemsink" };
	}

	@Override
	public UpgradeCoordinatesGuiProvider getGUI() {
		return NewGuiHandler.getGui(SneakyUpgradeConfigGuiProvider.class);
	}

	@Nullable
	public Direction getSide(@Nonnull ItemStack stack) {
		if (stack.isEmpty()) return null;
		if (!logisticspipes.utils.item.StackTag.hasTag(stack)) {
			logisticspipes.utils.item.StackTag.setTag(stack, new CompoundTag());
		}
		CompoundTag tag = Objects.requireNonNull(logisticspipes.utils.item.StackTag.getTag(stack));
		String sideString = tag.getString(SIDE_KEY);
		return Arrays.stream(Sides.values())
				.filter(side -> side.getLpName().equals(sideString))
				.map(Sides::getDir)
				.findFirst()
				.orElse(null);
	}
}
