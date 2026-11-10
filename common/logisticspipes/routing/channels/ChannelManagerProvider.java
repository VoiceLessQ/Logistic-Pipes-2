package logisticspipes.routing.channels;

import java.lang.ref.WeakReference;

import javax.annotation.Nonnull;

import net.minecraft.world.level.Level;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLevelEvents;

import logisticspipes.interfaces.routing.IChannelManager;
import logisticspipes.interfaces.routing.IChannelManagerProvider;

public class ChannelManagerProvider implements IChannelManagerProvider {

	private WeakReference<Level> worldWeakReference = null;
	private ChannelManager channelManager = null;

	public ChannelManagerProvider() {
		ServerLevelEvents.UNLOAD.register((server, level) -> onWorldUnload(level));
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

	public void onWorldUnload(Level level) {
		if (worldWeakReference != null) {
			if (worldWeakReference.get() == null || worldWeakReference.get() == level) {
				channelManager = null;
				worldWeakReference = null;
			}
		}
	}
}
