package logisticspipes.datafixer;

import net.neoforged.neoforge.common.NeoForge;
// Full DFU (DataFixerUpper) registration is not feasible for the 1.12.2 to 1.21 gap:
// Minecraft's own chunk format requires passing through every intermediate MC version.
// NeoForge 21.1 has no MissingMappingsEvent, so the item/block/BE rename maps in
// MissingMappingHandler are currently dead data. What actually runs:
//   ChunkDataEvent.Load walk (MissingMappingHandler.onChunkLoad) rewriting the raw
//   solid_block item NBT via DataFixerSolidBlockItems before MC deserializes it.
// Registry renames need a DFU fixer or a wider chunk NBT walk; see MIGRATION.md.

public class LPDataFixer {

	public static final LPDataFixer INSTANCE = new LPDataFixer();

	public static final int VERSION = 1;

	private LPDataFixer() {}

	public void init() {
		// ChunkDataEvent fires on the game bus, not the mod bus.
		NeoForge.EVENT_BUS.register(new MissingMappingHandler());
	}

}
