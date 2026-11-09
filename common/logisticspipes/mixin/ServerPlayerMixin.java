package logisticspipes.mixin;

import net.minecraft.server.level.ServerPlayer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import network.rs485.logisticspipes.property.PropertyUpdaterEventListener;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {

	// Forge PlayerContainerEvent.Close
	@Inject(method = "doCloseContainer", at = @At("HEAD"))
	private void logisticspipes$onCloseContainer(CallbackInfo ci) {
		ServerPlayer player = (ServerPlayer) (Object) this;
		if (player.containerMenu != player.inventoryMenu) {
			PropertyUpdaterEventListener.closeContainer(player);
		}
	}
}
