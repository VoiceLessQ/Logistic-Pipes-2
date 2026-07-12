package logisticspipes.logisticspipes;

import java.util.List;
import java.util.Objects;
import java.util.Random;
import javax.annotation.Nonnull;

import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import logisticspipes.interfaces.IClientInformationProvider;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.proxy.MainProxy;

public class ItemModuleInformationManager {

	public static void saveInformation(@Nonnull ItemStack stack, LogisticsModule module) {
		if (module == null) {
			return;
		}
		CompoundTag nbt = new CompoundTag();
		module.writeToNBT(nbt);
		if (nbt.equals(new CompoundTag())) {
			return;
		}
		if (MainProxy.isClient()) {
			ListTag list = new ListTag();
			String info1 = "Please reopen the window";
			String info2 = "to see the information.";
			list.add(net.minecraft.nbt.StringTag.valueOf(info1));
			list.add(net.minecraft.nbt.StringTag.valueOf(info2));
			CompoundTag tag = logisticspipes.utils.item.StackTag.getTag(stack);
			if (tag == null) tag = new CompoundTag();
			tag.put("informationList", list);
			tag.putDouble("Random-Stack-Prevent", new Random().nextDouble());
			logisticspipes.utils.item.StackTag.setTag(stack, tag);
			return;
		}
		CompoundTag tag = logisticspipes.utils.item.StackTag.getTag(stack);
		if (tag == null) tag = new CompoundTag();
		tag.put("moduleInformation", nbt);
		if (module instanceof IClientInformationProvider) {
			List<String> information = ((IClientInformationProvider) module).getClientInformation();
			if (information.size() > 0) {
				ListTag list = new ListTag();
				for (String info : information) {
					list.add(net.minecraft.nbt.StringTag.valueOf(info));
				}
				tag.put("informationList", list);
			}
		}
		tag.putDouble("Random-Stack-Prevent", new Random().nextDouble());
		logisticspipes.utils.item.StackTag.setTag(stack, tag);
	}

	public static void readInformation(@Nonnull ItemStack stack, LogisticsModule module) {
		if (module == null) {
			return;
		}
		if (logisticspipes.utils.item.StackTag.hasTag(stack)) {
			CompoundTag nbt = Objects.requireNonNull(logisticspipes.utils.item.StackTag.getTag(stack));
			if (nbt.contains("moduleInformation")) {
				CompoundTag moduleInformation = nbt.getCompound("moduleInformation");
				module.readFromNBT(moduleInformation);
			}
		}
	}
}
