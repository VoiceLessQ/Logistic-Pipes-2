package logisticspipes.proxy.object3d.impl;

import net.minecraft.world.phys.AABB;

import logisticspipes.proxy.object3d.interfaces.IBounds;
import logisticspipes.proxy.object3d.interfaces.IVec3;

/**
 * NeoForge 1.20.1 native replacement for CCL's Cuboid6 bounds.
 */
public final class LPBoundsImpl implements IBounds {

	private final LPVec3Impl min;
	private final LPVec3Impl max;

	public LPBoundsImpl(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		this.min = new LPVec3Impl(minX, minY, minZ);
		this.max = new LPVec3Impl(maxX, maxY, maxZ);
	}

	@Override public IVec3 max() { return max; }
	@Override public IVec3 min() { return min; }

	@Override
	public AABB toAABB() {
		return new AABB(min.x, min.y, min.z, max.x, max.y, max.z);
	}
}
