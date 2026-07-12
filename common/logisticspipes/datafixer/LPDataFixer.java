package logisticspipes.datafixer;

import net.neoforged.neoforge.common.NeoForge;
// Full DFU (DataFixerUpper) registration is not feasible for the 1.12.2→1.20.1 gap:
// Minecraft's own chunk format requires passing through every intermediate MC version.
// What IS covered by MissingMappingsEvent (fired on the Forge bus):
//   • Item registry renames        — MissingMappingHandler (ITEMS, namespace "logisticspipes")
//   • Block registry renames       — MissingMappingHandler (BLOCKS, namespace "logisticspipes")
//   • Block entity type renames    — MissingMappingHandler (BLOCK_ENTITY_TYPES, namespace "minecraft")
// What remains unhandled:
//   • Item NBT damage→id migration — DataFixerSolidBlockItems.fixTagCompound() exists but is
//     not called; requires a DFU DataFixTypes.ITEM_STACK fixer or ChunkDataEvent.Load walk.

public class LPDataFixer {

	public static final LPDataFixer INSTANCE = new LPDataFixer();

	public static final int VERSION = 1;

	private LPDataFixer() {}

	public void init() {
		// MissingMappingsEvent does NOT implement IModBusEvent — it fires on the Forge bus.
		NeoForge.EVENT_BUS.register(new MissingMappingHandler());
	}

}
