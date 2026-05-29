package logisticspipes.renderer;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;



import lombok.Getter;
import lombok.Setter;



import logisticspipes.LogisticsPipes;
import logisticspipes.modules.LogisticsModule.ModulePositionType;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.pipe.SlotFinderNumberPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.gui.SimpleGraphics;

public class GuiOverlay {

	@Getter
	private static final GuiOverlay instance = new GuiOverlay();

	private int oldX;
	private int oldY;
	private boolean hasBeenSaved;
	private boolean clicked;

	@Setter
	private int targetPosX;
	@Setter
	private int targetPosY;
	@Setter
	private int targetPosZ;
	@Setter
	private int pipePosX;
	@Setter
	private int pipePosY;
	@Setter
	private int pipePosZ;
	@Setter
	private ModulePositionType positionType;
	@Setter
	private int positionInt;
	@Setter
	private int slot;
	@Setter
	private boolean isOverlaySlotActive;

	private GuiOverlay() {
		// Mouse class removed in 1.20.1 (LWJGL 3 uses GLFW); fX/fY reflection no longer needed
	}

	public boolean isCompatibleGui() {
		Minecraft client = Minecraft.getInstance();
		if (client == null) return false;

		return client.screen instanceof AbstractContainerScreen;
	}

	public void preRender() {
		if (isOverlaySlotActive) {
			Minecraft mc = Minecraft.getInstance();
			oldX = (int) mc.mouseHandler.xpos();
			oldY = (int) mc.mouseHandler.ypos();
			hasBeenSaved = true;
		}
	}

	public void renderOverGui() {
		if (hasBeenSaved) {
			hasBeenSaved = false;
			// Mouse restore removed — GLFW mouse position is not directly settable in 1.20.1
		}
		if (isOverlaySlotActive) {
			Minecraft client = Minecraft.getInstance();
			AbstractContainerScreen gui = (AbstractContainerScreen) client.screen;

			int guiTop = gui.getGuiTop();
			int guiLeft = gui.getGuiLeft();

			int x = oldX * gui.width / client.getWindow().getScreenWidth();
			int y = gui.height - oldY * gui.height / client.getWindow().getScreenHeight() - 1;

			for (Slot slot : gui.getMenu().slots) {
				if (isMouseOverSlot(gui, slot, x, y)) {
					if (SimpleGraphics.guiGraphics != null) {
						RenderSystem.disableDepthTest();
						int k1 = slot.x + guiLeft;
						int i1 = slot.y + guiTop;
						SimpleGraphics.drawGradientRect(k1, i1, k1 + 16, i1 + 16, 0xa0ff0000, 0xa0ff0000, 0.0);
						RenderSystem.enableDepthTest();
					}
					if (clicked) {
						MainProxy.sendPacketToServer(PacketHandler.getPacket(SlotFinderNumberPacket.class)
								.setInventorySlot(slot.index)
								.setSlot(this.slot)
								.setPipePosX(pipePosX)
								.setPipePosY(pipePosY)
								.setPipePosZ(pipePosZ)
								.setType(positionType)
								.setPositionInt(positionInt)
								.setPosX(targetPosX)
								.setPosY(targetPosY)
								.setPosZ(targetPosZ));
						clicked = false;
						client.player.closeContainer();
						isOverlaySlotActive = false;
					}
					break;
				}
			}
			clicked = false;
		}
	}

	private boolean isMouseOverSlot(AbstractContainerScreen gui, Slot slot, int mouseX, int mouseY) {
		return isPointInRegion(gui, slot.x, slot.y, 16, 16, mouseX, mouseY);
	}

	private boolean isPointInRegion(AbstractContainerScreen gui, int x, int y, int width, int height, int pointX, int pointY) {
		int x0 = gui.getGuiLeft();
		int y0 = gui.getGuiTop();
		pointX -= x0;
		pointY -= y0;
		return pointX >= x - 1 && pointX < x + width + 1 && pointY >= y - 1 && pointY < y + height + 1;
	}
}
