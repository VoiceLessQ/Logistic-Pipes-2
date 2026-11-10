package logisticspipes.pipes.signs;

import net.minecraft.core.registries.BuiltInRegistries;

import java.util.List;

import net.minecraft.client.gui.Font;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.MainTarget;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

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
	@Environment(EnvType.CLIENT)
	public void render(CoreRoutedPipe pipe, LogisticsRenderPipe renderer) {
		// Legacy no-arg entrypoint — handled via the PoseStack overload below.
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void render(CoreRoutedPipe pipe, LogisticsRenderPipe renderer, PoseStack poseStack, SubmitNodeCollector collector, int packedLight) {
		PipeItemsCraftingLogistics cpipe = (PipeItemsCraftingLogistics) pipe;
		Font var17 = net.minecraft.client.Minecraft.getInstance().font;
		oldRenderedStack = null;
		if (cpipe != null) {
			List<ItemIdentifierStack> craftables = cpipe.getCraftedItems();

			String name = "";
			if (craftables != null && craftables.size() > 0) {
				ItemIdentifierStack itemstack = craftables.get(0);
				oldRenderedStack = itemstack;

				renderer.renderItemStackOnSign(itemstack.unsafeMakeNormalStack(), poseStack, collector, packedLight);
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
				collector.submitText(poseStack, -var17.width(idStr) / 2.0F, 0 * 10 - 4 * 5, Component.literal(idStr).getVisualOrderText(), false, Font.DisplayMode.NORMAL, packedLight, 0xFF000000, 0, 0);
				ModuleCrafter logisticsMod = cpipe.getLogisticsModule();
				oldSatelliteName = logisticsMod.clientSideSatelliteNames.satelliteName;
				if (!oldSatelliteName.isEmpty()) {
					String sat = "Sat: " + oldSatelliteName;
					collector.submitText(poseStack, -var17.width(sat) / 2.0F, 1 * 10 - 4 * 5, Component.literal(sat).getVisualOrderText(), false, Font.DisplayMode.NORMAL, packedLight, 0xFF000000, 0, 0);
				}
			} else {
				poseStack.pushPose();
				poseStack.mulPose(new org.joml.Quaternionf().rotationX((float) Math.toRadians(-180)));
				poseStack.translate(0.5F, 0.08F, 0.0F);
				poseStack.scale(1.0F / 90.0F, 1.0F / 90.0F, 1.0F / 90.0F);
				name = "Empty";
			}

			name = renderer.cut(name, var17);

			collector.submitText(poseStack, -var17.width(name) / 2.0F - 15, 3 * 10 - 4 * 5, Component.literal(name).getVisualOrderText(), false, Font.DisplayMode.NORMAL, packedLight, 0xFF000000, 0, 0);

			poseStack.popPose();
		}
	}

	@Override
	@Environment(EnvType.CLIENT)
	public RenderTarget getMCFrameBufferForSign() {
		// OpenGlHelper.isFramebufferEnabled() removed in 1.20.1 — FBOs are always available
		if(fbo == null) {
			fbo = new MainTarget(128, 128);
		}
		return (RenderTarget) fbo;
	}

	@Override
	@Environment(EnvType.CLIENT)
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
