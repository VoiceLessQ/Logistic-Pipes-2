package logisticspipes.gui.popup;

import logisticspipes.utils.gui.SimpleGraphics;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;

import logisticspipes.request.resources.IResource;
import logisticspipes.request.resources.IResource.ColorCode;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.SubGuiScreen;
import logisticspipes.utils.item.ItemIdentifierStack;
import network.rs485.logisticspipes.util.TextUtil;

public class GuiRequestPopup extends SubGuiScreen {

	private String[] text;
	private int mWidth = 0;
	private Player player;
	private logisticspipes.utils.gui.SmallGuiButton logButton = null;

	public GuiRequestPopup(Player player, Object... message) {
		super(200, (message.length * 10) + 40, 0, 0);
		List<String> textArray = new ArrayList<>();
		for (Object o : message) {
			if (o instanceof String) {
				textArray.add((String) o);
			} else if (o instanceof Collection<?>) {
				for (Object oTwo : (Collection<?>) o) {
					if (oTwo instanceof ItemIdentifierStack) {
						textArray.add(((ItemIdentifierStack) oTwo).getFriendlyName());
					}
					if (oTwo instanceof IResource) {
						textArray.add(((IResource) oTwo).getDisplayText(ColorCode.NONE));
					}
				}
			} else {
				textArray.add(o.toString());
			}
		}
		text = textArray.toArray(new String[] {});
		ySize = (text.length * 10) + 40;
		this.player = player;
	}

	@Override
	public void init() {
		super.init();
		logisticspipes.utils.gui.SmallGuiButton ok = new logisticspipes.utils.gui.SmallGuiButton(0, xCenter - 55, bottom - 25, 50, 20, "OK");
		ok.setPressListener(b -> exitGui());
		addRenderableWidget(ok);
		logButton = new logisticspipes.utils.gui.SmallGuiButton(1, xCenter + 5, bottom - 25, 50, 20, "Log");
		logButton.setPressListener(b -> {
			for (String msg : text) {
				player.sendSystemMessage(Component.literal(msg));
			}
			logButton.active = false;
		});
		addRenderableWidget(logButton);
	}

	@Override
	protected void renderGuiBackground(int mouseX, int mouseY) {
		if (mWidth == 0) {
			int lWidth = 0;
			for (String msg : text) {
				int tWidth = minecraft.font.width(msg);
				if (tWidth > lWidth) {
					lWidth = tWidth;
				}
			}
			xSize = mWidth = Math.max(Math.min(lWidth + 20, 400), 120);
			super.init();
		}
		LPGuiGraphics.drawGuiBackGround(minecraft, guiLeft, guiTop, right, bottom, 0.0f, true);
		for (int i = 0; i < text.length; i++) {
			if (text[i] == null) {
				continue;
			}
			String msg = TextUtil.getTrimmedString(text[i], mWidth - 10, font, "...");
			int stringWidth = minecraft.font.width(msg);
			SimpleGraphics.drawString(getGuiGraphics(), minecraft.font, msg, xCenter - (stringWidth / 2), guiTop + 10 + (i * 10), 0x404040, false);
		}
	}

}
