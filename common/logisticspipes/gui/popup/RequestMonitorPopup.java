package logisticspipes.gui.popup;

import com.mojang.blaze3d.systems.RenderSystem;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.minecraft.world.level.block.Block;

import logisticspipes.LogisticsPipes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas; // was TextureAtlas
import net.minecraft.world.level.block.Blocks;
import net.minecraft.resources.ResourceLocation;




import logisticspipes.pipes.PipeBlockRequestTable;
import logisticspipes.routing.order.IOrderInfoProvider;
import logisticspipes.routing.order.LinkedLogisticsOrderList;
import logisticspipes.utils.Color;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.SimpleGraphics;
import logisticspipes.utils.gui.SubGuiScreen;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.string.ChatColor;
import network.rs485.logisticspipes.util.TextUtil;

public class RequestMonitorPopup extends SubGuiScreen {

	private enum ZOOM_LEVEL {
		NORMAL(1, 165, 224, 1, 0, 0, 0),
		LEVEL_1(0.5F, 330, 465, 1, 50, -200, 100),
		LEVEL_2(0.25F, 660, 950, 2, 100, -400, -100);

		ZOOM_LEVEL(float zoom, int bottom, int right, int line, int moveY, int maxX, int maxY) {
			this.zoom = zoom;
			bottomRenderBorder = bottom;
			rightRenderBorder = right;
			this.line = line;
			this.moveY = moveY;
			this.maxX = maxX;
			this.maxY = maxY;
		}

		final float zoom;
		final int bottomRenderBorder;
		final int rightRenderBorder;
		final int line;
		final int moveY;
		final int maxX;
		final int maxY;

		ZOOM_LEVEL next() {
			int id = ordinal();
			if (id + 1 >= ZOOM_LEVEL.values().length) {
				return this;
			} else {
				return ZOOM_LEVEL.values()[id + 1];
			}
		}

		ZOOM_LEVEL prev() {
			int id = ordinal();
			if (id - 1 < 0) {
				return this;
			} else {
				return ZOOM_LEVEL.values()[id - 1];
			}
		}
	}

	private static final ResourceLocation achievementTextures = ResourceLocation.fromNamespaceAndPath("logisticspipes", "textures/gui/gui_border.png");
	private final PipeBlockRequestTable _table;
	private final int orderId;

	private int isMouseButtonDown;
	private int mouseX;
	private int mouseY;
	private double guiMapX;
	private double guiMapY;
	private int minY = -230;
	private int maxY = 0;
	private int minX = -800;
	private int maxX = 800;
	private ZOOM_LEVEL zoom = ZOOM_LEVEL.NORMAL;
	private Object[] tooltip = null;

	public RequestMonitorPopup(PipeBlockRequestTable table, int orderId) {
		super(256, 202, 0, 0);
		_table = table;
		this.orderId = orderId;
		guiMapY = -200;
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double delta) {
		if (delta < 0) {
			zoom = zoom.next();
		} else if (delta > 0) {
			zoom = zoom.prev();
		}
		return super.mouseScrolled(mx, my, delta);
	}

	@Override
	public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
		int k = (width - xSize) / 2;
		int l = (height - ySize) / 2;
		if (mx >= k + 8 && mx < k + 8 + 224 && my >= l + 17 && my < l + 17 + 155) {
			guiMapX -= dx / zoom.zoom;
			guiMapY -= dy / zoom.zoom;
		}
		return super.mouseDragged(mx, my, button, dx, dy);
	}

	@Override
	public void init() {
		super.init();
		logisticspipes.utils.gui.SmallGuiButton closeBtn = new logisticspipes.utils.gui.SmallGuiButton(0, width / 2 - 90, height / 2 + 74, 80, 20, "Close");
		closeBtn.setPressListener(b -> exitGui());
		addRenderableWidget(closeBtn);
		logisticspipes.utils.gui.SmallGuiButton saveBtn = new logisticspipes.utils.gui.SmallGuiButton(1, width / 2 + 10, height / 2 + 74, 80, 20, "Save as Image");
		saveBtn.setPressListener(b -> saveTreeToImage());
		addRenderableWidget(saveBtn);
	}

	@Override
	protected void renderToolTips(int mouseX, int mouseY, float par3) {
		if (tooltip != null && tooltip.length >= 5) {
			@SuppressWarnings("unchecked")
			List<String> lines = (List<String>) tooltip[4];
			getGuiGraphics().renderComponentTooltip(minecraft.font,
					lines.stream().map(net.minecraft.network.chat.Component::literal).collect(java.util.stream.Collectors.toList()),
					(int) tooltip[0], (int) tooltip[1]);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int par1, int par2) {

	}

	@Override
	protected void renderGuiBackground(int mouseX, int mouseY) {
		if (!_table.watchedRequests.containsKey(orderId)) {
			exitGui();
			return;
		}
		isMouseButtonDown = 0;

		if (guiMapY < minY) {
			guiMapY = minY;
		}
		if (guiMapY > maxY) {
			guiMapY = maxY;
		}
		if (guiMapX > maxX) {
			guiMapX = maxX;
		}
		if (guiMapX < minX) {
			guiMapX = minX;
		}

		createBoundary();
		drawTransparentBack();
		drawMap(mouseX, mouseY);
	}

	private void createBoundary() {
		int size = _table.watchedRequests.get(orderId).getValue2().getTreeRootSize();
		minX = -size * (40 / 2) + (int) (75 * (zoom.zoom));
		maxX = -minX + zoom.maxX;
		maxY = -100;
		findLowest(_table.watchedRequests.get(orderId).getValue2(), -200);
	}

	private void drawTransparentBack() {
		SimpleGraphics.drawGradientRect(0, 0, width, height, Color.BLANK, Color.BLANK, 0.0);
	}

	private void findLowest(LinkedLogisticsOrderList list, int lowerLimit) {
		lowerLimit += 48;
		for (LinkedLogisticsOrderList sub : list.getSubOrders()) {
			findLowest(sub, lowerLimit);
		}
		if (maxY < (lowerLimit + 10) * zoom.zoom) {
			maxY = (int) ((lowerLimit + 10) * zoom.zoom) + zoom.maxY;
		}
	}

	private void saveTreeToImage() {
		// Renders the whole request tree into an offscreen framebuffer and saves it as a PNG,
		// replacing LP1's glReadPixels tile-stitching which is gone in 1.20.1.
		if (!_table.watchedRequests.containsKey(orderId)) {
			return;
		}
		LinkedLogisticsOrderList list = _table.watchedRequests.get(orderId).getValue2();
		int imgWidth = Math.max(256, list.getTreeRootSize() * 40 + 160);
		int imgHeight = Math.max(256, treeDepth(list) * 48 + 140);
		int anchorX = imgWidth / 2 - 8;
		int anchorY = 60;

		int oldGuiLeft = guiLeft, oldGuiTop = guiTop, oldXSize = xSize, oldYSize = ySize;
		GuiGraphics oldStored = getGuiGraphics();
		com.mojang.blaze3d.pipeline.TextureTarget target = new com.mojang.blaze3d.pipeline.TextureTarget(imgWidth, imgHeight, true, Minecraft.ON_OSX);
		com.mojang.blaze3d.vertex.PoseStack modelView = RenderSystem.getModelViewStack();
		try {
			target.setClearColor(0.15F, 0.15F, 0.15F, 1.0F);
			target.clear(Minecraft.ON_OSX);
			target.bindWrite(true);
			RenderSystem.setProjectionMatrix(new org.joml.Matrix4f().setOrtho(0.0F, imgWidth, imgHeight, 0.0F, 1000.0F, 21000.0F),
					com.mojang.blaze3d.vertex.VertexSorting.ORTHOGRAPHIC_Z);
			modelView.pushPose();
			modelView.setIdentity();
			modelView.translate(0.0D, 0.0D, -11000.0D);
			RenderSystem.applyModelViewMatrix();

			GuiGraphics gg = new GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource());
			storedGuiGraphics = gg;
			SimpleGraphics.guiGraphics = gg;
			// Widen the clip rect so renderItemAt draws the full tree instead of the popup viewport
			guiLeft = -1;
			guiTop = -1;
			xSize = imgWidth + 17;
			ySize = imgHeight + 17;

			RenderSystem.disableBlend();
			if (!list.isEmpty()) {
				SimpleGraphics.drawVerticalLine(anchorX + 8, anchorY - 17, anchorY, Color.GREEN, 1);
			}
			renderLinkedOrderListLines(list, anchorX, anchorY);
			RenderSystem.setShaderColor(0.7F, 0.7F, 0.7F, 1.0F);
			String s = Integer.toString(orderId);
			int badgeY = list.isEmpty() ? anchorY + 18 : anchorY - 40;
			gg.blit(RequestMonitorPopup.achievementTextures, anchorX - 5, badgeY, 0.0f, 202.0f, 26, 26, 256, 256);
			gg.drawString(minecraft.font, s, anchorX + 9 - minecraft.font.width(s) / 2, badgeY + 10, 16777215, true);
			renderLinkedOrderListItems(list, anchorX, anchorY, Integer.MIN_VALUE / 2, Integer.MIN_VALUE / 2);
			RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
			RenderSystem.enableBlend();
			gg.flush();

			com.mojang.blaze3d.platform.NativeImage image = net.minecraft.client.Screenshot.takeScreenshot(target);
			saveImage(image);
		} catch (Exception e) {
			LogisticsPipes.log.error("Failed to render tree view PNG", e);
		} finally {
			guiLeft = oldGuiLeft;
			guiTop = oldGuiTop;
			xSize = oldXSize;
			ySize = oldYSize;
			storedGuiGraphics = oldStored;
			SimpleGraphics.guiGraphics = oldStored;
			modelView.popPose();
			RenderSystem.applyModelViewMatrix();
			target.destroyBuffers();
			Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
		}
	}

	private int treeDepth(LinkedLogisticsOrderList list) {
		int depth = 1;
		for (LinkedLogisticsOrderList sub : list.getSubOrders()) {
			depth = Math.max(depth, 1 + treeDepth(sub));
		}
		return depth;
	}

	private void saveImage(com.mojang.blaze3d.platform.NativeImage image) {
		File screenShotsFolder = new File(Minecraft.getInstance().gameDirectory, "screenshots");
		screenShotsFolder.mkdirs();
		DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
		String s = dateFormat.format(new Date());
		int i = 1;
		try {
			while (true) {
				File candidate = new File(screenShotsFolder, s + "_tree" + (i == 1 ? "" : "_" + i) + ".png");
				if (!candidate.exists()) {
					image.writeToFile(candidate);
					Minecraft.getInstance().player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Saved tree view as " + candidate.getName()));
					return;
				}
				++i;
			}
		} catch (IOException e) {
			LogisticsPipes.log.error("Failed to save tree view PNG", e);
		} finally {
			image.close();
		}
	}

	private void drawMap(int par1, int par2) {
		tooltip = null;
		int mapX = (int) Math.floor(guiMapX);
		int mapY = (int) Math.floor(guiMapY - zoom.moveY);
		int leftSide = ((width - xSize) / 2);
		int topSide = ((height - ySize) / 2);

		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		getGuiGraphics().blit(RequestMonitorPopup.achievementTextures, leftSide, topSide, 0.0f, 0.0f, xSize, ySize, 256, 256);

		guiTop *= 1 / zoom.zoom;
		guiLeft *= 1 / zoom.zoom;
		xSize *= 1 / zoom.zoom;
		ySize *= 1 / zoom.zoom;
		leftSide *= 1 / zoom.zoom;
		topSide *= 1 / zoom.zoom;
		par1 *= 1 / zoom.zoom;
		par2 *= 1 / zoom.zoom;

		int innerLeftSide = leftSide + 16;
		int innerTopSide = topSide + 17;

		RenderSystem.disableBlend();
		LinkedLogisticsOrderList list = _table.watchedRequests.get(orderId).getValue2();
		if (!list.isEmpty()) {
			SimpleGraphics.drawVerticalLine(innerLeftSide - mapX + 110, innerTopSide - mapY - 197, innerTopSide - mapY - 180, Color.GREEN, zoom.line);
		}
		renderLinkedOrderListLines(list, innerLeftSide - mapX + 102, innerTopSide - mapY - 180);
		for (Float progress : list.getProgresses()) {
			int pos = (int) (29.0F * progress);
			drawProgressPoint(innerLeftSide - mapX + 110, innerTopSide - mapY - 197 + pos, 0xff00ff00);
		}

		RenderSystem.setShaderColor(0.7F, 0.7F, 0.7F, 1.0F);
		String s = Integer.toString(orderId);
		if (!list.isEmpty()) {
			getGuiGraphics().blit(RequestMonitorPopup.achievementTextures, innerLeftSide - mapX + 97, innerTopSide - mapY - 220, 0.0f, 202.0f, 26, 26, 256, 256);
			getGuiGraphics().drawString(minecraft.font, s, innerLeftSide - mapX + 111 - minecraft.font.width(s) / 2, innerTopSide - mapY - 210, 16777215, true);
		} else {
			getGuiGraphics().blit(RequestMonitorPopup.achievementTextures, innerLeftSide - mapX + 97, innerTopSide - mapY - 162, 0.0f, 202.0f, 26, 26, 256, 256);
			getGuiGraphics().drawString(minecraft.font, s, innerLeftSide - mapX + 111 - minecraft.font.width(s) / 2, innerTopSide - mapY - 152, 16777215, true);
		}
		renderLinkedOrderListItems(list, innerLeftSide - mapX + 102, innerTopSide - mapY - 180, par1, par2);
		RenderSystem.enableBlend();

		guiTop *= zoom.zoom;
		guiLeft *= zoom.zoom;
		xSize *= zoom.zoom;
		ySize *= zoom.zoom;
		leftSide *= zoom.zoom;
		topSide *= zoom.zoom;

		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
		getGuiGraphics().blit(RequestMonitorPopup.achievementTextures, leftSide, topSide, 0.0f, 0.0f, xSize, ySize, 256, 256);
	}

	private void renderLinkedOrderListItems(LinkedLogisticsOrderList list, int xPos, int yPos, int par1, int par2) {
		int size = list.size();
		int startLeft = -(size - 1) * (30 / 2) + xPos;
		yPos += 13;
		for (IOrderInfoProvider aList : list) {
			if (aList.isInProgress()) {
				RenderSystem.setShaderColor(0.1F, 0.9F, 0.1F, 1.0F);
			} else {
				RenderSystem.setShaderColor(0.7F, 0.7F, 0.7F, 1.0F);
			}
			// GL_LIGHTING removed — use shaders
			RenderSystem.setShaderTexture(0, RequestMonitorPopup.achievementTextures);
			getGuiGraphics().blit(RequestMonitorPopup.achievementTextures, startLeft - 5, yPos - 5, 0.0f, 202.0f, 26, 26, 256, 256);
			RenderSystem.setShaderColor(0.7F, 0.7F, 0.7F, 1.0F);
			renderItemAt(aList.getAsDisplayItem(), startLeft, yPos);
			if (aList.isInProgress() && aList.getMachineProgress() != 0) {
				getGuiGraphics().fill(startLeft - 4, yPos + 20, startLeft + 20, yPos + 24, 0xff000000);
				getGuiGraphics().fill(startLeft - 3, yPos + 21, startLeft + 19, yPos + 23, 0xffffffff);
				getGuiGraphics().fill(startLeft - 3, yPos + 21, startLeft - 3 + (22 * aList.getMachineProgress() / 100), yPos + 23, 0xffff0000);
			}
			if (startLeft - 10 < par1 && par1 < startLeft + 20 && yPos - 6 < par2 && par2 < yPos + 20) {
				if (guiLeft < par1 && par1 < guiLeft + xSize - 16 && guiTop < par2 && par2 < guiTop + ySize - 16) {
					IOrderInfoProvider order = aList;
					List<String> tooltipList = new ArrayList<>();
					tooltipList.add(ChatColor.BLUE + "Request Type: " + ChatColor.YELLOW + order.getType().name());
					tooltipList.add(ChatColor.BLUE + "Send to Router ID: " + ChatColor.YELLOW + order.getRouterId());
					tooltip = new Object[] { (int) (par1 * zoom.zoom - 10), (int) (par2 * zoom.zoom), order
							.getAsDisplayItem().makeNormalStack(), true, tooltipList };
				}
			}
			startLeft += 30;
		}
		startLeft = xPos + 20 - list.getSubTreeRootSize() * (40 / 2);
		if (!list.getSubOrders().isEmpty()) {
			for (int i = 0; i < list.getSubOrders().size(); i++) {
				startLeft += list.getSubOrders().get(i).getTreeRootSize() * (40 / 2);
				renderLinkedOrderListItems(list.getSubOrders().get(i), startLeft - 20, yPos + 48, par1, par2);
				startLeft += list.getSubOrders().get(i).getTreeRootSize() * (40 / 2);
			}
		}
	}

	private void renderLinkedOrderListLines(LinkedLogisticsOrderList list, int xPos, int yPos) {
		int size = list.size();
		if (list.isEmpty()) {
			size = 1;
		}
		int startLeft = -(size - 1) * (30 / 2) + xPos;
		yPos += 13;
		int left = startLeft;
		for (int i = 0; i < list.size(); i++) {
			SimpleGraphics.drawVerticalLine(startLeft + 8, yPos - 13, yPos - 3, Color.GREEN, zoom.line);
			if (!list.getSubOrders().isEmpty()) {
				SimpleGraphics.drawVerticalLine(startLeft + 8, yPos + 18, yPos + 28, Color.GREEN, zoom.line);
			}
			startLeft += 30;
		}
		if (!list.isEmpty()) {
			SimpleGraphics.drawHorizontalLine(left + 8, startLeft - 22, yPos - 13, Color.GREEN, zoom.line);
		}
		if (!list.getSubOrders().isEmpty()) {
			if (!list.isEmpty()) {
				SimpleGraphics.drawHorizontalLine(left + 8, startLeft - 22, yPos + 28, Color.GREEN, zoom.line);
				startLeft -= 30;
			}
			SimpleGraphics.drawVerticalLine(left + ((startLeft - left) / 2) + 8, yPos + 28, yPos + 38, Color.GREEN, zoom.line);
			startLeft = xPos + 20 - list.getSubTreeRootSize() * (40 / 2);
			left = startLeft;
			for (int i = 0; i < list.getSubOrders().size(); i++) {
				startLeft += list.getSubOrders().get(i).getTreeRootSize() * (40 / 2);
				SimpleGraphics.drawVerticalLine(startLeft - 12, yPos + 38, yPos + 48, Color.GREEN, zoom.line);
				drawPointFor(list, xPos, yPos, i, startLeft);
				renderLinkedOrderListLines(list.getSubOrders().get(i), startLeft - 20, yPos + 48);
				startLeft += list.getSubOrders().get(i).getTreeRootSize() * (40 / 2);
			}
			if (!list.getSubOrders().isEmpty()) {
				left += list.getSubOrders().get(0).getTreeRootSize() * (40 / 2);
				startLeft -= list.getSubOrders().get(list.getSubOrders().size() - 1).getTreeRootSize() * (40 / 2);
			}
			SimpleGraphics.drawHorizontalLine(left - 12, startLeft - 12, yPos + 38, Color.GREEN, zoom.line);
		}
	}

	private void drawPointFor(LinkedLogisticsOrderList list, int xPos, int yPos, int i, int startLeft) {
		float totalLine = 10 + 1 + 10 + 1 + Math.abs(startLeft - (xPos + 20)) + 10 + 1 + 10;
		for (Float point : list.getSubOrders().get(i).getProgresses()) {
			int pos = (int) (totalLine * (1.0F - point));
			if (pos < 13) {
				int newSize = list.getSubOrders().get(i).size();
				int newStartLeft = -(newSize - 1) * (30 / 2) + startLeft - 20;
				for (int j = 0; j < newSize; j++) {
					drawProgressPoint(newStartLeft + 8, yPos + 48 + 12 - pos, 0xff00ff00);
					newStartLeft += 30;
				}
			} else if (pos < 10 + 1 + 10 + 1) {
				pos -= 10;
				drawProgressPoint(startLeft - 20 + 8, yPos + 38 + 12 - pos, 0xff00ff00);
			} else if (pos < Math.abs(startLeft - (xPos + 20)) + 10 + 1 + 10 + 1) {
				pos -= 10 + 1 + 10 + 1;
				if (startLeft < xPos + 20) {
					pos *= -1;
				}
				drawProgressPoint(startLeft - 12 - pos, yPos + 38, 0xff00ff00);
			} else if (pos < Math.abs(startLeft - (xPos + 20)) + 10 + 1 + 10 + 1 + 10 + 1) {
				pos -= 10 + 1 + 10 + 1 + Math.abs(startLeft - (xPos + 20)) + 10 + 1;
				drawProgressPoint(xPos + 8, yPos + 27 - pos, 0xff00ff00);
			} else if (pos < Math.abs(startLeft - (xPos + 20)) + 10 + 1 + 10 + 1 + 10 + 1 + 10 + 1) {
				pos -= 10 + 1 + 10 + 1 + Math.abs(startLeft - (xPos + 20)) + 10 + 1 + 10 + 1;
				int newSize = list.size();
				int newStartLeft = -(newSize - 1) * (30 / 2) + xPos;
				for (int j = 0; j < newSize; j++) {
					drawProgressPoint(newStartLeft + 8, yPos + 16 - pos, 0xff00ff00);
					newStartLeft += 30;
				}
			}
		}
	}

	private void renderItemAt(ItemIdentifierStack item, int x, int y) {
		if (guiLeft < x && x < guiLeft + xSize - 16 && guiTop < y && y < guiTop + ySize - 16) {
			net.minecraft.world.item.ItemStack stack = item.getItem().unsafeMakeNormalStack(1);
			if (!stack.isEmpty()) {
				getGuiGraphics().renderItem(stack, x, y);
			}
			String s = TextUtil.getThreeDigitFormattedNumber(item.getStackSize(), false);
			getGuiGraphics().drawString(minecraft.font, s, x + 17 - minecraft.font.width(s), y + 9, 16777215, true);
		}
	}

	protected void drawProgressPoint(int x, int y, int color) {
		int line = zoom.line + 1;
		getGuiGraphics().fill(x - line + 1, y - line + 1, x + line, y + line, color);
	}

	private TextureAtlasSprite getTexture(Block blockIn) {
		return Minecraft.getInstance().getBlockRenderer().getBlockModelShaper().getParticleIcon(blockIn.defaultBlockState());
	}
}
