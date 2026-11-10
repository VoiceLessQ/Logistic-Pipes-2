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
import net.minecraft.world.level.Level;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;



import logisticspipes.LogisticsPipes;
import logisticspipes.interfaces.ILogisticsItem;
import network.rs485.logisticspipes.util.TextUtil;

public class LogisticsItem extends Item implements ILogisticsItem {

	public LogisticsItem() {
		super(logisticspipes.LPRegistries.itemProperties());
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

	/**
	 * Adds all keys from the translation file in the format:
	 * item.className.tip([0-9]*) Tips start from 1 and increment. Sparse rows
	 * should be left empty (ie empty line must still have a key present) Shift
	 * shows full tooltip, without it, you just get the first line.
	 */
	@Override
	@Environment(EnvType.CLIENT)
	public void appendHoverText(@Nonnull ItemStack stack, Item.TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<net.minecraft.network.chat.Component> tooltipConsumer, net.minecraft.world.item.TooltipFlag flagIn) {
		java.util.List<net.minecraft.network.chat.Component> tooltip = new java.util.ArrayList<>();
		super.appendHoverText(stack, context, display, tooltipConsumer, flagIn);
		if (addShiftInfo()) {
			TextUtil.addTooltipInformation(stack, tooltip, net.minecraft.client.Minecraft.getInstance().hasShiftDown());
		}
		tooltip.forEach(tooltipConsumer);
	}

	public boolean addShiftInfo() {
		return true;
	}
}
