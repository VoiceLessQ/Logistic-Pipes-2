package logisticspipes.routing.debug;

import net.minecraft.client.Minecraft;



import logisticspipes.gui.hud.BasicHUDGui;
import logisticspipes.interfaces.IHUDConfig;
import logisticspipes.interfaces.IHeadUpDisplayRenderer;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.routingdebug.RoutingUpdateUntrace;
import logisticspipes.proxy.MainProxy;
import logisticspipes.routing.ExitRoute;
import logisticspipes.routing.PipeRoutingConnectionType;
import logisticspipes.routing.debug.ClientViewController.DebugInformation;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.SimpleGraphics;
import logisticspipes.utils.gui.hud.BasicHUDButton;

public class HUDRoutingTableGeneralInfo extends BasicHUDGui implements IHeadUpDisplayRenderer {

	private final DebugInformation route;
	private boolean isQuestion = false;
	private boolean display = true;
	private int line;

	HUDRoutingTableGeneralInfo(DebugInformation route) {
		this.route = route;
		if (route.isNew) {
			addUntraceButtons(route.newIndex);
		}

	}

	private void addUntraceButtons(final int index) {
		addRenderableWidget(new BasicHUDButton("Untrack", -25, -75, 50, 10) {

			@Override
			public boolean shouldRenderButton() {
				return !isQuestion && display;
			}

			@Override
			public void clicked() {
				isQuestion = true;
			}

			@Override
			public boolean buttonEnabled() {
				return !isQuestion && display;
			}
		});

		addRenderableWidget(new BasicHUDButton("Yes", -45, -75, 30, 10) {

			@Override
			public boolean shouldRenderButton() {
				return isQuestion && display;
			}

			@Override
			public void clicked() {
				MainProxy.sendPacketToServer(PacketHandler.getPacket(RoutingUpdateUntrace.class).setInteger(index));
				display = false;
			}

			@Override
			public boolean buttonEnabled() {
				return isQuestion && display;
			}
		});
		addRenderableWidget(new BasicHUDButton("No", 15, -75, 30, 10) {

			@Override
			public boolean shouldRenderButton() {
				return isQuestion && display;
			}

			@Override
			public void clicked() {
				isQuestion = false;
			}

			@Override
			public boolean buttonEnabled() {
				return isQuestion && display;
			}
		});
	}

	@Override
	public void renderHeadUpDisplay(double distance, boolean day, boolean shifted, Minecraft mc, IHUDConfig config) {
		if (route.isNew) {
			line = -65;
		} else {
			line = -75;
		}
		LPGuiGraphics.drawGuiBackGround(mc, -70, -80, 70, 80, 0, false);
		super.renderHeadUpDisplay(distance, day, shifted, mc, config);
		write("Routing Update in: ", mc);
		write(route.positions.toString(), mc);
		if (route.closedSet != null) {
			int left = -55;
			for (PipeRoutingConnectionType flag : PipeRoutingConnectionType.values) {
				if (route.closedSet.contains(flag)) {
					if (SimpleGraphics.guiGraphics != null) SimpleGraphics.guiGraphics.text(mc.font, "+", left, line, getColorForFlag(flag));
					left += mc.font.width("+");
				} else {
					if (SimpleGraphics.guiGraphics != null) SimpleGraphics.guiGraphics.text(mc.font, "-", left, line, getColorForFlag(flag));
					left += mc.font.width("-");
				}
			}
			line += 10;
		}
		if (route.routes != null) {
			for (ExitRoute exit : route.routes) {
				if (SimpleGraphics.guiGraphics != null) SimpleGraphics.guiGraphics.text(mc.font, "Possible: ", -55, line, 0xffffff);
				int left = -55 + mc.font.width("Possible: ");
				for (PipeRoutingConnectionType flag : PipeRoutingConnectionType.values) {
					if (exit.containsFlag(flag)) {
						if (SimpleGraphics.guiGraphics != null) SimpleGraphics.guiGraphics.text(mc.font, "+", left, line, getColorForFlag(flag));
						left += mc.font.width("+");
					} else {
						if (SimpleGraphics.guiGraphics != null) SimpleGraphics.guiGraphics.text(mc.font, "-", left, line, getColorForFlag(flag));
						left += mc.font.width("-");
					}
				}
				line += 10;
				write("  " + exit.debug.filterPosition, mc);
			}
		}
	}

	private int getColorForFlag(PipeRoutingConnectionType type) {
		switch (type) {
			case canRouteTo:
				return 0xff0000;
			case canRequestFrom:
				return 0x00ff00;
			case canPowerFrom:
				return 0x00ffff;
			case canPowerSubSystemFrom:
				return 0x0000ff;
		}
		return 0x000000;
	}

	private void write(String data, Minecraft mc) {
		if (SimpleGraphics.guiGraphics != null) {
			SimpleGraphics.guiGraphics.text(mc.font, data, -55, line, 0xffffff);
		}
		line += 10;
	}

	@Override
	public boolean display(IHUDConfig config) {
		return true;
	}

	@Override
	public boolean cursorOnWindow(int x, int y) {
		return -70 < x && x < 70 && -80 < y && y < 80;
	}
}
