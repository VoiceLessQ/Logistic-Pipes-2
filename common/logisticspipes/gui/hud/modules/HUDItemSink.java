package logisticspipes.gui.hud.modules;

import logisticspipes.utils.gui.SimpleGraphics;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;





import logisticspipes.interfaces.IHUDButton;
import logisticspipes.interfaces.IHUDModuleRenderer;
import logisticspipes.modules.ModuleItemSink;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.item.ItemStackRenderer;
import logisticspipes.utils.item.ItemStackRenderer.DisplayAmount;

public class HUDItemSink implements IHUDModuleRenderer {

	private final ModuleItemSink module;

	public HUDItemSink(ModuleItemSink module) {
		this.module = module;
	}

	@Override
	public void renderContent(boolean shifted) {
		Minecraft mc = Minecraft.getInstance();
		ItemStackRenderer.renderItemIdentifierStackListIntoGui(ItemIdentifierStack.getListFromInventory(module.getFilterInventory()), null, 0, -25, -32, 3, 9, 18, 18, 100.0F, DisplayAmount.NEVER, false, shifted);
		GuiGraphicsExtractor gg = logisticspipes.utils.gui.SimpleGraphics.guiGraphics;
		if (gg != null) {
			SimpleGraphics.drawString(gg, mc.font, "Default:", -29, 25, 0xff000000, false);
			if (module.isDefaultRoute()) {
				SimpleGraphics.drawString(gg, mc.font, "Yes", 11, 25, 0xff000000, false);
			} else {
				SimpleGraphics.drawString(gg, mc.font, "No", 15, 25, 0xff000000, false);
			}
		}
	}

	@Override
	public List<IHUDButton> getButtons() {
		return null;
	}
}
