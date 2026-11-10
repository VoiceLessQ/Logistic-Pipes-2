package logisticspipes.pipefxhandlers;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.pipes.basic.LogisticsBlockGenericPipe;
import logisticspipes.renderer.newpipe.LogisticsNewRenderPipe;

/** LP1's pipe hit/destroy effects (Forge addHitEffects/addDestroyEffects), called from ClientLevelMixin. */
@Environment(EnvType.CLIENT)
public final class PipeBreakParticles {

	private PipeBreakParticles() {}

	/**
	 * Vanilla crack particles, which skip the INVISIBLE render shape pipes use. They take the pipe model
	 * texture (the block's particle sprite): LP1 used the item icon, but the 26.x icons are the
	 * new-model overlays, fully transparent for transport pipes.
	 */
	public static boolean addHitEffects(ClientLevel level, BlockPos pos, Direction side) {
		BlockState state = level.getBlockState(pos);
		if (!(state.getBlock() instanceof LogisticsBlockGenericPipe)) return false;
		if (LogisticsBlockGenericPipe.getPipe(level, pos) == null) return false;
		// same placement as vanilla ClientLevel.addBreakingBlockEffect
		AABB box = state.getShape(level, pos).bounds();
		RandomSource rand = level.getRandom();
		double px = pos.getX() + rand.nextDouble() * (box.maxX - box.minX - 0.2) + 0.1 + box.minX;
		double py = pos.getY() + rand.nextDouble() * (box.maxY - box.minY - 0.2) + 0.1 + box.minY;
		double pz = pos.getZ() + rand.nextDouble() * (box.maxZ - box.minZ - 0.2) + 0.1 + box.minZ;
		switch (side) {
			case DOWN -> py = pos.getY() + box.minY - 0.1;
			case UP -> py = pos.getY() + box.maxY + 0.1;
			case NORTH -> pz = pos.getZ() + box.minZ - 0.1;
			case SOUTH -> pz = pos.getZ() + box.maxZ + 0.1;
			case WEST -> px = pos.getX() + box.minX - 0.1;
			case EAST -> px = pos.getX() + box.maxX + 0.1;
		}
		Minecraft.getInstance().particleEngine.add(new TerrainParticle(level, px, py, pz, 0, 0, 0, state, pos).setPower(0.2F).scale(0.6F));
		return true;
	}

	/** The pipe model breaks into shards; false leaves it to vanilla. */
	public static boolean addDestroyEffects(ClientLevel level, BlockPos pos, BlockState state) {
		if (!(state.getBlock() instanceof LogisticsBlockGenericPipe)) return false;
		CoreUnroutedPipe pipe = LogisticsBlockGenericPipe.getPipe(level, pos);
		if (pipe == null || pipe.container == null || pipe.container.renderState.cachedRenderer == null) return false;
		LogisticsNewRenderPipe.renderDestruction(pipe, level, pos.getX(), pos.getY(), pos.getZ(), Minecraft.getInstance().particleEngine);
		return true;
	}
}
