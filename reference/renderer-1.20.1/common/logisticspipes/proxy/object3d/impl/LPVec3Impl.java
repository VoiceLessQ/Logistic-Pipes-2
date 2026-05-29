package logisticspipes.proxy.object3d.impl;

import logisticspipes.proxy.object3d.interfaces.IVec3;

/**
 * NeoForge 1.20.1 native replacement for CCL's Vector3.
 */
public final class LPVec3Impl implements IVec3 {

	public final double x;
	public final double y;
	public final double z;

	public LPVec3Impl(double x, double y, double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override public double x() { return x; }
	@Override public double y() { return y; }
	@Override public double z() { return z; }

	@Override
	public Object getOriginal() {
		return this;
	}
}
