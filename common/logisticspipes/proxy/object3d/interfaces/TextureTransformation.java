package logisticspipes.proxy.object3d.interfaces;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public interface TextureTransformation extends I3DOperation {

	@Environment(EnvType.CLIENT)
	void update(TextureAtlasSprite registerIcon);

	@Environment(EnvType.CLIENT)
	TextureAtlasSprite getTexture();
}
