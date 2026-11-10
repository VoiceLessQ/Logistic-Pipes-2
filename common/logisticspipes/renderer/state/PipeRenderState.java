package logisticspipes.renderer.state;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter; // was BlockGetter

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import logisticspipes.interfaces.IClientState;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.renderer.newpipe.GLRenderList;
import logisticspipes.renderer.newpipe.RenderEntry;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;
import network.rs485.logisticspipes.world.CoordinateUtils;
import network.rs485.logisticspipes.world.DoubleCoordinates;

public class PipeRenderState implements IClientState {

	public enum LocalCacheType {
		QUADS
	}

	public final ConnectionMatrix pipeConnectionMatrix = new ConnectionMatrix();
	public final TextureMatrix textureMatrix = new TextureMatrix();

	public List<RenderEntry> cachedRenderer = null;
	public Cache<LocalCacheType, Object> objectCache = CacheBuilder.newBuilder().build();
	public int cachedRenderIndex = -1;
	public boolean forceRenderOldPipe = false;
	private boolean[] solidSidesCache = new boolean[6];
	private boolean savedStateHasMCMultiParts = false;

	public int[] buffer = null;
	public Map<Identifier, GLRenderList> renderLists;

	private boolean dirty = true;

	public PipeRenderState() {
	}

	public void clean() {
		dirty = false;
		pipeConnectionMatrix.clean();
		textureMatrix.clean();
		clearRenderCaches();
	}

	public boolean isDirty() {
		return dirty || pipeConnectionMatrix.isDirty() || textureMatrix.isDirty();
	}

	public boolean needsRenderUpdate() {
		return pipeConnectionMatrix.isDirty() || textureMatrix.isDirty();
	}

	public void checkForRenderUpdate(BlockGetter worldIn, BlockPos blockPos) {
		boolean[] solidSides = new boolean[6];
		for (Direction dir : Direction.values()) {
			DoubleCoordinates pos = CoordinateUtils.add(new DoubleCoordinates(blockPos), dir);
			BlockState blockSide = pos.getBlockState(worldIn);
			if (blockSide.isFaceSturdy(worldIn, pos.getBlockPos(), dir.getOpposite()) && !pipeConnectionMatrix.isConnected(dir)) {
				solidSides[dir.ordinal()] = true;
			}
		}
		if (!Arrays.equals(solidSides, solidSidesCache)) {
			solidSidesCache = solidSides.clone();
			clearRenderCaches();
		}
		DoubleCoordinates pos = new DoubleCoordinates(blockPos);
		BlockEntity tile = pos.getTileEntity(worldIn);
		if (tile instanceof LogisticsTileGenericPipe) {
			// MCMultiPart not available on 1.20.1 — hasParts is always false (former dummy behavior).
			boolean hasParts = false;
			if (savedStateHasMCMultiParts != hasParts) {
				savedStateHasMCMultiParts = hasParts;
				clearRenderCaches();
			}
		}
	}

	public void clearRenderCaches() {
		cachedRenderer = null;
		objectCache.invalidateAll();
		objectCache.cleanUp();
	}

	@Override
	public void writeData(LPDataOutput output) {
		pipeConnectionMatrix.writeData(output);
		textureMatrix.writeData(output);
	}

	@Override
	public void readData(LPDataInput input) {
		pipeConnectionMatrix.readData(input);
		textureMatrix.readData(input);
	}
}
