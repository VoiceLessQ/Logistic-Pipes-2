/*
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.utils;

import java.util.ArrayList;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.neoforge.capabilities.Capabilities;

import logisticspipes.LogisticsPipes;
import logisticspipes.interfaces.IInventoryUtil;
import logisticspipes.proxy.specialinventoryhandler.SpecialInventoryHandler;
import network.rs485.logisticspipes.connection.NeighborTileEntity;
import network.rs485.logisticspipes.inventory.ProviderMode;

public class InventoryUtilFactory {

	private final ArrayList<SpecialInventoryHandler.Factory> handlerFactories = new ArrayList<>();

	public void registerHandler(@Nonnull SpecialInventoryHandler.Factory handlerFactory) {
		if (handlerFactory.init()) {
			handlerFactories.add(handlerFactory);
			LogisticsPipes.log.info("Loaded SpecialInventoryHandler.Factory: " + handlerFactory.getClass().getCanonicalName());
		} else {
			LogisticsPipes.log.warn("Could not load SpecialInventoryHandler.Factory: " + handlerFactory.getClass().getCanonicalName());
		}
	}

	@Nullable
	public SpecialInventoryHandler getSpecialHandlerFor(BlockEntity tile, Direction direction, ProviderMode mode) {
		return handlerFactories.stream()
				.filter(factory -> factory.isType(tile, direction))
				.map(factory -> factory.getUtilForTile(tile, direction, mode))
				.filter(Objects::nonNull)
				.findAny()
				.orElse(null);
	}

	@Nullable
	public IInventoryUtil getInventoryUtil(@Nonnull NeighborTileEntity<BlockEntity> adj) {
		return getHidingInventoryUtil(adj.getTileEntity(), adj.getOurDirection(), ProviderMode.DEFAULT);
	}

	@Nullable
	public IInventoryUtil getInventoryUtil(BlockEntity inv, Direction dir) {
		return getHidingInventoryUtil(inv, dir, ProviderMode.DEFAULT);
	}

	@Nullable
	public IInventoryUtil getHidingInventoryUtil(@Nullable BlockEntity tile, @Nullable Direction direction, @Nonnull ProviderMode mode) {
		if (tile != null) {
			IInventoryUtil util = getSpecialHandlerFor(tile, direction, mode);
			if (util != null) {
				return util;
			}
			net.neoforged.neoforge.items.IItemHandler handler = tile.getLevel() == null ? null
					: tile.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, tile.getBlockPos(), direction);
			if (handler != null) {
				return new InventoryUtil(handler, mode);
			}
		}
		return null;
	}
}
