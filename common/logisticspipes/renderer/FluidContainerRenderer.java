package logisticspipes.renderer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.model.DynamicFluidContainerModel;
import net.neoforged.neoforge.client.model.geometry.IGeometryBakingContext;
import net.neoforged.neoforge.client.model.geometry.IGeometryLoader;
import net.neoforged.neoforge.client.model.geometry.IUnbakedGeometry;

import logisticspipes.LPConstants;
import logisticspipes.LPItems;
import logisticspipes.utils.FluidIdentifier;

/**
 * Fluid window rendering for the logistics fluid container item.
 *
 * <p>LP1 baked the fluid's still sprite through the stencil texture. NeoForge's
 * {@link DynamicFluidContainerModel} does the same, but finds the fluid through the
 * item fluid capability, which this item does not have. The
 * {@code logisticspipes:fluid_container} loader wraps it and resolves the fluid with
 * {@link FluidIdentifier} instead.</p>
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = LPConstants.LP_MOD_ID, value = Dist.CLIENT)
public class FluidContainerRenderer {

	@SubscribeEvent
	public static void registerGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
		event.register(ResourceLocation.fromNamespaceAndPath(LPConstants.LP_MOD_ID, "fluid_container"), Loader.INSTANCE);
	}

	@SubscribeEvent
	public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
		event.register((stack, tintIndex) -> tintIndex == 1 ? getFluidColor(stack) : 0xFFFFFFFF,
				LPItems.fluidContainer.get());
	}

	private static int getFluidColor(@Nonnull ItemStack stack) {
		FluidIdentifier ident = FluidIdentifier.get(stack);
		if (ident == null) return 0xFFFFFFFF;
		return IClientFluidTypeExtensions.of(ident.getFluid()).getTintColor(ident.makeFluidStack(1000));
	}

	private static class Loader implements IGeometryLoader<Geometry> {

		static final Loader INSTANCE = new Loader();

		@Override
		public Geometry read(JsonObject json, JsonDeserializationContext context) {
			if (!json.has("fluid")) json.addProperty("fluid", "minecraft:empty");
			return new Geometry(DynamicFluidContainerModel.Loader.INSTANCE.read(json, context));
		}
	}

	private record Geometry(DynamicFluidContainerModel base) implements IUnbakedGeometry<Geometry> {

		@Override
		public BakedModel bake(IGeometryBakingContext context, ModelBaker baker, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides) {
			return base.bake(context, baker, spriteGetter, modelState, new FluidOverrides(overrides, context, baker, base));
		}
	}

	/** Runs first inside NeoForge's own override handler, which only knows the capability path. */
	private static class FluidOverrides extends ItemOverrides {

		private final Map<Fluid, BakedModel> cache = new HashMap<>();
		private final ItemOverrides nested;
		private final IGeometryBakingContext context;
		private final ModelBaker baker;
		private final DynamicFluidContainerModel base;

		FluidOverrides(ItemOverrides nested, IGeometryBakingContext context, ModelBaker baker, DynamicFluidContainerModel base) {
			this.nested = nested;
			this.context = context;
			this.baker = baker;
			this.base = base;
		}

		@Override
		public BakedModel resolve(BakedModel model, ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
			BakedModel overridden = nested.resolve(model, stack, level, entity, seed);
			if (overridden != model) return overridden;
			FluidIdentifier ident = FluidIdentifier.get(stack);
			if (ident == null) return model;
			return cache.computeIfAbsent(ident.getFluid(),
					fluid -> base.withFluid(fluid).bake(context, baker, Material::sprite, BlockModelRotation.X0_Y0, this));
		}
	}
}
