package logisticspipes.gui.hud.modules;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;





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
		GuiGraphics gg = logisticspipes.utils.gui.SimpleGraphics.guiGraphics;
		if (gg != null) {
			gg.drawString(mc.font, "Default:", -29, 25, 0xff404040, false);
			if (module.isDefaultRoute()) {
				gg.drawString(mc.font, "Yes", 11, 25, 0xff404040, false);
			} else {
				gg.drawString(mc.font, "No", 15, 25, 0xff404040, false);
			}
		}
	}

	@Override
	public List<IHUDButton> getButtons() {
		return null;
	}
}
