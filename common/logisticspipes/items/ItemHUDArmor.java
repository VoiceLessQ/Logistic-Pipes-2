package logisticspipes.items;

import javax.annotation.Nonnull;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;

import logisticspipes.api.IHUDArmor;
import logisticspipes.interfaces.ILogisticsItem;
import logisticspipes.proxy.MainProxy;

public class ItemHUDArmor extends ArmorItem implements IHUDArmor, ILogisticsItem {

	public ItemHUDArmor() {
		super(logisticspipes.LPRegistries.HUD_ARMOR_MATERIAL, ArmorItem.Type.HELMET, new Properties());
	}

	@Override
	public net.minecraft.resources.ResourceLocation getArmorTexture(@Nonnull ItemStack stack, net.minecraft.world.entity.Entity entity, net.minecraft.world.entity.EquipmentSlot slot, net.minecraft.world.item.ArmorMaterial.Layer layer, boolean innerModel) {
		return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(logisticspipes.LPConstants.LP_MOD_ID, "textures/armor/logisticshud_1.png");
	}

	@Nonnull
	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, @Nonnull InteractionHand handIn) {
		ItemStack stack = player.getItemInHand(handIn);
		if (MainProxy.isClient(world)) {
			return InteractionResultHolder.pass(stack);
		}
		useItem(player, world);
		return InteractionResultHolder.success(stack);
	}

	@Nonnull
	@Override
	public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext _ctx) {
		Player player = _ctx.getPlayer();
		Level world = _ctx.getLevel();
		useItem(player, world);
		if (MainProxy.isClient(world)) {
			return InteractionResult.PASS;
		}
		return InteractionResult.SUCCESS;
	}

	private void useItem(Player player, Level world) {
		if (MainProxy.isServer(world)) {
			logisticspipes.network.NewGuiHandler.getGui(logisticspipes.network.guis.item.HUDSettingsGui.class)
					.setSlot(player.getInventory().selected)
					.open(player);
		}
	}

	@Override
	public boolean isEnabled(@Nonnull ItemStack item) {
		return true;
	}

}
