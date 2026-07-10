package logisticspipes.items;

import net.minecraft.client.gui.screens.Screen;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import logisticspipes.pipes.upgrades.IPipeUpgrade;
import network.rs485.logisticspipes.util.TextUtil;

public class ItemUpgrade extends LogisticsItem {

	//Values
	public static final int MAX_LIQUID_CRAFTER = 3;
	public static final int MAX_CRAFTING_CLEANUP = 4;
	public static final int MAX_ITEM_EXTRACTION = 8;
	public static final int MAX_ITEM_STACK_EXTRACTION = 8;

	private static class Upgrade {

		private Supplier<? extends IPipeUpgrade> upgradeConstructor;
		private Class<? extends IPipeUpgrade> upgradeClass;

		private Upgrade(Supplier<? extends IPipeUpgrade> moduleConstructor) {
			upgradeConstructor = moduleConstructor;
			upgradeClass = moduleConstructor.get().getClass();
		}

		private IPipeUpgrade getIPipeUpgrade() {
			if (upgradeConstructor == null) {
				return null;
			}
			return upgradeConstructor.get();
		}

		private Class<? extends IPipeUpgrade> getIPipeUpgradeClass() {
			return upgradeClass;
		}
	}

	private Upgrade upgradeType;

	public ItemUpgrade(Upgrade upgradeType) {
		super();
		this.upgradeType = upgradeType;
	}

	/** Factory for use with DeferredRegister. */
	public static ItemUpgrade of(@Nonnull Supplier<? extends IPipeUpgrade> upgradeConstructor) {
		return new ItemUpgrade(new Upgrade(upgradeConstructor));
	}

	@Nonnull
	public static Item getAndCheckUpgrade(ResourceLocation resource) {
		Objects.requireNonNull(resource, "Resource for upgrade is null. Was the upgrade registered?");
		return Objects.requireNonNull(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(resource), "Upgrade " + resource.toString() + " not found in Item registry");
	}

	public IPipeUpgrade getUpgradeForItem(@Nonnull ItemStack itemStack, IPipeUpgrade currentUpgrade) {
		if (itemStack.isEmpty()) {
			return null;
		}
		if (itemStack.getItem() != this) {
			return null;
		}
		if (upgradeType.getIPipeUpgradeClass() == null) {
			return null;
		}
		if (currentUpgrade != null) {
			if (upgradeType.getIPipeUpgradeClass().equals(currentUpgrade.getClass())) {
				return currentUpgrade;
			}
		}
		IPipeUpgrade newUpgrade = upgradeType.getIPipeUpgrade();
		if (newUpgrade == null) {
			return null;
		}
		return newUpgrade;
	}

	@Override
	public String getModelSubdir() {
		return "upgrade";
	}

	public static String SHIFT_INFO_PREFIX = "item.upgrade.info.";

	@Override
	@OnlyIn(Dist.CLIENT)
	public void appendHoverText(@Nonnull ItemStack stack, @Nullable Level worldIn, java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flagIn) {
		super.appendHoverText(stack, worldIn, tooltip, flagIn);
		IPipeUpgrade upgrade = getUpgradeForItem(stack, null);
		if (upgrade == null) {
			return;
		}
		List<String> pipe = Arrays.asList(upgrade.getAllowedPipes());
		List<String> module = Arrays.asList(upgrade.getAllowedModules());
		if (pipe.isEmpty() && module.isEmpty()) {
			return;
		}
		if (Screen.hasShiftDown()) {
			if (!pipe.isEmpty() && !module.isEmpty()) {
				//Can be applied to {0} pipes
				//and {0} modules
				String base1 = TextUtil.translate(ItemUpgrade.SHIFT_INFO_PREFIX + "both1");
				String base2 = TextUtil.translate(ItemUpgrade.SHIFT_INFO_PREFIX + "both2");
				tooltip.add(net.minecraft.network.chat.Component.literal(MessageFormat.format(base1, join(pipe))));
				tooltip.add(net.minecraft.network.chat.Component.literal(MessageFormat.format(base2, join(module))));
			} else if (!pipe.isEmpty()) {
				//Can be applied to {0} pipes
				String base = TextUtil.translate(ItemUpgrade.SHIFT_INFO_PREFIX + "pipe");
				tooltip.add(net.minecraft.network.chat.Component.literal(MessageFormat.format(base, join(pipe))));
			} else {
				//Can be applied to {0} modules
				String base = TextUtil.translate(ItemUpgrade.SHIFT_INFO_PREFIX + "module");
				tooltip.add(net.minecraft.network.chat.Component.literal(MessageFormat.format(base, join(module))));
			}
		} else {
			TextUtil.addTooltipInformation(stack, tooltip, false);
		}
	}

	@OnlyIn(Dist.CLIENT)
	private String join(List<String> join) {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < join.size() - 2; i++) {
			builder.append(TextUtil.translate(ItemUpgrade.SHIFT_INFO_PREFIX + join.get(i)));
			builder.append(", ");
		}
		if (join.size() > 1) {
			builder.append(TextUtil.translate(ItemUpgrade.SHIFT_INFO_PREFIX + join.get(join.size() - 2)));
			builder.append(" and ");
		}
		builder.append(TextUtil.translate(ItemUpgrade.SHIFT_INFO_PREFIX + join.get(join.size() - 1)));
		return builder.toString();
	}
}
