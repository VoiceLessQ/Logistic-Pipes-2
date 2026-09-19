
package logisticspipes.gui;

import net.minecraft.client.gui.GuiGraphics;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;



import logisticspipes.LPItems;
import logisticspipes.blocks.LogisticsProgramCompilerTileEntity;
import logisticspipes.items.ItemLogisticsPipe;
import logisticspipes.items.ItemModule;
import logisticspipes.items.ItemUpgrade;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.block.CompilerTriggerTaskPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.Color;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.InputBar;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.gui.TextListDisplay;
import network.rs485.logisticspipes.util.TextUtil;
import javax.annotation.Nonnull;

//TODO: Config Option for disabling program compilation
public class GuiProgramCompiler extends LogisticsBaseGuiScreen {

	private final LogisticsProgramCompilerTileEntity compiler;
	private final TextListDisplay.List categoryTextList;
	private final TextListDisplay.List programTextList;
	private final TextListDisplay categoryList;
	private final TextListDisplay programList;
	private final TextListDisplay programListLarge;
	private SmallGuiButton catUp;
	private SmallGuiButton catDn;
	private SmallGuiButton unlock;
	private SmallGuiButton progUp;
	private SmallGuiButton progDn;
	private SmallGuiButton programmerButton;
	private InputBar search;

	public GuiProgramCompiler(Player player, LogisticsProgramCompilerTileEntity compiler) {
		super(buildDummy(player, compiler), 180, 190, 0, 0);
		this.compiler = compiler;

		categoryTextList = new TextListDisplay.List() {

			@Override
			public int getSize() {
				if (compiler.getInventory().getItem(0).isEmpty()) {
					return 0;
				}
				ListTag list = compiler.getListTagForKey("compilerCategories");
				return (int) LogisticsProgramCompilerTileEntity.programByCategory.keySet().stream()
						.filter(it -> StreamSupport.stream(list.spliterator(), false).noneMatch(nbtBase -> ((StringTag) nbtBase).getAsString().equals(it.toString()))).count();
			}

			@Override
			public String getTextAt(int index) {
				if (compiler.getInventory().getItem(0).isEmpty()) {
					return "";
				}
				ListTag list = compiler.getListTagForKey("compilerCategories");
				return TextUtil.translate("gui.compiler." + LogisticsProgramCompilerTileEntity.programByCategory.keySet().stream()
						.filter(it -> StreamSupport.stream(list.spliterator(), false).noneMatch(nbtBase -> ((StringTag) nbtBase).getAsString().equals(it.toString())))
						.skip(index)
						.findFirst()
						.map(it -> String.format("%s.%s", it.getNamespace(), it.getPath()))
						.orElse(null));
			}

			@Override
			public int getTextColor(int index) {
				return 0xFFFFFF;
			}
		};
		categoryList = new TextListDisplay(this, 8, 30, 110, 104, 5, categoryTextList);

		programTextList = new TextListDisplay.List() {

			@Override
			public int getSize() {
				if (compiler.getInventory().getItem(0).isEmpty()) {
					return 0;
				}
				ListTag list = compiler.getListTagForKey("compilerCategories");
				return getProgramListForSelectionIndex(list).size();
			}

			@Override
			public String getTextAt(int index) {
				if (compiler.getInventory().getItem(0).isEmpty()) {
					return "";
				}
				ListTag list = compiler.getListTagForKey("compilerCategories");
				ResourceLocation sel = getProgramListForSelectionIndex(list).get(index);

				Item selItem = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(sel);
				if (selItem != null) {
					return TextUtil.translate(selItem.getDescriptionId());
				}
				return "UNDEFINED";
			}

			@Override
			public int getTextColor(int index) {
				if (compiler.getInventory().getItem(0).isEmpty()) {
					return 0xFFFFFF;
				}
				ListTag list = compiler.getListTagForKey("compilerCategories");
				ResourceLocation sel = getProgramListForSelectionIndex(list).get(index);

				ListTag listPrograms = compiler.getListTagForKey("compilerPrograms");
				return StreamSupport.stream(listPrograms.spliterator(), false).anyMatch(it -> ResourceLocation.parse(((StringTag) it).getAsString()).equals(sel))
						? 0xAAFFAA : 0xFFAAAA;
			}
		};

		programList = new TextListDisplay(this, 80, 30, 8, 104, 5, programTextList);
		programListLarge = new TextListDisplay(this, 8, 30, 8, 104, 5, programTextList);
	}
	private static DummyContainer buildDummy(Player player, LogisticsProgramCompilerTileEntity compiler) {
		DummyContainer dummy = new DummyContainer(player.getInventory(), compiler.getInventory());
		dummy.addRestrictedSlot(0, compiler.getInventory(), 10, 10, LPItems.disk.get());
		dummy.addRestrictedSlot(1, compiler.getInventory(), 154, 10, LPItems.logisticsProgrammer.get());
		dummy.addNormalSlotsForPlayerInventory(10, 105);
		return dummy;
	}


	@Override
	public void init() {
		
		super.init();
		catUp = new SmallGuiButton(0, leftPos + 8, topPos + 90, 15, 10, "/\\");
		catUp.setPressListener(b -> categoryList.scrollDown());
		addRenderableWidget(catUp);
		catDn = new SmallGuiButton(1, leftPos + 24, topPos + 90, 15, 10, "\\/");
		catDn.setPressListener(b -> categoryList.scrollUp());
		addRenderableWidget(catDn);
		unlock = new SmallGuiButton(2, leftPos + 40, topPos + 90, 40, 10, "Unlock");
		unlock.setPressListener(b -> {
			if (categoryList.getSelected() != -1) {
				ListTag list = compiler.getListTagForKey("compilerCategories");
				LogisticsProgramCompilerTileEntity.programByCategory.keySet().stream()
						.filter(it -> StreamSupport.stream(list.spliterator(), false).noneMatch(nbtBase -> ((StringTag) nbtBase).getAsString().equals(it.toString())))
						.skip(categoryList.getSelected())
						.findFirst()
						.ifPresent(it -> MainProxy.sendPacketToServer(PacketHandler.getPacket(CompilerTriggerTaskPacket.class).setCategory(it).setType("category").setTilePos(compiler)));
			}
		});
		addRenderableWidget(unlock);
		progUp = new SmallGuiButton(3, leftPos + 100, topPos + 90, 15, 10, "/\\");
		progUp.setPressListener(b -> {
			if (categoryTextList.getSize() == 0 && programTextList.getSize() != 0) programListLarge.scrollDown();
			else programList.scrollDown();
		});
		addRenderableWidget(progUp);
		progDn = new SmallGuiButton(4, leftPos + 116, topPos + 90, 15, 10, "\\/");
		progDn.setPressListener(b -> {
			if (categoryTextList.getSize() == 0 && programTextList.getSize() != 0) programListLarge.scrollUp();
			else programList.scrollUp();
		});
		addRenderableWidget(progDn);
		programmerButton = new SmallGuiButton(5, leftPos + 132, topPos + 90, 40, 10, "Compile");
		programmerButton.setPressListener(b -> {
			int selIndex = programList.getSelected();
			if (categoryTextList.getSize() == 0 && programTextList.getSize() != 0) {
				selIndex = programListLarge.getSelected();
			}
			if (selIndex != -1) {
				ListTag list = compiler.getListTagForKey("compilerCategories");
				ResourceLocation sel = getProgramListForSelectionIndex(list).get(selIndex);
				ListTag listPrograms = compiler.getListTagForKey("compilerPrograms");
				boolean flag = StreamSupport.stream(listPrograms.spliterator(), false)
						.anyMatch(it -> ResourceLocation.parse(((StringTag) it).getAsString()).equals(sel));
				MainProxy.sendPacketToServer(PacketHandler.getPacket(CompilerTriggerTaskPacket.class).setCategory(sel).setType(flag ? "flash" : "program").setTilePos(compiler));
			}
		});
		addRenderableWidget(programmerButton);

		search = new InputBar(font, this, leftPos + 30, topPos + 11, 120, 16);
	}

	@Override
	public void closeGui() throws IOException {
		super.closeGui();
		
	}

	@Override
	protected void renderBg(@Nonnull GuiGraphics guiGraphics, float var1, int var2, int var3) {
		LPGuiGraphics.drawGuiBackGround(minecraft, leftPos, topPos, right, bottom, 0.0f, true);
		LPGuiGraphics.drawPlayerInventoryBackground(minecraft, leftPos + 10, topPos + 105);
		LPGuiGraphics.drawSlotDiskBackground(minecraft, leftPos + 9, topPos + 9);
		LPGuiGraphics.drawSlotProgrammerBackground(minecraft, leftPos + 153, topPos + 9);

		if (compiler.getCurrentTask() != null) {
			guiGraphics.fill(leftPos + 9, topPos + 50, leftPos + 171, topPos + 66, Color.getValue(Color.BLACK));
			guiGraphics.fill(leftPos + 10, topPos + 51, leftPos + 170, topPos + 65, 0xFFFFFFFF);
			guiGraphics.fill(leftPos + 11, topPos + 52, leftPos + 11 + (int) (158 * compiler.getTaskProgress()), topPos + 64, Color.getValue(Color.GREEN));

			catUp.visible = false;
			catDn.visible = false;
			unlock.visible = false;
			progUp.visible = false;
			progDn.visible = false;
			programmerButton.visible = false;
		} else {
			catUp.visible = true;
			catDn.visible = true;
			unlock.visible = true;
			progUp.visible = true;
			progDn.visible = true;
			programmerButton.visible = true;
			if (categoryTextList.getSize() == 0 && programTextList.getSize() != 0) {
				catUp.visible = false;
				catDn.visible = false;
				unlock.visible = false;
				programListLarge.renderGuiBackground(var2, var3);
			} else {
				catUp.visible = true;
				catDn.visible = true;
				unlock.visible = true;
				categoryList.renderGuiBackground(var2, var3);
				programList.renderGuiBackground(var2, var3);
			}

			search.drawTextBox();

			int selIndex = programList.getSelected();
			if (categoryTextList.getSize() == 0 && programTextList.getSize() != 0) {
				selIndex = programListLarge.getSelected();
			}

			if (selIndex != -1) {
				ListTag list = compiler.getListTagForKey("compilerCategories");
				ResourceLocation sel = getProgramListForSelectionIndex(list).get(selIndex);

				ListTag listPrograms = compiler.getListTagForKey("compilerPrograms");
				if (StreamSupport.stream(listPrograms.spliterator(), false).anyMatch(it -> ResourceLocation.parse(((StringTag) it).getAsString()).equals(sel))) {
					programmerButton.setMessage(net.minecraft.network.chat.Component.literal("Flash"));
					programmerButton.active = !compiler.getInventory().getItem(1).isEmpty();
				} else {
					programmerButton.setMessage(net.minecraft.network.chat.Component.literal("Compile"));
					programmerButton.active = true;
				}
			}
		}
	}

	private List<ResourceLocation> getProgramListForSelectionIndex(ListTag list) {
		return StreamSupport.stream(list.spliterator(), false).flatMap(
				nbtBase -> LogisticsProgramCompilerTileEntity.programByCategory.get(ResourceLocation.parse(((StringTag) nbtBase).getAsString()))
						.stream())
				.filter(it -> TextUtil.translate(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(it).getDescriptionId()).toLowerCase().contains(search.getText().toLowerCase()))
				.sorted(Comparator.<ResourceLocation, Integer>comparing(o -> getSortingClass(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(o)))
						.<String>thenComparing(o -> TextUtil.translate(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(o).getDescriptionId()).toLowerCase())
				)
				.collect(Collectors.toList());
	}

	private int getSortingClass(Item object) {
		if (object instanceof ItemLogisticsPipe) {
			return 0;
		} else if (object instanceof ItemModule) {
			return 1;
		} else if (object instanceof ItemUpgrade) {
			return 2;
		}
		return 10;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (scrollY == 0 || compiler.getCurrentTask() != null) {
			return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
		}
		if (categoryTextList.getSize() == 0 && programTextList.getSize() != 0) {
			if (scrollY < 0) {
				programListLarge.mouseScrollUp();
			} else {
				programListLarge.mouseScrollDown();
			}
		} else {
			if (scrollY < 0) {
				categoryList.mouseScrollUp();
				programList.mouseScrollUp();
			} else {
				categoryList.mouseScrollDown();
				programList.mouseScrollDown();
			}
		}
		return true;
	}

	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (compiler.getCurrentTask() == null && search.keyPressed(keyCode, scanCode, modifiers)) {
			return true;
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}

	@Override
	public boolean charTyped(char typedChar, int keyCode) {
		if (compiler.getCurrentTask() == null) {
			if (!search.handleKey(typedChar, keyCode)) {
				return super.charTyped(typedChar, keyCode);
			}
			return true;
		} else {
			return super.charTyped(typedChar, keyCode);
		}
	}

	@Override
	public boolean mouseClicked(double par1, double par2, int par3) {
		if (compiler.getCurrentTask() == null) {
			search.handleClick(par1, par2, par3);
			if (categoryTextList.getSize() == 0 && programTextList.getSize() != 0) {
				programListLarge.mouseClicked(par1, par2, par3);
			} else {
				categoryList.mouseClicked(par1, par2, par3);
				programList.mouseClicked(par1, par2, par3);
			}
		}
		return super.mouseClicked(par1, par2, par3);
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int par1, int par2) {
		super.renderLabels(guiGraphics, par1, par2);
		if (compiler.getCurrentTask() != null) {
			guiGraphics.drawString(font, TextUtil.translate("gui.compiler.processing"), 10, 39, 0x000000, false);
			Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(compiler.getCurrentTask());
			String name;
			if (item != null) {
				name = item.getDescriptionId();
			} else {
				name = "gui.compiler." + compiler.getCurrentTask().toString().replace(':', '.');
			}
			String text = TextUtil.getTrimmedString(TextUtil.translate(name), 160, font, "...");
			guiGraphics.drawString(font, text, 10, 70, 0x000000, false);
			if (!compiler.isWasAbleToConsumePower()) {
				guiGraphics.drawString(font, TextUtil.translate("gui.compiler.nopower.1"), 68, 10, 0x000000, false);
				guiGraphics.drawString(font, TextUtil.translate("gui.compiler.nopower.2"), 35, 20, 0x000000, false);
			}
		} else {
			if (categoryTextList.getSize() == 0 && programTextList.getSize() != 0) {
				programListLarge.renderGuiForeground();
			} else {
				categoryList.renderGuiForeground();
				programList.renderGuiForeground();
			}
		}
	}
}
