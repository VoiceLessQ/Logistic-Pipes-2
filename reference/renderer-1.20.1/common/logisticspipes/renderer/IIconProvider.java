package logisticspipes.renderer;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public interface IIconProvider {

	@OnlyIn(Dist.CLIENT)
	TextureAtlasSprite getIcon(int iconIndex);

	void registerIcons(Object textureMap);
}
