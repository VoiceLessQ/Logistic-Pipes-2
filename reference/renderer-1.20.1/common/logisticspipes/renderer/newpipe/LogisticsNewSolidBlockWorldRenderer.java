package logisticspipes.renderer.newpipe;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.core.Direction;

import lombok.Getter;

import logisticspipes.LogisticsPipes;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.object3d.interfaces.IModel3D;
import logisticspipes.proxy.object3d.operation.LPRotation;
import logisticspipes.proxy.object3d.operation.LPScale;
import logisticspipes.proxy.object3d.operation.LPTranslation;
import logisticspipes.proxy.object3d.operation.LPUVScale;

public class LogisticsNewSolidBlockWorldRenderer {

	public enum CoverSides {
		DOWN(Direction.DOWN, "D"),
		NORTH(Direction.NORTH, "N"),
		SOUTH(Direction.SOUTH, "S"),
		WEST(Direction.WEST, "W"),
		EAST(Direction.EAST, "E");

		private Direction dir;
		@Getter
		private String letter;

		CoverSides(Direction dir, String letter) {
			this.dir = dir;
			this.letter = letter;
		}

		public Direction getDir(BlockRotation rot) {
			Direction result = dir;
			if (result != Direction.DOWN) {
				switch (rot.getInteger()) {
					case 0:
						result = result.getClockWise();
					case 3:
						result = result.getClockWise();
					case 1:
						result = result.getClockWise();
					case 2:
				}
			}
			return result;
		}
	}

	public enum BlockRotation {
		ZERO(0),
		ONE(1),
		TWO(2),
		THREE(3);

		@Getter
		private int integer;

		BlockRotation(int rot) {
			integer = rot;
		}

		static BlockRotation getRotation(int from) {
			for (BlockRotation rot : BlockRotation.values()) {
				if (rot.getInteger() == from) {
					return rot;
				}
			}
			return null;
		}
	}

	public static Map<BlockRotation, IModel3D> block = new HashMap<>();
	public static Map<CoverSides, Map<BlockRotation, IModel3D>> texturePlate_Inner = new HashMap<>();
	public static Map<CoverSides, Map<BlockRotation, IModel3D>> texturePlate_Outer = new HashMap<>();

	public static void loadModels() {
		if (!SimpleServiceLocator.cclProxy.isActivated()) return;
		try {
			Map<String, IModel3D> blockPartModels = SimpleServiceLocator.cclProxy.parseObjModels(LogisticsPipes.class.getResourceAsStream("/logisticspipes/models/BlockModel_result.obj"), 7, new LPScale(1 / 100f));

			LogisticsNewSolidBlockWorldRenderer.block = null;
			for (Entry<String, IModel3D> entry : blockPartModels.entrySet()) {
				if (entry.getKey().contains(" Block ")) {
					if (LogisticsNewSolidBlockWorldRenderer.block != null) {
						throw new UnsupportedOperationException();
					}
					LogisticsNewSolidBlockWorldRenderer.block = LogisticsNewSolidBlockWorldRenderer.computeRotated(entry.getValue().backfacedCopy().apply(new LPTranslation(0.0, 0.0, 1.0)));
				}
			}

			LogisticsNewSolidBlockWorldRenderer.texturePlate_Outer.clear();
			LogisticsNewSolidBlockWorldRenderer.texturePlate_Inner.clear();
			for (CoverSides side : CoverSides.values()) {
				String grp_Outer = "OutSide_" + side.getLetter();
				String grp_Inside = "Inside_" + side.getLetter();
				for (Entry<String, IModel3D> entry : blockPartModels.entrySet()) {
					if (entry.getKey().contains(" " + grp_Outer + " ")) {
						LogisticsNewSolidBlockWorldRenderer.texturePlate_Outer.put(side, LogisticsNewSolidBlockWorldRenderer.computeRotated(entry.getValue().backfacedCopy().apply(new LPTranslation(0.0, 0.0, 1.0))));
					}
					if (entry.getKey().contains(" " + grp_Inside + " ")) {
						LogisticsNewSolidBlockWorldRenderer.texturePlate_Inner.put(side, LogisticsNewSolidBlockWorldRenderer.computeRotated(entry.getValue().backfacedCopy().apply(new LPTranslation(0.0, 0.0, 1.0))));
					}
				}
				if (LogisticsNewSolidBlockWorldRenderer.texturePlate_Outer.get(side) == null) {
					throw new RuntimeException("Couldn't load OutSide " + side.name() + " (" + grp_Outer + ").");
				}
				if (LogisticsNewSolidBlockWorldRenderer.texturePlate_Inner.get(side) == null) {
					throw new RuntimeException("Couldn't load OutSide " + side.name() + " (" + grp_Outer + ").");
				}
			}

		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	private static Map<BlockRotation, IModel3D> computeRotated(IModel3D m) {
		// Scale V by 95/128 ≈ 0.742 rather than 96/128 = 0.750.
		// The solid-block textures are 128×128 with content in rows 0–95 and transparent
		// padding in rows 96–127.  After the V-flip in OBJParser the OBJ v=0 vertex maps
		// to v_mc = 1.0, and scale × 1.0 must land inside row 95, not on the row-95/96
		// boundary (0.75) where bilinear filtering mixes in the transparent row 96 and
		// produces A ≈ 127 — just below the cutout alpha threshold — making those faces
		// render as transparent / black.
		m = m.apply(new LPUVScale(1, 0.742));
		Map<BlockRotation, IModel3D> map = new HashMap<>();
		for (BlockRotation rot : BlockRotation.values()) {
			IModel3D model = m.copy();
			switch (rot.getInteger()) {
				case 0:
					model = model.apply(LPRotation.sideOrientation(0, 3));
					model = model.apply(new LPTranslation(0, 0, 1));
					break;
				case 1:
					model = model.apply(LPRotation.sideOrientation(0, 1));
					model = model.apply(new LPTranslation(1, 0, 0));
					break;
				case 2:
					break;
				case 3:
					model = model.apply(LPRotation.sideOrientation(0, 2));
					model = model.apply(new LPTranslation(1, 0, 1));
					break;
			}
			model.computeNormals();
			model.computeStandardLighting();
			map.put(rot, model);
		}
		return map;
	}
/*
	public void renderWorldBlock(BlockGetter world, LogisticsSolidTileEntity blockTile, Object renderer, int x, int y, int z) { // renderer was RenderBlocks, removed in 1.20.1
		// TODO: rendering deferred — Tessellator tess = Tessellator.instance;
		SimpleServiceLocator.cclProxy.getRenderState().reset();
		SimpleServiceLocator.cclProxy.getRenderState().setUseNormals(true);
		SimpleServiceLocator.cclProxy.getRenderState().setAlphaOverride(0xff);

		BlockRotation rotation = BlockRotation.ZERO;
		int brightness = 0;
		IIconTransformation icon;
		if(blockTile != null) {
			BlockRotation.getRotation(blockTile.getRotation());
			brightness = new DoubleCoordinates(blockTile).getBlock(world).getMixedBrightnessForBlock(world, blockTile.xCoord, blockTile.yCoord, blockTile.zCoord);
			icon = SimpleServiceLocator.cclProxy.createIconTransformer(LogisticsSolidBlock.getNewIcon(world, blockTile.xCoord, blockTile.yCoord, blockTile.zCoord));
		} else {
			brightness = LogisticsPipes.LogisticsSolidBlock.getMixedBrightnessForBlock(world, x, y, z);
			icon = SimpleServiceLocator.cclProxy.createIconTransformer(LogisticsSolidBlock.getNewIcon(world, x, y, z));
		}


		tess.setColorOpaque_F(1F, 1F, 1F);
		tess.setBrightness(brightness);

		//Draw
		LogisticsNewSolidBlockWorldRenderer.block.get(rotation).render(new LPTranslation(x, y, z), icon);
		if(blockTile != null) {
			DoubleCoordinates pos = new DoubleCoordinates(blockTile);
			for (CoverSides side : CoverSides.values()) {
				boolean render = true;
				DoubleCoordinates newPos = CoordinateUtils.sum(pos, side.getDir(rotation));
				BlockEntity sideTile = newPos.getBlockEntity(blockTile.getworld());
				if (sideTile instanceof LogisticsTileGenericPipe) {
					LogisticsTileGenericPipe tilePipe = (LogisticsTileGenericPipe) sideTile;
					if (tilePipe.renderState.pipeConnectionMatrix.isConnected(side.getDir(rotation).getOpposite())) {
						render = false;
					}
				}
				if (render) {
					LogisticsNewSolidBlockWorldRenderer.texturePlate_Outer.get(side).get(rotation).render(new LPTranslation(x, y, z), icon);
					LogisticsNewSolidBlockWorldRenderer.texturePlate_Inner.get(side).get(rotation).render(new LPTranslation(x, y, z), icon);
				}
			}
		}

	}

	public void renderInventoryBlock(Block block2, int metadata) {
		// TODO: glPushAttrib removed //don't break other mods' guis when holding a pipe
		//force transparency
		// TODO: glBlendFunc → RenderSystem.blendFunc()
		RenderSystem.enableBlend();

		// TODO: GL11.glTranslatef → poseStack.translate(-0.5F, -0.5F, -0.5F)
		Block block = LogisticsPipes.LogisticsPipeBlock;
		// TODO: rendering deferred — Tessellator tess = Tessellator.instance;

		BlockRotation rotation = BlockRotation.ZERO;

		tess.startDrawingQuads();

		TextureTransformation icon = SimpleServiceLocator.cclProxy.createIconTransformer(LogisticsSolidBlock.getNewIcon(metadata));

		//Draw
		LogisticsNewSolidBlockWorldRenderer.block.get(rotation).render(icon);
		if(metadata != LogisticsSolidBlock.LOGISTICS_BLOCK_FRAME) {
			for (CoverSides side : CoverSides.values()) {
				LogisticsNewSolidBlockWorldRenderer.texturePlate_Outer.get(side).get(rotation).render(icon);
			}
		}
		tess.draw();
		block.setBlockBounds(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 1.0F);

		// TODO: glPopAttrib removed // nicely leave the rendering how it was
	}*/

}
