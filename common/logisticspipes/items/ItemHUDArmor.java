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

	/** LP1 HUD glasses: no armour points, not dyeable (leather tinted the texture brown). */
	private static final net.minecraft.world.item.ArmorMaterial HUD_MATERIAL = new net.minecraft.world.item.ArmorMaterial() {
		@Override public int getDurabilityForType(@Nonnull ArmorItem.Type type) { return 0; }
		@Override public int getDefenseForType(@Nonnull ArmorItem.Type type) { return 0; }
		@Override public int getEnchantmentValue() { return 0; }
		@Override @Nonnull public net.minecraft.sounds.SoundEvent getEquipSound() { return net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_LEATHER; }
		@Override @Nonnull public net.minecraft.world.item.crafting.Ingredient getRepairIngredient() { return net.minecraft.world.item.crafting.Ingredient.EMPTY; }
		@Override @Nonnull public String getName() { return logisticspipes.LPConstants.LP_MOD_ID + ":hud"; }
		@Override public float getToughness() { return 0f; }
		@Override public float getKnockbackResistance() { return 0f; }
	};

	public ItemHUDArmor() {
		super(HUD_MATERIAL, ArmorItem.Type.HELMET, new Properties());
	}

	@Override
	public String getArmorTexture(ItemStack stack, net.minecraft.world.entity.Entity entity, net.minecraft.world.entity.EquipmentSlot slot, String type) {
		return logisticspipes.LPConstants.LP_MOD_ID + ":textures/armor/logisticshud_1.png";
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
