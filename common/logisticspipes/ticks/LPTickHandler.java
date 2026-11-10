package logisticspipes.ticks;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import com.google.common.collect.MapMaker;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import logisticspipes.commands.commands.debug.DebugGuiController;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.utils.FluidIdentifier;
import network.rs485.grow.ServerTickDispatcher;
import network.rs485.logisticspipes.world.DoubleCoordinates;

public class LPTickHandler {

	public static int adjChecksDone = 0;

	/** ClientTickEvents.END_CLIENT_TICK */
	@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
	public void clientTick() {
		FluidIdentifier.initFromForge(true);
		SimpleServiceLocator.clientBufferHandler.clientTick();
		MainProxy.proxy.tickClient();
		DebugGuiController.instance().execClient();
	}

	/** ServerTickEvents.END_SERVER_TICK */
	public void serverTick() {
		HudUpdateTick.tick();
		SimpleServiceLocator.serverBufferHandler.serverTick();
		MainProxy.proxy.tickServer();
		LPTickHandler.adjChecksDone = 0;
		DebugGuiController.instance().execServer();
		ServerTickDispatcher.INSTANCE.tick();
	}

	private static Map<Level, LPWorldInfo> worldInfo = new MapMaker().weakKeys().makeMap();

	/** ServerTickEvents.END_LEVEL_TICK */
	public void worldTick(ServerLevel level) {
		LPWorldInfo info = LPTickHandler.getWorldInfo(level);
		info.worldTick++;
	}

	public static LPWorldInfo getWorldInfo(Level world) {
		LPWorldInfo info = LPTickHandler.worldInfo.get(world);
		if (info == null) {
			info = new LPWorldInfo();
			LPTickHandler.worldInfo.put(world, info);
		}
		return info;
	}

	@Data
	public static class LPWorldInfo {

		@Getter
		@Setter(value = AccessLevel.PRIVATE)
		private long worldTick = 0;
		@Getter
		private Set<DoubleCoordinates> updateQueued = new HashSet<>();

		@Getter
		@Setter
		private boolean skipBlockUpdateForWorld = false;
	}
}
