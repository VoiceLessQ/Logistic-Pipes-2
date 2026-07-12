package logisticspipes.network;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;

import net.minecraft.world.inventory.AbstractContainerMenu;

import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import logisticspipes.LogisticsPipes;
import logisticspipes.network.abstractguis.GuiProvider;
import logisticspipes.network.abstractguis.PopupGuiProvider;
import logisticspipes.network.exception.TargetNotFoundException;
import logisticspipes.network.packets.gui.OpenGUIPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.StaticResolverUtil;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.utils.gui.SubGuiScreen;
import network.rs485.logisticspipes.util.LPDataIOWrapper;

public class NewGuiHandler {

	public static List<GuiProvider> guilist;
	public static Map<Class<? extends GuiProvider>, GuiProvider> guimap;

	private NewGuiHandler() { }

	private static final Field CONTAINER_ID_FIELD;
	static {
		Field f;
		try {
			f = AbstractContainerMenu.class.getDeclaredField("containerId");
			f.setAccessible(true);
		} catch (NoSuchFieldException e) {
			// Try SRG name fallback
			try {
				f = AbstractContainerMenu.class.getDeclaredField("f_38840_");
				f.setAccessible(true);
			} catch (NoSuchFieldException e2) {
				throw new RuntimeException("Cannot find AbstractContainerMenu.containerId field", e2);
			}
		}
		CONTAINER_ID_FIELD = f;
	}

	@SuppressWarnings("unchecked") // Suppressed because this cast should never fail.
	public static <T extends GuiProvider> T getGui(Class<T> clazz) {
		return (T) NewGuiHandler.guimap.get(clazz).template();
	}

	public static void initialize() {
		Set<Class<? extends GuiProvider>> classes = StaticResolverUtil.findClassesByType(GuiProvider.class);

		loadGuiProviders(classes);

		if (NewGuiHandler.guilist == null || NewGuiHandler.guilist.isEmpty()) {
			throw new RuntimeException("Cannot load GuiProvider Classes");
		}
	}

	private static void loadGuiProviders(Set<Class<? extends GuiProvider>> classesIn) {
		List<Class<? extends GuiProvider>> classes = classesIn.stream()
				.sorted(Comparator.comparing(Class::getCanonicalName))
				.collect(Collectors.toList());

		NewGuiHandler.guilist = new ArrayList<>(classes.size());
		NewGuiHandler.guimap = new HashMap<>(classes.size());

		int currentId = 0;
		for (Class<? extends GuiProvider> cls : classes) {
			try {
				final GuiProvider instance = (GuiProvider) cls.getConstructors()[0].newInstance(currentId);
				NewGuiHandler.guilist.add(instance);
				NewGuiHandler.guimap.put(cls, instance);
				currentId++;
			} catch (Throwable ignoredButPrinted) {
				ignoredButPrinted.printStackTrace();
			}
		}
	}

	public static void openGui(GuiProvider guiProvider, Player oPlayer) {
		if (!(oPlayer instanceof ServerPlayer)) {
			throw new UnsupportedOperationException("Gui can only be opened on the server side");
		}
		ServerPlayer player = (ServerPlayer) oPlayer;
		AbstractContainerMenu container;
		try {
			container = guiProvider.getContainer(player);
		} catch (Throwable t) {
			logisticspipes.LogisticsPipes.log.error("getContainer threw for provider {}", guiProvider.getClass().getSimpleName(), t);
			return;
		}
		if (container == null) {
			if (guiProvider instanceof PopupGuiProvider) {
				OpenGUIPacket packet = PacketHandler.getPacket(OpenGUIPacket.class);
				packet.setGuiID(guiProvider.getId());
				packet.setWindowID(-2);
				packet.setGuiData(LPDataIOWrapper.collectData(guiProvider::writeData));
				MainProxy.sendPacketToPlayer(packet, player);
			}
			return;
		}
		// NOTE: We cannot use NetworkHooks.openScreen here because LP containers
		// are not registered as MenuType<>s — NetworkHooks calls menu.getType() and
		// would NPE when serializing PlayMessages.OpenContainer. Instead, manually
		// perform the server-side container registration that NetworkHooks would do,
		// and rely on LP's existing OpenGUIPacket to construct the screen client-side.
		player.doCloseContainer();
		player.nextContainerCounter();
		int windowId = player.containerCounter;
		try {
			CONTAINER_ID_FIELD.setInt(container, windowId);
		} catch (IllegalAccessException ex) {
			throw new RuntimeException("Failed to set LP container windowId", ex);
		}
		player.containerMenu = container;
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(
				new net.neoforged.neoforge.event.entity.player.PlayerContainerEvent.Open(player, player.containerMenu));

		// Send OpenGUIPacket BEFORE initMenu so the client's containerMenu is
		// established before the slot-sync packets (ClientboundContainerSetContent)
		// arrive.  If initMenu fires first, slot packets reach the client while the
		// old menu is still active, the window-ID check fails, and the contents are
		// silently dropped — causing an empty-looking GUI on first open.
		OpenGUIPacket packet = PacketHandler.getPacket(OpenGUIPacket.class);
		packet.setGuiID(guiProvider.getId());
		packet.setWindowID(windowId);
		packet.setGuiData(LPDataIOWrapper.collectData(guiProvider::writeData));
		MainProxy.sendPacketToPlayer(packet, player);

		// initMenu registers the slot listener and sends the current slot contents.
		// Sending it AFTER the OpenGUIPacket means the slot-sync arrives on the
		// client after the GUI has set player.containerMenu — so the ID check passes.
		player.initMenu(player.containerMenu);
	}

	@OnlyIn(Dist.CLIENT)
	public static void openGui(OpenGUIPacket packet, Player player) {
		int guiID = packet.getGuiID();
		GuiProvider provider = NewGuiHandler.guilist.get(guiID).template();
		LPDataIOWrapper.provideData(packet.getGuiData(), provider::readData);

		if (provider instanceof PopupGuiProvider && packet.getWindowID() == -2) {
			if (Minecraft.getInstance().screen instanceof LogisticsBaseGuiScreen) {
				LogisticsBaseGuiScreen baseGUI = (LogisticsBaseGuiScreen) Minecraft.getInstance().screen;
				SubGuiScreen newSub;
				try {
					newSub = (SubGuiScreen) provider.getClientGui(player);
				} catch (TargetNotFoundException e) {
					throw e;
				} catch (Exception e) {
					LogisticsPipes.log.error(packet.getClass().getName());
					LogisticsPipes.log.error(packet.toString());
					throw new RuntimeException(e);
				}
				if (newSub != null) {
					if (!baseGUI.hasSubGui()) {
						baseGUI.setSubGui(newSub);
					} else {
						SubGuiScreen canidate = baseGUI.getSubGui();
						while (canidate.hasSubGui()) {
							canidate = canidate.getSubGui();
						}
						canidate.setSubGui(newSub);
					}
				}
			}
		} else {
			AbstractContainerScreen screen;
			try {
				screen = (AbstractContainerScreen) provider.getClientGui(player);
			} catch (TargetNotFoundException e) {
				throw e;
			} catch (Exception e) {
				LogisticsPipes.log.error("getClientGui failed for provider {}", provider.getClass().getName(), e);
				return;
			}
			// Mirror the server-side windowId onto the client-side menu so vanilla
			// slot-sync packets (ClientboundContainerSetContent etc.) reach this menu.
			if (screen != null && screen.getMenu() != null) {
				try {
					CONTAINER_ID_FIELD.setInt(screen.getMenu(), packet.getWindowID());
				} catch (ReflectiveOperationException ex) {
					LogisticsPipes.log.error("Failed to set client menu containerId", ex);
				}
				player.containerMenu = screen.getMenu();
			}
			if (screen == null) {
				LogisticsPipes.log.warn("getClientGui returned null for provider {} (guiID={}) — closing current screen instead of opening a GUI",
						provider.getClass().getName(), guiID);
			}
			Minecraft.getInstance().setScreen(screen);
		}
	}
}
