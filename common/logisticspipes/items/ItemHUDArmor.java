package logisticspipes.items;

import javax.annotation.Nonnull;

import net.minecraft.world.entity.player.Player;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.Level;

import logisticspipes.api.IHUDArmor;
import logisticspipes.interfaces.ILogisticsItem;
import logisticspipes.proxy.MainProxy;

public class ItemHUDArmor extends Item implements IHUDArmor, ILogisticsItem {

	/** LP1 HUD glasses: no armour points, not dyeable; model texture from assets/logisticspipes/equipment/logisticshud.json. */
	private static final ResourceKey<EquipmentAsset> HUD_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID,
			Identifier.fromNamespaceAndPath(logisticspipes.LPConstants.LP_MOD_ID, "logisticshud"));

	public ItemHUDArmor() {
		super(logisticspipes.LPRegistries.itemProperties().stacksTo(1).component(DataComponents.EQUIPPABLE,
				Equippable.builder(EquipmentSlot.HEAD).setEquipSound(SoundEvents.ARMOR_EQUIP_LEATHER).setAsset(HUD_ASSET).build()));
	}

	@Nonnull
	@Override
	public InteractionResult use(Level world, Player player, @Nonnull InteractionHand handIn) {
		if (MainProxy.isClient(world)) {
			return InteractionResult.PASS;
		}
		useItem(player, world);
		return InteractionResult.SUCCESS;
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
					.setSlot(player.getInventory().getSelectedSlot())
					.open(player);
		}
	}

	@Override
	public boolean isEnabled(@Nonnull ItemStack item) {
		return true;
	}

}
