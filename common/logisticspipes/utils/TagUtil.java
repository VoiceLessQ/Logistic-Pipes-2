package logisticspipes.utils;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import logisticspipes.LogisticsPipes;
import logisticspipes.proxy.MainProxy;

/** Bridges LP's CompoundTag based (de)serialization to 26.x ValueInput/ValueOutput and codec-only ItemStacks. */
public final class TagUtil {

	private static final MapCodec<CompoundTag> FLAT_TAG = MapCodec.assumeMapUnsafe(CompoundTag.CODEC);

	private TagUtil() {}

	public static CompoundTag read(ValueInput input) {
		return input.read(FLAT_TAG).orElseGet(CompoundTag::new);
	}

	public static void write(ValueOutput output, CompoundTag tag) {
		output.store(FLAT_TAG, tag);
	}

	public static HolderLookup.Provider registries() {
		MinecraftServer server = LogisticsPipes.getServer();
		if (server != null) return server.registryAccess();
		Level level = MainProxy.proxy.getWorld();
		return level != null ? level.registryAccess() : RegistryAccess.EMPTY;
	}

	/** Old ItemStack.save(tag): writes the stack's fields into {@code tag} and returns it. */
	public static CompoundTag saveStack(ItemStack stack, CompoundTag tag) {
		if (stack.isEmpty()) return tag;
		Tag encoded = ItemStack.CODEC.encodeStart(registries().createSerializationContext(NbtOps.INSTANCE), stack).getOrThrow();
		if (encoded instanceof CompoundTag compound) tag.merge(compound);
		return tag;
	}

	public static CompoundTag saveStack(ItemStack stack) {
		return saveStack(stack, new CompoundTag());
	}

	/** Old ItemStack.of(tag); empty on missing or invalid data. */
	public static ItemStack loadStack(CompoundTag tag) {
		if (tag.isEmpty()) return ItemStack.EMPTY;
		return ItemStack.CODEC.parse(registries().createSerializationContext(NbtOps.INSTANCE), tag).result().orElse(ItemStack.EMPTY);
	}
}
