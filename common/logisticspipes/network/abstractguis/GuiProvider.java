package logisticspipes.network.abstractguis;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

import net.neoforged.neoforge.common.util.FakePlayer;

import lombok.Getter;

import logisticspipes.network.NewGuiHandler;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

public abstract class GuiProvider {

	@Getter
	private final int id;

	public GuiProvider(int id) {
		this.id = id;
	}

	public void writeData(LPDataOutput output) {}

	public void readData(LPDataInput input) {}

	/**
	 * @return LogisticsBaseGuiScreen
	 */
	public abstract Object getClientGui(Player player);

	public abstract AbstractContainerMenu getContainer(Player player);

	public abstract GuiProvider template();

	public final void open(Player player) {
		if (player instanceof FakePlayer) return;
		NewGuiHandler.openGui(this, player);
	}
}
