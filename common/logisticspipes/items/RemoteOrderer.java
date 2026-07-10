package logisticspipes.items;

import java.util.List;
import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;

import net.neoforged.neoforge.server.ServerLifecycleHooks;

import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.pipe.RequestPipeDimension;
import logisticspipes.pipes.PipeItemsRemoteOrdererLogistics;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;

public class RemoteOrderer extends LogisticsItem {

	@Override
	public String getModelSubdir() {
		return "remote_orderer";
	}

	@Override
	public int getModelCount() {
		return 17;
	}

	// getShareTag() removed in 1.20 — NBT always shared now
	@Deprecated public boolean getShareTag__REMOVED() {
		return true;
	}

	@Override
	public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level worldIn, java.util.List<Component> tooltip, TooltipFlag flagIn) {
		super.appendHoverText(stack, worldIn, tooltip, flagIn);

		if (stack.hasTag() && Objects.requireNonNull(stack.getTag()).contains("connectedPipe-x")) {
			tooltip.add(Component.literal("\u00a77Has Remote Pipe"));
		}
	}

	@Nonnull
	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, @Nonnull InteractionHand handIn) {
		ItemStack par1ItemStack = player.getMainHandItem();
		if (par1ItemStack.isEmpty() || !par1ItemStack.hasTag()) {
			return InteractionResultHolder.fail(par1ItemStack);
		}
		PipeItemsRemoteOrdererLogistics pipe = RemoteOrderer.getPipe(par1ItemStack);
		if (pipe != null) {
			if (MainProxy.isServer(player.level())) {
				int energyUse = 0;
				if (pipe.getWorld() != player.level()) {
					energyUse += 2500;
				}
				energyUse += Math.sqrt(Math.pow(pipe.getX() - player.getX(), 2) + Math.pow(pipe.getY() - player.getY(), 2) + Math.pow(pipe.getZ() - player.getZ(), 2));
				if (pipe.useEnergy(energyUse)) {
					logisticspipes.network.packets.pipe.RequestPipeDimension dimPkt = PacketHandler.getPacket(logisticspipes.network.packets.pipe.RequestPipeDimension.class);
					dimPkt.setDimension(pipe.getWorld());
					MainProxy.sendPacketToPlayer(dimPkt, player);
					logisticspipes.network.guis.pipe.NormalOrdererGui gui = logisticspipes.network.NewGuiHandler.getGui(logisticspipes.network.guis.pipe.NormalOrdererGui.class);
					gui.setPosX(pipe.getX()).setPosY(pipe.getY()).setPosZ(pipe.getZ());
					gui.setDim(pipe.getWorld().dimension().location());
					gui.open(player);
				}
			}
		}
		return InteractionResultHolder.pass(par1ItemStack);
	}

	public static void connectToPipe(@Nonnull ItemStack stack, PipeItemsRemoteOrdererLogistics pipe) {
		stack.setTag(new CompoundTag());
		final CompoundTag tag = Objects.requireNonNull(stack.getTag());
		tag.putInt("connectedPipe-x", pipe.getX());
		tag.putInt("connectedPipe-y", pipe.getY());
		tag.putInt("connectedPipe-z", pipe.getZ());
		// Store dimension by registry name for forward compatibility; also keep hashed int
		// for compatibility with legacy packet dimension encoding.
		int dimension = pipe.getWorld().dimension().location().hashCode();
		tag.putInt("connectedPipe-world-dim", dimension);
		tag.putString("connectedPipe-world-dim-key", pipe.getWorld().dimension().location().toString());
	}

	public static PipeItemsRemoteOrdererLogistics getPipe(@Nonnull ItemStack stack) {
		if (stack.isEmpty() || !stack.hasTag()) {
			return null;
		}
		final CompoundTag tag = Objects.requireNonNull(stack.getTag());
		if (!tag.contains("connectedPipe-x") || !tag.contains("connectedPipe-y") || !tag.contains("connectedPipe-z")) {
			return null;
		}
		if (!tag.contains("connectedPipe-world-dim")) {
			return null;
		}
		// Resolve dimension: prefer ResourceLocation string, fall back to hash match.
		var server = ServerLifecycleHooks.getCurrentServer();
		if (server == null) return null;
		Level world = null;
		if (tag.contains("connectedPipe-world-dim-key")) {
			try {
				net.minecraft.resources.ResourceLocation rl = new net.minecraft.resources.ResourceLocation(tag.getString("connectedPipe-world-dim-key"));
				net.minecraft.resources.ResourceKey<Level> key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, rl);
				world = server.getLevel(key);
			} catch (Exception ignored) {}
		}
		if (world == null) {
			int wantHash = tag.getInt("connectedPipe-world-dim");
			for (net.minecraft.server.level.ServerLevel lvl : server.getAllLevels()) {
				if (lvl.dimension().location().hashCode() == wantHash) { world = lvl; break; }
			}
		}
		if (world == null) {
			return null;
		}
		BlockEntity tile = world.getBlockEntity(new BlockPos(tag.getInt("connectedPipe-x"), tag.getInt("connectedPipe-y"), tag.getInt("connectedPipe-z")));
		if (!(tile instanceof LogisticsTileGenericPipe)) {
			return null;
		}
		CoreUnroutedPipe pipe = ((LogisticsTileGenericPipe) tile).pipe;
		if (pipe instanceof PipeItemsRemoteOrdererLogistics) {
			return (PipeItemsRemoteOrdererLogistics) pipe;
		}
		return null;
	}


}
