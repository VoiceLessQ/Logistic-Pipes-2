package logisticspipes.mixin;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import logisticspipes.routing.pathfinder.changedetection.BlockChangeListener;

@Mixin(BlockItem.class)
public abstract class BlockItemMixin {

	// Forge BlockEvent.EntityPlaceEvent
	@Inject(method = "place", at = @At("RETURN"))
	private void logisticspipes$onPlaced(BlockPlaceContext context, CallbackInfoReturnable<InteractionResult> cir) {
		if (cir.getReturnValue().consumesAction()) {
			BlockChangeListener.onBlockPlaced(context.getLevel(), context.getClickedPos());
		}
	}
}
