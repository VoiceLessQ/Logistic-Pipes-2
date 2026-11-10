package logisticspipes.proxy.enderchest;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import codechicken.enderstorage.block.BlockEnderStorage;
import codechicken.enderstorage.tile.TileFrequencyOwner;

import logisticspipes.proxy.interfaces.IEnderStorageProxy;

public class EnderStorageProxy implements IEnderStorageProxy {

	@Override
	public boolean isEnderChestBlock(Block block) {
		return block instanceof BlockEnderStorage;
	}

	// the block's use() raytraces from the player's view, so open through the tile like LP1
	@Override
	public void openEnderChest(Level level, BlockPos pos, Player player) {
		if (level.getBlockEntity(pos) instanceof TileFrequencyOwner tile) {
			tile.activate(player, 0, InteractionHand.MAIN_HAND);
		}
	}
}
