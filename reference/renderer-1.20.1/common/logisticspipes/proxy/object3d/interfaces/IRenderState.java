package logisticspipes.proxy.object3d.interfaces;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter; // was IBlockAccess

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public interface IRenderState {

	void reset();

	void setAlphaOverride(int i);

	void draw();

	void setBrightness(BlockGetter world, BlockPos pos);

	@OnlyIn(Dist.CLIENT)
	void startDrawing(int mode, VertexFormat format);

}
