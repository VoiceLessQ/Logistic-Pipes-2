package logisticspipes.gui.hud;

import logisticspipes.utils.gui.SimpleGraphics;
import net.minecraft.client.Minecraft;





import logisticspipes.interfaces.IHUDButton;
import logisticspipes.interfaces.IHUDConfig;
import logisticspipes.interfaces.IHUDModuleHandler;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.pipes.PipeLogisticsChassis;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.hud.BasicHUDButton;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.item.ItemStackRenderer;
import logisticspipes.utils.item.ItemStackRenderer.DisplayAmount;

public class HudChassisPipe extends BasicHUDGui {

	private final PipeLogisticsChassis pipe;
	private final ItemIdentifierInventory moduleInventory;

	private int selected = -1;
	private int modulePage = 0;

	private int xCursor;
	private int yCursor;

	public HudChassisPipe(PipeLogisticsChassis pipeLogisticsChassis, ItemIdentifierInventory _moduleInventory) {
		pipe = pipeLogisticsChassis;
		moduleInventory = _moduleInventory;
		for (int i = 0; i < pipe.getChassisSize(); i++) {
			addRenderableWidget(new ItemButton(moduleInventory, i, -45, -35 + ((i % 3) * 27), 20, 25));
		}

		addRenderableWidget(new BasicHUDButton("<", -45, -45, 8, 8) {

			@Override
			public boolean shouldRenderButton() {
				return !isSlotSelected();
			}

			@Override
			public void clicked() {
				modulePage--;
			}

			@Override
			public boolean buttonEnabled() {
				return modulePage > 0;
			}
		});
		addRenderableWidget(new BasicHUDButton(">", -33, -45, 8, 8) {

			@Override
			public boolean shouldRenderButton() {
				return !isSlotSelected();
			}

			@Override
			public void clicked() {
				modulePage++;
			}

			@Override
			public boolean buttonEnabled() {
				return modulePage < ((pipe.getChassisSize() - 1) / 3);
			}
		});
		addRenderableWidget(new BasicHUDButton("x", 37, -45, 8, 8) {

			@Override
			public boolean shouldRenderButton() {
				return isSlotSelected();
			}

			@Override
			public void clicked() {
				resetSelection();
			}

			@Override
			public boolean buttonEnabled() {
				return true;
			}
		});
	}

	@Override
	public void renderHeadUpDisplay(double distance, boolean day, boolean shifted, Minecraft minecraft, IHUDConfig config) {
		LPGuiGraphics.setHudColor(day, 64);
		LPGuiGraphics.drawGuiBackGround(minecraft, -50, -50, 50, 50, 0, false);
		LPGuiGraphics.setHudColor(day, 127);
		super.renderHeadUpDisplay(distance, day, shifted, minecraft, config);
		net.minecraft.client.gui.GuiGraphicsExtractor gg = logisticspipes.utils.gui.SimpleGraphics.guiGraphics;
		int textColor = 0xff000000;
		if (selected != -1) {
			LogisticsModule selectedmodule = pipe.getSubModule(selected);
			if (selectedmodule == null) {
				return;
			}

			LPGuiGraphics.setHudColor(false, 64);
			LPGuiGraphics.drawGuiBackGround(minecraft, -23, -35, 45, 45, 0, false);
			LPGuiGraphics.setHudColor(false, 127);

			if (selectedmodule instanceof IHUDModuleHandler && ((IHUDModuleHandler) selectedmodule).getHUDRenderer() != null) {
				((IHUDModuleHandler) selectedmodule).getHUDRenderer().renderContent(shifted);
				if (((IHUDModuleHandler) selectedmodule).getHUDRenderer().getButtons() != null) {
					for (IHUDButton button : ((IHUDModuleHandler) selectedmodule).getHUDRenderer().getButtons()) {
						button.renderAlways(shifted);
						if (button.shouldRenderButton()) {
							button.renderButton(button.isFocused(), button.isblockFocused(), shifted);
						}
						if (!button.buttonEnabled() || !button.shouldRenderButton()) {
							continue;
						}
						if ((button.getX() - 1 < (xCursor - 11) && (xCursor - 11) < (button.getX() + button.sizeX() + 1)) && (button.getY() - 1 < (yCursor - 5) && (yCursor - 5) < (button.getY() + button.sizeY() + 1))) {
							if (!button.isFocused() && !button.isblockFocused()) {
								button.setFocused();
							} else if (button.focusedTime() > 400 && !button.isblockFocused()) {
								button.clicked();
								button.blockFocused();
							}
						} else if (button.isFocused() || button.isblockFocused()) {
							button.clearFocused();
						}
					}
				}
			} else {
				if (gg != null) {
					SimpleGraphics.drawString(gg, minecraft.font, "Nothing", -5, -15, textColor, false);
					SimpleGraphics.drawString(gg, minecraft.font, "to", 9, -5, textColor, false);
					SimpleGraphics.drawString(gg, minecraft.font, "display", -5, 5, textColor, false);
				}
			}
		} else {
			ItemStackRenderer.renderItemIdentifierStackListIntoGui(pipe.displayList, null, 0, -15, -35, 3, 12, 18, 18, 100.0F, DisplayAmount.ALWAYS, false, shifted);
		}
	}

	@Override
	public boolean display(IHUDConfig config) {
		if (!config.isChassisHUD()) {
			return false;
		}
		for (int i = 0; i < moduleInventory.getContainerSize(); i++) {
			if (moduleInventory.getIDStackInSlot(i) != null) {
				return true;
			}
		}
		return true;
	}

	@Override
	public boolean cursorOnWindow(int x, int y) {
		return -50 < x && x < 50 && -50 < y && y < 50;
	}

	@Override
	public void handleCursor(int x, int y) {
		super.handleCursor(x, y);
		xCursor = x;
		yCursor = y;
	}

	private void moduleClicked(int number) {
		selected = number;
		if (selected != -1) {
			LogisticsModule selectedmodule = pipe.getSubModule(selected);
			if (selectedmodule instanceof IHUDModuleHandler) {
				((IHUDModuleHandler) selectedmodule).startHUDWatching();
			}
		}
	}

	private void resetSelection() {
		if (selected != -1) {
			LogisticsModule selectedmodule = pipe.getSubModule(selected);
			if (selectedmodule instanceof IHUDModuleHandler) {
				((IHUDModuleHandler) selectedmodule).stopHUDWatching();
			}
		}
		selected = -1;
	}

	private boolean isSlotSelected() {
		return selected != -1;
	}

	private boolean isSlotSelected(int number) {
		return selected == number;
	}

	private boolean shouldDisplayButton(int number) {
		return modulePage * 3 <= number && number < (modulePage + 1) * 3;
	}

	public void stopWatching() {
		resetSelection();
	}

	private class ItemButton extends BasicHUDButton {

		private ItemIdentifierInventory inv;
		private int position;

		public ItemButton(ItemIdentifierInventory inv, int position, int x, int y, int width, int height) {
			super("item." + position, x, y, width, height);
			this.inv = inv;
			this.position = position;
		}

		@Override
		public void clicked() {
			moduleClicked(position);
		}

		@Override
		public void renderButton(boolean hover, boolean clicked, boolean shifted) {
			Minecraft mc = Minecraft.getInstance();

			if (shifted || hover || isSlotSelected(position)) {
				SimpleGraphics.setColor(1.0F, 1.0F, 1.0F, 1F);
			} else {
				SimpleGraphics.setColor(1.0F, 1.0F, 1.0F, 0.5F);
			}

			net.minecraft.client.gui.GuiGraphicsExtractor gg = logisticspipes.utils.gui.SimpleGraphics.guiGraphics;
			if (gg != null) {
				gg.pose().pushMatrix();
				gg.pose().translate(0.0F, 0.0F);
				gg.pose().scale(0.5F, 0.5F);
			}
			if (isSlotSelected(position)) {
				LPGuiGraphics.drawGuiBackGround(mc, posX * 2, posY * 2, (posX + sizeX) * 2 + 19, (posY + sizeY) * 2, 0, false, true, true, true, false);
			} else {
				LPGuiGraphics.drawGuiBackGround(mc, posX * 2, posY * 2, (posX + sizeX) * 2, (posY + sizeY) * 2, 0, false);
			}
			if (gg != null) {
				gg.pose().popMatrix();
			}

			ItemIdentifierStack module = inv.getIDStackInSlot(position);

			if (module != null) {
				boolean renderInColor = buttonEnabled() || isSlotSelected(position);
				ItemStackRenderer itemStackRenderer = new ItemStackRenderer(posX + ((sizeX - 16) / 2), posY + ((sizeY - 16) / 2), -0.002F, shifted, renderInColor);
				itemStackRenderer.setItemIdentStack(module).setDisplayAmount(DisplayAmount.NEVER);

				itemStackRenderer.renderInGui();
			}
		}

		@Override
		public void renderAlways(boolean shifted) {
			if (inv.getIDStackInSlot(position) == null && shouldDisplayButton(position)) {
				if (shifted) {
					SimpleGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
				} else {
					SimpleGraphics.setColor(1.0F, 1.0F, 1.0F, 0.3F);
				}
				Minecraft mc = Minecraft.getInstance();
				net.minecraft.client.gui.GuiGraphicsExtractor gg2 = logisticspipes.utils.gui.SimpleGraphics.guiGraphics;
				if (gg2 != null) {
					gg2.pose().pushMatrix();
					gg2.pose().scale(0.5F, 0.5F);
				}
				LPGuiGraphics.drawGuiBackGround(mc, posX * 2, posY * 2, (posX + sizeX) * 2, (posY + sizeY) * 2, 0, false);
				if (gg2 != null) {
					gg2.pose().popMatrix();
				}
			}
		}

		@Override
		public boolean shouldRenderButton() {
			return inv.getIDStackInSlot(position) != null && shouldDisplayButton(position);
		}

		@Override
		public boolean buttonEnabled() {
			return !isSlotSelected();
		}
	}
}
