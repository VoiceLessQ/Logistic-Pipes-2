package logisticspipes.recipes;

import javax.annotation.Nonnull;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;

import com.google.gson.JsonObject;


/**
 * Recipe key matching an item together with its custom data. Only used for LP's generated recipe JSON,
 * where it becomes Fabric's {@code fabric:custom_data} custom ingredient.
 */
public class NBTIngredient {

	private final Item item;
	private final CompoundTag tag;

	private NBTIngredient(Item item, CompoundTag tag) {
		this.item = item;
		this.tag = tag;
	}

	@Nonnull
	public static NBTIngredient of(@Nonnull final Item item, @Nonnull final CompoundTag tag) {
		return new NBTIngredient(item, tag.copy());
	}

	public JsonObject toJson() {
		JsonObject obj = new JsonObject();
		obj.addProperty("fabric:type", "fabric:custom_data");
		obj.addProperty("base", BuiltInRegistries.ITEM.getKey(item).toString());
		obj.addProperty("nbt", tag.toString());
		return obj;
	}
}
