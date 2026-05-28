package logisticspipes.network.guis.block;

import net.minecraft.world.entity.player.Player;

import logisticspipes.blocks.powertile.LogisticsPowerProviderTileEntity;
import logisticspipes.gui.GuiPowerProvider;
import logisticspipes.network.abstractguis.CoordinatesGuiProvider;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.gui.DummyContainer;

@StaticResolve
public class PowerProviderGui extends CoordinatesGuiProvider {

	public PowerProviderGui(int id) {
		super(id);
	}

	@Override
	public Object getClientGui(Player player) {
		return new GuiPowerProvider(player, getTileAs(player.level(), LogisticsPowerProviderTileEntity.class));
	}

	@Override
	public DummyContainer getContainer(Player player) {
		DummyContainer dummy = new DummyContainer(player, null, getTileAs(player.level(), LogisticsPowerProviderTileEntity.class));
		dummy.addNormalSlotsForPlayerInventory(8, 80);
		return dummy;
	}

	@Override
	public GuiProvider template() {
		return new PowerProviderGui(getId());
	}
}
