package logisticspipes.pipes.signs;

import com.mojang.blaze3d.pipeline.RenderTarget; // was net.minecraft.client.shader.Framebuffer
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.renderer.LogisticsRenderPipe;

public interface IPipeSign {

	// Methods used when assigning a sign
	boolean isAllowedFor(CoreRoutedPipe pipe);

	void addSignTo(CoreRoutedPipe pipe, Direction dir, Player player);

	// For Final Pipe
	void readFromNBT(CompoundTag tag);

	void writeToNBT(CompoundTag tag);

	void init(CoreRoutedPipe pipe, Direction dir);

	void activate(Player player);

	ModernPacket getPacket();

	void updateServerSide();

	@Environment(EnvType.CLIENT)
	void render(CoreRoutedPipe pipe, LogisticsRenderPipe renderer);

	@Environment(EnvType.CLIENT)
	default void render(CoreRoutedPipe pipe, LogisticsRenderPipe renderer, PoseStack poseStack, SubmitNodeCollector collector, int packedLight) {
		render(pipe, renderer);
	}

	@Environment(EnvType.CLIENT)
	RenderTarget getMCFrameBufferForSign(); // was Framebuffer

	@Environment(EnvType.CLIENT)
	boolean doesFrameBufferNeedUpdating(CoreRoutedPipe pipe, LogisticsRenderPipe renderer);
}
