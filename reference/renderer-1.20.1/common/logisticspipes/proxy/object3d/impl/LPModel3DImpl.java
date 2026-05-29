package logisticspipes.proxy.object3d.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.world.phys.AABB;

import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.object3d.interfaces.I3DOperation;
import logisticspipes.proxy.object3d.interfaces.IBounds;
import logisticspipes.proxy.object3d.interfaces.IModel3D;
import logisticspipes.proxy.object3d.interfaces.ITranslation;
import logisticspipes.proxy.object3d.interfaces.TextureTransformation;
import logisticspipes.proxy.object3d.operation.LPColourMultiplier;
import logisticspipes.proxy.object3d.operation.LPRotation;
import logisticspipes.proxy.object3d.operation.LPScale;
import logisticspipes.proxy.object3d.operation.LPTranslation;
import logisticspipes.proxy.object3d.operation.LPUVScale;
import logisticspipes.proxy.object3d.operation.LPUVTransformationList;
import logisticspipes.proxy.object3d.operation.LPUVTranslation;

/**
 * NeoForge 1.20.1 native {@link IModel3D} backed by a list of {@link LPQuadData}
 * quads parsed from a Wavefront OBJ file.
 *
 * <p>The class is effectively immutable for "apply-time" operations — each
 * {@link #apply(I3DOperation)} produces a new instance with transformed vertex
 * data. "Render-time" operations passed to {@link #render(I3DOperation...)}
 * (texture transformations, colour tinting) are applied on the fly against
 * the currently-bound {@link LPRenderStateImpl}.</p>
 */
public final class LPModel3DImpl implements IModel3D {

	private final List<LPQuadData> quads;
	private LPBoundsImpl cachedBounds;

	public LPModel3DImpl(List<LPQuadData> quads) {
		this.quads = quads;
	}

	public List<LPQuadData> getQuads() {
		return quads;
	}

	// ─── Apply-time transforms: translation, scale, rotation ────────────────

	@Override
	public IModel3D apply(I3DOperation op) {
		List<LPQuadData> out = new ArrayList<>(quads.size());
		for (LPQuadData q : quads) out.add(transformPositional(q.copy(), op));
		return new LPModel3DImpl(out);
	}

	private static LPQuadData transformPositional(LPQuadData q, I3DOperation op) {
		Object original = op.getOriginal();
		if (original instanceof LPTranslationImpl) {
			LPTranslationImpl t = (LPTranslationImpl) original;
			for (int i = 0; i < 4; i++) {
				q.x[i] += t.dx;
				q.y[i] += t.dy;
				q.z[i] += t.dz;
			}
		} else if (original instanceof LPUvOp) {
			// UV-space transform applied at geometry-bake time (e.g. LPUVScale via apply()).
			LPUvOp uvOp = (LPUvOp) original;
			float[] uv = new float[2];
			for (int i = 0; i < 4; i++) {
				uv[0] = q.u[i];
				uv[1] = q.v[i];
				uvOp.apply(uv);
				q.u[i] = uv[0];
				q.v[i] = uv[1];
			}
		} else if (original instanceof LPTransformOp) {
			LPTransformOp t = (LPTransformOp) original;
			switch (t.type) {
				case SCALE_UNIFORM:
				case SCALE_XYZ:
					for (int i = 0; i < 4; i++) {
						q.x[i] *= t.sx;
						q.y[i] *= t.sy;
						q.z[i] *= t.sz;
					}
					break;
				case ROTATION_AXIS_ANGLE: {
					double cos = Math.cos(t.angle);
					double sin = Math.sin(t.angle);
					double ax = t.ax, ay = t.ay, az = t.az;
					double mag = Math.sqrt(ax * ax + ay * ay + az * az);
					if (mag > 1e-9) { ax /= mag; ay /= mag; az /= mag; }
					for (int i = 0; i < 4; i++) {
						double px = q.x[i] - t.ox;
						double py = q.y[i] - t.oy;
						double pz = q.z[i] - t.oz;
						// Rodrigues' rotation formula
						double dot = ax * px + ay * py + az * pz;
						double rx = px * cos + (ay * pz - az * py) * sin + ax * dot * (1 - cos);
						double ry = py * cos + (az * px - ax * pz) * sin + ay * dot * (1 - cos);
						double rz = pz * cos + (ax * py - ay * px) * sin + az * dot * (1 - cos);
						q.x[i] = (float) (rx + t.ox);
						q.y[i] = (float) (ry + t.oy);
						q.z[i] = (float) (rz + t.oz);
					}
					break;
				}
				case ROTATION_SIDE: {
					// sideOrientation(0, meta): Y-axis rotation by (meta * 90°) CCW around origin.
					// The caller (computeRotated) follows each rotation with a LPTranslation to
					// bring the result back into the [0,1]^3 unit cube.
					double angleRad = t.meta * Math.PI / 2.0;
					double cos = Math.cos(angleRad);
					double sin = Math.sin(angleRad);
					for (int i = 0; i < 4; i++) {
						double x = q.x[i];
						double z = q.z[i];
						q.x[i] = (float) (cos * x - sin * z);
						q.z[i] = (float) (sin * x + cos * z);
					}
					break;
				}
				case COLOUR_MULTIPLIER:
					// Colour is a render-time state, not a positional transform.
					break;
			}
		}
		return q;
	}

	// ─── Render-time emission ───────────────────────────────────────────────

	@Override
	public void render(I3DOperation... ops) {
		LPRenderStateImpl rs = (LPRenderStateImpl) SimpleServiceLocator.cclProxy.getRenderState();
		if (rs == null || rs.buffer == null) return;

		TextureTransformation tex = null;
		LPUvOp uvOp = null;
		int colour = rs.colourARGB;

		// Classify operations into render-time categories. Positional ops passed
		// at render time (rare in LP) are applied to a temporary quad copy below.
		List<I3DOperation> positional = Collections.emptyList();
		List<LPUvOp> uvOps = new ArrayList<>();
		// Flatten ops: LPUVTransformationList children are processed recursively so the
		// inner LPUvOp (scale) is actually picked up — the previous cclProxy round-trip
		// silently dropped it, causing raw 0..1 UVs to sample the sprite right edge
		// and bleed into neighboring atlas sprites.
		java.util.Deque<I3DOperation> queue = new java.util.ArrayDeque<>();
		for (I3DOperation op : ops) if (op != null) queue.add(op);
		while (!queue.isEmpty()) {
			I3DOperation op = queue.poll();
			if (op instanceof logisticspipes.proxy.object3d.operation.LPUVTransformationList) {
				logisticspipes.proxy.object3d.operation.LPUVTransformationList list =
					(logisticspipes.proxy.object3d.operation.LPUVTransformationList) op;
				for (I3DOperation c : list.getChildren()) if (c != null) queue.add(c);
				continue;
			}
			if (op instanceof TextureTransformation) {
				tex = (TextureTransformation) op;
				continue;
			}
			Object o = op.getOriginal();
			if (o instanceof LPUvOp) {
				uvOps.add((LPUvOp) o);
			} else if (o instanceof LPTransformOp && ((LPTransformOp) o).type == LPTransformOp.Type.COLOUR_MULTIPLIER) {
				colour = ((LPTransformOp) o).colourARGB;
			} else if (o instanceof LPTransformOp || o instanceof LPTranslationImpl) {
				if (positional.isEmpty()) positional = new ArrayList<>();
				positional.add(op);
			}
		}
		if (!uvOps.isEmpty()) uvOp = LPUvOp.list(uvOps.toArray(new LPUvOp[0]));

		final VertexConsumer buf = rs.buffer;
		final org.joml.Matrix4f pose = rs.pose;
		final org.joml.Matrix3f nrm = rs.normal;
		final int light = rs.packedLight;
		final int overlay = rs.packedOverlay;
		final float a = ((colour >> 24) & 0xff) / 255.0f;
		final float r = ((colour >> 16) & 0xff) / 255.0f;
		final float g = ((colour >> 8) & 0xff) / 255.0f;
		final float b = (colour & 0xff) / 255.0f;

		float[] uv = new float[2];
		for (LPQuadData src : quads) {
			LPQuadData q = src;
			if (!positional.isEmpty()) {
				q = src.copy();
				for (I3DOperation op : positional) transformPositional(q, op);
			}
			for (int i = 0; i < 4; i++) {
				uv[0] = q.u[i];
				uv[1] = q.v[i];
				if (uvOp != null) uvOp.apply(uv);
				float finalU = uv[0];
				float finalV = uv[1];
				TextureTransformation innerTex = tex;
				if (innerTex instanceof logisticspipes.proxy.object3d.operation.LPUVTransformationList) {
					innerTex = ((logisticspipes.proxy.object3d.operation.LPUVTransformationList) innerTex).getInnerTexture();
				}
				if (innerTex instanceof LPTextureTransformationImpl) {
					LPTextureTransformationImpl ltt = (LPTextureTransformationImpl) innerTex;
					finalU = ltt.mapU(uv[0]);
					finalV = ltt.mapV(uv[1]);
				}
				if (pose != null) {
					buf.vertex(pose, q.x[i], q.y[i], q.z[i])
						.color(r, g, b, a)
						.uv(finalU, finalV)
						.overlayCoords(overlay)
						.uv2(light)
						.normal(nrm, q.nx[i], q.ny[i], q.nz[i])
						.endVertex();
				} else {
					buf.vertex(q.x[i], q.y[i], q.z[i])
						.color(r, g, b, a)
						.uv(finalU, finalV)
						.overlayCoords(overlay)
						.uv2(light)
						.normal(q.nx[i], q.ny[i], q.nz[i])
						.endVertex();
				}
			}
		}
	}

	@Override
	public List<BakedQuad> renderToQuads(VertexFormat format, I3DOperation... ops) {
		// TODO: used by item-form pipe model baking; defer until BakedModel integration.
		return Collections.emptyList();
	}

	@Override
	public void computeNormals() {
		for (LPQuadData q : quads) {
			double ax = q.x[1] - q.x[0], ay = q.y[1] - q.y[0], az = q.z[1] - q.z[0];
			double bx = q.x[2] - q.x[0], by = q.y[2] - q.y[0], bz = q.z[2] - q.z[0];
			double nx = ay * bz - az * by;
			double ny = az * bx - ax * bz;
			double nz = ax * by - ay * bx;
			double mag = Math.sqrt(nx * nx + ny * ny + nz * nz);
			if (mag <= 1e-9) {
				// Degenerate: v[0]==v[1] (triangle stored as [v0,v0,v1,v2] after backface-flip).
				// Fall back to (v[2]-v[0]) × (v[3]-v[0]) to recover the opposite-facing normal.
				ax = q.x[2] - q.x[0]; ay = q.y[2] - q.y[0]; az = q.z[2] - q.z[0];
				bx = q.x[3] - q.x[0]; by = q.y[3] - q.y[0]; bz = q.z[3] - q.z[0];
				nx = ay * bz - az * by;
				ny = az * bx - ax * bz;
				nz = ax * by - ay * bx;
				mag = Math.sqrt(nx * nx + ny * ny + nz * nz);
			}
			if (mag > 1e-9) { nx /= mag; ny /= mag; nz /= mag; }
			for (int i = 0; i < 4; i++) {
				q.nx[i] = (float) nx;
				q.ny[i] = (float) ny;
				q.nz[i] = (float) nz;
			}
		}
	}

	@Override
	public void computeStandardLighting() {
		// 1.20.1: lighting is computed per-vertex from packedLight; no pre-bake needed.
	}

	@Override
	public IBounds bounds() {
		if (cachedBounds != null) return cachedBounds;
		if (quads.isEmpty()) {
			cachedBounds = new LPBoundsImpl(0, 0, 0, 0, 0, 0);
			return cachedBounds;
		}
		float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
		float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
		for (LPQuadData q : quads) {
			for (int i = 0; i < 4; i++) {
				if (q.x[i] < minX) minX = q.x[i];
				if (q.y[i] < minY) minY = q.y[i];
				if (q.z[i] < minZ) minZ = q.z[i];
				if (q.x[i] > maxX) maxX = q.x[i];
				if (q.y[i] > maxY) maxY = q.y[i];
				if (q.z[i] > maxZ) maxZ = q.z[i];
			}
		}
		cachedBounds = new LPBoundsImpl(minX, minY, minZ, maxX, maxY, maxZ);
		return cachedBounds;
	}

	@Override
	public IModel3D copy() {
		List<LPQuadData> out = new ArrayList<>(quads.size());
		for (LPQuadData q : quads) out.add(q.copy());
		return new LPModel3DImpl(out);
	}

	@Override
	public IModel3D backfacedCopy() {
		List<LPQuadData> out = new ArrayList<>(quads.size() * 2);
		for (LPQuadData q : quads) {
			out.add(q.copy());
			out.add(q.flipped());
		}
		return new LPModel3DImpl(out);
	}

	@Override
	public IModel3D twoFacedCopy() {
		return backfacedCopy();
	}

	@Override
	public Object getOriginal() {
		return this;
	}

	@Override
	public IBounds getBoundsInside(AABB boundingBox) {
		// TODO: used for collision clipping in CCL; deferred — full bounds is a safe over-approximation.
		return bounds();
	}
}
