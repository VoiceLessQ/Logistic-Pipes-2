package logisticspipes.mixin;

import java.util.Set;

import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.RepositorySource;
import net.minecraft.server.packs.repository.ServerPacksSource;

import com.google.common.collect.ImmutableSet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import logisticspipes.recipes.LPRecipePack;

@Mixin(PackRepository.class)
public abstract class PackRepositoryMixin {

	@Shadow
	@Final
	@Mutable
	private Set<RepositorySource> sources;

	// Forge AddPackFindersEvent(SERVER_DATA): only the data pack repository carries a ServerPacksSource.
	@Inject(method = "<init>", at = @At("TAIL"))
	private void logisticspipes$addVirtualRecipePack(RepositorySource[] sources, CallbackInfo ci) {
		if (this.sources.stream().anyMatch(ServerPacksSource.class::isInstance)) {
			this.sources = ImmutableSet.<RepositorySource>builder().addAll(this.sources).add(LPRecipePack::addToRepository).build();
		}
	}
}
