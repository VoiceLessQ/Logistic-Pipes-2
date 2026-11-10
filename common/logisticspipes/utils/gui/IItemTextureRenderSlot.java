package logisticspipes.utils.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

public abstract class IItemTextureRenderSlot implements IRenderSlot {

	@Environment(EnvType.CLIENT)
	public abstract TextureAtlasSprite getTextureIcon();

	public abstract boolean drawSlotIcon();

	public abstract boolean customRender(Minecraft mc, float zLevel);

	@Override
	public int getSize() {
		return 18;
	}
}
