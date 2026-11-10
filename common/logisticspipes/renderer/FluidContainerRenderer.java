package logisticspipes.renderer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.serialization.MapCodec;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.special.SpecialModelRenderers;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.transfer.v1.client.fluid.FluidVariantRendering;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;

import logisticspipes.LPConstants;
import logisticspipes.LogisticsPipes;
import logisticspipes.utils.FluidIdentifier;

/**
 * Fluid window of the logistics fluid container item, as LP1: the contained fluid's still sprite,
 * tinted, wherever the stencil texture is opaque. A special item model
 * ({@code "type": "logisticspipes:fluid_container"}) composited over the empty-container layer.
 */
@Environment(EnvType.CLIENT)
public class FluidContainerRenderer implements SpecialModelRenderer<FluidVariant> {

	private static final Identifier STENCIL = Identifier.fromNamespaceAndPath(LPConstants.LP_MOD_ID, "textures/items/liquids/stencil.png");
	// item/generated layers span z 7.5..8.5 px; sit just outside so the window covers the base layer
	private static final float FRONT_Z = 8.5F / 16 + 0.001F;
	private static final float BACK_Z = 7.5F / 16 - 0.001F;

	/** Opaque stencil pixels as horizontal runs {x0, x1 (exclusive), y}, in stencil pixels. */
	private final List<int[]> runs;
	private final int size;

	private FluidContainerRenderer(List<int[]> runs, int size) {
		this.runs = runs;
		this.size = size;
	}

	public static void register() {
		SpecialModelRenderers.ID_MAPPER.put(Identifier.fromNamespaceAndPath(LPConstants.LP_MOD_ID, "fluid_container"), Unbaked.MAP_CODEC);
	}

	public record Unbaked() implements SpecialModelRenderer.Unbaked<FluidVariant> {

		public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(new Unbaked());

		@Override
		public SpecialModelRenderer<FluidVariant> bake(SpecialModelRenderer.BakingContext context) {
			List<int[]> runs = new ArrayList<>();
			try (InputStream in = Minecraft.getInstance().getResourceManager().open(STENCIL); NativeImage stencil = NativeImage.read(in)) {
				for (int y = 0; y < stencil.getHeight(); y++) {
					int start = -1;
					for (int x = 0; x <= stencil.getWidth(); x++) {
						boolean opaque = x < stencil.getWidth() && (stencil.getPixel(x, y) >>> 24) != 0;
						if (opaque && start < 0) {
							start = x;
						} else if (!opaque && start >= 0) {
							runs.add(new int[] { start, x, y });
							start = -1;
						}
					}
				}
				return new FluidContainerRenderer(runs, stencil.getWidth());
			} catch (IOException e) {
				LogisticsPipes.log.error("Could not read the fluid container stencil " + STENCIL, e);
				return null;
			}
		}

		@Override
		public MapCodec<Unbaked> type() {
			return MAP_CODEC;
		}
	}

	@Override
	public @Nullable FluidVariant extractArgument(ItemStack stack) {
		FluidIdentifier ident = FluidIdentifier.get(stack);
		return ident == null ? null : ident.makeFluidVariant();
	}

	@Override
	public void getExtents(Consumer<Vector3fc> output) {
		output.accept(new Vector3f(0.0F, 0.0F, BACK_Z));
		output.accept(new Vector3f(1.0F, 1.0F, FRONT_Z));
	}

	@Override
	public void submit(@Nullable FluidVariant fluid, PoseStack pose, SubmitNodeCollector collector, int light, int overlay, boolean hasFoil, int outlineColor) {
		if (fluid == null || fluid.isBlank()) {
			return;
		}
		Fluid f = fluid.getFluid();
		TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(f.defaultFluidState()).stillMaterial().sprite();
		int color = 0xFF000000 | FluidVariantRendering.getColor(fluid);
		collector.submitCustomGeometry(pose, Sheets.translucentBlockItemSheet(), (p, buffer) -> {
			for (int[] run : runs) {
				float u0 = (float) run[0] / size;
				float u1 = (float) run[1] / size;
				float v0 = (float) run[2] / size;
				float v1 = (float) (run[2] + 1) / size;
				// texture v grows downwards, model y upwards
				float y0 = 1 - v1;
				float y1 = 1 - v0;
				float su0 = sprite.getU(u0), su1 = sprite.getU(u1), sv0 = sprite.getV(v0), sv1 = sprite.getV(v1);
				vertex(buffer, p, u0, y0, FRONT_Z, su0, sv1, color, light, overlay, 1);
				vertex(buffer, p, u1, y0, FRONT_Z, su1, sv1, color, light, overlay, 1);
				vertex(buffer, p, u1, y1, FRONT_Z, su1, sv0, color, light, overlay, 1);
				vertex(buffer, p, u0, y1, FRONT_Z, su0, sv0, color, light, overlay, 1);
				vertex(buffer, p, u0, y1, BACK_Z, su0, sv0, color, light, overlay, -1);
				vertex(buffer, p, u1, y1, BACK_Z, su1, sv0, color, light, overlay, -1);
				vertex(buffer, p, u1, y0, BACK_Z, su1, sv1, color, light, overlay, -1);
				vertex(buffer, p, u0, y0, BACK_Z, su0, sv1, color, light, overlay, -1);
			}
		});
	}

	private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, float u, float v, int color, int light, int overlay, float normalZ) {
		buffer.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(overlay).setLight(light).setNormal(pose, 0, 0, normalZ);
	}
}
