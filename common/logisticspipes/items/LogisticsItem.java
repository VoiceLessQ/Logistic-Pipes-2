/**
 * Copyright (c) Krapht, 2011
 * <p>
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.items;

import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.client.gui.screens.Screen;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;


import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.level.Level;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;



import logisticspipes.LogisticsPipes;
import logisticspipes.interfaces.ILogisticsItem;
import network.rs485.logisticspipes.util.TextUtil;

public class LogisticsItem extends Item implements ILogisticsItem {

	public LogisticsItem() {
		super(new Item.Properties()); // creative tab registration handled via event in 1.20.1
	}

	protected LogisticsItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public String getModelPath() {
		String modelFile = BuiltInRegistries.ITEM.getKey(this).getPath();
		String dir = getModelSubdir();
		if (!dir.isEmpty()) {
			if (modelFile.startsWith(String.format("%s_", dir))) {
				modelFile = modelFile.substring(dir.length() + 1);
			}
			return String.format("%s/%s", dir, modelFile).replaceAll("/+", "/");
		}
		return modelFile;
	}

	public String getModelSubdir() {
		return "";
	}

	public int getModelCount() {
		return 1;
	}

	@Nonnull
	@Override
	public String getDescriptionId(@Nonnull ItemStack stack) {
		// getHasSubtypes() removed in 1.20.1 (damage-based subtypes no longer exist)
		return super.getDescriptionId(stack);
	}

	/**
	 * Adds all keys from the translation file in the format:
	 * item.className.tip([0-9]*) Tips start from 1 and increment. Sparse rows
	 * should be left empty (ie empty line must still have a key present) Shift
	 * shows full tooltip, without it, you just get the first line.
	 */
	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(@Nonnull ItemStack stack, @Nonnull Item.TooltipContext context, java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flagIn) {
		super.appendHoverText(stack, context, tooltip, flagIn);
		if (addShiftInfo()) {
			TextUtil.addTooltipInformation(stack, tooltip, Screen.hasShiftDown());
		}
	}

	public boolean addShiftInfo() {
		return true;
	}

	@Nonnull
	public String getHoverName(@Nonnull ItemStack itemstack) {
		// getHoverName(ItemStack) removed from Item in 1.20.1; kept as custom method for internal use
		return I18n.get(getDescriptionId(itemstack) + ".name").trim();
	}
}
