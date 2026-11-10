package logisticspipes.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import logisticspipes.LogisticsEventListener;

@Mixin(ResultSlot.class)
public abstract class ResultSlotMixin {

	@Shadow
	@Final
	private Player player;

	// Forge PlayerEvent.ItemCraftedEvent
	@Inject(method = "checkTakeAchievements", at = @At("HEAD"))
	private void logisticspipes$onItemCrafted(ItemStack carried, CallbackInfo ci) {
		LogisticsEventListener.onItemCrafting(player, carried);
	}
}
