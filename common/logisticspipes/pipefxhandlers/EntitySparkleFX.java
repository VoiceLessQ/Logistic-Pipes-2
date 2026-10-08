package logisticspipes.pipefxhandlers;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;

import java.util.Random;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import logisticspipes.LPConstants;

public class EntitySparkleFX extends Particle {

	private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(LPConstants.LP_MOD_ID, "textures/particles/particles.png");

	public int multiplier;
	public boolean shrink;
	public int particle;
	public int blendmode;

	public EntitySparkleFX(ClientLevel world, double x, double y, double z, float scalemult, float red, float green, float blue, int var12) {
		super(world, x, y, z, 0.0D, 0.0D, 0.0D);
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

	// LP1: own sheet, additive blend (SRC_ALPHA, ONE), no depth write
	private static final ParticleRenderType SPARKLE_RENDER_TYPE = new ParticleRenderType() {
		@Override
		public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
			RenderSystem.enableBlend();
			RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
			RenderSystem.depthMask(false);
			RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
			RenderSystem.setShaderTexture(0, TEXTURE);
			return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
		}

		@Override
		public String toString() {
			return "LOGISTICS_SPARKLE";
		}
	};

	@Override
	public ParticleRenderType getRenderType() {
		return SPARKLE_RENDER_TYPE;
	}

	@Override
	public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
		double px = Mth.lerp(partialTicks, xo, x) - camera.getPosition().x;
		double py = Mth.lerp(partialTicks, yo, y) - camera.getPosition().y;
		double pz = Mth.lerp(partialTicks, zo, z) - camera.getPosition().z;

		org.joml.Quaternionf rot = camera.rotation();
		org.joml.Vector3f right = new org.joml.Vector3f(1, 0, 0);
		org.joml.Vector3f up    = new org.joml.Vector3f(0, 1, 0);
		rot.transform(right);
		rot.transform(up);

		// LP1: half-extent = 0.1 * particleScale * lifeFade (always shrinking).
		// bbWidth = 0.2 * scalemult here, so 0.1 * bbWidth ~ LP1's 0.02 * scalemult.
		float s = 0.1f * this.bbWidth * ((float) (lifetime - age + 1) / (float) lifetime);
		// LP1 animation: one 8x8 sheet cell per `multiplier` ticks
		int frame = particle + age / multiplier;
		float u0 = frame % 8 / 8.0F;
		float u1 = u0 + 0.124875F;
		float v0 = frame / 8 / 8.0F;
		float v1 = v0 + 0.124875F;
		int r = (int) (rCol * 255);
		int g = (int) (gCol * 255);
		int b = (int) (bCol * 255);

		billboardVertex(buffer, px, py, pz, right, up, -s, -s, u1, v1, r, g, b);
		billboardVertex(buffer, px, py, pz, right, up, -s,  s, u1, v0, r, g, b);
		billboardVertex(buffer, px, py, pz, right, up,  s,  s, u0, v0, r, g, b);
		billboardVertex(buffer, px, py, pz, right, up,  s, -s, u0, v1, r, g, b);
	}

	private static void billboardVertex(VertexConsumer buf, double cx, double cy, double cz,
			org.joml.Vector3f right, org.joml.Vector3f up, float rs, float us,
			float u, float v, int r, int g, int b) {
		buf.addVertex((float) (cx + right.x * rs + up.x * us),
		           (float) (cy + right.y * rs + up.y * us),
		           (float) (cz + right.z * rs + up.z * us))
		   .setUv(u, v)
		   .setColor(r, g, b, 255);
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
