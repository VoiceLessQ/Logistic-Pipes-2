
package logisticspipes.gui;

import net.minecraft.client.gui.GuiGraphics;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;


import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.world.item.ItemStack;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.pow;
import static java.lang.Math.round;



import logisticspipes.blocks.stats.LogisticsStatisticsTileEntity;
import logisticspipes.blocks.stats.TrackingTask;
import logisticspipes.gui.popup.GuiAddTracking;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.block.RemoveAmoundTask;
import logisticspipes.network.packets.block.RequestAmountTaskSubGui;
import logisticspipes.network.packets.block.RequestRunningCraftingTasks;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.Color;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.ItemDisplay;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.math.Vec2;
import logisticspipes.utils.string.StringUtils;
import network.rs485.logisticspipes.util.TextUtil;
import javax.annotation.Nonnull;

public class GuiStatistics extends LogisticsBaseGuiScreen {

	private final String PREFIX = "gui.networkstatistics.";

	private int currentTab;
	private final TabTracker tabTracker = new TabTracker();
	private final TabCrafting tabCrafting = new TabCrafting();
	private final List<StatisticsTab> tabs = Arrays.asList(tabTracker, tabCrafting);

	private final LogisticsStatisticsTileEntity tile;

	private int prevMouseDragX;
	private int prevMouseDragY;

	public GuiStatistics(final LogisticsStatisticsTileEntity tile) {
		super(180, 220, 0, 0);
		this.tile = tile;
	}

	@Override
	public void init() {
		

		super.init();

		tabs.forEach(StatisticsTab::init);

		tabTracker.updateItemList();
	}

	@Override
	public void closeGui() throws IOException {
		super.closeGui();
		
	}

	@Override
	public void resetSubGui() {
		super.resetSubGui();
		tabTracker.updateItemList();
	}

	private StatisticsTab getActiveTab() {
		return tabs.get(currentTab);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int clickedMouseButton, double deltaX, double deltaY) {
		getActiveTab().onMouseDrag((int)mouseX, (int)mouseY, (int)(mouseX - prevMouseDragX), (int)(mouseY - prevMouseDragY));
		prevMouseDragX = (int)mouseX;
		prevMouseDragY = (int)mouseY;
		return super.mouseDragged(mouseX, mouseY, clickedMouseButton, deltaX, deltaY);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double lpScrollX, double delta) {
		getActiveTab().onMouseScroll((int) delta);
		return super.mouseScrolled(mouseX, mouseY, lpScrollX, delta);
	}

	@Override
	protected void renderBg(@Nonnull GuiGraphics guiGraphics, float f, int mouseX, int mouseY) {
		drawBG();
		getActiveTab().draw(mouseX, mouseY);

		super.renderBg(guiGraphics, f, mouseX, mouseY);
	}

	private void drawBG() {
		// background
		LPGuiGraphics.drawGuiBackGround(minecraft, leftPos, topPos + 20, right, bottom, 0.0f, true);
		LPGuiGraphics.drawGuiBackGround(minecraft, leftPos + (25 * currentTab) + 2, topPos - 2, leftPos + 27 + (25 * currentTab), topPos + 38, 0.0f, true, true, true, false, true);

		// tab selector panes
		for (int i = 0; i < tabs.size(); i++) {
			LPGuiGraphics.drawGuiBackGround(minecraft, leftPos + (25 * i) + 2, topPos - 2, leftPos + 27 + (25 * i), topPos + 35, 0.0f, false, true, true, false, true);
		}

		// First Tab
		LPGuiGraphics.drawStatsBackground(minecraft, leftPos + 6, topPos + 3);

		// Second tab background: item icons drawn lazily by TabCrafting.draw()
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (!hasSubGui()) {
			getActiveTab().keyPressed(keyCode);
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
		prevMouseDragX = (int)mouseX;
		prevMouseDragY = (int)mouseY;

		if (mouseButton == 0 && mouseX > leftPos && mouseX < leftPos + 220 && mouseY > topPos && mouseY < topPos + 20) {
			double tabX = mouseX - leftPos - 3;
			currentTab = max(0, min((int)(tabX / 25), tabs.size() - 1));
		} else {
			getActiveTab().handleClick((int)mouseX, (int)mouseY, mouseButton);
			return super.mouseClicked(mouseX, mouseY, mouseButton);
		}
		return true;
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		super.renderLabels(guiGraphics, mouseX, mouseY);
		getActiveTab().drawForegroundLayer(mouseX, mouseY);
	}

	@Override
	protected void checkButtons() {
		super.checkButtons();
		tabs.forEach(StatisticsTab::checkButtons);
	}

	public void handlePacket1(List<ItemIdentifierStack> identList) {
		tabTracker.handlePacket(identList);
	}

	public void handlePacket2(List<ItemIdentifierStack> identList) {
		tabCrafting.handlePacket(identList);
	}

	private interface StatisticsTab {

		void init();

		void draw(int mouseX, int mouseY);

		default void drawForegroundLayer(int mouseX, int mouseY) {}

		default void checkButtons() {}

		default void keyPressed(int keyCode) {}

		default void handleClick(int mouseX, int mouseY, int mouseButton) {}

		default void onMouseDrag(int x, int y, int dx, int dy) {}

		default void onMouseScroll(int dw) {}

	}

	private class TabTracker implements StatisticsTab {

		private ItemDisplay itemDisplay;

		private float xViewportOffset = -1434;
		private float yViewportOffset;
		private float xViewportScale = 15;
		private float yViewportScale = 15;

		private boolean isDraggingGraph = false;
		private boolean isDraggingXBar = false;
		private boolean isDraggingYBar = false;

		private final List<net.minecraft.client.gui.components.AbstractButton> BUTTONS = new ArrayList<>();

		// Buffered text labels populated in draw(), drawn in drawForegroundLayer()
		private String taskNameLabel = null;
		private final List<String> graphTexts = new ArrayList<>();
		private final List<int[]> graphTextPos = new ArrayList<>();

		@Override
		public void init() {
			SmallGuiButton b0 = new SmallGuiButton(0, leftPos + 10, topPos + 70, 20, 20, "<");
			b0.setPressListener(b -> itemDisplay.prevPage());
			BUTTONS.add(addRenderableWidget(b0));
			SmallGuiButton b1 = new SmallGuiButton(1, leftPos + 150, topPos + 70, 20, 20, ">");
			b1.setPressListener(b -> itemDisplay.nextPage());
			BUTTONS.add(addRenderableWidget(b1));
			SmallGuiButton b2 = new SmallGuiButton(2, leftPos + 37, topPos + 70, 40, 20, "Add");
			b2.setPressListener(b -> MainProxy.sendPacketToServer(PacketHandler.getPacket(RequestAmountTaskSubGui.class).setTilePos(tile)));
			BUTTONS.add(addRenderableWidget(b2));
			SmallGuiButton b3 = new SmallGuiButton(3, leftPos + 83, topPos + 70, 60, 20, "Remove");
			b3.setPressListener(b -> {
				if (itemDisplay.getSelectedItem() != null) {
					MainProxy.sendPacketToServer(PacketHandler.getPacket(RemoveAmoundTask.class).setItem(itemDisplay.getSelectedItem().getItem()).setTilePos(tile));
					Iterator<TrackingTask> iter = tile.tasks.iterator();
					while (iter.hasNext()) {
						TrackingTask task = iter.next();
						if (task.item == itemDisplay.getSelectedItem().getItem()) {
							iter.remove();
							break;
						}
					}
					updateItemList();
				}
			});
			BUTTONS.add(addRenderableWidget(b3));

			if (itemDisplay == null) {
				itemDisplay = new ItemDisplay(null, font, GuiStatistics.this, null, leftPos + 10, topPos + 18, imageWidth - 20, imageHeight - 100, 0, 0, 0, new int[] { 1, 10, 64, 64 }, true);
			}
			itemDisplay.reposition(leftPos + 10, topPos + 40, imageWidth - 20, 20, 0, 0);
		}

		@Override
		public void draw(int mouseX, int mouseY) {
			taskNameLabel = null;
			graphTexts.clear();
			graphTextPos.clear();
			itemDisplay.renderItemArea(0.0f);
			itemDisplay.renderPageNumber(right - 40, topPos + 28);
			if (itemDisplay.getSelectedItem() != null) {
				TrackingTask task = getSelectedTask();

				if (task != null) {
					LPGuiGraphics.drawSlotBackground(minecraft, leftPos + 10, topPos + 99);
					guiGraphics.renderItem(task.item.unsafeMakeNormalStack(1), leftPos + 12, topPos + 101);
					taskNameLabel = StringUtils.getWithMaxWidth(task.item.getFriendlyName(), 136, font);

					int xOrigo = xCenter - 72;
					int yOrigo = yCenter + 90;

					drawLine(xOrigo + 0, yOrigo + 0, xOrigo + 150, yOrigo + 0, Color.DARKER_GREY);
					drawLine(xOrigo + 0, yOrigo + 0, xOrigo + 0, yOrigo - 80, Color.DARKER_GREY);

					drawLine(xOrigo - 4, yOrigo - 80, xOrigo + 0, yOrigo - 80, Color.DARKER_GREY);

					drawLine(xOrigo + 150, yOrigo - 1, xOrigo + 150, yOrigo + 4, Color.DARKER_GREY);

					long[] data = getTaskData(task);

					float xViewportCenter = 75;
					float yViewportCenter = 40;

					java.util.Set<Integer> labeledYPixels = new java.util.HashSet<>();
					int rightLimit = 2; // we want to draw one more graph part past the right edge
					for (int i = 0; i < data.length; i++) {
						rightLimit--;
						if (rightLimit == 0) break;

						float x = i;
						float y = data[i];
						float prevX = x;
						float prevY = y;
						if (i > 0) {
							prevX = x - 1;
							prevY = data[i - 1];
						}

						x += xViewportOffset;
						x *= xViewportScale;
						x += xViewportCenter;
						prevX += xViewportOffset;
						prevX *= xViewportScale;
						prevX += xViewportCenter;

						y -= yViewportOffset;
						y *= yViewportScale;
						y += yViewportCenter;
						prevY -= yViewportOffset;
						prevY *= yViewportScale;
						prevY += yViewportCenter;

						if (x <= 150) rightLimit = 2;
						if (x < 0) continue;

						if (x <= 150) {
							int interval = max(1, (int) (40 / xViewportScale) + 1);
							if (i % interval == 0) {
								String s = formatTime(data.length - i - 1);
								int w = minecraft.font.width(s);
								drawLine(xOrigo + (int) x, yOrigo - 1, xOrigo + (int) x, yOrigo + 4, Color.DARKER_GREY);
								graphTexts.add(s);
							graphTextPos.add(new int[]{(int)(xOrigo - leftPos + (int) x - w / 2f), yOrigo - topPos + 6, Color.DARKER_GREY.getValue()});
							}
						}

						if (y > 0 && y < 80) {
							drawLine(xOrigo - 4, yOrigo - (int) y, xOrigo + 0, yOrigo - (int) y, Color.DARKER_GREY);
							int yPixel = (int) y;
							boolean tooClose = false;
							for (int labeled : labeledYPixels) {
								if (Math.abs(labeled - yPixel) < 10) { tooClose = true; break; }
							}
							if (!tooClose) {
								labeledYPixels.add(yPixel);
								String label = Long.toString(data[i]);
								int lw = minecraft.font.width(label);
								graphTexts.add(label);
							graphTextPos.add(new int[]{xOrigo - leftPos - 5 - lw, yOrigo - topPos - yPixel - 4, Color.DARKER_GREY.getValue()});
							}
						}

						drawGraphPart(xOrigo, yOrigo, (int) prevX, (int) prevY, (int) x, (int) y);
					}
				}
			}
		}

		@Nullable
		private TrackingTask getSelectedTask() {
			for (TrackingTask taskLoop : tile.tasks) {
				if (taskLoop.item == itemDisplay.getSelectedItem().getItem()) {
					return taskLoop;
				}
			}
			return null;
		}

		private long[] getTaskData(TrackingTask task) {
			long[] data = new long[task.amountRecorded.length];
			System.arraycopy(task.amountRecorded, task.arrayPos, data, 0, task.amountRecorded.length - task.arrayPos);
			System.arraycopy(task.amountRecorded, 0, data, task.amountRecorded.length - task.arrayPos, task.arrayPos);
			return data;
		}

		@Override
		public void drawForegroundLayer(int mouseX, int mouseY) {
			guiGraphics.drawString(minecraft.font, TextUtil.translate(PREFIX + "amount"), 10, 28, Color.getValue(Color.DARKER_GREY), false);
			if (taskNameLabel != null) {
				guiGraphics.drawString(minecraft.font, taskNameLabel, 32, 104, Color.getValue(Color.DARKER_GREY), false);
			}
			for (int i = 0; i < graphTexts.size(); i++) {
				int[] pos = graphTextPos.get(i);
				guiGraphics.drawString(minecraft.font, graphTexts.get(i), pos[0], pos[1], pos[2], false);
			}
		}

		@Override
		public void keyPressed(int keyCode) {
			if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_UP) { //PgUp
				itemDisplay.prevPage();
			} else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_DOWN) { //PgDn
				itemDisplay.nextPage();
			}
		}

		@Override
		public void handleClick(int mouseX, int mouseY, int mouseButton) {
			if (itemDisplay.handleClick(mouseX, mouseY, mouseButton)) {
				xViewportOffset = max(-1439, min(xViewportOffset, 0));
				TrackingTask task = getSelectedTask();
				if (task != null) {
					long[] data = getTaskData(task);
					yViewportOffset = data[round(-xViewportOffset)];
				}
			}

			int xOrigo = xCenter - 72;
			int yOrigo = yCenter + 90;
			isDraggingGraph = mouseButton == 0 && mouseX > xOrigo && mouseX < xOrigo + 150 && mouseY < yOrigo && mouseY > yOrigo - 80;
			isDraggingXBar = mouseButton == 0 && mouseX > xOrigo && mouseX < xOrigo + 150 && mouseY < yOrigo + 16 && mouseY > yOrigo + 4;
			isDraggingYBar = mouseButton == 0 && mouseX > xOrigo - 16 && mouseX < xOrigo - 4 && mouseY < yOrigo && mouseY > yOrigo - 80;
		}

		@Override
		public void checkButtons() {
			for (net.minecraft.client.gui.components.AbstractButton button : BUTTONS) {
				button.visible = getActiveTab() == this;
				if (button.getMessage().getString().equals("Remove")) {
					button.active = itemDisplay.getSelectedItem() != null;
				}
			}
		}

		@Override
		public void onMouseDrag(int x, int y, int dx, int dy) {
			if (isDraggingGraph) {
				xViewportOffset += dx / xViewportScale;
				yViewportOffset += dy / yViewportScale;
			} else if (isDraggingXBar) {
				float mul = (float) pow(1.25, dx / 2f);
				xViewportScale *= mul;
			} else if (isDraggingYBar) {
				float mul = (float) pow(1.25, -dy / 2f);
				yViewportScale *= mul;
			}
		}

		@Override
		public void onMouseScroll(int dw) {
			float mul = (float) pow(1.25, dw / 60f);
			xViewportScale *= mul;
			yViewportScale *= mul;
		}

		private void drawGraphPart(int xOrigo, int yOrigo, int prevX, int prevY, int x, int y) {
			Vec2 left = new Vec2(prevX, prevY);
			Vec2 right = new Vec2(x, y);

			// bounds check
			{
				Vec2 min = new Vec2(left.x, min(left.y, right.y));
				Vec2 max = new Vec2(right.x, max(left.y, right.y));

				if (!(min.x < 150 && max.x > 0 && min.y < 80 && max.y > 0)) return;
			}

			// clamp to the edges of the graph
			right = clampCorner(left, right, Vec2.ORIGIN, true);
			right = clampCorner(left, right, new Vec2(150, 80), false);
			left = clampCorner(right, left, Vec2.ORIGIN, true);
			left = clampCorner(right, left, new Vec2(150, 80), false);

			drawLine(xOrigo + (int) left.x, yOrigo - (int) left.y, xOrigo + (int) right.x, yOrigo - (int) right.y, Color.RED);

			int radius = 2;
			if (xViewportScale < 4) radius = 1;

			if (prevX >= 0 && prevX <= 150 && prevY >= 0 && prevY <= 80)
				guiGraphics.fill(xOrigo + prevX - radius + 1, yOrigo - prevY - radius + 1, xOrigo + prevX + radius, yOrigo - prevY + radius, Color.getValue(Color.BLACK));

			if (x >= 0 && x <= 150 && y >= 0 && y <= 80)
				guiGraphics.fill(xOrigo + x - radius + 1, yOrigo - y - radius + 1, xOrigo + x + radius, yOrigo - y + radius, Color.getValue(Color.BLACK));
		}

		private Vec2 clampYPlane(Vec2 v, Vec2 toClamp, float x0, boolean greater) {
			if (toClamp.x == x0) return toClamp;
			if (toClamp.x == v.x) return toClamp;

			if ((!greater && toClamp.x < x0) || (greater && toClamp.x > x0)) {
				return toClamp;
			}

			Vec2 dir = toClamp.sub(v);
			dir = dir.div(dir.x); // let dir.x=1 but keep vector's direction
			float dist = (x0 - toClamp.x);
			return toClamp.add(dir.mul(dist));
		}

		@SuppressWarnings("SuspiciousNameCombination")
		private Vec2 clampXPlane(Vec2 from, Vec2 to, float y0, boolean greater) {
			final Vec2 vec2 = clampYPlane(new Vec2(from.y, from.x), new Vec2(to.y, to.x), y0, greater);
			return new Vec2(vec2.y, vec2.x);
		}

		private Vec2 clampCorner(Vec2 from, Vec2 to, Vec2 corner, boolean greater) {
			return clampXPlane(from, clampYPlane(from, to, corner.x, greater), corner.y, greater);
		}

		private String formatTime(int minutes) {
			if (minutes == 0) return "Now";

			int mins = minutes % 60;
			minutes /= 60;
			int hours = minutes;

			StringBuilder sb = new StringBuilder();

			if (hours > 0) sb.append(hours).append("h");
			if (mins > 0) sb.append(mins).append("min");

			return sb.toString();
		}

		public void updateItemList() {
			List<ItemIdentifierStack> allItems = tile.tasks.stream().map(task -> task.item.makeStack(1))
					.collect(Collectors.toList());
			itemDisplay.setItemList(allItems);
		}

		public void handlePacket(List<ItemIdentifierStack> identList) {
			if (hasSubGui() && getSubGui() instanceof GuiAddTracking) {
				((GuiAddTracking) getSubGui()).handlePacket(identList);
			} else if (!hasSubGui()) {
				GuiAddTracking sub = new GuiAddTracking(tile);
				setSubGui(sub);
				sub.handlePacket(identList);
			}
		}

	}

	private class TabCrafting implements StatisticsTab {

		private ItemDisplay itemDisplay;

		private final List<net.minecraft.client.gui.components.AbstractButton> BUTTONS = new ArrayList<>();

		@Override
		public void init() {
			SmallGuiButton b6 = new SmallGuiButton(6, leftPos + 10, topPos + 40, 160, 20, TextUtil.translate(PREFIX + "gettasks"));
			b6.setPressListener(b -> MainProxy.sendPacketToServer(PacketHandler.getPacket(RequestRunningCraftingTasks.class).setTilePos(tile)));
			BUTTONS.add(addRenderableWidget(b6));
			SmallGuiButton b7 = new SmallGuiButton(7, leftPos + 90, topPos + 65, 10, 10, "<");
			b7.setPressListener(b -> itemDisplay.prevPage());
			BUTTONS.add(addRenderableWidget(b7));
			SmallGuiButton b8 = new SmallGuiButton(8, leftPos + 160, topPos + 65, 10, 10, ">");
			b8.setPressListener(b -> itemDisplay.nextPage());
			BUTTONS.add(addRenderableWidget(b8));

			if (itemDisplay == null) {
				itemDisplay = new ItemDisplay(null, font, GuiStatistics.this, null, leftPos + 10, topPos + 18, imageWidth - 20, imageHeight - 100, 0, 0, 0, new int[] { 1, 10, 64, 64 }, true);
				itemDisplay.setItemList(new ArrayList<>());
			}
			itemDisplay.reposition(leftPos + 10, topPos + 80, imageWidth - 20, 125, 0, 0);

		}

		@Override
		public void draw(int mouseX, int mouseY) {
			itemDisplay.renderItemArea(0.0f);
			itemDisplay.renderPageNumber(right - 50, topPos + 66);
		}

		@Override
		public void drawForegroundLayer(int mouseX, int mouseY) {
			guiGraphics.drawString(minecraft.font, TextUtil.translate(PREFIX + "crafting"), 10, 28, Color.getValue(Color.DARKER_GREY), false);
			if (hasSubGui()) {
				return;
			}
			Object[] tip = itemDisplay != null ? itemDisplay.getToolTip() : null;
			if (tip != null && tip.length >= 3) {
				guiGraphics.renderTooltip(minecraft.font, (ItemStack) tip[2], (int) tip[0], (int) tip[1]);
			}
		}

		@Override
		public void keyPressed(int keyCode) {
			if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_UP) { //PgUp
				itemDisplay.prevPage();
			} else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_DOWN) { //PgDn
				itemDisplay.nextPage();
			}
		}

		@Override
		public void handleClick(int mouseX, int mouseY, int mouseButton) {
			itemDisplay.handleClick(mouseX, mouseY, mouseButton);
		}

		@Override
		public void checkButtons() {
			for (net.minecraft.client.gui.components.AbstractButton button : BUTTONS) {
				button.visible = getActiveTab() == this;
			}
		}

		public void handlePacket(List<ItemIdentifierStack> identList) {
			itemDisplay.setItemList(identList);
		}

	}

}
