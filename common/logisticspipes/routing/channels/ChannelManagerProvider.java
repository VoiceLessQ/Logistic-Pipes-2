package logisticspipes.routing.channels;

import java.lang.ref.WeakReference;

import javax.annotation.Nonnull;

import net.minecraft.world.level.Level;

import net.minecraftforge.common.MinecraftForge;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.SubscribeEvent;

import logisticspipes.interfaces.routing.IChannelManager;
import logisticspipes.interfaces.routing.IChannelManagerProvider;

public class ChannelManagerProvider implements IChannelManagerProvider {

	private WeakReference<Level> worldWeakReference = null;
	private ChannelManager channelManager = null;

	public ChannelManagerProvider() {
		MinecraftForge.EVENT_BUS.register(this);
	}

	@Override
	public IChannelManager getChannelManager(@Nonnull Level world) {
		if (worldWeakReference == null || worldWeakReference.get() == null || channelManager == null) {
			worldWeakReference = new WeakReference<>(world);
			if (channelManager != null) {
				channelManager.setChanged();
			}
			channelManager = new ChannelManager(world);
		}
		return channelManager;
	}

	@SubscribeEvent
	public void onWorldUnload(LevelEvent.Unload worldEvent) {
		if (worldWeakReference != null) {
			if (worldWeakReference.get() == null || worldWeakReference.get() == worldEvent.getLevel()) {
				channelManager = null;
				worldWeakReference = null;
			}
		}
	}
}
