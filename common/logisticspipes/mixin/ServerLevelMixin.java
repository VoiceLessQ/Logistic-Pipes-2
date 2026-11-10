package logisticspipes.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.redstone.Orientation;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import logisticspipes.LogisticsEventListener;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {

	// Forge BlockEvent.NeighborNotifyEvent
	@Inject(method = "updateNeighborsAt(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/Block;Lnet/minecraft/world/level/redstone/Orientation;)V", at = @At("HEAD"))
	private void logisticspipes$onNeighborNotify(BlockPos pos, Block block, Orientation orientation, CallbackInfo ci) {
		LogisticsEventListener.onNeighborNotify((ServerLevel) (Object) this, pos, null);
	}

	@Inject(method = "updateNeighborsAtExceptFromFacing", at = @At("HEAD"))
	private void logisticspipes$onNeighborNotifyExcept(BlockPos pos, Block block, Direction skipDirection, Orientation orientation, CallbackInfo ci) {
		LogisticsEventListener.onNeighborNotify((ServerLevel) (Object) this, pos, skipDirection);
	}
}
