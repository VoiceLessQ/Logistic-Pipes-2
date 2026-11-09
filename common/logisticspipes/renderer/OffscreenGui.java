package logisticspipes.renderer;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.state.gui.GuiRenderState;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

/**
 * GuiRenderers that draw into their own target with a fixed GUI size instead of the window
 * (see GuiRendererMixin): the HUD panel atlas and one-shot GUI images.
 */
@Environment(EnvType.CLIENT)
public final class OffscreenGui {

	private record Target(RenderTarget target, int width, int height) {}

	private static final Map<GuiRenderer, Target> TARGETS = new IdentityHashMap<>();

	private OffscreenGui() {}

	/** {@code width} x {@code height} is the GUI size the renderer's ortho projection spans over {@code target}. */
	public static void bind(GuiRenderer renderer, RenderTarget target, int width, int height) {
		TARGETS.put(renderer, new Target(target, width, height));
	}

	public static boolean isOffscreen(GuiRenderer renderer) {
		return TARGETS.containsKey(renderer);
	}

	public static RenderTarget target(GuiRenderer renderer, RenderTarget fallback) {
		Target t = TARGETS.get(renderer);
		return t != null ? t.target() : fallback;
	}

	public static float width(GuiRenderer renderer, float fallback) {
		Target t = TARGETS.get(renderer);
		return t != null ? t.width() : fallback;
	}

	public static float height(GuiRenderer renderer, float fallback) {
		Target t = TARGETS.get(renderer);
		return t != null ? t.height() : fallback;
	}

	/**
	 * Draws {@code draw} onto a {@code width} x {@code height} GUI-unit canvas at {@code pixelScale} pixels per unit
	 * and passes the opaque image to {@code result} once the GPU readback completes; the consumer owns the image.
	 */
	public static void renderToImage(int width, int height, int pixelScale, Consumer<GuiGraphicsExtractor> draw, Consumer<NativeImage> result) {
		Minecraft mc = Minecraft.getInstance();
		GuiRenderState state = new GuiRenderState();
		GuiRenderer renderer = new GuiRenderer(state, mc.gameRenderer.featureRenderDispatcher(), List.of());
		TextureTarget target = new TextureTarget("LP offscreen GUI", width * pixelScale, height * pixelScale, true, GpuFormat.RGBA8_UNORM);
		bind(renderer, target, width, height);
		try {
			RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(target.getColorTexture(), GuiRenderer.CLEAR_COLOR, target.getDepthTexture(), 0.0);
			draw.accept(new GuiGraphicsExtractor(mc, state, -1, -1));
			renderer.render();
			renderer.endFrame();
		} catch (RuntimeException e) {
			release(renderer, target);
			throw e;
		}
		Screenshot.takeScreenshot(target, image -> {
			release(renderer, target);
			result.accept(image);
		});
	}

	private static void release(GuiRenderer renderer, TextureTarget target) {
		TARGETS.remove(renderer);
		renderer.close();
		target.destroyBuffers();
	}
}
