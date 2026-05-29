package logisticspipes.proxy.object3d.impl;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import logisticspipes.proxy.object3d.interfaces.TextureTransformation;

/**
 * Maps per-quad UVs in [0, 1] onto a texture atlas sprite's sub-rectangle.
 * In 1.20.1 block textures live on the block atlas and sprites expose
 * {@code getU0/getU1/getV0/getV1} for interpolation.
 */
public final class LPTextureTransformationImpl implements TextureTransformation {

	private TextureAtlasSprite sprite;

	public LPTextureTransformationImpl(TextureAtlasSprite sprite) {
		this.sprite = sprite;
	}

	@Override
	public void update(TextureAtlasSprite newSprite) {
		this.sprite = newSprite;
	}

	@Override
	public TextureAtlasSprite getTexture() {
		return sprite;
	}

	/** Maps a normalized UV (0..1) to atlas-space UV for {@link #sprite}.
	 *  Uses explicit getU0/getU1 interpolation with a half-texel inset so
	 *  mipmapped sampling can't bleed into neighboring atlas sprites. */
	public float mapU(float u) {
		if (sprite == null) return u;
		float u0 = sprite.getU0();
		float u1 = sprite.getU1();
		float inset = (u1 - u0) / 512.0f; // ~half texel for a 256px sprite
		float cu = u < 0 ? 0 : (u > 1 ? 1 : u);
		return (u0 + inset) + ((u1 - u0) - 2 * inset) * cu;
	}

	public float mapV(float v) {
		if (sprite == null) return v;
		float v0 = sprite.getV0();
		float v1 = sprite.getV1();
		float inset = (v1 - v0) / 512.0f;
		float cv = v < 0 ? 0 : (v > 1 ? 1 : v);
		return (v0 + inset) + ((v1 - v0) - 2 * inset) * cv;
	}

	@Override
	public Object getOriginal() {
		return this;
	}
}
