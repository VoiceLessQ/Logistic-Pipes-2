package logisticspipes.renderer;

import java.util.EnumMap;
import java.util.Map;
import javax.annotation.Nonnull;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import logisticspipes.LPConstants;
import logisticspipes.blocks.LogisticsSolidBlock;
import logisticspipes.blocks.LogisticsSolidTileEntity;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.object3d.impl.LPRenderStateImpl;
import logisticspipes.proxy.object3d.interfaces.IModel3D;
import logisticspipes.proxy.object3d.interfaces.TextureTransformation;
import logisticspipes.renderer.newpipe.LogisticsNewSolidBlockWorldRenderer;
import network.rs485.logisticspipes.world.CoordinateUtils;
import network.rs485.logisticspipes.world.DoubleCoordinates;

/**
 * Shared BlockEntityRenderer for all LP solid blocks. Reuses the OBJ-parsed 3D body
 * and 5 cover plates loaded by {@link LogisticsNewSolidBlockWorldRenderer} and renders
 * them through {@link LogisticsRenderPipe#submitGeometry}.
 *
 * <p>Each {@link LogisticsSolidBlock.Type} maps to a sprite at
 * {@code logisticspipes:solid_block/<name>} which is used as the plate texture.</p>
 */
public class LogisticsSolidBlockRenderer<T extends BlockEntity> implements BlockEntityRenderer<T, LogisticsSolidBlockRenderer.State> {

	public static class State extends BlockEntityRenderState {

		public BlockEntity blockEntity;
	}

	private static final Map<LogisticsSolidBlock.Type, TextureTransformation> SPRITE_CACHE =
			new EnumMap<>(LogisticsSolidBlock.Type.class);
	private static final Map<LogisticsSolidBlock.Type, TextureTransformation> SPRITE_CACHE_ACTIVE =
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
		return getIcon(type, false);
	}

	/** LP1: types with an active texture switch to {@code <name>_active} while the tile is active. */
	public static TextureTransformation getIcon(LogisticsSolidBlock.Type type, boolean active) {
		boolean useActive = active && type.isHasActiveTexture();
		Map<LogisticsSolidBlock.Type, TextureTransformation> cache = useActive ? SPRITE_CACHE_ACTIVE : SPRITE_CACHE;
		TextureTransformation cached = cache.get(type);
		if (cached != null) return cached;
		String name = textureNameFor(type) + (useActive ? "_active" : "");
		TextureAtlasSprite sprite = LogisticsRenderPipe.getBlockSprite(Identifier.fromNamespaceAndPath(LPConstants.LP_MOD_ID, "solid_block/" + name));
		TextureTransformation tx = SimpleServiceLocator.cclProxy.createIconTransformer(sprite);
		cache.put(type, tx);
		return tx;
	}

	public static void clearCache() {
		SPRITE_CACHE.clear();
		SPRITE_CACHE_ACTIVE.clear();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(T be, State state, float partialTicks, Vec3 cameraPosition, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
		BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
		state.blockEntity = be;
	}

	@Override
	public void submit(State state, @Nonnull PoseStack pose, @Nonnull SubmitNodeCollector collector, @Nonnull CameraRenderState camera) {
		BlockEntity be = state.blockEntity;
		if (be == null) return;
		int light = state.lightCoords;
		int overlay = OverlayTexture.NO_OVERLAY;
		Block block = be.getBlockState().getBlock();
		if (!(block instanceof LogisticsSolidBlock)) return;
		LogisticsSolidBlock.Type type = ((LogisticsSolidBlock) block).getType();
		if (!(be instanceof LogisticsSolidTileEntity) || be.getLevel() == null) {
			renderSolid(type, pose, collector, light, overlay);
			return;
		}
		LogisticsSolidTileEntity tile = (LogisticsSolidTileEntity) be;

		if (!SimpleServiceLocator.cclProxy.isActivated()) return;
		if (!(SimpleServiceLocator.cclProxy.getRenderState() instanceof LPRenderStateImpl)) return;
		if (LogisticsNewSolidBlockWorldRenderer.block == null
				|| LogisticsNewSolidBlockWorldRenderer.block.isEmpty()) return;

		TextureTransformation icon = getIcon(type, tile.isActive());
		if (icon == null) return;

		LogisticsNewSolidBlockWorldRenderer.BlockRotation rotation =
				LogisticsNewSolidBlockWorldRenderer.BlockRotation.getRotation(tile.getRotation());
		if (rotation == null) rotation = LogisticsNewSolidBlockWorldRenderer.BlockRotation.ZERO;

		IModel3D body = LogisticsNewSolidBlockWorldRenderer.block.get(rotation);
		java.util.List<IModel3D> plates = new java.util.ArrayList<>();

		// LP1 hid the cover plates on sides where an adjacent LP pipe connects into this
		// block, so the pipe visually enters the machine.
		DoubleCoordinates pos = new DoubleCoordinates(tile);
		for (LogisticsNewSolidBlockWorldRenderer.CoverSides side :
				LogisticsNewSolidBlockWorldRenderer.CoverSides.values()) {
			boolean renderPlate = true;
			DoubleCoordinates newPos = CoordinateUtils.sum(pos, side.getDir(rotation));
			BlockEntity sideTile = newPos.getTileEntity(tile.getLevel());
			if (sideTile instanceof LogisticsTileGenericPipe) {
				LogisticsTileGenericPipe tilePipe = (LogisticsTileGenericPipe) sideTile;
				if (tilePipe.renderState != null
						&& tilePipe.renderState.pipeConnectionMatrix.isConnected(side.getDir(rotation).getOpposite())) {
					renderPlate = false;
				}
			}
			if (renderPlate) {
				Map<LogisticsNewSolidBlockWorldRenderer.BlockRotation, IModel3D> outer =
						LogisticsNewSolidBlockWorldRenderer.texturePlate_Outer.get(side);
				Map<LogisticsNewSolidBlockWorldRenderer.BlockRotation, IModel3D> inner =
						LogisticsNewSolidBlockWorldRenderer.texturePlate_Inner.get(side);
				if (outer != null && outer.get(rotation) != null) {
					plates.add(outer.get(rotation));
				}
				if (inner != null && inner.get(rotation) != null) {
					plates.add(inner.get(rotation));
				}
			}
		}
		LogisticsRenderPipe.submitGeometry(collector, pose, LogisticsRenderPipe.ATLAS_TYPE, light, overlay, () -> {
			if (body != null) {
				body.render(icon);
			}
			plates.forEach(plate -> plate.render(icon));
		});
	}

	/** Shared draw path used by both the in-world BER and the item BEWLR. */
	public static void renderSolid(LogisticsSolidBlock.Type type, PoseStack pose,
			SubmitNodeCollector collector, int light, int overlay) {
		if (!SimpleServiceLocator.cclProxy.isActivated()) return;
		if (!(SimpleServiceLocator.cclProxy.getRenderState() instanceof LPRenderStateImpl)) return;
		if (LogisticsNewSolidBlockWorldRenderer.block == null
				|| LogisticsNewSolidBlockWorldRenderer.block.isEmpty()) return;

		TextureTransformation icon = getIcon(type);
		if (icon == null) return;
		LogisticsRenderPipe.submitGeometry(collector, pose, LogisticsRenderPipe.ATLAS_TYPE, light, overlay, () -> renderSolidModels(type, icon));
	}

	private static void renderSolidModels(LogisticsSolidBlock.Type type, TextureTransformation icon) {

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
	}
}
