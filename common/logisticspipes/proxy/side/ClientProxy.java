package logisticspipes.proxy.side;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import logisticspipes.gui.popup.SelectItemOutOfList;
import logisticspipes.items.ItemLogisticsPipe;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.gui.DummyContainerSlotClick;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.interfaces.IProxy;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.utils.gui.SubGuiScreen;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;

@OnlyIn(Dist.CLIENT)
public class ClientProxy implements IProxy {

	int renderIndex = 0;

	@Override
	public String getSide() {
		return "Client";
	}

	@Override
	public Level getWorld() {
		return Minecraft.getInstance().level;
	}

	@Override
	public void registerTileEntities() {
		// BlockEntityRenderer registration is handled via EntityRenderersEvent.RegisterRenderers
		// in LogisticsPipes.registerRenderers() — nothing to do here except initialise the render list.
		SimpleServiceLocator.setRenderListHandler(new logisticspipes.renderer.newpipe.GLRenderListHandler());
	}

	@Override
	public Player getClientPlayer() {
		return Minecraft.getInstance().player;
	}

	@Override
	public void registerParticles() {
		// LP has its own particle pipeline — particles are spawned directly via
		// mc.particleEngine.add(...) from PipeFXRenderHandler and do not go through
		// the vanilla ParticleType registry, so RegisterParticleProvidersEvent is
		// not involved. Wire the color → provider map once at client init.
		logisticspipes.pipefxhandlers.PipeFXRenderHandler.registerParticleHandler(
			logisticspipes.pipefxhandlers.Particles.WhiteParticle,
			new logisticspipes.pipefxhandlers.providers.EntityWhiteSparkleFXProvider());
		logisticspipes.pipefxhandlers.PipeFXRenderHandler.registerParticleHandler(
			logisticspipes.pipefxhandlers.Particles.RedParticle,
			new logisticspipes.pipefxhandlers.providers.EntityRedSparkleFXProvider());
		logisticspipes.pipefxhandlers.PipeFXRenderHandler.registerParticleHandler(
			logisticspipes.pipefxhandlers.Particles.BlueParticle,
			new logisticspipes.pipefxhandlers.providers.EntityBlueSparkleFXProvider());
		logisticspipes.pipefxhandlers.PipeFXRenderHandler.registerParticleHandler(
			logisticspipes.pipefxhandlers.Particles.GreenParticle,
			new logisticspipes.pipefxhandlers.providers.EntityGreenSparkleFXProvider());
		logisticspipes.pipefxhandlers.PipeFXRenderHandler.registerParticleHandler(
			logisticspipes.pipefxhandlers.Particles.GoldParticle,
			new logisticspipes.pipefxhandlers.providers.EntityGoldSparkleFXProvider());
		logisticspipes.pipefxhandlers.PipeFXRenderHandler.registerParticleHandler(
			logisticspipes.pipefxhandlers.Particles.VioletParticle,
			new logisticspipes.pipefxhandlers.providers.EntityVioletSparkleFXProvider());
		logisticspipes.pipefxhandlers.PipeFXRenderHandler.registerParticleHandler(
			logisticspipes.pipefxhandlers.Particles.OrangeParticle,
			new logisticspipes.pipefxhandlers.providers.EntityOrangeSparkleFXProvider());
		logisticspipes.pipefxhandlers.PipeFXRenderHandler.registerParticleHandler(
			logisticspipes.pipefxhandlers.Particles.LightGreenParticle,
			new logisticspipes.pipefxhandlers.providers.EntityLightGreenSparkleFXProvider());
		logisticspipes.pipefxhandlers.PipeFXRenderHandler.registerParticleHandler(
			logisticspipes.pipefxhandlers.Particles.LightRedParticle,
			new logisticspipes.pipefxhandlers.providers.EntityLightRedSparkleFXProvider());
	}

	@Override
	public String getName(ItemIdentifier item) {
		return item.getFriendlyName();
	}

	@Override
	public void updateNames(ItemIdentifier item, String name) {}

	@Override
	public void tick() {}

	@Override
	public void sendNameUpdateRequest(Player player) {}

	@Override
	public LogisticsTileGenericPipe getPipeInDimensionAt(ResourceLocation dimension, int x, int y, int z, Player player) {
		Level level = Minecraft.getInstance().level;
		if (level == null) return null;
		if (!level.dimension().location().equals(dimension)) return null;
		return getPipe(level, x, y, z);
	}

	private static LogisticsTileGenericPipe getPipe(Level level, int x, int y, int z) {
		if (level == null) return null;
		BlockPos pos = new BlockPos(x, y, z);
		if (level.isEmptyBlock(pos)) return null;
		BlockEntity tile = level.getBlockEntity(pos);
		return tile instanceof LogisticsTileGenericPipe ? (LogisticsTileGenericPipe) tile : null;
	}

	@Override
	public void addLogisticsPipesOverride(Object par1IIconRegister, int index, String override1, String override2, boolean flag) {
		// override2 == "NewPipeTexture" means this call targets LPnewPipeIconProvider's index
		// space (newTextureIndex, separate from LPpipeIconProvider's normal index).
		// flag == false selects the pre-generated base+overlay composite from overlay_gen/
		// (powered / unpowered / un-overlayed variants), exactly as LP1 did.
		if ("NewPipeTexture".equals(override2)) {
			logisticspipes.textures.TextureRegistrar.recordNew(index, override1);
		} else if (flag) {
			logisticspipes.textures.TextureRegistrar.record(index, override1);
		} else {
			logisticspipes.textures.TextureRegistrar.recordOverlay(index, override1, override2);
		}
	}

	@Override
	public void sendBroadCast(String message) {
		var player = Minecraft.getInstance().player;
		if (player != null) {
			player.sendSystemMessage(Component.literal("[LP] Client: " + message));
		}
	}

	@Override
	public void tickServer() {}

	@Override
	public void tickClient() {
		MainProxy.addTick();
		SimpleServiceLocator.renderListHandler.tick();
	}

	@Override
	public void setIconProviderFromPipe(ItemLogisticsPipe item, CoreUnroutedPipe dummyPipe) {
		// TODO: IIconProvider → deferred to renderer migration
	}

	@Override
	public LogisticsModule getModuleFromGui() {
		var screen = Minecraft.getInstance().screen;
		if (screen instanceof logisticspipes.gui.modules.ModuleBaseGui g) return g.getModule();
		if (screen instanceof logisticspipes.gui.GuiCraftingPipe g)        return g.getCraftingModule();
		return null;
	}

	@Override
	public boolean checkSinglePlayerOwner(String commandSenderName) {
		var server = Minecraft.getInstance().getSingleplayerServer();
		return server != null && !server.isPublished();
	}

	@Override
	public void openFluidSelectGui(final int slotId) {
		if (Minecraft.getInstance().screen instanceof LogisticsBaseGuiScreen) {
			final List<ItemIdentifierStack> list = new ArrayList<>();
			for (FluidIdentifier fluid : FluidIdentifier.all()) {
				if (fluid == null) {
					continue;
				}
				list.add(fluid.getItemIdentifier().makeStack(1));
			}
			SelectItemOutOfList subGui = new SelectItemOutOfList(list, slot -> {
				if (slot == -1) {
					return;
				}
				MainProxy.sendPacketToServer(PacketHandler.getPacket(DummyContainerSlotClick.class).setSlotId(slotId).setStack(list.get(slot).makeNormalStack()).setButton(0));
			});
			LogisticsBaseGuiScreen gui = (LogisticsBaseGuiScreen) Minecraft.getInstance().screen;
			if (!gui.hasSubGui()) {
				gui.setSubGui(subGui);
			} else {
				SubGuiScreen nextGui = gui.getSubGui();
				while (nextGui.hasSubGui()) {
					nextGui = nextGui.getSubGui();
				}
				nextGui.setSubGui(subGui);
			}
		} else {
			throw new UnsupportedOperationException(String.valueOf(Minecraft.getInstance().screen));
		}
	}

	@Override
	public void registerModels() {
		// Model registration migrated to ModelEvent.RegisterAdditional / baked-model system
	}

	@Override
	public void registerTextures() {
		// TextureAtlas sprite registration migrated to RegisterAtlasSpritesEvent
		renderIndex++;
	}

	@Override
	public void initModelLoader() {
		// TODO: IModelLoader → IUnbakedModel system in 1.20.1; deferred to renderer migration
	}

	@Override
	public int getRenderIndex() {
		return renderIndex;
	}
}
