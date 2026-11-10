package logisticspipes.mixin;

import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import logisticspipes.LogisticsEventListener;

@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin {

	// Forge ChunkWatchEvent.Watch
	@Inject(method = "markChunkPendingToSend(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/chunk/LevelChunk;)V", at = @At("HEAD"))
	private static void logisticspipes$watchChunk(ServerPlayer player, LevelChunk chunk, CallbackInfo ci) {
		LogisticsEventListener.watchChunk(player, chunk.getPos());
	}

	// Forge ChunkWatchEvent.UnWatch
	@Inject(method = "dropChunk", at = @At("HEAD"))
	private static void logisticspipes$unWatchChunk(ServerPlayer player, ChunkPos pos, CallbackInfo ci) {
		LogisticsEventListener.unWatchChunk(player, pos);
	}
}
