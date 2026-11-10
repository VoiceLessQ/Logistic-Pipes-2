package logisticspipes.renderer;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperties;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperty;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import logisticspipes.LPConstants;
import logisticspipes.recipes.OrdererDyeRecipe;

/** Item model select property {@code logisticspipes:orderer_color}: the remote orderer colour 0..16 from its custom data. */
@Environment(EnvType.CLIENT)
public record OrdererColorProperty() implements SelectItemModelProperty<Integer> {

	public static final SelectItemModelProperty.Type<OrdererColorProperty, Integer> TYPE = SelectItemModelProperty.Type.create(MapCodec.unit(new OrdererColorProperty()), Codec.INT);

	public static void register() {
		SelectItemModelProperties.ID_MAPPER.put(Identifier.fromNamespaceAndPath(LPConstants.LP_MOD_ID, "orderer_color"), TYPE);
	}

	@Override
	public Integer get(ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity owner, int seed, ItemDisplayContext displayContext) {
		return OrdererDyeRecipe.getVariant(stack);
	}

	@Override
	public Codec<Integer> valueCodec() {
		return Codec.INT;
	}

	@Override
	public SelectItemModelProperty.Type<OrdererColorProperty, Integer> type() {
		return TYPE;
	}
}
