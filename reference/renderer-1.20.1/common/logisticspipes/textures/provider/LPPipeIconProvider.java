package logisticspipes.textures.provider;

import java.util.ArrayList;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import logisticspipes.renderer.IIconProvider;

public class LPPipeIconProvider implements IIconProvider {

	@OnlyIn(Dist.CLIENT)
	private ArrayList<TextureAtlasSprite> icons;

	public LPPipeIconProvider() {
		if (FMLEnvironment.dist == Dist.CLIENT) {
			icons = new ArrayList<>();
		}
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public TextureAtlasSprite getIcon(int iconIndex) {
		return icons.get(iconIndex);
	}

	@OnlyIn(Dist.CLIENT)
	public void setIcon(int index, TextureAtlasSprite icon) {
		while (icons.size() < index + 1) {
			icons.add(null);
		}
		icons.set(index, icon);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void registerIcons(Object iconRegister) {}
}
