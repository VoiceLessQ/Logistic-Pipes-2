package logisticspipes.pipes.signs;

import net.minecraft.core.registries.BuiltInRegistries;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.BitSet;
import java.util.List;
import java.util.Map;

import net.minecraft.client.gui.Font;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.MainTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;



import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.guis.item.ItemAmountSignGui;
import logisticspipes.network.packets.pipe.ItemAmountSignUpdatePacket;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.renderer.LogisticsRenderPipe;
import logisticspipes.routing.ExitRoute;
import logisticspipes.routing.IRouter;
import logisticspipes.routing.PipeRoutingConnectionType;
import logisticspipes.routing.ServerRouter;
import logisticspipes.utils.ISimpleInventoryEventHandler;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.utils.tuples.Pair;
import network.rs485.logisticspipes.util.TextUtil;

public class ItemAmountPipeSign implements IPipeSign, ISimpleInventoryEventHandler {

	public ItemIdentifierInventory itemTypeInv = new ItemIdentifierInventory(1, "", 1);
	public int amount = 100;
	public CoreRoutedPipe pipe;
	public Direction dir;
	private boolean hasUpdated = false;

	@OnlyIn(Dist.CLIENT)
	private RenderTarget fbo;

	public ItemAmountPipeSign() {
		itemTypeInv.addListener(this);
	}

	@Override
	public boolean isAllowedFor(CoreRoutedPipe pipe) {
		return true;
	}

	@Override
	public void addSignTo(CoreRoutedPipe pipe, Direction dir, Player player) {
		pipe.addPipeSign(dir, new ItemAmountPipeSign(), player);
		openGUI(pipe, dir, player);
	}

	private void openGUI(CoreRoutedPipe pipe, Direction dir, Player player) {
		NewGuiHandler.getGui(ItemAmountSignGui.class).setDir(dir).setTilePos(pipe.container).open(player);
	}

	@Override
	public void readFromNBT(CompoundTag tag) {
		itemTypeInv.readFromNBT(tag);
	}

	@Override
	public void writeToNBT(CompoundTag tag) {
		itemTypeInv.writeToNBT(tag);
	}

	@Override
	public ModernPacket getPacket() {
		return PacketHandler.getPacket(ItemAmountSignUpdatePacket.class).setStack(itemTypeInv.getIDStackInSlot(0)).setInteger2(amount).putInt(dir.ordinal()).setTilePos(pipe.container);
	}

	@Override
	public void updateServerSide() {
		if (!pipe.isNthTick(20)) {
			return;
		}
		if (hasUpdated) {
			hasUpdated = false;
			return;
		}
		int newAmount = 0;
		if (itemTypeInv.getIDStackInSlot(0) != null) {
			Map<ItemIdentifier, Integer> availableItems = SimpleServiceLocator.logisticsManager.getAvailableItems(pipe.getRouter().getIRoutersByCost());
			if (availableItems != null) {
				BitSet set = new BitSet(ServerRouter.getBiggestSimpleID());
				spread(availableItems, set);
				if (availableItems.containsKey(itemTypeInv.getIDStackInSlot(0).getItem())) {
					newAmount = availableItems.get(itemTypeInv.getIDStackInSlot(0).getItem());
				}
			}
		}
		if (newAmount != amount) {
			amount = newAmount;
			sendUpdatePacket();
		}
	}

	private void spread(Map<ItemIdentifier, Integer> availableItems, BitSet set) { // Improve performance by updating a wall of Amount pipe signs all at once
		IRouter router = pipe.getRouter();
		if (set.get(router.getSimpleID())) return;
		set.set(router.getSimpleID());
		for (ExitRoute exit : router.getIRoutersByCost()) {
			if (exit.distanceToDestination > 2) break; // Only when the signs are in one wall. To not spread to far.
			if (!exit.filters.isEmpty()) continue;
			if (set.get(exit.destination.getSimpleID())) continue;
			if (exit.connectionDetails.contains(PipeRoutingConnectionType.canRequestFrom) && exit.connectionDetails.contains(PipeRoutingConnectionType.canRouteTo)) {
				CoreRoutedPipe cachedPipe = exit.destination.getCachedPipe();
				if (cachedPipe != null) {
					List<Pair<Direction, IPipeSign>> pipeSigns = cachedPipe.getPipeSigns();
					pipeSigns.stream()
							.filter(signPair -> signPair != null && signPair.getValue2() instanceof ItemAmountPipeSign)
							.forEach(signPair -> ((ItemAmountPipeSign) signPair.getValue2()).updateStats(availableItems, set));
				}
			}
		}
	}

	private void updateStats(Map<ItemIdentifier, Integer> availableItems, BitSet set) {
		hasUpdated = true;
		int newAmount = 0;
		if (itemTypeInv.getIDStackInSlot(0) != null) {
			if (availableItems.containsKey(itemTypeInv.getIDStackInSlot(0).getItem())) {
				newAmount = availableItems.get(itemTypeInv.getIDStackInSlot(0).getItem());
			}
		}
		if (newAmount != amount) {
			amount = newAmount;
			sendUpdatePacket();
		}
		spread(availableItems, set);
	}

	@Override
	public void activate(Player player) {
		openGUI(pipe, dir, player);
	}

	@Override
	public void init(CoreRoutedPipe pipe, Direction dir) {
		this.pipe = pipe;
		this.dir = dir;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void render(CoreRoutedPipe pipe, LogisticsRenderPipe renderer) {
		// Legacy no-arg entrypoint — handled via the PoseStack overload below.
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void render(CoreRoutedPipe pipe, LogisticsRenderPipe renderer, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		Font var17 = net.minecraft.client.Minecraft.getInstance().font;
		if (pipe != null) {
			String name = "";
			String idStr = "";
			String displayAmount = null;
			if (itemTypeInv != null && itemTypeInv.getIDStackInSlot(0) != null) {
				ItemStack itemstack = itemTypeInv.getIDStackInSlot(0).unsafeMakeNormalStack();

				renderer.renderItemStackOnSign(itemstack, poseStack, bufferSource, packedLight);
				Item item = itemstack.getItem();

				poseStack.pushPose();
				poseStack.mulPose(new org.joml.Quaternionf().rotationX((float) Math.toRadians(-180)));
				poseStack.translate(0.5F, 0.08F, 0.0F);
				poseStack.scale(1.0F / 90.0F, 1.0F / 90.0F, 1.0F / 90.0F);

				try {
					name = item.getName(itemstack).getString();
				} catch (Exception e) {
					try {
						name = item.getDescriptionId();
					} catch (Exception ignored) {}
				}

				idStr = String.format("ID: %d", BuiltInRegistries.ITEM.getId(item));
				displayAmount = TextUtil.getThreeDigitFormattedNumber(amount, false);
			} else {
				poseStack.pushPose();
				poseStack.mulPose(new org.joml.Quaternionf().rotationX((float) Math.toRadians(-180)));
				poseStack.translate(0.5F, 0.08F, 0.0F);
				poseStack.scale(1.0F / 90.0F, 1.0F / 90.0F, 1.0F / 90.0F);
				name = "Empty";
			}

			if (!idStr.isEmpty()) {
				var17.drawInBatch(Component.literal(idStr), -var17.width(idStr) / 2.0F, 0 * 10 - 4 * 5, 0,
						false, poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
			}
			if (displayAmount != null) {
				var17.drawInBatch(Component.literal("Amount:"), -var17.width("Amount:") / 2.0F, 1 * 10 - 4 * 5, 0,
						false, poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
				var17.drawInBatch(Component.literal(displayAmount), -var17.width(displayAmount) / 2.0F, 2 * 10 - 4 * 5, 0,
						false, poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
			}

			name = renderer.cut(name, var17);

			var17.drawInBatch(Component.literal(name), -var17.width(name) / 2.0F - 15, 3 * 10 - 4 * 5, 0,
					false, poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);

			poseStack.popPose();
		}
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public RenderTarget getMCFrameBufferForSign() {
		// OpenGlHelper.isFramebufferEnabled() removed in 1.20.1 — FBOs are always available
		if(fbo == null) {
			fbo = new MainTarget(256, 256);
		}
		return fbo;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public boolean doesFrameBufferNeedUpdating(CoreRoutedPipe pipe, LogisticsRenderPipe renderer) {
		return fbo == null;
	}

	@Override
	public void InventoryChanged(Container inventory) {
		if (inventory == itemTypeInv) {
			sendUpdatePacket();
		}
	}

	private void sendUpdatePacket() {
		if (MainProxy.isServer(pipe.getWorld())) {
			MainProxy.sendPacketToAllWatchingChunk(pipe.container, getPacket());
		}
	}
}
