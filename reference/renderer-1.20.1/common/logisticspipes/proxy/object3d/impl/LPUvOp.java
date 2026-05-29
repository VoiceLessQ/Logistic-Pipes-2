package logisticspipes.proxy.object3d.impl;

/**
 * Tagged union of UV-space transforms — the backing object returned by
 * {@code CCLProxy.getUVScale/getUVTranslation/getUVTransformationList}.
 */
public final class LPUvOp {

	public enum Type { TRANSLATE, SCALE, LIST }

	public final Type type;
	public float uDelta, vDelta;
	public float uScale, vScale;
	public LPUvOp[] list;

	private LPUvOp(Type type) {
		this.type = type;
	}

	public static LPUvOp translate(float du, float dv) {
		LPUvOp op = new LPUvOp(Type.TRANSLATE);
		op.uDelta = du;
		op.vDelta = dv;
		return op;
	}

	public static LPUvOp scale(float su, float sv) {
		LPUvOp op = new LPUvOp(Type.SCALE);
		op.uScale = su;
		op.vScale = sv;
		return op;
	}

	public static LPUvOp list(LPUvOp[] children) {
		LPUvOp op = new LPUvOp(Type.LIST);
		op.list = children;
		return op;
	}

	/** Apply this UV op in-place on a {u, v} pair. */
	public void apply(float[] uv) {
		switch (type) {
			case TRANSLATE:
				uv[0] += uDelta;
				uv[1] += vDelta;
				break;
			case SCALE:
				uv[0] *= uScale;
				uv[1] *= vScale;
				break;
			case LIST:
				for (LPUvOp child : list) child.apply(uv);
				break;
		}
	}
}
