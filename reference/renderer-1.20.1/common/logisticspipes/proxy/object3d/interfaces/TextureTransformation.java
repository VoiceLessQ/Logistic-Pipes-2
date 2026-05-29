package logisticspipes.proxy.object3d.interfaces;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public interface TextureTransformation extends I3DOperation {

	@OnlyIn(Dist.CLIENT)
	void update(TextureAtlasSprite registerIcon);

	@OnlyIn(Dist.CLIENT)
	TextureAtlasSprite getTexture();
}
