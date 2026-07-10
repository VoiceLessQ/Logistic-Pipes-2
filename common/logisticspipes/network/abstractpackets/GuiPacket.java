package logisticspipes.network.abstractpackets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.utils.gui.SubGuiScreen;

public abstract class GuiPacket extends ModernPacket {

	public GuiPacket(int id) {
		super(id);
	}

	@OnlyIn(Dist.CLIENT)
	protected <T> T getGui(Class<T> guiClass) {
		Screen currentScreen = Minecraft.getInstance().screen;
		if (currentScreen == null) {
			return null;
		}
		if (guiClass.isAssignableFrom(currentScreen.getClass())) {
			return (T) currentScreen;
		}
		SubGuiScreen subScreen = null;
		if (currentScreen instanceof LogisticsBaseGuiScreen) {
			subScreen = ((LogisticsBaseGuiScreen) currentScreen).getSubGui();
		}
		while (subScreen != null) {
			if (guiClass.isAssignableFrom(subScreen.getClass())) {
				return (T) subScreen;
			}
			subScreen = subScreen.getSubGui();
		}
		return null;
	}
}
