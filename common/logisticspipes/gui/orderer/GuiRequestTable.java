package logisticspipes.gui.orderer;

import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.SharedConstants;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;


import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;





import logisticspipes.LPItems;
import logisticspipes.config.Configs;
import logisticspipes.gui.popup.GuiDiskPopup;
import logisticspipes.gui.popup.GuiRequestPopup;
import logisticspipes.gui.popup.RequestMonitorPopup;
import logisticspipes.interfaces.IChainAddList;
import logisticspipes.interfaces.IDiskProvider;
import logisticspipes.interfaces.ISpecialItemRenderer;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.block.ClearCraftingGridPacket;
import logisticspipes.network.packets.block.CraftingCycleRecipe;
import logisticspipes.network.packets.orderer.DiskRequestConectPacket;
import logisticspipes.network.packets.orderer.OrdererRefreshRequestPacket;
import logisticspipes.network.packets.orderer.RequestComponentPacket;
import logisticspipes.network.packets.orderer.RequestSubmitListPacket;
import logisticspipes.network.packets.orderer.RequestSubmitPacket;
import logisticspipes.pipes.PipeBlockRequestTable;
import logisticspipes.proxy.MainProxy;
import logisticspipes.request.resources.IResource;
import logisticspipes.routing.order.IOrderInfoProvider;
import logisticspipes.routing.order.LinkedLogisticsOrderList;
import logisticspipes.utils.ChainAddArrayList;
import logisticspipes.utils.Color;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.GuiCheckBox;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.IItemSearch;
import logisticspipes.utils.gui.ISubGuiControler;
import logisticspipes.utils.gui.InputBar;
import logisticspipes.utils.gui.ItemDisplay;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.gui.extension.GuiExtension;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.string.ChatColor;
import logisticspipes.utils.tuples.Pair;
import network.rs485.logisticspipes.util.TextUtil;

public class GuiRequestTable extends LogisticsBaseGuiScreen implements IItemSearch, ISpecialItemRenderer, IDiskProvider {

	public final PipeBlockRequestTable _table;
	public final Player _entityPlayer;
	protected final String _title = "Request items";
	public ItemDisplay itemDisplay;
	public net.minecraft.resources.ResourceLocation dimension;
	protected DisplayOptions displayOptions = DisplayOptions.Both;
	private SmallGuiButton macroButton;
	private InputBar search;
	private boolean showRequest = true;
	private int startLeft;
	private int startXSize;
	private BitSet handledExtension = new BitSet();
	private int orderIdForButton;
	private net.minecraft.client.gui.components.AbstractButton[] cycleButtons = new net.minecraft.client.gui.components.AbstractButton[2];
	private IChainAddList<net.minecraft.client.gui.components.AbstractButton> moveWhileSmall = new ChainAddArrayList<>();
	private IChainAddList<net.minecraft.client.gui.components.AbstractButton> hideWhileSmall = new ChainAddArrayList<>();
	private net.minecraft.client.gui.components.AbstractButton hideShowButton;
	private GuiCheckBox popupCheck;

	public GuiRequestTable(Player entityPlayer, PipeBlockRequestTable table) {
		super(buildDummy(entityPlayer, table), 410, 240, 0, 0);
		_table = table;
		_entityPlayer = entityPlayer;
		if (GuiOrderer.cachetime + 100 < System.currentTimeMillis()) {
			dimension = _table.getWorld().dimension().location();
		} else {
			dimension = GuiOrderer.dimensioncache;
		}
		refreshItems();
	}
	private static DummyContainer buildDummy(Player entityPlayer, PipeBlockRequestTable table) {
		DummyContainer dummy = new DummyContainer(entityPlayer.getInventory(), table.matrix);
		int i = 0;
		for (int y = 0; y < 3; y++) {
			for (int x = 0; x < 9; x++) {
				dummy.addNormalSlot(i++, table.inv, (x * 18) + 20, (y * 18) + 80);
			}
		}
		i = 0;
		for (int y = 0; y < 3; y++) {
			for (int x = 0; x < 3; x++) {
				dummy.addDummySlot(i++, (x * 18) + 20, (y * 18) + 15);
			}
		}
		dummy.addCallableSlotHandler(0, table.resultInv, 101, 33, table::getResultForClick);
		dummy.addNormalSlot(0, table.toSortInv, 164, 51);
		dummy.addNormalSlot(0, table.diskInv, 164, 25);
		dummy.addNormalSlotsForPlayerInventory(20, 150);
		return dummy;
	}


	@Override
	public void init() {
		

		boolean reHide = false;
		if (!showRequest) {
			leftPos = startLeft;
			imageWidth = startXSize;
			showRequest = true;
			reHide = true;
		}
		super.init();

		// super.init() → rebuildWidgets() already cleared prior widgets
		addRenderableWidget(hideWhileSmall.addChain(wire(new logisticspipes.utils.gui.SmallGuiButton(0, right - 55, bottom - 25, 50, 20, "Request"), 0))); // Request
		addRenderableWidget(hideWhileSmall.addChain(wire(new SmallGuiButton(1, right - 15, topPos + 5, 10, 10, ">"), 1))); // Next page
		addRenderableWidget(hideWhileSmall.addChain(wire(new SmallGuiButton(2, right - 90, topPos + 5, 10, 10, "<"), 2))); // Prev page
		addRenderableWidget(hideWhileSmall.addChain(wire(new SmallGuiButton(10, right - 148, bottom - 15, 26, 10, "---"), 10))); // -64
		addRenderableWidget(hideWhileSmall.addChain(wire(new SmallGuiButton(4, right - 148, bottom - 26, 15, 10, "--"), 4))); // -10
		addRenderableWidget(hideWhileSmall.addChain(wire(new SmallGuiButton(5, right - 132, bottom - 26, 10, 10, "-"), 5))); // -1
		addRenderableWidget(hideWhileSmall.addChain(wire(new SmallGuiButton(6, right - 86, bottom - 26, 10, 10, "+"), 6))); // +1
		addRenderableWidget(hideWhileSmall.addChain(wire(new SmallGuiButton(7, right - 74, bottom - 26, 15, 10, "++"), 7))); // +10
		addRenderableWidget(hideWhileSmall.addChain(wire(new SmallGuiButton(11, right - 86, bottom - 15, 26, 10, "+++"), 11))); // +64
		popupCheck = new GuiCheckBox(8, leftPos + 209, bottom - 60, 14, 14, Configs.DISPLAY_POPUP);
		popupCheck.setPressListener(b -> handleBtn(8, b));
		addRenderableWidget(hideWhileSmall.addChain(popupCheck)); // Popup

		addRenderableWidget(hideWhileSmall.addChain(wire(new SmallGuiButton(3, leftPos + 210, bottom - 15, 46, 10, "Refresh"), 3))); // Refresh
		addRenderableWidget(hideWhileSmall.addChain(wire(new SmallGuiButton(13, leftPos + 210, bottom - 28, 46, 10, "Content"), 13))); // Component
		addRenderableWidget(hideWhileSmall.addChain(wire(new SmallGuiButton(9, leftPos + 210, bottom - 41, 46, 10, "Both"), 9)));
		addRenderableWidget(hideWhileSmall.addChain(wire(new SmallGuiButton(20, right - 116, bottom - 41, 26, 10, "Sort"), 20))); // Sort

		addRenderableWidget(moveWhileSmall.addChain(wire(new SmallGuiButton(14, leftPos + 96, topPos + 53, 10, 10, "+"), 14))); // +1
		addRenderableWidget(moveWhileSmall.addChain(wire(new SmallGuiButton(15, leftPos + 108, topPos + 53, 15, 10, "++"), 15))); // +10
		addRenderableWidget(moveWhileSmall.addChain(wire(new SmallGuiButton(16, leftPos + 96, topPos + 64, 26, 10, "+++"), 16))); // +64

		addRenderableWidget(moveWhileSmall.addChain(wire(new SmallGuiButton(30, leftPos + 96 + 2, topPos + 18, 10, 10, "X"), 30))); // x
		addRenderableWidget(moveWhileSmall.addChain(wire(new SmallGuiButton(31, leftPos + 108 + 2, topPos + 18, 10, 10, "~", 3), 31))); // ~

		addRenderableWidget(hideShowButton = wire(new SmallGuiButton(17, leftPos + 173, topPos + 5, 36, 10, "Hide"), 17)); // Hide
		addRenderableWidget(macroButton = wire(new SmallGuiButton(18, right - 55, bottom - 60, 50, 10, "Disk"), 18));
		macroButton.active = false;

		(cycleButtons[0] = addRenderableWidget(wire(new SmallGuiButton(21, leftPos + 124, topPos + 30, 15, 10, "/\\"), 21))).visible = false;
		(cycleButtons[1] = addRenderableWidget(wire(new SmallGuiButton(22, leftPos + 124, topPos + 42, 15, 10, "\\/"), 22))).visible = false;

		if (search == null) {
			search = new InputBar(font, this, leftPos + 205, bottom - 78, 200, 15);
		}
		search.reposition(leftPos + 205, bottom - 78, 200, 15);

		if (itemDisplay == null) {
			itemDisplay = new ItemDisplay(this, font, this, this, leftPos + 205, topPos + 18, 200, imageHeight - 100, right - 104, bottom - 24, 36, new int[] { 1, 10, 64, 64 }, true);
		}
		itemDisplay.reposition(leftPos + 205, topPos + 18, 200, imageHeight - 100, right - 104, bottom - 24);

		startLeft = leftPos;
		startXSize = imageWidth;
		if (reHide) {
			showRequest = false;
			imageWidth = startXSize - 210;
			leftPos = startLeft + 105;
			for (net.minecraft.client.gui.components.AbstractButton button : moveWhileSmall) {
				button.setX(button.getX() + 105);
			}
			hideShowButton.setX(hideShowButton.getX() + 90);
			hideShowButton.setMessage(net.minecraft.network.chat.Component.literal("Show"));
			for (net.minecraft.client.gui.components.AbstractButton button : hideWhileSmall) {
				button.visible = false;
			}
			macroButton.visible = false;
		}
	}

	@Override
	public void renderBg(@Nonnull GuiGraphics guiGraphics, float f, int i, int j) {
		for (net.minecraft.client.gui.components.AbstractButton cycleButton : cycleButtons) {
			cycleButton.visible = _table.targetType != null;
		}
		LPGuiGraphics.drawGuiBackGround(minecraft, leftPos, topPos, right - (showRequest ? 0 : 105), bottom, 0.0f, true);

		guiGraphics.fill(leftPos + 162, topPos + 23, leftPos + 182, topPos + 43, Color.getValue(Color.BLACK));
		guiGraphics.fill(leftPos + 164, topPos + 25, leftPos + 180, topPos + 41, Color.getValue(Color.DARKER_GREY));

		if (showRequest) {
			itemDisplay.renderPageNumber(right - 47, topPos + 6);

			itemDisplay.renderAmount(getStackAmount());
			//SearchInput
			search.drawTextBox();

			itemDisplay.renderSortMode(right - 103, bottom - 52);
			itemDisplay.renderItemArea(0.0f);
		}

		for (int x = 0; x < 9; x++) {
			for (int y = 0; y < 3; y++) {
				LPGuiGraphics.drawSlotBackground(minecraft, leftPos + (x * 18) + 19, topPos + (y * 18) + 79);
			}
		}
		for (int x = 0; x < 3; x++) {
			for (int y = 0; y < 3; y++) {
				LPGuiGraphics.drawSlotBackground(minecraft, leftPos + (x * 18) + 19, topPos + (y * 18) + 14);
			}
		}
		LPGuiGraphics.drawSlotBackground(minecraft, leftPos + 100, topPos + 32);
		LPGuiGraphics.drawSlotBackground(minecraft, leftPos + 163, topPos + 50);
		guiGraphics.fill(leftPos + 75, topPos + 38, leftPos + 95, topPos + 43, Color.getValue(Color.DARKER_GREY));
		for (int a = 0; a < 10; a++) {
			guiGraphics.fill(leftPos + 97 - a, topPos + 40 - a, leftPos + 98 - a, topPos + 41 + a, Color.getValue(Color.DARKER_GREY));
		}
		for (int a = 0; a < 15; a++) {
			guiGraphics.fill(leftPos + 164 + a, topPos + 51 + a, leftPos + 166 + a, topPos + 53 + a, Color.getValue(Color.DARKER_GREY));
			guiGraphics.fill(leftPos + 164 + a, topPos + 65 - a, leftPos + 166 + a, topPos + 67 - a, Color.getValue(Color.DARKER_GREY));
		}
		LPGuiGraphics.drawPlayerInventoryBackground(minecraft, leftPos + 20, topPos + 150);
		for (final Entry<Integer, Pair<IResource, LinkedLogisticsOrderList>> entry : _table.watchedRequests.entrySet()) {
			if (!handledExtension.get(entry.getKey())) {
				handledExtension.set(entry.getKey());
				extensionControllerLeft.addExtension(new GuiExtension() {

					private Map<Pair<Integer, Integer>, IOrderInfoProvider> ordererPosition = new HashMap<>();
					private int height;
					private int width = 4;
					private net.minecraft.client.gui.components.AbstractButton localControlledButton;

					@Override
					public void renderForeground(int left, int top) {
						if (!_table.watchedRequests.containsKey(entry.getKey())) {
							extensionControllerLeft.removeExtension(this);
							if (isFullyExtended() && localControlledButton != null) {
								removeWidget(localControlledButton);
								localControlledButton = null;
								orderIdForButton = -1;
							}
							return;
						}
						ordererPosition.clear();
						ItemStack stack;
						IResource resource = entry.getValue().getValue1();
						String s;
						if (resource != null) {
							stack = resource.getDisplayItem().makeNormalStack();
							guiGraphics.renderItem(stack, left + 4, top + 4);
							s = TextUtil.getThreeDigitFormattedNumber(stack.getCount(), false);
						} else {
							stack = ItemStack.EMPTY;
							s = "List";
						}
						// Draw number
						guiGraphics.drawString(minecraft.font, s, left + 22 - minecraft.font.width(s), top + 14, 16777215, true);
						if (isFullyExtended()) {
							if (localControlledButton == null || orderIdForButton != entry.getKey()) {
								if (localControlledButton != null) {
									removeWidget(localControlledButton);
								}
								localControlledButton = wire(new SmallGuiButton(100, leftPos - 35, topPos + 10, 30, 10, "more"), 100);
								addRenderableWidget(localControlledButton);
								orderIdForButton = entry.getKey();
							}
							List<IOrderInfoProvider> list = entry.getValue().getValue2().getList();
							calculateSize(left, top, list);
							String ident = String.format("ID: %d", entry.getKey());
							guiGraphics.drawString(minecraft.font, ident, left + 25, top + 7, 16777215, true);
							int x = left + 6;
							int y = top + 25;
							for (IOrderInfoProvider order : list) {
								stack = order.getAsDisplayItem().makeNormalStack();
								if (stack.getCount() <= 0) {
									continue;
								}
								guiGraphics.renderItem(stack, x, y);
								s = TextUtil.getThreeDigitFormattedNumber(stack.getCount(), false);
								// Draw number
								guiGraphics.drawString(minecraft.font, s, x + 17 - minecraft.font.width(s), y + 9, 16777215, true);
								ordererPosition.put(new Pair<>(x, y), order);
								x += 18;
								if (x > left + getFinalWidth() - 18) {
									x = left + 6;
									y += 18;
								}
							}
						} else if (isExtending()) {
							List<IOrderInfoProvider> list = entry.getValue().getValue2().getList();
							calculateSize(left, top, list);
						}
						if (!isFullyExtended() && localControlledButton != null) {
							removeWidget(localControlledButton);
							localControlledButton = null;
							orderIdForButton = -1;
						}
					}

					private void calculateSize(int left, int top, List<IOrderInfoProvider> list) {
						int x = left + 6;
						int y = 50;
						int line = 1;
						width = 4;
						for (IOrderInfoProvider order : list) {
							ItemStack stack = order.getAsDisplayItem().makeNormalStack();
							if (stack.getCount() <= 0) {
								continue;
							}
							if (line++ % (4 * 4) == 0) {
								width++;
							}
						}
						for (IOrderInfoProvider order : list) {
							ItemStack stack = order.getAsDisplayItem().makeNormalStack();
							if (stack.getCount() <= 0) {
								continue;
							}
							x += 18;
							if (x > left + getFinalWidth() - 18) {
								x = left + 6;
								y += 18;
							}
						}
						height = y;
						if (x == left + 6) {
							height -= 18;
						}
					}

					@Override
					public int getFinalWidth() {
						return Math.max(85, width * 18 + 8);
					}

					@Override
					public int getFinalHeight() {
						return Math.max(50, height);
					}

					@Override
					public void handleMouseOverAt(int xPos, int yPos) {
						if (isFullyExtended()) {
							ordererPosition.keySet().stream()
									.filter(key -> xPos >= key.getValue1() && xPos < key.getValue1() + 18 && yPos >= key
											.getValue2() && yPos < key.getValue2() + 18).forEach(key -> {
								IOrderInfoProvider order = ordererPosition.get(key);
								List<String> list = new ArrayList<>();
								list.add(ChatColor.BLUE + "Request Type: " + ChatColor.YELLOW + order.getType().name());
								list.add(ChatColor.BLUE + "Send to Router ID: " + ChatColor.YELLOW + order
										.getRouterId());
								guiGraphics.renderComponentTooltip(font, list.stream().map(net.minecraft.network.chat.Component::literal).collect(Collectors.toList()), xPos, yPos);
							});
						} else if (entry.getValue() != null && entry.getValue().getValue1() != null && entry.getValue().getValue1().getDisplayItem() != null) {
							guiGraphics.renderTooltip(font, entry.getValue().getValue1().getDisplayItem().makeNormalStack(), xPos, yPos);
						}
					}
				});
			}
		}
		super.renderExtensions();
	}

	public void refreshItems() {
		int integer;
		switch (displayOptions) {
			case Both:
				integer = 0;
				break;
			case SupplyOnly:
				integer = 1;
				break;
			case CraftOnly:
				integer = 2;
				break;
			default:
				integer = 3;
		}
		MainProxy.sendPacketToServer(PacketHandler.getPacket(OrdererRefreshRequestPacket.class).putInt(integer).setTilePos(_table.container).setDimension(dimension));
	}

	private SmallGuiButton wire(SmallGuiButton btn, int id) {
		btn.setPressListener(b -> handleBtn(id, b));
		return btn;
	}

	private void handleBtn(int id, net.minecraft.client.gui.components.AbstractButton guibutton) {
		if (id == 0 && itemDisplay.getSelectedItem() != null) {
			final ItemIdentifierStack stack = itemDisplay.getSelectedItem().getItem().makeStack(itemDisplay.getRequestCount());
			MainProxy.sendPacketToServer(PacketHandler.getPacket(RequestSubmitPacket.class).setStack(stack).setTilePos(_table.container).setDimension(dimension));
			refreshItems();
		} else if (id == 1) {
			itemDisplay.nextPage();
		} else if (id == 2) {
			itemDisplay.prevPage();
		} else if (id == 3) {
			refreshItems();
		} else if (id == 10) {
			itemDisplay.sub(3);
		} else if (id == 4) {
			itemDisplay.sub(2);
		} else if (id == 5) {
			itemDisplay.sub(1);
		} else if (id == 6) {
			itemDisplay.add(1);
		} else if (id == 7) {
			itemDisplay.add(2);
		} else if (id == 11) {
			itemDisplay.add(3);
		} else if (id == 8) {
			GuiCheckBox button = (GuiCheckBox) guibutton;
			Configs.DISPLAY_POPUP = button.change();
			Configs.savePopupState();
		} else if (id == 13 && itemDisplay.getSelectedItem() != null) {
			final ItemIdentifierStack stack = itemDisplay.getSelectedItem().getItem().makeStack(itemDisplay.getRequestCount());
			MainProxy.sendPacketToServer(PacketHandler.getPacket(RequestComponentPacket.class).setStack(stack).setTilePos(_table.container).setDimension(dimension));
		} else if (id == 9) {
			String displayString = "";
			switch (displayOptions) {
				case Both:
					displayOptions = DisplayOptions.CraftOnly;
					displayString = "Craft";
					break;
				case CraftOnly:
					displayOptions = DisplayOptions.SupplyOnly;
					displayString = "Supply";
					break;
				case SupplyOnly:
					displayOptions = DisplayOptions.Both;
					displayString = "Both";
					break;
			}
			guibutton.setMessage(net.minecraft.network.chat.Component.literal(displayString));
			refreshItems();
		} else if (id == 14) {
			requestMatrix(1);
		} else if (id == 15) {
			requestMatrix(10);
		} else if (id == 16) {
			requestMatrix(64);
		} else if (id == 17) {
			//hideShowButton
			// moveWhileSmall
			showRequest = !showRequest;
			if (showRequest) {
				imageWidth = startXSize;
				leftPos = startLeft;
				for (net.minecraft.client.gui.components.AbstractButton button : moveWhileSmall) {
					button.setX(button.getX() - 105);
				}
				hideShowButton.setX(hideShowButton.getX() - 90);
			} else {
				imageWidth = startXSize - 210;
				leftPos = startLeft + 105;
				for (net.minecraft.client.gui.components.AbstractButton button : moveWhileSmall) {
					button.setX(button.getX() + 105);
				}
				hideShowButton.setX(hideShowButton.getX() + 90);
			}
			hideShowButton.setMessage(net.minecraft.network.chat.Component.literal(showRequest ? "Hide" : "Show"));
			for (net.minecraft.client.gui.components.AbstractButton button : hideWhileSmall) {
				button.visible = showRequest;
			}
			macroButton.visible = showRequest;
			orderIdForButton = -1;
		} else if (id == 100) {
			extensionControllerLeft.retract();
			setSubGui(new RequestMonitorPopup(_table, orderIdForButton));
		} else if (id == 18) {
			MainProxy.sendPacketToServer(PacketHandler.getPacket(DiskRequestConectPacket.class).setPosX(_table.getX()).setPosY(_table.getY()).setPosZ(_table.getZ()));
			setSubGui(new GuiDiskPopup(this));
		} else if (id == 20) {
			itemDisplay.cycle();
		} else if (id == 21 || id == 22) {
			MainProxy.sendPacketToServer(PacketHandler.getPacket(CraftingCycleRecipe.class).setDown(id == 22).setTilePos(_table.container));
		} else if (id == 30) {
			MainProxy.sendPacketToServer(PacketHandler.getPacket(ClearCraftingGridPacket.class).setTilePos(_table.container));
			_table.cacheRecipe();
		} else if (id == 31) {
			ArrayList<ItemIdentifierStack> list = new ArrayList<>(9);
			list.addAll(_table.matrix.getItemsAndCount().entrySet().stream()
					.map(e -> e.getKey().makeStack(e.getValue())).collect(Collectors.toList()));
			for (Pair<ItemStack, Integer> entry : _table.inv) {
				if (entry.getValue1().isEmpty()) continue;
				int size = entry.getValue1().getCount();
				ItemIdentifier ident = ItemIdentifier.get(entry.getValue1());
				for (ItemIdentifierStack stack : list) {
					if (!stack.getItem().equals(ident)) continue;
					int toUse = Math.min(size, stack.getStackSize());
					stack.lowerStackSize(toUse);
					size -= toUse;
				}
			}
			list.removeIf(itemIdentifierStack -> itemIdentifierStack.getStackSize() <= 0);
			if (!list.isEmpty()) {
				MainProxy.sendPacketToServer(PacketHandler.getPacket(RequestSubmitListPacket.class).setIdentList(list).setTilePos(_table.container));
				refreshItems();
			}
		}
	}

	private void requestMatrix(int multiplier) {
		ArrayList<ItemIdentifierStack> list = new ArrayList<>(9);
		list.addAll(_table.matrix.getItemsAndCount().entrySet().stream()
				.map(e -> e.getKey().makeStack(e.getValue() * multiplier)).collect(Collectors.toList()));
		MainProxy.sendPacketToServer(PacketHandler.getPacket(RequestSubmitListPacket.class).setIdentList(list).setTilePos(_table.container));
		refreshItems();
	}

	protected int getStackAmount() {
		return 64;
	}

	public void handlePacket(Collection<ItemIdentifierStack> allItems) {
		itemDisplay.setItemList(allItems.stream().filter(Objects::nonNull).collect(Collectors.toList()));
	}

	@Override
	public void specialItemRendering(ItemIdentifier item, int x, int y) {
		// Thaumcraft aspect rendering — blocked: Thaumcraft not ported to 1.20.1.
	}

	@Override
	public void renderLabels(GuiGraphics guiGraphics, int par1, int par2) {
		super.renderLabels(guiGraphics, par1, par2);
		if (super.hasSubGui()) {
			return;
		}
		macroButton.active = !_table.diskInv.getItem(0).isEmpty() && _table.diskInv.getItem(0).getItem().equals(LPItems.disk.get());
		guiGraphics.drawString(minecraft.font, "Sort:", 136, 55, 0xffffff, false);
		if (showRequest) {
			guiGraphics.drawString(minecraft.font, _title, 180 + minecraft.font.width(_title) / 2, 6, 0x404040, false);
			if (popupCheck != null && popupCheck.getState()) {
				guiGraphics.drawString(minecraft.font, "Popup", 225, bottom - topPos - 56, 0x404040, false);
			} else {
				guiGraphics.drawString(minecraft.font, "Popup", 225, bottom - topPos - 56, Color.getValue(Color.GREY), false);
			}
		}
	}

	@Override
	public boolean itemSearched(ItemIdentifier item) {
		if (search.isEmpty()) {
			return true;
		}
		if (isSearched(item.getFriendlyName().toLowerCase(Locale.US), search.getText().toLowerCase(Locale.US))) {
			return true;
		}
		//if(isSearched(String.valueOf(BuiltInRegistries.ITEM.getId(item.item)), search.getContent())) return true;
		//Enchantment? Enchantment!
		net.minecraft.world.item.enchantment.ItemEnchantments enchantIdLvlMap = item.unsafeMakeNormalStack(1).getOrDefault(net.minecraft.core.component.DataComponents.ENCHANTMENTS, net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
		for (var e : enchantIdLvlMap.entrySet()) {
			if (e.getKey() != null) {
				String enchantname = e.getKey().value().description().getString();
				if (enchantname != null) {
					if (isSearched(enchantname.toLowerCase(Locale.US), search.getText().toLowerCase(Locale.US))) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean isSearched(String value, String search) {
		boolean flag = true;
		for (String s : search.split(" ")) {
			if (!value.contains(s)) {
				flag = false;
				break;
			}
		}
		return flag;
	}

	@Override
	public boolean mouseClicked(double i, double j, int k) {
		if (showRequest) {
			itemDisplay.handleClick((int) i, (int) j, k);
			search.handleClick((int) i, (int) j, k);
		}
		return super.mouseClicked(i, j, k);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double lpScrollX, double delta) {
		if (showRequest && itemDisplay != null) {
			itemDisplay.handleMouse(delta);
		}
		return super.mouseScrolled(mouseX, mouseY, lpScrollX, delta);
	}

	public void handleRequestAnswer(Collection<IResource> items, boolean error, ISubGuiControler control, Player player) {
		while (control.hasSubGui()) {
			control = control.getSubGui();
		}
		if (error) {
			control.setSubGui(new GuiRequestPopup(_entityPlayer, "You are missing:", items));
		} else {
			control.setSubGui(new GuiRequestPopup(_entityPlayer, "Request successful!", items));
		}
	}

	public void handleSimulateAnswer(Collection<IResource> used, Collection<IResource> missing, ISubGuiControler control, Player player) {
		while (control.hasSubGui()) {
			control = control.getSubGui();
		}
		control.setSubGui(new GuiRequestPopup(_entityPlayer, "Components: ", used, "Missing: ", missing));
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (hasSubGui()) {
			return super.keyPressed(keyCode, scanCode, modifiers);
		}
		if (search.isFocused() && !search.isEmpty() && search.keyPressed(keyCode, scanCode, modifiers)) {
			return true;
		}
		if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_A && Screen.hasControlDown()) {
			itemDisplay.setMaxAmount();
			return true;
		} else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_D && Screen.hasControlDown()) {
			itemDisplay.resetAmount();
			return true;
		} else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_UP) {
			itemDisplay.prevPage();
			return true;
		} else if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_PAGE_DOWN) {
			itemDisplay.nextPage();
			return true;
		}
		// Track everything except Escape when in search bar
		if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE || !search.keyPressed(keyCode, scanCode, modifiers)) {
			return super.keyPressed(keyCode, scanCode, modifiers);
		}
		return true;
	}

	@Override
	public boolean charTyped(char c, int i) {
		if (search.isFocused()) {
			if (!search.isEmpty() && search.handleKey(c, i))
				return true;
		} else if (Screen.hasAltDown() && net.minecraft.util.StringUtil.isAllowedChatCharacter(c)) {
			itemDisplay.setFocused(false);
			search.setFocused(true);
			search.setText("");
			search.handleKey(c, i);
			return true;
		}
		if (!itemDisplay.keyTyped(c, i)) {
			if (!search.handleKey(c, i)) {
				return super.charTyped(c, i);
			}
		}
		return false;
	}

	@Override
	public void resetSubGui() {
		super.resetSubGui();
		refreshItems();
	}

	@Override
	@Nonnull
	public ItemStack getDisk() {
		return _table.diskInv.getItem(0);
	}

	@Override
	public int getX() {
		return _table.getX();
	}

	@Override
	public int getY() {
		return _table.getY();
	}

	@Override
	public int getZ() {
		return _table.getZ();
	}

	@Override
	public ItemDisplay getItemDisplay() {
		return itemDisplay;
	}

	private enum DisplayOptions {
		Both,
		SupplyOnly,
		CraftOnly,
	}
}
