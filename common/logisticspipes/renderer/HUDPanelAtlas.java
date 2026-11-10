package logisticspipes.renderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import logisticspipes.LPConstants;
import logisticspipes.utils.gui.SimpleGraphics;

/**
 * World-space HUD panels. 26.x has no GUI drawing with a 3D pose, so each panel is drawn by a private
 * {@link GuiRenderer} into one cell of an offscreen atlas (just before the vanilla GUI pass, see
 * GuiRendererMixin) and shown in the world as a textured quad. The quad submitted in frame N shows the
 * cell drawn at the end of frame N-1; cells stay bound to the same panel while it is visible.
 */
@Environment(EnvType.CLIENT)
public final class HUDPanelAtlas {

	/** GUI units per cell; panels are drawn around the cell centre and span at most ±64. */
	public static final int CELL = 128;
	private static final int COLUMNS = 4;
	public static final int SIZE = CELL * COLUMNS;
	private static final int PIXELS_PER_UNIT = 4;
	private static final Identifier TEXTURE_ID = Identifier.fromNamespaceAndPath(LPConstants.LP_MOD_ID, "hud_panels");

	// The atlas holds premultiplied colour (GUI blending onto a transparent target).
	private static final RenderPipeline PANEL_PIPELINE = RenderPipeline.builder(RenderPipelines.WORLD_TEXT_SNIPPET)
			.withLocation(Identifier.fromNamespaceAndPath(LPConstants.LP_MOD_ID, "pipeline/hud_panel"))
			.withVertexShader("core/text")
			.withFragmentShader("core/text")
			.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA))
			.withCull(false)
			.build();
	private static final RenderPipeline PANEL_SEE_THROUGH_PIPELINE = RenderPipeline.builder(RenderPipelines.TEXT_SNIPPET)
			.withLocation(Identifier.fromNamespaceAndPath(LPConstants.LP_MOD_ID, "pipeline/hud_panel_see_through"))
			.withVertexShader("core/text")
			.withFragmentShader("core/text")
			.withShaderDefine("IS_SEE_THROUGH")
			.withColorTargetState(new ColorTargetState(BlendFunction.TRANSLUCENT_PREMULTIPLIED_ALPHA))
			// ignores depth but writes it, so panels drawn later behind it stay hidden
			.withDepthStencilState(new DepthStencilState(CompareOp.ALWAYS_PASS, true))
			.withCull(false)
			.build();
	private static final RenderType PANEL = RenderType.create("logisticspipes_hud_panel",
			RenderSetup.builder(PANEL_PIPELINE).withTexture("Sampler0", TEXTURE_ID).useLightmap().sortOnUpload().createRenderSetup());
	private static final RenderType PANEL_SEE_THROUGH = RenderType.create("logisticspipes_hud_panel_see_through",
			RenderSetup.builder(PANEL_SEE_THROUGH_PIPELINE).withTexture("Sampler0", TEXTURE_ID).createRenderSetup());

	private static final HUDPanelAtlas INSTANCE = new HUDPanelAtlas();

	private final GuiRenderState renderState = new GuiRenderState();
	private GuiRenderer renderer;
	private TextureTarget target;
	private final AtlasTexture texture = new AtlasTexture();
	private final Map<Object, Integer> cells = new HashMap<>();
	private final List<Pending> pending = new ArrayList<>();

	private record Pending(Object key, int cell, float scale, Consumer<GuiGraphicsExtractor> draw) {}

	public static HUDPanelAtlas instance() {
		return INSTANCE;
	}

	/**
	 * Queues {@code draw} (GUI coordinates, origin at the panel centre, drawing into
	 * {@link SimpleGraphics#guiGraphics}) and submits its quad at the origin of {@code pose}, where one unit
	 * is one GUI pixel and +y points down. {@code scale} shrinks the content to fit a cell; the quad grows by
	 * the inverse so the panel keeps its size.
	 */
	public void submit(Object key, float scale, boolean seeThrough, PoseStack pose, SubmitNodeCollector collector, Consumer<GuiGraphicsExtractor> draw) {
		Integer cell = cells.get(key);
		if (cell == null) {
			cell = freeCell();
			if (cell < 0) {
				return;
			}
			cells.put(key, cell);
		}
		pending.add(new Pending(key, cell, scale, draw));
		float half = CELL / 2.0F / scale;
		float u0 = (float) (cell % COLUMNS) / COLUMNS;
		float v0 = 1.0F - (float) (cell / COLUMNS) / COLUMNS;
		float u1 = u0 + 1.0F / COLUMNS;
		float v1 = v0 - 1.0F / COLUMNS;
		collector.submitCustomGeometry(pose, seeThrough ? PANEL_SEE_THROUGH : PANEL, (mat, buffer) -> {
			buffer.addVertex(mat, -half, -half, 0).setUv(u0, v0).setLight(0xF000F0).setColor(-1);
			buffer.addVertex(mat, -half, half, 0).setUv(u0, v1).setLight(0xF000F0).setColor(-1);
			buffer.addVertex(mat, half, half, 0).setUv(u1, v1).setLight(0xF000F0).setColor(-1);
			buffer.addVertex(mat, half, -half, 0).setUv(u1, v0).setLight(0xF000F0).setColor(-1);
		});
	}

	private int freeCell() {
		for (int i = 0; i < COLUMNS * COLUMNS; i++) {
			if (!cells.containsValue(i)) {
				return i;
			}
		}
		return -1;
	}

	/** Draws the panels queued during this frame's level render into the atlas; runs right before the vanilla GUI pass. */
	public void renderQueued() {
		if (pending.isEmpty()) {
			cells.clear();
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		ensureResources(mc);
		RenderSystem.getDevice().createCommandEncoder().clearColorAndDepthTextures(target.getColorTexture(), GuiRenderer.CLEAR_COLOR, target.getDepthTexture(), 0.0);
		GuiGraphicsExtractor gg = new GuiGraphicsExtractor(mc, renderState, -1, -1);
		GuiGraphicsExtractor previous = SimpleGraphics.guiGraphics;
		SimpleGraphics.guiGraphics = gg;
		Map<Object, Integer> used = new HashMap<>();
		try {
			for (Pending panel : pending) {
				used.put(panel.key(), panel.cell());
				gg.pose().pushMatrix();
				gg.pose().translate((panel.cell() % COLUMNS) * CELL + CELL / 2.0F, (panel.cell() / COLUMNS) * CELL + CELL / 2.0F);
				gg.pose().scale(panel.scale(), panel.scale());
				try {
					panel.draw().accept(gg);
				} finally {
					gg.pose().popMatrix();
					SimpleGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
				}
			}
		} finally {
			SimpleGraphics.guiGraphics = previous;
			pending.clear();
			cells.keySet().retainAll(used.keySet());
		}
		renderer.render();
		renderer.endFrame();
	}

	private void ensureResources(Minecraft mc) {
		if (renderer == null) {
			renderer = new GuiRenderer(renderState, mc.gameRenderer.featureRenderDispatcher(), List.of());
			target = new TextureTarget("LP HUD panels", SIZE * PIXELS_PER_UNIT, SIZE * PIXELS_PER_UNIT, true, GpuFormat.RGBA8_UNORM);
			OffscreenGui.bind(renderer, target, SIZE, SIZE);
		}
		if (mc.getTextureManager().getTexture(TEXTURE_ID) != texture) {
			mc.getTextureManager().register(TEXTURE_ID, texture);
		}
		texture.bind(target);
	}

	/** Exposes the atlas colour target to RenderTypes by id; the target owns the GPU texture. */
	private static final class AtlasTexture extends AbstractTexture {

		AtlasTexture() {
			sampler = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
		}

		void bind(RenderTarget target) {
			texture = target.getColorTexture();
			textureView = target.getColorTextureView();
		}

		@Override
		public void close() {
			texture = null;
			textureView = null;
		}
	}
}
