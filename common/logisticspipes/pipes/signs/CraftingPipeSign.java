package logisticspipes.pipes.signs;

import net.minecraft.core.registries.BuiltInRegistries;

import java.util.List;

import net.minecraft.client.gui.Font;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.MainTarget;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import org.jetbrains.annotations.Nullable;


import logisticspipes.modules.LogisticsModule.ModulePositionType;
import logisticspipes.modules.ModuleCrafter;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.packets.cpipe.CPipeSatelliteImportBack;
import logisticspipes.pipes.PipeItemsCraftingLogistics;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.renderer.LogisticsRenderPipe;
import logisticspipes.utils.item.ItemIdentifierStack;

public class CraftingPipeSign implements IPipeSign {

	public CoreRoutedPipe pipe;
	public Direction dir;

	private Object fbo;
	private ItemIdentifierStack oldRenderedStack = null;
	private String oldSatelliteName = "";

	@Override
	public boolean isAllowedFor(CoreRoutedPipe pipe) {
		return pipe instanceof PipeItemsCraftingLogistics;
	}

	@Override
	public void addSignTo(CoreRoutedPipe pipe, Direction dir, Player player) {
		pipe.addPipeSign(dir, new CraftingPipeSign(), player);
	}

	@Override
	public void readFromNBT(CompoundTag tag) {}

	@Override
	public void writeToNBT(CompoundTag tag) {}

	@Override
	public ModernPacket getPacket() {
		PipeItemsCraftingLogistics cpipe = (PipeItemsCraftingLogistics) pipe;
		return PacketHandler.getPacket(CPipeSatelliteImportBack.class)
				.setInventory(cpipe.getDummyInventory())
				.setType(ModulePositionType.IN_PIPE)
				.setPosX(cpipe.getX())
				.setPosY(cpipe.getY())
				.setPosZ(cpipe.getZ());
	}

	@Override
	public void updateServerSide() {}

	@Override
	public void init(CoreRoutedPipe pipe, Direction dir) {
		this.pipe = pipe;
		this.dir = dir;
	}

	@Override
	public void activate(Player player) {}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void render(CoreRoutedPipe pipe, LogisticsRenderPipe renderer) {
		// Legacy no-arg entrypoint — handled via the PoseStack overload below.
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void render(CoreRoutedPipe pipe, LogisticsRenderPipe renderer, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		PipeItemsCraftingLogistics cpipe = (PipeItemsCraftingLogistics) pipe;
		Font var17 = net.minecraft.client.Minecraft.getInstance().font;
		oldRenderedStack = null;
		if (cpipe != null) {
			List<ItemIdentifierStack> craftables = cpipe.getCraftedItems();

			String name = "";
			if (craftables != null && craftables.size() > 0) {
				ItemIdentifierStack itemstack = craftables.get(0);
				oldRenderedStack = itemstack;

				renderer.renderItemStackOnSign(itemstack.unsafeMakeNormalStack(), poseStack, bufferSource, packedLight);
				Item item = itemstack.getItem().item;

				poseStack.pushPose();
				poseStack.mulPose(new org.joml.Quaternionf().rotationX((float) Math.toRadians(-180)));
				poseStack.translate(0.5F, 0.08F, 0.0F);
				poseStack.scale(1.0F / 90.0F, 1.0F / 90.0F, 1.0F / 90.0F);

				try {
					name = item.getName(itemstack.unsafeMakeNormalStack()).getString();
				} catch (Exception e) {
					try {
						name = item.getDescriptionId();
					} catch (Exception ignored) {}
				}

				String idStr = String.format("ID: %d", BuiltInRegistries.ITEM.getId(item));
				var17.drawInBatch(Component.literal(idStr), -var17.width(idStr) / 2.0F, 0 * 10 - 4 * 5, 0,
						false, poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
				ModuleCrafter logisticsMod = cpipe.getLogisticsModule();
				oldSatelliteName = logisticsMod.clientSideSatelliteNames.satelliteName;
				if (!oldSatelliteName.isEmpty()) {
					String sat = "Sat: " + oldSatelliteName;
					var17.drawInBatch(Component.literal(sat), -var17.width(sat) / 2.0F, 1 * 10 - 4 * 5, 0,
							false, poseStack.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, packedLight);
				}
			} else {
				poseStack.pushPose();
				poseStack.mulPose(new org.joml.Quaternionf().rotationX((float) Math.toRadians(-180)));
				poseStack.translate(0.5F, 0.08F, 0.0F);
				poseStack.scale(1.0F / 90.0F, 1.0F / 90.0F, 1.0F / 90.0F);
				name = "Empty";
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
			fbo = new MainTarget(128, 128);
		}
		return (RenderTarget) fbo;
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public boolean doesFrameBufferNeedUpdating(CoreRoutedPipe pipe, LogisticsRenderPipe renderer) {
		ItemIdentifierStack itemstack = getItemIdentifierStack((PipeItemsCraftingLogistics) pipe);
		if (itemstack != null && oldRenderedStack != null) {
			return fbo == null || !oldRenderedStack.equals(itemstack);
		} else if (itemstack == null && oldRenderedStack == null) {
			return fbo == null;
		} else {
			return true;
		}
	}

	@Nullable
	private ItemIdentifierStack getItemIdentifierStack(PipeItemsCraftingLogistics cpipe) {
		if(cpipe == null) return null;
		List<ItemIdentifierStack> craftables = cpipe.getCraftedItems();
		ItemIdentifierStack itemstack = null;
		if (craftables != null && craftables.size() > 0) {
			itemstack = craftables.get(0);
		}
		return itemstack;
	}
}
