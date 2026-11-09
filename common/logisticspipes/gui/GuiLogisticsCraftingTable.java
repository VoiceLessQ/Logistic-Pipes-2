package logisticspipes.gui;

import java.util.Arrays;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.Player;

import logisticspipes.blocks.crafting.LogisticsCraftingTableTileEntity;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.block.CraftingCycleRecipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.item.ItemStackRenderer;
import logisticspipes.utils.item.ItemStackRenderer.DisplayAmount;
import javax.annotation.Nonnull;

public class GuiLogisticsCraftingTable extends LogisticsBaseGuiScreen {

	public LogisticsCraftingTableTileEntity _crafter;

	private int fuzzyPanelSelection = -1;
	private int fuzzyPanelHover = -1;
	private int fuzzyPanelHoverTime = 0;

	private net.minecraft.client.gui.components.AbstractButton[] cycleButtons = new net.minecraft.client.gui.components.AbstractButton[2];

	public GuiLogisticsCraftingTable(Player player, LogisticsCraftingTableTileEntity crafter) {
		super(buildDummy(player, crafter), 176, 218, 0, 0);
		_crafter = crafter;
		((DummyContainer) this.menu).guiHolderForJEI = this;
	}
	private static DummyContainer buildDummy(Player player, LogisticsCraftingTableTileEntity crafter) {
		DummyContainer dummy = new DummyContainer(player.getInventory(), crafter.matrix);

		for (int x = 0; x < 3; x++) {
			for (int y = 0; y < 3; y++) {
				if (crafter.isFuzzy()) {
					dummy.addFuzzyDummySlot(y * 3 + x, 35 + x * 18, 10 + y * 18, crafter.inputFuzzy(y * 3 + x));
				} else {
					dummy.addDummySlot(y * 3 + x, 35 + x * 18, 10 + y * 18);
				}
			}
		}
		if (crafter.isFuzzy()) {
			dummy.addFuzzyUnmodifiableSlot(0, crafter.resultInv, 125, 28, crafter.outputFuzzy());
		} else {
			dummy.addUnmodifiableSlot(0, crafter.resultInv, 125, 28);
		}
		for (int y = 0; y < 2; y++) {
			for (int x = 0; x < 9; x++) {
				dummy.addNormalSlot(y * 9 + x, crafter.inv, 8 + x * 18, 80 + y * 18);
			}
		}
		dummy.addNormalSlotsForPlayerInventory(8, 135);
		return dummy;
	}


	@Override
	public void init() {
		super.init();
		SmallGuiButton upBtn = new SmallGuiButton(0, leftPos + 144, topPos + 25, 15, 10, "/\\");
		upBtn.setPressListener(b -> MainProxy.sendPacketToServer(PacketHandler.getPacket(CraftingCycleRecipe.class).setDown(false).setTilePos(_crafter)));
		(cycleButtons[0] = addRenderableWidget(upBtn)).visible = false;
		SmallGuiButton dnBtn = new SmallGuiButton(1, leftPos + 144, topPos + 37, 15, 10, "\\/");
		dnBtn.setPressListener(b -> MainProxy.sendPacketToServer(PacketHandler.getPacket(CraftingCycleRecipe.class).setDown(true).setTilePos(_crafter)));
		(cycleButtons[1] = addRenderableWidget(dnBtn)).visible = false;
	}

	@Override
	protected void renderBg(@Nonnull GuiGraphicsExtractor guiGraphics, float fA, int iA, int jA) {
		for (net.minecraft.client.gui.components.AbstractButton cycleButton : cycleButtons) {
			cycleButton.visible = _crafter.targetType != null;
		}
		LPGuiGraphics.drawGuiBackGround(minecraft, leftPos, topPos, right, bottom, 0.0f, true);
		LPGuiGraphics.drawGuiBackGround(minecraft, leftPos, topPos, right, bottom, 0.0f, true);
		for (int x = 0; x < 3; x++) {
			for (int y = 0; y < 3; y++) {
				LPGuiGraphics.drawSlotBackground(minecraft, leftPos + 34 + x * 18, topPos + 9 + y * 18);
			}
		}
		LPGuiGraphics.drawSlotBackground(minecraft, leftPos + 124, topPos + 27);
		for (int x = 0; x < 9; x++) {
			for (int y = 0; y < 2; y++) {
				LPGuiGraphics.drawSlotBackground(minecraft, leftPos + 7 + x * 18, topPos + 79 + y * 18);
			}
		}
		LPGuiGraphics.drawPlayerInventoryBackground(minecraft, leftPos + 8, topPos + 135);

		ItemIdentifierStack[] items = new ItemIdentifierStack[9];
		for (int i = 0; i < 9; i++) {
			if (_crafter.matrix.getIDStackInSlot(i) != null) {
				items[i] = _crafter.matrix.getIDStackInSlot(i);
			}
		}

		ItemStackRenderer.renderItemIdentifierStackListIntoGui(Arrays.asList(items), null, 0, leftPos + 8, topPos + 79, 9, 9, 18, 18, 0.0F, DisplayAmount.NEVER);

		for (int a = 0; a < 9; a++) {
			guiGraphics.fill(leftPos + 8 + (a * 18), topPos + 80, leftPos + 24 + (a * 18), topPos + 96, 0xc08b8b8b);
		}
	}

	private boolean isMouseInFuzzyPanel(int mx, int my) {
		if (fuzzyPanelSelection == -1) {
			return false;
		}
		int posX = -60;
		int posY = 0;
		return mx >= posX && my >= posY && mx <= posX + 60 && my <= posY + 52;
	}
}
