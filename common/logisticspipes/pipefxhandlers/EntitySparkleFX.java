package logisticspipes.pipefxhandlers;

import java.util.Random;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;

import logisticspipes.LPConstants;

public class EntitySparkleFX extends SingleQuadParticle {

	private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(LPConstants.LP_MOD_ID, "textures/particles/particles.png");
	// LP1's own 8x8 sheet instead of an atlas sprite
	// LP1 blended sparkles additively (SRC_ALPHA, ONE) without writing depth
	private static final RenderPipeline ADDITIVE_PARTICLE = RenderPipeline.builder(RenderPipelines.PARTICLE_SNIPPET)
			.withLocation(Identifier.fromNamespaceAndPath(LPConstants.LP_MOD_ID, "pipeline/additive_particle"))
			.withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
			.withDepthStencilState(new DepthStencilState(CompareOp.GREATER_THAN_OR_EQUAL, false))
			.build();
	private static final SingleQuadParticle.Layer LAYER = new SingleQuadParticle.Layer(true, TEXTURE, ADDITIVE_PARTICLE);

	public int multiplier;
	public boolean shrink;
	public int particle;
	public int blendmode;

	public EntitySparkleFX(ClientLevel world, double x, double y, double z, float scalemult, float red, float green, float blue, int var12) {
		super(world, x, y, z, 0.0D, 0.0D, 0.0D, Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.PARTICLES).missingSprite());
		shrink = false;
		particle = 0;
		blendmode = 1;

		rCol = red;
		gCol = green;
		bCol = blue;
		gravity = 0.07F;
		xd = 0.0D;
		yd = 0.0D;
		zd = 0.0D;
		this.scale(scalemult);
		lifetime = 3 * var12 - 1;
		multiplier = var12;
		hasPhysics = false;
	}

	@Override
	protected SingleQuadParticle.Layer getLayer() {
		return LAYER;
	}

	@Override
	public float getQuadSize(float partialTicks) {
		// LP1: half-extent = 0.1 * particleScale * lifeFade (always shrinking); bbWidth = 0.2 * scalemult
		return 0.1f * this.bbWidth * ((float) (lifetime - age + 1) / (float) lifetime);
	}

	// LP1 animation: one 8x8 sheet cell per `multiplier` ticks
	private int frame() {
		return particle + age / multiplier;
	}

	@Override
	protected float getU0() {
		return frame() % 8 / 8.0F + 0.124875F;
	}

	@Override
	protected float getU1() {
		return frame() % 8 / 8.0F;
	}

	@Override
	protected float getV0() {
		return frame() / 8 / 8.0F;
	}

	@Override
	protected float getV1() {
		return frame() / 8 / 8.0F + 0.124875F;
	}

	/**
	 * Called to update the entity's position/logic.
	 */
	@Override
	public void tick() {
		try {
			LocalPlayer var1 = Minecraft.getInstance().player;

			if (var1.distanceToSqr(x, y, z) > 2500) {
				remove();
			}

			xo = x;
			yo = y;
			zo = z;

			if (age++ >= lifetime) {
				remove();
			}

			xd -= 0.05D * gravity - 0.1D * gravity * new Random().nextDouble();
			yd -= 0.05D * gravity - 0.1D * gravity * new Random().nextDouble();
			zd -= 0.05D * gravity - 0.1D * gravity * new Random().nextDouble();

			move(xd, yd, zd);
			xd *= 0.9800000190734863D;
			yd *= 0.9800000190734863D;
			zd *= 0.9800000190734863D;

			if (onGround) {
				xd *= 0.699999988079071D;
				zd *= 0.699999988079071D;
			}
		} catch (Exception ignored) {
		}
	}
}
