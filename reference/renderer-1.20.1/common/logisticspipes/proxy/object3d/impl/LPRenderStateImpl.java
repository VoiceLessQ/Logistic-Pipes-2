package logisticspipes.proxy.object3d.impl;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import logisticspipes.proxy.object3d.interfaces.IRenderState;

/**
 * NeoForge 1.20.1 replacement for CCL's {@code CCRenderState}.
 *
 * <p>Holds the current {@link VertexConsumer} and lighting/colour state that
 * {@link LPModel3DImpl#render} reads when emitting quads. Lifecycle is:</p>
 * <ol>
 *   <li>{@link #bind} called by {@code LogisticsRenderPipe.render} each frame
 *       with the buffer obtained from {@code MultiBufferSource}.</li>
 *   <li>Pipe code calls {@code model.render(ops...)}; ops may include a
 *       {@link LPTransformOp#COLOUR_MULTIPLIER} that this class latches.</li>
 *   <li>{@link #draw} is the end-of-frame no-op hook (buffer is flushed by
 *       {@code MultiBufferSource}).</li>
 * </ol>
 */
public final class LPRenderStateImpl implements IRenderState {

	/** Current buffer; reset per-frame in {@link #bind}. */
	public VertexConsumer buffer;

	/** Current PoseStack pose + normal matrices; vertex positions must be multiplied
	 *  by these in {@link LPModel3DImpl#render} so emitted geometry lands in world space. */
	public Matrix4f pose;
	public Matrix3f normal;

	/** Light+overlay from the outer BER call, passed to each vertex. */
	public int packedLight = 0x00f000f0; // full-bright default
	public int packedOverlay = net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;

	/** Optional uniform colour multiplier (0xAARRGGBB), 0xffffffff = white. */
	public int colourARGB = 0xffffffff;

	/** Alpha override from {@link #setAlphaOverride}, -1 = disabled. */
	private int alphaOverride = -1;

	/**
	 * Attach a buffer + lighting state for the current rendering call.
	 * Must be called before any {@code model.render(ops...)}.
	 */
	public void bind(VertexConsumer buffer, int packedLight, int packedOverlay) {
		this.buffer = buffer;
		this.packedLight = packedLight;
		this.packedOverlay = packedOverlay;
		this.colourARGB = 0xffffffff;
	}

	/** Full bind including the current PoseStack matrices. Preferred over the
	 *  3-arg overload when called from within a BlockEntityRenderer. */
	public void bind(VertexConsumer buffer, Matrix4f pose, Matrix3f normal, int packedLight, int packedOverlay) {
		this.buffer = buffer;
		this.pose = pose;
		this.normal = normal;
		this.packedLight = packedLight;
		this.packedOverlay = packedOverlay;
		this.colourARGB = 0xffffffff;
	}

	@Override
	public void reset() {
		this.colourARGB = 0xffffffff;
		this.alphaOverride = -1;
	}

	@Override
	public void setAlphaOverride(int i) {
		this.alphaOverride = i;
	}

	@Override
	public void draw() {
		// No-op: MultiBufferSource.BufferSource batches and flushes on endBatch().
	}

	@Override
	public void setBrightness(BlockGetter world, BlockPos pos) {
		if (world instanceof net.minecraft.world.level.BlockAndTintGetter && pos != null) {
			this.packedLight = net.minecraft.client.renderer.LevelRenderer.getLightColor(
				(net.minecraft.world.level.BlockAndTintGetter) world, pos);
		}
		// Otherwise leave packedLight as supplied by the BER caller.
	}

	@Override
	public void startDrawing(int mode, VertexFormat format) {
		// No-op: 1.20.1 uses fixed RenderType pipelines selected by the BER caller.
	}

	public int effectiveColourARGB() {
		if (alphaOverride >= 0) {
			return (alphaOverride << 24) | (colourARGB & 0x00ffffff);
		}
		return colourARGB;
	}
}
