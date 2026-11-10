package logisticspipes.recipes;

import javax.annotation.Nonnull;

import net.minecraft.world.item.Item;

import lombok.Data;

@Data
public class CraftingParts {

	/**
	 * Iron Chip
	 * FPGA
	 */
	@Nonnull
	private final Item chipFpga;
	/**
	 * Gold Chip
	 * Basic Microcontroller
	 */
	@Nonnull
	private final Item chipBasic;
	/**
	 * Diamond Chip
	 * Advanced Microcontroller
	 */
	@Nonnull
	private final Item chipAdvanced;
}
