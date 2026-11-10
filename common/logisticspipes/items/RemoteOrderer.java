package logisticspipes.items;

import net.minecraft.world.item.Item;
import logisticspipes.utils.item.CustomDataUtil;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.network.chat.Component;


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
	public void appendHoverText(@Nonnull ItemStack stack, Item.TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<net.minecraft.network.chat.Component> tooltipConsumer, TooltipFlag flagIn) {
		java.util.List<net.minecraft.network.chat.Component> tooltip = new java.util.ArrayList<>();
		super.appendHoverText(stack, context, display, tooltipConsumer, flagIn);

		if (CustomDataUtil.hasTag(stack) && CustomDataUtil.getOrCreateTag(stack).contains("connectedPipe-x")) {
			tooltip.add(Component.literal("\u00a77Has Remote Pipe"));
		}
		tooltip.forEach(tooltipConsumer);
	}

	@Nonnull
	@Override
	public InteractionResult use(Level world, Player player, @Nonnull InteractionHand handIn) {
		ItemStack par1ItemStack = player.getMainHandItem();
		if (par1ItemStack.isEmpty() || !CustomDataUtil.hasTag(par1ItemStack)) {
			return InteractionResult.FAIL;
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
					gui.setDim(pipe.getWorld().dimension().identifier());
					gui.open(player);
				}
			}
		}
		return InteractionResult.PASS;
	}

	public static void connectToPipe(@Nonnull ItemStack stack, PipeItemsRemoteOrdererLogistics pipe) {
		CustomDataUtil.setTag(stack, new CompoundTag());
		final CompoundTag tag = CustomDataUtil.getOrCreateTag(stack);
		tag.putInt("connectedPipe-x", pipe.getX());
		tag.putInt("connectedPipe-y", pipe.getY());
		tag.putInt("connectedPipe-z", pipe.getZ());
		// Store dimension by registry name for forward compatibility; also keep hashed int
		// for compatibility with legacy packet dimension encoding.
		int dimension = pipe.getWorld().dimension().identifier().hashCode();
		tag.putInt("connectedPipe-world-dim", dimension);
		tag.putString("connectedPipe-world-dim-key", pipe.getWorld().dimension().identifier().toString());
		CustomDataUtil.setTag(stack, tag);
	}

	public static PipeItemsRemoteOrdererLogistics getPipe(@Nonnull ItemStack stack) {
		if (stack.isEmpty() || !CustomDataUtil.hasTag(stack)) {
			return null;
		}
		final CompoundTag tag = CustomDataUtil.getOrCreateTag(stack);
		if (!tag.contains("connectedPipe-x") || !tag.contains("connectedPipe-y") || !tag.contains("connectedPipe-z")) {
			return null;
		}
		if (!tag.contains("connectedPipe-world-dim")) {
			return null;
		}
		// Resolve dimension: prefer Identifier string, fall back to hash match.
		var server = logisticspipes.LogisticsPipes.getServer();
		if (server == null) return null;
		Level world = null;
		if (tag.contains("connectedPipe-world-dim-key")) {
			try {
				net.minecraft.resources.Identifier rl = net.minecraft.resources.Identifier.parse(tag.getStringOr("connectedPipe-world-dim-key", ""));
				net.minecraft.resources.ResourceKey<Level> key = net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, rl);
				world = server.getLevel(key);
			} catch (Exception ignored) {}
		}
		if (world == null) {
			int wantHash = tag.getIntOr("connectedPipe-world-dim", 0);
			for (net.minecraft.server.level.ServerLevel lvl : server.getAllLevels()) {
				if (lvl.dimension().identifier().hashCode() == wantHash) { world = lvl; break; }
			}
		}
		if (world == null) {
			return null;
		}
		BlockEntity tile = world.getBlockEntity(new BlockPos(tag.getIntOr("connectedPipe-x", 0), tag.getIntOr("connectedPipe-y", 0), tag.getIntOr("connectedPipe-z", 0)));
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
