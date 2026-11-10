package logisticspipes.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.Projection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import logisticspipes.renderer.HUDPanelAtlas;
import logisticspipes.renderer.OffscreenGui;

/** Lets LP's offscreen GuiRenderers (HUD panel atlas, GUI images) draw into their own target instead of the main one. */
@Mixin(GuiRenderer.class)
public abstract class GuiRendererMixin {

	@Inject(method = "render", at = @At("HEAD"))
	private void logisticspipes$renderHudPanels(CallbackInfo ci) {
		if (!OffscreenGui.isOffscreen((GuiRenderer) (Object) this)) {
			HUDPanelAtlas.instance().renderQueued();
		}
	}

	@Redirect(method = "draw", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;mainRenderTarget()Lcom/mojang/blaze3d/pipeline/RenderTarget;"))
	private RenderTarget logisticspipes$offscreenTarget(GameRenderer gameRenderer) {
		return OffscreenGui.target((GuiRenderer) (Object) this, gameRenderer.mainRenderTarget());
	}

	@Redirect(method = "draw", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/Projection;setupOrtho(FFFFZ)V"))
	private void logisticspipes$offscreenOrtho(Projection projection, float zNear, float zFar, float width, float height, boolean invertY) {
		GuiRenderer self = (GuiRenderer) (Object) this;
		projection.setupOrtho(zNear, zFar, OffscreenGui.width(self, width), OffscreenGui.height(self, height), invertY);
	}
}
