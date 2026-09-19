
package logisticspipes.gui;

import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;

import logisticspipes.LPItems;
import logisticspipes.items.LogisticsItemCard;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import javax.annotation.Nonnull;

public class GuiFreqCardContent extends LogisticsBaseGuiScreen {

	public GuiFreqCardContent(Player player, Container card) {
		super(buildDummy(player, card), 180, 130, 0, 0);
	}
	private static DummyContainer buildDummy(Player player, Container card) {
		DummyContainer dummy = new DummyContainer(player.getInventory(), card);
		dummy.addRestrictedSlot(0, card, 82, 15, itemStack ->
				!itemStack.isEmpty() && itemStack.getItem() == LPItems.itemCard.get() && LogisticsItemCard.getCardType(itemStack) == LogisticsItemCard.FREQ_CARD);
		dummy.addNormalSlotsForPlayerInventory(10, 45);
		return dummy;
	}


	@Override
	protected void renderBg(@Nonnull GuiGraphics guiGraphics, float var1, int var2, int var3) {
		LPGuiGraphics.drawGuiBackGround(minecraft, leftPos, topPos, right, bottom, 0.0f, true);
		LPGuiGraphics.drawPlayerInventoryBackground(minecraft, leftPos + 10, topPos + 45);
		LPGuiGraphics.drawSlotBackground(minecraft, leftPos + 81, topPos + 14);
	}

}
