package logisticspipes.proxy.object3d.impl;

/**
 * Raw data for a single quad: 4 vertices each with position, UV, normal.
 *
 * <p>All arrays are length 4, indexed [0..3] in the order the OBJ face declared
 * them. This is deliberately a mutable plain-old-data class — transforms like
 * translation / scale / rotation are applied in-place on a copy created via
 * {@link #copy()}.</p>
 */
public final class LPQuadData {

	public final float[] x = new float[4];
	public final float[] y = new float[4];
	public final float[] z = new float[4];
	public final float[] u = new float[4];
	public final float[] v = new float[4];
	public final float[] nx = new float[4];
	public final float[] ny = new float[4];
	public final float[] nz = new float[4];

	public LPQuadData copy() {
		LPQuadData other = new LPQuadData();
		System.arraycopy(x, 0, other.x, 0, 4);
		System.arraycopy(y, 0, other.y, 0, 4);
		System.arraycopy(z, 0, other.z, 0, 4);
		System.arraycopy(u, 0, other.u, 0, 4);
		System.arraycopy(v, 0, other.v, 0, 4);
		System.arraycopy(nx, 0, other.nx, 0, 4);
		System.arraycopy(ny, 0, other.ny, 0, 4);
		System.arraycopy(nz, 0, other.nz, 0, 4);
		return other;
	}

	/** Returns a copy with the winding order reversed (flips the normal). */
	public LPQuadData flipped() {
		LPQuadData other = new LPQuadData();
		for (int i = 0; i < 4; i++) {
			int j = 3 - i;
			other.x[i] = x[j];
			other.y[i] = y[j];
			other.z[i] = z[j];
			other.u[i] = u[j];
			other.v[i] = v[j];
			other.nx[i] = -nx[j];
			other.ny[i] = -ny[j];
			other.nz[i] = -nz[j];
		}
		return other;
	}
}
