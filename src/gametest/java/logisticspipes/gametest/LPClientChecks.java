package logisticspipes.gametest;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.BlockHitResult;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;

import org.lwjgl.glfw.GLFW;

import logisticspipes.LPConstants;
import logisticspipes.interfaces.IHUDModuleRenderer;
import logisticspipes.pipefxhandlers.EntitySparkleFX;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.QuickSortChestMarkerStorage;
import logisticspipes.utils.gui.InputBar;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.utils.gui.SimpleGraphics;
import network.rs485.logisticspipes.module.AsyncExtractorModule;

/**
 * Client checks for fixes that need a client (runClientCheck). Results are "LPCHECK PASS|FAIL" log lines;
 * the HUD check also saves run/screenshots/lpcheck_hud.png for a visual look. Quits when done.
 */
@EventBusSubscriber(modid = LPConstants.LP_MOD_ID, value = Dist.CLIENT)
public final class LPClientChecks {

	private static final boolean ENABLED = Boolean.getBoolean("lp.clientcheck");
	private static final int SETTLE_TICKS = 100;

	private static int tick = -1;
	private static int failures = 0;
	private static int particleBaseline;
	private static boolean drawHud = false;
	private static final List<IHUDModuleRenderer> hudRenderers = new ArrayList<>();

	private LPClientChecks() {}

	private static void result(String name, boolean ok, String detail) {
		if (!ok) failures++;
		logisticspipes.LogisticsPipes.log.info("LPCHECK {} {}: {}", ok ? "PASS" : "FAIL", name, detail);
	}

	@SubscribeEvent
	public static void onClientTick(ClientTickEvent.Post event) {
		if (!ENABLED) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return;
		tick++;
		try {
			switch (tick) {
				case SETTLE_TICKS -> {
					checkQuickSort(mc);
					checkInputBar(mc);
					checkSolidSides(mc);
					checkOrdererColours(mc);
					checkFluidContainerModel(mc);
					checkModuleGuis(mc);
					checkSlotFinder(mc);
					startParticleCheck(mc);
				}
				case SETTLE_TICKS + 3 -> finishParticleCheck(mc);
				case SETTLE_TICKS + 4 -> startHud();
				case SETTLE_TICKS + 8 -> Screenshot.grab(mc.gameDirectory, "lpcheck_hud.png", mc.getMainRenderTarget(),
						msg -> logisticspipes.LogisticsPipes.log.info("LPCHECK INFO hud screenshot: {}", msg.getString()));
				case SETTLE_TICKS + 9 -> {
					drawHud = false;
					openSideConfig(mc);
				}
				case SETTLE_TICKS + 11 -> Screenshot.grab(mc.gameDirectory, "lpcheck_sideconfig.png", mc.getMainRenderTarget(),
						msg -> logisticspipes.LogisticsPipes.log.info("LPCHECK INFO side config screenshot: {}", msg.getString()));
				case SETTLE_TICKS + 12 -> {
					mc.setScreen(null);
					drawHud = false;
					logisticspipes.LogisticsPipes.log.info("LPCHECK DONE failures={}", failures);
					mc.stop();
				}
				default -> {}
			}
		} catch (Throwable t) {
			result("driver", false, t.toString());
			logisticspipes.LogisticsPipes.log.error("LPCHECK driver crashed", t);
			mc.stop();
		}
	}

	/** Worklog item 3: markers stay on for chest screens (also across a swap), clear on close or other screens. */
	private static void checkQuickSort(Minecraft mc) {
		QuickSortChestMarkerStorage qs = QuickSortChestMarkerStorage.getInstance();
		var inv = mc.player.getInventory();

		qs.enable();
		mc.setScreen(new InventoryScreen(mc.player));
		result("quicksort.inventoryScreenDisables", !qs.isActivated(), "activated=" + qs.isActivated());
		mc.setScreen(null);

		qs.enable();
		mc.setScreen(new ContainerScreen(ChestMenu.threeRows(990, inv), inv, Component.literal("a")));
		boolean afterOpen = qs.isActivated();
		mc.setScreen(new ContainerScreen(ChestMenu.threeRows(991, inv), inv, Component.literal("b")));
		boolean afterSwap = qs.isActivated();
		mc.setScreen(null);
		boolean afterClose = qs.isActivated();
		result("quicksort.chestKeepsMarkers", afterOpen && afterSwap, "open=" + afterOpen + " swap=" + afterSwap);
		result("quicksort.closeClears", !afterClose, "activated after close=" + afterClose);
	}

	/** Each orderer colour (CUSTOM_MODEL_DATA 0..16) resolves to its own texture via the model overrides. */
	private static void checkOrdererColours(Minecraft mc) {
		StringBuilder bad = new StringBuilder();
		for (int v = 0; v <= 16; v++) {
			net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(logisticspipes.LPItems.remoteOrderer.get());
			if (v > 0) stack.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA, new net.minecraft.world.item.component.CustomModelData(v));
			String tex = mc.getItemRenderer().getModel(stack, mc.level, mc.player, 0).getParticleIcon().contents().name().getPath();
			if (!tex.equals("items/remote_orderer/" + v)) bad.append(v).append("->").append(tex).append(' ');
		}
		result("orderer.colourModels", bad.length() == 0, bad.length() == 0 ? "17 variants" : bad.toString());
	}

	/** Filled fluid container bakes the fluid's still sprite through the stencil, like LP1. */
	private static void checkFluidContainerModel(Minecraft mc) {
		var water = logisticspipes.utils.FluidIdentifierStack.getFromStack(new net.neoforged.neoforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000));
		net.minecraft.world.item.ItemStack filled = logisticspipes.proxy.SimpleServiceLocator.logisticsFluidManager.getFluidContainer(water).makeNormalStack();
		net.minecraft.world.item.ItemStack empty = new net.minecraft.world.item.ItemStack(logisticspipes.LPItems.fluidContainer.get());
		String filledTex = mc.getItemRenderer().getModel(filled, mc.level, mc.player, 0).getParticleIcon().contents().name().getPath();
		String emptyTex = mc.getItemRenderer().getModel(empty, mc.level, mc.player, 0).getParticleIcon().contents().name().getPath();
		result("fluidContainer.model", filledTex.equals("block/water_still") && emptyTex.equals("items/liquids/empty"),
				"filled=" + filledTex + " empty=" + emptyTex);
	}

	/** Kotlin module GUIs: shift-click fills a filter slot, hovering a fuzzy slot opens the flag popup, a click flips a flag. */
	private static void checkModuleGuis(Minecraft mc) throws ReflectiveOperationException {
		var inv = mc.player.getInventory();
		net.minecraft.world.item.ItemStack saved = inv.getItem(9);
		inv.setItem(9, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND));
		var provider = network.rs485.logisticspipes.gui.module.ProviderGui.create(inv, new logisticspipes.modules.ModuleProvider(), net.minecraft.world.item.ItemStack.EMPTY);
		mc.setScreen(provider);
		var container = (network.rs485.logisticspipes.inventory.container.ProviderContainer) provider.getMenu();
		net.minecraft.world.inventory.Slot from = container.slots.stream().filter(s -> s.container == inv && s.getContainerSlot() == 9).findFirst().orElseThrow();
		var slotClicked = net.minecraft.client.gui.screens.inventory.AbstractContainerScreen.class.getDeclaredMethod("slotClicked",
				net.minecraft.world.inventory.Slot.class, int.class, int.class, net.minecraft.world.inventory.ClickType.class);
		slotClicked.setAccessible(true);
		slotClicked.invoke(provider, from, from.index, 0, net.minecraft.world.inventory.ClickType.QUICK_MOVE);
		net.minecraft.world.item.ItemStack filter = container.getFilterSlots().get(0).getItem();
		mc.setScreen(null);
		inv.setItem(9, saved);
		result("moduleGui.shiftClickFilter", filter.is(net.minecraft.world.item.Items.DIAMOND), "filter0=" + filter);

		var sink = network.rs485.logisticspipes.gui.module.ItemSinkGui.create(inv, new logisticspipes.modules.ModuleItemSink(), net.minecraft.world.item.ItemStack.EMPTY, true, false);
		mc.setScreen(sink);
		var fuzzySlot = (network.rs485.logisticspipes.gui.widget.FuzzyItemSlot) sink.getMenu().slots.stream()
				.filter(s -> s instanceof network.rs485.logisticspipes.gui.widget.FuzzyItemSlot).findFirst().orElseThrow();
		GuiGraphics graphics = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
		sink.render(graphics, sink.getGuiLeft() + fuzzySlot.x + 8, sink.getGuiTop() + fuzzySlot.y + 8, 0F);
		graphics.flush();
		var selector = sink.getFuzzySelector();
		boolean opened = selector.getActive() && selector.getCurrentSlot() == fuzzySlot;
		var flag = fuzzySlot.getUsedFlags().iterator().next();
		var body = selector.getRelativeBody();
		boolean clicked = sink.mouseClicked(body.getRoundedLeft() + 7, body.getRoundedTop() + 10 + 10 * flag.ordinal(), 0);
		boolean flipped = network.rs485.logisticspipes.util.FuzzyUtil.INSTANCE.get(fuzzySlot.getFlagGetter().invoke(), flag);
		mc.setScreen(null);
		result("moduleGui.fuzzySelector", opened && clicked && flipped, "opened=" + opened + " clicked=" + clicked + " flipped " + flag + "=" + flipped);
	}

	/** Slot finder: a left click on a slot is consumed, picks it and closes; a click off-slot is consumed only. */
	private static void checkSlotFinder(Minecraft mc) {
		logisticspipes.renderer.GuiOverlay overlay = logisticspipes.renderer.GuiOverlay.getInstance();
		InventoryScreen screen = new InventoryScreen(mc.player);
		mc.setScreen(screen);
		overlay.setOverlaySlotActive(true);
		net.minecraft.world.inventory.Slot slot = screen.getMenu().slots.get(9);
		double sx = screen.getGuiLeft() + slot.x + 8, sy = screen.getGuiTop() + slot.y + 8;

		boolean offSlot = click(screen, 1, 1, 0);
		boolean rightClick = click(screen, sx, sy, 1);
		boolean stillActive = overlay.isOverlaySlotActive();
		logisticspipes.LogisticsPipes.log.info("LPCHECK INFO slotFinder: next SlotFinderNumberPacket targets 0,0,0; its debug 'Packet handling error' is expected");
		boolean onSlot = click(screen, sx, sy, 0);
		boolean closed = mc.screen == null;
		boolean deactivated = !overlay.isOverlaySlotActive();
		overlay.setOverlaySlotActive(false);
		mc.setScreen(null);
		result("slotFinder.click", offSlot && !rightClick && stillActive && onSlot && closed && deactivated,
				"offSlot=" + offSlot + " right=" + rightClick + " active=" + stillActive + " onSlot=" + onSlot + " closed=" + closed + " off=" + deactivated);
	}

	private static boolean click(net.minecraft.client.gui.screens.Screen screen, double x, double y, int button) {
		return net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(
				new net.neoforged.neoforge.client.event.ScreenEvent.MouseButtonPressed.Pre(screen, x, y, button)).isCanceled();
	}

	/** Worklog item 6: click inside focuses, editing keys work, click outside unfocuses. */
	private static void checkInputBar(Minecraft mc) {
		LogisticsBaseGuiScreen screen = new LogisticsBaseGuiScreen(176, 166, 0, 0) {
			@Override
			protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {}
		};
		InputBar bar = new InputBar(mc.font, screen, 10, 10, 100, 14);
		bar.handleClick(20, 15, 0);
		boolean focusedIn = bar.isFocused();
		bar.handleKey('a', 0);
		bar.handleKey('b', 0);
		bar.keyPressed(GLFW.GLFW_KEY_BACKSPACE, 0, 0);
		String text = bar.getValue();
		bar.handleClick(400, 400, 0);
		boolean focusedOut = bar.isFocused();
		result("inputbar.clickFocuses", focusedIn, "focused after inside click=" + focusedIn);
		result("inputbar.editingKeys", "a".equals(text), "text after a,b,backspace='" + text + "'");
		result("inputbar.clickOutsideUnfocuses", !focusedOut, "focused after outside click=" + focusedOut);
	}

	/** Disconnection popup over a routed pipe; the screenshot should show the pipe model. */
	private static void openSideConfig(Minecraft mc) {
		for (LogisticsTileGenericPipe pipe : loadedPipes(mc)) {
			if (pipe.pipe instanceof logisticspipes.pipes.basic.CoreRoutedPipe routed) {
				mc.setScreen(new logisticspipes.gui.popup.DisconnectionConfigurationPopup(routed, null));
				logisticspipes.LogisticsPipes.log.info("LPCHECK INFO side config opened at {}", pipe.getBlockPos());
				return;
			}
		}
		result("sideConfig", false, "no routed pipe loaded");
	}

	private static List<LogisticsTileGenericPipe> loadedPipes(Minecraft mc) {
		List<LogisticsTileGenericPipe> pipes = new ArrayList<>();
		ChunkPos center = mc.player.chunkPosition();
		for (int dx = -4; dx <= 4; dx++) {
			for (int dz = -4; dz <= 4; dz++) {
				LevelChunk chunk = mc.level.getChunkSource().getChunk(center.x + dx, center.z + dz, false);
				if (chunk == null) continue;
				for (BlockEntity be : chunk.getBlockEntities().values()) {
					if (be instanceof LogisticsTileGenericPipe pipe && pipe.pipe != null) pipes.add(pipe);
				}
			}
		}
		return pipes;
	}

	/** Worklog item 4: the client tick fills solidSidesCache on cold load (end caps). */
	private static void checkSolidSides(Minecraft mc) throws ReflectiveOperationException {
		Field cacheField = logisticspipes.renderer.state.PipeRenderState.class.getDeclaredField("solidSidesCache");
		cacheField.setAccessible(true);
		int pipes = 0, withSolid = 0, mismatched = 0;
		String firstMismatch = "";
		for (LogisticsTileGenericPipe pipe : loadedPipes(mc)) {
			pipes++;
			boolean[] cache = (boolean[]) cacheField.get(pipe.renderState);
			boolean any = false;
			for (Direction dir : Direction.values()) {
				BlockPos side = pipe.getBlockPos().relative(dir);
				boolean expected = mc.level.getBlockState(side).isFaceSturdy(mc.level, side, dir.getOpposite())
						&& !pipe.renderState.pipeConnectionMatrix.isConnected(dir);
				any |= expected;
				if (cache[dir.ordinal()] != expected && mismatched++ == 0) {
					firstMismatch = pipe.getBlockPos() + " " + dir;
				}
			}
			if (any) withSolid++;
		}
		result("solidSides.cacheMatchesWorld", pipes > 0 && withSolid > 0 && mismatched == 0,
				"pipes=" + pipes + " withSolidSide=" + withSolid + " mismatched=" + mismatched + " " + firstMismatch);
	}

	private static BlockPos particlePos;
	private static EntitySparkleFX sparkle;

	/** Worklog item 7: breaking a rendered pipe spawns LP model shards via addDestroyEffects. */
	private static void startParticleCheck(Minecraft mc) {
		ClientLevel level = mc.level;
		for (LogisticsTileGenericPipe pipe : loadedPipes(mc)) {
			if (pipe.renderState.cachedRenderer != null) {
				particlePos = pipe.getBlockPos();
				break;
			}
		}
		if (particlePos == null) {
			result("particles.destroyEffects", false, "no rendered pipe in range");
			return;
		}
		// textured sparkle sheet: a broken vertex format or texture throws during the next frames
		// placed in view so frustum culling does not skip render()
		var eye = mc.gameRenderer.getMainCamera().getPosition().add(mc.player.getLookAngle().scale(2));
		sparkle = new EntitySparkleFX(level, eye.x, eye.y, eye.z, 1F, 1F, 0.5F, 0F, 6);
		mc.particleEngine.add(sparkle);
		particleBaseline = Integer.parseInt(mc.particleEngine.countParticles());
		BlockState state = level.getBlockState(particlePos);
		BlockHitResult hit = new BlockHitResult(particlePos.getCenter(), Direction.UP, particlePos, false);
		result("particles.hitEffects", IClientBlockExtensions.of(state).addHitEffects(state, level, hit, mc.particleEngine),
				"pipe-icon crack particles at " + particlePos);
		boolean handled = IClientBlockExtensions.of(state).addDestroyEffects(state, level, particlePos, mc.particleEngine);
		if (!handled) {
			result("particles.destroyEffects", false, "addDestroyEffects returned false at " + particlePos);
			particlePos = null;
		}
	}

	private static void finishParticleCheck(Minecraft mc) {
		if (particlePos == null) return;
		result("particles.sparkle", sparkle.isAlive(), "sparkle in view rendered for 3 ticks");
		int now = Integer.parseInt(mc.particleEngine.countParticles());
		result("particles.destroyEffects", now > particleBaseline, "particles " + particleBaseline + " -> " + now + " at " + particlePos);
	}

	/** Worklog item 8: HUDAsyncExtractor text, drawn on screen for the screenshot. */
	private static void startHud() throws ReflectiveOperationException {
		AsyncExtractorModule defaultSide = new AsyncExtractorModule();
		AsyncExtractorModule north = new AsyncExtractorModule();
		// the public setter syncs to the server and needs an in-world module; set the property directly
		Field prop = AsyncExtractorModule.class.getDeclaredField("sneakyDirectionProp");
		prop.setAccessible(true);
		Object property = prop.get(north);
		property.getClass().getMethod("setValue", Object.class).invoke(property, Direction.NORTH);
		hudRenderers.clear();
		hudRenderers.add(defaultSide.getHUDRenderer());
		hudRenderers.add(north.getHUDRenderer());
		drawHud = true;
	}

	@SubscribeEvent
	public static void onRenderGui(RenderGuiEvent.Post event) {
		if (!ENABLED || !drawHud) return;
		GuiGraphics gg = event.getGuiGraphics();
		GuiGraphics previous = SimpleGraphics.guiGraphics;
		SimpleGraphics.guiGraphics = gg;
		try {
			int x = gg.guiWidth() / 2 - 60;
			int y = gg.guiHeight() / 2;
			for (IHUDModuleRenderer renderer : hudRenderers) {
				gg.pose().pushPose();
				gg.pose().translate(x, y, 0);
				gg.fill(-30, -30, 30, 30, 0xffc6c6c6);
				renderer.renderContent(false);
				gg.pose().popPose();
				x += 120;
			}
		} finally {
			SimpleGraphics.guiGraphics = previous;
		}
	}
}
