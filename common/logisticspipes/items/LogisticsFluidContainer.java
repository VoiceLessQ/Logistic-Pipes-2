package logisticspipes.items;

import net.minecraft.client.gui.screens.Screen;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;


// net.minecraft.world.item.CreativeModeTab removed — use CreativeModeTab
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.level.Level;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;



import logisticspipes.interfaces.IItemAdvancedExistance;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.utils.FluidIdentifierStack;
import logisticspipes.utils.item.ItemIdentifierStack;

public class LogisticsFluidContainer extends LogisticsItem implements IItemAdvancedExistance {

	static int capacity = 8000;

	public LogisticsFluidContainer() {
		super(new net.minecraft.world.item.Item.Properties().stacksTo(1));
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
	public String getDescriptionId(@Nonnull ItemStack stack) {
		FluidIdentifierStack fluidStack = SimpleServiceLocator.logisticsFluidManager.getFluidFromContainer(ItemIdentifierStack.getFromStack(stack));
		if (fluidStack != null) {
			// Fluid.getDescriptionId() removed in 1.20.1; use FluidStack.getDisplayName()
			String s = fluidStack.makeFluidStack().getDisplayName().getString();
			if (s != null) {
				return s;
			}
		}
		return super.getDescriptionId(stack);
	}

	@Override
	@Nonnull
	public net.minecraft.network.chat.Component getName(@Nonnull ItemStack itemstack) {
		// getUnlocalizedNameInefficiently removed in 1.20.1; use getDescriptionId() for base key
		String translationKey = getDescriptionId(itemstack);
		String baseKey = getDescriptionId();
		return net.minecraft.network.chat.Component.literal(
				I18n.get(translationKey + (translationKey.equals(baseKey) ? ".name" : "")).trim());
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(@Nonnull ItemStack stack, net.minecraft.world.item.Item.TooltipContext worldIn, java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flagIn) {
		super.appendHoverText(stack, worldIn, tooltip, flagIn);
		if (Screen.hasShiftDown()) {
			FluidIdentifierStack fluidStack = SimpleServiceLocator.logisticsFluidManager.getFluidFromContainer(ItemIdentifierStack.getFromStack(stack));
			if (fluidStack != null) {
				tooltip.add(net.minecraft.network.chat.Component.literal("Type:  " + fluidStack.makeFluidStack().getDisplayName().getString()));
				tooltip.add(net.minecraft.network.chat.Component.literal("Value: " + fluidStack.getAmount() + "mB"));
			}
		}
	}

	// fillItemCategory removed in 1.20.1 — creative tab content registered via BuildCreativeModeTabContentsEvent
}
