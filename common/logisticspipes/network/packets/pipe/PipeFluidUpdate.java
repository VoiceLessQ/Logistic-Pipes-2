package logisticspipes.network.packets.pipe;

import java.util.BitSet;

import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.FluidIdentifierStack;

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
	private FluidIdentifierStack[] renderCache = new FluidIdentifierStack[Direction.values().length];
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
				net.minecraft.world.level.material.Fluid fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.getValue(Identifier.tryParse(input.readUTF()));
				int amount = input.readInt();
				FluidIdentifier ident = FluidIdentifier.get(fluid, input.readCompoundTag(), null);
				renderCache[i] = ident == null ? null : ident.makeFluidIdentifierStack(amount);
			}
		}
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		for (int i = 0; i < renderCache.length; i++) {
			bits.set(i, renderCache[i] != null && renderCache[i].getAmount() > 0);
		}
		output.writeBitSet(bits);
		for (FluidIdentifierStack aRenderCache : renderCache) {
			if (aRenderCache != null && aRenderCache.getAmount() > 0) {
				output.writeUTF(aRenderCache.getFluid().fluidID);
				output.writeInt(aRenderCache.getAmount());
				output.writeCompoundTag(aRenderCache.getFluid().tag);
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
