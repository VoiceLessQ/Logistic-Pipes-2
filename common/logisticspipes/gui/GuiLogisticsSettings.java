package logisticspipes.gui;


import net.minecraft.world.entity.player.Player;




import logisticspipes.LPItems;
import logisticspipes.LogisticsPipes;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.PlayerConfigToServerPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.GuiCheckBox;
import logisticspipes.utils.gui.InputBar;
import logisticspipes.utils.gui.LogisticsBaseTabGuiScreen;
import network.rs485.logisticspipes.config.ClientConfiguration;
import network.rs485.logisticspipes.util.TextUtil;

public class GuiLogisticsSettings extends LogisticsBaseTabGuiScreen {

	private static final String PREFIX = "gui.settings.";

	public GuiLogisticsSettings(final Player player) {
		super(buildDummy(player), 180, 220);
		addTab(new PipeRenderSettings());
	}
	private static DummyContainer buildDummy(final Player player) {
		DummyContainer dummy = new DummyContainer(player.getInventory(), null);
		dummy.addNormalSlotsForPlayerInventory(10, 135);
		return dummy;
	}


	private class PipeRenderSettings extends TabSubGui {

		private InputBar renderDistance;
		private InputBar contentRenderDistance;
		private GuiCheckBox useNewRendererButton;
		private GuiCheckBox useFallbackRendererButton;

		private PipeRenderSettings() {}

		@Override
		public void initTab() {
			

			ClientConfiguration config = LogisticsPipes.getClientPlayerConfig();
			if (renderDistance == null) {
				renderDistance = new InputBar(font, getBaseScreen(), 15, 75, 30, 15, false, true, InputBar.Align.RIGHT);
				renderDistance.putInt(config.getRenderPipeDistance());
			}
			renderDistance.reposition(15, 80, 30, 15);
			if (contentRenderDistance == null) {
				contentRenderDistance = new InputBar(font, getBaseScreen(), 15, 105, 30, 15, false, true, InputBar.Align.RIGHT);
				contentRenderDistance.putInt(config.getRenderPipeContentDistance());
			}
			contentRenderDistance.reposition(15, 110, 30, 15);
			//useNewRendererButton = (GuiCheckBox) addRenderableWidget(new GuiCheckBox(0, leftPos + 15, topPos + 30, 16, 16, config.isUseNewRenderer()));
			//useFallbackRendererButton = (GuiCheckBox) addRenderableWidget(new GuiCheckBox(0, leftPos + 15, topPos + 50, 16, 16, config.isUseFallbackRenderer()));
		}

		@Override
		public void renderIcon(int x, int y) {
			if (logisticspipes.utils.gui.SimpleGraphics.guiGraphics != null) {
				logisticspipes.utils.gui.SimpleGraphics.guiGraphics.renderItem(new net.minecraft.world.item.ItemStack(LPItems.pipeBasic.get()), x, y);
			}
		}

		@Override
		public void renderBackgroundContent() {}

		@Override
		public void buttonClicked(net.minecraft.client.gui.components.AbstractButton button) {
			if (button == useNewRendererButton) {
				useNewRendererButton.change();
			}
			if (button == useFallbackRendererButton) {
				useFallbackRendererButton.change();
			}
		}

		@Override
		public void renderForegroundContent() {
			renderDistance.drawTextBox();
			contentRenderDistance.drawTextBox();
			//guiGraphics.drawString(font, StringUtil.translate(PREFIX + "pipenewrenderer"), 38, 34, 0x404040, false);
			//guiGraphics.drawString(font, StringUtil.translate(PREFIX + "pipefallbackrenderer"), 38, 54, 0x404040, false);
			guiGraphics.drawString(font, TextUtil.translate(PREFIX + "piperenderdistance"), 10, 70, 0x404040, false);
			guiGraphics.drawString(font, TextUtil.translate(PREFIX + "pipecontentrenderdistance"), 10, 100, 0x404040, false);
		}

		@Override
		public boolean handleClick(int x, int y, int type) {
			boolean val1 = renderDistance.handleClick(x - leftPos, y - topPos, type);
			boolean val2 = contentRenderDistance.handleClick(x - leftPos, y - topPos, type);
			return val1 || val2;
		}

		@Override
		public boolean handleKey(int code, char c) {
			return renderDistance.handleKey(c, code) || contentRenderDistance.handleKey(c, code);
		}

		@Override
		public void guiClose() {
			ClientConfiguration config = LogisticsPipes.getClientPlayerConfig();
			try {
				config.setRenderPipeDistance(renderDistance.getInt());
				config.setRenderPipeContentDistance(contentRenderDistance.getInt());
			} catch (Exception e) {
				LogisticsPipes.log.error("Failed to update render distance config", e);
			}
			//config.setUseNewRenderer(useNewRendererButton.getState());
			//config.setUseFallbackRenderer(useFallbackRendererButton.getState());

			MainProxy.sendPacketToServer(
					PacketHandler.getPacket(PlayerConfigToServerPacket.class).setConfig(config));

		}
	}
}
