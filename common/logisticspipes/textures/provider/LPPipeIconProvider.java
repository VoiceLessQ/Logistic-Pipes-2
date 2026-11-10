package logisticspipes.textures.provider;

import java.util.ArrayList;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import logisticspipes.renderer.IIconProvider;

public class LPPipeIconProvider implements IIconProvider {

	@Environment(EnvType.CLIENT)
	private ArrayList<TextureAtlasSprite> icons;

	public LPPipeIconProvider() {
		if (net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
			icons = new ArrayList<>();
		}
	}

	@Override
	@Environment(EnvType.CLIENT)
	public TextureAtlasSprite getIcon(int iconIndex) {
		return icons.get(iconIndex);
	}

	@Environment(EnvType.CLIENT)
	public void setIcon(int index, TextureAtlasSprite icon) {
		while (icons.size() < index + 1) {
			icons.add(null);
		}
		icons.set(index, icon);
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void registerIcons(Object iconRegister) {}
}
