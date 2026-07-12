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

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.InterModComms;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;

import com.mojang.logging.LogUtils;
import lombok.Getter;
import lombok.SneakyThrows;
import org.slf4j.Logger;

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
import network.rs485.logisticspipes.compat.TheOneProbeIntegration;
import network.rs485.logisticspipes.config.ClientConfiguration;
import network.rs485.logisticspipes.config.ServerConfigurationManager;
import network.rs485.logisticspipes.gui.font.LPFontRenderer;
import network.rs485.logisticspipes.guidebook.ItemGuideBook;
import network.rs485.logisticspipes.property.PropertyUpdaterEventListener;
import network.rs485.util.SystemUtilKt;

//@formatter:off
//CHECKSTYLE:OFF

@Mod(LPConstants.LP_MOD_ID)
public class LogisticsPipes {
	//@formatter:on
	//CHECKSTYLE:ON

	public static final String UNKNOWN = "unknown";
	// Dev-only: gates the + power-cheat button, the starter-pack bypass, security-station
	// override, etc. Flipped off automatically in a shipped jar so players can't free-cheat
	// infinite RF from the power junction.
	private static final boolean DEBUG = !net.neoforged.fml.loading.FMLEnvironment.production;
	private Consumer<ServerStartedEvent> minecraftTestStartMethod = null;

	public static boolean isDEBUG() {
		return DEBUG;
	}

	@Getter
	private static String VERSION = UNKNOWN;
	@Getter
	private static String VENDOR = UNKNOWN;
	@Getter
	private static String TARGET = UNKNOWN;

	public LogisticsPipes() {
		this(thedarkcolour.kotlinforforge.forge.ForgeKt.getMOD_BUS());
	}

	public LogisticsPipes(IEventBus modEventBus) {
		instance = this;
		loadManifestValues(LogisticsPipes.class.getClassLoader());
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Configs.SPEC);
		LPRegistries.register(modEventBus);
		modEventBus.addListener(LPCapabilities::register);

		modEventBus.addListener(this::preInit);
		modEventBus.addListener(PacketHandler::onRegisterPayloads);
		modEventBus.addListener(this::commonSetup);
		modEventBus.addListener(this::postInit);
		modEventBus.addListener(this::onAddPackFinders);
		// `clientSetup` and `registerRenderers` are @OnlyIn(Dist.CLIENT); on a dedicated
		// server FML's runtime-dist-cleaner strips them, and the `this::method` reference
		// here would NoSuchMethodError before the constructor finishes.
		if (net.neoforged.fml.loading.FMLEnvironment.dist == net.neoforged.api.distmarker.Dist.CLIENT) {
			modEventBus.addListener(this::clientSetup);
			modEventBus.addListener(this::registerRenderers);
			modEventBus.register(logisticspipes.textures.TextureRegistrar.class);
		}
		LPDataFixer.INSTANCE.init();
		// Networking payloads register via PacketHandler.onRegisterPayloads (mod bus).
		// Items/blocks/BEs/creative-tabs are registered via DeferredRegister in LPRegistries.

		NeoForge.EVENT_BUS.register(this);
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
	public static final Logger log = LogUtils.getLogger();
	public static ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
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

	// ── FML lifecycle (mod event bus) ────────────────────────────────────────

	private void preInit(FMLCommonSetupEvent event) {
		PacketHandler.initialize();
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

		if (ModList.get().isLoaded(LPConstants.theOneProbeModID)) {
			InterModComms.sendTo(LPConstants.theOneProbeModID, "getTheOneProbe",
					TheOneProbeIntegration.class::getName);
		}

		MainProxy.proxy.initModelLoader();
	}

	private void commonSetup(FMLCommonSetupEvent event) {
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

		NeoForge.EVENT_BUS.register(new LPTickHandler());
		NeoForge.EVENT_BUS.register(new QueuedTasks());
		NeoForge.EVENT_BUS.register(new LogisticsEventListener());
		NeoForge.EVENT_BUS.register(new LPChatListener());
		NeoForge.EVENT_BUS.register(new BlockChangeListener());
		NeoForge.EVENT_BUS.register(PropertyUpdaterEventListener.INSTANCE);

		// Client-side setup (runs on client only)
		event.enqueueWork(() -> {
			if (net.neoforged.fml.loading.FMLEnvironment.dist == Dist.CLIENT) {
				RenderTickHandler sub = new RenderTickHandler();
				NeoForge.EVENT_BUS.register(sub);
				NeoForge.EVENT_BUS.register(network.rs485.logisticspipes.gui.WidgetScreenHudSuppressor.INSTANCE);
				SimpleServiceLocator.setClientPacketBufferHandlerThread(new ClientPacketBufferHandlerThread());
				LPFontRenderer.Factory.asyncPreload();

				// Preload all render models so they don't get loaded (and crash) on concurrent
				// render-thread class loading. Each loader is wrapped in its own try/catch so a
				// failure in one OBJ file / group lookup doesn't halt init. These reference
				// client-only renderer classes, so they MUST stay inside this Dist.CLIENT guard —
				// the method-reference bootstraps below would otherwise link client classes on a
				// dedicated server.
				safeLoadModels("LogisticsNewRenderPipe",       LogisticsNewRenderPipe::loadModels);
				safeLoadModels("LogisticsNewSolidBlockWorldRenderer", LogisticsNewSolidBlockWorldRenderer::loadModels);
				safeLoadModels("CurveTubeRenderer",            CurveTubeRenderer::loadModels);
				safeLoadModels("GainTubeRenderer",             GainTubeRenderer::loadModels);
				safeLoadModels("LineTubeRenderer",             LineTubeRenderer::loadModels);
				safeLoadModels("SpeedupTubeRenderer",          SpeedupTubeRenderer::loadModels);
				safeLoadModels("SCurveTubeRenderer",           SCurveTubeRenderer::loadModels);

				// Fluid container "filled" model predicate (client-only class, stays in the guard).
				logisticspipes.renderer.FluidContainerRenderer.registerItemProperties();
			}
		});

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
						.getDeclaredMethod("serverStart", ServerStartedEvent.class);
				minecraftTestStartMethod = (ServerStartedEvent serverStartedEvent) -> {
					try {
						serverStartMethod.invoke(minecraftTestInstance, serverStartedEvent);
					} catch (ReflectiveOperationException e) {
						throw new RuntimeException("Could not run server started hook in " + minecraftTestInstance, e);
					}
				};
				NeoForge.EVENT_BUS.register(minecraftTestInstance);
			} catch (ReflectiveOperationException e) {
				throw new RuntimeException("Error accessing minecraft test instance", e);
			}
		}
	}

	private void postInit(FMLLoadCompleteEvent event) {
		postInitRun.forEach(Runnable::run);
		postInitRun = null;

		SpecialInventoryHandlerManager.load();
		SpecialTankHandlerManager.load();

		// Dead-mod integrations removed (BuildCraft, Thermal Dynamics/Expansion, IC2, EnderCore,
		// MCMultiPart) — none of these mods exist on 1.20.1.
		SimpleServiceLocator.addCraftingRecipeProvider(new LogisticsCraftingTable());

		// BlockEntityTypes are registered via DeferredRegister in LPRegistries.
		MainProxy.proxy.registerTileEntities();
		MainProxy.proxy.registerParticles();

		FluidIdentifier.initFromForge(false);

		versionChecker = VersionChecker.runVersionCheck();
	}

	@OnlyIn(Dist.CLIENT)
	private void clientSetup(FMLClientSetupEvent event) {
		// Texture atlas sprites and item/block models are supplied declaratively via
		// JSON in assets/logisticspipes/models/** in 1.20.1 — no code registration.
		// The legacy MainProxy.proxy.registerTextures() / registerModels() and
		// LogisticsPipes.textures.registerBlockIcons(...) paths are deferred to the
		// renderer rewrite; they are intentionally not called here.
		// BlockEntityRenderer for the pipe BE is registered via
		// EntityRenderersEvent.RegisterRenderers (see registerRenderers below).
		event.enqueueWork(() -> {
			net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
				LPRegistries.PIPE.get(),
				net.minecraft.client.renderer.RenderType.cutout());
		});
	}

	@OnlyIn(Dist.CLIENT)
	private void registerRenderers(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) {
		LogisticsPipes.log.debug("registerRenderers: BE_PIPE={}", LPRegistries.BE_PIPE.get());
		event.registerBlockEntityRenderer(LPRegistries.BE_PIPE.get(), logisticspipes.renderer.LogisticsRenderPipe::new);
		// LP solid blocks: shared BER draws the OBJ body + cover plates with the per-type sprite.
		event.registerBlockEntityRenderer(LPRegistries.BE_POWER_JUNCTION.get(),    logisticspipes.renderer.LogisticsSolidBlockRenderer::new);
		event.registerBlockEntityRenderer(LPRegistries.BE_POWER_PROVIDER_RF.get(), logisticspipes.renderer.LogisticsSolidBlockRenderer::new);
		event.registerBlockEntityRenderer(LPRegistries.BE_POWER_PROVIDER_EU.get(), logisticspipes.renderer.LogisticsSolidBlockRenderer::new);
		event.registerBlockEntityRenderer(LPRegistries.BE_SECURITY_STATION.get(),  logisticspipes.renderer.LogisticsSolidBlockRenderer::new);
		event.registerBlockEntityRenderer(LPRegistries.BE_CRAFTING_TABLE.get(),    logisticspipes.renderer.LogisticsSolidBlockRenderer::new);
		event.registerBlockEntityRenderer(LPRegistries.BE_STATISTICS_TABLE.get(),  logisticspipes.renderer.LogisticsSolidBlockRenderer::new);
		event.registerBlockEntityRenderer(LPRegistries.BE_PROGRAM_COMPILER.get(),  logisticspipes.renderer.LogisticsSolidBlockRenderer::new);
		event.registerBlockEntityRenderer(LPRegistries.BE_FRAME.get(),             logisticspipes.renderer.LogisticsSolidBlockRenderer::new);
	}

	private static void safeLoadModels(String name, Runnable loader) {
		try {
			loader.run();
		} catch (Throwable t) {
			LogisticsPipes.log.warn("[CCL-replacement] {} failed to load models — pipe visuals will be incomplete: {}", name, t.toString());
		}
	}

	public static boolean isTesting() {
		return SystemUtilKt.checkBooleanProperty("logisticspipes.test");
	}

	// ── Game event bus (server lifecycle) ────────────────────────────────────

	@SubscribeEvent
	public void beforeStart(ServerAboutToStartEvent event) {
		ServerTickDispatcher.INSTANCE.serverStart();
	}

	@SubscribeEvent
	public void cleanup(ServerStoppingEvent event) {
		SimpleServiceLocator.routerManager.serverStopClean();
		QueuedTasks.clearAllTasks();
		HudUpdateTick.clearUpdateFlags();
		PipeItemsSatelliteLogistics.cleanup();
		PipeFluidSatellite.cleanup();
		ServerRouter.cleanup();
		if (net.neoforged.fml.loading.FMLEnvironment.dist == Dist.CLIENT) {
			LogisticsHUDRenderer.instance().clear();
		}
		ServerTickDispatcher.INSTANCE.cleanup();
		LogisticsPipes.serverConfigManager = null;
	}

	@SubscribeEvent
	public void registerCommands(net.neoforged.neoforge.event.RegisterCommandsEvent event) {
		new logisticspipes.commands.LogisticsPipesCommand().register(event.getDispatcher());
	}

	@SubscribeEvent
	public void serverStarted(ServerStartedEvent event) {
		if (minecraftTestStartMethod != null) minecraftTestStartMethod.accept(event);
	}

	// ── Registry ─────────────────────────────────────────────────────────────
	// Items/blocks/BEs are registered via DeferredRegister in LPRegistries.
	// These setName stubs are retained as no-ops for legacy call sites that still
	// pass through them; the ResourceLocation is set at DeferredRegister.register time.

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

	private void onAddPackFinders(AddPackFindersEvent event) {
		if (event.getPackType() != PackType.SERVER_DATA) return;
		event.addRepositorySource(consumer -> {
			Pack pack = Pack.readMetaAndCreate(
					"logisticspipes:virtual_recipes",
					Component.literal("LogisticsPipes virtual recipes"),
					true,
					id -> new LPRecipePack(),
					PackType.SERVER_DATA,
					Pack.Position.TOP,
					PackSource.BUILT_IN);
			if (pack != null) consumer.accept(pack);
		});
	}

	private void registerRecipes() {
		// NBT ingredient type is registered via LPRegistries.INGREDIENT_TYPES.
		RecipeManager.recipeProvider.add(new LPChipRecipes());
		RecipeManager.recipeProvider.add(new UpgradeChippedCraftingRecipes());
		RecipeManager.recipeProvider.add(new ModuleChippedCraftingRecipes());
		RecipeManager.recipeProvider.add(new PipeChippedCraftingRecipes());
		RecipeManager.recipeProvider.add(new CraftingRecipes());
		RecipeManager.loadRecipes();
	}

	public void registerPipes(Object registry) {
		// Pipes are registered via DeferredRegister in LPRegistries / LPItems.
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
