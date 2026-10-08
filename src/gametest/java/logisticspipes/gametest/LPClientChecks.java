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

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.extensions.common.IClientBlockExtensions;

import org.lwjgl.glfw.GLFW;

import logisticspipes.LPConstants;
import logisticspipes.interfaces.IHUDModuleRenderer;
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
					startParticleCheck(mc);
				}
				case SETTLE_TICKS + 3 -> finishParticleCheck(mc);
				case SETTLE_TICKS + 4 -> startHud();
				case SETTLE_TICKS + 8 -> Screenshot.grab(mc.gameDirectory, "lpcheck_hud.png", mc.getMainRenderTarget(),
						msg -> logisticspipes.LogisticsPipes.log.info("LPCHECK INFO hud screenshot: {}", msg.getString()));
				case SETTLE_TICKS + 12 -> {
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
		particleBaseline = Integer.parseInt(mc.particleEngine.countParticles());
		BlockState state = level.getBlockState(particlePos);
		boolean handled = IClientBlockExtensions.of(state).addDestroyEffects(state, level, particlePos, mc.particleEngine);
		if (!handled) {
			result("particles.destroyEffects", false, "addDestroyEffects returned false at " + particlePos);
			particlePos = null;
		}
	}

	private static void finishParticleCheck(Minecraft mc) {
		if (particlePos == null) return;
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
