package logisticspipes.renderer;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public interface IIconProvider {

	@Environment(EnvType.CLIENT)
	TextureAtlasSprite getIcon(int iconIndex);

	void registerIcons(Object textureMap);
}
