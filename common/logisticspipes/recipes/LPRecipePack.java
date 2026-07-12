package logisticspipes.recipes;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.metadata.pack.PackMetadataSection;
import net.minecraft.server.packs.resources.IoSupplier;

import logisticspipes.LPConstants;

/**
 * Virtual data pack that serves programmer-based crafting recipe JSON files.
 * The recipe JSON strings are generated at startup by {@link RecipeManager.LocalCraftingManager}
 * and stored in {@link RecipeManager#craftingManager}{@code .virtualRecipes}.
 */
public class LPRecipePack implements PackResources {

	private static final String PACK_ID = "logisticspipes:virtual_recipes";
	private static final String NAMESPACE = LPConstants.LP_MOD_ID;

	public static final net.minecraft.server.packs.PackLocationInfo LOCATION = new net.minecraft.server.packs.PackLocationInfo(
			PACK_ID,
			Component.literal("LogisticsPipes virtual recipes"),
			net.minecraft.server.packs.repository.PackSource.BUILT_IN,
			java.util.Optional.empty());

	@Override
	public net.minecraft.server.packs.PackLocationInfo location() {
		return LOCATION;
	}

	@Nullable
	@Override
	public IoSupplier<InputStream> getRootResource(String... paths) {
		return null;
	}

	@Nullable
	@Override
	public IoSupplier<InputStream> getResource(PackType type, ResourceLocation location) {
		if (type != PackType.SERVER_DATA) return null;
		if (!NAMESPACE.equals(location.getNamespace())) return null;
		if (!location.getPath().startsWith("recipes/")) return null;

		// Strip "recipes/" prefix, reconstruct ResourceLocation key
		String recipeName = location.getPath().substring("recipes/".length());
		if (recipeName.endsWith(".json")) {
			recipeName = recipeName.substring(0, recipeName.length() - ".json".length());
		}
		ResourceLocation recipeKey = ResourceLocation.fromNamespaceAndPath(NAMESPACE, recipeName);
		String json = RecipeManager.craftingManager.virtualRecipes.get(recipeKey);
		if (json == null) return null;

		byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
		return () -> new ByteArrayInputStream(bytes);
	}

	@Override
	public void listResources(PackType type, String namespace, String path, ResourceOutput output) {
		if (type != PackType.SERVER_DATA) return;
		if (!NAMESPACE.equals(namespace)) return;
		if (!"recipes".equals(path) && !path.startsWith("recipes/")) return;

		for (ResourceLocation key : RecipeManager.craftingManager.virtualRecipes.keySet()) {
			if (!key.getNamespace().equals(NAMESPACE)) continue;
			ResourceLocation fileLocation = ResourceLocation.fromNamespaceAndPath(NAMESPACE, "recipes/" + key.getPath() + ".json");
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
	public <T> T getMetadataSection(MetadataSectionSerializer<T> deserializer) {
		if (deserializer == PackMetadataSection.TYPE) {
			// Pack format 48 = 1.21.1 data packs
			return (T) new PackMetadataSection(
					Component.literal("LogisticsPipes virtual recipes"),
					48,
					java.util.Optional.empty());
		}
		return null;
	}

	@Override
	public void close() {
		// nothing to close — data is in-memory
	}
}
