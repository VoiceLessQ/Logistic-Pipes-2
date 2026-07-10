package logisticspipes.ticks;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderGuiOverlayEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.gui.overlay.VanillaGuiOverlay;
import net.neoforged.neoforge.event.TickEvent.Phase;
import net.neoforged.neoforge.event.TickEvent.RenderTickEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;

import logisticspipes.LPBlocks;
import logisticspipes.interfaces.ITubeOrientation;
import logisticspipes.items.ItemLogisticsPipe;
import logisticspipes.pipes.basic.CoreMultiBlockPipe;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericSubMultiBlock;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.object3d.impl.LPRenderStateImpl;
import logisticspipes.renderer.GuiOverlay;
import logisticspipes.renderer.LogisticsHUDRenderer;
import logisticspipes.routing.debug.ClientViewController;
import logisticspipes.utils.LPPositionSet;
import network.rs485.logisticspipes.world.DoubleCoordinates;
import network.rs485.logisticspipes.world.DoubleCoordinatesType;

public class RenderTickHandler {

	private static final ResourceLocation GHOST_PIPE_TEXTURE = new ResourceLocation("logisticspipes", "textures/blocks/pipes/white.png");

	private long renderTicks = 0;

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void renderTick(RenderTickEvent event) {
		if (event.phase == Phase.START) {
			if (GuiOverlay.getInstance().isCompatibleGui()) {
				GuiOverlay.getInstance().preRender();
			}
			ClientViewController.instance().tick();
		} else {
			renderTicks++;
			// TODO: migrate HUD rendering to 1.20 PoseStack / GameRenderer approach.
			// mc.entityRenderer.setupCameraTransform() and ActiveRenderInfo.updateRenderInfo() were removed.
			// See Task #7 (GameRenderer.setupCamera AT entry).
			if (GuiOverlay.getInstance().isCompatibleGui()) {
				GuiOverlay.getInstance().renderOverGui();
			}
		}
	}

	/** LP1 drew the HUD targeting cross right after the vanilla crosshair; the CROSSHAIR
	 *  overlay Post event only fires when the crosshair actually rendered, matching
	 *  LP1's {@code GuiIngameForge.renderCrosshairs} check. */
	@SubscribeEvent
	@OnlyIn(Dist.CLIENT)
	public void renderGuiOverlay(RenderGuiOverlayEvent.Post event) {
		if (!event.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id())) {
			return;
		}
		LogisticsHUDRenderer.instance().renderPlayerDisplay(renderTicks, event.getGuiGraphics());
	}

	@SubscribeEvent
	@OnlyIn(Dist.CLIENT)
	public void renderWorldLast(RenderLevelStageEvent worldEvent) {
		// Only render once per frame, at the AFTER_PARTICLES stage.
		if (worldEvent.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;

		PoseStack poseStack = worldEvent.getPoseStack();
		float partialTick = worldEvent.getPartialTick();
		MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();
		int packedLight = 0xF000F0;

		long renderTicks = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0L;
		LogisticsHUDRenderer.instance().renderWorldRelative(renderTicks, partialTick, poseStack, bufferSource, packedLight);
		bufferSource.endBatch();

		// We are not holding an Item that needs to render a ghost pipe!
		if (!displayPipeGhost()) return;

		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		HitResult box = mc.hitResult;

		// The box is null or we are targeting something else than a block!
		if (box == null || box.getType() != HitResult.Type.BLOCK) return;

		BlockHitResult blockHit = (BlockHitResult) box;
		Inventory inventory = mc.player.getInventory();
		ItemStack stack = inventory.items.get(inventory.selected);
		CoreUnroutedPipe pipe = ((ItemLogisticsPipe) stack.getItem()).getDummyPipe();
		Level world = player.level();
		Direction side = blockHit.getDirection();
		BlockPos pos = blockHit.getBlockPos();
		Block block = world.getBlockState(pos).getBlock();

		if (block == Blocks.SNOW && world.getBlockState(pos).canBeReplaced()) {
			side = Direction.UP;
		} else if (!world.getBlockState(pos).canBeReplaced()) {
			pos = pos.relative(side);
		}

		boolean isFreeSpace = true;
		ITubeOrientation orientation = null;

		if (pipe instanceof CoreMultiBlockPipe) {
			CoreMultiBlockPipe multiPipe = (CoreMultiBlockPipe) pipe;
			DoubleCoordinates placeAt = new DoubleCoordinates(pos);
			LPPositionSet<DoubleCoordinatesType<CoreMultiBlockPipe.SubBlockTypeForShare>> globalPos = new LPPositionSet<>(DoubleCoordinatesType.class);
			globalPos.add(new DoubleCoordinatesType<>(placeAt, CoreMultiBlockPipe.SubBlockTypeForShare.NON_SHARE));
			LPPositionSet<DoubleCoordinatesType<CoreMultiBlockPipe.SubBlockTypeForShare>> positions = multiPipe.getSubBlocks();
			orientation = multiPipe.getTubeOrientation(player, pos.getX(), pos.getZ());

			if (orientation == null) return;

			orientation.rotatePositions(positions);
			positions.stream().map(p -> p.add(placeAt)).forEach(globalPos::add);
			globalPos.addToAll(orientation.getOffset());

			for (DoubleCoordinatesType<CoreMultiBlockPipe.SubBlockTypeForShare> posType : globalPos) {
				if (!world.isEmptyBlock(posType.getBlockPos())) {
					BlockEntity tile = world.getBlockEntity(posType.getBlockPos());
					boolean canPlace = false;
					if (tile instanceof LogisticsTileGenericSubMultiBlock) {
						if (CoreMultiBlockPipe.canShare(((LogisticsTileGenericSubMultiBlock) tile).getSubTypes(), posType.getType())) {
							canPlace = true;
						}
					}
					if (!canPlace) {
						isFreeSpace = false;
						break;
					}
				}
			}
		} else {
			if (!world.isEmptyBlock(pos)) {
				isFreeSpace = false;
			}
		}

		// No free space to render anything!
		if (!isFreeSpace) return;

		// Ghost pipe rendering — LP1 drew the pipe highlight model at the target position
		// with the plain white pipe texture and alpha forced to 0x50.
		poseStack.pushPose();
		net.minecraft.world.phys.Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
		double gx = pos.getX() + (orientation != null ? orientation.getOffset().getXInt() : 0);
		double gy = pos.getY() + (orientation != null ? orientation.getOffset().getYInt() : 0);
		double gz = pos.getZ() + (orientation != null ? orientation.getOffset().getZInt() : 0);
		poseStack.translate(gx - cam.x + 0.001, gy - cam.y + 0.001, gz - cam.z + 0.001);

		VertexConsumer ghostBuffer = bufferSource.getBuffer(RenderType.entityTranslucentCull(GHOST_PIPE_TEXTURE));
		LPRenderStateImpl renderState = (LPRenderStateImpl) SimpleServiceLocator.cclProxy.getRenderState();
		renderState.reset();
		renderState.bind(ghostBuffer, poseStack.last().pose(), poseStack.last().normal(), packedLight, OverlayTexture.NO_OVERLAY);
		renderState.setAlphaOverride(0x50);
		pipe.getHighlightRenderer().renderHighlight(orientation);
		bufferSource.endBatch();
		// Clear the alpha override so the next bound model (pipe BERs share this state) renders opaque.
		renderState.reset();
		poseStack.popPose();
	}

	private boolean displayPipeGhost() {
		Player player = Minecraft.getInstance().player;
		if (player == null) return false;

		Inventory pInventory = player.getInventory();
		if (pInventory == null) return false;

		NonNullList<ItemStack> inv = pInventory.items;
		if (inv == null) return false;

		return inv.size() > pInventory.selected
				&& inv.get(pInventory.selected).getItem() instanceof ItemLogisticsPipe;
	}
}
