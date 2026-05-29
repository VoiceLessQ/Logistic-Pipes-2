/*
package logisticspipes.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.HashMap;
import java.util.Map;

import logisticspipes.renderer.CustomBlockRenderer.RenderInfo;

import net.minecraft.client.Minecraft;
// import net.minecraft.client.renderer.GLAllocation; // removed — display lists not available
import net.minecraft.client.renderer.texture.TextureAtlas; // was TextureAtlas
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;



public final class FluidRenderer {

	public static final int DISPLAY_STAGES = 100;
	//private static final ResourceLocation BLOCK_TEXTURE = TextureAtlas.locationBlocksTexture;
	private static Map<Fluid, int[]> flowingRenderCache = new HashMap<>();
	private static Map<Fluid, int[]> stillRenderCache = new HashMap<>();
	private static final RenderInfo liquidBlock = new RenderInfo();

	/**
	 * Deactivate default constructor
	 * /
	private FluidRenderer() {

	}

	public static TextureAtlasSprite getFluidTexture(FluidStack fluidStack, boolean flowing) {
		if (fluidStack == null) {
			return null;
		}
		return FluidRenderer.getFluidTexture(fluidStack.getFluid(), flowing);
	}

	public static TextureAtlasSprite getFluidTexture(Fluid fluid, boolean flowing) {
		if (fluid == null) {
			return null;
		}
		TextureAtlasSprite icon = flowing ? fluid.getFlowingIcon() : fluid.getStillIcon();
		if (icon == null) {
			icon = ((TextureAtlas) Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS_TEXTURE)).getAtlasSprite("missingno");
		}
		return icon;
	}

	public static ResourceLocation getFluidSheet(FluidStack liquid) {
		if (liquid == null) {
			return FluidRenderer.BLOCK_TEXTURE;
		}
		return FluidRenderer.getFluidSheet(liquid.getFluid());
	}

	public static ResourceLocation getFluidSheet(Fluid liquid) {
		return FluidRenderer.BLOCK_TEXTURE;
	}

	public static void setColorForFluidStack(FluidStack fluidstack) {
		if (fluidstack == null) {
			return;
		}

		int color = fluidstack.getFluid().getColor(fluidstack);
		FluidRenderer.setGLColorFromInt(color);
	}

	private static void setGLColorFromInt(int color) {
		float red = (color >> 16 & 255) / 255.0F;
		float green = (color >> 8 & 255) / 255.0F;
		float blue = (color & 255) / 255.0F;
		RenderSystem.setShaderColor(red, green, blue, 1.0F);
	}

	public static int[] getFluidDisplayLists(FluidStack fluidStack, Level world, boolean flowing) {
		if (fluidStack == null) {
			return null;
		}
		Fluid fluid = fluidStack.getFluid();
		if (fluid == null) {
			return null;
		}
		Map<Fluid, int[]> cache = flowing ? FluidRenderer.flowingRenderCache : FluidRenderer.stillRenderCache;
		int[] diplayLists = cache.get(fluid);
		if (diplayLists != null) {
			return diplayLists;
		}

		diplayLists = new int[FluidRenderer.DISPLAY_STAGES];

		if (fluid.getBlock() != null) {
			FluidRenderer.liquidBlock.baseBlock = fluid.getBlock();
			FluidRenderer.liquidBlock.texture = FluidRenderer.getFluidTexture(fluidStack, flowing);
		} else {
			FluidRenderer.liquidBlock.baseBlock = Blocks.water;
			FluidRenderer.liquidBlock.texture = FluidRenderer.getFluidTexture(fluidStack, flowing);
		}

		cache.put(fluid, diplayLists);

		// GL_LIGHTING removed — use shaders
		RenderSystem.disableBlend();
		// TODO: GL11.glDisable(GL11.GL_CULL_FACE) → RenderSystem equivalent

		for (int s = 0; s < FluidRenderer.DISPLAY_STAGES; ++s) {
			diplayLists[s] = GLAllocation.generateDisplayLists(1);
			// TODO: glNewList removed — use vertex buffer rendering

			FluidRenderer.liquidBlock.minX = 0.01f;
			FluidRenderer.liquidBlock.minY = 0;
			FluidRenderer.liquidBlock.minZ = 0.01f;

			FluidRenderer.liquidBlock.maxX = 0.99f;
			FluidRenderer.liquidBlock.maxY = (float) s / (float) FluidRenderer.DISPLAY_STAGES;
			FluidRenderer.liquidBlock.maxZ = 0.99f;

			CustomBlockRenderer.INSTANCE.renderBlock(FluidRenderer.liquidBlock, world, 0, 0, 0, false, true);

			// TODO: glEndList removed
		}

		RenderSystem.setShaderColor(1, 1, 1, 1);
		// TODO: GL11.glEnable(GL11.GL_CULL_FACE) → RenderSystem equivalent
		RenderSystem.enableBlend();
		// GL_LIGHTING removed — use shaders

		return diplayLists;
	}
}
*/