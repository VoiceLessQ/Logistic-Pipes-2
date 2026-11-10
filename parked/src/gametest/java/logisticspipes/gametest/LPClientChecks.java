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

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.extensions.common.IClientBlockExtensions;

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
 * Registered by LogisticsPipes when lp.clientcheck is set.
 */
public final class LPClientChecks {

	private static final boolean ENABLED = Boolean.getBoolean("lp.clientcheck");
	private static final int SETTLE_TICKS = 100;

	private static int tick = -1;
	private static int failures = 0;
	private static int particleBaseline;
	private static boolean drawHud = false;
	private static boolean drawFont = false;
	private static BlockPos blockInfoAt = null;
	private static final boolean JEI_LOADED = net.minecraftforge.fml.ModList.get().isLoaded(LPConstants.jeiModID);
	private static final List<IHUDModuleRenderer> hudRenderers = new ArrayList<>();

	private LPClientChecks() {}

	private static void result(String name, boolean ok, String detail) {
		if (!ok) failures++;
		logisticspipes.LogisticsPipes.log.info("LPCHECK {} {}: {}", ok ? "PASS" : "FAIL", name, detail);
	}

	@SubscribeEvent
	public static void onClientTick(TickEvent.ClientTickEvent event) {
		if (!ENABLED || event.phase != TickEvent.Phase.END) return;
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.level == null) return;
		tick++;
		if (mc.getSingleplayerServer() == null) {
			remoteTick(mc);
			return;
		}
		try {
			switch (tick) {
				case SETTLE_TICKS - 20 -> facePipe(mc);
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
				case SETTLE_TICKS + 12 -> openCraftingExtensions(mc);
				case SETTLE_TICKS + 14 -> Screenshot.grab(mc.gameDirectory, "lpcheck_crafting.png", mc.getMainRenderTarget(),
						msg -> logisticspipes.LogisticsPipes.log.info("LPCHECK INFO crafting screenshot: {}", msg.getString()));
				case SETTLE_TICKS + 15 -> {
					mc.setScreen(null);
					startTheOneProbe(mc);
				}
				// TOP polls the server for probe info; give it time to answer and draw
				case SETTLE_TICKS + 55 -> {
					if (TOP_LOADED) Screenshot.grab(mc.gameDirectory, "lpcheck_top.png", mc.getMainRenderTarget(),
							msg -> logisticspipes.LogisticsPipes.log.info("LPCHECK INFO top screenshot: {}", msg.getString()));
				}
				case SETTLE_TICKS + 56 -> {
					mc.options.keyShift.setDown(false);
					startTubes(mc);
				}
				// let the client receive and mesh the tube chunks before the shot
				case SETTLE_TICKS + 86 -> Screenshot.grab(mc.gameDirectory, "lpcheck_tubes.png", mc.getMainRenderTarget(),
						msg -> logisticspipes.LogisticsPipes.log.info("LPCHECK INFO tubes screenshot: {}", msg.getString()));
				case SETTLE_TICKS + 87 -> startGlasses(mc);
				case SETTLE_TICKS + 117 -> {
					checkGlasses();
					checkHudPower(mc);
					checkHudBlockInfo(mc);
					Screenshot.grab(mc.gameDirectory, "lpcheck_glasses.png", mc.getMainRenderTarget(),
							msg -> logisticspipes.LogisticsPipes.log.info("LPCHECK INFO glasses screenshot: {}", msg.getString()));
				}
				case SETTLE_TICKS + 120 -> {
					if (!JEI_LOADED) {
						result("hud.blockInfo", blockInfoAt != null, "no JEI: panel render path ran " + (blockInfoAt != null ? "without error" : "and failed"));
					} else if (blockInfoAt != null) {
						Screenshot.grab(mc.gameDirectory, "lpcheck_blockinfo.png", mc.getMainRenderTarget(),
								msg -> logisticspipes.LogisticsPipes.log.info("LPCHECK INFO block info screenshot: {}", msg.getString()));
					}
					blockInfoAt = null;
					mc.options.keyShift.setDown(true);
				}
				case SETTLE_TICKS + 122 -> Screenshot.grab(mc.gameDirectory, "lpcheck_glasses_focus.png", mc.getMainRenderTarget(),
						msg -> logisticspipes.LogisticsPipes.log.info("LPCHECK INFO glasses focus screenshot: {}", msg.getString()));
				case SETTLE_TICKS + 123 -> {
					mc.options.keyShift.setDown(false);
					startSign(mc);
				}
				// the sign packet and teleport need a few server ticks
				case SETTLE_TICKS + 143 -> checkSign(mc);
				case SETTLE_TICKS + 144 -> Screenshot.grab(mc.gameDirectory, "lpcheck_sign.png", mc.getMainRenderTarget(),
						msg -> logisticspipes.LogisticsPipes.log.info("LPCHECK INFO sign screenshot: {}", msg.getString()));
				case SETTLE_TICKS + 145 -> finishTubes(mc);
				case SETTLE_TICKS + 146 -> openMonitor(mc);
				// two rendered frames so the popup has drawn before the shot
				case SETTLE_TICKS + 149 -> checkMonitor(mc);
				case SETTLE_TICKS + 151 -> openItemPicker(mc);
				case SETTLE_TICKS + 153 -> checkItemPicker(mc);
				// a frame with the typed text before reading pixels
				case SETTLE_TICKS + 155 -> checkItemPickerDrawn(mc);
				case SETTLE_TICKS + 156 -> openDiskPopup(mc);
				case SETTLE_TICKS + 158 -> checkDiskPopup(mc);
				case SETTLE_TICKS + 160 -> {
					Screenshot.grab(mc.gameDirectory, "lpcheck_disk.png", mc.getMainRenderTarget(),
							msg -> logisticspipes.LogisticsPipes.log.info("LPCHECK INFO disk screenshot: {}", msg.getString()));
					mc.setScreen(null);
				}
				case SETTLE_TICKS + 161 -> openOrderer(mc);
				case SETTLE_TICKS + 163 -> checkOrderer(mc);
				case SETTLE_TICKS + 165 -> checkOrdererDrawn(mc);
				case SETTLE_TICKS + 166 -> {
					checkOrdererPageKeys(mc);
					checkClientRecipeList(mc);
					checkHudModuleText(mc);
				}
				case SETTLE_TICKS + 167 -> openFluidPicker(mc);
				case SETTLE_TICKS + 169 -> checkFluidPicker(mc);
				case SETTLE_TICKS + 170 -> openGuideBook();
				case SETTLE_TICKS + 173 -> checkGuideBook(mc, "guide.menuDrawn", "lpcheck_guide_menu.png", 100);
				case SETTLE_TICKS + 174 -> guidePage("/dev_zero_guides/active_routing.md");
				case SETTLE_TICKS + 177 -> checkGuideBook(mc, "guide.pageDrawn", "lpcheck_guide_page.png", 100);
				// a missing page opens LP1's 404 page, the only shipped guide image
				case SETTLE_TICKS + 178 -> guidePage("/lpcheck_missing.md");
				case SETTLE_TICKS + 181 -> {
					checkGuideBook(mc, "guide.missingDrawn", "lpcheck_guide_404.png", 3);
					mc.setScreen(null);
				}
				case SETTLE_TICKS + 182 -> startRequestStation(mc);
				// routers need a few seconds to find each other before a request can route
				case SETTLE_TICKS + 242 -> openRequestTable(mc);
				case SETTLE_TICKS + 247 -> submitStickRequest(mc);
				case SETTLE_TICKS + 257 -> openWatchedMonitor(mc);
				// two rendered frames so the popup has drawn before the shot
				case SETTLE_TICKS + 260 -> checkWatchedMonitor(mc);
				case SETTLE_TICKS + 263 -> checkZoomedMonitor(mc);
				case SETTLE_TICKS + 400 -> checkStickDelivery(mc);
				case SETTLE_TICKS + 401 -> {
					mc.setScreen(null);
					drawFont = true;
				}
				case SETTLE_TICKS + 404 -> {
					checkFontGlyphs(mc);
					drawFont = false;
					startFinderStation(mc);
				}
				// pipe and chest reach the client before the request
				case SETTLE_TICKS + 414 -> sendSlotFinder(mc);
				case SETTLE_TICKS + 420 -> clickSlotFinder(mc);
				case SETTLE_TICKS + 426 -> checkSlotFinderAssigned(mc);
				case SETTLE_TICKS + 427 -> openSecurityStation(mc);
				case SETTLE_TICKS + 432 -> pressSecurityWidgets(mc);
				case SETTLE_TICKS + 437 -> checkSecurityStation(mc);
				case SETTLE_TICKS + 441 -> finishRequestStation(mc);
				case SETTLE_TICKS + 443 -> {
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

	/** Each orderer colour (CustomModelData 0..16) resolves to its own texture via the model overrides. */
	private static void checkOrdererColours(Minecraft mc) {
		StringBuilder bad = new StringBuilder();
		for (int v = 0; v <= 16; v++) {
			net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(logisticspipes.LPItems.remoteOrderer.get());
			if (v > 0) stack.getOrCreateTag().putInt(logisticspipes.recipes.OrdererDyeRecipe.COLOR_TAG, v);
			String tex = mc.getItemRenderer().getModel(stack, mc.level, mc.player, 0).getParticleIcon().contents().name().getPath();
			if (!tex.equals("items/remote_orderer/" + v)) bad.append(v).append("->").append(tex).append(' ');
		}
		result("orderer.colourModels", bad.length() == 0, bad.length() == 0 ? "17 variants" : bad.toString());
	}

	/** Filled fluid container bakes the fluid's still sprite through the stencil, like LP1. */
	private static void checkFluidContainerModel(Minecraft mc) {
		var water = logisticspipes.utils.FluidIdentifierStack.getFromStack(new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000));
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
		return net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(
				new net.minecraftforge.client.event.ScreenEvent.MouseButtonPressed.Pre(screen, x, y, button));
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

	private static final boolean TOP_LOADED = net.minecraftforge.fml.ModList.get().isLoaded(LPConstants.theOneProbeModID);

	/** TOP: LPText draws through the reflected RenderHelper, then the real overlay over a routed pipe for the screenshot. */
	private static void startTheOneProbe(Minecraft mc) throws ReflectiveOperationException {
		if (!TOP_LOADED) {
			logisticspipes.LogisticsPipes.log.info("LPCHECK INFO top: The One Probe not installed, skipped");
			return;
		}
		// the probe step waits 40 ticks; a pause menu from lost focus would cover the screenshot
		mc.options.pauseOnLostFocus = false;
		String problem = TheOneProbeClientCheck.drawElement(mc);
		result("top.drawElement", problem == null, problem == null ? "LPText drawn" : problem);
		for (LogisticsTileGenericPipe pipe : loadedPipes(mc)) {
			BlockPos pos = pipe.getBlockPos();
			if (pipe.pipe instanceof logisticspipes.pipes.basic.CoreRoutedPipe && mc.level.isEmptyBlock(pos.above()) && mc.level.isEmptyBlock(pos.above(2))) {
				TheOneProbeClientCheck.lookAtPipe(mc, pos);
				logisticspipes.LogisticsPipes.log.info("LPCHECK INFO top: looking down at {} {}", pipe.pipe.getClass().getSimpleName(), pos);
				return;
			}
		}
		result("top.overlay", false, "no routed pipe with two air blocks above");
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

	/** Crafting GUI with fluid x3, byproduct and cleanup extensions; the screenshot should show their collapsed icons. */
	private static void openCraftingExtensions(Minecraft mc) {
		for (LogisticsTileGenericPipe pipe : loadedPipes(mc)) {
			if (pipe.pipe instanceof logisticspipes.pipes.PipeItemsCraftingLogistics crafting) {
				mc.setScreen(new logisticspipes.gui.GuiCraftingPipe(mc.player, crafting.getLogisticsModule(), false, 3, new int[3], true, false, 1, false));
				return;
			}
		}
		result("craftingGui", false, "no crafting pipe loaded");
	}

	private static BlockPos tubeBase;
	private static net.minecraft.world.phys.Vec3 tubeReturn;
	private static float tubeYaw, tubePitch;

	/** High-speed curve and gain tubes on a sky platform; the screenshot should show both bending the right way. */
	private static void startTubes(Minecraft mc) {
		var server = mc.getSingleplayerServer();
		if (server == null) {
			result("tubes.placed", false, "no integrated server");
			return;
		}
		mc.options.pauseOnLostFocus = false;
		server.execute(() -> {
			var sp = server.getPlayerList().getPlayer(mc.player.getUUID());
			if (sp == null) {
				result("tubes.placed", false, "no server player");
				return;
			}
			tubeReturn = sp.position();
			tubeYaw = sp.getYRot();
			tubePitch = sp.getXRot();
			tubeBase = sp.blockPosition().above(40);
			BlockPos b = tubeBase;
			tubeCommand(sp, "gamemode creative");
			tubeCommand(sp, "time set noon");
			tubeCommand(sp, String.format("fill %d %d %d %d %d %d stone", b.getX() - 10, b.getY() - 1, b.getZ() - 10, b.getX() + 10, b.getY() - 1, b.getZ() + 6));
			tubeCommand(sp, String.format("fill %d %d %d %d %d %d air", b.getX() - 10, b.getY(), b.getZ() - 10, b.getX() + 10, b.getY() + 6, b.getZ() + 6));
			sp.getAbilities().flying = true;
			sp.onUpdateAbilities();
			String curve = placePipe(sp, "pipe_hs_curve", b.offset(-4, 0, 0), "HSTube");
			String gain = placePipe(sp, "pipe_hs_gain", b.offset(4, 0, 0), "HSTube");
			result("tubes.placed", curve == null && gain == null, "curve=" + (curve == null ? "ok" : curve) + " gain=" + (gain == null ? "ok" : gain));
			sp.teleportTo(sp.serverLevel(), b.getX() + 0.5, b.getY() + 5, b.getZ() + 7.5, 180f, 35f);
		});
	}

	/** Uses the pipe item from {@code stand} on the floor three blocks north, like a player would. */
	private static String placePipe(net.minecraft.server.level.ServerPlayer sp, String id, BlockPos stand, String classPrefix) {
		var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.tryParse("logisticspipes:" + id));
		var stack = new net.minecraft.world.item.ItemStack(item);
		if (stack.isEmpty()) return "item missing";
		sp.teleportTo(sp.serverLevel(), stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 180f, 30f);
		sp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
		BlockPos floor = stand.offset(0, -1, -3);
		var hit = new BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(floor).add(0, 0.5, 0), Direction.UP, floor, false);
		var res = stack.useOn(new net.minecraft.world.item.context.UseOnContext(sp, net.minecraft.world.InteractionHand.MAIN_HAND, hit));
		sp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
		for (int dx = -3; dx <= 3; dx++) {
			for (int dz = -6; dz <= 0; dz++) {
				if (sp.serverLevel().getBlockEntity(floor.offset(dx, 1, dz)) instanceof LogisticsTileGenericPipe pipe && pipe.pipe != null
						&& pipe.pipe.getClass().getSimpleName().startsWith(classPrefix)) {
					logisticspipes.LogisticsPipes.log.info("LPCHECK INFO placed: {} at {} ({})", pipe.pipe.getClass().getSimpleName(), pipe.getBlockPos(), res);
					return null;
				}
			}
		}
		return "not placed (" + res + ")";
	}

	private static void tubeCommand(net.minecraft.server.level.ServerPlayer sp, String command) {
		sp.server.getCommands().performPrefixedCommand(sp.createCommandSourceStack().withPermission(4).withSuppressedOutput(), command);
	}

	/** HUD glasses on the tube platform: two chest providers one behind the other plus a satellite; screenshots plain and shift-focused. */
	private static void startGlasses(Minecraft mc) {
		var server = mc.getSingleplayerServer();
		if (server == null || tubeBase == null) {
			result("glasses.placed", false, "no platform");
			return;
		}
		server.execute(() -> {
			var sp = server.getPlayerList().getPlayer(mc.player.getUUID());
			if (sp == null) {
				result("glasses.placed", false, "no server player");
				return;
			}
			BlockPos b = tubeBase;
			tubeCommand(sp, String.format("fill %d %d %d %d %d %d air", b.getX() - 10, b.getY(), b.getZ() - 10, b.getX() + 10, b.getY() + 6, b.getZ() + 6));
			glassesChest(sp, b.offset(0, 0, -3), net.minecraft.world.item.Items.DIAMOND, 5);
			glassesChest(sp, b.offset(1, 0, -6), net.minecraft.world.item.Items.REDSTONE, 32);
			String front = placePipe(sp, "pipe_provider", b.offset(0, 0, 1), "PipeItemsProvider");
			String back = placePipe(sp, "pipe_provider", b.offset(1, 0, -2), "PipeItemsProvider");
			String sat = placePipe(sp, "pipe_satellite", b.offset(2, 0, 1), "PipeItemsSatellite");
			requestBlock(sp, b.offset(-2, 0, -1), "logisticspipes:power_junction{powerLevel:1000000}");
			result("glasses.placed", front == null && back == null && sat == null,
					"front=" + (front == null ? "ok" : front) + " back=" + (back == null ? "ok" : back) + " satellite=" + (sat == null ? "ok" : sat));
			var glasses = new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.tryParse("logisticspipes:hud_glasses")));
			sp.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, glasses);
			// eye 1.62 above the floor, front provider centre 4 blocks north and 1.12 lower
			sp.teleportTo(sp.serverLevel(), b.getX() + 0.5, b.getY(), b.getZ() + 2.5, 180f, 15.6f);
		});
	}

	private static void glassesChest(net.minecraft.server.level.ServerPlayer sp, BlockPos pos, net.minecraft.world.item.Item item, int count) {
		sp.serverLevel().setBlock(pos, net.minecraft.world.level.block.Blocks.CHEST.defaultBlockState(), 3);
		if (sp.serverLevel().getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest) {
			chest.setItem(0, new net.minecraft.world.item.ItemStack(item, count));
		}
	}

	/** Item amount sign on the front provider's free south face, seen up close without glasses. */
	private static void startSign(Minecraft mc) {
		var server = mc.getSingleplayerServer();
		if (server == null || tubeBase == null) return;
		server.execute(() -> {
			var sp = server.getPlayerList().getPlayer(mc.player.getUUID());
			if (sp == null) return;
			BlockPos b = tubeBase;
			sp.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.item.ItemStack.EMPTY);
			BlockPos at = b.offset(0, 0, -2);
			boolean ok = sp.serverLevel().getBlockEntity(at) instanceof LogisticsTileGenericPipe pipe
					&& pipe.pipe instanceof logisticspipes.pipes.basic.CoreRoutedPipe;
			if (ok) {
				var sign = new logisticspipes.pipes.signs.ItemAmountPipeSign();
				sign.itemTypeInv.setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND));
				sign.amount = 64;
				((logisticspipes.pipes.basic.CoreRoutedPipe) ((LogisticsTileGenericPipe) sp.serverLevel().getBlockEntity(at)).pipe)
						.addPipeSign(Direction.SOUTH, sign, sp);
			}
			result("sign.placed", ok, "provider at " + at);
			// eye 1.62 above the floor, pipe centre 1.5 blocks north and 1.12 lower
			sp.teleportTo(sp.serverLevel(), b.getX() + 0.5, b.getY(), b.getZ(), 180f, 36.7f);
		});
	}

	/** The client copy of the provider must have received the sign. */
	private static void checkSign(Minecraft mc) {
		int signs = tubeBase != null && mc.level.getBlockEntity(tubeBase.offset(0, 0, -2)) instanceof LogisticsTileGenericPipe pipe
				&& pipe.pipe instanceof logisticspipes.pipes.basic.CoreRoutedPipe routed ? routed.getPipeSigns().size() : -1;
		result("sign.client", signs == 1, "signs=" + signs);
	}

	private static LogisticsBaseGuiScreen monitorHost;
	private static logisticspipes.gui.popup.RequestMonitorPopup monitor;

	/** 2026-07-02 batch: request monitor popup over a host screen, with a three-level order tree. */
	private static void openMonitor(Minecraft mc) {
		var table = new logisticspipes.pipes.PipeBlockRequestTable(logisticspipes.LPItems.requestTable.get());
		var root = new logisticspipes.routing.order.LinkedLogisticsOrderList();
		root.add(monitorOrder(net.minecraft.world.item.Items.CHEST, 4, true));
		var planks = new logisticspipes.routing.order.LinkedLogisticsOrderList();
		planks.add(monitorOrder(net.minecraft.world.item.Items.OAK_PLANKS, 32, false));
		var logs = new logisticspipes.routing.order.LinkedLogisticsOrderList();
		logs.add(monitorOrder(net.minecraft.world.item.Items.OAK_LOG, 8, false));
		planks.getSubOrders().add(logs);
		var iron = new logisticspipes.routing.order.LinkedLogisticsOrderList();
		iron.add(monitorOrder(net.minecraft.world.item.Items.IRON_INGOT, 2, false));
		root.getSubOrders().add(planks);
		root.getSubOrders().add(iron);
		table.watchedRequests.put(7, new logisticspipes.utils.tuples.Pair<>(null, root));
		monitorHost = new LogisticsBaseGuiScreen(176, 166, 0, 0) {
			@Override
			protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {}
		};
		mc.setScreen(monitorHost);
		monitor = new logisticspipes.gui.popup.RequestMonitorPopup(table, 7);
		monitorHost.setSubGui(monitor);
	}

	private static logisticspipes.routing.order.IOrderInfoProvider monitorOrder(net.minecraft.world.item.Item item, int count, boolean inProgress) {
		var display = logisticspipes.utils.item.ItemIdentifierStack.getFromStack(new net.minecraft.world.item.ItemStack(item, count));
		return new logisticspipes.routing.order.IOrderInfoProvider() {
			public boolean isFinished() { return false; }
			public logisticspipes.utils.item.ItemIdentifierStack getAsDisplayItem() { return display; }
			public ResourceType getType() { return inProgress ? ResourceType.CRAFTING : ResourceType.PROVIDER; }
			public int getRouterId() { return 1; }
			public boolean isInProgress() { return inProgress; }
			public boolean isWatched() { return true; }
			public void setWatched() {}
			public List<Float> getProgresses() { return List.of(); }
			public byte getMachineProgress() { return (byte) (inProgress ? 40 : 0); }
			public logisticspipes.utils.item.ItemIdentifier getTargetType() { return null; }
			public network.rs485.logisticspipes.world.DoubleCoordinates getTargetPosition() { return null; }
		};
	}

	/** Popup drew, wheel zooms, drag pans, Save as Image writes a tree PNG, ESC closes only the popup. */
	private static void checkMonitor(Minecraft mc) throws ReflectiveOperationException, java.io.IOException {
		Screenshot.grab(mc.gameDirectory, "lpcheck_monitor.png", mc.getMainRenderTarget(),
				msg -> logisticspipes.LogisticsPipes.log.info("LPCHECK INFO monitor screenshot: {}", msg.getString()));
		boolean drawn = monitorHost.hasSubGui() && monitor.getGuiGraphics() != null;
		result("monitor.drawn", drawn, "open=" + monitorHost.hasSubGui() + " rendered=" + (monitor.getGuiGraphics() != null));

		double cx = monitorHost.width / 2.0, cy = monitorHost.height / 2.0;
		Field zoomField = logisticspipes.gui.popup.RequestMonitorPopup.class.getDeclaredField("zoom");
		zoomField.setAccessible(true);
		Object zoomBefore = zoomField.get(monitor);
		monitorHost.mouseScrolled(cx, cy, -1);
		Object zoomAfter = zoomField.get(monitor);
		result("monitor.wheelZoom", zoomBefore != zoomAfter, zoomBefore + " -> " + zoomAfter);

		Field mapXField = logisticspipes.gui.popup.RequestMonitorPopup.class.getDeclaredField("guiMapX");
		mapXField.setAccessible(true);
		double mapBefore = mapXField.getDouble(monitor);
		monitorHost.mouseDragged(cx, cy, 0, 10, 0);
		double mapAfter = mapXField.getDouble(monitor);
		result("monitor.dragPan", mapAfter != mapBefore, "guiMapX " + mapBefore + " -> " + mapAfter);

		java.io.File shots = new java.io.File(mc.gameDirectory, "screenshots");
		java.util.Set<String> before = treeShots(shots);
		boolean clicked = monitorHost.mouseClicked(cx + 50, cy + 84, 0);
		java.util.Set<String> added = treeShots(shots);
		added.removeAll(before);
		String png = "none";
		boolean drawnTree = false;
		if (added.size() == 1) {
			png = added.iterator().next();
			try (var in = new java.io.FileInputStream(new java.io.File(shots, png));
					var image = com.mojang.blaze3d.platform.NativeImage.read(in)) {
				int clear = image.getPixelRGBA(0, 0), other = 0;
				for (int y = 0; y < image.getHeight(); y++) {
					for (int x = 0; x < image.getWidth(); x++) {
						if (image.getPixelRGBA(x, y) != clear) other++;
					}
				}
				drawnTree = other > 500;
				png += " " + image.getWidth() + "x" + image.getHeight() + " drawnPixels=" + other;
			}
		}
		result("monitor.savePng", clicked && drawnTree, "clicked=" + clicked + " png=" + png);

		monitorHost.keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0);
		boolean popupClosed = !monitorHost.hasSubGui();
		boolean hostOpen = mc.screen == monitorHost;
		mc.setScreen(null);
		result("monitor.escClosesPopupOnly", popupClosed && hostOpen, "popupClosed=" + popupClosed + " hostOpen=" + hostOpen);
	}

	private static java.util.Set<String> treeShots(java.io.File dir) {
		String[] names = dir.list((d, n) -> n.endsWith(".png") && n.contains("_tree"));
		return names == null ? new java.util.HashSet<>() : new java.util.HashSet<>(java.util.Arrays.asList(names));
	}

	private static LogisticsBaseGuiScreen popupHost;
	private static logisticspipes.gui.popup.SelectItemOutOfList itemPicker;
	private static logisticspipes.gui.popup.GuiDiskPopup diskPopup;

	private static LogisticsBaseGuiScreen openPopupHost(Minecraft mc) {
		popupHost = new LogisticsBaseGuiScreen(176, 166, 0, 0) {
			@Override
			protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {}
		};
		mc.setScreen(popupHost);
		return popupHost;
	}

	private static void typeInto(LogisticsBaseGuiScreen host, String text) {
		for (char c : text.toCharArray()) host.charTyped(c, 0);
	}

	private static void openItemPicker(Minecraft mc) {
		var items = new ArrayList<logisticspipes.utils.item.ItemIdentifierStack>();
		items.add(logisticspipes.utils.item.ItemIdentifierStack.getFromStack(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_INGOT)));
		items.add(logisticspipes.utils.item.ItemIdentifierStack.getFromStack(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.OAK_LOG)));
		itemPicker = new logisticspipes.gui.popup.SelectItemOutOfList(items, slot -> {});
		openPopupHost(mc).setSubGui(itemPicker);
	}

	/** The picker's search bar takes focus on click, types through the base screen and filters the list. */
	private static void checkItemPicker(Minecraft mc) throws ReflectiveOperationException {
		Field searchField = logisticspipes.gui.popup.SelectItemOutOfList.class.getDeclaredField("search");
		searchField.setAccessible(true);
		InputBar search = (InputBar) searchField.get(itemPicker);
		int left = popupHost.width / 2 - 156 / 2, bottom = popupHost.height / 2 + 188 / 2;
		popupHost.mouseClicked(left + 20, bottom - 15, 0);
		typeInto(popupHost, "irn");
		popupHost.keyPressed(GLFW.GLFW_KEY_BACKSPACE, 0, 0);
		typeInto(popupHost, "on");
		String text = search.getText();
		result("picker.searchTypes", "iron".equals(text), "focused=" + search.isFocused() + " text='" + text + "'");
		boolean iron = itemPicker.itemSearched(logisticspipes.utils.item.ItemIdentifier.get(net.minecraft.world.item.Items.IRON_INGOT, 0, null));
		boolean log = itemPicker.itemSearched(logisticspipes.utils.item.ItemIdentifier.get(net.minecraft.world.item.Items.OAK_LOG, 0, null));
		result("picker.searchFilters", iron && !log, "iron shown=" + iron + " log shown=" + log);
	}

	/** InputBar.drawTextBox must draw the box: its inner fill is black. */
	private static void checkItemPickerDrawn(Minecraft mc) {
		double scale = mc.getWindow().getGuiScale();
		int left = popupHost.width / 2 - 156 / 2, bottom = popupHost.height / 2 + 188 / 2;
		int x0 = (int) ((left + 10) * scale), x1 = (int) ((left + 90) * scale);
		int y0 = (int) ((bottom - 21) * scale), y1 = (int) ((bottom - 11) * scale);
		int black = 0, total = 0;
		try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
			for (int y = y0; y < y1; y++) {
				for (int x = x0; x < x1; x++) {
					total++;
					if ((image.getPixelRGBA(x, y) & 0xFFFFFF) == 0) black++;
				}
			}
		}
		result("picker.searchDrawn", black > total / 2, "black " + black + "/" + total);
		Screenshot.grab(mc.gameDirectory, "lpcheck_picker.png", mc.getMainRenderTarget(),
				msg -> logisticspipes.LogisticsPipes.log.info("LPCHECK INFO picker screenshot: {}", msg.getString()));
		mc.setScreen(null);
	}

	private static void openDiskPopup(Minecraft mc) {
		net.minecraft.world.item.ItemStack disk = new net.minecraft.world.item.ItemStack(logisticspipes.LPItems.disk.get());
		diskPopup = new logisticspipes.gui.popup.GuiDiskPopup(new logisticspipes.interfaces.IDiskProvider() {
			@Override public net.minecraft.world.item.ItemStack getDisk() { return disk; }
			@Override public int getX() { return 0; }
			@Override public int getY() { return 0; }
			@Override public int getZ() { return 0; }
			@Override public logisticspipes.utils.gui.ItemDisplay getItemDisplay() { return null; }
		});
		openPopupHost(mc).setSubGui(diskPopup);
	}

	/** Name field: click edits, typing, backspace and the cursor keys work; Enter/ESC/outside click would send a rename. */
	private static void checkDiskPopup(Minecraft mc) throws ReflectiveOperationException {
		int left = popupHost.width / 2 - 150 / 2, top = popupHost.height / 2 - 200 / 2;
		popupHost.mouseClicked(left + 20, top + 35, 0);
		typeInto(popupHost, "ab");
		popupHost.keyPressed(GLFW.GLFW_KEY_BACKSPACE, 0, 0);
		popupHost.keyPressed(GLFW.GLFW_KEY_LEFT, 0, 0);
		typeInto(popupHost, "x");
		Field name1 = logisticspipes.gui.popup.GuiDiskPopup.class.getDeclaredField("name1");
		Field name2 = logisticspipes.gui.popup.GuiDiskPopup.class.getDeclaredField("name2");
		name1.setAccessible(true);
		name2.setAccessible(true);
		String name = name1.get(diskPopup) + "|" + name2.get(diskPopup);
		result("disk.nameTypes", "Diskx|a".equals(name), "before|after cursor='" + name + "'");
	}

	private static logisticspipes.gui.orderer.NormalGuiOrderer orderer;

	/** No pipe at 0,0,0: the refresh packet finds none and returns. */
	private static void openOrderer(Minecraft mc) {
		orderer = new logisticspipes.gui.orderer.NormalGuiOrderer(0, 0, 0, mc.level.dimension().location(), mc.player);
		mc.setScreen(orderer);
	}

	/** The orderer search bar takes focus on click, types and filters. */
	private static void checkOrderer(Minecraft mc) throws ReflectiveOperationException {
		Field searchField = logisticspipes.gui.orderer.GuiOrderer.class.getDeclaredField("search");
		searchField.setAccessible(true);
		InputBar search = (InputBar) searchField.get(orderer);
		int left = orderer.getGuiLeft(), bottom = orderer.getGuiTop() + 240;
		orderer.mouseClicked(left + 20, bottom - 70, 0);
		typeInto(orderer, "irn");
		orderer.keyPressed(GLFW.GLFW_KEY_BACKSPACE, 0, 0);
		typeInto(orderer, "on");
		String text = search.getText();
		result("orderer.searchTypes", "iron".equals(text), "focused=" + search.isFocused() + " text='" + text + "'");
		boolean iron = orderer.itemSearched(logisticspipes.utils.item.ItemIdentifier.get(net.minecraft.world.item.Items.IRON_INGOT, 0, null));
		boolean log = orderer.itemSearched(logisticspipes.utils.item.ItemIdentifier.get(net.minecraft.world.item.Items.OAK_LOG, 0, null));
		result("orderer.searchFilters", iron && !log, "iron shown=" + iron + " log shown=" + log);
	}

	/** The orderer search box is drawn: right of the text its inner fill is black. */
	private static void checkOrdererDrawn(Minecraft mc) {
		double scale = mc.getWindow().getGuiScale();
		int left = orderer.getGuiLeft(), bottom = orderer.getGuiTop() + 240;
		int x0 = (int) ((left + 60) * scale), x1 = (int) ((left + 200) * scale);
		int y0 = (int) ((bottom - 76) * scale), y1 = (int) ((bottom - 65) * scale);
		int black = 0, total = 0;
		try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
			for (int y = y0; y < y1; y++) {
				for (int x = x0; x < x1; x++) {
					total++;
					if ((image.getPixelRGBA(x, y) & 0xFFFFFF) == 0) black++;
				}
			}
		}
		result("orderer.searchDrawn", black > total / 2, "black " + black + "/" + total);
		Screenshot.grab(mc.gameDirectory, "lpcheck_orderer.png", mc.getMainRenderTarget(),
				msg -> logisticspipes.LogisticsPipes.log.info("LPCHECK INFO orderer screenshot: {}", msg.getString()));
		// 100 items for the page-key check; the next frame computes maxPage
		try {
			Field searchField = logisticspipes.gui.orderer.GuiOrderer.class.getDeclaredField("search");
			searchField.setAccessible(true);
			InputBar search = (InputBar) searchField.get(orderer);
			search.setText("");
			search.setFocused(false);
		} catch (ReflectiveOperationException e) {
			logisticspipes.LogisticsPipes.log.info("LPCHECK INFO orderer search reset failed: {}", e.toString());
		}
		List<logisticspipes.utils.item.ItemIdentifierStack> items = new java.util.ArrayList<>();
		for (net.minecraft.world.item.Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
			if (item == net.minecraft.world.item.Items.AIR) continue;
			items.add(logisticspipes.utils.item.ItemIdentifier.get(item, 0, null).makeStack(1));
			if (items.size() == 100) break;
		}
		orderer.itemDisplay.setItemList(items);
	}

	/** Module HUD text as LP1 draws it: black async extractor lines, mod sink names cut to 12 characters. */
	private static void checkHudModuleText(Minecraft mc) {
		List<String> texts = new ArrayList<>();
		List<Integer> colours = new ArrayList<>();
		var recorder = new net.minecraft.client.gui.GuiGraphics(mc, mc.renderBuffers().bufferSource()) {
			@Override
			public int drawString(net.minecraft.client.gui.Font font, String text, int x, int y, int colour, boolean shadow) {
				texts.add(text);
				colours.add(colour);
				return 0;
			}
		};
		var previous = logisticspipes.utils.gui.SimpleGraphics.guiGraphics;
		logisticspipes.utils.gui.SimpleGraphics.guiGraphics = recorder;
		try {
			var extractor = new network.rs485.logisticspipes.module.AsyncExtractorModule();
			extractor.getHUDRenderer().renderContent(false);
			String extractorText = texts + " " + colours.stream().map(Integer::toHexString).toList();
			boolean extractorOk = texts.equals(List.of("Extract", "from:", "DEFAULT")) && colours.stream().allMatch(c -> (c & 0xFFFFFF) == 0);
			texts.clear();
			colours.clear();
			var sink = new logisticspipes.modules.ModuleModBasedItemSink();
			sink.modList.replaceContent(List.of("averyveryverylongmodid", "short"));
			sink.getHUDRenderer().renderContent(false);
			boolean sinkOk = texts.equals(List.of("averyveryver", "short"));
			result("hud.moduleText", extractorOk && sinkOk, "extractor " + extractorText + ", mod sink " + texts);
		} finally {
			logisticspipes.utils.gui.SimpleGraphics.guiGraphics = previous;
		}
	}

	private static BlockPos remoteTable;

	/** Joined to a dedicated server: only the checks that need a client without an integrated server. */
	private static void remoteTick(Minecraft mc) {
		try {
			switch (tick) {
				case SETTLE_TICKS -> {
					checkClientRecipeList(mc);
					remoteTable = mc.player.blockPosition().east(2);
					mc.player.connection.sendCommand(String.format("setblock %d %d %d logisticspipes:pipe{pipeIdName:\"logisticspipes:pipe_request_table\"}",
							remoteTable.getX(), remoteTable.getY(), remoteTable.getZ()));
				}
				case SETTLE_TICKS + 20 -> {
					checkRemoteTableRecipe(mc);
					mc.player.connection.sendCommand(String.format("setblock %d %d %d air", remoteTable.getX(), remoteTable.getY(), remoteTable.getZ()));
				}
				case SETTLE_TICKS + 25 -> {
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

	/** A remote client has no server recipe manager; the table's crafting preview must still find the stick recipe. */
	private static void checkRemoteTableRecipe(Minecraft mc) {
		if (!(mc.level.getBlockEntity(remoteTable) instanceof LogisticsTileGenericPipe tile
				&& tile.pipe instanceof logisticspipes.pipes.PipeBlockRequestTable table)) {
			result("recipes.remoteTable", false, "no client request table at " + remoteTable + " (is the player op?)");
			return;
		}
		var planks = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.OAK_PLANKS);
		table.matrix.setItem(1, planks.copy());
		table.matrix.setItem(4, planks.copy());
		table.cacheRecipe();
		var out = table.resultInv.getItem(0);
		result("recipes.remoteTable", out.is(net.minecraft.world.item.Items.STICK) && out.getCount() == 4, "preview=" + out);
	}

	/** The request table's client half reads the synced client recipe list; it must hold LP's own recipes. */
	private static void checkClientRecipeList(Minecraft mc) {
		var recipes = logisticspipes.utils.CraftingUtil.getRecipeList(mc.level);
		long pipes = recipes.stream().filter(r -> r.getResultItem(mc.level.registryAccess()).is(logisticspipes.LPItems.pipeBasic.get())).count();
		result("recipes.clientList", pipes > 0, recipes.size() + " crafting recipes, " + pipes + " for pipe_basic");
	}

	/** PgDn and PgUp page through the orderer's item list; PgUp on page 1 wraps to the last page. */
	private static void checkOrdererPageKeys(Minecraft mc) throws ReflectiveOperationException {
		Field pageField = logisticspipes.utils.gui.ItemDisplay.class.getDeclaredField("page");
		Field maxField = logisticspipes.utils.gui.ItemDisplay.class.getDeclaredField("maxPage");
		pageField.setAccessible(true);
		maxField.setAccessible(true);
		var display = orderer.itemDisplay;
		int max = maxField.getInt(display);
		int start = pageField.getInt(display);
		orderer.keyPressed(GLFW.GLFW_KEY_PAGE_DOWN, 0, 0);
		int down = pageField.getInt(display);
		orderer.keyPressed(GLFW.GLFW_KEY_PAGE_UP, 0, 0);
		int up = pageField.getInt(display);
		orderer.keyPressed(GLFW.GLFW_KEY_PAGE_UP, 0, 0);
		int wrap = pageField.getInt(display);
		result("orderer.pageKeys", max > 0 && start == 0 && down == 1 && up == 0 && wrap == max,
				"max=" + max + " start=" + start + " pgdn=" + down + " pgup=" + up + " wrap=" + wrap);
		mc.setScreen(null);
	}

	private static BlockPos requestBase;
	private static logisticspipes.gui.orderer.GuiRequestTable requestGui;

	/** Station D on the sky platform with a request table that has a crafting monitoring upgrade. */
	private static void startRequestStation(Minecraft mc) {
		var server = mc.getSingleplayerServer();
		if (server == null || tubeBase == null) {
			result("request.placed", false, "no platform");
			return;
		}
		server.execute(() -> {
			var sp = server.getPlayerList().getPlayer(mc.player.getUUID());
			if (sp == null) return;
			BlockPos b = tubeBase;
			tubeCommand(sp, String.format("fill %d %d %d %d %d %d stone", b.getX() - 10, b.getY() - 1, b.getZ() - 10, b.getX() + 10, b.getY() - 1, b.getZ() + 6));
			BlockPos r = requestBase = b.offset(-2, 0, -6);
			requestBlock(sp, r, "chest{Items:[{Slot:0b,id:\"minecraft:oak_planks\",Count:32b}]}");
			requestBlock(sp, r.offset(0, 0, 1), "logisticspipes:pipe{pipeIdName:\"logisticspipes:pipe_chassis_mk1\",\"modules.listSize\":1,\"modules.listItem.0\":{\"slotted_module.slot\":0,\"slotted_module.name\":\"provider\"}}");
			requestBlock(sp, r.offset(0, 0, 2), "logisticspipes:power_junction");
			requestBlock(sp, r.offset(0, 0, 3), "logisticspipes:creative_power_source");
			requestBlock(sp, r.offset(1, 0, 1), "logisticspipes:pipe{pipeIdName:\"logisticspipes:pipe_transport_basic\"}");
			requestBlock(sp, r.offset(2, 0, 1), "logisticspipes:pipe{pipeIdName:\"logisticspipes:pipe_request_table\"}");
			requestBlock(sp, r.offset(3, 0, 1), "logisticspipes:pipe{pipeIdName:\"logisticspipes:pipe_transport_basic\"}");
			requestBlock(sp, r.offset(4, 0, 0), "logisticspipes:crafting_table{matrixitemsCount:9,matrixitems:[{index:1,id:\"minecraft:oak_planks\",Count:1b},{index:4,id:\"minecraft:oak_planks\",Count:1b}]}");
			requestBlock(sp, r.offset(4, 0, 1), "logisticspipes:pipe{pipeIdName:\"logisticspipes:pipe_crafting\",dummyInvitemsCount:11,dummyInvitems:[{index:1,id:\"minecraft:oak_planks\",Count:1b},{index:4,id:\"minecraft:oak_planks\",Count:1b},{index:9,id:\"minecraft:stick\",Count:4b}]}");
			boolean monitoring = false;
			if (sp.serverLevel().getBlockEntity(r.offset(2, 0, 1)) instanceof LogisticsTileGenericPipe tile
					&& tile.pipe instanceof logisticspipes.pipes.PipeBlockRequestTable table) {
				table.getOriginalUpgradeManager().inv.setItem(0, new net.minecraft.world.item.ItemStack(logisticspipes.LPRegistries.UPGRADE_CRAFTING_MONITORING.get()));
				table.getOriginalUpgradeManager().inv.setChanged();
				monitoring = table.getUpgradeManager().hasCraftingMonitoringUpgrade();
			}
			var placed = sp.serverLevel().getBlockEntity(r.offset(2, 0, 1));
			result("request.placed", monitoring, "monitoring upgrade=" + monitoring + " at table: "
					+ (placed instanceof LogisticsTileGenericPipe tile ? String.valueOf(tile.pipe) : String.valueOf(placed)));
			// eye 1.62 above the floor, table centre 2.5 blocks north and 1.12 lower
			sp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
			sp.teleportTo(sp.serverLevel(), r.getX() + 2.5, r.getY(), r.getZ() + 4, 180f, 24f);
		});
	}

	private static void requestBlock(net.minecraft.server.level.ServerPlayer sp, BlockPos pos, String block) {
		tubeCommand(sp, String.format("setblock %d %d %d %s", pos.getX(), pos.getY(), pos.getZ(), block));
	}

	/** Right-clicks the table like a player, so the server adds the player as a GUI watcher. */
	private static void openRequestTable(Minecraft mc) {
		if (requestBase == null) return;
		BlockPos table = requestBase.offset(2, 0, 1);
		var hit = new BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(table).add(0, 0, 0.5), Direction.SOUTH, table, false);
		mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
	}

	/** Same packet the table's Request button sends; sticks are crafted, so the order has a sub-tree. */
	private static void submitStickRequest(Minecraft mc) {
		requestGui = mc.screen instanceof logisticspipes.gui.orderer.GuiRequestTable gui ? gui : null;
		result("request.guiOpens", requestGui != null, "screen=" + (mc.screen == null ? "none" : mc.screen.getClass().getSimpleName()));
		if (requestGui == null) return;
		var sticks = logisticspipes.utils.item.ItemIdentifier.get(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK)).makeStack(8);
		logisticspipes.proxy.MainProxy.sendPacketToServer(logisticspipes.network.PacketHandler.getPacket(logisticspipes.network.packets.orderer.RequestSubmitPacket.class)
				.setStack(sticks).setTilePos(requestGui._table.container).setDimension(requestGui.dimension));
	}

	/** The watched order reached the client; open its monitor the way the table's monitor button does. */
	private static void openWatchedMonitor(Minecraft mc) {
		if (requestGui == null) return;
		var watched = requestGui._table.watchedRequests;
		Integer id = watched.isEmpty() ? null : watched.keySet().iterator().next();
		var orders = id == null ? null : watched.get(id).getValue2();
		boolean tree = orders != null && !orders.isEmpty() && !orders.getSubOrders().isEmpty();
		result("request.watched", tree, "watched=" + watched.size() + (orders == null ? "" : " roots=" + orders.size() + " subOrders=" + orders.getSubOrders().size()
				+ " treeSize=" + orders.getTreeRootSize() + " resource=" + watched.get(id).getValue1()));
		// dismiss the "Request successful" popup first, as the player does with OK
		if (requestGui.hasSubGui()) requestGui.keyPressed(GLFW.GLFW_KEY_ESCAPE, 0, 0);
		if (id != null) requestGui.setSubGui(new logisticspipes.gui.popup.RequestMonitorPopup(requestGui._table, id));
	}

	private static int treeGreen;

	private static void checkWatchedMonitor(Minecraft mc) throws ReflectiveOperationException {
		if (requestGui == null) return;
		boolean drawn = mc.screen == requestGui && requestGui.hasSubGui()
				&& requestGui.getSubGui() instanceof logisticspipes.gui.popup.RequestMonitorPopup popup && popup.getGuiGraphics() != null;
		result("request.monitorDrawn", drawn, "screen=" + (mc.screen == requestGui) + " popup="
				+ (requestGui.hasSubGui() ? requestGui.getSubGui().getClass().getSimpleName() : "none"));
		Screenshot.grab(mc.gameDirectory, "lpcheck_request_monitor.png", mc.getMainRenderTarget(),
				msg -> logisticspipes.LogisticsPipes.log.info("LPCHECK INFO request monitor screenshot: {}", msg.getString()));
		if (!drawn) {
			mc.setScreen(null);
			return;
		}
		// LP1 tiles stone behind the tree; without it the request table shows through the map
		int[] hole = monitorHolePixels(mc, requestGui.getSubGui());
		result("request.monitorBackground", hole[0] > hole[2] * 7 / 10, "stone-grey " + hole[0] + "/" + hole[2]);
		// the table's own text and item icons must not draw over the popup
		result("request.popupCoversTable", hole[3] > hole[4] * 95 / 100, "plain frame band " + hole[3] + "/" + hole[4]);
		treeGreen = hole[1];
		int countBefore = requestGui.itemDisplay.getRequestCount();
		requestGui.mouseScrolled(requestGui.width / 2.0, requestGui.height / 2.0, -1);
		int countAfter = requestGui.itemDisplay.getRequestCount();
		result("request.wheelStaysInPopup", countBefore == countAfter, "table request amount " + countBefore + " -> " + countAfter);
	}

	/** Zooming out draws the tree smaller, so less of the hole is green. */
	private static void checkZoomedMonitor(Minecraft mc) throws ReflectiveOperationException {
		if (requestGui == null || mc.screen != requestGui || !requestGui.hasSubGui()) return;
		int[] hole = monitorHolePixels(mc, requestGui.getSubGui());
		result("request.monitorZoomScales", treeGreen > 0 && hole[1] < treeGreen * 6 / 10, "green " + treeGreen + " -> " + hole[1]);
		Screenshot.grab(mc.gameDirectory, "lpcheck_request_monitor_zoom.png", mc.getMainRenderTarget(),
				msg -> logisticspipes.LogisticsPipes.log.info("LPCHECK INFO request monitor zoom screenshot: {}", msg.getString()));
		checkSaveTreeImage(mc, requestGui.getSubGui());
		mc.setScreen(null);
	}

	/** The popup's "Save as Image" button writes a PNG of the whole tree to screenshots/. */
	private static void checkSaveTreeImage(Minecraft mc, logisticspipes.utils.gui.SubGuiScreen popup) {
		java.io.File dir = new java.io.File(mc.gameDirectory, "screenshots");
		java.io.File[] before = dir.listFiles((d, n) -> n.endsWith("_tree.png") || n.matches(".*_tree_\\d+\\.png"));
		java.util.Set<String> old = new java.util.HashSet<>();
		if (before != null) for (java.io.File f : before) old.add(f.getName());
		var button = popup.children().stream()
				.filter(c -> c instanceof net.minecraft.client.gui.components.AbstractButton b && b.getMessage().getString().equals("Save as Image"))
				.map(c -> (net.minecraft.client.gui.components.AbstractButton) c).findFirst();
		if (button.isEmpty()) {
			result("request.saveImage", false, "no Save as Image button");
			return;
		}
		button.get().onPress();
		java.io.File[] after = dir.listFiles((d, n) -> (n.endsWith("_tree.png") || n.matches(".*_tree_\\d+\\.png")) && !old.contains(n));
		if (after == null || after.length == 0) {
			result("request.saveImage", false, "no new _tree.png");
			return;
		}
		int green = 0, drawn = 0, total = 0;
		try (var in = new java.io.FileInputStream(after[0]); var image = com.mojang.blaze3d.platform.NativeImage.read(in)) {
			for (int y = 0; y < image.getHeight(); y++) {
				for (int x = 0; x < image.getWidth(); x++) {
					int p = image.getPixelRGBA(x, y);
					int r = p & 0xFF, g = (p >> 8) & 0xFF, b = (p >> 16) & 0xFF;
					total++;
					// clear colour is 0.15 grey (38,38,38)
					if (Math.abs(r - 38) > 6 || Math.abs(g - 38) > 6 || Math.abs(b - 38) > 6) drawn++;
					if (g > 150 && r < 100 && b < 100) green++;
				}
			}
		} catch (java.io.IOException e) {
			result("request.saveImage", false, "unreadable " + after[0].getName() + ": " + e);
			return;
		}
		result("request.saveImage", green > 0 && drawn > 200, after[0].getName() + " green " + green + " drawn " + drawn + "/" + total);
	}

	/** Stone-grey, tree-green and total pixel counts inside the monitor's map hole, then plain and total in the frame's top band. */
	private static int[] monitorHolePixels(Minecraft mc, logisticspipes.utils.gui.SubGuiScreen popup) throws ReflectiveOperationException {
		Field xField = logisticspipes.utils.gui.SubGuiScreen.class.getDeclaredField("xSize");
		Field yField = logisticspipes.utils.gui.SubGuiScreen.class.getDeclaredField("ySize");
		xField.setAccessible(true);
		yField.setAccessible(true);
		int left = (popup.width - xField.getInt(popup)) / 2, top = (popup.height - yField.getInt(popup)) / 2;
		double scale = mc.getWindow().getGuiScale();
		int x0 = (int) ((left + 22) * scale), x1 = (int) ((left + 234) * scale);
		int y0 = (int) ((top + 23) * scale), y1 = (int) ((top + 164) * scale);
		int grey = 0, green = 0, total = 0, band = 0, bandTotal = 0;
		try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
			// the frame's top band is plain (198,198,198) in gui_border.png
			for (int y = (int) ((top + 3) * scale); y < (int) ((top + 15) * scale); y++) {
				for (int x = x0; x < x1; x++) {
					int p = image.getPixelRGBA(x, y);
					bandTotal++;
					if (Math.abs((p & 0xFF) - 198) < 8 && Math.abs(((p >> 8) & 0xFF) - 198) < 8 && Math.abs(((p >> 16) & 0xFF) - 198) < 8) band++;
				}
			}
			for (int y = y0; y < y1; y++) {
				for (int x = x0; x < x1; x++) {
					int p = image.getPixelRGBA(x, y);
					int a = p & 0xFF, g = (p >> 8) & 0xFF, c = (p >> 16) & 0xFF;
					total++;
					int max = Math.max(a, Math.max(g, c)), min = Math.min(a, Math.min(g, c));
					if (max - min < 16 && min >= 55 && max <= 120) grey++;
					if (g > 150 && a < 100 && c < 100) green++;
				}
			}
		}
		return new int[] { grey, green, total, band, bandTotal };
	}

	/** The crafted sticks arrive in the table and the finished order leaves the client's watch list. */
	private static void checkStickDelivery(Minecraft mc) {
		var server = mc.getSingleplayerServer();
		if (server == null || requestBase == null) return;
		BlockPos at = requestBase.offset(2, 0, 1);
		int clientWatched = requestGui == null ? -1 : requestGui._table.watchedRequests.size();
		server.execute(() -> {
			int sticks = 0;
			if (server.overworld().getBlockEntity(at) instanceof LogisticsTileGenericPipe tile
					&& tile.pipe instanceof logisticspipes.pipes.PipeBlockRequestTable table) {
				for (int i = 0; i < table.inv.getContainerSize(); i++) {
					if (table.inv.getItem(i).is(net.minecraft.world.item.Items.STICK)) sticks += table.inv.getItem(i).getCount();
				}
			}
			result("request.delivered", sticks >= 8 && clientWatched == 0, "sticks in table=" + sticks + " client watched=" + clientWatched);
		});
	}

	private static void finishRequestStation(Minecraft mc) {
		var server = mc.getSingleplayerServer();
		if (server == null || tubeBase == null) return;
		server.execute(() -> {
			var sp = server.getPlayerList().getPlayer(mc.player.getUUID());
			if (sp == null) return;
			BlockPos b = tubeBase;
			tubeCommand(sp, String.format("fill %d %d %d %d %d %d air", b.getX() - 10, b.getY() - 1, b.getZ() - 10, b.getX() + 10, b.getY() + 6, b.getZ() + 6));
			sp.teleportTo(sp.serverLevel(), tubeReturn.x, tubeReturn.y, tubeReturn.z, tubeYaw, tubePitch);
		});
	}

	/** H4: the fluid-type picker opens as a popup listing the registered fluids. */
	private static void openFluidPicker(Minecraft mc) {
		openPopupHost(mc);
		logisticspipes.proxy.MainProxy.proxy.openFluidSelectGui(0);
	}

	private static void checkFluidPicker(Minecraft mc) throws ReflectiveOperationException {
		boolean open = popupHost.hasSubGui() && popupHost.getSubGui() instanceof logisticspipes.gui.popup.SelectItemOutOfList;
		int size = -1;
		boolean water = false;
		if (open) {
			Field candidate = logisticspipes.gui.popup.SelectItemOutOfList.class.getDeclaredField("candidate");
			candidate.setAccessible(true);
			List<?> list = (List<?>) candidate.get(popupHost.getSubGui());
			size = list.size();
			var waterId = logisticspipes.utils.FluidIdentifier.get(new net.minecraftforge.fluids.FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000)).getItemIdentifier();
			water = list.stream().anyMatch(o -> ((logisticspipes.utils.item.ItemIdentifierStack) o).getItem().equals(waterId));
		}
		result("fluidPicker.opens", open && water, "open=" + open + " fluids=" + size + " water=" + water);
		Screenshot.grab(mc.gameDirectory, "lpcheck_fluidpicker.png", mc.getMainRenderTarget(),
				msg -> logisticspipes.LogisticsPipes.log.info("LPCHECK INFO fluid picker screenshot: {}", msg.getString()));
		mc.setScreen(null);
	}

	/** H1: setScreen(null) skips onClose, so the book state is not saved to the held item. */
	private static void openGuideBook() {
		network.rs485.logisticspipes.guidebook.ItemGuideBook.openGuideBook(net.minecraft.world.InteractionHand.MAIN_HAND,
				new net.minecraft.world.item.ItemStack(logisticspipes.LPItems.getItemGuideBook()));
	}

	private static void guidePage(String page) throws ReflectiveOperationException {
		var book = Minecraft.getInstance().screen;
		java.lang.reflect.Method change = book.getClass().getDeclaredMethod("changePage", String.class);
		change.setAccessible(true);
		change.invoke(book, page);
	}

	/** Page contents draw light text and images over the dark pattern; a full-width image fills over a third. */
	private static void checkGuideBook(Minecraft mc, String name, String shot, int minFraction) throws ReflectiveOperationException {
		if (!(mc.screen instanceof network.rs485.logisticspipes.gui.guidebook.GuiGuideBook book)) {
			result(name, false, "screen=" + mc.screen);
			return;
		}
		Field areaField = book.getClass().getDeclaredField("visibleArea");
		areaField.setAccessible(true);
		var area = (network.rs485.logisticspipes.util.math.MutableRectangle) areaField.get(book);
		double scale = mc.getWindow().getGuiScale();
		int x0 = (int) (area.getX0() * scale), x1 = (int) (area.getX1() * scale);
		int y0 = (int) (area.getY0() * scale), y1 = (int) (area.getY1() * scale);
		int bright = 0, total = 0;
		try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
			for (int y = y0; y < y1; y++) {
				for (int x = x0; x < x1; x++) {
					int p = image.getPixelRGBA(x, y);
					total++;
					if (Math.max(p & 0xFF, Math.max((p >> 8) & 0xFF, (p >> 16) & 0xFF)) > 160) bright++;
				}
			}
		}
		result(name, bright > total / minFraction, "bright " + bright + "/" + total);
		Screenshot.grab(mc.gameDirectory, shot, mc.getMainRenderTarget(),
				msg -> logisticspipes.LogisticsPipes.log.info("LPCHECK INFO guide screenshot: {}", msg.getString()));
	}

	/** The HUD renderer must have picked up all three pipes as panels. */
	private static void checkGlasses() throws ReflectiveOperationException {
		Field f = logisticspipes.renderer.LogisticsHUDRenderer.class.getDeclaredField("list");
		f.setAccessible(true);
		int panels = ((java.util.List<?>) f.get(logisticspipes.renderer.LogisticsHUDRenderer.instance())).size();
		result("glasses.panels", panels >= 3, "panels=" + panels);
	}

	/** Removes the platform and tubes so the LP Testbed save stays as built. */
	private static void finishTubes(Minecraft mc) {
		var server = mc.getSingleplayerServer();
		if (server == null || tubeBase == null) return;
		server.execute(() -> {
			var sp = server.getPlayerList().getPlayer(mc.player.getUUID());
			if (sp == null) return;
			BlockPos b = tubeBase;
			sp.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.item.ItemStack.EMPTY);
			tubeCommand(sp, String.format("fill %d %d %d %d %d %d air", b.getX() - 10, b.getY() - 1, b.getZ() - 10, b.getX() + 10, b.getY() + 6, b.getZ() + 6));
			sp.teleportTo(sp.serverLevel(), tubeReturn.x, tubeReturn.y, tubeReturn.z, tubeYaw, tubePitch);
		});
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
	/** Turns to the nearest pipe so the particle check finds a rendered one, whatever rotation the save holds. */
	private static void facePipe(Minecraft mc) {
		var eye = mc.player.getEyePosition();
		net.minecraft.world.phys.Vec3 best = null;
		for (LogisticsTileGenericPipe pipe : loadedPipes(mc)) {
			var c = pipe.getBlockPos().getCenter();
			if (best == null || c.distanceToSqr(eye) < best.distanceToSqr(eye)) best = c;
		}
		if (best == null) return;
		var d = best.subtract(eye);
		float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
		float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z)));
		mc.player.setYRot(yaw);
		mc.player.setXRot(pitch);
		mc.player.yRotO = yaw;
		mc.player.xRotO = pitch;
	}

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

	/** HUD power readout: the glasses list holds the junction with its synced level, drawn as LP1 lays it out. */
	private static void checkHudPower(Minecraft mc) throws ReflectiveOperationException {
		Field f = logisticspipes.renderer.LogisticsHUDRenderer.class.getDeclaredField("list");
		f.setAccessible(true);
		var junction = ((java.util.List<?>) f.get(logisticspipes.renderer.LogisticsHUDRenderer.instance())).stream()
				.filter(p -> p instanceof logisticspipes.blocks.powertile.LogisticsPowerJunctionTileEntity)
				.map(p -> (logisticspipes.blocks.powertile.LogisticsPowerJunctionTileEntity) p).findFirst().orElse(null);
		if (junction == null) {
			result("hud.powerLevel", false, "junction not in the glasses list");
			return;
		}
		List<String> texts = new ArrayList<>();
		List<Integer> bars = new ArrayList<>();
		var recorder = new GuiGraphics(mc, mc.renderBuffers().bufferSource()) {
			@Override
			public int drawString(net.minecraft.client.gui.Font font, String text, int x, int y, int colour, boolean shadow) {
				texts.add(text);
				return 0;
			}

			@Override
			public void blit(net.minecraft.resources.ResourceLocation texture, int x, int y, float u, float v, int width, int height, int texWidth, int texHeight) {
				bars.add(height);
			}
		};
		var config = new logisticspipes.hud.HUDConfig(mc.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD));
		var renderer = junction.getRenderer();
		var previous = SimpleGraphics.guiGraphics;
		SimpleGraphics.guiGraphics = recorder;
		try {
			renderer.renderHeadUpDisplay(2, false, false, mc, config);
		} finally {
			SimpleGraphics.guiGraphics = previous;
		}
		// background tiles come first; then the 61 px frame and, at 50 %, a 59 - 29 = 30 px fill
		List<Integer> gauge = bars.size() < 2 ? bars : bars.subList(bars.size() - 2, bars.size());
		boolean ok = renderer.display(config) && texts.equals(List.of("Stored Energy:", "1 000 000 LP", "/ 2 000 000 LP")) && gauge.equals(List.of(61, 30));
		result("hud.powerLevel", ok, "display=" + renderer.display(config) + " texts=" + texts + " gauge=" + gauge);
	}

	/** Ctrl-hold block info: JEI tooltip and mod name for the chest, drawn as text plus the item; no panel without JEI. */
	private static void checkHudBlockInfo(Minecraft mc) throws ReflectiveOperationException {
		// power junction left of the view; the real render path runs on it for a few frames
		blockInfoAt = tubeBase.offset(-2, 0, -1);
		if (!JEI_LOADED) return;
		var chest = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.CHEST);
		List<Component> lines = logisticspipes.modplugins.jei.JEIPluginLoader.getInfo(chest);
		List<String> texts = new ArrayList<>();
		List<net.minecraft.world.item.ItemStack> items = new ArrayList<>();
		var recorder = new GuiGraphics(mc, mc.renderBuffers().bufferSource()) {
			@Override
			public int drawString(net.minecraft.client.gui.Font font, Component text, int x, int y, int colour, boolean shadow) {
				texts.add(text.getString());
				return 0;
			}

			@Override
			public void renderItem(net.minecraft.world.item.ItemStack stack, int x, int y) {
				items.add(stack);
			}
		};
		var draw = logisticspipes.renderer.LogisticsHUDRenderer.class.getDeclaredMethod("drawBlockInfo", Minecraft.class, net.minecraft.world.item.ItemStack.class, List.class, double.class);
		draw.setAccessible(true);
		var previous = SimpleGraphics.guiGraphics;
		SimpleGraphics.guiGraphics = recorder;
		int growingTexts;
		try {
			draw.invoke(null, mc, chest, lines, 0.5D);
			growingTexts = texts.size();
			draw.invoke(null, mc, chest, lines, 1.0D);
		} finally {
			SimpleGraphics.guiGraphics = previous;
		}
		List<String> expected = lines.stream().map(Component::getString).toList();
		boolean ok = lines.size() >= 2 && expected.get(0).equals("Chest") && "Minecraft".equals(net.minecraft.ChatFormatting.stripFormatting(expected.get(expected.size() - 1)))
				&& growingTexts == 0 && texts.equals(expected) && items.size() == 1 && items.get(0).is(net.minecraft.world.item.Items.CHEST);
		result("hud.blockInfo", ok, "lines=" + expected + " drawn=" + texts + " whileGrowing=" + growingTexts + " items=" + items);
	}

	private static final String FONT_SAMPLE = "Ag@Q";
	private static final int FONT_X = 10, FONT_Y = 10, FONT_STEP = 16;

	/** Guide font: each sample glyph on screen matches its BDF bitmap pixel for pixel (vanilla font or a broken atlas would not). */
	private static void checkFontGlyphs(Minecraft mc) {
		var font = network.rs485.logisticspipes.gui.font.FontParser.INSTANCE.read(
				new net.minecraft.resources.ResourceLocation(LPConstants.LP_MOD_ID, "fonts/ter-u12n.bdf"));
		int scale = (int) mc.getWindow().getGuiScale();
		List<String> bad = new ArrayList<>();
		try (var image = Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
			for (int k = 0; k < FONT_SAMPLE.length(); k++) {
				char c = FONT_SAMPLE.charAt(k);
				var glyph = font.getGlyphs().get(c);
				int[] want = inkBox(glyph.getWidth(), glyph.getHeight(), (x, y) -> glyph.getBitmap().get(y * glyph.getWidth() + x));
				int left = (FONT_X + k * FONT_STEP - 2) * scale, top = (FONT_Y - 4) * scale;
				int[] got = inkBox(FONT_STEP, 24, (x, y) -> bright(image.getPixelRGBA(left + x * scale + scale / 2, top + y * scale + scale / 2)));
				if (want == null || got == null || want[2] - want[0] != got[2] - got[0] || want[3] - want[1] != got[3] - got[1]) {
					bad.add(c + " box want=" + java.util.Arrays.toString(want) + " got=" + java.util.Arrays.toString(got));
					continue;
				}
				int mismatches = 0;
				for (int y = 0; y <= want[3] - want[1]; y++) {
					for (int x = 0; x <= want[2] - want[0]; x++) {
						boolean bit = glyph.getBitmap().get((want[1] + y) * glyph.getWidth() + want[0] + x);
						boolean px = bright(image.getPixelRGBA(left + (got[0] + x) * scale + scale / 2, top + (got[1] + y) * scale + scale / 2));
						if (bit != px) mismatches++;
					}
				}
				if (mismatches > 0) bad.add(c + " " + mismatches + " pixels differ");
			}
		}
		Screenshot.grab(mc.gameDirectory, "lpcheck_font.png", mc.getMainRenderTarget(),
				msg -> logisticspipes.LogisticsPipes.log.info("LPCHECK INFO font screenshot: {}", msg.getString()));
		result("guide.fontGlyphs", bad.isEmpty(), bad.isEmpty() ? FONT_SAMPLE + " match ter-u12n at gui scale " + scale : bad.toString());
	}

	private static boolean bright(int abgr) {
		return Math.max(abgr & 0xFF, Math.max((abgr >> 8) & 0xFF, (abgr >> 16) & 0xFF)) > 160;
	}

	/** Bounding box {x0, y0, x1, y1} of the set cells, or null when none are set. */
	private static int[] inkBox(int width, int height, java.util.function.BiPredicate<Integer, Integer> ink) {
		int[] box = null;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				if (!ink.test(x, y)) continue;
				if (box == null) box = new int[] { x, y, x, y };
				box[0] = Math.min(box[0], x);
				box[1] = Math.min(box[1], y);
				box[2] = Math.max(box[2], x);
				box[3] = Math.max(box[3], y);
			}
		}
		return box;
	}

	private static BlockPos finderBase;

	/** Slot finder and security station stand: chest (ender chest with EnderStorage) + supplier pipe, security station. */
	private static void startFinderStation(Minecraft mc) {
		var server = mc.getSingleplayerServer();
		if (server == null || requestBase == null) {
			result("slotFinder.placed", false, "no platform");
			return;
		}
		server.execute(() -> {
			var sp = server.getPlayerList().getPlayer(mc.player.getUUID());
			if (sp == null) return;
			BlockPos s = finderBase = requestBase.offset(-4, 0, 4);
			boolean ender = net.minecraftforge.fml.ModList.get().isLoaded(LPConstants.enderStorageModID);
			requestBlock(sp, s, ender ? LPConstants.enderStorageModID + ":ender_chest" : "chest");
			requestBlock(sp, s.east(), "logisticspipes:pipe{pipeIdName:\"logisticspipes:pipe_supplier\"}");
			requestBlock(sp, s.south(2), "logisticspipes:security_station");
			sp.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
			sp.teleportTo(sp.serverLevel(), s.getX() + 1.5, s.getY(), s.getZ() + 4.5, 180f, 20f);
			result("slotFinder.placed", sp.serverLevel().getBlockEntity(s.east()) instanceof LogisticsTileGenericPipe tile
					&& tile.pipe instanceof logisticspipes.pipes.PipeItemsSupplierLogistics, (ender ? "ender chest" : "chest") + " at " + s);
		});
	}

	/** Same packet the supplier GUI's Set button sends for pattern slot 0. */
	private static void sendSlotFinder(Minecraft mc) {
		if (finderBase == null) return;
		if (!(mc.level.getBlockEntity(finderBase.east()) instanceof LogisticsTileGenericPipe tile
				&& tile.pipe instanceof logisticspipes.pipes.PipeItemsSupplierLogistics supplier)) {
			result("slotFinder.endToEnd", false, "no client supplier pipe");
			return;
		}
		logisticspipes.proxy.MainProxy.sendPacketToServer(logisticspipes.network.PacketHandler.getPacket(logisticspipes.network.packets.pipe.SlotFinderOpenGuiPacket.class)
				.setSlot(0).setModulePos(supplier.getLogisticsModule()));
	}

	/** The server opened the chest and armed the overlay on this client; pick chest slot 4. */
	private static void clickSlotFinder(Minecraft mc) throws ReflectiveOperationException {
		if (finderBase == null) return;
		var overlay = logisticspipes.renderer.GuiOverlay.getInstance();
		int[] target = new int[3];
		String[] names = { "targetPosX", "targetPosY", "targetPosZ" };
		for (int i = 0; i < 3; i++) {
			Field f = overlay.getClass().getDeclaredField(names[i]);
			f.setAccessible(true);
			target[i] = f.getInt(overlay);
		}
		boolean armed = overlay.isOverlaySlotActive() && new BlockPos(target[0], target[1], target[2]).equals(finderBase);
		String screen = mc.screen == null ? "none" : mc.screen.getClass().getName();
		if (!(mc.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> chest) || !armed) {
			result("slotFinder.endToEnd", false, "screen=" + screen + " overlay active=" + overlay.isOverlaySlotActive() + " target=" + java.util.Arrays.toString(target));
			overlay.setOverlaySlotActive(false);
			return;
		}
		var slot = chest.getMenu().slots.get(4);
		click(chest, chest.getGuiLeft() + slot.x + 8, chest.getGuiTop() + slot.y + 8, 0);
		logisticspipes.LogisticsPipes.log.info("LPCHECK INFO slotFinder: overlay armed on {}, clicked slot 4", screen);
	}

	/** The click went back to the server and set the supplier's pattern slot 0 to chest slot 4. */
	private static void checkSlotFinderAssigned(Minecraft mc) {
		var server = mc.getSingleplayerServer();
		if (server == null || finderBase == null) return;
		boolean closed = mc.screen == null;
		boolean off = !logisticspipes.renderer.GuiOverlay.getInstance().isOverlaySlotActive();
		mc.setScreen(null);
		server.execute(() -> {
			int assigned = -1;
			if (server.overworld().getBlockEntity(finderBase.east()) instanceof LogisticsTileGenericPipe tile
					&& tile.pipe instanceof logisticspipes.pipes.PipeItemsSupplierLogistics supplier) {
				assigned = supplier.getLogisticsModule().slotAssignmentPattern.get(0);
			}
			result("slotFinder.endToEnd", assigned == 4 && closed && off, "pattern[0]=" + assigned + " screen closed=" + closed + " overlay off=" + off);
		});
	}

	/** Opens the station like a player; dev builds run with DEBUG, which shows the CC widgets without CC: Tweaked. */
	private static void openSecurityStation(Minecraft mc) {
		if (finderBase == null) return;
		BlockPos station = finderBase.south(2);
		var hit = new BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(station).add(0, 0, 0.5), Direction.SOUTH, station, false);
		mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
	}

	private static net.minecraft.client.gui.components.AbstractButton securityWidget(logisticspipes.gui.GuiSecurityStation gui, String name) throws ReflectiveOperationException {
		Field f = logisticspipes.gui.GuiSecurityStation.class.getDeclaredField(name);
		f.setAccessible(true);
		return (net.minecraft.client.gui.components.AbstractButton) f.get(gui);
	}

	/** CC widgets show, the allow-CC box toggles, Edit Table opens the access table popup; add computer id 2 there. */
	private static void pressSecurityWidgets(Minecraft mc) throws ReflectiveOperationException {
		if (!(mc.screen instanceof logisticspipes.gui.GuiSecurityStation gui)) {
			result("security.ccWidgets", false, "screen=" + (mc.screen == null ? "none" : mc.screen.getClass().getSimpleName()));
			return;
		}
		var allow = securityWidget(gui, "checkAllowCC");
		var edit = securityWidget(gui, "btnEditTable");
		boolean visible = allow.visible && edit.visible;
		allow.onPress();
		edit.onPress();
		var popup = gui.hasSubGui() ? gui.getSubGui() : null;
		boolean popupOpen = popup instanceof logisticspipes.gui.popup.GuiEditCCAccessTable;
		if (popupOpen) {
			for (String label : new String[] { "+", "+", network.rs485.logisticspipes.util.TextUtil.translate("gui.securitystation.popup.ccAccess.Add") }) {
				popup.children().stream().filter(w -> w instanceof net.minecraft.client.gui.components.AbstractButton b && b.getMessage().getString().equals(label))
						.findFirst().ifPresent(w -> ((net.minecraft.client.gui.components.AbstractButton) w).onPress());
			}
		}
		logisticspipes.LogisticsPipes.log.info("LPCHECK INFO security: widgets visible={} popup={}", visible, popupOpen);
		if (!visible || !popupOpen) result("security.ccWidgets", false, "widgets visible=" + visible + " popup=" + popup);
	}

	/** The station on the server took both edits: CC allowed, computer 2 excluded. */
	private static void checkSecurityStation(Minecraft mc) {
		var server = mc.getSingleplayerServer();
		boolean client = mc.screen instanceof logisticspipes.gui.GuiSecurityStation gui && gui.hasSubGui();
		Screenshot.grab(mc.gameDirectory, "lpcheck_security.png", mc.getMainRenderTarget(),
				msg -> logisticspipes.LogisticsPipes.log.info("LPCHECK INFO security screenshot: {}", msg.getString()));
		mc.setScreen(null);
		if (server == null || finderBase == null) return;
		server.execute(() -> {
			boolean allow = false;
			List<Integer> excluded = List.of();
			if (server.overworld().getBlockEntity(finderBase.south(2)) instanceof logisticspipes.blocks.LogisticsSecurityTileEntity tile) {
				allow = tile.allowCC;
				excluded = List.copyOf(tile.excludedCC);
			}
			result("security.ccWidgets", client && allow && excluded.equals(List.of(2)), "popup open=" + client + " server allowCC=" + allow + " excluded=" + excluded);
		});
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
	public static void onRenderLevel(net.minecraftforge.client.event.RenderLevelStageEvent event) {
		if (!ENABLED || blockInfoAt == null || event.getStage() != net.minecraftforge.client.event.RenderLevelStageEvent.Stage.AFTER_PARTICLES) return;
		try {
			var render = logisticspipes.renderer.LogisticsHUDRenderer.class.getDeclaredMethod("renderBlockInfo", BlockHitResult.class, int.class, com.mojang.blaze3d.vertex.PoseStack.class, net.minecraft.client.renderer.MultiBufferSource.class);
			render.setAccessible(true);
			var hit = new BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(blockInfoAt), Direction.SOUTH, blockInfoAt, false);
			render.invoke(logisticspipes.renderer.LogisticsHUDRenderer.instance(), hit, 100, event.getPoseStack(), Minecraft.getInstance().renderBuffers().bufferSource());
		} catch (ReflectiveOperationException e) {
			result("hud.blockInfoDrawn", false, e.toString());
			blockInfoAt = null;
		}
	}

	@SubscribeEvent
	public static void onRenderGui(RenderGuiEvent.Post event) {
		if (ENABLED && drawFont) drawFontSample(event.getGuiGraphics());
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

	private static void drawFontSample(GuiGraphics gg) {
		gg.fill(FONT_X - 4, FONT_Y - 6, FONT_X + FONT_SAMPLE.length() * FONT_STEP + 4, FONT_Y + 22, 0xff000000);
		GuiGraphics previous = SimpleGraphics.guiGraphics;
		SimpleGraphics.guiGraphics = gg;
		try {
			var renderer = network.rs485.logisticspipes.gui.font.LPFontRenderer.Factory.get("ter-u12n");
			for (int k = 0; k < FONT_SAMPLE.length(); k++) {
				renderer.drawString(String.valueOf(FONT_SAMPLE.charAt(k)), FONT_X + k * FONT_STEP, FONT_Y, 0xffffffff, java.util.Set.of(), 1f);
			}
		} finally {
			SimpleGraphics.guiGraphics = previous;
		}
	}
}
