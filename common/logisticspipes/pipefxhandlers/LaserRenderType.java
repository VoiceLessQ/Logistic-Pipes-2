package logisticspipes.pipefxhandlers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureManager;

/** Untextured colour quads for the power lasers; the particle engine owns begin/end. */
final class LaserRenderType implements ParticleRenderType {

	static final ParticleRenderType INSTANCE = new LaserRenderType();

	private LaserRenderType() {}

	@Override
	public BufferBuilder begin(Tesselator tesselator, TextureManager textureManager) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.depthMask(false);
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		return tesselator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
	}

	@Override
	public String toString() {
		return "LOGISTICS_LASER";
	}
}
