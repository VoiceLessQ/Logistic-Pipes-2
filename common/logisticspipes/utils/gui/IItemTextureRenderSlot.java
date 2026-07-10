package logisticspipes.utils.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public abstract class IItemTextureRenderSlot implements IRenderSlot {

	@OnlyIn(Dist.CLIENT)
	public abstract TextureAtlasSprite getTextureIcon();

	public abstract boolean drawSlotIcon();

	public abstract boolean customRender(Minecraft mc, float zLevel);

	@Override
	public int getSize() {
		return 18;
	}
}
