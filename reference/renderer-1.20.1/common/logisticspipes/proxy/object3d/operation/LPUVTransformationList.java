package logisticspipes.proxy.object3d.operation;

import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.object3d.interfaces.I3DOperation;
import logisticspipes.proxy.object3d.interfaces.TextureTransformation;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * Composite op holding both UV transforms (scale/translate) and a texture binding.
 * Implements {@link TextureTransformation} so {@code LPModel3DImpl.render()} picks
 * up the texture via its {@code op instanceof TextureTransformation} branch, and
 * {@code getOriginal()} exposes the combined LPUvOp so the UV branch still fires.
 */
public class LPUVTransformationList implements I3DOperation, TextureTransformation {

	private final I3DOperation[] children;
	private TextureTransformation tex;

	public LPUVTransformationList(I3DOperation... uvTranslation) {
		this.children = uvTranslation;
		for (I3DOperation op : uvTranslation) {
			if (op instanceof TextureTransformation) {
				tex = (TextureTransformation) op;
				break;
			}
		}
	}

	public I3DOperation[] getChildren() {
		return children;
	}

	@Override
	public Object getOriginal() {
		return this;
	}

	@Override
	public void update(TextureAtlasSprite sprite) {
		if (tex != null) tex.update(sprite);
	}

	@Override
	public TextureAtlasSprite getTexture() {
		return tex == null ? null : tex.getTexture();
	}

	public TextureTransformation getInnerTexture() {
		return tex;
	}
}
