package logisticspipes.interfaces;

/** Forge's BlockEntity onLoad/onChunkUnloaded hooks, fired from Fabric's block entity and chunk events. */
public interface IBlockEntityLifecycle {

	void onLoad();

	default void onChunkUnloaded() {}
}
