package logisticspipes.pipefxhandlers;

import java.util.List;

import net.minecraft.client.Camera;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.particle.v1.ParticleGroupRegistry;

/** LP particles that draw their own geometry (lasers, pipe destruction models) instead of a single sprite quad. */
public class CustomParticleGroup extends ParticleGroup<Particle> {

	// Fabric derives the group id from the name, so it must be a valid identifier
	public static final ParticleRenderType TYPE = new ParticleRenderType("logisticspipes:custom", "LPC");

	public interface Rendered {

		/** Positions are relative to {@code cameraPos}, like vanilla particle quads. */
		void submit(SubmitNodeCollector collector, Vec3 cameraPos, float partialTicks);
	}

	public CustomParticleGroup(ParticleEngine engine) {
		super(engine);
	}

	public static void register() {
		ParticleGroupRegistry.register(TYPE, CustomParticleGroup::new);
	}

	@Override
	public ParticleGroupRenderState extractRenderState(Frustum frustum, Camera camera, float partialTicks) {
		Vec3 cameraPos = camera.position();
		List<Rendered> rendered = particles.stream().map(Rendered.class::cast).toList();
		return (collector, cameraState) -> rendered.forEach(particle -> particle.submit(collector, cameraPos, partialTicks));
	}
}
