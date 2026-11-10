package logisticspipes.mixin;

import javax.annotation.Nullable;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import logisticspipes.LogisticsEventListener;

@Mixin(Gui.class)
public abstract class GuiMixin {

	@Shadow
	@Nullable
	private Screen screen;

	// Forge ScreenEvent.Opening, then ScreenEvent.Closing for the replaced screen
	@Inject(method = "setScreen", at = @At("HEAD"))
	private void logisticspipes$onSetScreen(@Nullable Screen newScreen, CallbackInfo ci) {
		Screen old = this.screen;
		if (newScreen != null) {
			LogisticsEventListener.onGuiOpen(old, newScreen);
		}
		if (old != null && newScreen != old) {
			LogisticsEventListener.onGuiClose();
		}
	}
}
