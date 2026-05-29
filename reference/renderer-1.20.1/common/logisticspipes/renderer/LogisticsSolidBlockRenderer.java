package logisticspipes.renderer;

import java.util.EnumMap;
import java.util.Map;
import javax.annotation.Nonnull;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import logisticspipes.LPConstants;
import logisticspipes.blocks.LogisticsSolidBlock;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.object3d.impl.LPRenderStateImpl;
import logisticspipes.proxy.object3d.interfaces.IModel3D;
import logisticspipes.proxy.object3d.interfaces.TextureTransformation;
import logisticspipes.renderer.newpipe.LogisticsNewSolidBlockWorldRenderer;

/**
 * Shared BlockEntityRenderer for all LP solid blocks. Reuses the OBJ-parsed 3D body
 * and 5 cover plates loaded by {@link LogisticsNewSolidBlockWorldRenderer} and renders
 * them to the cutoutMipped buffer via {@link LPRenderStateImpl}.
 *
 * <p>Each {@link LogisticsSolidBlock.Type} maps to a sprite at
 * {@code logisticspipes:solid_block/<name>} which is used as the plate texture.</p>
 */
public class LogisticsSolidBlockRenderer<T extends BlockEntity> implements BlockEntityRenderer<T> {

	private static final Map<LogisticsSolidBlock.Type, TextureTransformation> SPRITE_CACHE =
			new EnumMap<>(LogisticsSolidBlock.Type.class);

	public LogisticsSolidBlockRenderer(BlockEntityRendererProvider.Context context) {}

	public static String textureNameFor(LogisticsSolidBlock.Type type) {
		switch (type) {
			case LOGISTICS_BLOCK_FRAME:        return "frame";
			case LOGISTICS_POWER_JUNCTION:     return "power_junction";
			case LOGISTICS_SECURITY_STATION:   return "security_station";
			case LOGISTICS_AUTOCRAFTING_TABLE: return "crafting_table";
			case LOGISTICS_FUZZYCRAFTING_TABLE:return "crafting_table_fuzzy";
			case LOGISTICS_STATISTICS_TABLE:   return "statistics_table";
			case LOGISTICS_RF_POWERPROVIDER:   return "power_provider_rf";
			case LOGISTICS_IC2_POWERPROVIDER:  return "power_provider_eu";
			case LOGISTICS_BC_POWERPROVIDER:   return "power_provider_mj";
			case LOGISTICS_PROGRAM_COMPILER:   return "program_compiler";
			default:                           return "frame";
		}
	}

	public static TextureTransformation getIcon(LogisticsSolidBlock.Type type) {
		TextureTransformation cached = SPRITE_CACHE.get(type);
		if (cached != null) return cached;
		TextureAtlasSprite sprite = Minecraft.getInstance()
				.getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
				.apply(new ResourceLocation(LPConstants.LP_MOD_ID, "solid_block/" + textureNameFor(type)));
		TextureTransformation tx = SimpleServiceLocator.cclProxy.createIconTransformer(sprite);
		SPRITE_CACHE.put(type, tx);
		return tx;
	}

	public static void clearCache() {
		SPRITE_CACHE.clear();
	}

	@Override
	public void render(@Nonnull T be, float partialTicks, @Nonnull PoseStack pose,
			@Nonnull MultiBufferSource buffers, int light, int overlay) {
		Block block = be.getBlockState().getBlock();
		if (!(block instanceof LogisticsSolidBlock)) return;
		renderSolid(((LogisticsSolidBlock) block).getType(), pose, buffers, light, overlay);
	}

	/** Shared draw path used by both the in-world BER and the item BEWLR. */
	public static void renderSolid(LogisticsSolidBlock.Type type, PoseStack pose,
			MultiBufferSource buffers, int light, int overlay) {
		if (!SimpleServiceLocator.cclProxy.isActivated()) return;
		if (!(SimpleServiceLocator.cclProxy.getRenderState() instanceof LPRenderStateImpl)) return;
		if (LogisticsNewSolidBlockWorldRenderer.block == null
				|| LogisticsNewSolidBlockWorldRenderer.block.isEmpty()) return;

		LPRenderStateImpl rs = (LPRenderStateImpl) SimpleServiceLocator.cclProxy.getRenderState();
		VertexConsumer buffer = buffers.getBuffer(RenderType.cutoutMipped());
		rs.bind(buffer, pose.last().pose(), pose.last().normal(), light, overlay);
		rs.reset();

		TextureTransformation icon = getIcon(type);
		if (icon == null) return;

		LogisticsNewSolidBlockWorldRenderer.BlockRotation rotation =
				LogisticsNewSolidBlockWorldRenderer.BlockRotation.ZERO;

		IModel3D body = LogisticsNewSolidBlockWorldRenderer.block.get(rotation);
		if (body != null) {
			body.render(icon);
		}
		// Frame has no outer/inner cover plates in the legacy inventory render; mirror that.
		if (type != LogisticsSolidBlock.Type.LOGISTICS_BLOCK_FRAME) {
			for (LogisticsNewSolidBlockWorldRenderer.CoverSides side :
					LogisticsNewSolidBlockWorldRenderer.CoverSides.values()) {
				Map<LogisticsNewSolidBlockWorldRenderer.BlockRotation, IModel3D> outer =
						LogisticsNewSolidBlockWorldRenderer.texturePlate_Outer.get(side);
				Map<LogisticsNewSolidBlockWorldRenderer.BlockRotation, IModel3D> inner =
						LogisticsNewSolidBlockWorldRenderer.texturePlate_Inner.get(side);
				if (outer != null && outer.get(rotation) != null) {
					outer.get(rotation).render(icon);
				}
				if (inner != null && inner.get(rotation) != null) {
					inner.get(rotation).render(icon);
				}
			}
		}
		rs.draw();
	}
}
