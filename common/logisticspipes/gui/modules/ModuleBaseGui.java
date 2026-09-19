package logisticspipes.gui.modules;

import net.minecraft.world.inventory.AbstractContainerMenu;

import lombok.Getter;

import logisticspipes.modules.LogisticsModule;
import logisticspipes.modules.LogisticsModule.ModulePositionType;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.gui.GuiOpenChassis;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;

public abstract class ModuleBaseGui extends LogisticsBaseGuiScreen {

	@Getter
	protected LogisticsModule module;

	public ModuleBaseGui(AbstractContainerMenu par1Container, LogisticsModule module) {
		super(par1Container);
		this.module = module;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (module == null || hasSubGui()) {
			return super.keyPressed(keyCode, scanCode, modifiers);
		}
		if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE || minecraft.options.keyInventory.matches(keyCode, scanCode)) {
			boolean handled = super.keyPressed(keyCode, scanCode, modifiers);
			if (module.getSlot() == ModulePositionType.SLOT) {
				MainProxy.sendPacketToServer(PacketHandler.getPacket(GuiOpenChassis.class).setBlockPos(module.getBlockPos()));
			}
			return handled;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}
}
