package logisticspipes.proxy.object3d.interfaces;

import net.minecraft.world.phys.AABB;

public interface IBounds {

	IVec3 max();

	IVec3 min();

	AABB toAABB();

}
