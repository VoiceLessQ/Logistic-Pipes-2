package logisticspipes.network.packets.modules;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.gui.GuiSupplierPipe;
import logisticspipes.modules.ModuleActiveSupplier;
import logisticspipes.modules.ModuleActiveSupplier.PatternMode;
import logisticspipes.modules.ModuleActiveSupplier.SupplyMode;
import logisticspipes.network.abstractpackets.IntegerModuleCoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class SupplierPipeMode extends IntegerModuleCoordinatesPacket {

	@Getter
	@Setter
	private boolean hasPatternUpgrade;

	public SupplierPipeMode(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new SupplierPipeMode(getId());
	}

	@Override
	public void processPacket(Player player) {
		ModuleActiveSupplier module = this.getLogisticsModule(player, ModuleActiveSupplier.class);
		if (module == null) {
			return;
		}
		if (hasPatternUpgrade) {
			module.patternMode.setValue(PatternMode.values()[getInteger()]);
		} else {
			module.requestMode.setValue(SupplyMode.values()[getInteger()]);
		}
		if (Minecraft.getInstance().gui.screen() instanceof GuiSupplierPipe) {
			((GuiSupplierPipe) Minecraft.getInstance().gui.screen()).refreshMode();
		}
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		hasPatternUpgrade = input.readBoolean();
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeBoolean(hasPatternUpgrade);
	}

}
