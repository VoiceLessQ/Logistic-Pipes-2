package logisticspipes.textures.provider;

import java.util.ArrayList;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.object3d.interfaces.TextureTransformation;

public class LPPipeIconTransformerProvider {

	@Environment(EnvType.CLIENT)
	private ArrayList<TextureTransformation> icons;

	public LPPipeIconTransformerProvider() {
		if (net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
			icons = new ArrayList<>();
		}
	}

	@Environment(EnvType.CLIENT)
	public TextureTransformation getIcon(int iconIndex) {
		if (iconIndex < 0 || iconIndex >= icons.size()) return null;
		return icons.get(iconIndex);
	}

	@Environment(EnvType.CLIENT)
	public void setIcon(int index, TextureAtlasSprite icon) {
		while (icons.size() < index + 1) {
			icons.add(null);
		}
		if (icons.get(index) != null) {
			icons.get(index).update(icon);
		} else {
			icons.set(index, SimpleServiceLocator.cclProxy.createIconTransformer(icon));
		}
	}
}
