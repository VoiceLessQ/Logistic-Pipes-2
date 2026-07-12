package logisticspipes;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class LPCapabilities {

	private LPCapabilities() {}

	public static void register(RegisterCapabilitiesEvent event) {
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, LPRegistries.BE_PIPE.get(),
				(be, side) -> be.getItemHandlerForSide(side));
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, LPRegistries.BE_PIPE.get(),
				(be, side) -> be.getFluidHandlerForSide(side));
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, LPRegistries.BE_POWER_PROVIDER_RF.get(),
				(be, side) -> be.getEnergyInterface());
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, LPRegistries.BE_POWER_JUNCTION.get(),
				(be, side) -> be.getEnergyInterface());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, LPRegistries.BE_CRAFTING_TABLE.get(),
				(be, side) -> be.getInvWrapper());
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, LPRegistries.BE_CREATIVE_POWER_SOURCE.get(),
				(be, side) -> be);
	}
}
