package logisticspipes.textures.provider;

import java.util.ArrayList;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.object3d.interfaces.TextureTransformation;

public class LPPipeIconTransformerProvider {

	@OnlyIn(Dist.CLIENT)
	private ArrayList<TextureTransformation> icons;

	public LPPipeIconTransformerProvider() {
		if (FMLEnvironment.dist == Dist.CLIENT) {
			icons = new ArrayList<>();
		}
	}

	@OnlyIn(Dist.CLIENT)
	public TextureTransformation getIcon(int iconIndex) {
		if (iconIndex < 0 || iconIndex >= icons.size()) return null;
		return icons.get(iconIndex);
	}

	@OnlyIn(Dist.CLIENT)
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
