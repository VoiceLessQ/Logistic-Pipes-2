package logisticspipes.hud;

import javax.annotation.Nonnull;

import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

import logisticspipes.interfaces.IHUDConfig;

public class HUDConfig implements IHUDConfig {

	private CompoundTag configTag;

	public HUDConfig(@Nonnull ItemStack stack) {
		this(logisticspipes.utils.item.StackTag.getTag(stack));
		logisticspipes.utils.item.StackTag.setTag(stack, configTag);
	}

	public HUDConfig(CompoundTag tag) {
		configTag = tag;
		if (configTag == null) {
			configTag = new CompoundTag();
		}

		if (configTag.isEmpty()) {
			configTag.putBoolean("HUDChassie", true);
			configTag.putBoolean("HUDCrafting", true);
			configTag.putBoolean("HUDInvSysCon", true);
			configTag.putBoolean("HUDPowerJunction", true);
			configTag.putBoolean("HUDProvider", true);
			configTag.putBoolean("HUDSatellite", true);
		}
	}

	@Override
	public boolean isChassisHUD() {
		return configTag.getBoolean("HUDChassie");
	}

	@Override
	public boolean isHUDCrafting() {
		return configTag.getBoolean("HUDCrafting");
	}

	@Override
	public boolean isHUDInvSysCon() {
		return configTag.getBoolean("HUDInvSysCon");
	}

	@Override
	public boolean isHUDPowerLevel() {
		return configTag.getBoolean("HUDPowerJunction");
	}

	@Override
	public boolean isHUDProvider() {
		return configTag.getBoolean("HUDProvider");
	}

	@Override
	public boolean isHUDSatellite() {
		return configTag.getBoolean("HUDSatellite");
	}

	@Override
	public void setChassisHUD(boolean flag) {
		configTag.putBoolean("HUDChassie", flag);
	}

	@Override
	public void setHUDCrafting(boolean flag) {
		configTag.putBoolean("HUDCrafting", flag);
	}

	@Override
	public void setHUDInvSysCon(boolean flag) {
		configTag.putBoolean("HUDInvSysCon", flag);
	}

	@Override
	public void setHUDPowerJunction(boolean flag) {
		configTag.putBoolean("HUDPowerJunction", flag);
	}

	@Override
	public void setHUDProvider(boolean flag) {
		configTag.putBoolean("HUDProvider", flag);
	}

	@Override
	public void setHUDSatellite(boolean flag) {
		configTag.putBoolean("HUDSatellite", flag);
	}
}
