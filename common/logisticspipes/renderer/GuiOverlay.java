package logisticspipes.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;



import lombok.Getter;
import lombok.Setter;



import logisticspipes.modules.LogisticsModule.ModulePositionType;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.pipe.SlotFinderNumberPacket;
import logisticspipes.proxy.MainProxy;

public class GuiOverlay {

	@Getter
	private static final GuiOverlay instance = new GuiOverlay();

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
	@Getter
	@Setter
	private boolean isOverlaySlotActive;

	private GuiOverlay() {
		// Mouse class removed in 1.20.1 (LWJGL 3 uses GLFW); fX/fY reflection no longer needed
	}

	/** Red box over the hovered slot; called from ScreenEvent.Render.Post. */
	public void render(AbstractContainerScreen<?> gui, GuiGraphics graphics, int mouseX, int mouseY) {
		if (!isOverlaySlotActive) return;
		Slot slot = slotAt(gui, mouseX, mouseY);
		if (slot == null) return;
		int x = slot.x + gui.getGuiLeft();
		int y = slot.y + gui.getGuiTop();
		graphics.pose().pushPose();
		graphics.pose().translate(0, 0, 400);
		graphics.fill(x, y, x + 16, y + 16, 0xa0ff0000);
		graphics.pose().popPose();
	}

	/** Picks the clicked slot. Returns true to cancel the click, as LP1 drained the mouse events. */
	public boolean mouseClicked(AbstractContainerScreen<?> gui, double mouseX, double mouseY, int button) {
		if (!isOverlaySlotActive || button != 0) return false;
		Slot slot = slotAt(gui, (int) mouseX, (int) mouseY);
		if (slot != null) {
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
			isOverlaySlotActive = false;
			Minecraft.getInstance().player.closeContainer();
		}
		return true;
	}

	private Slot slotAt(AbstractContainerScreen<?> gui, int mouseX, int mouseY) {
		for (Slot slot : gui.getMenu().slots) {
			if (isMouseOverSlot(gui, slot, mouseX, mouseY)) return slot;
		}
		return null;
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
