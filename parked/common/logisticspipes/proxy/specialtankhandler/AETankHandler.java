package logisticspipes.proxy.specialtankhandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.fluids.FluidStack;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IInWorldGridNodeHost;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEKey;
import appeng.api.storage.MEStorage;
import it.unimi.dsi.fastutil.objects.Object2LongMap;

import logisticspipes.interfaces.ISpecialTankAccessHandler;
import logisticspipes.utils.FluidIdentifier;

public class AETankHandler implements ISpecialTankAccessHandler {

	// same capability as AEInterfaceInventoryHandler
	private static final Capability<MEStorage> ME_STORAGE = CapabilityManager.get(new CapabilityToken<>() {});

	@Nullable
	private static MEStorage getStorage(@Nullable BlockEntity tile) {
		return tile == null ? null : tile.getCapability(ME_STORAGE, null).orElse(null);
	}

	private static IActionSource source(BlockEntity tile) {
		return tile instanceof IActionHost host ? IActionSource.ofMachine(host) : IActionSource.empty();
	}

	@Override
	public boolean init() {
		return true;
	}

	@Override
	public boolean isType(BlockEntity tile) {
		return getStorage(tile) != null;
	}

	@Override
	public List<BlockEntity> getBaseTilesFor(BlockEntity tile) {
		List<BlockEntity> tiles = new ArrayList<>(1);
		BlockEntity base = getBaseTileEntity(tile);
		tiles.add(base != null ? base : tile);
		return tiles;
	}

	@Override
	public Map<FluidIdentifier, Long> getAvailableLiquid(BlockEntity tile) {
		Map<FluidIdentifier, Long> map = new HashMap<>();
		MEStorage storage = getStorage(tile);
		if (storage == null) {
			return map;
		}
		for (Object2LongMap.Entry<AEKey> entry : storage.getAvailableStacks()) {
			if (entry.getKey() instanceof AEFluidKey key) {
				map.merge(FluidIdentifier.get(key.getFluid(), key.copyTag(), null), entry.getLongValue(), Long::sum);
			}
		}
		return map;
	}

	@Override
	public FluidStack drainFrom(BlockEntity tile, FluidIdentifier ident, Integer amount, boolean drain) {
		MEStorage storage = getStorage(tile);
		FluidStack wanted = ident.makeFluidStack(amount);
		if (storage == null || wanted == null || wanted.isEmpty()) {
			return null;
		}
		AEFluidKey key = AEFluidKey.of(wanted);
		long extracted = storage.extract(key, amount, drain ? Actionable.MODULATE : Actionable.SIMULATE, source(tile));
		if (extracted <= 0) {
			return null;
		}
		return key.toStack((int) Math.min(extracted, Integer.MAX_VALUE));
	}

	// the grid pivot stands in for every interface on one network
	@Nullable
	private static BlockEntity getBaseTileEntity(BlockEntity tile) {
		if (!(tile instanceof IInWorldGridNodeHost host)) {
			return null;
		}
		IGridNode node = host.getGridNode(null);
		IGrid grid = node == null ? null : node.getGrid();
		IGridNode pivot = grid == null ? null : grid.getPivot();
		return pivot != null && pivot.getOwner() instanceof BlockEntity base ? base : null;
	}
}
