package logisticspipes.network.packets.routingdebug;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;



import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.packets.routingdebug.RoutingUpdateTargetResponse.TargetMode;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.StaticResolve;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class RoutingUpdateAskForTarget extends ModernPacket {

	public RoutingUpdateAskForTarget(int id) {
		super(id);
	}

	@Override
	public void readData(LPDataInput input) {}

	@Override
	public void processPacket(Player player) {
		if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
			handleClient();
		}
	}

	// See OpenChatGui: the client refs (Minecraft/HitResult/BlockHitResult/EntityHitResult) live in
	// this @OnlyIn helper so they are stripped before verification on the dedicated server, letting
	// the packet class link and be sent server-side. processPacket stays free of client classes.
	@net.neoforged.api.distmarker.OnlyIn(net.neoforged.api.distmarker.Dist.CLIENT)
	private void handleClient() {
		HitResult box = Minecraft.getInstance().hitResult;
		if (box == null) {
			MainProxy.sendPacketToServer(PacketHandler.getPacket(RoutingUpdateTargetResponse.class).setMode(TargetMode.None));
		} else if (box.getType() == HitResult.Type.BLOCK) {
			MainProxy.sendPacketToServer(PacketHandler.getPacket(RoutingUpdateTargetResponse.class).setMode(TargetMode.Block)
					.setAdditions(new int[] { ((BlockHitResult) box).getBlockPos().getX(), ((BlockHitResult) box).getBlockPos().getY(), ((BlockHitResult) box).getBlockPos().getZ() }));
		} else if (box.getType() == HitResult.Type.ENTITY) {
			MainProxy.sendPacketToServer(PacketHandler.getPacket(RoutingUpdateTargetResponse.class).setMode(TargetMode.Entity)
					.setAdditions(new int[] { ((EntityHitResult) box).getEntity().getId() }));
		}
	}

	@Override
	public void writeData(LPDataOutput output) {}

	@Override
	public ModernPacket template() {
		return new RoutingUpdateAskForTarget(getId());
	}

	@Override
	public boolean isCompressable() {
		return true;
	}
}
