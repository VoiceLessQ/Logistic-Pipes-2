package logisticspipes.proxy.ccl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import logisticspipes.proxy.interfaces.ICCLProxy;
import logisticspipes.proxy.object3d.impl.LPBoundsImpl;
import logisticspipes.proxy.object3d.impl.LPModel3DImpl;
import logisticspipes.proxy.object3d.impl.LPQuadData;
import logisticspipes.proxy.object3d.impl.LPRenderStateImpl;
import logisticspipes.proxy.object3d.impl.LPTextureTransformationImpl;
import logisticspipes.proxy.object3d.impl.LPTransformOp;
import logisticspipes.proxy.object3d.impl.LPTranslationImpl;
import logisticspipes.proxy.object3d.impl.LPUvOp;
import logisticspipes.proxy.object3d.impl.OBJParser;
import logisticspipes.proxy.object3d.interfaces.I3DOperation;
import logisticspipes.proxy.object3d.interfaces.IModel3D;
import logisticspipes.proxy.object3d.interfaces.IRenderState;
import logisticspipes.proxy.object3d.interfaces.ITranslation;
import logisticspipes.proxy.object3d.interfaces.IVec3;
import logisticspipes.proxy.object3d.interfaces.TextureTransformation;
import logisticspipes.proxy.object3d.operation.LPScale;

/**
 * NeoForge 1.20.1 native replacement for the old CodeChickenLib-backed proxy.
 *
 * <p>All factory methods now return concrete implementations from
 * {@code logisticspipes.proxy.object3d.impl}. The CCL dependency is fully
 * removed — the interfaces ({@code IModel3D}, {@code IRenderState}, etc.) are
 * now served by a minimal internal mesh/transform pipeline that emits quads
 * directly through a {@code VertexConsumer}.</p>
 *
 * <p>{@link #isActivated()} returns {@code true}: the native pipeline is live
 * and {@code LogisticsNewRenderPipe.loadModels()} runs at mod init (wrapped in
 * {@code LogisticsPipes.safeLoadModels}).</p>
 */
public class CCLProxy implements ICCLProxy {

	// Lazily created: LPRenderStateImpl references the client-only OverlayTexture, so eager
	// (static) init crashes a dedicated server when ProxyManager.load() does `new CCLProxy()`.
	// getRenderState() is only ever called from client renderers/FX, so first-touch init keeps
	// the single-shared-instance semantics while never loading client classes on the server.
	private static LPRenderStateImpl RENDER_STATE;

	@Override
	public TextureTransformation createIconTransformer(TextureAtlasSprite sprite) {
		return new LPTextureTransformationImpl(sprite);
	}

	@Override
	public IRenderState getRenderState() {
		LPRenderStateImpl rs = RENDER_STATE;
		if (rs == null) {
			rs = new LPRenderStateImpl();
			RENDER_STATE = rs;
		}
		return rs;
	}

	@Override
	public Map<String, IModel3D> parseObjModels(InputStream stream, int ignoredCCLFormatFlag, LPScale scale) throws IOException {
		Map<String, List<LPQuadData>> parsed = OBJParser.parse(stream);
		Map<String, IModel3D> out = new LinkedHashMap<>(parsed.size());
		LPTransformOp scaleOp = scale == null ? null : (LPTransformOp) scale.getOriginal();
		for (Map.Entry<String, List<LPQuadData>> entry : parsed.entrySet()) {
			IModel3D model = new LPModel3DImpl(entry.getValue());
			if (scaleOp != null) {
				model = model.apply(scale);
			}
			out.put(entry.getKey(), model);
		}
		return out;
	}

	@Override
	public Object getRotation(int side, int meta) {
		return LPTransformOp.sideRotation(side, meta);
	}

	@Override
	public Object getScale(double d, double e, double f) {
		return LPTransformOp.scale(d, e, f);
	}

	@Override
	public Object getScale(double d) {
		return LPTransformOp.uniformScale(d);
	}

	@Override
	public ITranslation getTranslation(double d, double e, double f) {
		return new LPTranslationImpl(d, e, f);
	}

	@Override
	public ITranslation getTranslation(IVec3 min) {
		return new LPTranslationImpl(min.x(), min.y(), min.z());
	}

	@Override
	public Object getUVScale(double i, double d) {
		return LPUvOp.scale((float) i, (float) d);
	}

	@Override
	public Object getUVTranslation(float i, float f) {
		return LPUvOp.translate(i, f);
	}

	@Override
	public Object getUVTransformationList(I3DOperation[] uvTranslation) {
		List<LPUvOp> children = new ArrayList<>(uvTranslation.length);
		for (I3DOperation op : uvTranslation) {
			if (op == null) continue;
			Object o = op.getOriginal();
			if (o instanceof LPUvOp) children.add((LPUvOp) o);
		}
		return LPUvOp.list(children.toArray(new LPUvOp[0]));
	}

	@Override
	public IModel3D wrapModel(Object model) {
		if (model instanceof IModel3D) return (IModel3D) model;
		if (model instanceof List) {
			@SuppressWarnings("unchecked")
			List<LPQuadData> list = (List<LPQuadData>) model;
			return new LPModel3DImpl(list);
		}
		return new LPModel3DImpl(new ArrayList<>());
	}

	@Override
	public boolean isActivated() {
		// Activated: CCL has been replaced by the native pipeline in
		// logisticspipes.proxy.object3d.impl. LogisticsNewRenderPipe.loadModels()
		// and related loaders are now called at mod init, each wrapped in a
		// try/catch (see LogisticsPipes.safeLoadModels) so a single group-name
		// mismatch doesn't halt startup.
		return true;
	}

	@Override
	public Object getRotation(double angleRadians, int axisX, int axisY, int axisZ) {
		return LPTransformOp.rotation(angleRadians, axisX, axisY, axisZ);
	}

	@Override
	public IModel3D combine(Collection<IModel3D> list) {
		List<LPQuadData> merged = new ArrayList<>();
		for (IModel3D m : list) {
			if (m instanceof LPModel3DImpl) {
				merged.addAll(((LPModel3DImpl) m).getQuads());
			}
		}
		return new LPModel3DImpl(merged);
	}

	@Override
	public Object getColourMultiplier(int argb) {
		return LPTransformOp.colour(argb);
	}

	@Override
	public Object getDefaultBlockState() {
		// IModelState removed in 1.20.1; downstream code uses BakedModel, not IModelState
		return null;
	}

	/**
	 * Helper for tests / debugging to materialize a bounds without needing the
	 * rest of the render chain. Not part of {@link ICCLProxy}.
	 */
	public LPBoundsImpl unitBounds() {
		return new LPBoundsImpl(0, 0, 0, 1, 1, 1);
	}
}
