package logisticspipes.proxy.object3d.interfaces;

import java.util.List;

import net.minecraft.client.resources.model.geometry.BakedQuad;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.world.phys.AABB;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public interface IModel3D {

	IModel3D backfacedCopy();

	void render(I3DOperation... i3dOperations);

	@Environment(EnvType.CLIENT)
	List<BakedQuad> renderToQuads(VertexFormat format, I3DOperation... i3dOperations);

	void computeNormals();

	void computeStandardLighting();

	IBounds bounds();

	IModel3D apply(I3DOperation translation);

	IModel3D copy();

	IModel3D twoFacedCopy();

	Object getOriginal();

	IBounds getBoundsInside(AABB boundingBox);

}
