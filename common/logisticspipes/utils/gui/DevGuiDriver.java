package logisticspipes.utils.gui;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;

import org.lwjgl.glfw.GLFW;

import logisticspipes.LogisticsPipes;

/**
 * Dev-only unattended GUI test driver, enabled with -Dlp.guitest=true or LP_GUITEST=1 in a dev environment.
 * Executes one line per client tick from run/guitest/in.txt (appended to while the client runs) and logs to out.txt.
 */
public final class DevGuiDriver {

	private static File dir;
	private static int lineIndex;
	private static int waitTicks;
	private static PrintWriter out;
	/** Modifier keys the world HUD treats as held ({@code hold shift|ctrl|none}); GLFW key state can't be faked. */
	public static boolean holdShift, holdControl;

	private DevGuiDriver() {}

	public static void registerIfEnabled() {
		if (!FabricLoader.getInstance().isDevelopmentEnvironment()) return;
		if (!Boolean.getBoolean("lp.guitest") && !"1".equals(System.getenv("LP_GUITEST"))) return;
		dir = new File(Minecraft.getInstance().gameDirectory, "guitest");
		dir.mkdirs();
		try {
			Files.writeString(new File(dir, "in.txt").toPath(), "");
			out = new PrintWriter(Files.newBufferedWriter(new File(dir, "out.txt").toPath(), StandardCharsets.UTF_8), true);
		} catch (IOException e) {
			LogisticsPipes.log.error("guitest: cannot init", e);
			return;
		}
		ClientTickEvents.END_CLIENT_TICK.register(DevGuiDriver::tick);
	}

	private static void tick(Minecraft mc) {
		if (waitTicks > 0) {
			waitTicks--;
			return;
		}
		List<String> lines;
		try {
			lines = Files.readAllLines(new File(dir, "in.txt").toPath());
		} catch (IOException e) {
			return;
		}
		if (lineIndex >= lines.size()) return;
		String line = lines.get(lineIndex++).trim();
		if (line.isEmpty() || line.startsWith("#")) return;
		log("> " + line);
		try {
			run(mc, line);
		} catch (Throwable t) {
			log("! " + t);
			LogisticsPipes.log.error("guitest: " + line, t);
		}
	}

	private static void run(Minecraft mc, String line) throws Exception {
		String[] a = line.split(" ");
		Screen screen = mc.gui.screen();
		switch (a[0]) {
			case "wait" -> waitTicks = Integer.parseInt(a[1]);
			case "cmd" -> {
				MinecraftServer server = mc.getSingleplayerServer();
				String command = line.substring(4);
				if (server == null) {
					mc.player.connection.sendCommand(command);
					return;
				}
				server.execute(() -> {
					ServerPlayer player = server.getPlayerList().getPlayers().get(0);
					server.getCommands().performPrefixedCommand(player.createCommandSourceStack().withPermission(PermissionSet.ALL_PERMISSIONS), command);
				});
			}
			case "sneak" -> mc.options.keyShift.setDown(a[1].equals("on"));
			case "use" -> {
				BlockPos pos = new BlockPos(Integer.parseInt(a[1]), Integer.parseInt(a[2]), Integer.parseInt(a[3]));
				Direction face = a.length > 4 ? Direction.byName(a[4]) : Direction.UP;
				Vec3 hit = Vec3.atCenterOf(pos).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
				log("  hand " + mc.player.getMainHandItem() + " slot " + mc.player.getInventory().getSelectedSlot());
				log("  " + mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(hit, face, pos, false)));
			}
			case "useitem" -> log("  " + mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND));
			case "click", "rclick", "dclick" -> {
				double x = Double.parseDouble(a[1]);
				double y = Double.parseDouble(a[2]);
				int button = a[0].equals("rclick") ? 1 : 0;
				int mods = a.length > 3 ? Integer.parseInt(a[3]) : 0;
				MouseButtonEvent event = new MouseButtonEvent(x, y, new MouseButtonInfo(button, mods));
				log("  pressed=" + screen.mouseClicked(event, a[0].equals("dclick")) + " released=" + screen.mouseReleased(event));
			}
			case "hover" -> {
				var w = mc.getWindow();
				var onMove = net.minecraft.client.MouseHandler.class.getDeclaredMethod("onMove", long.class, double.class, double.class);
				onMove.setAccessible(true);
				onMove.invoke(mc.mouseHandler, mc.getWindow().handle(), Double.parseDouble(a[1]) * w.getScreenWidth() / w.getGuiScaledWidth(), Double.parseDouble(a[2]) * w.getScreenHeight() / w.getGuiScaledHeight());
			}
			case "scroll" -> log("  " + screen.mouseScrolled(Double.parseDouble(a[1]), Double.parseDouble(a[2]), 0, Double.parseDouble(a[3])));
			case "key" -> {
				int key = Integer.parseInt(a[1]);
				int mods = a.length > 2 ? Integer.parseInt(a[2]) : 0;
				log("  " + screen.keyPressed(new KeyEvent(key, GLFW.glfwGetKeyScancode(key), mods)));
			}
			case "type" -> line.substring(5).codePoints().forEach(c -> screen.charTyped(new CharacterEvent(c)));
			case "shot" -> {
				String name = a[1] + ".png";
				Screenshot.grab(mc.gameDirectory, name, mc.gameRenderer.mainRenderTarget(), 1, c -> log("  shot " + name + ": " + c.getString()));
			}
			case "close" -> screen.onClose();
			case "scale" -> {
				mc.options.guiScale().set(Integer.parseInt(a[1]));
				mc.resizeGui();
			}
			case "quit" -> mc.stop();
			case "hold" -> {
				holdShift = a[1].equals("shift");
				holdControl = a[1].equals("ctrl");
			}
			case "crack" -> {
				BlockPos pos = new BlockPos(Integer.parseInt(a[1]), Integer.parseInt(a[2]), Integer.parseInt(a[3]));
				for (int i = 0; i < Integer.parseInt(a[5]); i++) {
					mc.level.addBreakingBlockEffect(pos, Direction.byName(a[4]));
				}
			}
			case "sparkle" -> logisticspipes.pipefxhandlers.PipeFXRenderHandler.spawnGenericParticle(logisticspipes.pipefxhandlers.Particles.valueOf(a[1]),
					Double.parseDouble(a[2]), Double.parseDouble(a[3]), Double.parseDouble(a[4]), Integer.parseInt(a[5]));
			case "attack" -> log("  " + mc.gameMode.startDestroyBlock(new BlockPos(Integer.parseInt(a[1]), Integer.parseInt(a[2]), Integer.parseInt(a[3])), Direction.byName(a[4])));
			case "camera" -> mc.options.setCameraType(net.minecraft.client.CameraType.values()[Integer.parseInt(a[1])]);
			case "inv" -> mc.gui.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(mc.player));
			case "dump" -> dump(mc, screen);
			case "scan" -> {
				int r = Integer.parseInt(a[1]);
				BlockPos.betweenClosed(mc.player.blockPosition().offset(-r, -r, -r), mc.player.blockPosition().offset(r, r, r)).forEach(p -> {
					var state = mc.level.getBlockState(p);
					if (!state.isAir() && !state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK) && !state.is(net.minecraft.world.level.block.Blocks.DIRT) && !state.is(net.minecraft.world.level.block.Blocks.BEDROCK)) {
						var be = mc.level.getBlockEntity(p);
						log("  " + p.toShortString() + " " + net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()) + (be instanceof logisticspipes.pipes.basic.LogisticsTileGenericPipe pipe && pipe.pipe != null ? " " + pipe.pipe.getClass().getSimpleName() : ""));
					}
				});
			}
			default -> log("! unknown command");
		}
	}

	private static void dump(Minecraft mc, Screen screen) {
		log("  hit " + (mc.hitResult instanceof BlockHitResult b ? b.getType() + " " + b.getBlockPos().toShortString() : mc.hitResult));
		log("  window " + mc.getWindow().getGuiScaledWidth() + "x" + mc.getWindow().getGuiScaledHeight() + " scale " + mc.getWindow().getGuiScale());
		if (screen == null) {
			log("  no screen");
			return;
		}
		log("  screen " + screen.getClass().getName() + " " + screen.width + "x" + screen.height);
		if (screen instanceof AbstractContainerScreen<?> cs) {
			int left = 0, top = 0;
			if (screen instanceof LogisticsBaseGuiScreen lp) {
				left = lp.getGuiLeft();
				top = lp.getGuiTop();
				log("  lp gui left/top " + left + "," + top + " size " + lp.getXSize() + "x" + lp.getYSize() + " subGui " + (lp.hasSubGui() ? lp.getSubGui().getClass().getName() : "-"));
			}
			for (Slot slot : cs.getMenu().slots) {
				log("  slot " + slot.index + " " + slot.getClass().getSimpleName() + " @" + (left + slot.x) + "," + (top + slot.y) + " " + slot.getItem());
			}
		}
		for (GuiEventListener child : screen.children()) {
			if (child instanceof AbstractWidget w) {
				log("  widget " + w.getClass().getSimpleName() + " @" + w.getX() + "," + w.getY() + " " + w.getWidth() + "x" + w.getHeight() + " '" + w.getMessage().getString() + "' active=" + w.active + " visible=" + w.visible);
			} else {
				log("  child " + child.getClass().getName());
			}
		}
	}

	private static void log(String s) {
		out.println(s);
		LogisticsPipes.log.info("guitest {}", s);
	}
}
