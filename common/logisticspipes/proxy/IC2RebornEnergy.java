package logisticspipes.proxy;

import java.util.Set;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.fabricmc.loader.api.FabricLoader;

import de.nexiii.ic2reborn.block.entity.BasicMachineBlockEntity;
import de.nexiii.ic2reborn.registry.Ic2RebornBlocks;

/**
 * IC2 Reborn has no energy API: its cables only deliver to its own final BasicMachineBlockEntity.
 * LP blocks therefore pull EU from adjacent IC2 Reborn generators, storage units and transformers.
 */
public final class IC2RebornEnergy {

	public static final boolean LOADED = FabricLoader.getInstance().isModLoaded("ic2reborn");

	private IC2RebornEnergy() {}

	/** Takes up to maxEU from adjacent IC2 Reborn emitters and returns the amount taken. */
	public static int pull(BlockEntity self, int maxEU) {
		if (!LOADED || maxEU <= 0 || self.getLevel() == null || self.getLevel().isClientSide()) return 0;
		return Impl.pull(self, maxEU);
	}

	private static final class Impl {

		private static final Set<Block> EMITTERS = Set.of(Ic2RebornBlocks.GENERATOR, Ic2RebornBlocks.GEOTHERMAL_GENERATOR, Ic2RebornBlocks.SOLAR_PANEL,
				Ic2RebornBlocks.WATER_MILL, Ic2RebornBlocks.WIND_MILL, Ic2RebornBlocks.NUCLEAR_REACTOR, Ic2RebornBlocks.BATBOX, Ic2RebornBlocks.MFE_UNIT,
				Ic2RebornBlocks.MFSU, Ic2RebornBlocks.LV_TRANSFORMER, Ic2RebornBlocks.MV_TRANSFORMER, Ic2RebornBlocks.HV_TRANSFORMER);

		static int pull(BlockEntity self, int maxEU) {
			int taken = 0;
			for (Direction dir : Direction.values()) {
				if (!(self.getLevel().getBlockEntity(self.getBlockPos().relative(dir)) instanceof BasicMachineBlockEntity machine)) continue;
				if (!EMITTERS.contains(machine.getBlockState().getBlock()) || !machine.emitsEnergyTo(dir.getOpposite())) continue;
				int got = machine.energy().extract(maxEU - taken, false);
				if (got > 0) {
					machine.setChanged();
					taken += got;
					if (taken >= maxEU) break;
				}
			}
			return taken;
		}
	}
}
