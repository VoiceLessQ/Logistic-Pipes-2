package logisticspipes;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.resources.Identifier;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

import logisticspipes.commands.chathelper.LPChatListener;
import logisticspipes.interfaces.IBlockEntityLifecycle;
import logisticspipes.network.LPPacketPayload;
import logisticspipes.network.PacketHandler;
import logisticspipes.pipefxhandlers.CustomParticleGroup;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.renderer.FluidContainerRenderer;
import logisticspipes.renderer.OrdererColorProperty;
import logisticspipes.renderer.LogisticsPipeItemRenderer;
import logisticspipes.renderer.LogisticsRenderPipe;
import logisticspipes.renderer.LogisticsSolidBlockItemRenderer;
import logisticspipes.renderer.LogisticsSolidBlockRenderer;
import logisticspipes.renderer.newpipe.LogisticsNewRenderPipe;
import logisticspipes.renderer.newpipe.LogisticsNewSolidBlockWorldRenderer;
import logisticspipes.renderer.newpipe.tube.CurveTubeRenderer;
import logisticspipes.renderer.newpipe.tube.GainTubeRenderer;
import logisticspipes.renderer.newpipe.tube.LineTubeRenderer;
import logisticspipes.renderer.newpipe.tube.SCurveTubeRenderer;
import logisticspipes.renderer.newpipe.tube.SpeedupTubeRenderer;
import logisticspipes.textures.TextureRegistrar;
import logisticspipes.ticks.ClientPacketBufferHandlerThread;
import logisticspipes.ticks.LPTickHandler;
import logisticspipes.ticks.RenderTickHandler;
import logisticspipes.utils.gui.DevGuiDriver;
import logisticspipes.utils.gui.sideconfig.SideConfigDisplay;
import network.rs485.logisticspipes.gui.WidgetScreenHudSuppressor;
import network.rs485.logisticspipes.gui.font.LPFontRenderer;

/** Client entrypoint: the client half of Forge commonSetup, clientSetup and registerRenderers. Runs after {@link LogisticsPipes#onInitialize}. */
public class LogisticsPipesClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(LPPacketPayload.TYPE, (payload, context) -> PacketHandler.handlePayload(payload, context.player()));

		LPTickHandler tickHandler = new LPTickHandler();
		ClientTickEvents.END_CLIENT_TICK.register(client -> tickHandler.clientTick());
		// client-side listeners live here: their lambdas pass ClientLevel/LocalPlayer, which a server-loaded class must not link against
		LogisticsEventListener listener = new LogisticsEventListener();
		ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, level) -> listener.WorldLoad(level));
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> listener.clientLoggedIn());
		ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> listener.onItemStackToolTip(stack, lines));
		LPChatListener.registerClient();
		new RenderTickHandler().register();
		WidgetScreenHudSuppressor.INSTANCE.register();
		ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
			LogisticsPipes.instance.postInit();
			// fonts load through the resource manager, which only exists once the client has started
			LPFontRenderer.Factory.asyncPreload();
		});
		ClientBlockEntityEvents.BLOCK_ENTITY_LOAD.register((blockEntity, level) -> {
			if (blockEntity instanceof IBlockEntityLifecycle lifecycle) lifecycle.onLoad();
		});

		SimpleServiceLocator.setClientPacketBufferHandlerThread(new ClientPacketBufferHandlerThread());

		// Preload all render models so they don't get loaded (and crash) on concurrent
		// render-thread class loading. Each loader is wrapped in its own try/catch so a
		// failure in one OBJ file / group lookup doesn't halt init.
		LogisticsPipes.safeLoadModels("LogisticsNewRenderPipe",       LogisticsNewRenderPipe::loadModels);
		LogisticsPipes.safeLoadModels("LogisticsNewSolidBlockWorldRenderer", LogisticsNewSolidBlockWorldRenderer::loadModels);
		LogisticsPipes.safeLoadModels("CurveTubeRenderer",            CurveTubeRenderer::loadModels);
		LogisticsPipes.safeLoadModels("GainTubeRenderer",             GainTubeRenderer::loadModels);
		LogisticsPipes.safeLoadModels("LineTubeRenderer",             LineTubeRenderer::loadModels);
		LogisticsPipes.safeLoadModels("SpeedupTubeRenderer",          SpeedupTubeRenderer::loadModels);
		LogisticsPipes.safeLoadModels("SCurveTubeRenderer",           SCurveTubeRenderer::loadModels);

		// The pipe block's cutout layer is derived from its sprites' alpha in 26.x (no ItemBlockRenderTypes).
		registerRenderers();
		TextureRegistrar.register();
		CustomParticleGroup.register();
		FluidContainerRenderer.register();
		OrdererColorProperty.register();
		SideConfigDisplay.registerRenderer();
		DevGuiDriver.registerIfEnabled();
	}

	private static void registerRenderers() {
		SpecialModelRenderers.ID_MAPPER.put(Identifier.fromNamespaceAndPath(LPConstants.LP_MOD_ID, "pipe"), LogisticsPipeItemRenderer.Unbaked.MAP_CODEC);
		SpecialModelRenderers.ID_MAPPER.put(Identifier.fromNamespaceAndPath(LPConstants.LP_MOD_ID, "solid_block"), LogisticsSolidBlockItemRenderer.Unbaked.MAP_CODEC);
		LogisticsPipes.log.debug("registerRenderers: BE_PIPE={}", LPRegistries.BE_PIPE.get());
		BlockEntityRenderers.register(LPRegistries.BE_PIPE.get(), LogisticsRenderPipe::new);
		// LP solid blocks: shared BER draws the OBJ body + cover plates with the per-type sprite.
		BlockEntityRenderers.register(LPRegistries.BE_POWER_JUNCTION.get(),    LogisticsSolidBlockRenderer::new);
		BlockEntityRenderers.register(LPRegistries.BE_POWER_PROVIDER_RF.get(), LogisticsSolidBlockRenderer::new);
		BlockEntityRenderers.register(LPRegistries.BE_POWER_PROVIDER_EU.get(), LogisticsSolidBlockRenderer::new);
		BlockEntityRenderers.register(LPRegistries.BE_SECURITY_STATION.get(),  LogisticsSolidBlockRenderer::new);
		BlockEntityRenderers.register(LPRegistries.BE_CRAFTING_TABLE.get(),    LogisticsSolidBlockRenderer::new);
		BlockEntityRenderers.register(LPRegistries.BE_STATISTICS_TABLE.get(),  LogisticsSolidBlockRenderer::new);
		BlockEntityRenderers.register(LPRegistries.BE_PROGRAM_COMPILER.get(),  LogisticsSolidBlockRenderer::new);
		BlockEntityRenderers.register(LPRegistries.BE_FRAME.get(),             LogisticsSolidBlockRenderer::new);
	}
}
