package logisticspipes.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ServerPlayer.class)
public interface ServerPlayerAccessor {

	@Invoker("initMenu")
	void logisticspipes$initMenu(AbstractContainerMenu container);

	@Invoker("nextContainerCounter")
	void logisticspipes$nextContainerCounter();

	@Accessor("containerCounter")
	int logisticspipes$getContainerCounter();
}
