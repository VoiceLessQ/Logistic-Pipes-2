
package logisticspipes.gui;

import logisticspipes.utils.gui.SimpleGraphics;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;

import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.utils.item.ItemIdentifier;
import network.rs485.logisticspipes.util.TextUtil;
import javax.annotation.Nonnull;

public class GuiFluidBasic extends LogisticsBaseGuiScreen {

	public GuiFluidBasic(Player player, Container inventory) {
		super(buildDummy(player, inventory), 180, 130, 0, 0);
	}
	private static DummyContainer buildDummy(Player player, Container inventory) {
		DummyContainer dummy = new DummyContainer(player.getInventory(), inventory);
		dummy.addFluidSlot(0, inventory, 28, 13);
		dummy.addNormalSlotsForPlayerInventory(10, 45);
		return dummy;
	}


	@Override
	public void init() {
		super.init();
	}

	@Override
	protected void renderBg(@Nonnull GuiGraphicsExtractor guiGraphics, float var1, int var2, int var3) {
		LPGuiGraphics.drawGuiBackGround(minecraft, leftPos, topPos, right, bottom, 0.0f, true);
		LPGuiGraphics.drawPlayerInventoryBackground(minecraft, leftPos + 10, topPos + 45);
		LPGuiGraphics.drawSlotBackground(minecraft, leftPos + 27, topPos + 12);
	}

	@Override
	protected void renderLabels(GuiGraphicsExtractor guiGraphics, int par1, int par2) {
		super.renderLabels(guiGraphics, par1, par2);
		if (getMenu().getSlot(0).getItem() == null) {
			SimpleGraphics.drawString(guiGraphics, minecraft.font, TextUtil.translate("gui.fluidbasic.Empty"), 50, 18, 0x404040, false);
		} else {
			SimpleGraphics.drawString(guiGraphics, minecraft.font, ItemIdentifier.get(getMenu().getSlot(0).getItem()).getFriendlyName(), 50, 18, 0x404040, false);
		}
	}
}
