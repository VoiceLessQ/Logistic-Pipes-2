/*
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import net.minecraft.server.MinecraftServer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import logisticspipes.interfaces.IBlockEntityLifecycle;
import logisticspipes.asm.wrapper.LogisticsWrapperHandler;
import logisticspipes.blocks.LogisticsProgramCompilerTileEntity;
import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.blocks.LogisticsSolidBlock;
import logisticspipes.blocks.crafting.LogisticsCraftingTableTileEntity;
import logisticspipes.blocks.powertile.LogisticsIC2PowerProviderTileEntity;
import logisticspipes.blocks.powertile.LogisticsPowerJunctionTileEntity;
import logisticspipes.blocks.powertile.LogisticsRFPowerProviderTileEntity;
import logisticspipes.blocks.stats.LogisticsStatisticsTileEntity;
import logisticspipes.commands.LogisticsPipesCommand;
import logisticspipes.commands.chathelper.LPChatListener;
import logisticspipes.config.Configs;
import logisticspipes.datafixer.LPDataFixer;
import logisticspipes.items.ItemBlankModule;
import logisticspipes.items.ItemDisk;
import logisticspipes.items.ItemHUDArmor;
import logisticspipes.items.ItemLogisticsChips;
import logisticspipes.items.ItemLogisticsPipe;
import logisticspipes.items.ItemLogisticsProgrammer;
import logisticspipes.items.ItemModule;
import logisticspipes.items.ItemParts;
import logisticspipes.items.ItemPipeController;
import logisticspipes.items.ItemPipeManager;
import logisticspipes.items.ItemPipeSignCreator;
import logisticspipes.items.ItemUpgrade;
import logisticspipes.items.LogisticsBrokenItem;
import logisticspipes.items.LogisticsFluidContainer;
import logisticspipes.items.LogisticsItemCard;
import logisticspipes.items.LogisticsSolidBlockItem;
import logisticspipes.items.RemoteOrderer;
import logisticspipes.logistics.LogisticsFluidManager;
import logisticspipes.logistics.LogisticsManager;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.PacketHandler;
import logisticspipes.pipes.PipeBlockRequestTable;
import logisticspipes.pipes.PipeFluidBasic;
import logisticspipes.pipes.PipeFluidExtractor;
import logisticspipes.pipes.PipeFluidInsertion;
import logisticspipes.pipes.PipeFluidProvider;
import logisticspipes.pipes.PipeFluidRequestLogistics;
import logisticspipes.pipes.PipeFluidSatellite;
import logisticspipes.pipes.PipeFluidSupplierMk2;
import logisticspipes.pipes.PipeFluidTerminus;
import logisticspipes.pipes.PipeItemsBasicLogistics;
import logisticspipes.pipes.PipeItemsCraftingLogistics;
import logisticspipes.pipes.PipeItemsFirewall;
import logisticspipes.pipes.PipeItemsFluidSupplier;
import logisticspipes.pipes.PipeItemsInvSysConnector;
import logisticspipes.pipes.PipeItemsProviderLogistics;
import logisticspipes.pipes.PipeItemsRemoteOrdererLogistics;
import logisticspipes.pipes.PipeItemsRequestLogistics;
import logisticspipes.pipes.PipeItemsRequestLogisticsMk2;
import logisticspipes.pipes.PipeItemsSatelliteLogistics;
import logisticspipes.pipes.PipeItemsSupplierLogistics;
import logisticspipes.pipes.PipeItemsSystemDestinationLogistics;
import logisticspipes.pipes.PipeItemsSystemEntranceLogistics;
import logisticspipes.pipes.PipeLogisticsChassisMk1;
import logisticspipes.pipes.PipeLogisticsChassisMk2;
import logisticspipes.pipes.PipeLogisticsChassisMk3;
import logisticspipes.pipes.PipeLogisticsChassisMk4;
import logisticspipes.pipes.PipeLogisticsChassisMk5;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.pipes.basic.LogisticsBlockGenericPipe;
import logisticspipes.pipes.basic.LogisticsBlockGenericSubMultiBlock;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericSubMultiBlock;
import logisticspipes.pipes.basic.fluid.FluidRoutedPipe;
import logisticspipes.pipes.tubes.HSTubeCurve;
import logisticspipes.pipes.tubes.HSTubeGain;
import logisticspipes.pipes.tubes.HSTubeLine;
import logisticspipes.pipes.tubes.HSTubeSCurve;
import logisticspipes.pipes.tubes.HSTubeSpeedup;
import logisticspipes.pipes.unrouted.PipeItemsBasicTransport;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.ProxyManager;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.SpecialInventoryHandlerManager;
import logisticspipes.proxy.SpecialTankHandlerManager;
import logisticspipes.proxy.computers.objects.LPGlobalCCAccess;
import logisticspipes.proxy.progressprovider.MachineProgressProvider;
import logisticspipes.proxy.recipeproviders.LogisticsCraftingTable;
import logisticspipes.proxy.specialconnection.SpecialPipeConnection;
import logisticspipes.proxy.specialconnection.SpecialTileConnection;
import logisticspipes.proxy.specialtankhandler.SpecialTankHandler;
import logisticspipes.recipes.CraftingRecipes;
import logisticspipes.recipes.LPChipRecipes;
import logisticspipes.recipes.LPRecipePack;
import logisticspipes.recipes.MiscItemRecipes;
import logisticspipes.recipes.ModuleChippedCraftingRecipes;
import logisticspipes.recipes.NBTIngredient;
import logisticspipes.recipes.PipeChippedCraftingRecipes;
import logisticspipes.recipes.RecipeManager;
import logisticspipes.recipes.UpgradeChippedCraftingRecipes;
import logisticspipes.renderer.LogisticsHUDRenderer;
import logisticspipes.renderer.newpipe.LogisticsNewRenderPipe;
import logisticspipes.renderer.newpipe.LogisticsNewSolidBlockWorldRenderer;
import logisticspipes.renderer.newpipe.tube.CurveTubeRenderer;
import logisticspipes.renderer.newpipe.tube.GainTubeRenderer;
import logisticspipes.renderer.newpipe.tube.LineTubeRenderer;
import logisticspipes.renderer.newpipe.tube.SCurveTubeRenderer;
import logisticspipes.renderer.newpipe.tube.SpeedupTubeRenderer;
import logisticspipes.routing.RouterManager;
import logisticspipes.routing.ServerRouter;
import logisticspipes.routing.channels.ChannelManagerProvider;
import logisticspipes.routing.pathfinder.PipeInformationManager;
import logisticspipes.routing.pathfinder.changedetection.BlockChangeListener;
import logisticspipes.textures.Textures;
import logisticspipes.ticks.ClientPacketBufferHandlerThread;
import logisticspipes.ticks.HudUpdateTick;
import logisticspipes.ticks.LPTickHandler;
import logisticspipes.ticks.QueuedTasks;
import logisticspipes.ticks.RenderTickHandler;
import logisticspipes.ticks.RoutingTableUpdateThread;
import logisticspipes.ticks.ServerPacketBufferHandlerThread;
import logisticspipes.ticks.VersionChecker;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.InventoryUtilFactory;
import logisticspipes.utils.RoutedItemHelper;
import logisticspipes.utils.StaticResolverUtil;
import logisticspipes.utils.tuples.Pair;
import network.rs485.grow.ServerTickDispatcher;
import network.rs485.logisticspipes.config.ClientConfiguration;
import network.rs485.logisticspipes.config.ServerConfigurationManager;
import network.rs485.logisticspipes.gui.font.LPFontRenderer;
import network.rs485.logisticspipes.guidebook.ItemGuideBook;
import network.rs485.logisticspipes.property.PropertyUpdaterEventListener;
import network.rs485.util.SystemUtilKt;

//@formatter:off
//CHECKSTYLE:OFF

public class LogisticsPipes implements ModInitializer {
	//@formatter:on
	//CHECKSTYLE:ON

	public static final String UNKNOWN = "unknown";
	// Dev-only: gates the + power-cheat button, the starter-pack bypass, security-station
	// override, etc. Flipped off automatically in a shipped jar so players can't free-cheat
	// infinite RF from the power junction.
	private static final boolean DEBUG = FabricLoader.getInstance().isDevelopmentEnvironment();
	private Consumer<MinecraftServer> minecraftTestStartMethod = null;
	@Nullable
	private static MinecraftServer server;
	private boolean postInitDone = false;

	public static boolean isDEBUG() {
		return DEBUG;
	}

	@Getter
	private static String VERSION = UNKNOWN;
	@Getter
	private static String VENDOR = UNKNOWN;
	@Getter
	private static String TARGET = UNKNOWN;

	/** Main entrypoint: the Forge constructor, preInit and commonSetup in their old order. Client parts run in {@link LogisticsPipesClient}. */
	@Override
	public void onInitialize() {
		instance = this;
		loadManifestValues(LogisticsPipes.class.getClassLoader());
		logisticspipes.items.ItemPipeSignCreator.registerPipeSignTypes();
		LPRegistries.register();
		LPDataFixer.INSTANCE.init();

		ServerLifecycleEvents.SERVER_STARTING.register(this::beforeStart);
		ServerLifecycleEvents.SERVER_STARTED.register(this::serverStarted);
		ServerLifecycleEvents.SERVER_STOPPING.register(this::cleanup);
		ServerLifecycleEvents.SERVER_STOPPED.register(stopped -> LogisticsPipes.server = null);
		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> registerCommands(dispatcher));

		preInit();
		commonSetup();
	}

	/** The running server (integrated or dedicated), or null outside a session. Replaces logisticspipes.LogisticsPipes.getServer(). */
	public static MinecraftServer getServer() {
		return server;
	}

	private static void loadManifestValues(ClassLoader loader) {
		try {
			final Enumeration<URL> resources = loader.getResources(JarFile.MANIFEST_NAME);
			boolean foundLp;
			do {
				final Manifest manifest = new Manifest(resources.nextElement().openStream());
				foundLp = "LogisticsPipes".equals(manifest.getMainAttributes().getValue("Specification-Title"));
				if (foundLp) {
					// DEBUG was made final + gated on !FMLEnvironment.production in
					// commit 7e2591c38 ("gate DEBUG on production env"); the manifest-
					// based override is no longer needed and would now be a final-
					// reassignment compile error. Production/dev split is handled by
					// the field initialiser at line 213.
					LogisticsPipes.VERSION = manifest.getMainAttributes().getValue("Implementation-Version");
					LogisticsPipes.VENDOR = manifest.getMainAttributes().getValue("Implementation-Vendor");
					LogisticsPipes.TARGET = manifest.getMainAttributes().getValue("Implementation-Target");
				}
			} while (resources.hasMoreElements() && !foundLp);
		} catch (IOException e) {
			LogisticsPipes.log.error("There was a problem loading our MANIFEST file, Logistics Pipes will not know about its origin");
		}
	}

	public static LogisticsPipes instance;

	private static boolean certificateError = false;

	public static String getVersionString() {
		return Stream.of(
				"Logistics Pipes " + LogisticsPipes.VERSION,
				LogisticsPipes.certificateError ? "certificate error" : "",
				LogisticsPipes.DEBUG ? "debug mode" : "",
				"target " + LogisticsPipes.TARGET,
				"vendor " + LogisticsPipes.VENDOR)
				.filter(str -> !str.isEmpty())
				.collect(Collectors.joining(", "));
	}

	// other statics
	public static Textures textures = new Textures();
	public static final Logger log = LoggerFactory.getLogger("LogisticsPipes");
	// Daemon worker: a dedicated server only exits once all non-daemon threads end.
	public static ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor(r -> {
		Thread t = new Thread(r, "LogisticsPipes Worker");
		t.setDaemon(true);
		return t;
	});
	public static VersionChecker versionChecker;

	private Queue<Runnable> postInitRun = new LinkedList<>();
	private static LPGlobalCCAccess generalAccess;
	private static ClientConfiguration playerConfig;
	private static ServerConfigurationManager serverConfigManager;

	private List<Supplier<Pair<Item, Item>>> resetRecipeList = new ArrayList<>();

	public static boolean isDevelopmentEnvironment() {
		if (!isDEBUG()) {
			return false;
		} else {
			boolean eclipseCheck = (new java.io.File(".classpath")).exists();
			boolean ideaCheck = System.getProperty("java.class.path").contains("idea_rt.jar");
			return eclipseCheck || ideaCheck;
		}
	}

	// ── Init (formerly the FML mod event bus) ────────────────────────────────

	private void preInit() {
		PacketHandler.initialize();
		PacketHandler.registerMessages();
		NewGuiHandler.initialize();

		log.info("====================================================");
		log.info(" LogisticsPipes Logger initialized");
		log.info("====================================================");

		// StaticResolverUtil scans ModFileScanData lazily on first findClassesByType() call.

		ProxyManager.load();
		Configs.load();

		if (LogisticsPipes.UNKNOWN.equals(LogisticsPipes.VERSION)) {
			LogisticsPipes.log.warn("Could not determine Logistics Pipes version, we do need that " + JarFile.MANIFEST_NAME + ", don't you know?");
		}
		LogisticsPipes.log.info("Running " + getVersionString());

		SimpleServiceLocator.setPipeInformationManager(new PipeInformationManager());
		SimpleServiceLocator.setLogisticsFluidManager(new LogisticsFluidManager());

		MainProxy.proxy.initModelLoader();
	}

	private void commonSetup() {
		registerRecipes();


		RouterManager manager = new RouterManager();
		SimpleServiceLocator.setRouterManager(manager);
		SimpleServiceLocator.setChannelConnectionManager(manager);
		SimpleServiceLocator.setSecurityStationManager(manager);
		SimpleServiceLocator.setLogisticsManager(new LogisticsManager());
		SimpleServiceLocator.setInventoryUtilFactory(new InventoryUtilFactory());
		SimpleServiceLocator.setSpecialConnectionHandler(new SpecialPipeConnection());
		SimpleServiceLocator.setSpecialConnectionHandler(new SpecialTileConnection());
		SimpleServiceLocator.setSpecialTankHandler(new SpecialTankHandler());
		SimpleServiceLocator.setMachineProgressProvider(new MachineProgressProvider());
		SimpleServiceLocator.setRoutedItemHelper(new RoutedItemHelper());
		SimpleServiceLocator.setChannelManagerProvider(new ChannelManagerProvider());

		LPTickHandler tickHandler = new LPTickHandler();
		QueuedTasks queuedTasks = new QueuedTasks();
		ServerTickEvents.END_SERVER_TICK.register(tickServer -> tickHandler.serverTick());
		ServerTickEvents.END_LEVEL_TICK.register(tickHandler::worldTick);
		ServerTickEvents.END_SERVER_TICK.register(tickServer -> queuedTasks.tickEnd());
		ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((blockEntity, level) -> {
			if (blockEntity instanceof IBlockEntityLifecycle lifecycle) lifecycle.onLoad();
		});
		ServerChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> chunk.getBlockEntities().values().forEach(blockEntity -> {
			if (blockEntity instanceof IBlockEntityLifecycle lifecycle) lifecycle.onChunkUnloaded();
		}));
		LogisticsEventListener.register();
		LPChatListener.register();
		BlockChangeListener.register();
		// PropertyUpdaterEventListener is called directly by NewGuiHandler and ServerPlayerMixin.

		SimpleServiceLocator.setServerPacketBufferHandlerThread(new ServerPacketBufferHandlerThread());
		for (int i = 0; i < Configs.MULTI_THREAD_NUMBER; i++) {
			new RoutingTableUpdateThread(i);
		}

		if (isTesting()) {
			final Class<?> testClass;
			try {
				testClass = Class.forName("network.rs485.logisticspipes.integration.MinecraftTest");
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException("Error loading minecraft test class", e);
			}
			try {
				final Object minecraftTestInstance = testClass.getDeclaredField("INSTANCE").get(null);
				final java.lang.reflect.Method serverStartMethod = testClass
						.getDeclaredMethod("serverStart", MinecraftServer.class);
				minecraftTestStartMethod = (MinecraftServer startedServer) -> {
					try {
						serverStartMethod.invoke(minecraftTestInstance, startedServer);
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException("Could not run server started hook in " + minecraftTestInstance, e);
					}
				};
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException("Error accessing minecraft test instance", e);
			}
		}
	}

	/** Forge FMLLoadCompleteEvent: after every mod initialized. Runs on CLIENT_STARTED, or SERVER_STARTING on a dedicated server. */
	void postInit() {
		if (postInitDone) return;
		postInitDone = true;
		postInitRun.forEach(Runnable::run);
		postInitRun = null;

		SpecialInventoryHandlerManager.load();
		SpecialTankHandlerManager.load();

		// Dead-mod integrations removed (BuildCraft, Thermal Dynamics/Expansion, IC2, EnderCore,
		// MCMultiPart) — none of these mods exist on 1.20.1.
		SimpleServiceLocator.addCraftingRecipeProvider(new LogisticsCraftingTable());

		MainProxy.proxy.registerTileEntities();
		MainProxy.proxy.registerParticles();

		FluidIdentifier.initFromForge(false);

		versionChecker = new VersionChecker();
	}

	static void safeLoadModels(String name, Runnable loader) {
		try {
			loader.run();
		} catch (Throwable t) {
			LogisticsPipes.log.warn("[CCL-replacement] {} failed to load models — pipe visuals will be incomplete: {}", name, t.toString());
		}
	}

	public static boolean isTesting() {
		return SystemUtilKt.checkBooleanProperty("logisticspipes.test");
	}

	// ── Server lifecycle ─────────────────────────────────────────────────────

	private void beforeStart(MinecraftServer startingServer) {
		LogisticsPipes.server = startingServer;
		postInit();
		ServerTickDispatcher.INSTANCE.serverStart();
	}

	private void cleanup(MinecraftServer stoppingServer) {
		SimpleServiceLocator.routerManager.serverStopClean();
		QueuedTasks.clearAllTasks();
		HudUpdateTick.clearUpdateFlags();
		PipeItemsSatelliteLogistics.cleanup();
		PipeFluidSatellite.cleanup();
		ServerRouter.cleanup();
		if (net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
			LogisticsHUDRenderer.instance().clear();
		}
		ServerTickDispatcher.INSTANCE.cleanup();
		LogisticsPipes.serverConfigManager = null;
	}

	private void registerCommands(com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher) {
		new logisticspipes.commands.LogisticsPipesCommand().register(dispatcher);
	}

	private void serverStarted(MinecraftServer startedServer) {
		if (minecraftTestStartMethod != null) minecraftTestStartMethod.accept(startedServer);
	}

	// ── Registry ─────────────────────────────────────────────────────────────
	// Items/blocks/BEs are registered in LPRegistries.
	// These setName stubs are retained as no-ops for legacy call sites that still
	// pass through them; the Identifier is set at LPRegistries.registerItem/registerBlock time.

	public static <T extends Item> T setName(T item, String name) {
		return item;
	}

	public static <T extends Item> T setName(T item, String name, String modID) {
		return item;
	}

	public static <T extends Block> T setName(T block, String name) {
		return block;
	}

	// ── Recipes ───────────────────────────────────────────────────────────────

	// The virtual recipe data pack (Forge AddPackFindersEvent) is added by PackRepositoryMixin.

	private void registerRecipes() {
		RecipeManager.recipeProvider.add(new LPChipRecipes());
		RecipeManager.recipeProvider.add(new UpgradeChippedCraftingRecipes());
		RecipeManager.recipeProvider.add(new ModuleChippedCraftingRecipes());
		RecipeManager.recipeProvider.add(new PipeChippedCraftingRecipes());
		RecipeManager.recipeProvider.add(new CraftingRecipes());
		RecipeManager.recipeProvider.add(new MiscItemRecipes());
	}

	public void registerPipes(Object registry) {
		// Pipes are registered in LPRegistries / LPItems.
	}

	// ── Computer/CC access ───────────────────────────────────────────────────

	public static Object getComputerLP() {
		if (LogisticsPipes.generalAccess == null) {
			LogisticsPipes.generalAccess = new LPGlobalCCAccess();
		}
		return LogisticsPipes.generalAccess;
	}

	// ── Config accessors ─────────────────────────────────────────────────────

	public static ClientConfiguration getClientPlayerConfig() {
		if (LogisticsPipes.playerConfig == null) {
			LogisticsPipes.playerConfig = new ClientConfiguration();
		}
		return LogisticsPipes.playerConfig;
	}

	public static ServerConfigurationManager getServerConfigManager() {
		if (LogisticsPipes.serverConfigManager == null) {
			LogisticsPipes.serverConfigManager = new ServerConfigurationManager();
		}
		return LogisticsPipes.serverConfigManager;
	}
}
