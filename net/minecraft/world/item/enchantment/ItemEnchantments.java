package net.minecraft.world.item.enchantment;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;
import org.jspecify.annotations.Nullable;

public class ItemEnchantments implements TooltipProvider {
	public static final ItemEnchantments EMPTY = new ItemEnchantments(new Object2IntOpenHashMap<>());
	private static final Codec<Integer> LEVEL_CODEC = Codec.intRange(1, 255);
	public static final Codec<ItemEnchantments> CODEC = Codec.unboundedMap(Enchantment.CODEC, LEVEL_CODEC)
		.xmap(map -> new ItemEnchantments(new Object2IntOpenHashMap<>(map)), itemEnchantments -> itemEnchantments.enchantments);
	public static final StreamCodec<RegistryFriendlyByteBuf, ItemEnchantments> STREAM_CODEC = StreamCodec.composite(
		ByteBufCodecs.map(Object2IntOpenHashMap::new, Enchantment.STREAM_CODEC, ByteBufCodecs.VAR_INT),
		itemEnchantments -> itemEnchantments.enchantments,
		ItemEnchantments::new
	);
	final Object2IntOpenHashMap<Holder<Enchantment>> enchantments;

	ItemEnchantments(Object2IntOpenHashMap<Holder<Enchantment>> object2IntOpenHashMap) {
		this.enchantments = object2IntOpenHashMap;

		for (Entry<Holder<Enchantment>> entry : object2IntOpenHashMap.object2IntEntrySet()) {
			int i = entry.getIntValue();
			if (i < 0 || i > 255) {
				throw new IllegalArgumentException("Enchantment " + entry.getKey() + " has invalid level " + i);
			}
		}
	}

	public int getLevel(Holder<Enchantment> holder) {
		return this.enchantments.getInt(holder);
	}

	@Override
	public void addToTooltip(Item.TooltipContext tooltipContext, Consumer<Component> consumer, TooltipFlag tooltipFlag, DataComponentGetter dataComponentGetter) {
		HolderLookup.Provider provider = tooltipContext.registries();
		HolderSet<Enchantment> holderSet = getTagOrEmpty(provider, Registries.ENCHANTMENT, EnchantmentTags.TOOLTIP_ORDER);

		for (Holder<Enchantment> holder : holderSet) {
			int i = this.enchantments.getInt(holder);
			if (i > 0) {
				consumer.accept(Enchantment.getFullname(holder, i));
			}
		}

		for (Entry<Holder<Enchantment>> entry : this.enchantments.object2IntEntrySet()) {
			Holder<Enchantment> holder2 = (Holder<Enchantment>)entry.getKey();
			if (!holderSet.contains(holder2)) {
				consumer.accept(Enchantment.getFullname((Holder<Enchantment>)entry.getKey(), entry.getIntValue()));
			}
		}
	}

	private static <T> HolderSet<T> getTagOrEmpty(@Nullable HolderLookup.Provider provider, ResourceKey<Registry<T>> resourceKey, TagKey<T> tagKey) {
		if (provider != null) {
			Optional<HolderSet.Named<T>> optional = provider.lookupOrThrow(resourceKey).get(tagKey);
			if (optional.isPresent()) {
				return (HolderSet<T>)optional.get();
			}
		}

		return HolderSet.direct();
	}

	public Set<Holder<Enchantment>> keySet() {
		return Collections.unmodifiableSet(this.enchantments.keySet());
	}

	public Set<Entry<Holder<Enchantment>>> entrySet() {
		return Collections.unmodifiableSet(this.enchantments.object2IntEntrySet());
	}

	public int size() {
		return this.enchantments.size();
	}

	public boolean isEmpty() {
		return this.enchantments.isEmpty();
	}

	public boolean equals(Object object) {
		if (this == object) {
			return true;
		} else {
			return object instanceof ItemEnchantments itemEnchantments ? this.enchantments.equals(itemEnchantments.enchantments) : false;
		}
	}

	public int hashCode() {
		return this.enchantments.hashCode();
	}

	public String toString() {
		return "ItemEnchantments{enchantments=" + this.enchantments + "}";
	}

	public static class Mutable {
		private final Object2IntOpenHashMap<Holder<Enchantment>> enchantments = new Object2IntOpenHashMap<>();

		public Mutable(ItemEnchantments itemEnchantments) {
			this.enchantments.putAll(itemEnchantments.enchantments);
		}

		public void set(Holder<Enchantment> holder, int i) {
			if (i <= 0) {
				this.enchantments.removeInt(holder);
			} else {
				this.enchantments.put(holder, Math.min(i, 255));
			}
		}

		public void upgrade(Holder<Enchantment> holder, int i) {
			if (i > 0) {
				this.enchantments.merge(holder, Math.min(i, 255), Integer::max);
			}
		}

		public void removeIf(Predicate<Holder<Enchantment>> predicate) {
			this.enchantments.keySet().removeIf(predicate);
		}

		public int getLevel(Holder<Enchantment> holder) {
			return this.enchantments.getOrDefault(holder, 0);
		}

		public Set<Holder<Enchantment>> keySet() {
			return this.enchantments.keySet();
		}

		public ItemEnchantments toImmutable() {
			return new ItemEnchantments(this.enchantments);
		}
	}
}
