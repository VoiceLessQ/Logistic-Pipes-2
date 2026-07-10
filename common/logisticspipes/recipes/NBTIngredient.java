package logisticspipes.recipes;

import java.util.Arrays;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.core.registries.BuiltInRegistries;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.neoforged.neoforge.common.crafting.AbstractIngredient;
import net.neoforged.neoforge.common.crafting.IIngredientSerializer;

public class NBTIngredient extends AbstractIngredient {

	public static final ResourceLocation ID = new ResourceLocation("logisticspipes", "nbt");

	public static final IIngredientSerializer<NBTIngredient> SERIALIZER = new IIngredientSerializer<NBTIngredient>() {
		@Override
		public NBTIngredient parse(FriendlyByteBuf buffer) {
			int count = buffer.readVarInt();
			ItemStack[] stacks = new ItemStack[count];
			for (int i = 0; i < count; i++) {
				stacks[i] = buffer.readItem();
			}
			return new NBTIngredient(stacks);
		}

		@Override
		public NBTIngredient parse(JsonObject json) {
			try {
				String itemId = json.get("item").getAsString();
				ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.get(new ResourceLocation(itemId)));
				if (json.has("nbt")) {
					CompoundTag tag = TagParser.parseTag(json.get("nbt").getAsString());
					stack.setTag(tag);
				}
				return new NBTIngredient(stack);
			} catch (Exception e) {
				throw new RuntimeException("Failed to parse logisticspipes:nbt ingredient", e);
			}
		}

		@Override
		public void write(FriendlyByteBuf buffer, NBTIngredient ingredient) {
			ItemStack[] stacks = ingredient.getItems();
			buffer.writeVarInt(stacks.length);
			for (ItemStack stack : stacks) {
				buffer.writeItem(stack);
			}
		}
	};

	private final ItemStack[] matchingStacks;

	protected NBTIngredient(ItemStack... stacks) {
		super(Arrays.stream(stacks).map(Ingredient.ItemValue::new));
		this.matchingStacks = stacks;
	}

	@Override
	public boolean isSimple() {
		return false;
	}

	@Override
	public IIngredientSerializer<NBTIngredient> getSerializer() {
		return SERIALIZER;
	}

	@Override
	public JsonElement toJson() {
		JsonObject obj = new JsonObject();
		obj.addProperty("type", ID.toString());
		ItemStack stack = matchingStacks[0];
		obj.addProperty("item", BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
		if (stack.hasTag()) {
			obj.addProperty("nbt", stack.getTag().toString());
		}
		return obj;
	}

	@Nonnull
	@Override
	public ItemStack[] getItems() {
		return this.matchingStacks;
	}

	@Override
	public boolean test(@Nullable final ItemStack inputStack) {
		if (inputStack == null) return false;
		for (final ItemStack stack : matchingStacks) {
			if (stack.getItem() == inputStack.getItem() && ItemStack.isSameItemSameTags(inputStack, stack)) {
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
					return new NBTIngredient(stacks);
				}
			}
		}
		return EMPTY;
	}

}
