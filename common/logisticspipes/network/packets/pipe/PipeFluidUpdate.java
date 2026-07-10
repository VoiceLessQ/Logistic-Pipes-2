package logisticspipes.network.packets.pipe;

import java.util.BitSet;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

import net.neoforged.neoforge.fluids.FluidStack;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.transport.PipeFluidTransportLogistics;
import logisticspipes.utils.StaticResolve;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class PipeFluidUpdate extends CoordinatesPacket {

	@Getter(value = AccessLevel.PRIVATE)
	@Setter
	private FluidStack[] renderCache = new FluidStack[Direction.values().length];
	private BitSet bits = new BitSet();

	public PipeFluidUpdate(int id) {
		super(id);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		bits = input.readBitSet();
		for (int i = 0; i < renderCache.length; i++) {
			if (bits.get(i)) {
				net.minecraft.world.level.material.Fluid fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(input.readUTF()));
				renderCache[i] = new FluidStack(fluid, input.readInt(), input.readCompoundTag());
			}
		}
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		for (int i = 0; i < renderCache.length; i++) {
			bits.set(i, renderCache[i] != null && !renderCache[i].isEmpty());
		}
		output.writeBitSet(bits);
		for (FluidStack aRenderCache : renderCache) {
			if (aRenderCache != null && !aRenderCache.isEmpty()) {
				output.writeUTF(net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(aRenderCache.getFluid()).toString());
				output.writeInt(aRenderCache.getAmount());
				output.writeCompoundTag(aRenderCache.getTag());
			}
		}
	}

	@Override
	public void processPacket(Player player) {
		LogisticsTileGenericPipe pipe = this.getPipe(player.level());
		if (pipe == null || pipe.pipe == null) {
			return;
		}
		if (!(pipe.pipe.transport instanceof PipeFluidTransportLogistics)) {
			return;
		}
		((PipeFluidTransportLogistics) pipe.pipe.transport).renderCache = renderCache;
	}

	@Override
	public ModernPacket template() {
		return new PipeFluidUpdate(getId());
	}

	@Override
	public boolean isCompressable() {
		return true;
	}
}
