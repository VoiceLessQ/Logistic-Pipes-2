package logisticspipes.utils.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public interface IGuiAccess {

	int getGuiLeft();

	int getGuiTop();

	int getXSize();

	int getYSize();

	int getRight();

	int getBottom();

	Minecraft getMC();

	GuiGraphicsExtractor getGuiGraphics();
}
