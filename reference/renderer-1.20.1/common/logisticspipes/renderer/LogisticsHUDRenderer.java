package logisticspipes.renderer;
import net.minecraft.client.gui.GuiGraphics;

import net.minecraft.client.gui.screens.Screen;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Quaternionf;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nonnull;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.HitResult;



import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;




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
		return Minecraft.getInstance().player != null && Minecraft.getInstance().player.getInventory() != null && Minecraft.getInstance().player.getInventory().armor != null && !Minecraft.getInstance().player.getInventory().armor.get(3).isEmpty()
				&& checkItemStackForHUD(Minecraft.getInstance().player.getInventory().armor.get(3));
	}

	private boolean checkItemStackForHUD(@Nonnull ItemStack stack) {
		if (stack.getItem() instanceof IHUDArmor) {
			return ((IHUDArmor) stack.getItem()).isEnabled(stack);
		}
		return false;
	}

	private boolean displayCross = false;

	//TODO: only load this once, rather than twice
	private static final ResourceLocation TEXTURE = new ResourceLocation("textures/gui/icons.png");

	public void renderPlayerDisplay(long renderTicks, GuiGraphics guiGraphics) {
		if (!displayRenderer()) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (displayHUD() && displayCross) {
			int width = mc.getWindow().getGuiScaledWidth();
			int height = mc.getWindow().getGuiScaledHeight();
			if (mc.gui != null && guiGraphics != null) {
				guiGraphics.blit(TEXTURE, width / 2 - 7, height / 2 - 7, 0, 0, 16, 16);
			}
		}
	}

	@OnlyIn(Dist.CLIENT)
	public void renderWorldRelative(long renderTicks, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
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
			config = new HUDConfig(mc.player.getInventory().armor.get(3));
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
							if (Screen.hasShiftDown()) { //if(Minecraft.getInstance().player.isCrouching()) {
								thisIsLast = renderer;
								displayCross = true;
							}
							cursorHandled = true;
						}
					}
				}
				RenderSystem.enableBlend();
				RenderSystem.defaultBlendFunc();
				if (thisIsLast != renderer) {
					displayOneView(renderer, config, partialTick, false, poseStack, bufferSource, packedLight);
				}
				poseStack.popPose();
			}
		}
		if (thisIsLast != null) {
			poseStack.pushPose();
			RenderSystem.disableBlend();
			RenderSystem.disableDepthTest();
			displayOneView(thisIsLast, config, partialTick, true, poseStack, bufferSource, packedLight);
			RenderSystem.enableBlend();
			RenderSystem.enableDepthTest();
			poseStack.popPose();
		}

		poseStack.pushPose();
		HitResult box = mc.hitResult;
		if (box != null && box.getType() == HitResult.Type.BLOCK) {
			if (Screen.hasControlDown()) {
				progress = Math.min(progress + (2 * Math.max(1, (int) Math.floor((System.currentTimeMillis() - last) / 50.0D))), 100);
			} else {
				progress = Math.max(progress - (2 * Math.max(1, (int) Math.floor((System.currentTimeMillis() - last) / 50.0D))), 0);
			}
			if (progress != 0) {
				// HUD world-space info panel — requires NEI/info provider not yet ported to 1.20.1
			}
		} else if (!Screen.hasControlDown()) {
			progress = 0;
		}
		poseStack.popPose();

		//Render Laser
		RenderSystem.disableDepthTest();
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		if (!lasers.isEmpty()) {
			RenderSystem.setShader(GameRenderer::getPositionColorShader);
			Tesselator tes = Tesselator.getInstance();
			BufferBuilder bb = tes.getBuilder();
			for (LaserData data : lasers) {
				poseStack.pushPose();
				double x = data.getPosX() + 0.5 - player.xo - ((player.getX() - player.xo) * partialTick);
				double y = data.getPosY() + 0.5 - player.yo - ((player.getY() - player.yo) * partialTick);
				double z = data.getPosZ() + 0.5 - player.zo - ((player.getZ() - player.zo) * partialTick);
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
				org.joml.Matrix4f mat = poseStack.last().pose();

				bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
				for (float i = 0; i < 6 * data.getLength(); i += 1.0f) {
					int[] c = getLaserColor(i, data.getConnectionType());
					float shift = 100f * i / 6f;
					float s = (data.isStartPipe() && i == 0) ? -6.0f : 0.0f;
					// Top
					bb.vertex(mat, 19.7f+shift, 3,-3).color(c[0],c[1],c[2],c[3]).endVertex();
					bb.vertex(mat,  3.0f+shift+s, 3,-3).color(c[0],c[1],c[2],c[3]).endVertex();
					bb.vertex(mat,  3.0f+shift+s, 3, 3).color(c[0],c[1],c[2],c[3]).endVertex();
					bb.vertex(mat, 19.7f+shift, 3, 3).color(c[0],c[1],c[2],c[3]).endVertex();
					// Bottom
					bb.vertex(mat, 19.7f+shift,-3, 3).color(c[0],c[1],c[2],c[3]).endVertex();
					bb.vertex(mat,  3.0f+shift+s,-3, 3).color(c[0],c[1],c[2],c[3]).endVertex();
					bb.vertex(mat,  3.0f+shift+s,-3,-3).color(c[0],c[1],c[2],c[3]).endVertex();
					bb.vertex(mat, 19.7f+shift,-3,-3).color(c[0],c[1],c[2],c[3]).endVertex();
					// +Z side
					bb.vertex(mat, 19.7f+shift, 3, 3).color(c[0],c[1],c[2],c[3]).endVertex();
					bb.vertex(mat,  3.0f+shift+s, 3, 3).color(c[0],c[1],c[2],c[3]).endVertex();
					bb.vertex(mat,  3.0f+shift+s,-3, 3).color(c[0],c[1],c[2],c[3]).endVertex();
					bb.vertex(mat, 19.7f+shift,-3, 3).color(c[0],c[1],c[2],c[3]).endVertex();
					// -Z side
					bb.vertex(mat, 19.7f+shift,-3,-3).color(c[0],c[1],c[2],c[3]).endVertex();
					bb.vertex(mat,  3.0f+shift+s,-3,-3).color(c[0],c[1],c[2],c[3]).endVertex();
					bb.vertex(mat,  3.0f+shift+s, 3,-3).color(c[0],c[1],c[2],c[3]).endVertex();
					bb.vertex(mat, 19.7f+shift, 3,-3).color(c[0],c[1],c[2],c[3]).endVertex();
				}
				if (data.isStartPipe()) {
					int[] c = getLaserColor(0, data.getConnectionType());
					bb.vertex(mat,-3, 3, 3).color(c[0],c[1],c[2],c[3]).endVertex();
					bb.vertex(mat,-3, 3,-3).color(c[0],c[1],c[2],c[3]).endVertex();
					bb.vertex(mat,-3,-3,-3).color(c[0],c[1],c[2],c[3]).endVertex();
					bb.vertex(mat,-3,-3, 3).color(c[0],c[1],c[2],c[3]).endVertex();
				}
				if (data.isFinalPipe()) {
					int[] c = getLaserColor(6 * (float) data.getLength() - 1, data.getConnectionType());
					float ex = 100.0f * data.getLength() + 3f;
					bb.vertex(mat,ex, 3,-3).color(c[0],c[1],c[2],c[3]).endVertex();
					bb.vertex(mat,ex, 3, 3).color(c[0],c[1],c[2],c[3]).endVertex();
					bb.vertex(mat,ex,-3, 3).color(c[0],c[1],c[2],c[3]).endVertex();
					bb.vertex(mat,ex,-3,-3).color(c[0],c[1],c[2],c[3]).endVertex();
				}
				BufferUploader.drawWithShader(bb.end());
				poseStack.popPose();
			}
		}
		RenderSystem.enableDepthTest();
		last = System.currentTimeMillis();
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

	private void displayOneView(IHeadUpDisplayRendererProvider renderer, IHUDConfig config, float partialTick, boolean shifted, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
		Minecraft mc = Minecraft.getInstance();
		Player player = mc.player;
		double x = renderer.getX() + 0.5 - player.xo - ((player.getX() - player.xo) * partialTick);
		double y = renderer.getY() + 0.5 - player.yo - ((player.getY() - player.yo) * partialTick);
		double z = renderer.getZ() + 0.5 - player.zo - ((player.getZ() - player.zo) * partialTick);
		// HUD sub-renderers draw into a GuiGraphics stashed on SimpleGraphics so their existing
		// guiGraphics.drawString/fill/renderItem calls work without a signature change.
		// The public 2-arg GuiGraphics ctor creates its own internal PoseStack, so we apply the
		// HUD billboard transforms to gg.pose() rather than the external poseStack.
		net.minecraft.client.gui.GuiGraphics previous = logisticspipes.utils.gui.SimpleGraphics.guiGraphics;
		if (bufferSource instanceof net.minecraft.client.renderer.MultiBufferSource.BufferSource bs) {
			net.minecraft.client.gui.GuiGraphics gg = new net.minecraft.client.gui.GuiGraphics(mc, bs);
			com.mojang.blaze3d.vertex.PoseStack ggPose = gg.pose();
			ggPose.pushPose();
			ggPose.translate((float) x, (float) y, (float) z);
			ggPose.mulPose(new Quaternionf().rotationX((float) Math.toRadians(90.0F)));
			ggPose.mulPose(new Quaternionf().rotationZ((float) Math.toRadians(getAngle(z, x) + 90)));
			ggPose.mulPose(new Quaternionf().rotationX((float) Math.toRadians((-1) * getAngle(Math.hypot(x, z), y - player.getEyeHeight()) + 180)));
			ggPose.translate(0.0F, 0.0F, -0.4F);
			ggPose.scale(0.01F, 0.01F, 1F);
			logisticspipes.utils.gui.SimpleGraphics.guiGraphics = gg;
			try {
				renderer.getRenderer().renderHeadUpDisplay(Math.hypot(x, Math.hypot(y, z)), false, shifted, mc, config);
			} finally {
				logisticspipes.utils.gui.SimpleGraphics.guiGraphics = previous;
				ggPose.popPose();
			}
		}
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

		panelPos.add(panelView, 0.44D);

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

		cursorX *= 50 / 0.47D;
		cursorY *= 50 / 0.47D;
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
		return (playerWearsHUD() || debugHUD != null) && mc.screen == null && mc.options.getCameraType().isFirstPerson() && !mc.options.hideGui;
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
