/*
 * Copyright (c) 2015  RS485
 *
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * https://github.com/RS485/LogisticsPipes/blob/mc16/LICENSE.md
 */

package logisticspipes.utils.item;

import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;


import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import lombok.Data;
import lombok.experimental.Accessors;

import logisticspipes.LPItems;
import logisticspipes.utils.Color;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.IItemSearch;
import logisticspipes.utils.gui.SimpleGraphics;
import network.rs485.logisticspipes.util.TextUtil;

@Data
@Accessors(chain = true)
@Environment(EnvType.CLIENT)
public class ItemStackRenderer {

	private TextureManager texManager;
	private Font font;

	@Nonnull
	private ItemStack itemstack = ItemStack.EMPTY;
	private ItemIdentifierStack itemIdentStack;
	private int posX;
	private int posY;
	private float zLevel;
	private float scaleX;
	private float scaleY;
	private float scaleZ;
	private DisplayAmount displayAmount;
	private boolean renderEffects;
	private boolean ignoreDepth;
	private boolean renderInColor;
	private ItemEntity entityitem;
	private Level world;
	private float partialTickTime;

	public ItemStackRenderer(int posX, int posY, float zLevel, boolean renderEffects, boolean ignoreDepth) {
		this.posX = posX;
		this.posY = posY;
		this.zLevel = zLevel;
		this.renderEffects = renderEffects;
		this.ignoreDepth = ignoreDepth;
		font = Minecraft.getInstance().font;
		world = null;
		texManager = Minecraft.getInstance().getTextureManager();
		scaleX = 1.0F;
		scaleY = 1.0F;
		scaleZ = 1.0F;
	}

	public static void renderItemIdentifierStackListIntoGui(List<ItemIdentifierStack> _allItems, IItemSearch IItemSearch, int page, int left, int top, int columns, int items, int xSize, int ySize, float zLevel, DisplayAmount displayAmount) {
		ItemStackRenderer.renderItemIdentifierStackListIntoGui(_allItems, IItemSearch, page, left, top, columns, items, xSize, ySize, zLevel, displayAmount, true, false);
	}

	public static void renderItemIdentifierStackListIntoGui(List<ItemIdentifierStack> _allItems, IItemSearch IItemSearch, int page, int left, int top, int columns, int items, int xSize, int ySize, float zLevel, DisplayAmount displayAmount, boolean renderEffect, boolean ignoreDepth) {
		ItemStackRenderer itemStackRenderer = new ItemStackRenderer(0, 0, zLevel, renderEffect, ignoreDepth);
		itemStackRenderer.setDisplayAmount(displayAmount);
		ItemStackRenderer.renderItemIdentifierStackListIntoGui(_allItems, IItemSearch, page, left, top, columns, items, xSize, ySize, itemStackRenderer);
	}

	public static void renderItemIdentifierStackListIntoGui(List<ItemIdentifierStack> _allItems, IItemSearch IItemSearch, int page, int left, int top, int columns, int items, int xSize, int ySize, ItemStackRenderer itemStackRenderer) {
		int ppi = 0;
		int column = 0;
		int row = 0;

		for (ItemIdentifierStack identifierStack : _allItems) {
			if (identifierStack == null) {
				column++;
				if (column >= columns) {
					row++;
					column = 0;
				}
				ppi++;
				continue;
			}
			ItemIdentifier item = identifierStack.getItem();
			if (IItemSearch != null && !IItemSearch.itemSearched(item)) {
				continue;
			}
			ppi++;

			if (ppi <= items * page) {
				continue;
			}
			if (ppi > items * (page + 1)) {
				continue;
			}
			ItemStack itemstack = identifierStack.unsafeMakeNormalStack();
			int x = left + xSize * column;
			int y = top + ySize * row + 1;

			if (!itemstack.isEmpty()) {
				itemStackRenderer.setItemstack(itemstack).setPosX(x).setPosY(y);
				itemStackRenderer.renderInGui();
			}

			column++;
			if (column >= columns) {
				row++;
				column = 0;
			}
		}
	}

	public void renderInGui() {
		net.minecraft.client.gui.GuiGraphicsExtractor gg = logisticspipes.utils.gui.SimpleGraphics.guiGraphics;
		if (gg == null) return;

		ItemStack stack = itemstack;
		if ((stack == null || stack.isEmpty()) && itemIdentStack != null) {
			stack = itemIdentStack.getItem().unsafeMakeNormalStack(1);
		}
		if (stack == null || stack.isEmpty()) return;

		gg.item(stack, posX, posY);

		String countLabel = null;
		if (displayAmount == DisplayAmount.ALWAYS) {
			long count = itemIdentStack != null ? itemIdentStack.getStackSize() : stack.getCount();
			countLabel = String.valueOf(count);
		} else if (displayAmount == DisplayAmount.HIDE_ONE) {
			long count = itemIdentStack != null ? itemIdentStack.getStackSize() : stack.getCount();
			if (count != 1) {
				countLabel = String.valueOf(count);
			}
		}
		gg.itemDecorations(font, stack, posX, posY, countLabel);
	}

	private void setupGuiTransform(int xPosition, int yPosition, boolean isGui3d) {
		// no-op: replaced by GuiGraphicsExtractor.renderItem in renderInGui()
	}

	public void renderInWorld() {
		// Legacy no-arg entry point — call sites that still use this path have no PoseStack
		// context and only run under the CCL-activated branch (currently dormant).
	}

	public void renderInWorld(com.mojang.blaze3d.vertex.PoseStack poseStack, net.minecraft.client.renderer.SubmitNodeCollector collector, int packedLight, int packedOverlay) {
		if (itemstack == null || itemstack.isEmpty()) return;
		Minecraft mc = Minecraft.getInstance();
		net.minecraft.client.renderer.item.ItemStackRenderState state = new net.minecraft.client.renderer.item.ItemStackRenderState();
		mc.getItemModelResolver().updateForTopItem(state, itemstack, net.minecraft.world.item.ItemDisplayContext.GROUND, mc.level, null, 0);
		state.submit(poseStack, collector, packedLight, packedOverlay, 0);
	}

	public void renderItemInGui(float x, float y, Item item, float zLevel, float scale) {
		net.minecraft.client.gui.GuiGraphicsExtractor gg = logisticspipes.utils.gui.SimpleGraphics.guiGraphics;
		if (gg == null || item == null) return;
		ItemStack stack = new ItemStack(item);
		if (stack.isEmpty()) return;
		org.joml.Matrix3x2fStack pose = gg.pose();
		pose.pushMatrix();
		pose.translate(x, y);
		pose.scale(scale, scale);
		gg.item(stack, 0, 0);
		pose.popMatrix();
	}

	public enum DisplayAmount {
		HIDE_ONE,
		ALWAYS,
		NEVER,
	}

}
