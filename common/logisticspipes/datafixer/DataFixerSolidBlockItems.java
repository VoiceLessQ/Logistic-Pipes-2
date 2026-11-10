package logisticspipes.datafixer;

import net.minecraft.core.registries.BuiltInRegistries;

import javax.annotation.Nonnull;

import net.minecraft.nbt.CompoundTag;

import logisticspipes.items.LogisticsSolidBlockItem;

// DataFixer for solid block item meta→id migration. Not registered via DFU (no 1.12.2→1.20.1 upgrade path).
public class DataFixerSolidBlockItems {

	public static final String TYPE = "ITEM_INSTANCE"; // was FixTypes.ITEM_INSTANCE
	public static final int VERSION = 1;


	public int getFixVersion() {
		return VERSION;
	}

	@Nonnull

	public CompoundTag fixTagCompound(CompoundTag compound) {
		if (
				!compound.getString("id").equals("logisticspipes:solid_block") &&
						!compound.getString("id").equals("logisticspipes:tile.logisticssolidblock")
		) return compound;

		int meta = compound.getShortOr("Damage", (short) 0);
		compound.remove("Damage");
		compound.putString("id", BuiltInRegistries.ITEM.getKey(LogisticsSolidBlockItem.updateItemMap.get(meta)).toString());

		return compound;
	}
}
