package logisticspipes.items;

import net.minecraft.world.item.Item;
import net.minecraft.client.gui.screens.Screen;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;


// net.minecraft.world.item.CreativeModeTab removed — use CreativeModeTab
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;



import logisticspipes.interfaces.IItemAdvancedExistance;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.utils.FluidIdentifierStack;
import logisticspipes.utils.item.ItemIdentifierStack;

public class LogisticsFluidContainer extends LogisticsItem implements IItemAdvancedExistance {

	static int capacity = 8000;

	public LogisticsFluidContainer() {
		super(logisticspipes.LPRegistries.itemProperties().stacksTo(1));
	}

	@Override
	public boolean canExistInNormalInventory(@Nonnull ItemStack stack) {
		return false;
	}

	@Override
	public boolean canExistInWorld(@Nonnull ItemStack stack) {
		return false;
	}

	@Override
	@Nonnull
	public net.minecraft.network.chat.Component getName(@Nonnull ItemStack itemstack) {
		FluidIdentifierStack fluidStack = SimpleServiceLocator.logisticsFluidManager.getFluidFromContainer(ItemIdentifierStack.getFromStack(itemstack));
		if (fluidStack != null) {
			return fluidStack.getFluid().getDisplayName();
		}
		return super.getName(itemstack);
	}

	@Override
	@Environment(EnvType.CLIENT)
	public void appendHoverText(@Nonnull ItemStack stack, Item.TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<net.minecraft.network.chat.Component> tooltipConsumer, net.minecraft.world.item.TooltipFlag flagIn) {
		java.util.List<net.minecraft.network.chat.Component> tooltip = new java.util.ArrayList<>();
		super.appendHoverText(stack, context, display, tooltipConsumer, flagIn);
		if (net.minecraft.client.Minecraft.getInstance().hasShiftDown()) {
			FluidIdentifierStack fluidStack = SimpleServiceLocator.logisticsFluidManager.getFluidFromContainer(ItemIdentifierStack.getFromStack(stack));
			if (fluidStack != null) {
				tooltip.add(net.minecraft.network.chat.Component.literal("Type:  " + fluidStack.getFluid().getDisplayName().getString()));
				tooltip.add(net.minecraft.network.chat.Component.literal("Value: " + fluidStack.getAmount() + "mB"));
			}
		}
		tooltip.forEach(tooltipConsumer);
	}

	// fillItemCategory removed in 1.20.1 — creative tab content registered via BuildCreativeModeTabContentsEvent
}
