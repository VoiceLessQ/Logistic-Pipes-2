package logisticspipes.network.guis.pipe;

import net.minecraft.world.entity.player.Player;

import logisticspipes.LPItems;
import logisticspipes.gui.GuiFreqCardContent;
import logisticspipes.items.LogisticsItemCard;
import logisticspipes.network.abstractguis.CoordinatesGuiProvider;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.pipes.PipeItemsSystemDestinationLogistics;
import logisticspipes.pipes.PipeItemsSystemEntranceLogistics;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.gui.DummyContainer;

@StaticResolve
public class FreqCardGui extends CoordinatesGuiProvider {

	public FreqCardGui(int id) {
		super(id);
	}

	private CoreUnroutedPipe getFreqPipe(Player player) {
		LogisticsTileGenericPipe tile = getTileAs(player.level(), LogisticsTileGenericPipe.class);
		if (tile == null) return null;
		if (tile.pipe instanceof PipeItemsSystemEntranceLogistics
				|| tile.pipe instanceof PipeItemsSystemDestinationLogistics) {
			return tile.pipe;
		}
		return null;
	}

	@Override
	public Object getClientGui(Player player) {
		CoreUnroutedPipe pipe = getFreqPipe(player);
		if (pipe instanceof PipeItemsSystemEntranceLogistics entrance) {
			return new GuiFreqCardContent(player, entrance.inv);
		}
		if (pipe instanceof PipeItemsSystemDestinationLogistics destination) {
			return new GuiFreqCardContent(player, destination.inv);
		}
		return null;
	}

	@Override
	public DummyContainer getContainer(Player player) {
		CoreUnroutedPipe pipe = getFreqPipe(player);
		if (pipe == null) return null;
		var inv = pipe instanceof PipeItemsSystemEntranceLogistics e ? e.inv
				: ((PipeItemsSystemDestinationLogistics) pipe).inv;
		DummyContainer dummy = new DummyContainer(player.getInventory(), inv);
		dummy.addRestrictedSlot(0, inv, 82, 15, itemStack ->
				!itemStack.isEmpty() && itemStack.getItem() == LPItems.itemCard.get()
						&& LogisticsItemCard.getCardType(itemStack) == LogisticsItemCard.FREQ_CARD);
		dummy.addNormalSlotsForPlayerInventory(10, 45);
		return dummy;
	}

	@Override
	public GuiProvider template() {
		return new FreqCardGui(getId());
	}
}
