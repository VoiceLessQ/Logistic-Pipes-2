package logisticspipes.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import logisticspipes.pipefxhandlers.PipeBreakParticles;

/** Forge IClientBlockExtensions.addHitEffects/addDestroyEffects for LP pipes. */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {

	@Inject(method = "addBreakingBlockEffect", at = @At("HEAD"), cancellable = true)
	private void logisticspipes$pipeHitEffects(BlockPos pos, Direction direction, CallbackInfo ci) {
		if (PipeBreakParticles.addHitEffects((ClientLevel) (Object) this, pos, direction)) {
			ci.cancel();
		}
	}

	@Inject(method = "addDestroyBlockEffect", at = @At("HEAD"), cancellable = true)
	private void logisticspipes$pipeDestroyEffects(BlockPos pos, BlockState state, CallbackInfo ci) {
		if (PipeBreakParticles.addDestroyEffects((ClientLevel) (Object) this, pos, state)) {
			ci.cancel();
		}
	}
}
