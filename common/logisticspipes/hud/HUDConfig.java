package logisticspipes.hud;

import javax.annotation.Nonnull;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import logisticspipes.interfaces.IHUDConfig;

public class HUDConfig implements IHUDConfig {

	private CompoundTag configTag;
	private ItemStack stack;

	public HUDConfig(@Nonnull ItemStack stack) {
		this(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag());
		this.stack = stack;
		save();
	}

	// the stack holds an immutable copy, so every change is written back
	private void save() {
		if (stack != null) {
			CustomData.set(DataComponents.CUSTOM_DATA, stack, configTag);
		}
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
		return configTag.getBooleanOr("HUDChassie", false);
	}

	@Override
	public boolean isHUDCrafting() {
		return configTag.getBooleanOr("HUDCrafting", false);
	}

	@Override
	public boolean isHUDInvSysCon() {
		return configTag.getBooleanOr("HUDInvSysCon", false);
	}

	@Override
	public boolean isHUDPowerLevel() {
		return configTag.getBooleanOr("HUDPowerJunction", false);
	}

	@Override
	public boolean isHUDProvider() {
		return configTag.getBooleanOr("HUDProvider", false);
	}

	@Override
	public boolean isHUDSatellite() {
		return configTag.getBooleanOr("HUDSatellite", false);
	}

	@Override
	public void setChassisHUD(boolean flag) {
		configTag.putBoolean("HUDChassie", flag);
		save();
	}

	@Override
	public void setHUDCrafting(boolean flag) {
		configTag.putBoolean("HUDCrafting", flag);
		save();
	}

	@Override
	public void setHUDInvSysCon(boolean flag) {
		configTag.putBoolean("HUDInvSysCon", flag);
		save();
	}

	@Override
	public void setHUDPowerJunction(boolean flag) {
		configTag.putBoolean("HUDPowerJunction", flag);
		save();
	}

	@Override
	public void setHUDProvider(boolean flag) {
		configTag.putBoolean("HUDProvider", flag);
		save();
	}

	@Override
	public void setHUDSatellite(boolean flag) {
		configTag.putBoolean("HUDSatellite", flag);
		save();
	}
}
