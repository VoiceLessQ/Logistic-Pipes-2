package logisticspipes.proxy.object3d.impl;

/**
 * Tagged union of all positional / colour transforms that can be applied to an
 * {@link LPModel3DImpl}. Used as the concrete backing object returned by
 * {@code CCLProxy.getScale/getRotation/getColourMultiplier/...}.
 *
 * <p>Kept as a single struct-like class (rather than a sealed hierarchy) so that
 * {@code LPModel3DImpl.render(I3DOperation...)} can dispatch via {@link #type}
 * without virtual calls during hot-path rendering.</p>
 */
public final class LPTransformOp {

	public enum Type {
		SCALE_UNIFORM,
		SCALE_XYZ,
		ROTATION_AXIS_ANGLE,    // angle (radians) around unit axis (ax, ay, az) at origin (ox, oy, oz)
		ROTATION_SIDE,          // CCL sideOrientation(side, meta): side→from 0 (base), rotated to `meta` around Y
		COLOUR_MULTIPLIER
	}

	public final Type type;
	public double sx, sy, sz;        // scale components
	public double ax, ay, az;        // axis (rotation) or origin
	public double ox, oy, oz;        // origin (rotation)
	public double angle;              // radians
	public int colourARGB;            // packed 0xAARRGGBB
	public int side;                  // ROTATION_SIDE: from-side
	public int meta;                  // ROTATION_SIDE: to-side/meta

	private LPTransformOp(Type type) {
		this.type = type;
	}

	public static LPTransformOp uniformScale(double s) {
		LPTransformOp op = new LPTransformOp(Type.SCALE_UNIFORM);
		op.sx = op.sy = op.sz = s;
		return op;
	}

	public static LPTransformOp scale(double sx, double sy, double sz) {
		LPTransformOp op = new LPTransformOp(Type.SCALE_XYZ);
		op.sx = sx;
		op.sy = sy;
		op.sz = sz;
		return op;
	}

	public static LPTransformOp rotation(double angleRad, int axisX, int axisY, int axisZ) {
		LPTransformOp op = new LPTransformOp(Type.ROTATION_AXIS_ANGLE);
		op.angle = angleRad;
		op.ax = axisX;
		op.ay = axisY;
		op.az = axisZ;
		op.ox = op.oy = op.oz = 0.5;
		return op;
	}

	public static LPTransformOp sideRotation(int from, int to) {
		LPTransformOp op = new LPTransformOp(Type.ROTATION_SIDE);
		op.side = from;
		op.meta = to;
		return op;
	}

	public static LPTransformOp colour(int argb) {
		LPTransformOp op = new LPTransformOp(Type.COLOUR_MULTIPLIER);
		op.colourARGB = argb;
		return op;
	}
}
