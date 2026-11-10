package logisticspipes.utils;

import javax.annotation.Nonnull;

import net.minecraft.world.item.ItemStack;

public enum MinecraftColor {
	BLACK(0xff000000),
	RED(0xffff0000),
	GREEN(0xff00ff00),
	BROWN(0xff895836),
	BLUE(0xff0000ff),
	PURPLE(0xffB064D8),
	CYAN(0xff3C8EB0),
	LIGHT_GRAY(0xffBABAC1),
	GRAY(0xff848484),
	PINK(0xffF7B4D6),
	LIME(0xff83D41C),
	YELLOW(0xffE7E72A),
	LIGHT_BLUE(0xff82ACE7),
	MAGENTA(0xffDB7AD5),
	ORANGE(0xffE69E34),
	WHITE(0xffffffff),
	BLANK(0x00000000);

	private final int colorCode;

	MinecraftColor(int colorCode) {
		this.colorCode = colorCode;
	}

	// In 1.20.1 dyes are separate items; map via DyeItem / DyeColor
	private static final java.util.Map<net.minecraft.world.item.Item, MinecraftColor> DYE_TO_COLOR;
	static {
		DYE_TO_COLOR = new java.util.HashMap<>();
		for (MinecraftColor color : values()) {
			if (color != BLANK) {
				net.minecraft.world.item.Item dyeItem = dyeItem(color.toDyeColor());
				DYE_TO_COLOR.put(dyeItem, color);
			}
		}
	}

	private net.minecraft.world.item.DyeColor toDyeColor() {
		return switch (this) {
			case BLACK -> net.minecraft.world.item.DyeColor.BLACK;
			case RED -> net.minecraft.world.item.DyeColor.RED;
			case GREEN -> net.minecraft.world.item.DyeColor.GREEN;
			case BROWN -> net.minecraft.world.item.DyeColor.BROWN;
			case BLUE -> net.minecraft.world.item.DyeColor.BLUE;
			case PURPLE -> net.minecraft.world.item.DyeColor.PURPLE;
			case CYAN -> net.minecraft.world.item.DyeColor.CYAN;
			case LIGHT_GRAY -> net.minecraft.world.item.DyeColor.LIGHT_GRAY;
			case GRAY -> net.minecraft.world.item.DyeColor.GRAY;
			case PINK -> net.minecraft.world.item.DyeColor.PINK;
			case LIME -> net.minecraft.world.item.DyeColor.LIME;
			case YELLOW -> net.minecraft.world.item.DyeColor.YELLOW;
			case LIGHT_BLUE -> net.minecraft.world.item.DyeColor.LIGHT_BLUE;
			case MAGENTA -> net.minecraft.world.item.DyeColor.MAGENTA;
			case ORANGE -> net.minecraft.world.item.DyeColor.ORANGE;
			case WHITE -> net.minecraft.world.item.DyeColor.WHITE;
			default -> throw new IllegalStateException("No DyeColor for " + this);
		};
	}

	public static MinecraftColor getColor(@Nonnull ItemStack item) {
		if (!item.isEmpty()) {
			MinecraftColor color = DYE_TO_COLOR.get(item.getItem());
			if (color != null) return color;
		}
		return BLANK;
	}

	public int getColorCode() {
		return colorCode;
	}

	@Nonnull
	public ItemStack getItemStack() {
		if (this == BLANK) {
			return ItemStack.EMPTY;
		}
		return new ItemStack(dyeItem(toDyeColor()));
	}

	public MinecraftColor getNext() {
		if (this == BLANK) {
			return BLACK;
		}
		return MinecraftColor.values()[ordinal() + 1];
	}

	public MinecraftColor getPrev() {
		if (this == BLACK) {
			return BLANK;
		}
		return MinecraftColor.values()[ordinal() - 1];
	}

	private static net.minecraft.world.item.Item dyeItem(net.minecraft.world.item.DyeColor color) {
		return net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.withDefaultNamespace(color.getName() + "_dye"));
	}
}
