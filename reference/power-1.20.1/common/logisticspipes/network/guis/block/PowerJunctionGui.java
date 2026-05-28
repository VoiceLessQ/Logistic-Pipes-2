package logisticspipes.network.guis.block;

import net.minecraft.world.entity.player.Player;

import logisticspipes.blocks.powertile.LogisticsPowerJunctionTileEntity;
import logisticspipes.gui.GuiPowerJunction;
import logisticspipes.network.abstractguis.CoordinatesGuiProvider;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.gui.DummyContainer;

@StaticResolve
public class PowerJunctionGui extends CoordinatesGuiProvider {

	public PowerJunctionGui(int id) {
		super(id);
	}

	@Override
	public Object getClientGui(Player player) {
		return new GuiPowerJunction(player, getTileAs(player.level(), LogisticsPowerJunctionTileEntity.class));
	}

	@Override
	public DummyContainer getContainer(Player player) {
		DummyContainer dummy = new DummyContainer(player, null, getTileAs(player.level(), LogisticsPowerJunctionTileEntity.class));
		dummy.addNormalSlotsForPlayerInventory(8, 80);
		return dummy;
	}

	@Override
	public GuiProvider template() {
		return new PowerJunctionGui(getId());
	}
}
