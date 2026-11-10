package logisticspipes.pipefxhandlers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import lombok.Setter;
import lombok.experimental.Accessors;

import network.rs485.logisticspipes.world.DoubleCoordinates;

@Accessors(chain = true)
public class PipeFXLaserPowerBeam extends Particle implements CustomParticleGroup.Rendered {

	@Setter
	private boolean reverse = false;

	private final float length;
	private final Direction dir;
	private final float r;
	private final float g;
	private final float b;

	public PipeFXLaserPowerBeam(ClientLevel world, DoubleCoordinates pos, float length, Direction dir, int color, BlockEntity tile) {
		super(world, pos.getXCoord() + 0.5D, pos.getYCoord() + 0.5D, pos.getZCoord() + 0.5D, 0.0D, 0.0D, 0.0D);
		this.length = length;
		this.dir = dir;
		this.r = ((color >> 16) & 0xFF) / 255.0f;
		this.g = ((color >> 8) & 0xFF) / 255.0f;
		this.b = (color & 0xFF) / 255.0f;
		this.lifetime = 6000;
		this.hasPhysics = false;
	}

	@Override
	public ParticleRenderType getGroup() {
		return CustomParticleGroup.TYPE;
	}

	@Override
	public void submit(SubmitNodeCollector collector, Vec3 cameraPos, float partialTicks) {
		collector.submitCustomGeometry(new PoseStack(), RenderTypes.debugQuads(), (pose, bb) -> render(bb, pose, cameraPos, partialTicks));
	}

	private void render(VertexConsumer bb, PoseStack.Pose pose, Vec3 cameraPos, float partialTicks) {
		double px = Mth.lerp(partialTicks, xo, x) - cameraPos.x;
		double py = Mth.lerp(partialTicks, yo, y) - cameraPos.y;
		double pz = Mth.lerp(partialTicks, zo, z) - cameraPos.z;

		// Beam axis and sign
		Direction actual = reverse ? dir.getOpposite() : dir;
		float ax = actual.getStepX();
		float ay = actual.getStepY();
		float az = actual.getStepZ();

		// Two perpendicular axes for the cross-section half-width
		float hw = 0.04f;
		float p1x, p1y, p1z, p2x, p2y, p2z;
		if (actual.getAxis() == Direction.Axis.Y) {
			p1x = hw; p1y = 0; p1z = 0;
			p2x = 0;  p2y = 0; p2z = hw;
		} else if (actual.getAxis() == Direction.Axis.X) {
			p1x = 0; p1y = hw; p1z = 0;
			p2x = 0; p2y = 0;  p2z = hw;
		} else { // Z
			p1x = hw; p1y = 0; p1z = 0;
			p2x = 0;  p2y = hw; p2z = 0;
		}

		int ri = (int) (r * 255);
		int gi = (int) (g * 255);
		int bi = (int) (b * 255);
		int ai = 200;


		// Two crossed quads along the beam axis for a 3D beam look
		beamQuad(bb, pose, px, py, pz, ax, ay, az, length, p1x, p1y, p1z, ri, gi, bi, ai);
		beamQuad(bb, pose, px, py, pz, ax, ay, az, length, p2x, p2y, p2z, ri, gi, bi, ai);

	}

	/** Render one flat quad along the beam axis, offset by ±perp. */
	private static void beamQuad(VertexConsumer bb, PoseStack.Pose pose,
			double ox, double oy, double oz,
			float ax, float ay, float az, float len,
			float px, float py, float pz,
			int r, int g, int b, int a) {
		// Near end: origin ± perp
		// Far end:  origin + axis*len ± perp
		float fx = (float) ox + ax * len;
		float fy = (float) oy + ay * len;
		float fz = (float) oz + az * len;

		bb.addVertex(pose, (float) ox + px, (float) oy + py, (float) oz + pz).setColor(r, g, b, a);
		bb.addVertex(pose, (float) ox - px, (float) oy - py, (float) oz - pz).setColor(r, g, b, a);

		bb.addVertex(pose, fx - px, fy - py, fz - pz).setColor(r, g, b, a);
		bb.addVertex(pose, fx + px, fy + py, fz + pz).setColor(r, g, b, a);
	}
}
