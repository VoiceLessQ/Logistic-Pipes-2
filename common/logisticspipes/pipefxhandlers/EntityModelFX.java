package logisticspipes.pipefxhandlers;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.object3d.impl.LPRenderStateImpl;
import logisticspipes.proxy.object3d.interfaces.I3DOperation;
import logisticspipes.proxy.object3d.interfaces.IModel3D;

public class EntityModelFX extends Particle implements CustomParticleGroup.Rendered {

	private final IModel3D model;
	private final I3DOperation[] operations;
	private final Identifier texture;

	public EntityModelFX(ClientLevel world, double x, double y, double z, IModel3D model, I3DOperation[] i3dOperations, Identifier texture) {
		super(world, x, y, z, 0, -5, 0);
		this.model = model;
		this.operations = i3dOperations;
		this.texture = texture;
	}

	@Override
	public ParticleRenderType getGroup() {
		return CustomParticleGroup.TYPE;
	}

	@Override
	public void submit(SubmitNodeCollector collector, Vec3 cameraPos, float partialTicks) {
		PoseStack poseStack = new PoseStack();
		poseStack.translate(Mth.lerp(partialTicks, xo, x) - cameraPos.x, Mth.lerp(partialTicks, yo, y) - cameraPos.y, Mth.lerp(partialTicks, zo, z) - cameraPos.z);
		int packedLight = LightCoordsUtil.getLightCoords(level, BlockPos.containing(x, y, z));
		collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(texture), (pose, buffer) -> {
			if (!(SimpleServiceLocator.cclProxy.getRenderState() instanceof LPRenderStateImpl state)) return;
			state.bind(buffer, pose.pose(), pose.normal(), packedLight, OverlayTexture.NO_OVERLAY);
			state.reset();
			state.setAlphaOverride(0xff);
			model.render(operations);
		});
	}
}
