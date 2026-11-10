package logisticspipes.textures.provider;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlas; // was TextureAtlas
import net.minecraft.resources.Identifier;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import logisticspipes.renderer.IIconProvider;

public class LPActionTriggerIconProvider implements IIconProvider {

	public static int actionDisablePipeIconIndex = 0;
	public static int triggerCraftingIconIndex = 1;
	public static int triggerPowerDischargingIconIndex = 2;
	public static int triggerPowerNeededIconIndex = 3;
	public static int triggerSupplierFailedIconIndex = 4;
	public static int triggerHasDestinationIconIndex = 5;
	public static int actionRobotRoutingIconIndex = 6;

	@Environment(EnvType.CLIENT)
	private TextureAtlasSprite[] icons;

	public LPActionTriggerIconProvider() {
		if (net.fabricmc.loader.api.FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
			icons = new TextureAtlasSprite[7];
		}
	}

	@Override
	@Environment(EnvType.CLIENT)
	public TextureAtlasSprite getIcon(int iconIndex) {
		if (iconIndex > 6) {
			return null;
		}
		return icons[iconIndex];
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void registerIcons(Object icon) {
		// TextureAtlas.registerSprite removed in 1.20.1 — BuildCraft integration not ported
	}
}
