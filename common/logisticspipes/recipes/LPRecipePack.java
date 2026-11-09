package logisticspipes.recipes;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionType;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.resources.IoSupplier;
import net.minecraft.util.InclusiveRange;

import logisticspipes.LPConstants;

/**
 * Virtual data pack that serves programmer-based crafting recipe JSON files.
 * The recipe JSON strings are generated at startup by {@link RecipeManager.LocalCraftingManager}
 * and stored in {@link RecipeManager#craftingManager}{@code .virtualRecipes}.
 */
public class LPRecipePack implements PackResources {

	private static final String PACK_ID = "logisticspipes:virtual_recipes";
	private static final String NAMESPACE = LPConstants.LP_MOD_ID;
	private static final String RECIPE_DIR = "recipe";
	private static final Component TITLE = Component.literal("LogisticsPipes virtual recipes");
	private static final PackLocationInfo LOCATION = new PackLocationInfo(PACK_ID, TITLE, PackSource.BUILT_IN, Optional.empty());

	/** Repository source for the server data pack repository (added by PackRepositoryMixin). */
	public static void addToRepository(Consumer<Pack> consumer) {
		Pack pack = Pack.readMetaAndCreate(LOCATION, new Pack.ResourcesSupplier() {
			@Override
			public PackResources openPrimary(PackLocationInfo location) {
				return new LPRecipePack();
			}

			@Override
			public PackResources openFull(PackLocationInfo location, Pack.Metadata metadata) {
				return new LPRecipePack();
			}
		}, PackType.SERVER_DATA, new PackSelectionConfig(true, Pack.Position.TOP, false));
		if (pack != null) consumer.accept(pack);
	}

	@Override
	public PackLocationInfo location() {
		return LOCATION;
	}

	@Nullable
	@Override
	public IoSupplier<InputStream> getRootResource(String... paths) {
		return null;
	}

	@Nullable
	@Override
	public IoSupplier<InputStream> getResource(PackType type, Identifier location) {
		if (type != PackType.SERVER_DATA) return null;
		if (!NAMESPACE.equals(location.getNamespace())) return null;
		if (!location.getPath().startsWith(RECIPE_DIR + "/")) return null;

		String recipeName = location.getPath().substring(RECIPE_DIR.length() + 1);
		if (recipeName.endsWith(".json")) {
			recipeName = recipeName.substring(0, recipeName.length() - ".json".length());
		}
		Identifier recipeKey = Identifier.fromNamespaceAndPath(NAMESPACE, recipeName);
		RecipeManager.loadRecipes();
		String json = RecipeManager.craftingManager.virtualRecipes.get(recipeKey);
		if (json == null) return null;

		byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
		return () -> new ByteArrayInputStream(bytes);
	}

	@Override
	public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
		if (type != PackType.SERVER_DATA) return;
		if (!NAMESPACE.equals(namespace)) return;
		if (!RECIPE_DIR.equals(path) && !path.startsWith(RECIPE_DIR + "/")) return;

		RecipeManager.loadRecipes();
		for (Identifier key : RecipeManager.craftingManager.virtualRecipes.keySet()) {
			if (!key.getNamespace().equals(NAMESPACE)) continue;
			Identifier fileLocation = Identifier.fromNamespaceAndPath(NAMESPACE, RECIPE_DIR + "/" + key.getPath() + ".json");
			IoSupplier<InputStream> supplier = getResource(type, fileLocation);
			if (supplier != null) {
				output.accept(fileLocation, supplier);
			}
		}
	}

	@Override
	public Set<String> getNamespaces(PackType type) {
		if (type != PackType.SERVER_DATA) return Set.of();
		return Set.of(NAMESPACE);
	}

	@SuppressWarnings("unchecked")
	@Nullable
	@Override
	public <T> T getMetadataSection(MetadataSectionType<T> type) {
		if (type == PackMetadataSection.SERVER_TYPE || type == PackMetadataSection.FALLBACK_TYPE) {
			var format = SharedConstants.getCurrentVersion().packVersion(PackType.SERVER_DATA);
			return (T) new PackMetadataSection(TITLE, new InclusiveRange<>(format, format));
		}
		return null;
	}

	@Override
	public void close() {
		// nothing to close — data is in-memory
	}
}
