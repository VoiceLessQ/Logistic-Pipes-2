package logisticspipes.recipes;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;

import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;

/** Ingredient matching item + full component (NBT) equality; 1.20.5+ ICustomIngredient shape. */
public class NBTIngredient implements ICustomIngredient {

	public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("logisticspipes", "nbt");

	public static final MapCodec<NBTIngredient> CODEC = ItemStack.CODEC.listOf().fieldOf("stacks")
			.xmap(list -> new NBTIngredient(list.toArray(new ItemStack[0])), ing -> List.of(ing.matchingStacks));

	public static final StreamCodec<RegistryFriendlyByteBuf, NBTIngredient> STREAM_CODEC =
			ItemStack.LIST_STREAM_CODEC.map(
					list -> new NBTIngredient(list.toArray(new ItemStack[0])),
					ing -> List.of(ing.matchingStacks));

	public static final IngredientType<NBTIngredient> TYPE = new IngredientType<>(NBTIngredient.CODEC, NBTIngredient.STREAM_CODEC);

	private final ItemStack[] matchingStacks;

	protected NBTIngredient(ItemStack... stacks) {
		this.matchingStacks = stacks;
	}

	@Override
	public boolean isSimple() {
		return false;
	}

	@Override
	public IngredientType<?> getType() {
		return logisticspipes.LPRegistries.NBT_INGREDIENT_TYPE.get();
	}

	public JsonElement toJson() {
		JsonObject obj = NBTIngredient.CODEC.codec()
				.encodeStart(JsonOps.INSTANCE, this)
				.getOrThrow()
				.getAsJsonObject();
		obj.addProperty("type", NBTIngredient.ID.toString());
		return obj;
	}

	@Nonnull
	@Override
	public Stream<ItemStack> getItems() {
		return Arrays.stream(matchingStacks);
	}

	@Override
	public boolean test(@Nullable final ItemStack inputStack) {
		if (inputStack == null) return false;
		for (final ItemStack stack : matchingStacks) {
			if (ItemStack.isSameItemSameComponents(inputStack, stack)) {
				return true;
			}
		}
		return false;
	}

	@Nonnull
	public static Ingredient fromStacks(final ItemStack... stacks) {
		if (stacks.length > 0) {
			for (final ItemStack itemstack : stacks) {
				if (!itemstack.isEmpty()) {
					return new NBTIngredient(stacks).toVanilla();
				}
			}
		}
		return Ingredient.EMPTY;
	}

}
