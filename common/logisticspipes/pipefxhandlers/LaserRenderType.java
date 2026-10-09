package logisticspipes.pipefxhandlers;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
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
	public void begin(BufferBuilder bb, TextureManager textureManager) {
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.depthMask(false);
		RenderSystem.setShader(GameRenderer::getPositionColorShader);
		bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
	}

	@Override
	public void end(Tesselator tesselator) {
		BufferUploader.drawWithShader(tesselator.getBuilder().end());
		RenderSystem.depthMask(true);
		RenderSystem.disableBlend();
	}

	@Override
	public String toString() {
		return "LOGISTICS_LASER";
	}
}
