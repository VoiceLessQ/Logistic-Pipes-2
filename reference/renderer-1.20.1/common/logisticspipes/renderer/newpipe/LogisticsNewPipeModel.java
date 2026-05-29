package logisticspipes.renderer.newpipe;

// TODO: rendering deferred — LogisticsNewPipeModel needs full rewrite for 1.20.1
// Removed: IModel, IModelState, ICustomModelLoader, ItemOverrideList, ItemCameraTransforms,
//          VertexFormat (old), IResourceManager, javax.vecmath, ModelResourceLocation (old path),
//          IExtendedBlockState, BlockRenderLayer, MinecraftForgeClient, PerspectiveMapWrapper

import java.util.Map;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import com.google.common.collect.Maps;

import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.object3d.interfaces.TextureTransformation;

@OnlyIn(Dist.CLIENT)
public class LogisticsNewPipeModel {

	private static final ResourceLocation BASE_TEXTURE = new ResourceLocation("logisticspipes", "blocks/blank_pipe");
	public static TextureAtlasSprite BASE_TEXTURE_SPRITE;
	public static TextureTransformation BASE_TEXTURE_TRANSFORM;

	public static void registerTextures(TextureAtlas iconRegister) {
		// TODO: rendering deferred — TextureAtlas.registerSprite removed; use event-based registration
	}

	// Kept for external references
	public static Map<Object, CoreUnroutedPipe> nameTextureIdMap = Maps.newLinkedHashMap();

	public static class LogisticsNewPipeModelLoader {
		// TODO: rendering deferred — was ICustomModelLoader
	}
}
