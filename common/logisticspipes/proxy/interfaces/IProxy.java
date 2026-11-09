package logisticspipes.proxy.interfaces;

import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import logisticspipes.items.ItemLogisticsPipe;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.item.ItemIdentifier;

public interface IProxy {

	String getSide();

	/** Returns the client-side main level, or null on the dedicated server. */
	Level getWorld();

	void registerTileEntities();

	Player getClientPlayer();

	void addLogisticsPipesOverride(Object par1IIconRegister, int index, String override1, String override2, boolean flag);

	void registerParticles();

	String getName(ItemIdentifier item);

	void updateNames(ItemIdentifier item, String name);

	void tick();

	void sendNameUpdateRequest(Player player);

	LogisticsTileGenericPipe getPipeInDimensionAt(Identifier dimension, int x, int y, int z, Player player);

	void sendBroadCast(String message);

	void tickServer();

	void tickClient();

	void setIconProviderFromPipe(ItemLogisticsPipe item, CoreUnroutedPipe dummyPipe);

	LogisticsModule getModuleFromGui();

	boolean checkSinglePlayerOwner(String commandSenderName);

	void openFluidSelectGui(int slotId);

	default void registerModels() {}

	void registerTextures();

	void initModelLoader();

	int getRenderIndex();
}
