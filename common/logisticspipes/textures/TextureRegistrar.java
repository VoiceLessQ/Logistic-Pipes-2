package logisticspipes.textures;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.reloader.ResourceReloaderKeys;

import logisticspipes.LPConstants;
import logisticspipes.renderer.LogisticsSolidBlockRenderer;

/**
 * Collects (index, fileName) pairs registered via {@link Textures#registerBlockIcons(Object)}
 * and binds real {@link TextureAtlasSprite}s into the LP icon providers during the block
 * atlas reload. Powered / unpowered / un-overlayed variants resolve to LP1's
 * pre-generated composites under {@code blocks/pipes/overlay_gen/} (stitched via the
 * atlas config in {@code assets/minecraft/atlases/blocks.json}).
 */
public class TextureRegistrar {

	private static final Identifier RELOADER_ID = Identifier.fromNamespaceAndPath(LPConstants.LP_MOD_ID, "sprites");

	private static final List<Entry> ENTRIES = new ArrayList<>();
	private static final List<Entry> NEW_ENTRIES = new ArrayList<>();
	private static boolean collected = false;

	public static void record(int index, String fileName) {
		if (fileName == null || fileName.isEmpty()) return;
		String path = resolvePath(fileName);
		if (path == null) return;
		ENTRIES.add(new Entry(index, Identifier.fromNamespaceAndPath(LPConstants.LP_MOD_ID, path)));
	}

	public static void recordNew(int index, String fileName) {
		if (fileName == null || fileName.isEmpty()) return;
		String path = resolvePath(fileName);
		if (path == null) return;
		NEW_ENTRIES.add(new Entry(index, Identifier.fromNamespaceAndPath(LPConstants.LP_MOD_ID, path)));
	}

	/**
	 * Base+overlay composite, pre-generated on disk under overlay_gen/ (LP1 layout):
	 * e.g. ("pipes/basic", "pipes/status_overlay/powered-pipe")
	 * → blocks/pipes/overlay_gen/basic/powered-pipe.
	 */
	public static void recordOverlay(int index, String fileName, String overlayName) {
		if (fileName == null || fileName.isEmpty() || overlayName == null || overlayName.isEmpty()) return;
		String path = "blocks/" + fileName.replace("pipes/", "pipes/overlay_gen/")
				+ "/" + overlayName.replace("pipes/status_overlay/", "");
		ENTRIES.add(new Entry(index, Identifier.fromNamespaceAndPath(LPConstants.LP_MOD_ID, path)));
	}

	// Maps the legacy Textures.java fileName (e.g. "pipes/basic") to the actual
	// on-disk texture path under assets/logisticspipes/textures/. The 1.12.2
	// code assumed a flat "blocks/<fileName>" layout, but the real files live
	// under several subfolders in resources/assets/logisticspipes/textures/blocks/pipes/.
	private static String resolvePath(String fileName) {
		// Flat file directly under blocks/pipes/ (no new_texture/ prefix)
		if (fileName.equals("pipes/liquid_connector")) {
			return "blocks/" + fileName;
		}
		// Status overlays live under blocks/pipes/status_overlay/
		if (fileName.startsWith("pipes/status_overlay/")) {
			return "blocks/" + fileName;
		}
		// Chassi status overlays exist only as overlay_gen composites in 1.12.2 —
		// no flat sprite file to bind; skip (getSprite would return the missing sprite).
		if (fileName.startsWith("pipes/chassi/status_overlay/")) {
			return null;
		}
		// Everything else (basic pipes, chassi mk1–5, transport) lives under
		// blocks/pipes/new_texture/ — strip the leading "pipes/" and re-prefix.
		if (fileName.startsWith("pipes/")) {
			return "blocks/pipes/new_texture/" + fileName.substring("pipes/".length());
		}
		return null;
	}

	private static void collectOnce() {
		if (collected) return;
		collected = true;
		// Runs registerBlockIcons with a null register; ClientProxy.addLogisticsPipesOverride
		// now forwards each call into record() above.
		new Textures().registerBlockIcons(null);
	}

	/** Re-binds the sprites after every resource reload, once the atlases are stitched. */
	public static void register() {
		ResourceLoader loader = ResourceLoader.get(PackType.CLIENT_RESOURCES);
		loader.registerReloadListener(RELOADER_ID, (ResourceManagerReloadListener) resourceManager -> onPost());
		loader.addListenerOrdering(ResourceReloaderKeys.Client.ATLAS, RELOADER_ID);
	}

	private static void onPost() {
		collectOnce();
		LogisticsSolidBlockRenderer.clearCache();
		logisticspipes.renderer.LogisticsRenderPipe.requestTableIcon = null;
		TextureAtlas atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);

		// Bind the three static pipe-model textures consumed directly by
		// LogisticsNewRenderPipe (basicPipeTexture / statusTexture / statusBCTexture).
		// These are looked up by Identifier rather than by Textures.java index.
		TextureAtlasSprite base = atlas.getSprite(
			Identifier.fromNamespaceAndPath(LPConstants.LP_MOD_ID, "blocks/pipes/pipemodel"));
		TextureAtlasSprite status = atlas.getSprite(
			Identifier.fromNamespaceAndPath(LPConstants.LP_MOD_ID, "blocks/pipes/pipemodel-status"));
		TextureAtlasSprite statusBC = atlas.getSprite(
			Identifier.fromNamespaceAndPath(LPConstants.LP_MOD_ID, "blocks/pipes/pipemodel-status-bc"));
		TextureAtlasSprite inactive = atlas.getSprite(
			Identifier.fromNamespaceAndPath(LPConstants.LP_MOD_ID, "blocks/pipes/pipemodel-inactive"));
		TextureAtlasSprite innerBox = atlas.getSprite(
			Identifier.fromNamespaceAndPath(LPConstants.LP_MOD_ID, "blocks/pipes/innerbox"));
		TextureAtlasSprite glassCenter = atlas.getSprite(
			Identifier.fromNamespaceAndPath(LPConstants.LP_MOD_ID, "blocks/pipes/glass_texture_center"));
		if (base != null) {
			logisticspipes.renderer.newpipe.LogisticsNewRenderPipe.basicPipeTexture =
				logisticspipes.proxy.SimpleServiceLocator.cclProxy.createIconTransformer(base);
		}
		if (status != null) {
			logisticspipes.renderer.newpipe.LogisticsNewRenderPipe.statusTexture =
				logisticspipes.proxy.SimpleServiceLocator.cclProxy.createIconTransformer(status);
		}
		if (statusBC != null) {
			logisticspipes.renderer.newpipe.LogisticsNewRenderPipe.statusBCTexture =
				logisticspipes.proxy.SimpleServiceLocator.cclProxy.createIconTransformer(statusBC);
		}
		if (inactive != null) {
			logisticspipes.renderer.newpipe.LogisticsNewRenderPipe.inactiveTexture =
				logisticspipes.proxy.SimpleServiceLocator.cclProxy.createIconTransformer(inactive);
		}
		if (innerBox != null) {
			logisticspipes.renderer.newpipe.LogisticsNewRenderPipe.innerBoxTexture =
				logisticspipes.proxy.SimpleServiceLocator.cclProxy.createIconTransformer(innerBox);
		}
		if (glassCenter != null) {
			logisticspipes.renderer.newpipe.LogisticsNewRenderPipe.glassCenterTexture =
				logisticspipes.proxy.SimpleServiceLocator.cclProxy.createIconTransformer(glassCenter);
		}
		for (Entry e : ENTRIES) {
			TextureAtlasSprite sprite = atlas.getSprite(e.rl);
			if (sprite == null) continue;
			if (Textures.LPpipeIconProvider != null) {
				Textures.LPpipeIconProvider.setIcon(e.index, sprite);
			}
		}
		for (Entry e : NEW_ENTRIES) {
			TextureAtlasSprite sprite = atlas.getSprite(e.rl);
			if (sprite == null) continue;
			if (Textures.LPnewPipeIconProvider != null) {
				Textures.LPnewPipeIconProvider.setIcon(e.index, sprite);
			}
		}
	}

	private record Entry(int index, Identifier rl) {}
}
