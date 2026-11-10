package logisticspipes.renderer;
import net.minecraft.client.renderer.RenderPipelines;
import logisticspipes.utils.gui.SimpleGraphics;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.world.entity.EquipmentSlot;
import org.joml.Quaternionf;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nonnull;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;



import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;




import logisticspipes.api.IHUDArmor;
import logisticspipes.config.Configs;
import logisticspipes.hud.HUDConfig;
import logisticspipes.interfaces.IDebugHUDProvider;
import logisticspipes.interfaces.IHUDConfig;
import logisticspipes.interfaces.IHeadUpDisplayBlockRendererProvider;
import logisticspipes.interfaces.IHeadUpDisplayRendererProvider;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.routing.IRouter;
import logisticspipes.routing.LaserData;
import logisticspipes.routing.PipeRoutingConnectionType;
import logisticspipes.utils.gui.DevGuiDriver;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.item.ItemStackRenderer;
import logisticspipes.utils.item.ItemStackRenderer.DisplayAmount;
import logisticspipes.utils.math.Vector3d;
import logisticspipes.utils.tuples.Pair;

public class LogisticsHUDRenderer {

	public IDebugHUDProvider debugHUD = null;

	private LinkedList<IHeadUpDisplayRendererProvider> list = new LinkedList<>();
	private double lastXPos = 0;
	private double lastYPos = 0;
	private double lastZPos = 0;

	private int progress = 0;
	private long last = 0;

	private ArrayList<IHeadUpDisplayBlockRendererProvider> providers = new ArrayList<>();

	private List<LaserData> lasers = new ArrayList<>();

	private static LogisticsHUDRenderer renderer = null;

	public void add(IHeadUpDisplayBlockRendererProvider provider) {
		IHeadUpDisplayBlockRendererProvider toRemove = null;
		for (IHeadUpDisplayBlockRendererProvider listedProvider : providers) {
			if (listedProvider.getX() == provider.getX() && listedProvider.getY() == provider.getY() && listedProvider.getZ() == provider.getZ()) {
				toRemove = listedProvider;
				break;
			}
		}
		if (toRemove != null) {
			providers.remove(toRemove);
		}
		providers.add(provider);
	}

	public void remove(IHeadUpDisplayBlockRendererProvider provider) {
		providers.remove(provider);
	}

	public void clear() {
		providers.clear();
		LogisticsHUDRenderer.instance().clearList(false);
	}

	private void clearList(boolean flag) {
		if (flag) {
			list.forEach(IHeadUpDisplayRendererProvider::stopWatching);
		}
		list.clear();
	}

	private void refreshList(double x, double y, double z) {
		ArrayList<Pair<Double, IHeadUpDisplayRendererProvider>> newList = new ArrayList<>();
		for (IRouter router : SimpleServiceLocator.routerManager.getRouters()) {
			if (router == null) {
				continue;
			}
			CoreRoutedPipe pipe = router.getPipe();
			if (!(pipe instanceof IHeadUpDisplayRendererProvider)) {
				continue;
			}
			if (pipe.getWorld() == Minecraft.getInstance().level) {
				double dis = Math.hypot(pipe.getX() - x + 0.5, Math.hypot(pipe.getY() - y + 0.5, pipe.getZ() - z + 0.5));
				if (dis < Configs.LOGISTICS_HUD_RENDER_DISTANCE && dis > 0.75) {
					newList.add(new Pair<>(dis, (IHeadUpDisplayRendererProvider) pipe));
					if (!list.contains(pipe)) {
						((IHeadUpDisplayRendererProvider) pipe).startWatching();
					}
				}
			}
		}

		List<IHeadUpDisplayBlockRendererProvider> remove = new ArrayList<>();
		providers.stream().filter(provider -> provider.getLevelForHUD() == Minecraft.getInstance().level)
				.forEach(provider -> {
					double dis = Math.hypot(provider.getX() - x + 0.5, Math.hypot(provider.getY() - y + 0.5, provider.getZ() - z + 0.5));
					if (dis < Configs.LOGISTICS_HUD_RENDER_DISTANCE && dis > 0.75 && !provider.isHUDInvalid() && provider.isHUDExistent()) {
						newList.add(new Pair<>(dis, provider));
						if (!list.contains(provider)) {
							provider.startWatching();
						}
					} else if (provider.isHUDInvalid() || !provider.isHUDExistent()) {
						remove.add(provider);
					}
				});
		for (IHeadUpDisplayBlockRendererProvider provider : remove) {
			providers.remove(provider);
		}

		if (newList.size() < 1) {
			clearList(true);
			return;
		}
		newList.sort(Comparator.comparing(Pair::getValue1));
		for (IHeadUpDisplayRendererProvider part : list) {
			boolean contains = false;
			for (Pair<Double, IHeadUpDisplayRendererProvider> inpart : newList) {
				if (inpart.getValue2().equals(part)) {
					contains = true;
					break;
				}
			}
			if (!contains) {
				part.stopWatching();
			}
		}
		clearList(false);
		for (Pair<Double, IHeadUpDisplayRendererProvider> part : newList) {
			list.addLast(part.getValue2());
		}
	}

	private boolean playerWearsHUD() {
		return Minecraft.getInstance().player != null && checkItemStackForHUD(Minecraft.getInstance().player.getItemBySlot(EquipmentSlot.HEAD));
	}

	private boolean checkItemStackForHUD(@Nonnull ItemStack stack) {
		if (stack.getItem() instanceof IHUDArmor) {
			return ((IHUDArmor) stack.getItem()).isEnabled(stack);
		}
		return false;
	}

	private boolean displayCross = false;

	// LP1's panel size and distance from the block centre toward the viewer.
	private static final float PANEL_SCALE = 0.01F;
	private static final float PANEL_OFFSET = 0.4F;
	// Solid blocks (power junction/provider) would hide a panel at 0.4; put it just outside the cube.
	private static final float BLOCK_PANEL_OFFSET = 0.9F;
	private static final Identifier CROSSHAIR = Identifier.withDefaultNamespace("hud/crosshair");

	public void renderPlayerDisplay(long renderTicks, GuiGraphicsExtractor guiGraphics) {
		if (!displayRenderer()) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (displayHUD() && displayCross) {
			int width = mc.getWindow().getGuiScaledWidth();
			int height = mc.getWindow().getGuiScaledHeight();
			if (mc.gui != null && guiGraphics != null) {
				// LP1 redrew the vanilla crosshair tinted black to mark a HUD target lock.
				guiGraphics.blitSprite(RenderPipelines.GUI_TEXTURED, CROSSHAIR, width / 2 - 7, height / 2 - 7, 15, 15, 0xFF000000);
			}
		}
	}

	@Environment(EnvType.CLIENT)
	public void renderWorldRelative(long renderTicks, float partialTick, PoseStack poseStack, SubmitNodeCollector collector, int packedLight) {
		if (!displayRenderer()) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		if (list.size() == 0 || Math.hypot(lastXPos - player.getX(), Math.hypot(lastYPos - player.getY(), lastZPos - player.getZ())) > 0.5 || (renderTicks % 10 == 0 && (lastXPos != player.getX() || lastYPos != player.getY() || lastZPos != player.getZ())) || renderTicks % 600 == 0) {
			refreshList(player.getX(), player.getY(), player.getZ());
			lastXPos = player.getX();
			lastYPos = player.getY();
			lastZPos = player.getZ();
		}
		boolean cursorHandled = false;
		displayCross = false;
		IHUDConfig config;
		if (debugHUD == null) {
			config = new HUDConfig(mc.player.getItemBySlot(EquipmentSlot.HEAD));
		} else {
			config = new IHUDConfig() {

				@Override
				public boolean isHUDSatellite() {
					return false;
				}

				@Override
				public boolean isHUDProvider() {
					return false;
				}

				@Override
				public boolean isHUDPowerLevel() {
					return false;
				}

				@Override
				public boolean isHUDInvSysCon() {
					return false;
				}

				@Override
				public boolean isHUDCrafting() {
					return false;
				}

				@Override
				public boolean isChassisHUD() {
					return false;
				}

				@Override
				public void setChassisHUD(boolean state) {}

				@Override
				public void setHUDCrafting(boolean state) {}

				@Override
				public void setHUDInvSysCon(boolean state) {}

				@Override
				public void setHUDPowerJunction(boolean state) {}

				@Override
				public void setHUDProvider(boolean state) {}

				@Override
				public void setHUDSatellite(boolean state) {}
			};
		}
		IHeadUpDisplayRendererProvider thisIsLast = null;
		List<IHeadUpDisplayRendererProvider> toUse = list;
		if (debugHUD != null) {
			toUse = debugHUD.getHUDs();
		}

		for (IHeadUpDisplayRendererProvider renderer : toUse) {
			if (renderer.getRenderer() == null) {
				continue;
			}
			if (renderer.getRenderer().display(config)) {
				poseStack.pushPose();
				if (!cursorHandled) {
					double x = renderer.getX() + 0.5 - player.getX();
					double y = renderer.getY() + 0.5 - player.getY();
					double z = renderer.getZ() + 0.5 - player.getZ();
					if (Math.hypot(x, Math.hypot(y, z)) < 0.75 || (renderer instanceof IHeadUpDisplayBlockRendererProvider && (((IHeadUpDisplayBlockRendererProvider) renderer).isHUDInvalid() || !((IHeadUpDisplayBlockRendererProvider) renderer).isHUDExistent()))) {
						refreshList(player.getX(), player.getY(), player.getZ());
						poseStack.popPose();
						break;
					}
					int[] pos = getCursor(renderer);
					if (pos.length == 2) {
						if (renderer.getRenderer().cursorOnWindow(pos[0], pos[1])) {
							renderer.getRenderer().handleCursor(pos[0], pos[1]);
							if (Minecraft.getInstance().hasShiftDown() || DevGuiDriver.holdShift) { //if(Minecraft.getInstance().player.isCrouching()) {
								thisIsLast = renderer;
								displayCross = true;
							}
							cursorHandled = true;
						}
					}
				}
				if (thisIsLast != renderer) {
					displayOneView(renderer, config, false, poseStack, collector);
				}
				poseStack.popPose();
			}
		}
		if (thisIsLast != null) {
			poseStack.pushPose();
			displayOneView(thisIsLast, config, true, poseStack, collector);
			poseStack.popPose();
		}

		poseStack.pushPose();
		HitResult box = mc.hitResult;
		if (box != null && box.getType() == HitResult.Type.BLOCK) {
			if ((Minecraft.getInstance().hasControlDown() || DevGuiDriver.holdControl)) {
				progress = Math.min(progress + (2 * Math.max(1, (int) Math.floor((System.currentTimeMillis() - last) / 50.0D))), 100);
			} else {
				progress = Math.max(progress - (2 * Math.max(1, (int) Math.floor((System.currentTimeMillis() - last) / 50.0D))), 0);
			}
			if (progress != 0 && box instanceof BlockHitResult blockHit) {
				renderBlockInfo(blockHit, progress, poseStack, collector);
			}
		} else if (!(Minecraft.getInstance().hasControlDown() || DevGuiDriver.holdControl)) {
			progress = 0;
		}
		poseStack.popPose();

		//Render Laser
		if (!lasers.isEmpty()) {
			// The pose origin is the interpolated camera, not the player's feet as in 1.12.
			net.minecraft.world.phys.Vec3 cam = mc.gameRenderer.mainCamera().position();
			for (LaserData data : lasers) {
				poseStack.pushPose();
				double x = data.getPosX() + 0.5 - cam.x;
				double y = data.getPosY() + 0.5 - cam.y;
				double z = data.getPosZ() + 0.5 - cam.z;
				poseStack.translate((float) x, (float) y, (float) z);
				switch (data.getDir()) {
					case NORTH: poseStack.mulPose(new Quaternionf().rotationY( (float) Math.toRadians( 90.0F))); break;
					case SOUTH: poseStack.mulPose(new Quaternionf().rotationY( (float) Math.toRadians(-90.0F))); break;
					case WEST:  poseStack.mulPose(new Quaternionf().rotationY( (float) Math.toRadians(180.0F))); break;
					case UP:    poseStack.mulPose(new Quaternionf().rotationZ( (float) Math.toRadians( 90.0F))); break;
					case DOWN:  poseStack.mulPose(new Quaternionf().rotationZ( (float) Math.toRadians(-90.0F))); break;
					default: break;
				}
				poseStack.scale(0.01F, 0.01F, 0.01F);
				collector.submitCustomGeometry(poseStack, RenderTypes.debugQuads(), (mat, bb) -> renderLaser(data, mat, bb));
				poseStack.popPose();
			}
		}
		last = System.currentTimeMillis();
	}

	private void renderLaser(LaserData data, PoseStack.Pose mat, VertexConsumer bb) {
		for (float i = 0; i < 6 * data.getLength(); i += 1.0f) {
			int[] c = getLaserColor(i, data.getConnectionType());
			float shift = 100f * i / 6f;
			float s = (data.isStartPipe() && i == 0) ? -6.0f : 0.0f;
			// Top
			bb.addVertex(mat, 19.7f+shift, 3,-3).setColor(c[0],c[1],c[2],c[3]);
			bb.addVertex(mat,  3.0f+shift+s, 3,-3).setColor(c[0],c[1],c[2],c[3]);
			bb.addVertex(mat,  3.0f+shift+s, 3, 3).setColor(c[0],c[1],c[2],c[3]);
			bb.addVertex(mat, 19.7f+shift, 3, 3).setColor(c[0],c[1],c[2],c[3]);
			// Bottom
			bb.addVertex(mat, 19.7f+shift,-3, 3).setColor(c[0],c[1],c[2],c[3]);
			bb.addVertex(mat,  3.0f+shift+s,-3, 3).setColor(c[0],c[1],c[2],c[3]);
			bb.addVertex(mat,  3.0f+shift+s,-3,-3).setColor(c[0],c[1],c[2],c[3]);
			bb.addVertex(mat, 19.7f+shift,-3,-3).setColor(c[0],c[1],c[2],c[3]);
			// +Z side
			bb.addVertex(mat, 19.7f+shift, 3, 3).setColor(c[0],c[1],c[2],c[3]);
			bb.addVertex(mat,  3.0f+shift+s, 3, 3).setColor(c[0],c[1],c[2],c[3]);
			bb.addVertex(mat,  3.0f+shift+s,-3, 3).setColor(c[0],c[1],c[2],c[3]);
			bb.addVertex(mat, 19.7f+shift,-3, 3).setColor(c[0],c[1],c[2],c[3]);
			// -Z side
			bb.addVertex(mat, 19.7f+shift,-3,-3).setColor(c[0],c[1],c[2],c[3]);
			bb.addVertex(mat,  3.0f+shift+s,-3,-3).setColor(c[0],c[1],c[2],c[3]);
			bb.addVertex(mat,  3.0f+shift+s, 3,-3).setColor(c[0],c[1],c[2],c[3]);
			bb.addVertex(mat, 19.7f+shift, 3,-3).setColor(c[0],c[1],c[2],c[3]);
		}
		if (data.isStartPipe()) {
			int[] c = getLaserColor(0, data.getConnectionType());
			bb.addVertex(mat,-3, 3, 3).setColor(c[0],c[1],c[2],c[3]);
			bb.addVertex(mat,-3, 3,-3).setColor(c[0],c[1],c[2],c[3]);
			bb.addVertex(mat,-3,-3,-3).setColor(c[0],c[1],c[2],c[3]);
			bb.addVertex(mat,-3,-3, 3).setColor(c[0],c[1],c[2],c[3]);
		}
		if (data.isFinalPipe()) {
			int[] c = getLaserColor(6 * (float) data.getLength() - 1, data.getConnectionType());
			float ex = 100.0f * data.getLength() + 3f;
			bb.addVertex(mat,ex, 3,-3).setColor(c[0],c[1],c[2],c[3]);
			bb.addVertex(mat,ex, 3, 3).setColor(c[0],c[1],c[2],c[3]);
			bb.addVertex(mat,ex,-3, 3).setColor(c[0],c[1],c[2],c[3]);
			bb.addVertex(mat,ex,-3,-3).setColor(c[0],c[1],c[2],c[3]);
		}
	}

	private int[] getLaserColor(float i, EnumSet<PipeRoutingConnectionType> flags) {
		if (!flags.isEmpty()) {
			int k = 0;
			for (PipeRoutingConnectionType type : PipeRoutingConnectionType.values) {
				if (flags.contains(type)) k++;
				if (k - 1 == (int) i % flags.size()) return getLaserTypeColor(type);
			}
		}
		return new int[]{255, 255, 255, 128};
	}

	private int[] getLaserTypeColor(PipeRoutingConnectionType type) {
		switch (type) {
			case canRouteTo:     return new int[]{255, 255,   0, 128};
			case canRequestFrom: return new int[]{  0, 255,   0, 128};
			case canPowerFrom:   return new int[]{  0,   0, 255, 128};
			default:             return new int[]{255, 255, 255, 128};
		}
	}

	private void displayOneView(IHeadUpDisplayRendererProvider renderer, IHUDConfig config, boolean shifted, PoseStack poseStack, SubmitNodeCollector collector) {
		Minecraft mc = Minecraft.getInstance();
		// The level-stage pose origin is the interpolated camera, not the player's feet as in 1.12.
		net.minecraft.world.phys.Vec3 cam = mc.gameRenderer.mainCamera().position();
		double x = renderer.getX() + 0.5 - cam.x;
		double y = renderer.getY() + 0.5 - cam.y;
		double z = renderer.getZ() + 0.5 - cam.z;
		double distance = Math.hypot(x, Math.hypot(y, z));
		poseStack.pushPose();
		poseStack.translate((float) x, (float) y, (float) z);
		poseStack.mulPose(new Quaternionf().rotationX((float) Math.toRadians(90.0F)));
		poseStack.mulPose(new Quaternionf().rotationZ((float) Math.toRadians(getAngle(z, x) + 90)));
		// y is camera relative and therefore already eye relative; LP1 subtracted the eye height here.
		poseStack.mulPose(new Quaternionf().rotationX((float) Math.toRadians((-1) * getAngle(Math.hypot(x, z), y) + 180)));
		poseStack.translate(0.0F, 0.0F, -panelOffset(renderer));
		poseStack.scale(PANEL_SCALE, PANEL_SCALE, PANEL_SCALE);
		// LP1 drew the shift-locked panel last without depth test so it stays readable in front of blocks.
		HUDPanelAtlas.instance().submit(renderer, 1.0F, shifted, poseStack, collector,
				gg -> renderer.getRenderer().renderHeadUpDisplay(distance, false, shifted, mc, config));
		poseStack.popPose();
	}

	/** LP1's Ctrl-hold panel beside the looked-at block: the block's item tooltip plus its mod name (LP1 took it from NEI/JEI). */
	private void renderBlockInfo(BlockHitResult box, int progress, PoseStack poseStack, SubmitNodeCollector collector) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || mc.player == null) return;
		BlockPos pos = box.getBlockPos();
		ItemStack stack = mc.level.getBlockState(pos).getCloneItemStack(mc.level, pos, false);
		if (stack.isEmpty()) return;
		List<Component> textData = new ArrayList<>(stack.getTooltipLines(net.minecraft.world.item.Item.TooltipContext.of(mc.level), mc.player, net.minecraft.world.item.TooltipFlag.NORMAL));
		String namespace = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace();
		net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer(namespace)
				.ifPresent(mod -> textData.add(Component.literal(mod.getMetadata().getName()).withStyle(net.minecraft.ChatFormatting.BLUE, net.minecraft.ChatFormatting.ITALIC)));
		net.minecraft.world.phys.Vec3 cam = mc.gameRenderer.mainCamera().position();
		double x = pos.getX() + 0.5D - cam.x;
		double y = pos.getY() + 0.5D - cam.y;
		double z = pos.getZ() + 0.5D - cam.z;
		// LP1 measured the tilt from the feet
		double feetY = y + mc.player.getEyeHeight();
		double dProgress = progress / 100D;
		int height = Math.max(32, 10 * textData.size() + 15);
		int width = 0;
		for (Component s : textData) {
			width = Math.max(width, mc.font.width(s) + 22);
		}
		width = Math.max(32, width + 15);
		float fit = Math.min(1.0F, (HUDPanelAtlas.CELL - 2.0F) / Math.max(width, height));
		poseStack.pushPose();
		poseStack.translate((float) x, (float) y, (float) z);
		poseStack.mulPose(new Quaternionf().rotationX((float) Math.toRadians(90.0F)));
		poseStack.mulPose(new Quaternionf().rotationZ((float) Math.toRadians(getAngle(z, x) + 110F)));
		poseStack.mulPose(new Quaternionf().rotationX((float) Math.toRadians((-1) * getAngle(Math.hypot(x + 0.8, z + 0.8), feetY + 0.5) + 180)));
		poseStack.translate((float) (0.4D * dProgress + 0.6D), (float) (-0.2D * dProgress - 0.6D), 0.0F);
		poseStack.scale(0.01F, 0.01F, 0.01F);
		int panelWidth = width;
		int panelHeight = height;
		HUDPanelAtlas.instance().submit(BLOCK_INFO_KEY, fit, true, poseStack, collector,
				gg -> drawBlockInfo(mc, gg, stack, textData, panelWidth, panelHeight, dProgress));
		poseStack.popPose();
	}

	private static final Object BLOCK_INFO_KEY = new Object();

	/** Draws the block info panel, origin at its centre. */
	private static void drawBlockInfo(Minecraft mc, GuiGraphicsExtractor gg, ItemStack stack, List<Component> textData, int width, int heigth, double dProgress) {
		int left = (int) ((-0.5 * (width - 32)) * dProgress) - 16;
		int top = (int) ((-0.5 * (heigth - 32)) * dProgress) - 16;
		SimpleGraphics.setColor(1.0F, 1.0F, 1.0F, 96 / 127F);
		LPGuiGraphics.drawGuiBackGround(mc, left, top, (int) ((0.5 * (width - 32)) * dProgress) + 16, (int) ((0.5 * (heigth - 32)) * dProgress) + 16, 0, false);
		SimpleGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		if (dProgress < 1.0D) return;
		gg.pose().pushMatrix();
		gg.pose().translate(left, top);
		for (int i = 0; i < textData.size(); i++) {
			SimpleGraphics.drawString(gg, mc.font, textData.get(i), 28, 8 + i * 10, 0x000000, false);
		}
		gg.pose().scale(1.5F * 0.8F, 1.5F * 0.8F);
		gg.item(stack, 5, 6);
		gg.pose().popMatrix();
	}

	private static float panelOffset(IHeadUpDisplayRendererProvider renderer) {
		return renderer instanceof IHeadUpDisplayBlockRendererProvider ? BLOCK_PANEL_OFFSET : PANEL_OFFSET;
	}

	private float getAngle(double x, double y) {
		return (float) (Math.atan2(x, y) * 360 / (2 * Math.PI));
	}

	public double up(double input) {
		input %= 360.0D;
		while (input < 0 && !Double.isNaN(input) && !Double.isInfinite(input)) {
			input += 360;
		}
		return input;
	}

	private int[] getCursor(IHeadUpDisplayRendererProvider renderer) {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;

		Vector3d playerView = Vector3d.getFromAngles((270 - player.getYRot()) / 360 * -2 * Math.PI, (player.getXRot()) / 360 * -2 * Math.PI);
		Vector3d playerPos = new Vector3d();
		playerPos.x = player.getX();
		playerPos.y = player.getY() + player.getEyeHeight();
		playerPos.z = player.getZ();

		Vector3d panelPos = new Vector3d();
		panelPos.x = renderer.getX() + 0.5;
		panelPos.y = renderer.getY() + 0.5;
		panelPos.z = renderer.getZ() + 0.5;

		Vector3d panelView = new Vector3d();
		panelView.x = playerPos.x - panelPos.x;
		panelView.y = playerPos.y - panelPos.y;
		panelView.z = playerPos.z - panelPos.z;

		// Cursor plane tracks the rendered panel: LP1 used 0.44 for a 0.4 panel offset.
		panelPos.add(panelView, panelOffset(renderer) + 0.04D);

		double d = panelPos.x * panelView.x + panelPos.y * panelView.y + panelPos.z * panelView.z;
		double c = panelView.x * playerPos.x + panelView.y * playerPos.y + panelView.z * playerPos.z;
		double b = panelView.x * playerView.x + panelView.y * playerView.y + panelView.z * playerView.z;
		double a = (d - c) / b;

		Vector3d viewPos = new Vector3d();
		viewPos.x = playerPos.x + a * playerView.x - panelPos.x;
		viewPos.y = playerPos.y + a * playerView.y - panelPos.y;
		viewPos.z = playerPos.z + a * playerView.z - panelPos.z;

		Vector3d panelScalVector1 = new Vector3d();

		if (panelView.y == 0) {
			panelScalVector1.x = 0;
			panelScalVector1.y = 1;
			panelScalVector1.z = 0;
		} else {
			panelScalVector1 = panelView.getOrtogonal(-panelView.x, null, -panelView.z).makeVectorLength(1.0D);
		}

		Vector3d panelScalVector2 = new Vector3d();

		if (panelView.z == 0) {
			panelScalVector2.x = 0;
			panelScalVector2.y = 0;
			panelScalVector2.z = 1;
		} else {
			panelScalVector2 = panelView.getOrtogonal(1.0D, 0.0D, null).makeVectorLength(1.0D);
		}

		if (panelScalVector1.y == 0) {
			return new int[] {};
		}

		double cursorY = -viewPos.y / panelScalVector1.y;

		Vector3d restViewPos = viewPos.clone();
		restViewPos.x += cursorY * panelScalVector1.x;
		restViewPos.y = 0;
		restViewPos.z += cursorY * panelScalVector1.z;

		double cursorX;

		if (panelScalVector2.x == 0) {
			cursorX = restViewPos.z / panelScalVector2.z;
		} else {
			cursorX = restViewPos.x / panelScalVector2.x;
		}

		// 50 px = panel half-width in blocks (50 * scale), with LP1's 0.94 plane fudge (0.47/0.5).
		cursorX *= 50 / (47.0D * PANEL_SCALE);
		cursorY *= 50 / (47.0D * PANEL_SCALE);
		if (panelView.z < 0) {
			cursorX *= -1;
		}
		if (panelView.y < 0) {
			cursorY *= -1;
		}

		return new int[] { (int) cursorX, (int) cursorY };
	}

	public boolean displayRenderer() {
		if (!displayHUD()) {
			if (list.size() != 0) {
				clearList(true);
			}
		}
		return displayHUD();
	}

	private boolean displayHUD() {
		Minecraft mc = Minecraft.getInstance();
		return (playerWearsHUD() || debugHUD != null) && mc.gui.screen() == null && mc.options.getCameraType().isFirstPerson() && !mc.gui.hud.isHidden();
	}

	public void resetLasers() {
		lasers.clear();
	}

	public void setLasers(List<LaserData> newLasers) {
		lasers.clear();
		lasers.addAll(newLasers);
	}

	public boolean hasLasers() {
		return !lasers.isEmpty();
	}

	public static LogisticsHUDRenderer instance() {
		if (LogisticsHUDRenderer.renderer == null) {
			LogisticsHUDRenderer.renderer = new LogisticsHUDRenderer();
		}
		return LogisticsHUDRenderer.renderer;
	}
}
