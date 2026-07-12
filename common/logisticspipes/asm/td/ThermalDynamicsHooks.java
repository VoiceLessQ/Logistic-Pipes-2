package logisticspipes.asm.td;

import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

// import cofh.thermaldynamics.duct.item.TravelingItem;

import logisticspipes.renderer.LogisticsRenderPipe;
import logisticspipes.routing.ItemRoutingInformation;

public class ThermalDynamicsHooks {

	public static void travelingItemToNBT(Object /* TravelingItem */ travelingItem, CompoundTag paramCompoundTag) {
		if (((ILPTravelingItemInfo) travelingItem).getLPRoutingInfoAddition() != null) {
			CompoundTag save = new CompoundTag();
			((ItemRoutingInformation) ((ILPTravelingItemInfo) travelingItem).getLPRoutingInfoAddition()).writeToNBT(save);
			paramCompoundTag.put("LPRoutingInformation", save);
		}
	}

	public static void travelingItemNBTContructor(Object /* TravelingItem */ travelingItem, CompoundTag paramCompoundTag) {
		if (!paramCompoundTag.contains("LPRoutingInformation")) {
			return;
		}
		((ILPTravelingItemInfo) travelingItem).setLPRoutingInfoAddition(new ItemRoutingInformation());
		((ItemRoutingInformation) ((ILPTravelingItemInfo) travelingItem).getLPRoutingInfoAddition()).readFromNBT(paramCompoundTag.getCompound("LPRoutingInformation"));
	}

	public static void renderItemTransportBox(Object /* TravelingItem */ item) {
		// TODO: ThermalDynamics TravelingItem.stack field access deferred — TD not on classpath for 1.20.1
	}

	public static ItemStack handleItemSendPacket(ItemStack stack, Object /* TravelingItem */ item) {
		// TODO: ThermalDynamics TravelingItem.stack field access deferred — TD not on classpath for 1.20.1
		if (((ILPTravelingItemInfo) item).getLPRoutingInfoAddition() != null) {
			stack = stack.copy();
			CompoundTag transportTag = logisticspipes.utils.item.StackTag.hasTag(stack) ? logisticspipes.utils.item.StackTag.getTag(stack) : new CompoundTag();
			transportTag.putString("LogsitcsPipes_ITEM_ON_TRANSPORTATION", "YES");
			logisticspipes.utils.item.StackTag.setTag(stack, transportTag);
		}
		return stack;
	}
}