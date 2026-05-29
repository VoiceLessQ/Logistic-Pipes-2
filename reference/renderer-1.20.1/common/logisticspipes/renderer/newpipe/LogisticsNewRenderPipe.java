package logisticspipes.renderer.newpipe;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nonnull;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.Level;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.google.common.collect.Lists;
import lombok.AllArgsConstructor;
import lombok.Data;


import logisticspipes.LPConstants;
import logisticspipes.LogisticsPipes;
import logisticspipes.interfaces.ITubeOrientation;
import logisticspipes.pipefxhandlers.EntityModelFX;
import logisticspipes.pipes.PipeItemsBasicLogistics;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.object3d.interfaces.I3DOperation;
import logisticspipes.proxy.object3d.interfaces.IBounds;
import logisticspipes.proxy.object3d.interfaces.IModel3D;
import logisticspipes.proxy.object3d.interfaces.IRenderState;
import logisticspipes.proxy.object3d.interfaces.IVec3;
import logisticspipes.proxy.object3d.interfaces.TextureTransformation;
import logisticspipes.proxy.object3d.operation.LPColourMultiplier;
import logisticspipes.proxy.object3d.operation.LPScale;
import logisticspipes.proxy.object3d.operation.LPTranslation;
import logisticspipes.proxy.object3d.operation.LPUVScale;
import logisticspipes.proxy.object3d.operation.LPUVTransformationList;
import logisticspipes.proxy.object3d.operation.LPUVTranslation;
import logisticspipes.renderer.state.PipeRenderState;
import logisticspipes.textures.Textures;
import logisticspipes.utils.tuples.Quartet;
import network.rs485.logisticspipes.config.ClientConfiguration;
import network.rs485.logisticspipes.world.CoordinateUtils;
import network.rs485.logisticspipes.world.DoubleCoordinates;

public class LogisticsNewRenderPipe implements IHighlightPlacementRenderer {

	enum Edge {
		Upper_North(Direction.UP, Direction.NORTH),
		Upper_South(Direction.UP, Direction.SOUTH),
		Upper_East(Direction.UP, Direction.EAST),
		Upper_West(Direction.UP, Direction.WEST),
		Lower_North(Direction.DOWN, Direction.NORTH),
		Lower_South(Direction.DOWN, Direction.SOUTH),
		Lower_East(Direction.DOWN, Direction.EAST),
		Lower_West(Direction.DOWN, Direction.WEST),
		Middle_North_West(Direction.NORTH, Direction.WEST),
		Middle_North_East(Direction.NORTH, Direction.EAST),
		Lower_South_East(Direction.SOUTH, Direction.EAST),
		Lower_South_West(Direction.SOUTH, Direction.WEST);

		final Direction part1;
		final Direction part2;

		Edge(Direction part1, Direction part2) {
			this.part1 = part1;
			this.part2 = part2;
		}
	}

	enum UpDown {
		UP("U", Direction.UP),
		DOWN("D", Direction.DOWN);

		final String s;
		final Direction dir;

		UpDown(String s, Direction dir) {
			this.s = s;
			this.dir = dir;
		}
	}

	enum NorthSouth {
		NORTH("N", Direction.NORTH),
		SOUTH("S", Direction.SOUTH);

		final String s;
		final Direction dir;

		NorthSouth(String s, Direction dir) {
			this.s = s;
			this.dir = dir;
		}
	}

	enum EastWest {
		EAST("E", Direction.EAST),
		WEST("W", Direction.WEST);

		final String s;
		final Direction dir;

		EastWest(String s, Direction dir) {
			this.s = s;
			this.dir = dir;
		}
	}

	enum Corner {
		UP_NORTH_WEST(UpDown.UP, NorthSouth.NORTH, EastWest.WEST),
		UP_NORTH_EAST(UpDown.UP, NorthSouth.NORTH, EastWest.EAST),
		UP_SOUTH_WEST(UpDown.UP, NorthSouth.SOUTH, EastWest.WEST),
		UP_SOUTH_EAST(UpDown.UP, NorthSouth.SOUTH, EastWest.EAST),
		DOWN_NORTH_WEST(UpDown.DOWN, NorthSouth.NORTH, EastWest.WEST),
		DOWN_NORTH_EAST(UpDown.DOWN, NorthSouth.NORTH, EastWest.EAST),
		DOWN_SOUTH_WEST(UpDown.DOWN, NorthSouth.SOUTH, EastWest.WEST),
		DOWN_SOUTH_EAST(UpDown.DOWN, NorthSouth.SOUTH, EastWest.EAST);

		final UpDown ud;
		final NorthSouth ns;
		final EastWest ew;

		Corner(UpDown ud, NorthSouth ns, EastWest ew) {
			this.ud = ud;
			this.ns = ns;
			this.ew = ew;
		}
	}

	enum Turn {
		NORTH_SOUTH(Direction.NORTH, Direction.SOUTH),
		EAST_WEST(Direction.EAST, Direction.WEST),
		UP_DOWN(Direction.UP, Direction.DOWN);

		final Direction dir1;
		final Direction dir2;

		Turn(Direction dir1, Direction dir2) {
			this.dir1 = dir1;
			this.dir2 = dir2;
		}
	}

	enum PipeTurnCorner {
		UP_NORTH_WEST_TURN_NORTH_SOUTH(Corner.UP_NORTH_WEST, Turn.NORTH_SOUTH, 1),
		UP_NORTH_WEST_TURN_EAST_WEST(Corner.UP_NORTH_WEST, Turn.EAST_WEST, 14),
		UP_NORTH_WEST_TURN_UP_DOWN(Corner.UP_NORTH_WEST, Turn.UP_DOWN, 23),
		UP_NORTH_EAST_TURN_NORTH_SOUTH(Corner.UP_NORTH_EAST, Turn.NORTH_SOUTH, 2),
		UP_NORTH_EAST_TURN_EAST_WEST(Corner.UP_NORTH_EAST, Turn.EAST_WEST, 9),
		UP_NORTH_EAST_TURN_UP_DOWN(Corner.UP_NORTH_EAST, Turn.UP_DOWN, 22),
		UP_SOUTH_WEST_TURN_NORTH_SOUTH(Corner.UP_SOUTH_WEST, Turn.NORTH_SOUTH, 6),
		UP_SOUTH_WEST_TURN_EAST_WEST(Corner.UP_SOUTH_WEST, Turn.EAST_WEST, 13),
		UP_SOUTH_WEST_TURN_UP_DOWN(Corner.UP_SOUTH_WEST, Turn.UP_DOWN, 24),
		UP_SOUTH_EAST_TURN_NORTH_SOUTH(Corner.UP_SOUTH_EAST, Turn.NORTH_SOUTH, 5),
		UP_SOUTH_EAST_TURN_EAST_WEST(Corner.UP_SOUTH_EAST, Turn.EAST_WEST, 10),
		UP_SOUTH_EAST_TURN_UP_DOWN(Corner.UP_SOUTH_EAST, Turn.UP_DOWN, 21),
		DOWN_NORTH_WEST_TURN_NORTH_SOUTH(Corner.DOWN_NORTH_WEST, Turn.NORTH_SOUTH, 4),
		DOWN_NORTH_WEST_TURN_EAST_WEST(Corner.DOWN_NORTH_WEST, Turn.EAST_WEST, 15),
		DOWN_NORTH_WEST_TURN_UP_DOWN(Corner.DOWN_NORTH_WEST, Turn.UP_DOWN, 20),
		DOWN_NORTH_EAST_TURN_NORTH_SOUTH(Corner.DOWN_NORTH_EAST, Turn.NORTH_SOUTH, 3),
		DOWN_NORTH_EAST_TURN_EAST_WEST(Corner.DOWN_NORTH_EAST, Turn.EAST_WEST, 12),
		DOWN_NORTH_EAST_TURN_UP_DOWN(Corner.DOWN_NORTH_EAST, Turn.UP_DOWN, 17),
		DOWN_SOUTH_WEST_TURN_NORTH_SOUTH(Corner.DOWN_SOUTH_WEST, Turn.NORTH_SOUTH, 7),
		DOWN_SOUTH_WEST_TURN_EAST_WEST(Corner.DOWN_SOUTH_WEST, Turn.EAST_WEST, 16),
		DOWN_SOUTH_WEST_TURN_UP_DOWN(Corner.DOWN_SOUTH_WEST, Turn.UP_DOWN, 19),
		DOWN_SOUTH_EAST_TURN_NORTH_SOUTH(Corner.DOWN_SOUTH_EAST, Turn.NORTH_SOUTH, 8),
		DOWN_SOUTH_EAST_TURN_EAST_WEST(Corner.DOWN_SOUTH_EAST, Turn.EAST_WEST, 11),
		DOWN_SOUTH_EAST_TURN_UP_DOWN(Corner.DOWN_SOUTH_EAST, Turn.UP_DOWN, 18);

		final Corner corner;
		final Turn turn;
		final int number;

		PipeTurnCorner(Corner corner, Turn turn, int number) {
			this.corner = corner;
			this.turn = turn;
			this.number = number;
		}

		public Direction getPointer() {
			List<Direction> canidates = new ArrayList<>();
			canidates.add(corner.ew.dir);
			canidates.add(corner.ns.dir);
			canidates.add(corner.ud.dir);
			if (canidates.contains(turn.dir1)) {
				return turn.dir1;
			} else if (canidates.contains(turn.dir2)) {
				return turn.dir2;
			} else {
				throw new UnsupportedOperationException(name());
			}
		}
	}

	enum PipeSupportOri {
		UP_DOWN("U"),
		SIDE("S");

		final String s;

		PipeSupportOri(String s) {
			this.s = s;
		}
	}

	enum PipeSupport {
		UP_UP(Direction.UP, PipeSupportOri.UP_DOWN),
		UP_SIDE(Direction.UP, PipeSupportOri.SIDE),
		DOWN_UP(Direction.DOWN, PipeSupportOri.UP_DOWN),
		DOWN_SIDE(Direction.DOWN, PipeSupportOri.SIDE),
		NORTH_UP(Direction.NORTH, PipeSupportOri.UP_DOWN),
		NORTH_SIDE(Direction.NORTH, PipeSupportOri.SIDE),
		SOUTH_UP(Direction.SOUTH, PipeSupportOri.UP_DOWN),
		SOUTH_SIDE(Direction.SOUTH, PipeSupportOri.SIDE),
		EAST_UP(Direction.EAST, PipeSupportOri.UP_DOWN),
		EAST_SIDE(Direction.EAST, PipeSupportOri.SIDE),
		WEST_UP(Direction.WEST, PipeSupportOri.UP_DOWN),
		WEST_SIDE(Direction.WEST, PipeSupportOri.SIDE);

		PipeSupport(Direction dir, PipeSupportOri ori) {
			this.dir = dir;
			this.ori = ori;
		}

		final Direction dir;
		final PipeSupportOri ori;
	}

	enum PipeMount {
		UP_NORTH(Direction.UP, Direction.NORTH),
		UP_SOUTH(Direction.UP, Direction.SOUTH),
		UP_EAST(Direction.UP, Direction.EAST),
		UP_WEST(Direction.UP, Direction.WEST),
		DOWN_NORTH(Direction.DOWN, Direction.NORTH),
		DOWN_SOUTH(Direction.DOWN, Direction.SOUTH),
		DOWN_EAST(Direction.DOWN, Direction.EAST),
		DOWN_WEST(Direction.DOWN, Direction.WEST),
		NORTH_UP(Direction.NORTH, Direction.UP),
		NORTH_DOWN(Direction.NORTH, Direction.DOWN),
		NORTH_EAST(Direction.NORTH, Direction.EAST),
		NORTH_WEST(Direction.NORTH, Direction.WEST),
		SOUTH_UP(Direction.SOUTH, Direction.UP),
		SOUTH_DOWN(Direction.SOUTH, Direction.DOWN),
		SOUTH_EAST(Direction.SOUTH, Direction.EAST),
		SOUTH_WEST(Direction.SOUTH, Direction.WEST),
		EAST_UP(Direction.EAST, Direction.UP),
		EAST_DOWN(Direction.EAST, Direction.DOWN),
		EAST_NORTH(Direction.EAST, Direction.NORTH),
		EAST_SOUTH(Direction.EAST, Direction.SOUTH),
		WEST_UP(Direction.WEST, Direction.UP),
		WEST_DOWN(Direction.WEST, Direction.DOWN),
		WEST_NORTH(Direction.WEST, Direction.NORTH),
		WEST_SOUTH(Direction.WEST, Direction.SOUTH);

		Direction dir;
		Direction side;

		PipeMount(Direction dir, Direction side) {
			this.dir = dir;
			this.side = side;
		}
	}

	public static Map<Direction, List<IModel3D>> sideNormal = new HashMap<>();
	static Map<Direction, List<IModel3D>> sideBC = new HashMap<>();
	static Map<Edge, IModel3D> edges = new HashMap<>();
	static Map<Corner, List<IModel3D>> corners_M = new HashMap<>();
	static Map<Corner, List<IModel3D>> corners_I3 = new HashMap<>();
	static Map<PipeTurnCorner, IModel3D> corners_I = new HashMap<>();
	static Map<PipeSupport, IModel3D> supports = new HashMap<>();
	static Map<PipeTurnCorner, IModel3D> spacers = new HashMap<>();
	static Map<PipeMount, IModel3D> mounts = new HashMap<>();

	static Map<Direction, List<IModel3D>> texturePlate_Inner = new HashMap<>();
	static Map<Direction, List<IModel3D>> texturePlate_Outer = new HashMap<>();
	static Map<Direction, Quartet<List<IModel3D>, List<IModel3D>, List<IModel3D>, List<IModel3D>>> sideTexturePlate = new HashMap<>();
	static Map<PipeMount, List<IModel3D>> textureConnectorPlate = new HashMap<>();

	static Map<ScaleObject, IModel3D> scaleMap = new HashMap<>();

	@Data
	@AllArgsConstructor
	private static class ScaleObject {

		private final IModel3D original;
		private final double scale;
	}

	static IModel3D innerTransportBox;
	public static IModel3D highlight;

	private static final List<RenderEntry> pipeFrameRenderList = new ArrayList<>();

	public static TextureTransformation basicPipeTexture;
	public static TextureTransformation inactiveTexture;
	public static TextureTransformation glassCenterTexture;
	public static TextureTransformation innerBoxTexture;
	public static TextureTransformation statusTexture;
	public static TextureTransformation statusBCTexture;

	public static void loadModels() {
		if (!SimpleServiceLocator.cclProxy.isActivated()) return;
		try {
			Map<String, IModel3D> pipePartModels = SimpleServiceLocator.cclProxy.parseObjModels(LogisticsPipes.class.getResourceAsStream("/logisticspipes/models/PipeModel_moved.obj"), 7, new LPScale(1 / 100f));
			List<IModel3D> highlightList = new ArrayList<>();

			for (Direction dir : Direction.values()) {
				LogisticsNewRenderPipe.sideNormal.put(dir, new ArrayList<>());
				String grp = "Side_" + LogisticsNewRenderPipe.getDirAsString_Type1(dir);
				pipePartModels.entrySet().stream()
						.filter(entry -> entry.getKey().contains(" " + grp + " ") || entry.getKey().endsWith(" " + grp))
						.forEach(entry -> LogisticsNewRenderPipe.sideNormal.get(dir).add(LogisticsNewRenderPipe.compute(entry.getValue().backfacedCopy().apply(new LPTranslation(0.0, 0.0, 1.0)))));
				if (LogisticsNewRenderPipe.sideNormal.get(dir).size() != 4) {
					throw new RuntimeException("Couldn't load " + dir.name() + " (" + grp + "). Only loaded " + LogisticsNewRenderPipe.sideNormal.get(dir).size());
				}
			}

			for (Direction dir : Direction.values()) {
				LogisticsNewRenderPipe.sideBC.put(dir, new ArrayList<>());
				String grp = "Side_BC_" + LogisticsNewRenderPipe.getDirAsString_Type1(dir);
				pipePartModels.entrySet().stream()
						.filter(entry -> entry.getKey().contains(" " + grp + " ") || entry.getKey().endsWith(" " + grp))
						.forEach(entry -> LogisticsNewRenderPipe.sideBC.get(dir).add(LogisticsNewRenderPipe.compute(entry.getValue().backfacedCopy().apply(new LPTranslation(0.0, 0.0, 1.0)))));
				if (LogisticsNewRenderPipe.sideBC.get(dir).size() != 8) {
					throw new RuntimeException("Couldn't load " + dir.name() + " (" + grp + "). Only loaded " + LogisticsNewRenderPipe.sideBC.get(dir).size());
				}
			}

			for (Edge edge : Edge.values()) {
				String grp;
				if (edge.part1 == Direction.UP || edge.part1 == Direction.DOWN) {
					grp = "Edge_M_" + LogisticsNewRenderPipe.getDirAsString_Type1(edge.part1) + "_" + LogisticsNewRenderPipe.getDirAsString_Type1(edge.part2);
				} else {
					grp = "Edge_M_S_" + LogisticsNewRenderPipe.getDirAsString_Type1(edge.part1) + LogisticsNewRenderPipe.getDirAsString_Type1(edge.part2);
				}
				for (Entry<String, IModel3D> entry : pipePartModels.entrySet()) {
					if (entry.getKey().contains(" " + grp + " ") || entry.getKey().endsWith(" " + grp)) {
						LogisticsNewRenderPipe.edges.put(edge, LogisticsNewRenderPipe.compute(entry.getValue().backfacedCopy().apply(new LPTranslation(0.0, 0.0, 1.0))));
						break;
					}
				}
				if (LogisticsNewRenderPipe.edges.get(edge) == null) {
					throw new RuntimeException("Couldn't load " + edge.name() + " (" + grp + ")");
				}
				highlightList.add(LogisticsNewRenderPipe.edges.get(edge));
			}

			for (Corner corner : Corner.values()) {
				LogisticsNewRenderPipe.corners_M.put(corner, new ArrayList<>());
				String grp = "Corner_M_" + corner.ud.s + "_" + corner.ns.s + corner.ew.s;
				pipePartModels.entrySet().stream()
						.filter(entry -> entry.getKey().contains(" " + grp + " ") || entry.getKey().endsWith(" " + grp))
						.forEach(entry -> LogisticsNewRenderPipe.corners_M.get(corner).add(LogisticsNewRenderPipe.compute(entry.getValue().backfacedCopy().apply(new LPTranslation(0.0, 0.0, 1.0)))));
				if (LogisticsNewRenderPipe.corners_M.get(corner).size() != 2) {
					throw new RuntimeException("Couldn't load " + corner.name() + " (" + grp + "). Only loaded " + LogisticsNewRenderPipe.corners_M.get(corner).size());
				}
				highlightList.addAll(LogisticsNewRenderPipe.corners_M.get(corner));
			}

			for (Corner corner : Corner.values()) {
				LogisticsNewRenderPipe.corners_I3.put(corner, new ArrayList<>());
				String grp = "Corner_I3_" + corner.ud.s + "_" + corner.ns.s + corner.ew.s;
				pipePartModels.entrySet().stream()
						.filter(entry -> entry.getKey().contains(" " + grp + " ") || entry.getKey().endsWith(" " + grp))
						.forEach(entry -> LogisticsNewRenderPipe.corners_I3.get(corner).add(LogisticsNewRenderPipe.compute(entry.getValue().backfacedCopy().apply(new LPTranslation(0.0, 0.0, 1.0)))));
				if (LogisticsNewRenderPipe.corners_I3.get(corner).size() != 2) {
					throw new RuntimeException("Couldn't load " + corner.name() + " (" + grp + "). Only loaded " + LogisticsNewRenderPipe.corners_I3.get(corner).size());
				}
			}

			for (PipeSupport support : PipeSupport.values()) {
				String grp = "Support_" + LogisticsNewRenderPipe.getDirAsString_Type1(support.dir) + "_" + support.ori.s;
				for (Entry<String, IModel3D> entry : pipePartModels.entrySet()) {
					if (entry.getKey().contains(" " + grp + " ") || entry.getKey().endsWith(" " + grp)) {
						LogisticsNewRenderPipe.supports.put(support, LogisticsNewRenderPipe.compute(entry.getValue().backfacedCopy().apply(new LPTranslation(0.0, 0.0, 1.0))));
						break;
					}
				}
				if (LogisticsNewRenderPipe.supports.get(support) == null) {
					throw new RuntimeException("Couldn't load " + support.name() + " (" + grp + ")");
				}
			}

			for (PipeTurnCorner corner : PipeTurnCorner.values()) {
				String grp = "Corner_I_" + corner.corner.ud.s + "_" + corner.corner.ns.s + corner.corner.ew.s;
				for (Entry<String, IModel3D> entry : pipePartModels.entrySet()) {
					if (entry.getKey().contains(" " + grp)) {
						char c = ' ';
						if (!entry.getKey().endsWith(" " + grp)) {
							c = entry.getKey().charAt(entry.getKey().indexOf(" " + grp) + (" " + grp).length());
						}
						if (Character.isDigit(c)) {
							if (c == '2') {
								if (corner.turn != Turn.NORTH_SOUTH) {
									continue;
								}
							} else if (c == '1') {
								if (corner.turn != Turn.EAST_WEST) {
									continue;
								}
							} else {
								throw new UnsupportedOperationException();
							}
						} else {
							if (corner.turn != Turn.UP_DOWN) {
								continue;
							}
						}
						LogisticsNewRenderPipe.corners_I.put(corner, LogisticsNewRenderPipe.compute(entry.getValue().backfacedCopy().apply(new LPTranslation(0.0, 0.0, 1.0))));
						break;
					}
				}
				if (LogisticsNewRenderPipe.corners_I.get(corner) == null) {
					throw new RuntimeException("Couldn't load " + corner.name() + " (" + grp + ")");
				}
			}

			for (PipeTurnCorner corner : PipeTurnCorner.values()) {
				String grp = "Spacer" + corner.number;
				for (Entry<String, IModel3D> entry : pipePartModels.entrySet()) {
					if (entry.getKey().contains(" " + grp + " ") || entry.getKey().endsWith(" " + grp)) {
						LogisticsNewRenderPipe.spacers.put(corner, LogisticsNewRenderPipe.compute(entry.getValue().backfacedCopy().apply(new LPTranslation(0.0, 0.0, 1.0))));
						break;
					}
				}
				if (LogisticsNewRenderPipe.spacers.get(corner) == null) {
					throw new RuntimeException("Couldn't load " + corner.name() + " (" + grp + ")");
				}
			}

			for (PipeMount mount : PipeMount.values()) {
				String grp = "Mount_" + LogisticsNewRenderPipe.getDirAsString_Type1(mount.dir) + "_" + LogisticsNewRenderPipe.getDirAsString_Type1(mount.side);
				for (Entry<String, IModel3D> entry : pipePartModels.entrySet()) {
					if (entry.getKey().contains(" " + grp + " ") || entry.getKey().endsWith(" " + grp)) {
						LogisticsNewRenderPipe.mounts.put(mount, LogisticsNewRenderPipe.compute(entry.getValue().backfacedCopy().apply(new LPTranslation(0.0, 0.0, 1.0))));
						break;
					}
				}
				if (LogisticsNewRenderPipe.mounts.get(mount) == null) {
					throw new RuntimeException("Couldn't load " + mount.name() + " (" + grp + ")");
				}
			}

			for (Direction dir : Direction.values()) {
				LogisticsNewRenderPipe.texturePlate_Inner.put(dir, new ArrayList<>());
				String grp = "Inner_Plate_" + LogisticsNewRenderPipe.getDirAsString_Type1(dir);
				pipePartModels.entrySet().stream().filter(entry -> entry.getKey().contains(" " + grp))
						.forEach(entry -> LogisticsNewRenderPipe.texturePlate_Inner.get(dir).add(LogisticsNewRenderPipe.compute(entry.getValue().backfacedCopy().apply(new LPTranslation(0.0, 0.0, 1.0)))));
				if (LogisticsNewRenderPipe.texturePlate_Inner.get(dir).size() != 2) {
					throw new RuntimeException("Couldn't load " + dir.name() + " (" + grp + "). Only loaded " + LogisticsNewRenderPipe.texturePlate_Inner.get(dir).size());
				}
			}

			for (Direction dir : Direction.values()) {
				LogisticsNewRenderPipe.texturePlate_Outer.put(dir, new ArrayList<>());
				String grp = "Texture_Plate_" + LogisticsNewRenderPipe.getDirAsString_Type1(dir);
				pipePartModels.entrySet().stream().filter(entry -> entry.getKey().contains(" " + grp))
						.forEach(entry -> LogisticsNewRenderPipe.texturePlate_Outer.get(dir).add(LogisticsNewRenderPipe.compute(entry.getValue().backfacedCopy().apply(new LPTranslation(0.0, 0.0, 1.0)).apply(new LPTranslation(-0.5, -0.5, -0.5)).apply(new LPScale(1.001D)).apply(new LPTranslation(0.5, 0.5, 0.5)))));
				if (LogisticsNewRenderPipe.texturePlate_Outer.get(dir).size() != 2) {
					throw new RuntimeException("Couldn't load " + dir.name() + " (" + grp + "). Only loaded " + LogisticsNewRenderPipe.texturePlate_Outer.get(dir).size());
				}
			}

			for (Direction dir : Direction.values()) {
				LogisticsNewRenderPipe.sideTexturePlate.put(dir, new Quartet<>(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
				String grp = "Texture_Side_" + LogisticsNewRenderPipe.getDirAsString_Type1(dir);
				for (Entry<String, IModel3D> entry : pipePartModels.entrySet()) {
					if (entry.getKey().contains(" " + grp)) {
						IModel3D model = LogisticsNewRenderPipe.compute(entry.getValue().backfacedCopy().apply(new LPTranslation(0.0, 0.0, 1.0)));
						double sizeA = (model.bounds().max().x() - model.bounds().min().x()) + (model.bounds().max().y() - model.bounds().min().y()) + (model.bounds().max().z() - model.bounds().min().z());
						double dis = Math.pow(model.bounds().min().x() - 0.5D, 2) + Math.pow(model.bounds().min().y() - 0.5D, 2) + Math.pow(model.bounds().min().z() - 0.5D, 2);
						if (sizeA < 0.5D) {
							if ((dis > 0.22 && dis < 0.24) || (dis > 0.38 && dis < 0.40)) {
								LogisticsNewRenderPipe.sideTexturePlate.get(dir).getValue4().add(model);
							} else if ((dis < 0.2 && dis > 0.18) || (dis < 0.36 && dis > 0.34)) {
								LogisticsNewRenderPipe.sideTexturePlate.get(dir).getValue2().add(model);
							} else {
								throw new UnsupportedOperationException("Dis: " + dis);
							}
						} else {
							if ((dis > 0.22 && dis < 0.24) || (dis > 0.38 && dis < 0.40)) {
								LogisticsNewRenderPipe.sideTexturePlate.get(dir).getValue3().add(model);
							} else if ((dis < 0.2 && dis > 0.18) || (dis < 0.36 && dis > 0.34)) {
								LogisticsNewRenderPipe.sideTexturePlate.get(dir).getValue1().add(model);
							} else {
								throw new UnsupportedOperationException("Dis: " + dis);
							}
						}
					}
				}
				if (LogisticsNewRenderPipe.sideTexturePlate.get(dir).getValue1().size() != 8) {
					throw new RuntimeException("Couldn't load " + dir.name() + " (" + grp + "). Only loaded " + LogisticsNewRenderPipe.sideTexturePlate.get(dir).getValue1().size());
				}
				if (LogisticsNewRenderPipe.sideTexturePlate.get(dir).getValue2().size() != 8) {
					throw new RuntimeException("Couldn't load " + dir.name() + " (" + grp + "). Only loaded " + LogisticsNewRenderPipe.sideTexturePlate.get(dir).getValue2().size());
				}
				if (LogisticsNewRenderPipe.sideTexturePlate.get(dir).getValue3().size() != 8) {
					throw new RuntimeException("Couldn't load " + dir.name() + " (" + grp + "). Only loaded " + LogisticsNewRenderPipe.sideTexturePlate.get(dir).getValue3().size());
				}
				if (LogisticsNewRenderPipe.sideTexturePlate.get(dir).getValue4().size() != 8) {
					throw new RuntimeException("Couldn't load " + dir.name() + " (" + grp + "). Only loaded " + LogisticsNewRenderPipe.sideTexturePlate.get(dir).getValue4().size());
				}
			}

			for (PipeMount mount : PipeMount.values()) {
				LogisticsNewRenderPipe.textureConnectorPlate.put(mount, new ArrayList<>());
				String grp = "Texture_Connector_" + LogisticsNewRenderPipe.getDirAsString_Type1(mount.dir) + "_" + LogisticsNewRenderPipe.getDirAsString_Type1(mount.side);
				pipePartModels.entrySet().stream()
						.filter(entry -> entry.getKey().contains(" " + grp + " ") || entry.getKey().endsWith(" " + grp))
						.forEach(entry -> LogisticsNewRenderPipe.textureConnectorPlate.get(mount).add(LogisticsNewRenderPipe.compute(entry.getValue().backfacedCopy().apply(new LPTranslation(0.0, 0.0, 1.0)))));
				if (LogisticsNewRenderPipe.textureConnectorPlate.get(mount).size() != 4) {
					throw new RuntimeException("Couldn't load " + mount.name() + " (" + grp + "). Only loaded " + LogisticsNewRenderPipe.textureConnectorPlate.get(mount).size());
				}
			}

			highlight = SimpleServiceLocator.cclProxy.combine(highlightList);

			pipePartModels = SimpleServiceLocator.cclProxy.parseObjModels(LogisticsPipes.class.getResourceAsStream("/logisticspipes/models/PipeModel_Transport_Box.obj"), 7, new LPScale(1 / 100f));

			LogisticsNewRenderPipe.innerTransportBox = LogisticsNewRenderPipe.compute(pipePartModels.get("InnerTransportBox").backfacedCopy().apply(new LPTranslation(0.0, 0.0, 1.0)).apply(new LPTranslation(-0.5, -0.5, -0.5)).apply(new LPScale(0.99D)).apply(new LPTranslation(0.5, 0.5, 0.5)));

		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	private static String getDirAsString_Type1(Direction dir) {
		switch (dir) {
			case NORTH:
				return "N";
			case SOUTH:
				return "S";
			case EAST:
				return "E";
			case WEST:
				return "W";
			case UP:
				return "U";
			case DOWN:
				return "D";
			default:
				return "UNKNWON";
		}
	}

	public static IModel3D compute(IModel3D m) {
		m.computeNormals();
		return m;
	}

	public static void registerTextures(TextureAtlas iconRegister) {
		// Texture registration is handled by TextureRegistrar via TextureStitchEvent.Post.
	}

	private ClientConfiguration config = LogisticsPipes.getClientPlayerConfig();

	public void renderTileEntityAt(LogisticsTileGenericPipe pipeTile, double x, double y, double z, float partialTickTime, double distance) {
		boolean inHand = false;
		if (pipeTile == null) return;
		if (pipeTile.pipe == null) return;
		if (pipeTile.pipe.isPipeBlock()) return;
		PipeRenderState renderState = pipeTile.renderState;

		if (renderState.renderLists != null && renderState.renderLists.values().stream().anyMatch(GLRenderList::isInvalid)) {
			renderState.renderLists = null;
		}

		if (renderState.renderLists == null) {
			renderState.renderLists = new HashMap<>();
		}

		if (distance > config.getRenderPipeDistance() * config.getRenderPipeDistance()) {
			/*if (config.isUseFallbackRenderer()) {
				renderState.forceRenderOldPipe = true;
			}*/
		} else {
			renderState.forceRenderOldPipe = false;
			boolean recalculateList = checkAndCalculateRenderCache(pipeTile);
			renderList(x, y, z, renderState.renderLists, renderState.cachedRenderer, recalculateList);
			if (recalculateList) {
				net.minecraft.core.BlockPos bpos = pipeTile.getBlockPos();
				net.minecraft.client.Minecraft.getInstance().levelRenderer.setBlocksDirty(
						bpos.getX(), bpos.getY(), bpos.getZ(),
						bpos.getX(), bpos.getY(), bpos.getZ());
			}
		}
	}

	public static boolean checkAndCalculateRenderCache(LogisticsTileGenericPipe pipeTile) {
		PipeRenderState renderState = pipeTile.renderState;

		if (renderState.cachedRenderIndex != MainProxy.proxy.getRenderIndex()) {
			renderState.clearRenderCaches();
		}

		if (renderState.cachedRenderer == null) {
			ArrayList<RenderEntry> objectsToRender = new ArrayList<>();

			if (pipeTile.pipe != null && pipeTile.pipe.actAsNormalPipe()) {
				fillObjectsToRenderList(objectsToRender, pipeTile, renderState);
			}
			if (pipeTile.pipe != null && pipeTile.pipe.getSpecialRenderer() != null) {
				pipeTile.pipe.getSpecialRenderer().renderToList(pipeTile.pipe, objectsToRender);
			}

			renderState.cachedRenderIndex = MainProxy.proxy.getRenderIndex();
			renderState.cachedRenderer = Collections.unmodifiableList(objectsToRender);
			return true;
		}
		return false;
	}

	private static void renderList(double x, double y, double z, Map<ResourceLocation, GLRenderList> renderLists, List<RenderEntry> cachedRenderer, boolean recalculateList) {
		// 1.20.1 rewrite: the old 1.12.2 code cached geometry into GL display lists
		// (GLRenderList) keyed by texture. Display lists are removed in 1.20.1 and
		// the modern pipeline already batches per-frame through MultiBufferSource,
		// so the caching layer is both impossible and unnecessary. We now emit
		// every RenderEntry directly through the VertexConsumer bound on
		// LPRenderStateImpl by the outer BlockEntityRenderer.
		//
		// All pipe textures live on the block atlas (LOCATION_BLOCKS) and resolve
		// through the TextureTransformation operation each entry carries, so we
		// don't need per-texture RenderType selection here — the bound solid
		// buffer is correct for every entry.
		IRenderState rs = SimpleServiceLocator.cclProxy.getRenderState();
		if (rs == null) return;
		rs.reset();
		for (RenderEntry entry : cachedRenderer) {
			entry.getModel().render(entry.getOperations());
		}
		rs.draw();
	}

	private static void fillObjectsToRenderList(List<RenderEntry> objectsToRender, LogisticsTileGenericPipe pipeTile, PipeRenderState renderState) {
		fillObjectsToRenderList(objectsToRender, pipeTile.pipe, pipeTile, renderState);
	}

	/** Tile-free overload for item (BEWLR) rendering. Pass the dummyPipe directly;
	 *  neighbour-dependent adjustments fall back to unit bounds when no tile exists. */
	public static void fillObjectsToRenderList(List<RenderEntry> objectsToRender, CoreUnroutedPipe pipe, PipeRenderState renderState) {
		fillObjectsToRenderList(objectsToRender, pipe, null, renderState);
	}

	private static void fillObjectsToRenderList(List<RenderEntry> objectsToRender, CoreUnroutedPipe pipe, LogisticsTileGenericPipe pipeTile, PipeRenderState renderState) {
		List<Edge> edgesToRender = new ArrayList<>(Arrays.asList(Edge.values()));
		Map<Corner, Integer> connectionAtCorner = new HashMap<>();
		List<PipeMount> mountCanidates = new ArrayList<>(Arrays.asList(PipeMount.values()));

		int connectionCount = 0;

		for (Direction dir : Direction.values()) {
			if (renderState.pipeConnectionMatrix.isConnected(dir) || pipe.hasSpecialPipeEndAt(dir)) {
				connectionCount++;
				if (renderState.pipeConnectionMatrix.isTDConnected(dir) || renderState.pipeConnectionMatrix.isBCConnected(dir)) {
					I3DOperation[] texture = new I3DOperation[] { LogisticsNewRenderPipe.basicPipeTexture };
					if (renderState.textureMatrix.isRouted()) {
						if (renderState.textureMatrix.isRoutedInDir(dir)) {
							if (renderState.textureMatrix.isSubPowerInDir(dir)) {
								texture = new I3DOperation[] { new LPUVTransformationList(new LPUVTranslation(0, +23F / 100), LogisticsNewRenderPipe.statusBCTexture) };
							} else {
								texture = new I3DOperation[] { LogisticsNewRenderPipe.statusBCTexture };
							}
						} else {
							texture = new I3DOperation[] { new LPUVTransformationList(new LPUVTranslation(0, -23F / 100), LogisticsNewRenderPipe.statusBCTexture) };
						}
					}
					for (IModel3D model : LogisticsNewRenderPipe.sideBC.get(dir)) {
						objectsToRender.add(new RenderEntry(model, texture));
					}
				} else if (!pipe.hasSpecialPipeEndAt(dir)) {
					I3DOperation[] texture = new I3DOperation[] { LogisticsNewRenderPipe.basicPipeTexture };
					if (renderState.textureMatrix.isRouted()) {
						if (renderState.textureMatrix.isRoutedInDir(dir)) {
							if (renderState.textureMatrix.isSubPowerInDir(dir)) {
								texture = new I3DOperation[] { new LPUVTransformationList(new LPUVTranslation(-2.5F / 10, 0), LogisticsNewRenderPipe.statusTexture) };
							} else {
								texture = new I3DOperation[] { LogisticsNewRenderPipe.statusTexture };
							}
						} else {
							if (renderState.textureMatrix.isHasPowerUpgrade()) {
								if (renderState.textureMatrix.getPointedOrientation() == dir) {
									texture = new I3DOperation[] { new LPUVTransformationList(new LPUVTranslation(+2.5F / 10, 0), LogisticsNewRenderPipe.statusTexture) };
								} else {
									texture = new I3DOperation[] { new LPUVTransformationList(new LPUVTranslation(-2.5F / 10, 37F / 100), LogisticsNewRenderPipe.statusTexture) };
								}
							} else {
								if (renderState.textureMatrix.getPointedOrientation() == dir) {
									texture = new I3DOperation[] { new LPUVTransformationList(new LPUVTranslation(+2.5F / 10, 37F / 100), LogisticsNewRenderPipe.statusTexture) };
								} else {
									texture = new I3DOperation[] { new LPUVTransformationList(new LPUVTranslation(0, 37F / 100), LogisticsNewRenderPipe.statusTexture) };
								}
							}
						}
					}
					for (IModel3D model : LogisticsNewRenderPipe.sideNormal.get(dir)) {
						double[] bounds = { 0, 0, 0, 1, 1, 1 }; // 1.20.1: Block.FULL_BLOCK_AABB removed; default to unit cube
						if (pipeTile != null && pipeTile.getWorld() != null) { //This can be null in some cases now !!!
							DoubleCoordinates coords = CoordinateUtils.add(new DoubleCoordinates((BlockEntity) pipeTile), dir);
							BlockState blockState = coords.getBlockState(pipeTile.getWorld());
							net.minecraft.world.phys.shapes.VoxelShape shape = blockState.getCollisionShape(pipeTile.getWorld(), coords.getBlockPos());
							AABB bb = shape.isEmpty() ? null : shape.bounds();
							if (bb == null) bb = new AABB(0, 0, 0, 1, 1, 1); // 1.20.1: Block.FULL_BLOCK_AABB removed
							bounds = new double[] { bb.minY, bb.minZ, bb.minX, bb.maxY, bb.maxZ, bb.maxX };
						}
						double bound = bounds[dir.ordinal() / 2 + (dir.ordinal() % 2 == 0 ? 3 : 0)];
						ScaleObject key = new ScaleObject(model, bound);
						IModel3D model2 = LogisticsNewRenderPipe.scaleMap.get(key);
						if (model2 == null) {
							model2 = model.copy();
							IVec3 min = model2.bounds().min();
							model2 = model2.apply(new LPTranslation(min).inverse());
							double toAdd;
							if (dir.ordinal() % 2 == 1) {
								toAdd = 1 + (bound / LPConstants.PIPE_MIN_POS);
								model2 = model2.apply(new LPScale(dir.getNormal().getX() != 0 ? toAdd : 1, dir.getNormal().getY() != 0 ? toAdd : 1, dir.getNormal().getZ() != 0 ? toAdd : 1));
							} else {
								bound = 1 - bound;
								toAdd = 1 + (bound / LPConstants.PIPE_MIN_POS);
								model2 = model2.apply(new LPScale(dir.getNormal().getX() != 0 ? toAdd : 1, dir.getNormal().getY() != 0 ? toAdd : 1, dir.getNormal().getZ() != 0 ? toAdd : 1));
								model2 = model2.apply(new LPTranslation(dir.getNormal().getX() * bound, dir.getNormal().getY() * bound, dir.getNormal().getZ() * bound));
							}
							model2 = model2.apply(new LPTranslation(min));
							LogisticsNewRenderPipe.scaleMap.put(key, model2);
						}
						objectsToRender.add(new RenderEntry(model2, texture));
					}
				}
				for (Edge edge : Edge.values()) {
					if (edge.part1 == dir || edge.part2 == dir) {
						edgesToRender.remove(edge);
						for (PipeMount mount : PipeMount.values()) {
							if ((mount.dir == edge.part1 && mount.side == edge.part2) || (mount.dir == edge.part2 && mount.side == edge.part1)) {
								mountCanidates.remove(mount);
							}
						}
					}
				}
				for (Corner corner : Corner.values()) {
					if (corner.ew.dir == dir || corner.ns.dir == dir || corner.ud.dir == dir) {
						if (!connectionAtCorner.containsKey(corner)) {
							connectionAtCorner.put(corner, 1);
						} else {
							connectionAtCorner.put(corner, connectionAtCorner.get(corner) + 1);
						}
					}
				}
			}
		}

		for (Corner corner : Corner.values()) {
			TextureTransformation cornerTexture = LogisticsNewRenderPipe.basicPipeTexture;
			if (!renderState.textureMatrix.isHasPower() && renderState.textureMatrix.isRouted()) {
				cornerTexture = LogisticsNewRenderPipe.inactiveTexture;
			} else if (!renderState.textureMatrix.isRouted() && connectionCount > 2) {
				cornerTexture = LogisticsNewRenderPipe.inactiveTexture;
			}
			int count = connectionAtCorner.containsKey(corner) ? connectionAtCorner.get(corner) : 0;
			if (count == 0) {
				for (IModel3D model : LogisticsNewRenderPipe.corners_M.get(corner)) {
					objectsToRender.add(new RenderEntry(model, cornerTexture));
				}
			} else if (count == 1) {
				for (PipeTurnCorner turn : PipeTurnCorner.values()) {
					if (turn.corner != corner) {
						continue;
					}
					if (renderState.pipeConnectionMatrix.isConnected(turn.getPointer()) || pipe.hasSpecialPipeEndAt(turn.getPointer())) {
						objectsToRender.add(new RenderEntry(LogisticsNewRenderPipe.spacers.get(turn), cornerTexture));
						break;
					}
				}
			} else if (count == 2) {
				for (PipeTurnCorner turn : PipeTurnCorner.values()) {
					if (turn.corner != corner) {
						continue;
					}
					if (!renderState.pipeConnectionMatrix.isConnected(turn.getPointer()) || pipe.hasSpecialPipeEndAt(turn.getPointer())) {
						objectsToRender.add(new RenderEntry(LogisticsNewRenderPipe.corners_I.get(turn), cornerTexture));
						break;
					}
				}
			} else if (count == 3) {
				for (IModel3D model : LogisticsNewRenderPipe.corners_I3.get(corner)) {
					objectsToRender.add(new RenderEntry(model, cornerTexture));
				}
			}
		}

		edgesToRender.stream()
				.map(edge -> new RenderEntry(LogisticsNewRenderPipe.edges.get(edge), LogisticsNewRenderPipe.basicPipeTexture))
				.forEach(objectsToRender::add);

		for (int i = 0; i < 6; i += 2) {
			Direction dir = Direction.from3DDataValue(i);
			List<Direction> list = new ArrayList<>(Arrays.asList(Direction.values()));
			list.remove(dir);
			list.remove(dir.getOpposite());
			if (renderState.pipeConnectionMatrix.isConnected(dir) && renderState.pipeConnectionMatrix.isConnected(dir.getOpposite())) {
				boolean found = false;
				for (Direction dir2 : list) {
					if (renderState.pipeConnectionMatrix.isConnected(dir2)) {
						found = true;
						break;
					}
				}
				if (!found) {
					switch (dir) {
						case DOWN:
							objectsToRender.add(new RenderEntry(LogisticsNewRenderPipe.supports.get(PipeSupport.EAST_SIDE), LogisticsNewRenderPipe.basicPipeTexture));
							objectsToRender.add(new RenderEntry(LogisticsNewRenderPipe.supports.get(PipeSupport.WEST_SIDE), LogisticsNewRenderPipe.basicPipeTexture));
							objectsToRender.add(new RenderEntry(LogisticsNewRenderPipe.supports.get(PipeSupport.NORTH_SIDE), LogisticsNewRenderPipe.basicPipeTexture));
							objectsToRender.add(new RenderEntry(LogisticsNewRenderPipe.supports.get(PipeSupport.SOUTH_SIDE), LogisticsNewRenderPipe.basicPipeTexture));
							break;
						case NORTH:
							objectsToRender.add(new RenderEntry(LogisticsNewRenderPipe.supports.get(PipeSupport.EAST_UP), LogisticsNewRenderPipe.basicPipeTexture));
							objectsToRender.add(new RenderEntry(LogisticsNewRenderPipe.supports.get(PipeSupport.WEST_UP), LogisticsNewRenderPipe.basicPipeTexture));
							objectsToRender.add(new RenderEntry(LogisticsNewRenderPipe.supports.get(PipeSupport.UP_SIDE), LogisticsNewRenderPipe.basicPipeTexture));
							objectsToRender.add(new RenderEntry(LogisticsNewRenderPipe.supports.get(PipeSupport.DOWN_SIDE), LogisticsNewRenderPipe.basicPipeTexture));
							break;
						case WEST:
							objectsToRender.add(new RenderEntry(LogisticsNewRenderPipe.supports.get(PipeSupport.UP_UP), LogisticsNewRenderPipe.basicPipeTexture));
							objectsToRender.add(new RenderEntry(LogisticsNewRenderPipe.supports.get(PipeSupport.DOWN_UP), LogisticsNewRenderPipe.basicPipeTexture));
							objectsToRender.add(new RenderEntry(LogisticsNewRenderPipe.supports.get(PipeSupport.NORTH_UP), LogisticsNewRenderPipe.basicPipeTexture));
							objectsToRender.add(new RenderEntry(LogisticsNewRenderPipe.supports.get(PipeSupport.SOUTH_UP), LogisticsNewRenderPipe.basicPipeTexture));
							break;
						default:
							break;
					}
				}
			}
		}

		boolean[] solidSides = new boolean[6];
		if (pipeTile != null && pipeTile.getWorld() != null) { // This can be null in some cases now !!!
			for (Direction dir : Direction.values()) {
				DoubleCoordinates pos = CoordinateUtils.add(new DoubleCoordinates((BlockEntity) pipeTile), dir);
				BlockState blockState = pos.getBlockState(pipeTile.getWorld());
				if (blockState.isAir() ||
						!blockState.isFaceSturdy(pipeTile.getWorld(), pos.getBlockPos(), dir.getOpposite()) ||
						renderState.pipeConnectionMatrix.isConnected(dir)) {
					mountCanidates.removeIf(mount -> mount.dir == dir);
				} else {
					solidSides[dir.ordinal()] = true;
				}
			}

			// TODO(1.20.1): MCMP not ported — hasParts always false, no mounts removed
			// mountCanidates.removeIf(mount -> SimpleServiceLocator.mcmpProxy.hasParts(pipeTile));
		} else {
			mountCanidates.clear();
		}

		if (!mountCanidates.isEmpty()) {
			if (solidSides[Direction.DOWN.ordinal()]) {
				findOponentOnSameSide(mountCanidates, Direction.DOWN);
			} else if (solidSides[Direction.UP.ordinal()]) {
				findOponentOnSameSide(mountCanidates, Direction.UP);
			} else {
				removeFromSide(mountCanidates, Direction.DOWN);
				removeFromSide(mountCanidates, Direction.UP);
				if (mountCanidates.size() > 2) {
					removeIfHasOponentSide(mountCanidates);
				}
				if (mountCanidates.size() > 2) {
					removeIfHasConnectedSide(mountCanidates);
				}
				if (mountCanidates.size() > 2) {
					findOponentOnSameSide(mountCanidates, mountCanidates.get(0).dir);
				}
			}

			if (LogisticsPipes.isDEBUG() && mountCanidates.size() > 2) {
				new RuntimeException("Trying to render " + mountCanidates.size() + " Mounts").printStackTrace();
			}

			mountCanidates.stream()
					.map(mount -> new RenderEntry(LogisticsNewRenderPipe.mounts.get(mount), LogisticsNewRenderPipe.basicPipeTexture))
					.forEach(objectsToRender::add);
		}

		for (Direction dir : Direction.values()) {
			if (!renderState.pipeConnectionMatrix.isConnected(dir)) {
				for (IModel3D model : LogisticsNewRenderPipe.texturePlate_Outer.get(dir)) {
					int ti = renderState.textureMatrix.getTextureIndex();
					TextureTransformation icon = Textures.LPnewPipeIconProvider.getIcon(ti);
					if (icon != null) {
						objectsToRender.add(new RenderEntry(model, new LPUVTransformationList(new LPUVScale(12f / 16, 12f / 16), icon)));
					}
				}
			}
		}
		if (renderState.textureMatrix.isFluid()) {
			for (Direction dir : Direction.values()) {
				if (!renderState.pipeConnectionMatrix.isConnected(dir)) {
					LogisticsNewRenderPipe.texturePlate_Inner.get(dir).stream()
							.map(model -> new RenderEntry(model, new I3DOperation[] { LogisticsNewRenderPipe.glassCenterTexture }))
							.forEach(objectsToRender::add);
				} else {
					if (!renderState.textureMatrix.isRoutedInDir(dir)) {
						LogisticsNewRenderPipe.sideTexturePlate.get(dir).getValue1().stream()
								.map(model -> new RenderEntry(model, new I3DOperation[] { LogisticsNewRenderPipe.basicPipeTexture }))
								.forEach(objectsToRender::add);
					}
				}
			}
		}
		// TODO(1.20.1): Thermal Dynamics not ported — no TD connection rendering
		// SimpleServiceLocator.thermalDynamicsProxy.renderPipeConnections(pipeTile, objectsToRender);
	}

	private static void findOponentOnSameSide(List<PipeMount> mountCanidates, Direction dir) {
		boolean[] sides = new boolean[6];
		Iterator<PipeMount> iter = mountCanidates.iterator();
		while (iter.hasNext()) {
			PipeMount mount = iter.next();
			if (mount.dir != dir) {
				iter.remove();
			} else {
				sides[mount.side.ordinal()] = true;
			}
		}
		if (mountCanidates.size() <= 2) {
			return;
		}
		List<Direction> keep = new ArrayList<>();
		if (sides[2] && sides[3]) {
			keep.add(Direction.NORTH);
			keep.add(Direction.SOUTH);
		} else if (sides[4] && sides[5]) {
			keep.add(Direction.EAST);
			keep.add(Direction.WEST);
		} else if (sides[0] && sides[1]) {
			keep.add(Direction.UP);
			keep.add(Direction.DOWN);
		}
		iter = mountCanidates.iterator();
		while (iter.hasNext()) {
			PipeMount mount = iter.next();
			if (!keep.contains(mount.side)) {
				iter.remove();
			}
		}
	}

	private static void removeFromSide(List<PipeMount> mountCanidates, Direction dir) {
		mountCanidates.removeIf(mount -> mount.dir == dir);
	}

	private static void reduceToOnePerSide(List<PipeMount> mountCanidates, Direction dir, Direction pref) {
		boolean found = false;
		for (PipeMount mount : mountCanidates) {
			if (mount.dir != dir) {
				continue;
			}
			if (mount.side == pref) {
				found = true;
				break;
			}
		}
		if (!found) {
			reduceToOnePerSide(mountCanidates, dir);
		} else {
			Iterator<PipeMount> iter = mountCanidates.iterator();
			while (iter.hasNext()) {
				PipeMount mount = iter.next();
				if (mount.dir != dir) {
					continue;
				}
				if (mount.side != pref) {
					iter.remove();
				}
			}
		}
	}

	private static void reduceToOnePerSide(List<PipeMount> mountCanidates, Direction dir) {
		boolean found = false;
		Iterator<PipeMount> iter = mountCanidates.iterator();
		while (iter.hasNext()) {
			PipeMount mount = iter.next();
			if (mount.dir != dir) {
				continue;
			}
			if (found) {
				iter.remove();
			} else {
				found = true;
			}
		}
	}

	private static void removeIfHasOponentSide(List<PipeMount> mountCanidates) {
		boolean[] sides = new boolean[6];
		for (PipeMount mount : mountCanidates) {
			sides[mount.dir.ordinal()] = true;
		}
		if (sides[2] && sides[3]) {
			removeFromSide(mountCanidates, Direction.EAST);
			removeFromSide(mountCanidates, Direction.WEST);
			reduceToOnePerSide(mountCanidates, Direction.NORTH);
			reduceToOnePerSide(mountCanidates, Direction.SOUTH);
		} else if (sides[4] && sides[5]) {
			removeFromSide(mountCanidates, Direction.NORTH);
			removeFromSide(mountCanidates, Direction.SOUTH);
			reduceToOnePerSide(mountCanidates, Direction.EAST);
			reduceToOnePerSide(mountCanidates, Direction.WEST);
		}
	}

	private static void removeIfHasConnectedSide(List<PipeMount> mountCanidates) {
		boolean[] sides = new boolean[6];
		for (PipeMount mount : mountCanidates) {
			sides[mount.dir.ordinal()] = true;
		}
		for (int i = 2; i < 6; i++) {
			Direction dir = Direction.from3DDataValue(i);
			Direction rot = dir.getClockWise();
			if (sides[dir.ordinal()] && sides[rot.ordinal()]) {
				reduceToOnePerSide(mountCanidates, dir, dir.getCounterClockWise());
				reduceToOnePerSide(mountCanidates, rot, rot.getClockWise());
			}
		}
	}

	@OnlyIn(Dist.CLIENT)
	public static void renderDestruction(CoreUnroutedPipe pipe, net.minecraft.client.multiplayer.ClientLevel world, int x, int y, int z, ParticleEngine effectRenderer) {
		if (pipe.container != null && pipe.container.renderState != null && pipe.container.renderState.cachedRenderer != null) {
			for (RenderEntry entry : pipe.container.renderState.cachedRenderer) {
				IModel3D model = entry.getModel().twoFacedCopy();
				IBounds bounds = model.bounds();
				double xMid = (bounds.min().x() + bounds.max().x()) / 2;
				double yMid = (bounds.min().y() + bounds.max().y()) / 2;
				double zMid = (bounds.min().z() + bounds.max().z()) / 2;
				model.apply(new LPTranslation(-xMid, -yMid, -zMid));
				effectRenderer.add(new EntityModelFX(world, x + xMid, y + yMid, z + zMid, model, entry.getOperations(), entry.getTexture()));
			}
		}
	}

	public static void renderBoxWithDir(Direction dir) {
		List<RenderEntry> objectsToRender = new ArrayList<>();
		List<Edge> edgesToRender = new ArrayList<>(Arrays.asList(Edge.values()));
		Map<Corner, Integer> connectionAtCorner = new HashMap<>();

		for (Edge edge : Edge.values()) {
			if (edge.part1 == dir || edge.part2 == dir) {
				edgesToRender.remove(edge);
			}
		}
		for (Corner corner : Corner.values()) {
			if (corner.ew.dir == dir || corner.ns.dir == dir || corner.ud.dir == dir) {
				if (!connectionAtCorner.containsKey(corner)) {
					connectionAtCorner.put(corner, 1);
				} else {
					connectionAtCorner.put(corner, connectionAtCorner.get(corner) + 1);
				}
			}
		}
		for (Corner corner : Corner.values()) {
			TextureTransformation cornerTexture = LogisticsNewRenderPipe.basicPipeTexture;
			int count = connectionAtCorner.getOrDefault(corner, 0);
			if (count == 0) {
				LogisticsNewRenderPipe.corners_M.get(corner).stream()
						.map(model -> new RenderEntry(model, new I3DOperation[] { cornerTexture }))
						.forEach(objectsToRender::add);
			} else if (count == 1) {
				for (PipeTurnCorner turn : PipeTurnCorner.values()) {
					if (turn.corner != corner) {
						continue;
					}
					if (turn.getPointer() == dir) {
						objectsToRender.add(new RenderEntry(LogisticsNewRenderPipe.spacers.get(turn), cornerTexture));
						break;
					}
				}
			} else if (count == 2) {
				for (PipeTurnCorner turn : PipeTurnCorner.values()) {
					if (turn.corner != corner) {
						continue;
					}
					if (turn.getPointer() == dir) {
						objectsToRender.add(new RenderEntry(LogisticsNewRenderPipe.corners_I.get(turn), cornerTexture));
						break;
					}
				}
			} else if (count == 3) {
				LogisticsNewRenderPipe.corners_I3.get(corner).stream()
						.map(model -> new RenderEntry(model, new I3DOperation[] { cornerTexture }))
						.forEach(objectsToRender::add);
			}
		}

		edgesToRender.stream()
				.map(edge -> new RenderEntry(LogisticsNewRenderPipe.edges.get(edge), LogisticsNewRenderPipe.basicPipeTexture))
				.forEach(objectsToRender::add);
		for (RenderEntry model : objectsToRender) {
			model.getModel().render(model.getOperations());
		}
	}

	@Nonnull
	public ArrayList<BakedQuad> getQuadsFromRenderList(List<RenderEntry> renderEntryList, VertexFormat format, boolean skipNonBlockTextures) {
		ArrayList<BakedQuad> quads = Lists.newArrayList();
		for (RenderEntry model : renderEntryList) {
			ResourceLocation texture = model.getTexture();
			if (texture == null) {
				throw new NullPointerException();
			}
			if (texture.equals(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS)) {
				quads.addAll(model.getModel().renderToQuads(format, model.getOperations()));
			}
		}
		return quads;
	}

	@Override
	public void renderHighlight(ITubeOrientation orientation) {
		highlight.render(LPColourMultiplier.instance(0xFFFFFFFF));
	}

	public static List<RenderEntry> getBasicPipeFrameRenderList() {
		if (pipeFrameRenderList.isEmpty()) {
			LogisticsTileGenericPipe pipe = new LogisticsTileGenericPipe(net.minecraft.core.BlockPos.ZERO, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
			pipe.pipe = new PipeItemsBasicLogistics(null);
			fillObjectsToRenderList(pipeFrameRenderList, pipe, pipe.renderState);
		}
		return pipeFrameRenderList;
	}
}
