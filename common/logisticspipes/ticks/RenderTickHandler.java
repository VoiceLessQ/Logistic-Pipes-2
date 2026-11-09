package logisticspipes.ticks;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.gui.GuiGraphicsExtractor;

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

	private static final Identifier GHOST_PIPE_TEXTURE = Identifier.fromNamespaceAndPath("logisticspipes", "textures/blocks/pipes/white.png");

	private long renderTicks = 0;

	public void register() {
		// Forge RenderTickEvent START/END: once per rendered level frame
		LevelRenderEvents.START_MAIN.register(context -> renderTick(true));
		LevelRenderEvents.END_MAIN.register(context -> renderTick(false));
		ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
			if (screen instanceof AbstractContainerScreen<?> gui) {
				ScreenEvents.afterExtract(screen).register((s, graphics, mouseX, mouseY, partialTick) -> slotFinderRender(gui, graphics, mouseX, mouseY));
				ScreenMouseEvents.allowMouseClick(screen).register((s, click) -> !slotFinderClick(gui, click.x(), click.y(), click.button()));
			}
		});
		HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR,
				Identifier.fromNamespaceAndPath("logisticspipes", "hud_targeting"), (graphics, deltaTracker) -> renderGuiOverlay(graphics));
		LevelRenderEvents.AFTER_TRANSLUCENT_FEATURES.register(context -> renderWorldLast(context.poseStack(), context.submitNodeCollector(),
				Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false)));
	}

	public void renderTick(boolean start) {
		if (start) {
			ClientViewController.instance().tick();
		} else {
			renderTicks++;
		}
	}

	public void slotFinderRender(AbstractContainerScreen<?> gui, GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		GuiOverlay.getInstance().render(gui, graphics, mouseX, mouseY);
	}

	/** @return true when the overlay consumed the click */
	public boolean slotFinderClick(AbstractContainerScreen<?> gui, double mouseX, double mouseY, int button) {
		return GuiOverlay.getInstance().mouseClicked(gui, mouseX, mouseY, button);
	}

	/** LP1 drew the HUD targeting cross right after the vanilla crosshair (HUD element attached after CROSSHAIR). */
	@Environment(EnvType.CLIENT)
	public void renderGuiOverlay(GuiGraphicsExtractor graphics) {
		LogisticsHUDRenderer.instance().renderPlayerDisplay(renderTicks, graphics);
	}

	@Environment(EnvType.CLIENT)
	public void renderWorldLast(PoseStack poseStack, SubmitNodeCollector collector, float partialTick) {
		int packedLight = 0xF000F0;

		LogisticsHUDRenderer.instance().renderWorldRelative(renderTicks, partialTick, poseStack, collector, packedLight);

		// We are not holding an Item that needs to render a ghost pipe!
		if (!displayPipeGhost()) return;

		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		HitResult box = mc.hitResult;

		// The box is null or we are targeting something else than a block!
		if (box == null || box.getType() != HitResult.Type.BLOCK) return;

		BlockHitResult blockHit = (BlockHitResult) box;
		Inventory inventory = mc.player.getInventory();
		ItemStack stack = inventory.getNonEquipmentItems().get(inventory.getSelectedSlot());
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
		net.minecraft.world.phys.Vec3 cam = Minecraft.getInstance().gameRenderer.mainCamera().position();
		double gx = pos.getX() + (orientation != null ? orientation.getOffset().getXInt() : 0);
		double gy = pos.getY() + (orientation != null ? orientation.getOffset().getYInt() : 0);
		double gz = pos.getZ() + (orientation != null ? orientation.getOffset().getZInt() : 0);
		poseStack.translate(gx - cam.x + 0.001, gy - cam.y + 0.001, gz - cam.z + 0.001);

		final ITubeOrientation ghostOrientation = orientation;
		collector.submitCustomGeometry(poseStack, RenderTypes.entityTranslucent(GHOST_PIPE_TEXTURE), (pose, ghostBuffer) -> {
			LPRenderStateImpl renderState = (LPRenderStateImpl) SimpleServiceLocator.cclProxy.getRenderState();
			renderState.reset();
			renderState.bind(ghostBuffer, pose.pose(), pose.normal(), packedLight, OverlayTexture.NO_OVERLAY);
			renderState.setAlphaOverride(0x50);
			pipe.getHighlightRenderer().renderHighlight(ghostOrientation);
			// Clear the alpha override so the next bound model (pipe BERs share this state) renders opaque.
			renderState.reset();
		});
		poseStack.popPose();
	}

	private boolean displayPipeGhost() {
		Player player = Minecraft.getInstance().player;
		if (player == null) return false;

		Inventory pInventory = player.getInventory();
		if (pInventory == null) return false;

		NonNullList<ItemStack> inv = pInventory.getNonEquipmentItems();
		if (inv == null) return false;

		return inv.size() > pInventory.getSelectedSlot()
				&& inv.get(pInventory.getSelectedSlot()).getItem() instanceof ItemLogisticsPipe;
	}
}
