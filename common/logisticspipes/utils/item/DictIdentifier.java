package logisticspipes.utils.item;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;

import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Identity for a single item tag (the 1.20.1 replacement for an OreDictionary entry).
 *
 * <p>The old OreDictionary-based code referred to entries by {@code int} id. We preserve that
 * shape — so {@link DictItemIdentifier} can keep using {@link java.util.BitSet} — and map each
 * {@link TagKey} to a stable id on first sight.
 *
 * <p>Name format is the tag's full location ({@code "forge:ingots/iron"}). Category is the tag's
 * namespace + path prefix up to the last {@code '/'}, so {@code "forge:ingots/iron"} has category
 * {@code "forge:ingots"}, mirroring the old {@code "ingotIron" -> "ingot"} grouping.
 */
public class DictIdentifier {

	private static final List<DictIdentifier> identifiers = new ArrayList<>();
	private static final ConcurrentHashMap<Identifier, Integer> tagToId = new ConcurrentHashMap<>();

	static synchronized DictIdentifier getForTag(@Nonnull TagKey<Item> tag) {
		Identifier loc = tag.location();
		Integer existing = tagToId.get(loc);
		if (existing != null) {
			return identifiers.get(existing);
		}
		int id = identifiers.size();
		DictIdentifier ident = new DictIdentifier(id, loc);
		identifiers.add(ident);
		tagToId.put(loc, id);
		return ident;
	}

	static synchronized DictIdentifier getForId(int id) {
		return identifiers.get(id);
	}

	private final int id;
	private final String name;
	private final String category;

	private DictIdentifier(int id, Identifier loc) {
		this.id = id;
		this.name = loc.toString();
		int slash = this.name.lastIndexOf('/');
		this.category = slash < 0 ? this.name : this.name.substring(0, slash);
	}

	public int getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getCategory() {
		return category;
	}

	public boolean canNameMatch(DictIdentifier ident) {
		return name.equals(ident.name);
	}

	public boolean canCategoryMatch(DictIdentifier ident) {
		return category.equals(ident.category);
	}

	public void debugDumpData(boolean isClient, StringBuilder builder) {
		builder.append(id).append('{').append(name).append('}');
	}
}
