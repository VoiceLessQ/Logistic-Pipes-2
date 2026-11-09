package logisticspipes.datafixer;

import java.util.Map;
import javax.annotation.Nonnull;

import net.minecraft.nbt.CompoundTag;

import com.google.common.collect.ImmutableMap;

// DataFixer for block entity ID renames. Not registered via DFU (no 1.12.2→1.20.1 upgrade path).
public class DataFixerTE {

	public static final String TYPE = "BLOCK_ENTITY"; // was FixTypes.BLOCK_ENTITY
	public static final int VERSION = 0;

	private Map<String, String> tileIDMap = ImmutableMap.<String, String>builder()
			.put("minecraft:logisticspipes.blocks.logisticssolderingtileentity", "logisticspipes:soldering_station")
			.put("minecraft:logisticspipes.blocks.powertile.logisticspowerjuntiontileentity", "logisticspipes:power_junction")
			.put("minecraft:logisticspipes.blocks.powertile.logisticsrfpowerprovidertileentity", "logisticspipes:power_provider_rf")
			.put("minecraft:logisticspipes.blocks.powertile.logisticsic2powerprovidertileentity", "logisticspipes:power_provider_ic2")
			.put("minecraft:logisticspipes.blocks.logisticssecuritytileentity", "logisticspipes:security_station")
			.put("minecraft:logisticspipes.blocks.crafting.logisticscraftingtabletileentity", "logisticspipes:logistics_crafting_table")
			.put("minecraft:logisticspipes.pipes.basic.logisticstilegenericpipe", "logisticspipes:pipe")
			.put("minecraft:logisticspipes.blocks.stats.logisticsstatisticstileentity", "logisticspipes:statistics_table")
			.put("minecraft:logisticspipes.blocks.logisticsprogramcompilertileentity", "logisticspipes:program_compiler")
			.put("minecraft:logisticspipes.pipes.basic.logisticstilegenericsubmultiblock", "logisticspipes:submultiblock")
			.build();


	public int getFixVersion() {
		return VERSION;
	}

	@Nonnull

	public CompoundTag fixTagCompound(CompoundTag compound) {
		String teName = compound.getStringOr("id", "");

		compound.putString("id", tileIDMap.getOrDefault(teName, teName));

		return compound;
	}

}
